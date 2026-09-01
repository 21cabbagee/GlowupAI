package com.glowup.ai.data.repository

import com.glowup.ai.core.util.GlowResult
import com.glowup.ai.core.util.onSuccess
import com.glowup.ai.data.local.LocalDataCleaner
import com.glowup.ai.data.local.SessionStore
import com.glowup.ai.data.remote.GlowUpApi
import com.glowup.ai.data.remote.apiCall
import com.glowup.ai.data.remote.apiCallNoContent
import com.glowup.ai.data.remote.dto.toDomain
import com.glowup.ai.data.repository.support.MutationLock
import com.glowup.ai.domain.model.Analytics
import com.glowup.ai.domain.model.ExportBundle
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Owns: `GET /export`, `DELETE /api/users/{id}`, `GET /analytics`.
 *
 * `DELETE` returns `204 No Content` — [deleteAccount] uses [apiCallNoContent], which never
 * attempts to parse a body (frontend-api-map.md "Error handling must preserve structured detail",
 * the `204` line). On confirmed success it clears ONLY GlowUp's own [SessionStore] keys — never a
 * blanket wipe — so the caller can route to onboarding with a clean, authoritative "no user" state
 * (ANDROID_PLAN.md 3.7: "typed `DELETE` confirmation ... on `204` clears only GlowUp keys").
 */
@Singleton
class PrivacyRepository
    @Inject
    constructor(
        private val api: GlowUpApi,
        private val sessionStore: SessionStore,
        private val localDataCleaner: LocalDataCleaner,
        private val invalidationBus: com.glowup.ai.data.repository.support.CacheInvalidationBus,
    ) {
        private val mutations = MutationLock<String>()
        val pendingKeys: StateFlow<Set<String>> = mutations.pendingKeys

        suspend fun exportData(userId: String): GlowResult<ExportBundle> = apiCall { api.exportUser(userId).toDomain() }

        /**
         * `GET /analytics` — engagement-derived product metrics ONLY (activation, verdict-open
         * rate, etc.). frontend-api-map.md is explicit that these must never be presented as
         * clinical confidence; ANDROID_PLAN.md 3.7 requires the panel that renders this to label
         * them as such. This is NOT `GET /api/admin/audit` — that global, cross-user audit log must
         * never be called from this app (frontend-api-map.md "Admin routes" + trap table).
         */
        suspend fun getAnalytics(userId: String): GlowResult<Analytics> = apiCall { api.getAnalytics(userId).toDomain() }

        /** Caller must have already collected a typed confirmation (ANDROID_PLAN.md 3.7) before
         * invoking this — this repository performs the deletion, it does not gate it. */
        suspend fun deleteAccount(userId: String): GlowResult<Unit> =
            mutations
                .run("delete_account:$userId") {
                    apiCallNoContent { api.deleteUser(userId) }
                }.onSuccess {
                    try {
                        localDataCleaner.clearUser(userId)
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (_: Exception) {
                        // The server deletion already succeeded; local cleanup remains best effort.
                    } finally {
                        sessionStore.clearSession()
                        invalidationBus.publish(
                            com.glowup.ai.data.repository.support.InvalidationSignal
                                .SessionCleared(userId),
                        )
                    }
                }
    }
