package com.glowup.ai.feature.capture

import org.junit.Assert.assertEquals
import org.junit.Test

class CaptureImageProcessorTest {
    @Test
    fun `large device images are sampled close to the upload target`() {
        assertEquals(8, CaptureImageProcessor.calculateInSampleSize(12_000, 9_000))
        assertEquals(4, CaptureImageProcessor.calculateInSampleSize(8_000, 6_000))
        assertEquals(1, CaptureImageProcessor.calculateInSampleSize(2_000, 1_500))
    }
}
