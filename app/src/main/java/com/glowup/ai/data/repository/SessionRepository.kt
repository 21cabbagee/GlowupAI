package com.glowup.ai.data.repository

import com.glowup.ai.core.util.GlowResult
import com.glowup.ai.core.util.onSuccess
import com.glowup.ai.data.local.LocalDataCleaner
import com.glowup.ai.data.local.SessionStore
import com.glowup.ai.data.remote.GlowUpApi
import com.glowup.ai.data.remote.apiCall
import com.glowup.ai.data.remote.dto.ConsentRequestDto
import com.glowup.ai.data.remote.dto.HealthDto
import com.glowup.ai.data.remote.dto.UserCreateRequestDto
import com.glowup.ai.data.remote.dto.toDomain
import com.glowup.ai.data.remote.dto.toDto
import com.glowup.ai.data.repository.support.MutationLock
import com.glowup.ai.domain.model.HealthStatus
import com.glowup.ai.domain.model.Profile
import com.glowup.ai.domain.model.ProfileUpdateRequest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.CancellationException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Owns: `GET /api/health`, `POST /api/auth/session`, `POST /api/users`,
 * `GET /profile`, `PATCH /profile`, `POST /consent`.
 *
 * [ensureUser] is THE fix for ANDROID_PLAN.md's headline bug (`ApiService.kt:150` calling
 * `createUser()` on every single analysis): it checks [SessionStore.userId] first and only ever
 * calls the create-a-user endpoint when no id is stored yet. Every other repository reads the
 * user id from [SessionStore] — none of them may call `createUser`/`authSession` themselves.
 *
 * Every method that returns a fresh [Profile] persists the fields [SessionStore] caches
 * (`user_id`, plan/entitlement, consent state, onboarding completion) so a cold start can restore
 * a candidate identity before the first `GET /profile` round-trip completes.
 */
@Singleton
class SessionRepository @Inject constructor(
    private val api: GlowUpApi,
    private val sessionStore: SessionStore,
    private val localDataCleaner: LocalDataCleaner,
    private val invalidationBus: com.glowup.ai.data.repository.support.CacheInvalidationBus,
) {

    /** `POST /api/users`, `POST /consent`, and `PATCH /profile` are not idempotent — guard
     * concurrent taps on the same action. */
    private val mutations = MutationLock<String>()

    val userIdFlow: Flow<String?> = sessionStore.userIdFlow

    suspend fun health(): GlowResult<HealthStatus> = apiCall { api.health().toDomain() }

    /**
     * The bug fix: reuse a stored user id if one exists; only create a new backend user the very
     * first time this device has never seen one. Never call this on every capture/analysis.
     */
    suspend fun ensureUser(skinType: String? = null): GlowResult<Profile> {
        val existing = sessionStore.userId()
        if (existing != null) return refreshProfile(existing)
        return mutations.run("create_user") {
            apiCall { api.createUser(UserCreateRequestDto(skinType = skinType)).toDomain() }
        }.onSuccess { persist(it) }
    }

    /** `POST /api/auth/session` is documented idempotent per Firebase uid — still worth a lock so
     * a rotating token / rapid re-auth doesn't fire two concurrent profile-creating calls. */
    suspend fun authenticateWithFirebase(): GlowResult<Profile> =
        mutations.run("auth_session") {
            apiCall { api.authSession().toDomain() }
        }.onSuccess { persist(it) }

    suspend fun refreshProfile(userId: String? = null): GlowResult<Profile> {
        val id = userId ?: sessionStore.userId() ?: return GlowResult.Failure(
            com.glowup.ai.data.remote.ApiError.Unknown(IllegalStateException("refreshProfile called with no stored user id")),
        )
        return apiCall { api.getProfile(id).toDomain() }.onSuccess { persist(it) }
    }

    suspend fun updateProfile(request: ProfileUpdateRequest): GlowResult<Profile> {
        val id = sessionStore.userId() ?: return notSignedIn()
        return mutations.run("update_profile") {
            apiCall { api.updateProfile(id, request.toDto()).toDomain() }
        }.onSuccess { persist(it) }
    }

    /**
     * Declining must keep the profile usable — see [com.glowup.ai.domain.SessionState.ConsentDeclined].
     * This repository just relays the result and publishes [com.glowup.ai.data.repository.support.InvalidationSignal.ConsentChanged];
     * it never infers or grants consent locally.
     */
    suspend fun grantConsent(facialData: Boolean, policyVersion: String?): GlowResult<Profile> {
        val id = sessionStore.userId() ?: return notSignedIn()
        return mutations.run("consent") {
            apiCall { api.grantConsent(id, ConsentRequestDto(facialData = facialData, policyVersion = policyVersion)).toDomain() }
        }.onSuccess { profile ->
            persist(profile)
            invalidationBus.publish(com.glowup.ai.data.repository.support.InvalidationSignal.ConsentChanged(id))
        }
    }

    /** Clears user-scoped data before removing identity keys, including stale offline captures. */
    suspend fun clearSession() {
        val userId = sessionStore.userId()
        try {
            userId?.let { localDataCleaner.clearUser(it) }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            // Local cleanup is best effort; stale identity must never survive sign-out.
        } finally {
            sessionStore.clearSession()
            userId?.let { invalidationBus.publish(com.glowup.ai.data.repository.support.InvalidationSignal.SessionCleared(it)) }
        }
    }

    private suspend fun persist(profile: Profile) {
        sessionStore.setUserId(profile.user.id)
        profile.user.firebaseUid?.let { sessionStore.setFirebaseUid(it) }
        sessionStore.setEntitlement(profile.entitlement.plan, profile.entitlement.status)
        sessionStore.setConsentState(profile.user.consentState)
        profile.experienceProfile?.let { sessionStore.setOnboardingComplete(it.onboardingComplete) }
    }

    private fun <T> notSignedIn(): GlowResult<T> = GlowResult.Failure(
        com.glowup.ai.data.remote.ApiError.Unknown(IllegalStateException("No stored user id — call ensureUser()/authenticateWithFirebase() first")),
    )
}
