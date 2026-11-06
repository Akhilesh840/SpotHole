package com.pothole.khadda.detector

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Hybrid Vision Analyzer for validating physical shock anomalies using YOLOv8 detection.
 *
 * Preprocessing:
 * - Rotates bitmap by [rotationDegrees] so the road is upright.
 * - Letterboxes with aspect ratio preservation to 640x640 with gray padding 114.
 * - Normalizes RGB pixels to [0.0, 1.0].
 *
 * Output Parsing:
 * - Supports detection output shapes [1, 5, 8400] (channels first) and [1, 8400, 5] (channels last).
 * - Extracts maximum class confidence for the pothole class.
 */
class PotholeVisionAnalyzer(
    private val context: Context,
    val modelFileName: String = DEFAULT_MODEL_FILE
) {

    companion object {
        private const val TAG = "VisionAnalyzer"
        const val DEFAULT_MODEL_FILE = "pothole_model.tflite"
        const val INPUT_WIDTH = 640
        const val INPUT_HEIGHT = 640
        const val NUM_CHANNELS = 3
    }

    enum class AnalyzerStatus {
        READY,
        FAILED
    }

    var status: AnalyzerStatus = AnalyzerStatus.FAILED
        private set

    var loadError: String? = null
        private set

    var tensorLayout: TensorLayout? = null
        private set

    var outputShape: IntArray? = null
        private set

    private var interpreter: Interpreter? = null

    init {
        loadModel()
    }

    private fun loadModel() {
        try {
            Log.d(TAG, "Loading TFLite model: $modelFileName")
            val modelBuffer = FileUtil.loadMappedFile(context, modelFileName)
            val options = Interpreter.Options().apply {
                setNumThreads(4) // 4 threads for mobile CPU inference
            }
            val interp = Interpreter(modelBuffer, options)

            // Read output tensor 0 shape at load time
            val outTensor = interp.getOutputTensor(0)
            val shape = outTensor.shape()
            outputShape = shape
            Log.i(TAG, "Model '$modelFileName' loaded successfully. Output tensor 0 shape: ${shape.contentToString()}")

            val layout = PotholeOutputParser.detectLayout(shape)
            if (layout == null) {
                val errorMsg = "Unsupported output tensor shape: ${shape.contentToString()}. Expected [1, 5, 8400] or [1, 8400, 5]."
                Log.e(TAG, errorMsg)
                interp.close()
                status = AnalyzerStatus.FAILED
                loadError = errorMsg
                return
            }

            tensorLayout = layout
            interpreter = interp
            status = AnalyzerStatus.READY
            loadError = null
            Log.i(TAG, "PotholeVisionAnalyzer is READY with tensor layout: $layout")
        } catch (e: Exception) {
            val errorMsg = "Failed to load model '$modelFileName': ${e.message}"
            Log.e(TAG, errorMsg, e)
            status = AnalyzerStatus.FAILED
            loadError = errorMsg
            interpreter = null
        }
    }

    /**
     * Executes inference on the provided bitmap and returns a structured [VisionResult].
     *
     * @param bitmap Raw camera frame bitmap.
     * @param rotationDegrees Sensor rotation degrees (e.g. from CameraX imageInfo.rotationDegrees).
     * @return [VisionResult] containing max confidence, error if any, and inference duration.
     */
    fun analyze(bitmap: Bitmap, rotationDegrees: Int = 0): VisionResult {
        if (status != AnalyzerStatus.READY || interpreter == null || tensorLayout == null) {
            val errorMsg = loadError ?: "Analyzer is in FAILED state"
            Log.e(TAG, "Cannot run inference: $errorMsg")
            return VisionResult.error(errorMsg)
        }

        val startTime = System.currentTimeMillis()

        // 0. Fast Human Face Pre-Check
        if (detectHumanFace(bitmap)) {
            Log.i(TAG, "Human face detected in camera frame; rejecting as human subject")
            return VisionResult(
                maxConfidence = 0.0f,
                error = null,
                inferenceMs = System.currentTimeMillis() - startTime,
                rejectionReason = "Human face detected in frame"
            )
        }

        var letterboxedBitmap: Bitmap? = null

        try {
            // 1. Preprocessing: Rotation + Letterbox (aspect-ratio preserved, gray padding 114)
            letterboxedBitmap = LetterboxUtil.createLetterboxedBitmap(
                src = bitmap,
                rotationDegrees = rotationDegrees,
                targetSize = INPUT_WIDTH,
                paddingValue = LetterboxUtil.DEFAULT_PADDING_GRAY
            )

            // 2. Convert to Float ByteBuffer [1, 640, 640, 3] normalized to [0.0, 1.0]
            val inputBuffer = ByteBuffer.allocateDirect(1 * INPUT_WIDTH * INPUT_HEIGHT * NUM_CHANNELS * 4).apply {
                order(ByteOrder.nativeOrder())
            }

            val intValues = IntArray(INPUT_WIDTH * INPUT_HEIGHT)
            letterboxedBitmap.getPixels(intValues, 0, INPUT_WIDTH, 0, 0, INPUT_WIDTH, INPUT_HEIGHT)

            for (pixel in intValues) {
                inputBuffer.putFloat(((pixel shr 16) and 0xFF) / 255.0f) // Red
                inputBuffer.putFloat(((pixel shr 8) and 0xFF) / 255.0f)  // Green
                inputBuffer.putFloat((pixel and 0xFF) / 255.0f)          // Blue
            }

            // 3. Prepare output buffer and run inference
            val interp = interpreter!!
            val (rawMaxConfidence, bestDetection) = when (tensorLayout!!) {
                TensorLayout.CHANNELS_FIRST -> {
                    val anchors = outputShape?.getOrNull(2) ?: PotholeOutputParser.DEFAULT_ANCHORS
                    val output = Array(1) { Array(PotholeOutputParser.EXPECTED_CHANNELS) { FloatArray(anchors) } }
                    interp.run(inputBuffer, output)
                    PotholeOutputParser.parseChannelsFirstWithFiltering(output, filterGeometry = true)
                }
                TensorLayout.CHANNELS_LAST -> {
                    val anchors = outputShape?.getOrNull(1) ?: PotholeOutputParser.DEFAULT_ANCHORS
                    val output = Array(1) { Array(anchors) { FloatArray(PotholeOutputParser.EXPECTED_CHANNELS) } }
                    interp.run(inputBuffer, output)
                    PotholeOutputParser.parseChannelsLastWithFiltering(output, filterGeometry = true)
                }
            }

            var finalConfidence = rawMaxConfidence
            var rejectionReason: String? = null

            if (bestDetection != null && rawMaxConfidence >= VisionResult.CONFIDENCE_THRESHOLD) {
                // Check skin chrominance on the candidate box
                if (hasExcessiveSkin(letterboxedBitmap, bestDetection)) {
                    Log.i(TAG, "Detection box contains excessive skin chrominance (>25%); rejecting as human")
                    finalConfidence = 0.0f
                    rejectionReason = "Skin chrominance detected in detection box"
                }
            } else if (rawMaxConfidence < VisionResult.CONFIDENCE_THRESHOLD) {
                rejectionReason = "Low confidence ($rawMaxConfidence < ${VisionResult.CONFIDENCE_THRESHOLD})"
            }

            val inferenceMs = System.currentTimeMillis() - startTime
            val bestBoxArray = bestDetection?.let { floatArrayOf(it.cx, it.cy, it.w, it.h) }
            Log.d(TAG, "Inference completed in ${inferenceMs}ms: maxConfidence=$finalConfidence, reason=$rejectionReason")

            return VisionResult(
                maxConfidence = finalConfidence,
                error = null,
                inferenceMs = inferenceMs,
                rejectionReason = rejectionReason,
                bestBox = bestBoxArray
            )
        } catch (e: Exception) {
            val inferenceMs = System.currentTimeMillis() - startTime
            val errorMsg = "Inference execution failed: ${e.message}"
            Log.e(TAG, errorMsg, e)
            return VisionResult.error(errorMsg, inferenceMs)
        } finally {
            letterboxedBitmap?.recycle()
        }
    }

    private fun detectHumanFace(bitmap: Bitmap): Boolean {
        return try {
            val width = if (bitmap.width % 2 == 0) bitmap.width else bitmap.width - 1
            val height = bitmap.height
            val rgb565 = if (bitmap.config == Bitmap.Config.RGB_565 && bitmap.width == width) {
                bitmap
            } else {
                val b = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
                val canvas = android.graphics.Canvas(b)
                canvas.drawBitmap(bitmap, 0f, 0f, null)
                b
            }
            val detector = android.media.FaceDetector(width, height, 3)
            val faces = arrayOfNulls<android.media.FaceDetector.Face>(3)
            val numFaces = detector.findFaces(rgb565, faces)
            if (rgb565 !== bitmap) rgb565.recycle()
            numFaces > 0
        } catch (t: Throwable) {
            false
        }
    }

    private fun isSkinColor(r: Int, g: Int, b: Int): Boolean {
        val y = 0.299 * r + 0.587 * g + 0.114 * b
        val cr = (r - y) * 0.713 + 128
        val cb = (b - y) * 0.564 + 128
        return (cr in 133.0..173.0) && (cb in 77.0..127.0) && (r > g) && (r > b)
    }

    private fun hasExcessiveSkin(bitmap: Bitmap, box: PotholeDetection): Boolean {
        return try {
            val x1 = (box.x1).toInt().coerceIn(0, bitmap.width - 1)
            val y1 = (box.y1).toInt().coerceIn(0, bitmap.height - 1)
            val x2 = (box.x2).toInt().coerceIn(x1 + 1, bitmap.width)
            val y2 = (box.y2).toInt().coerceIn(y1 + 1, bitmap.height)
            val w = x2 - x1
            val h = y2 - y1
            if (w < 4 || h < 4) return false

            var skinCount = 0
            var sampleCount = 0
            val step = maxOf(1, (w * h) / 400)
            val pixels = IntArray(w * h)
            bitmap.getPixels(pixels, 0, w, x1, y1, w, h)

            for (i in pixels.indices step step) {
                val p = pixels[i]
                val r = (p shr 16) and 0xFF
                val g = (p shr 8) and 0xFF
                val b = p and 0xFF
                if (isSkinColor(r, g, b)) {
                    skinCount++
                }
                sampleCount++
            }
            if (sampleCount == 0) false else (skinCount.toFloat() / sampleCount) > 0.25f
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Backward-compatible convenience method returning a boolean presence flag.
     * Note: If model failed or was not loaded, returns false and logs error instead of silent mock true.
     */
    fun validatePothole(bitmap: Bitmap, rotationDegrees: Int = 0): Boolean {
        val result = analyze(bitmap, rotationDegrees)
        return result.isPothole
    }

    fun close() {
        try {
            interpreter?.close()
        } catch (e: Exception) {
            Log.w(TAG, "Error closing interpreter: ${e.message}")
        }
        interpreter = null
        status = AnalyzerStatus.FAILED
    }
}
