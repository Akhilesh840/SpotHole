package com.pothole.khadda.detector

/**
 * Result of vision model inference on a camera frame.
 *
 * @property maxConfidence Maximum pothole confidence score across all anchors [0.0, 1.0].
 * @property error Error message if model load or inference failed; null if successful.
 * @property inferenceMs Wall-clock duration of preprocessing and inference in milliseconds.
 */
data class VisionResult(
    val maxConfidence: Float,
    val error: String? = null,
    val inferenceMs: Long = 0L,
    val rejectionReason: String? = null,
    val bestBox: FloatArray? = null
) {
    /**
     * A pothole is present when there are no errors, no rejection reason, and maxConfidence meets or exceeds threshold.
     */
    val isPothole: Boolean get() = error == null && rejectionReason == null && maxConfidence >= CONFIDENCE_THRESHOLD

    companion object {
        /**
         * Confidence threshold for pothole detection.
         */
        const val CONFIDENCE_THRESHOLD = 0.50f

        fun error(message: String, inferenceMs: Long = 0L): VisionResult {
            return VisionResult(
                maxConfidence = 0.0f,
                error = message,
                inferenceMs = inferenceMs,
                rejectionReason = message
            )
        }
    }
}
