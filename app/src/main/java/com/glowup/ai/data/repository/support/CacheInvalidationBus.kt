package com.glowup.ai.data.repository.support

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The cache invalidation matrix from frontend-api-map.md trap #7, as an event bus rather than a
 * document: any repository that performs a mutation [publish]es what happened, and
 * [HomeRepository][com.glowup.ai.data.repository.HomeRepository] (which owns the two
 * side-effecting, never-poll `GET /dashboard` / `GET /engagement` caches) subscribes and marks its
 * caches stale accordingly. This keeps mutation repositories (`CaptureRepository`,
 * `RoutineRepository`, `ExperimentRepository`, `SessionRepository`, `BillingRepository`) free of
 * any direct dependency on `HomeRepository`.
 *
 * | Trigger (publisher)                                   | Invalidates                          |
 * |--------------------------------------------------------|---------------------------------------|
 * | Capture accepted (`CaptureRepository`)                  | dashboard, engagement, history        |
 * | Routine event logged (`RoutineRepository`)              | dashboard, engagement                 |
 * | Experiment create/status change (`ExperimentRepository`)| dashboard, experiments                |
 * | Consent change (`SessionRepository`)                    | dashboard, engagement (plan-scoped)   |
 * | Subscription upgrade/cancel (`BillingRepository`)       | dashboard, engagement (plan changed!) |
 * | Shelf-scan confirm (`RoutineRepository`)                | products, dashboard                   |
 */
sealed class InvalidationSignal {
    data class CaptureAccepted(
        val userId: String,
        val vertical: String,
    ) : InvalidationSignal()

    data class RoutineEventLogged(
        val userId: String,
    ) : InvalidationSignal()

    data class ExperimentChanged(
        val userId: String,
    ) : InvalidationSignal()

    data class ConsentChanged(
        val userId: String,
    ) : InvalidationSignal()

    data class SubscriptionChanged(
        val userId: String,
    ) : InvalidationSignal()

    data class ShelfScanConfirmed(
        val userId: String,
    ) : InvalidationSignal()

    data class SessionCleared(
        val userId: String,
    ) : InvalidationSignal()
}

@Singleton
class CacheInvalidationBus
    @Inject
    constructor() {
        private val _events =
            MutableSharedFlow<InvalidationSignal>(
                // A singleton subscriber may be starting while a mutation publishes.
                // Replaying the latest invalidation is harmless and prevents a cold
                // start from losing the only signal that matters for its caches.
                replay = 1,
                extraBufferCapacity = 32,
            )
        val events: SharedFlow<InvalidationSignal> = _events

        fun publish(signal: InvalidationSignal) {
            _events.tryEmit(signal)
        }
    }
