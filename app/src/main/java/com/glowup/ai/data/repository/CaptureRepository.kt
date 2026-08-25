package com.glowup.ai.data.repository

import com.glowup.ai.core.util.GlowResult
import com.glowup.ai.data.local.CaptureImageStore
import com.glowup.ai.data.local.CaptureOutboxDao
import com.glowup.ai.data.local.CaptureOutboxEntity
import com.glowup.ai.data.remote.ApiError
import com.glowup.ai.data.remote.GlowUpApi
import com.glowup.ai.data.remote.NetworkJson
import com.glowup.ai.data.remote.apiCall
import com.glowup.ai.data.remote.dto.CaptureQualityInputDto
import com.glowup.ai.data.remote.dto.toDomain
import com.glowup.ai.data.remote.dto.toDto
import com.glowup.ai.data.repository.support.CacheInvalidationBus
import com.glowup.ai.data.repository.support.InvalidationSignal
import com.glowup.ai.data.repository.support.MutationLock
import com.glowup.ai.data.work.CaptureOutboxProcessor
import com.glowup.ai.data.work.OutboxOutcome
import com.glowup.ai.data.work.WorkScheduler
import com.glowup.ai.domain.model.CaptureCreateRequest
import com.glowup.ai.domain.model.CaptureResult
import com.glowup.ai.domain.model.MeasurementFeedback
import com.glowup.ai.domain.model.MeasurementFeedbackRequest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.serializer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Owns: `POST /api/captures`, `POST /measurement-feedback`, and the local capture outbox that
 * backs them.
 *
 * `POST /api/captures` is NOT idempotent (frontend-api-map.md trap #9) and the capture quality
 * gate is server-authoritative (trap #6/#3) — this repository never marks a frame accepted itself
 * and never auto-retries a submission. On an ambiguous failure (no HTTP response at all —
 * [ApiError.Network]) it queues the request into the outbox instead of retrying inline; only
 * [com.glowup.ai.data.work.CaptureUploadWorker] (via [drainOutboxOnce]) ever re-attempts it, and
 * only after reconciling against `GET /history` first — see [CaptureOutboxProcessor].
 */
@Singleton
class CaptureRepository @Inject constructor(
    private val api: GlowUpApi,
    private val outboxDao: CaptureOutboxDao,
    private val imageStore: CaptureImageStore,
    private val invalidationBus: CacheInvalidationBus,
    private val workScheduler: WorkScheduler,
) {

    private val mutations = MutationLock<String>()

    /** Lets a ViewModel disable the capture/upload button while a submission is outstanding. */
    val pendingKeys: StateFlow<Set<String>> = mutations.pendingKeys

    val pendingOutboxCount: Flow<Int> = outboxDao.pendingCountFlow()
    val outboxFlow: Flow<List<CaptureOutboxEntity>> = outboxDao.allFlow()

    /** [outboxFlow] filtered to one user — lets `feature/capture` show "status unknown, retrying
     * in background" / "couldn't upload, retake" banners for just the signed-in user's own rows
     * without duplicating outbox-reading logic outside this repository. */
    fun outboxForUser(userId: String): Flow<List<CaptureOutboxEntity>> =
        outboxFlow.map { rows -> rows.filter { it.userId == userId } }

    private val processor = CaptureOutboxProcessor(
        upload = { request -> apiCall { api.createCapture(request.toDto()).toDomain() } },
        wasAlreadyAccepted = { userId, vertical, capturedAt -> wasAlreadyAccepted(userId, vertical, capturedAt) },
        loadImageBase64 = { path -> imageStore.read(path) },
    )

    /**
     * Attempts an immediate upload. On success, publishes [InvalidationSignal.CaptureAccepted] so
     * `HomeRepository`'s dashboard/engagement/history caches go stale. On a [ApiError.Network]
     * failure (timeout / no connectivity — "status unknown" per trap #9) the request is queued to
     * the outbox and the worker is scheduled; the caller must show "status unknown, retrying in
     * background", never call this again with the same intent.
     */
    suspend fun submitCapture(request: CaptureCreateRequest): GlowResult<CaptureResult> =
        mutations.run("submit_capture:${request.userId}:${request.vertical}") {
            when (val result = apiCall { api.createCapture(request.toDto()).toDomain() }) {
                is GlowResult.Success -> {
                    invalidationBus.publish(InvalidationSignal.CaptureAccepted(request.userId, request.vertical))
                    result
                }
                is GlowResult.Failure -> {
                    if (result.error is ApiError.Network) {
                        enqueueOffline(request)
                    }
                    result
                }
            }
        }

    /** Queues a capture (taken offline, or whose immediate upload above hit an ambiguous
     * network failure) for background upload. Never called a second time for the same in-memory
     * request by [submitCapture] — only the worker re-attempts, and only after reconciling. */
    suspend fun enqueueOffline(request: CaptureCreateRequest): Long {
        val imagePath = imageStore.save(request.imageBase64)
        val qualityJson = request.pose?.let {
            NetworkJson.encodeToString(
                CaptureQualityInputDto.serializer(),
                CaptureQualityInputDto(it.facePresent, it.yawDegrees, it.pitchDegrees, it.distanceCm, it.expressionNeutral),
            )
        }
        val deviceMetaJson = request.deviceMeta?.let { NetworkJson.encodeToString(serializer<Map<String, String>>(), it) }
        val capturedAt = request.capturedAt ?: isoNow()
        val id = outboxDao.insert(
            CaptureOutboxEntity(
                userId = request.userId,
                vertical = request.vertical,
                imagePath = imagePath,
                qualityJson = qualityJson,
                isBaseline = request.isBaseline,
                experimentId = request.experimentId,
                capturedAt = capturedAt,
                deviceMetaJson = deviceMetaJson,
                createdAtMillis = System.currentTimeMillis(),
            ),
        )
        workScheduler.scheduleCaptureUpload()
        return id
    }

    /**
     * Drains every pending outbox row once. Called by [com.glowup.ai.data.work.CaptureUploadWorker]
     * (and available for an in-app "retry now" affordance). Returns `true` if every row that was
     * attempted this pass either uploaded or was confirmed already-accepted — i.e. nothing needs a
     * backoff retry — so the caller (the Worker) knows whether to report success or request a
     * WorkManager retry with backoff.
     */
    suspend fun drainOutboxOnce(): Boolean {
        var allSettled = true
        for (entry in outboxDao.pending()) {
            when (val outcome = processor.process(entry)) {
                is OutboxOutcome.Uploaded -> {
                    outboxDao.deleteById(entry.id)
                    imageStore.delete(entry.imagePath)
                    invalidationBus.publish(InvalidationSignal.CaptureAccepted(entry.userId, entry.vertical))
                }
                OutboxOutcome.AlreadyAccepted -> {
                    outboxDao.deleteById(entry.id)
                    imageStore.delete(entry.imagePath)
                }
                is OutboxOutcome.RetryLater -> {
                    allSettled = false
                    outboxDao.update(entry.copy(attemptCount = entry.attemptCount + 1, lastError = outcome.error.toString()))
                }
                is OutboxOutcome.PermanentFailure -> {
                    outboxDao.update(entry.copy(status = "failed_permanent", lastError = outcome.error.toString()))
                }
            }
        }
        return allSettled
    }

    suspend fun addMeasurementFeedback(userId: String, request: MeasurementFeedbackRequest): GlowResult<MeasurementFeedback> =
        apiCall { api.addMeasurementFeedback(userId, request.toDto()).toDomain() }

    private suspend fun wasAlreadyAccepted(userId: String, vertical: String, capturedAt: String): Boolean {
        val result = apiCall { api.getHistory(userId, vertical).map { it.toDomain() } }
        return (result as? GlowResult.Success)?.data?.any { it.capturedAt == capturedAt } ?: false
    }

    private fun isoNow(): String {
        val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
        format.timeZone = TimeZone.getTimeZone("UTC")
        return format.format(Date())
    }
}
