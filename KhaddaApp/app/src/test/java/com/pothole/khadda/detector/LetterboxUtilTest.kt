package com.pothole.khadda.detector

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for Letterbox preprocessing math and rotation handling.
 */
class LetterboxUtilTest {

    @Test
    fun testLetterbox_landscapeImage_padsVertical() {
        // 1920x1080 -> 640x640 target
        // Scale is 640 / 1920 = 0.33333334
        // Scaled dimensions: width = 640, height = round(1080 * 0.33333334) = 360
        // Padding: padW = 0, padH = 640 - 360 = 280 -> padTop = 140, padBottom = 140
        val params = LetterboxUtil.calculateLetterbox(
            srcWidth = 1920,
            srcHeight = 1080,
            targetWidth = 640,
            targetHeight = 640
        )

        assertEquals(640, params.scaledWidth)
        assertEquals(360, params.scaledHeight)
        assertEquals(0f, params.padLeft, 0.01f)
        assertEquals(0f, params.padRight, 0.01f)
        assertEquals(140f, params.padTop, 0.01f)
        assertEquals(140f, params.padBottom, 0.01f)
        assertEquals(640 / 1920f, params.scale, 0.001f)
    }

    @Test
    fun testLetterbox_portraitImage_padsHorizontal() {
        // 1080x1920 -> 640x640 target
        // Scale is 640 / 1920 = 0.33333334
        // Scaled dimensions: width = 360, height = 640
        // Padding: padW = 280 -> padLeft = 140, padRight = 140, padTop = 0, padBottom = 0
        val params = LetterboxUtil.calculateLetterbox(
            srcWidth = 1080,
            srcHeight = 1920,
            targetWidth = 640,
            targetHeight = 640
        )

        assertEquals(360, params.scaledWidth)
        assertEquals(640, params.scaledHeight)
        assertEquals(140f, params.padLeft, 0.01f)
        assertEquals(140f, params.padRight, 0.01f)
        assertEquals(0f, params.padTop, 0.01f)
        assertEquals(0f, params.padBottom, 0.01f)
    }

    @Test
    fun testLetterbox_squareImage_noPadding() {
        val params = LetterboxUtil.calculateLetterbox(
            srcWidth = 640,
            srcHeight = 640,
            targetWidth = 640,
            targetHeight = 640
        )

        assertEquals(640, params.scaledWidth)
        assertEquals(640, params.scaledHeight)
        assertEquals(0f, params.padLeft, 0.01f)
        assertEquals(0f, params.padTop, 0.01f)
        assertEquals(1.0f, params.scale, 0.001f)
    }

    @Test
    fun testRotation_dimensionsSwapCorrectly() {
        val originalW = 1920
        val originalH = 1080

        // 0 degrees: upright, no swap
        val dim0 = LetterboxUtil.getRotatedDimensions(originalW, originalH, 0)
        assertEquals(1920, dim0.first)
        assertEquals(1080, dim0.second)

        // 90 degrees: swap width and height
        val dim90 = LetterboxUtil.getRotatedDimensions(originalW, originalH, 90)
        assertEquals(1080, dim90.first)
        assertEquals(1920, dim90.second)

        // 180 degrees: no dimension swap
        val dim180 = LetterboxUtil.getRotatedDimensions(originalW, originalH, 180)
        assertEquals(1920, dim180.first)
        assertEquals(1080, dim180.second)

        // 270 degrees: swap width and height
        val dim270 = LetterboxUtil.getRotatedDimensions(originalW, originalH, 270)
        assertEquals(1080, dim270.first)
        assertEquals(1920, dim270.second)

        // Negative or multiple 360 degrees
        val dim360 = LetterboxUtil.getRotatedDimensions(originalW, originalH, 360)
        assertEquals(1920, dim360.first)
        assertEquals(1080, dim360.second)

        val dimNeg90 = LetterboxUtil.getRotatedDimensions(originalW, originalH, -90)
        assertEquals(1080, dimNeg90.first)
        assertEquals(1920, dimNeg90.second)
    }
}
