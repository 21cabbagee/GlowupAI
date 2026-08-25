package com.glowup.ai.data.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.glowup.ai.data.local.SessionStore
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

@EntryPoint
@InstallIn(SingletonComponent::class)
interface ReminderWorkerEntryPoint {
    fun sessionStore(): SessionStore
    fun workScheduler(): WorkScheduler
    fun reminderNotifier(): ReminderNotifier
}

/**
 * Fires the next-capture nudge and reschedules itself — from cadence/window data the app already
 * pulled from `GET /engagement`/`GET /capture-guide` on a real screen visit
 * ([HomeRepository][com.glowup.ai.data.repository.HomeRepository] persists it into
 * [SessionStore] every time). This worker deliberately never calls the network itself: doing so
 * on a timer would be exactly the "never poll `GET /engagement`" trap (frontend-api-map.md trap
 * #7) this whole module exists to avoid. If the server hasn't provided a cadence yet (brand new
 * user, or the app hasn't opened Home since), this worker has nothing to reschedule and stays
 * cancelled until `HomeRepository` populates one.
 */
class ReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val entryPoint = EntryPointAccessors.fromApplication(applicationContext, ReminderWorkerEntryPoint::class.java)
        val sessionStore = entryPoint.sessionStore()
        val settings = sessionStore.reminderSettingsFlow.first()

        // A worker can already be running when the user disables reminders. Exit before both
        // notification and self-rescheduling so the setting wins over the in-flight work.
        if (!settings.enabled) return Result.success()

        entryPoint.reminderNotifier().notifyCaptureDue(settings)
        settings.cadenceDays?.let { days ->
            entryPoint.workScheduler().scheduleReminder(TimeUnit.DAYS.toMillis(days.toLong()))
        }

        return Result.success()
    }
}
