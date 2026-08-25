package com.glowup.ai.data.work

import com.glowup.ai.core.util.GlowResult
import com.glowup.ai.data.local.CaptureOutboxEntity
import com.glowup.ai.data.remote.ApiError
import com.glowup.ai.data.remote.NetworkJson
import com.glowup.ai.data.remote.dto.CaptureQualityInputDto
import com.glowup.ai.domain.model.CaptureCreateRequest
import com.glowup.ai.domain.model.CaptureResult
import com.glowup.ai.domain.model.CapturePose

/** Outcome of processing a single [CaptureOutboxEntity] row. Deleting the row is the CALLER's
 * job (see [com.glowup.ai.data.repository.CaptureRepository.drainOutboxOnce] and
 * [CaptureUploadWorker]) — this class only decides what happened. */
sealed class OutboxOutcome {
    data class Uploaded(val result: CaptureResult) : OutboxOutcome()

    /** Reconciliation found the capture already accepted from a previous, ambiguously-failed
     * attempt — the row must be deleted WITHOUT re-uploading. */
    object AlreadyAccepted : OutboxOutcome()

    /** Ambiguous or transient failure (network/server/unauthorized) — the row must be kept,
     * `attemptCount` incremented, and WorkManager's exponential backoff applied before the next
     * attempt tries again (which will reconcile first — see [process]). */
    data class RetryLater(val error: ApiError) : OutboxOutcome()

    /** The server explicitly rejected this exact payload (quality/validation/conflict) — safe to
     * stop retrying automatically; the row moves to `failed_permanent` for a manual retake. */
    data class PermanentFailure(val error: ApiError) : OutboxOutcome()
}

/**
 * The outbox idempotency mechanism required by ANDROID_PLAN.md 2.4 / frontend-api-map.md trap #9:
 * "a duplicate accepted capture corrupts the user's history." `POST /api/captures` has no
 * server-side idempotency key, so this class implements idempotency on the CLIENT side instead:
 *
 * On any retry (`entry.attemptCount > 0` — i.e. a previous attempt for this exact row already ran
 * and its outcome was never confirmed), [process] calls [wasAlreadyAccepted] BEFORE re-uploading.
 * That callback looks for a history item at the row's own client-stamped `capturedAt` for the
 * same user/vertical — if the first attempt's request reached the server and was accepted but the
 * response never made it back (a timeout is indistinguishable from a dropped response), the
 * capture is already in the user's history and re-sending it would create a duplicate. Finding it
 * short-circuits to [OutboxOutcome.AlreadyAccepted] instead of uploading again.
 *
 * Collaborators are injected as plain suspend lambdas (not [com.glowup.ai.data.remote.GlowUpApi]
 * directly) so this class is unit-testable with hand-written fakes instead of a 50-method mock.
 */
class CaptureOutboxProcessor(
    private val upload: suspend (CaptureCreateRequest) -> GlowResult<CaptureResult>,
    private val wasAlreadyAccepted: suspend (userId: String, vertical: String, capturedAt: String) -> Boolean,
    private val loadImageBase64: suspend (path: String) -> String,
) {

    suspend fun process(entry: CaptureOutboxEntity): OutboxOutcome {
        if (entry.attemptCount > 0 && wasAlreadyAccepted(entry.userId, entry.vertical, entry.capturedAt)) {
            return OutboxOutcome.AlreadyAccepted
        }

        val base64 = loadImageBase64(entry.imagePath)
        val pose = entry.qualityJson
            ?.let { json -> runCatching { NetworkJson.decodeFromString(CaptureQualityInputDto.serializer(), json) }.getOrNull() }
            ?.let { CapturePose(it.facePresent, it.yawDegrees, it.pitchDegrees, it.distanceCm, it.expressionNeutral) }
        val deviceMeta = entry.deviceMetaJson
            ?.let { json -> runCatching { NetworkJson.decodeFromString(kotlinx.serialization.serializer<Map<String, String>>(), json) }.getOrNull() }

        val request = CaptureCreateRequest(
            userId = entry.userId,
            imageBase64 = base64,
            pose = pose,
            isBaseline = entry.isBaseline,
            vertical = entry.vertical,
            experimentId = entry.experimentId,
            capturedAt = entry.capturedAt,
            deviceMeta = deviceMeta,
        )

        return when (val result = upload(request)) {
            is GlowResult.Success -> OutboxOutcome.Uploaded(result.data)
            is GlowResult.Failure -> when (result.error) {
                is ApiError.CaptureQualityRejected,
                is ApiError.Validation,
                is ApiError.Conflict,
                is ApiError.NotFound,
                -> OutboxOutcome.PermanentFailure(result.error)
                else -> OutboxOutcome.RetryLater(result.error)
            }
        }
    }
}
