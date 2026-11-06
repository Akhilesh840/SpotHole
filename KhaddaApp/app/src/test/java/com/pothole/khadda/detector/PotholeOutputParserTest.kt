package com.pothole.khadda.detector

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for Pothole output tensor parsing, geometric filtering, and confidence evaluation.
 */
class PotholeOutputParserTest {

    @Test
    fun testDetectLayout_identifiesChannelsFirst() {
        val shape = intArrayOf(1, 5, 8400)
        val layout = PotholeOutputParser.detectLayout(shape)
        assertEquals(TensorLayout.CHANNELS_FIRST, layout)
    }

    @Test
    fun testDetectLayout_identifiesChannelsLast() {
        val shape = intArrayOf(1, 8400, 5)
        val layout = PotholeOutputParser.detectLayout(shape)
        assertEquals(TensorLayout.CHANNELS_LAST, layout)
    }

    @Test
    fun testDetectLayout_rejectsUnsupportedShapes() {
        // Segmentation shape
        assertNull(PotholeOutputParser.detectLayout(intArrayOf(1, 37, 8400)))
        assertNull(PotholeOutputParser.detectLayout(intArrayOf(1, 32, 160, 160)))
        // 2D shapes or wrong channel counts
        assertNull(PotholeOutputParser.detectLayout(intArrayOf(1, 10)))
        assertNull(PotholeOutputParser.detectLayout(intArrayOf(1, 6, 8400)))
        assertNull(PotholeOutputParser.detectLayout(null))
    }

    @Test
    fun testParseChannelsFirst_extractsMaxConfidence() {
        val numAnchors = 10
        // Shape [1, 5, 10]
        val output = Array(1) {
            Array(5) {
                FloatArray(numAnchors)
            }
        }

        // Fill valid road pothole coordinates: cx=320, cy=350, w=80, h=40
        for (i in 0 until numAnchors) {
            output[0][0][i] = 320.0f
            output[0][1][i] = 350.0f
            output[0][2][i] = 80.0f
            output[0][3][i] = 40.0f
        }

        // Fill confidence scores at channel index 4
        output[0][4][0] = 0.05f
        output[0][4][3] = 0.78f // Max
        output[0][4][5] = 0.42f
        output[0][4][9] = 0.15f

        val maxConf = PotholeOutputParser.parseChannelsFirst(output)
        assertEquals(0.78f, maxConf, 0.0001f)
    }

    @Test
    fun testParseChannelsLast_extractsMaxConfidence() {
        val numAnchors = 10
        // Shape [1, 10, 5]
        val output = Array(1) {
            Array(numAnchors) {
                FloatArray(5)
            }
        }

        // Fill valid road pothole coordinates
        for (i in 0 until numAnchors) {
            output[0][i][0] = 320.0f
            output[0][i][1] = 350.0f
            output[0][i][2] = 80.0f
            output[0][i][3] = 40.0f
        }

        // Fill confidence scores at column index 4
        output[0][0][4] = 0.02f
        output[0][4][4] = 0.33f
        output[0][7][4] = 0.91f // Max
        output[0][9][4] = 0.65f

        val maxConf = PotholeOutputParser.parseChannelsLast(output)
        assertEquals(0.91f, maxConf, 0.0001f)
    }

    @Test
    fun testGeometricFiltering_rejectsHumanSilhouettesAndNonRoad() {
        // 1. Standing human: tall narrow vertical box (w=40, h=160 -> aspect ratio 0.25)
        assertFalse(
            "Tall vertical box typical of a standing human body should be rejected",
            PotholeOutputParser.isValidPotholeBox(cx = 320f, cy = 350f, w = 40f, h = 160f)
        )

        // 2. Upper frame (sky / ceiling / horizon level): cy = 180 (< 640 * 0.40 = 256)
        assertFalse(
            "Detection in upper 40% of frame (sky / horizon / oncoming vehicles) should be rejected",
            PotholeOutputParser.isValidPotholeBox(cx = 320f, cy = 180f, w = 100f, h = 50f)
        )

        // 3. Tiny noise/gravel speckles (< 32px dimension)
        assertFalse(
            "Tiny speckle noise smaller than 32px should be rejected",
            PotholeOutputParser.isValidPotholeBox(cx = 320f, cy = 400f, w = 24f, h = 24f)
        )

        // 4. Real horizontal pothole on road: cy = 400, w = 120, h = 50 (aspect ratio 2.4)
        assertTrue(
            "Horizontal road pothole should be accepted",
            PotholeOutputParser.isValidPotholeBox(cx = 320f, cy = 400f, w = 120f, h = 50f)
        )
    }

    @Test
    fun testVisionResult_potholePresenceThreshold() {
        // Threshold is 0.50f
        val above = VisionResult(maxConfidence = 0.50f)
        assertTrue("0.50f should be detected as pothole", above.isPothole)

        val higher = VisionResult(maxConfidence = 0.85f)
        assertTrue("0.85f should be detected as pothole", higher.isPothole)

        val below = VisionResult(maxConfidence = 0.499f)
        assertFalse("0.499f should not be detected as pothole", below.isPothole)

        // Error or rejectionReason must reject detection even if confidence was high
        val withError = VisionResult(maxConfidence = 0.99f, error = "TFLite runtime exception")
        assertFalse("Result with error must not be detected as pothole", withError.isPothole)

        val withRejection = VisionResult(maxConfidence = 0.99f, rejectionReason = "Human face detected")
        assertFalse("Result with rejection reason must not be detected as pothole", withRejection.isPothole)
    }
}
