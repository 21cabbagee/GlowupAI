package com.glowup.ai.feature.capture

import com.glowup.ai.domain.model.AppearanceMetric
import com.glowup.ai.domain.model.CaptureQuality
import com.glowup.ai.domain.model.CaptureResult
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class CaptureResultCacheTest {
    @Test
    fun `clear removes results from the previous session`() {
        val cache = CaptureResultCache()
        val result =
            CaptureResult(
                id = "capture-1",
                capturedAt = "2026-09-15T00:00:00.000Z",
                isBaseline = false,
                status = "accepted",
                captureQuality =
                    CaptureQuality(
                        facePresent = true,
                        yawDegrees = 0.0,
                        pitchDegrees = 0.0,
                        brightness = 0.5,
                        sharpness = 0.5,
                        distanceCm = 40.0,
                        expressionNeutral = true,
                        referenceCardPresent = false,
                        score = 0.9,
                        accepted = true,
                        failedChecks = emptyList(),
                        coaching = emptyList(),
                    ),
                analysisJobId = null,
                metric = AppearanceMetric(null, null, null, null, null, null, null),
                vertical = "skin",
            )

        cache.put(result)
        assertNotNull(cache.get("capture-1"))

        cache.clear()

        assertNull(cache.get("capture-1"))
    }
}
