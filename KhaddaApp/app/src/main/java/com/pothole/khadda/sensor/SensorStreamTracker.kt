package com.pothole.khadda.sensor

import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import java.util.Locale

/**
 * Metric snapshot quantifying the health and integrity of the 100 Hz sensor stream.
 */
data class SensorStreamMetrics(
    val sampleCount: Long,
    val durationSeconds: Double,
    val meanHz: Double,
    val p95GapMs: Double,
    val maxGapMs: Double,
    val meanCallbackDurationMs: Double,
    val p99CallbackDurationMs: Double,
    val maxCallbackDurationMs: Double
) {
    fun toTelemetryString(): String {
        return String.format(
            Locale.US,
            """
AchievedSensorRateMeanHz: %.2f
SensorEventCount: %d
SessionDurationSeconds: %.2f
SensorGapP95Ms: %.2f
SensorGapMaxMs: %.2f
CallbackDurationMeanMs: %.3f
CallbackDurationP99Ms: %.3f
CallbackDurationMaxMs: %.3f
            """.trimIndent(),
            meanHz,
            sampleCount,
            durationSeconds,
            p95GapMs,
            maxGapMs,
            meanCallbackDurationMs,
            p99CallbackDurationMs,
            maxCallbackDurationMs
        )
    }
}

/**
 * Zero-allocation in-memory tracker for accelerometer event rate, event gaps,
 * and callback execution latency.
 */
class SensorStreamTracker {

    private var sampleCount: Long = 0L
    private var firstTimestampNs: Long = 0L
    private var lastTimestampNs: Long = 0L

    // Inter-sample gap tracking (in milliseconds)
    private var maxGapMs: Double = 0.0
    // Histogram 0 to 500 ms (1ms bucket resolution)
    private val gapHistogram = IntArray(501)
    private var gapCount: Long = 0L

    // Callback execution duration tracking (in milliseconds)
    private var totalCallbackDurationNs: Long = 0L
    private var maxCallbackDurationMs: Double = 0.0
    // Histogram 0 to 50 ms (0.1ms bucket resolution, 501 buckets)
    private val durationHistogram = IntArray(501)
    private var durationCount: Long = 0L

    fun reset() {
        sampleCount = 0L
        firstTimestampNs = 0L
        lastTimestampNs = 0L
        maxGapMs = 0.0
        gapHistogram.fill(0)
        gapCount = 0L
        totalCallbackDurationNs = 0L
        maxCallbackDurationMs = 0.0
        durationHistogram.fill(0)
        durationCount = 0L
    }

    /**
     * Records a sensor arrival and computes inter-sample gap.
     */
    fun recordSampleArrival(timestampNs: Long) {
        sampleCount++
        if (firstTimestampNs == 0L) {
            firstTimestampNs = timestampNs
            lastTimestampNs = timestampNs
            return
        }

        val gapNs = timestampNs - lastTimestampNs
        lastTimestampNs = timestampNs

        if (gapNs > 0) {
            val gapMs = gapNs / 1_000_000.0
            if (gapMs > maxGapMs) {
                maxGapMs = gapMs
            }
            val bucket = min(500, max(0, gapMs.roundToInt()))
            gapHistogram[bucket]++
            gapCount++
        }
    }

    /**
     * Records the duration taken to execute the sensor callback.
     */
    fun recordCallbackDuration(durationNs: Long) {
        totalCallbackDurationNs += durationNs
        val durationMs = durationNs / 1_000_000.0
        if (durationMs > maxCallbackDurationMs) {
            maxCallbackDurationMs = durationMs
        }
        // Bucket in 0.1ms steps (e.g. 1.2ms -> bucket 12)
        val bucket = min(500, max(0, (durationMs * 10.0).roundToInt()))
        durationHistogram[bucket]++
        durationCount++
    }

    fun getMetrics(): SensorStreamMetrics {
        val durationSeconds = if (lastTimestampNs > firstTimestampNs) {
            (lastTimestampNs - firstTimestampNs) / 1_000_000_000.0
        } else {
            0.0
        }

        val meanHz = if (durationSeconds > 0.0 && sampleCount > 1) {
            (sampleCount - 1) / durationSeconds
        } else {
            0.0
        }

        // Compute 95th percentile gap
        val p95GapMs = calculatePercentileFromHistogram(gapHistogram, gapCount, 0.95, 1.0)

        // Compute mean callback duration
        val meanDurationMs = if (durationCount > 0) {
            (totalCallbackDurationNs / durationCount.toDouble()) / 1_000_000.0
        } else {
            0.0
        }

        // Compute 99th percentile callback duration (scaled by 0.1ms per bucket)
        val p99DurationMs = calculatePercentileFromHistogram(durationHistogram, durationCount, 0.99, 0.1)

        return SensorStreamMetrics(
            sampleCount = sampleCount,
            durationSeconds = durationSeconds,
            meanHz = meanHz,
            p95GapMs = p95GapMs,
            maxGapMs = maxGapMs,
            meanCallbackDurationMs = meanDurationMs,
            p99CallbackDurationMs = p99DurationMs,
            maxCallbackDurationMs = maxCallbackDurationMs
        )
    }

    private fun calculatePercentileFromHistogram(
        histogram: IntArray,
        totalItems: Long,
        percentile: Double,
        scaleFactor: Double
    ): Double {
        if (totalItems <= 0) return 0.0
        val targetRank = (totalItems * percentile).toLong()
        var accumulated = 0L
        for (i in histogram.indices) {
            accumulated += histogram[i]
            if (accumulated >= targetRank) {
                return i * scaleFactor
            }
        }
        return (histogram.size - 1) * scaleFactor
    }
}
