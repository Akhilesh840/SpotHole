package com.pothole.khadda.model

import com.pothole.khadda.data.local.Converters
import com.pothole.khadda.service.DetectionForegroundService
import org.junit.Assert.*
import org.junit.Test
import kotlin.math.abs

/**
 * Unit tests for CandidateEvent data structures, Room type converters, and selection math.
 */
class CandidateEventTest {

    private val converters = Converters()

    @Test
    fun testCandidateStatus_convertersRoundTrip() {
        for (status in CandidateStatus.values()) {
            val stringVal = converters.fromCandidateStatus(status)
            val parsed = converters.toCandidateStatus(stringVal)
            assertEquals(status, parsed)
        }
        // Unknown status fallback to PENDING
        assertEquals(CandidateStatus.PENDING, converters.toCandidateStatus("UNKNOWN_STATUS"))
    }

    @Test
    fun testFramePaths_convertersRoundTrip() {
        val paths = listOf("/path/to/frame0.jpg", "/path/to/frame1.jpg", "/path/to/frame2.jpg")
        val json = converters.fromStringList(paths)
        assertTrue(json.contains("frame0.jpg"))
        assertTrue(json.contains("frame1.jpg"))
        assertTrue(json.contains("frame2.jpg"))

        val restored = converters.toStringList(json)
        assertEquals(paths, restored)

        // Empty list handling
        val emptyRestored = converters.toStringList("")
        assertTrue(emptyRestored.isEmpty())
    }

    @Test
    fun testCandidateLookbackSelection_picksNearestAndNeighborsCappedAt3() {
        // Simulate frame timestamps at 200ms intervals (5 FPS)
        // t0 = 1000ms, t1 = 1200ms, t2 = 1400ms, t3 = 1600ms, t4 = 1800ms
        val timestamps = listOf(1000L, 1200L, 1400L, 1600L, 1800L)
        val targetTime = 1420L // Closest is index 2 (1400ms)

        var bestIdx = 0
        var minDiff = Long.MAX_VALUE
        for (i in timestamps.indices) {
            val diff = abs(timestamps[i] - targetTime)
            if (diff < minDiff) {
                minDiff = diff
                bestIdx = i
            }
        }
        assertEquals(2, bestIdx)

        val targetIndices = mutableListOf<Int>()
        if (bestIdx - 1 >= 0) targetIndices.add(bestIdx - 1)
        targetIndices.add(bestIdx)
        if (bestIdx + 1 < timestamps.size) targetIndices.add(bestIdx + 1)

        val selected = targetIndices.take(DetectionForegroundService.MAX_CANDIDATE_FRAMES)
        assertEquals(listOf(1, 2, 3), selected)
        assertEquals(3, selected.size)
        assertTrue(selected.size <= DetectionForegroundService.MAX_CANDIDATE_FRAMES)
    }

    @Test
    fun testCandidateLookbackSelection_atBoundary_handlesEdgeCases() {
        val timestamps = listOf(1000L, 1200L, 1400L)
        
        // Edge 1: Closest is first frame (index 0)
        val targetTimeStart = 900L
        var bestIdxStart = 0
        var minDiff = Long.MAX_VALUE
        for (i in timestamps.indices) {
            val diff = abs(timestamps[i] - targetTimeStart)
            if (diff < minDiff) {
                minDiff = diff
                bestIdxStart = i
            }
        }
        assertEquals(0, bestIdxStart)
        val indicesStart = mutableListOf<Int>()
        if (bestIdxStart - 1 >= 0) indicesStart.add(bestIdxStart - 1)
        indicesStart.add(bestIdxStart)
        if (bestIdxStart + 1 < timestamps.size) indicesStart.add(bestIdxStart + 1)
        assertEquals(listOf(0, 1), indicesStart)

        // Edge 2: Closest is last frame (index 2)
        val targetTimeEnd = 2000L
        var bestIdxEnd = 0
        minDiff = Long.MAX_VALUE
        for (i in timestamps.indices) {
            val diff = abs(timestamps[i] - targetTimeEnd)
            if (diff < minDiff) {
                minDiff = diff
                bestIdxEnd = i
            }
        }
        assertEquals(2, bestIdxEnd)
        val indicesEnd = mutableListOf<Int>()
        if (bestIdxEnd - 1 >= 0) indicesEnd.add(bestIdxEnd - 1)
        indicesEnd.add(bestIdxEnd)
        if (bestIdxEnd + 1 < timestamps.size) indicesEnd.add(bestIdxEnd + 1)
        assertEquals(listOf(1, 2), indicesEnd)
    }

    @Test
    fun testCandidateCap_constant() {
        assertEquals(300, DetectionForegroundService.MAX_CANDIDATES_PER_SESSION)
        assertEquals(5, DetectionForegroundService.MAX_CANDIDATE_FRAMES)
    }
}
