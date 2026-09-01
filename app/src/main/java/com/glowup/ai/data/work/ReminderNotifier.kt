package com.glowup.ai.data.work

import com.glowup.ai.data.local.SessionStore
import javax.inject.Inject

/**
 * Surfaces the actual capture-due nudge to the user. [ReminderWorker] only decides WHEN to fire
 * (from the server's own cadence, never a client-invented interval) — the notification UI,
 * POST-13 permission flow, and streak-at-risk copy belong to ANDROID_PLAN.md Phase 4.2, which
 * should provide a real implementation and bind it in place of [NoOpReminderNotifier].
 */
fun interface ReminderNotifier {
    suspend fun notifyCaptureDue(settings: SessionStore.ReminderSettings)
}

/** Default binding so Task 2.4 compiles standalone before Phase 4.2 lands. */
class NoOpReminderNotifier
    @Inject
    constructor() : ReminderNotifier {
        override suspend fun notifyCaptureDue(settings: SessionStore.ReminderSettings) {
            // Intentionally inert. Phase 4.2 replaces the WorkModule binding with a real notifier.
        }
    }
