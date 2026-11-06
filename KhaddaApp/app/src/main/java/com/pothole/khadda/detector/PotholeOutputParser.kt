package com.pothole.khadda.detector

/**
 * Tensor layout for YOLO one-class detection models.
 */
enum class TensorLayout {
    /**
     * Channels first: shape [1, 5, 8400]
     * Index 0..3: cx, cy, w, h; Index 4: class confidence score
     */
    CHANNELS_FIRST,

    /**
     * Channels last: shape [1, 8400, 5]
     * Index 0..3: cx, cy, w, h; Index 4: class confidence score
     */
    CHANNELS_LAST
}

data class PotholeDetection(
    val cx: Float,
    val cy: Float,
    val w: Float,
    val h: Float,
    val confidence: Float
) {
    val x1: Float get() = cx - w / 2f
    val y1: Float get() = cy - h / 2f
    val x2: Float get() = cx + w / 2f
    val y2: Float get() = cy + h / 2f
    val aspectRatio: Float get() = w / maxOf(1.0f, h)
}

/**
 * Parser for YOLOv8 single-class pothole detection outputs.
 */
object PotholeOutputParser {

    const val EXPECTED_CHANNELS = 5
    const val DEFAULT_ANCHORS = 8400
    const val CLASS_INDEX = 4

    const val MIN_ROAD_CY_RATIO = 0.40f // Road ROI: lower 60% of image (cy >= 256 for 640x640)
    const val MIN_ASPECT_RATIO = 0.45f  // Standing human body has w/h < 0.45
    const val MIN_BOX_DIMENSION = 32f   // Discard noise and gravel smaller than 32x32 px

    fun isValidPotholeBox(cx: Float, cy: Float, w: Float, h: Float, frameSize: Int = 640): Boolean {
        // 1. Must be within the road ROI (lower 60% of frame)
        if (cy < frameSize * MIN_ROAD_CY_RATIO) return false
        // 2. Aspect ratio: potholes are horizontal/elliptical depressions, not tall vertical standing strips
        if (w / maxOf(1.0f, h) < MIN_ASPECT_RATIO) return false
        // 3. Minimum dimensions
        if (w < MIN_BOX_DIMENSION || h < MIN_BOX_DIMENSION) return false
        // 4. Must not cover entire frame (> 85% of image width and height)
        if (w > frameSize * 0.85f && h > frameSize * 0.85f) return false
        return true
    }

    /**
     * Validates output tensor shape and detects whether it is [1, 5, 8400] or [1, 8400, 5].
     * Returns null if the shape is unsupported (e.g. segmentation [1, 37, 8400], etc.).
     */
    fun detectLayout(shape: IntArray?): TensorLayout? {
        if (shape == null || shape.size != 3 || shape[0] != 1) {
            return null
        }

        return when {
            shape[1] == EXPECTED_CHANNELS && shape[2] == DEFAULT_ANCHORS -> TensorLayout.CHANNELS_FIRST
            shape[1] == DEFAULT_ANCHORS && shape[2] == EXPECTED_CHANNELS -> TensorLayout.CHANNELS_LAST
            // Also allow flexible anchor counts if channels == 5
            shape[1] == EXPECTED_CHANNELS && shape[2] > 0 -> TensorLayout.CHANNELS_FIRST
            shape[1] > 0 && shape[2] == EXPECTED_CHANNELS -> TensorLayout.CHANNELS_LAST
            else -> null
        }
    }

    /**
     * Extracts maximum confidence from channels-first array [1][5][N] with geometric validation.
     * @param output 3D float array with shape [1][5][numAnchors]
     * @param filterGeometry whether to reject boxes with non-road or vertical human aspect ratios
     * @return Pair of max confidence and best valid PotholeDetection
     */
    fun parseChannelsFirstWithFiltering(
        output: Array<Array<FloatArray>>,
        filterGeometry: Boolean = true
    ): Pair<Float, PotholeDetection?> {
        require(output.isNotEmpty() && output[0].size > CLASS_INDEX) {
            "Invalid channels-first tensor dimensions: expected at least ${CLASS_INDEX + 1} channels"
        }
        var maxConfidence = 0.0f
        var bestDetection: PotholeDetection? = null

        val cxArr = output[0][0]
        val cyArr = output[0][1]
        val wArr = output[0][2]
        val hArr = output[0][3]
        val confidences = output[0][CLASS_INDEX]

        for (i in confidences.indices) {
            val conf = confidences[i]
            if (conf > maxConfidence) {
                val cx = cxArr[i]
                val cy = cyArr[i]
                val w = wArr[i]
                val h = hArr[i]
                if (!filterGeometry || isValidPotholeBox(cx, cy, w, h)) {
                    maxConfidence = conf
                    bestDetection = PotholeDetection(cx, cy, w, h, conf)
                }
            }
        }
        return Pair(maxConfidence, bestDetection)
    }

    fun parseChannelsFirst(output: Array<Array<FloatArray>>): Float {
        return parseChannelsFirstWithFiltering(output, filterGeometry = true).first
    }

    /**
     * Extracts maximum confidence from channels-last array [1][N][5] with geometric validation.
     * @param output 3D float array with shape [1][numAnchors][5]
     * @param filterGeometry whether to reject boxes with non-road or vertical human aspect ratios
     * @return Pair of max confidence and best valid PotholeDetection
     */
    fun parseChannelsLastWithFiltering(
        output: Array<Array<FloatArray>>,
        filterGeometry: Boolean = true
    ): Pair<Float, PotholeDetection?> {
        require(output.isNotEmpty() && output[0].isNotEmpty() && output[0][0].size > CLASS_INDEX) {
            "Invalid channels-last tensor dimensions: expected at least ${CLASS_INDEX + 1} elements per anchor"
        }
        var maxConfidence = 0.0f
        var bestDetection: PotholeDetection? = null

        val anchors = output[0]
        for (i in anchors.indices) {
            val anchor = anchors[i]
            val conf = anchor[CLASS_INDEX]
            if (conf > maxConfidence) {
                val cx = anchor[0]
                val cy = anchor[1]
                val w = anchor[2]
                val h = anchor[3]
                if (!filterGeometry || isValidPotholeBox(cx, cy, w, h)) {
                    maxConfidence = conf
                    bestDetection = PotholeDetection(cx, cy, w, h, conf)
                }
            }
        }
        return Pair(maxConfidence, bestDetection)
    }

    fun parseChannelsLast(output: Array<Array<FloatArray>>): Float {
        return parseChannelsLastWithFiltering(output, filterGeometry = true).first
    }
}
