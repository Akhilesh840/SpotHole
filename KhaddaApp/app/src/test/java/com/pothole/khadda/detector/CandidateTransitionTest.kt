package com.pothole.khadda.detector

import com.pothole.khadda.model.CandidateEvent
import com.pothole.khadda.model.CandidateStatus
import com.pothole.khadda.model.SeverityLevel
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests validating candidate lifecycle status transitions
 * (PENDING -> CONFIRMED, REJECTED, ERROR) and confidence thresholding.
 */
class CandidateTransitionTest {

    private fun createSamplePendingCandidate(): CandidateEvent {
        return CandidateEvent(
            sessionId = "session_123",
            timestampMs = 1700000000000L,
            latitude = 18.5204,
            longitude = 73.8567,
            speedMps = 5.5f,
            triggeredZDiff = true,
            triggeredZThresh = true,
            triggeredStdev = false,
            triggeredGZero = false,
            zDiff = 6.2,
            zThresh = 4.1,
            stdev = 2.0,
            impact = 6.2,
            severity = SeverityLevel.MEDIUM
        )
    }

    @Test
    fun testInitialCandidateStatus_isPending() {
        val candidate = createSamplePendingCandidate()
        assertEquals(CandidateStatus.PENDING, candidate.status)
        assertEquals(0.0f, candidate.maxConfidence, 0.001f)
    }

    @Test
    fun testTransition_pendingToConfirmed_whenConfidenceAboveOrEqualThreshold() {
        val candidate = createSamplePendingCandidate()

        // Threshold boundary test (0.50f exactly)
        val statusExact = CandidateVerifier.evaluateStatus(0.50f, null)
        assertEquals(CandidateStatus.CONFIRMED, statusExact)

        // Higher confidence
        val statusHigh = CandidateVerifier.evaluateStatus(0.78f, null)
        assertEquals(CandidateStatus.CONFIRMED, statusHigh)

        candidate.status = statusHigh
        candidate.maxConfidence = 0.78f
        assertEquals(CandidateStatus.CONFIRMED, candidate.status)
    }

    @Test
    fun testTransition_pendingToRejected_whenConfidenceBelowThreshold() {
        val candidate = createSamplePendingCandidate()

        // Below threshold (0.499f)
        val statusJustBelow = CandidateVerifier.evaluateStatus(0.499f, null)
        assertEquals(CandidateStatus.REJECTED, statusJustBelow)

        // Low confidence
        val statusLow = CandidateVerifier.evaluateStatus(0.05f, null)
        assertEquals(CandidateStatus.REJECTED, statusLow)

        candidate.status = statusLow
        candidate.maxConfidence = 0.05f
        assertEquals(CandidateStatus.REJECTED, candidate.status)
    }

    @Test
    fun testTransition_pendingToError_whenAnalyzerFails() {
        val candidate = createSamplePendingCandidate()

        // Shape mismatch or model error must set ERROR even if confidence was high
        val statusWithError = CandidateVerifier.evaluateStatus(0.95f, "Unsupported output shape [1, 37, 8400]")
        assertEquals(CandidateStatus.ERROR, statusWithError)

        candidate.status = statusWithError
        assertEquals(CandidateStatus.ERROR, candidate.status)
    }

    @Test
    fun testMultiFrameConsensus_requiresMultipleFramesAboveThreshold() {
        // 5 candidate frames: only 1 frame has a high spike (noise / motion blur) -> REJECTED
        val singleSpike = listOf(0.12f, 0.58f, 0.18f, 0.10f, 0.08f)
        assertEquals(CandidateStatus.REJECTED, CandidateVerifier.evaluateMultiFrameStatus(singleSpike, null))

        // 5 candidate frames: 2 frames above threshold (persisted across movement) -> CONFIRMED
        val persistentPothole = listOf(0.15f, 0.56f, 0.62f, 0.40f, 0.10f)
        assertEquals(CandidateStatus.CONFIRMED, CandidateVerifier.evaluateMultiFrameStatus(persistentPothole, null))

        // 5 candidate frames: all low confidence -> REJECTED
        val allLow = listOf(0.08f, 0.22f, 0.15f, 0.30f, 0.18f)
        assertEquals(CandidateStatus.REJECTED, CandidateVerifier.evaluateMultiFrameStatus(allLow, null))

        // Single-frame candidate (simulations or legacy single capture): passes if >= 0.50f
        val singleFramePass = listOf(0.65f)
        assertEquals(CandidateStatus.CONFIRMED, CandidateVerifier.evaluateMultiFrameStatus(singleFramePass, null))
    }

    @Test
    fun testMultiFrameMaxConfidence_simulation() {
        // Simulating frame scanning across 3 candidate frames
        val frameConfidencesPass = listOf(0.12f, 0.65f, 0.20f)
        var maxConfPass = 0.0f
        for (conf in frameConfidencesPass) {
            if (conf > maxConfPass) maxConfPass = conf
        }
        assertEquals(0.65f, maxConfPass, 0.0001f)
        assertEquals(CandidateStatus.CONFIRMED, CandidateVerifier.evaluateStatus(maxConfPass, null))

        val frameConfidencesFail = listOf(0.08f, 0.22f, 0.15f)
        var maxConfFail = 0.0f
        for (conf in frameConfidencesFail) {
            if (conf > maxConfFail) maxConfFail = conf
        }
        assertEquals(0.22f, maxConfFail, 0.0001f)
        assertEquals(CandidateStatus.REJECTED, CandidateVerifier.evaluateStatus(maxConfFail, null))
    }

    @Test
    fun testDeleteRejectedImages_defaultConstant() {
        // Constant must default to false so images are kept for debugging/retraining
        assertFalse(CandidateVerifier.DELETE_REJECTED_IMAGES)
    }
}
