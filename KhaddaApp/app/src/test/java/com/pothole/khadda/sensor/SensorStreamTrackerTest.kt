package com.pothole.khadda.sensor

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for SensorStreamTracker: event rate math, inter-sample gap percentiles,
 * callback duration percentiles, and telemetry formatting.
 */
class SensorStreamTrackerTest {

    private lateinit var tracker: SensorStreamTracker

    @Before
    fun setUp() {
        tracker = SensorStreamTracker()
    }

    @Test
    fun testIdealStreamRate_100Hz() {
        var currentNs = 1_000_000_000L
        val intervalNs = 10_000_000L // 10ms = 100 Hz

        // Feed 101 samples (100 intervals = 1.0 second)
        for (i in 0..100) {
            tracker.recordSampleArrival(currentNs)
            tracker.recordCallbackDuration(200_000L) // 0.2ms callback
            currentNs += intervalNs
        }

        val metrics = tracker.getMetrics()
        assertEquals(101L, metrics.sampleCount)
        assertEquals(1.0, metrics.durationSeconds, 0.001)
        assertEquals(100.0, metrics.meanHz, 0.1)
        assertEquals(10.0, metrics.maxGapMs, 0.1)
        assertEquals(10.0, metrics.p95GapMs, 1.0)
        assertEquals(0.2, metrics.meanCallbackDurationMs, 0.05)
        assertEquals(0.2, metrics.maxCallbackDurationMs, 0.05)
    }

    @Test
    fun testGapOutliersAndPercentiles() {
        var currentNs = 1_000_000_000L

        // Record 1st sample
        tracker.recordSampleArrival(currentNs)

        // 94 intervals of 10ms
        for (i in 1..94) {
            currentNs += 10_000_000L
            tracker.recordSampleArrival(currentNs)
        }

        // 5 intervals of 30ms
        for (i in 1..5) {
            currentNs += 30_000_000L
            tracker.recordSampleArrival(currentNs)
        }

        // 1 interval of 120ms
        currentNs += 120_000_000L
        tracker.recordSampleArrival(currentNs)

        val metrics = tracker.getMetrics()
        assertEquals(101L, metrics.sampleCount)
        assertEquals(120.0, metrics.maxGapMs, 0.1)
        // 95th percentile of 100 intervals should be at or near 30ms
        assertTrue("p95 gap should be between 10ms and 35ms, got ${metrics.p95GapMs}", metrics.p95GapMs in 10.0..35.0)
    }

    @Test
    fun testCallbackDurationPercentiles() {
        // Feed 100 callback durations:
        // 98 samples at 0.4ms (400,000 ns)
        for (i in 1..98) {
            tracker.recordCallbackDuration(400_000L)
        }
        // 1 sample at 1.8ms (1,800,000 ns)
        tracker.recordCallbackDuration(1_800_000L)
        // 1 sample at 4.5ms (4,500,000 ns)
        tracker.recordCallbackDuration(4_500_000L)

        val metrics = tracker.getMetrics()
        assertEquals(4.5, metrics.maxCallbackDurationMs, 0.05)
        // p99 percentile of 100 values should be at least 1.8ms
        assertTrue("p99 callback duration should be >= 1.5ms, got ${metrics.p99CallbackDurationMs}", metrics.p99CallbackDurationMs >= 1.5)
        assertTrue("mean callback duration should be < 1.0ms, got ${metrics.meanCallbackDurationMs}", metrics.meanCallbackDurationMs < 1.0)
    }

    @Test
    fun testResetClearsAllMetrics() {
        tracker.recordSampleArrival(1_000_000_000L)
        tracker.recordSampleArrival(1_010_000_000L)
        tracker.recordCallbackDuration(500_000L)

        var metrics = tracker.getMetrics()
        assertEquals(2L, metrics.sampleCount)

        tracker.reset()
        metrics = tracker.getMetrics()
        assertEquals(0L, metrics.sampleCount)
        assertEquals(0.0, metrics.durationSeconds, 0.001)
        assertEquals(0.0, metrics.meanHz, 0.001)
        assertEquals(0.0, metrics.maxGapMs, 0.001)
        assertEquals(0.0, metrics.p95GapMs, 0.001)
        assertEquals(0.0, metrics.meanCallbackDurationMs, 0.001)
    }

    @Test
    fun testTelemetryStringFormatting() {
        var currentNs = 1_000_000_000L
        for (i in 0..10) {
            tracker.recordSampleArrival(currentNs)
            tracker.recordCallbackDuration(300_000L)
            currentNs += 10_000_000L
        }

        val text = tracker.getMetrics().toTelemetryString()
        assertTrue(text.contains("AchievedSensorRateMeanHz:"))
        assertTrue(text.contains("SensorEventCount: 11"))
        assertTrue(text.contains("SessionDurationSeconds:"))
        assertTrue(text.contains("SensorGapP95Ms:"))
        assertTrue(text.contains("SensorGapMaxMs:"))
        assertTrue(text.contains("CallbackDurationMeanMs:"))
        assertTrue(text.contains("CallbackDurationP99Ms:"))
        assertTrue(text.contains("CallbackDurationMaxMs:"))
    }
}
