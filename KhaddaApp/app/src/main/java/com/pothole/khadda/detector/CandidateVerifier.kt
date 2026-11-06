package com.pothole.khadda.detector

import android.content.Context
import android.graphics.BitmapFactory
import android.util.Log
import com.pothole.khadda.data.repository.PotholeRepository
import com.pothole.khadda.model.CandidateEvent
import com.pothole.khadda.model.CandidateStatus
import com.pothole.khadda.model.PotholeEvent
import com.pothole.khadda.model.RepairStatus
import java.io.File
import java.util.UUID

data class VerificationSummary(
    val totalProcessed: Int,
    val confirmedCount: Int,
    val rejectedCount: Int,
    val errorCount: Int,
    val avgInferenceMs: Long
)

/**
 * Verification engine for post-ride batch validation of CandidateEvents using TFLite.
 */
object CandidateVerifier {

    private const val TAG = "CandidateVerifier"

    /**
     * Constant governing whether to delete image files of rejected candidates.
     * Default is false: retained for retraining and debugging.
     */
    const val DELETE_REJECTED_IMAGES: Boolean = false

    /**
     * Minimum number of frames that must detect the pothole above threshold
     * for a multi-frame candidate event to be confirmed.
     */
    const val MIN_CONFIRMED_FRAMES: Int = 2

    /**
     * Evaluates candidate status across multi-frame detections.
     * Prevents single-frame noise spikes (motion blur, glare, temporary shadow) from confirming.
     */
    fun evaluateMultiFrameStatus(frameConfidences: List<Float>, error: String?): CandidateStatus {
        if (error != null) return CandidateStatus.ERROR
        if (frameConfidences.isEmpty()) return CandidateStatus.REJECTED

        val passedFrames = frameConfidences.count { it >= VisionResult.CONFIDENCE_THRESHOLD }
        val isConfirmed = if (frameConfidences.size == 1) {
            passedFrames >= 1
        } else {
            // Require at least MIN_CONFIRMED_FRAMES to meet the threshold,
            // OR if top-2 average meets threshold with high top score
            val sorted = frameConfidences.sortedDescending()
            val top2Avg = if (sorted.size >= 2) (sorted[0] + sorted[1]) / 2f else sorted[0]
            passedFrames >= MIN_CONFIRMED_FRAMES || (top2Avg >= VisionResult.CONFIDENCE_THRESHOLD && sorted[0] >= VisionResult.CONFIDENCE_THRESHOLD + 0.10f)
        }

        return if (isConfirmed) CandidateStatus.CONFIRMED else CandidateStatus.REJECTED
    }

    /**
     * Backward-compatible status evaluation from single max confidence.
     */
    fun evaluateStatus(maxConfidence: Float, error: String?): CandidateStatus {
        return evaluateMultiFrameStatus(listOf(maxConfidence), error)
    }

    /**
     * Verifies a single candidate across ALL its recorded frames without early exit.
     * Updates candidate status in Room and promotes confirmed candidates to PotholeEvent.
     */
    suspend fun verifyCandidate(
        context: Context,
        candidate: CandidateEvent,
        analyzer: PotholeVisionAnalyzer,
        repository: PotholeRepository
    ): CandidateStatus {
        var maxConfidence = 0.0f
        var totalInferenceMs = 0L
        var framesAnalyzed = 0
        var analyzerError: String? = null
        var bestFramePath: String? = null
        val frameConfidences = mutableListOf<Float>()

        // Analyze ALL frames of each candidate (no early exit)
        for (path in candidate.framePaths) {
            val file = File(path)
            if (!file.exists()) {
                Log.w(TAG, "Candidate frame missing: $path")
                continue
            }

            val bitmap = BitmapFactory.decodeFile(file.absolutePath)
            if (bitmap == null) {
                Log.w(TAG, "Failed to decode candidate frame: $path")
                continue
            }

            val result = analyzer.analyze(bitmap, rotationDegrees = 0)
            bitmap.recycle() // Release decoded bitmap immediately

            if (result.error != null) {
                analyzerError = result.error
            } else {
                frameConfidences.add(result.maxConfidence)
                if (result.maxConfidence > maxConfidence) {
                    maxConfidence = result.maxConfidence
                    bestFramePath = path
                }
                totalInferenceMs += result.inferenceMs
                framesAnalyzed++
            }
        }

        val avgInferenceMs = if (framesAnalyzed > 0) totalInferenceMs / framesAnalyzed else 0L
        val status = evaluateMultiFrameStatus(frameConfidences, analyzerError)

        // Update candidate row in Room
        repository.updateCandidateStatus(candidate.id, status, maxConfidence, avgInferenceMs)

        // If CONFIRMED, insert into existing PotholeEvent table with reference to candidateId
        if (status == CandidateStatus.CONFIRMED) {
            val eventId = UUID.randomUUID().toString()
            val event = PotholeEvent(
                eventId = eventId,
                sessionId = candidate.sessionId,
                timestamp = candidate.timestampMs,
                latitude = candidate.latitude,
                longitude = candidate.longitude,
                zDiffValue = candidate.impact,
                severity = candidate.severity,
                algorithm = "HYBRID_VISION",
                status = RepairStatus.REPORTED,
                syncStatus = "QUEUED",
                candidateId = candidate.id
            )
            repository.insertEvent(event)

            // Link best frame image to pothole_${eventId}.jpg so map and history galleries display it
            val frameToCopy = bestFramePath ?: candidate.framePaths.firstOrNull()
            if (frameToCopy != null) {
                val src = File(frameToCopy)
                val dest = File(context.filesDir, "pothole_${event.eventId}.jpg")
                if (src.exists()) {
                    try {
                        src.copyTo(dest, overwrite = true)
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to copy confirmed image: ${e.message}")
                    }
                }
            }
            Log.i(TAG, "Promoted candidate ${candidate.id} to PotholeEvent $eventId (confidence=$maxConfidence)")
        } else if (status == CandidateStatus.REJECTED) {
            val frameToCopy = bestFramePath ?: candidate.framePaths.firstOrNull()
            if (frameToCopy != null) {
                val src = File(frameToCopy)
                val dest = File(context.filesDir, "rejected_pothole_${candidate.id}.jpg")
                if (src.exists()) {
                    try {
                        src.copyTo(dest, overwrite = true)
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to copy rejected image: ${e.message}")
                    }
                }
            }
            Log.i(TAG, "Candidate ${candidate.id} REJECTED (maxConfidence=$maxConfidence)")
            if (DELETE_REJECTED_IMAGES) {
                for (path in candidate.framePaths) {
                    try { File(path).delete() } catch (e: Exception) {}
                }
            }
        } else {
            Log.e(TAG, "Candidate ${candidate.id} ERROR: $analyzerError")
        }

        return status
    }
}
