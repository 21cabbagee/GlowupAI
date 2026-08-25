package com.glowup.ai.data.work

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Enqueues [CaptureUploadWorker] and [ReminderWorker]. Kept as a thin, testable seam so
 * repositories never touch `WorkManager` directly.
 *
 * NOTE ([GlowUpApplication][com.glowup.ai.GlowUpApplication] does not implement
 * `Configuration.Provider`): both workers are instantiated by WorkManager's DEFAULT
 * `WorkerFactory` (reflection, no-arg-besides-context-and-params constructor) and reach their
 * dependencies through a Hilt `@EntryPoint` inside `doWork()` — see
 * [CaptureUploadWorkerEntryPoint]/[ReminderWorkerEntryPoint] — rather than `HiltWorkerFactory`,
 * which would require registering a custom `Configuration` on the `Application` class. That file
 * belongs to the DI/shell agent (Task 2.5), not this task.
 */
@Singleton
class WorkScheduler @Inject constructor(@ApplicationContext private val context: Context) {

    companion object {
        const val CAPTURE_UPLOAD_WORK_NAME = "glowup_capture_upload"
        const val REMINDER_WORK_NAME = "glowup_reminder"
    }

    private val workManager: WorkManager get() = WorkManager.getInstance(context)

    /** Exponential backoff, network-constrained. `KEEP` so a burst of offline captures collapses
     * into one worker run that drains the whole outbox rather than one worker per capture. */
    fun scheduleCaptureUpload() {
        val request = OneTimeWorkRequestBuilder<CaptureUploadWorker>()
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, WorkManagerMinBackoffMillis, TimeUnit.MILLISECONDS)
            .build()
        workManager.enqueueUniqueWork(CAPTURE_UPLOAD_WORK_NAME, ExistingWorkPolicy.KEEP, request)
    }

    /**
     * Schedules the next-capture nudge with an initial delay computed from the SERVER's own
     * cadence/window (already persisted into [com.glowup.ai.data.local.SessionStore] by
     * `HomeRepository.getEngagement`) — never a client-invented interval. [delayMillis] `<= 0`
     * means "no server-provided schedule yet"; callers should not call this until one exists.
     */
    fun scheduleReminder(delayMillis: Long) {
        if (delayMillis <= 0) return
        val request = OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
            .build()
        workManager.enqueueUniqueWork(REMINDER_WORK_NAME, ExistingWorkPolicy.REPLACE, request)
    }

    fun cancelReminder() = workManager.cancelUniqueWork(REMINDER_WORK_NAME)
}

private const val WorkManagerMinBackoffMillis = 30_000L
