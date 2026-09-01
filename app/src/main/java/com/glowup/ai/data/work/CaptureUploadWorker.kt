package com.glowup.ai.data.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.glowup.ai.data.repository.CaptureRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface CaptureUploadWorkerEntryPoint {
    fun captureRepository(): CaptureRepository
}

/**
 * Drains the capture outbox. Never marks a row "done" except on confirmed success or confirmed
 * prior acceptance (see [CaptureOutboxProcessor]) — a duplicate accepted capture would corrupt the
 * user's history, so this worker would rather retry-with-backoff forever than guess.
 *
 * Enqueued by [WorkScheduler.scheduleCaptureUpload] with `NetworkType.CONNECTED` +
 * `BackoffPolicy.EXPONENTIAL`; [androidx.work.Result.retry] here is what makes WorkManager apply
 * that backoff between attempts.
 */
class CaptureUploadWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val repository =
            EntryPointAccessors
                .fromApplication(
                    applicationContext,
                    CaptureUploadWorkerEntryPoint::class.java,
                ).captureRepository()

        val allSettled = repository.drainOutboxOnce()
        return if (allSettled) Result.success() else Result.retry()
    }
}
