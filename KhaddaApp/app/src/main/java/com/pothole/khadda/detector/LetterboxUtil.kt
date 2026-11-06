package com.pothole.khadda.detector

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import kotlin.math.roundToInt

/**
 * Geometric letterbox and rotation parameters.
 */
data class LetterboxParams(
    val scale: Float,
    val scaledWidth: Int,
    val scaledHeight: Int,
    val padLeft: Float,
    val padTop: Float,
    val padRight: Float,
    val padBottom: Float,
    val targetWidth: Int = 640,
    val targetHeight: Int = 640
)

/**
 * Utilities for YOLOv8 letterbox preprocessing (aspect-ratio preserving resize with gray padding 114)
 * and camera sensor rotation normalization.
 */
object LetterboxUtil {

    const val DEFAULT_TARGET_SIZE = 640
    const val DEFAULT_PADDING_GRAY = 114 // Gray value 114 (YOLO standard)

    /**
     * Calculates letterbox scale and centered padding dimensions without depending on Android UI classes.
     */
    fun calculateLetterbox(
        srcWidth: Int,
        srcHeight: Int,
        targetWidth: Int = DEFAULT_TARGET_SIZE,
        targetHeight: Int = DEFAULT_TARGET_SIZE
    ): LetterboxParams {
        require(srcWidth > 0 && srcHeight > 0) { "Source dimensions must be positive ($srcWidth x $srcHeight)" }
        require(targetWidth > 0 && targetHeight > 0) { "Target dimensions must be positive ($targetWidth x $targetHeight)" }

        val scale = minOf(targetWidth.toFloat() / srcWidth, targetHeight.toFloat() / srcHeight)
        val scaledW = (srcWidth * scale).roundToInt()
        val scaledH = (srcHeight * scale).roundToInt()

        val padW = (targetWidth - scaledW).toFloat()
        val padH = (targetHeight - scaledH).toFloat()

        val padLeft = padW / 2f
        val padRight = padW - padLeft
        val padTop = padH / 2f
        val padBottom = padH - padTop

        return LetterboxParams(
            scale = scale,
            scaledWidth = scaledW,
            scaledHeight = scaledH,
            padLeft = padLeft,
            padTop = padTop,
            padRight = padRight,
            padBottom = padBottom,
            targetWidth = targetWidth,
            targetHeight = targetHeight
        )
    }

    /**
     * Calculates the effective image dimensions after applying rotationDegrees (0, 90, 180, 270).
     */
    fun getRotatedDimensions(width: Int, height: Int, rotationDegrees: Int): Pair<Int, Int> {
        val normalized = ((rotationDegrees % 360) + 360) % 360
        return if (normalized == 90 || normalized == 270) {
            Pair(height, width)
        } else {
            Pair(width, height)
        }
    }

    /**
     * Applies rotationDegrees so the image is upright, then letterboxes to targetSize x targetSize
     * with the specified gray padding color (default 114).
     */
    fun createLetterboxedBitmap(
        src: Bitmap,
        rotationDegrees: Int = 0,
        targetSize: Int = DEFAULT_TARGET_SIZE,
        paddingValue: Int = DEFAULT_PADDING_GRAY
    ): Bitmap {
        val normalizedRotation = ((rotationDegrees % 360) + 360) % 360
        val rotatedBitmap = if (normalizedRotation != 0) {
            val matrix = Matrix().apply { postRotate(normalizedRotation.toFloat()) }
            Bitmap.createBitmap(src, 0, 0, src.width, src.height, matrix, true)
        } else {
            src
        }

        val params = calculateLetterbox(rotatedBitmap.width, rotatedBitmap.height, targetSize, targetSize)
        val output = Bitmap.createBitmap(targetSize, targetSize, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)

        // Fill with gray padding 114 (0xFF727272)
        val grayColor = (0xFF shl 24) or (paddingValue shl 16) or (paddingValue shl 8) or paddingValue
        canvas.drawColor(grayColor)

        val destRect = RectF(
            params.padLeft,
            params.padTop,
            params.padLeft + params.scaledWidth,
            params.padTop + params.scaledHeight
        )
        val paint = Paint(Paint.FILTER_BITMAP_FLAG)
        canvas.drawBitmap(rotatedBitmap, null, destRect, paint)

        // Clean up intermediate rotated bitmap if newly allocated
        if (rotatedBitmap !== src) {
            rotatedBitmap.recycle()
        }

        return output
    }
}
