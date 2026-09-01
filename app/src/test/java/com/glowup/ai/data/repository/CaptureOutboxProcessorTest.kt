package com.glowup.ai.data.repository

import com.glowup.ai.core.util.GlowResult
import com.glowup.ai.data.local.CaptureOutboxEntity
import com.glowup.ai.data.remote.ApiError
import com.glowup.ai.data.work.CaptureOutboxProcessor
import com.glowup.ai.data.work.OutboxOutcome
import com.glowup.ai.domain.model.AppearanceMetric
import com.glowup.ai.domain.model.CaptureQuality
import com.glowup.ai.domain.model.CaptureResult
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Covers ANDROID_PLAN.md 2.4's outbox idempotency requirement: "a duplicate accepted capture
 * corrupts the user's history" — [CaptureOutboxProcessor] must reconcile against history BEFORE
 * re-uploading a retried row, and must never treat an explicit server rejection the same as an
 * ambiguous network failure.
 */
class CaptureOutboxProcessorTest {
    private fun entry(attemptCount: Int = 0) =
        CaptureOutboxEntity(
            id = 1,
            userId = "user-1",
            vertical = "skin",
            imagePath = "/tmp/fake.b64",
            qualityJson = null,
            isBaseline = false,
            experimentId = null,
            capturedAt = "2026-08-24T10:00:00.000Z",
            deviceMetaJson = null,
            attemptCount = attemptCount,
            createdAtMillis = 0,
        )

    private fun fakeCaptureResult() =
        CaptureResult(
            id = "capture-1",
            capturedAt = "2026-08-24T10:00:00.000Z",
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

    @Test
    fun `first attempt uploads directly without checking history`() =
        runTest {
            var historyChecked = false
            val processor =
                CaptureOutboxProcessor(
                    upload = { GlowResult.Success(fakeCaptureResult()) },
                    wasAlreadyAccepted = { _, _, _ ->
                        historyChecked = true
                        false
                    },
                    loadImageBase64 = { "base64==" },
                )

            val outcome = processor.process(entry(attemptCount = 0))

            assertTrue(outcome is OutboxOutcome.Uploaded)
            assertTrue("first attempt must not pay for a reconciliation check", !historyChecked)
        }

    @Test
    fun `a retry reconciles first and skips re-upload if already accepted`() =
        runTest {
            var uploadCalled = false
            val processor =
                CaptureOutboxProcessor(
                    upload = {
                        uploadCalled = true
                        GlowResult.Success(fakeCaptureResult())
                    },
                    wasAlreadyAccepted = { userId, vertical, capturedAt ->
                        userId == "user-1" && vertical == "skin" && capturedAt == "2026-08-24T10:00:00.000Z"
                    },
                    loadImageBase64 = { "base64==" },
                )

            val outcome = processor.process(entry(attemptCount = 1))

            assertEquals(OutboxOutcome.AlreadyAccepted, outcome)
            assertTrue("must never re-upload a capture confirmed already accepted", !uploadCalled)
        }

    @Test
    fun `a retry that reconciles as NOT accepted uploads again`() =
        runTest {
            var uploadCalled = false
            val processor =
                CaptureOutboxProcessor(
                    upload = {
                        uploadCalled = true
                        GlowResult.Success(fakeCaptureResult())
                    },
                    wasAlreadyAccepted = { _, _, _ -> false },
                    loadImageBase64 = { "base64==" },
                )

            val outcome = processor.process(entry(attemptCount = 2))

            assertTrue(outcome is OutboxOutcome.Uploaded)
            assertTrue(uploadCalled)
        }

    @Test
    fun `an ambiguous network failure is RetryLater, never a permanent failure`() =
        runTest {
            val networkError = ApiError.Network(RuntimeException("timeout"))
            val processor =
                CaptureOutboxProcessor(
                    upload = { GlowResult.Failure(networkError) },
                    wasAlreadyAccepted = { _, _, _ -> false },
                    loadImageBase64 = { "base64==" },
                )

            val outcome = processor.process(entry())

            assertEquals(OutboxOutcome.RetryLater(networkError), outcome)
        }

    @Test
    fun `an explicit quality rejection is a permanent failure, never retried`() =
        runTest {
            val quality =
                CaptureQuality(
                    facePresent = false,
                    yawDegrees = 20.0,
                    pitchDegrees = 0.0,
                    brightness = 0.1,
                    sharpness = 0.1,
                    distanceCm = 80.0,
                    expressionNeutral = true,
                    referenceCardPresent = false,
                    score = 0.1,
                    accepted = false,
                    failedChecks = listOf("face_present"),
                    coaching = emptyList(),
                )
            val rejection = ApiError.CaptureQualityRejected(quality, emptyList())
            val processor =
                CaptureOutboxProcessor(
                    upload = { GlowResult.Failure(rejection) },
                    wasAlreadyAccepted = { _, _, _ -> false },
                    loadImageBase64 = { "base64==" },
                )

            val outcome = processor.process(entry())

            assertEquals(OutboxOutcome.PermanentFailure(rejection), outcome)
        }
}
