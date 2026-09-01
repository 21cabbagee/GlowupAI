package com.glowup.ai.data.repository

import com.glowup.ai.core.util.GlowResult
import com.glowup.ai.core.util.onSuccess
import com.glowup.ai.data.local.SessionStore
import com.glowup.ai.data.remote.GlowUpApi
import com.glowup.ai.data.remote.apiCall
import com.glowup.ai.data.remote.dto.UpgradeRequestDto
import com.glowup.ai.data.remote.dto.toDomain
import com.glowup.ai.data.repository.support.CacheInvalidationBus
import com.glowup.ai.data.repository.support.InvalidationSignal
import com.glowup.ai.data.repository.support.MutationLock
import com.glowup.ai.domain.model.Subscription
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Owns: `GET /subscription`, `POST /subscription/upgrade`, `POST /subscription/cancel`.
 *
 * [getSubscription] is the ONLY authoritative source of entitlement — frontend-api-map.md trap
 * #6/#12: Premium requires BOTH `plan == "premium"` AND `status == "active"`
 * ([Subscription.isPremium] is the single place that rule lives; never re-derive it from a tap).
 * Upgrade/cancel are not idempotent and change the `{userId, plan}` cache key everywhere else in
 * the app, so both publish [InvalidationSignal.SubscriptionChanged] on success — and update the
 * locally cached plan in [SessionStore] immediately so the NEXT cache lookup (even before a
 * fresh `GET /dashboard`) already misses the old plan's entries.
 */
@Singleton
class BillingRepository
    @Inject
    constructor(
        private val api: GlowUpApi,
        private val sessionStore: SessionStore,
        private val invalidationBus: CacheInvalidationBus,
    ) {
        private val mutations = MutationLock<String>()
        val pendingKeys: StateFlow<Set<String>> = mutations.pendingKeys

        suspend fun getSubscription(userId: String): GlowResult<Subscription> =
            apiCall { api.getSubscription(userId).toDomain() }.onSuccess {
                sessionStore.setEntitlement(it.plan, it.status)
            }

        suspend fun upgrade(
            userId: String,
            source: String = "local_checkout",
        ): GlowResult<Subscription> =
            mutations
                .run("upgrade:$userId") {
                    apiCall { api.upgradeSubscription(userId, UpgradeRequestDto(source)).toDomain() }
                }.onSuccess {
                    sessionStore.setEntitlement(it.plan, it.status)
                    invalidationBus.publish(InvalidationSignal.SubscriptionChanged(userId))
                }

        suspend fun cancel(userId: String): GlowResult<Subscription> =
            mutations
                .run("cancel:$userId") {
                    apiCall { api.cancelSubscription(userId).toDomain() }
                }.onSuccess {
                    sessionStore.setEntitlement(it.plan, it.status)
                    invalidationBus.publish(InvalidationSignal.SubscriptionChanged(userId))
                }
    }
