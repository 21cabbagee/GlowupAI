package com.glowup.ai.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.glowup.ai.domain.model.ConsentState
import com.glowup.ai.domain.model.EntitlementStatus
import com.glowup.ai.domain.model.Plan
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Local session identity + cached-session-shape state, backed by Jetpack DataStore Preferences.
 *
 * This is what fixes the single worst bug in the app: `ApiService.kt:150` calls `createUser()` on
 * every analysis because nothing persists the id `POST /api/users` returned, so every capture
 * orphans the previous history under a brand-new backend user. [SessionRepository] must always
 * check [userId] first and only call `createUser`/`authSession` when it is null.
 *
 * All keys live behind [GLOWUP_PREFERENCE_KEYS] and [clearSession] removes ONLY those keys. It
 * deliberately never calls `Preferences.edit { it.clear() }` — the web client's
 * `localStorage.clear()` destroyed unrelated data on the same origin, and this store must not
 * repeat that mistake even though today it is the only writer of this particular DataStore file.
 */
@Singleton
class SessionStore @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {

    private object Keys {
        val USER_ID = stringPreferencesKey("glowup_user_id")
        val FIREBASE_UID = stringPreferencesKey("glowup_firebase_uid")
        val PLAN = stringPreferencesKey("glowup_plan")
        val ENTITLEMENT_STATUS = stringPreferencesKey("glowup_entitlement_status")
        val CONSENT_STATE = stringPreferencesKey("glowup_consent_state")
        val ONBOARDING_STEP = stringPreferencesKey("glowup_onboarding_step")
        val ONBOARDING_COMPLETE = booleanPreferencesKey("glowup_onboarding_complete")
        val SELECTED_VERTICAL = stringPreferencesKey("glowup_selected_vertical")
        val THEME_PREFERENCE = stringPreferencesKey("glowup_theme_preference")
        val REMINDER_ENABLED = booleanPreferencesKey("glowup_reminder_enabled")
        val REMINDER_CADENCE_DAYS = intPreferencesKey("glowup_reminder_cadence_days")
        val REMINDER_NEXT_AT = stringPreferencesKey("glowup_reminder_next_at")
        val REMINDER_WINDOW_START = stringPreferencesKey("glowup_reminder_window_start")
        val REMINDER_WINDOW_END = stringPreferencesKey("glowup_reminder_window_end")
        val NOTIFICATION_PERMISSION_PROMPTED = booleanPreferencesKey("glowup_notification_permission_prompted")

        /** Every key this store owns. [clearSession] removes exactly this set — nothing else. */
        val ALL: Set<Preferences.Key<*>> = setOf(
            USER_ID, FIREBASE_UID, PLAN, ENTITLEMENT_STATUS, CONSENT_STATE, ONBOARDING_STEP,
            ONBOARDING_COMPLETE, SELECTED_VERTICAL, THEME_PREFERENCE, REMINDER_ENABLED,
            REMINDER_CADENCE_DAYS, REMINDER_NEXT_AT, REMINDER_WINDOW_START, REMINDER_WINDOW_END,
            NOTIFICATION_PERMISSION_PROMPTED,
        )
    }

    // -- Identity -------------------------------------------------------------------------------

    val userIdFlow: Flow<String?> = dataStore.data.map { it[Keys.USER_ID] }
    suspend fun userId(): String? = userIdFlow.first()
    suspend fun setUserId(userId: String) = dataStore.edit { it[Keys.USER_ID] = userId }

    val firebaseUidFlow: Flow<String?> = dataStore.data.map { it[Keys.FIREBASE_UID] }
    suspend fun setFirebaseUid(uid: String) = dataStore.edit { it[Keys.FIREBASE_UID] = uid }

    // -- Cached plan / entitlement (used ONLY as a cache key / offline hint — never as the
    // authoritative Premium check; that is always [com.glowup.ai.domain.model.Entitlement.isPremium]
    // derived from a fresh profile response). --------------------------------------------------

    val planFlow: Flow<Plan> = dataStore.data.map { Plan.fromRaw(it[Keys.PLAN]) }
    suspend fun plan(): Plan = planFlow.first()
    val entitlementStatusFlow: Flow<EntitlementStatus> =
        dataStore.data.map { EntitlementStatus.fromRaw(it[Keys.ENTITLEMENT_STATUS]) }

    suspend fun setEntitlement(plan: Plan, status: EntitlementStatus) = dataStore.edit {
        it[Keys.PLAN] = plan.name.lowercase()
        it[Keys.ENTITLEMENT_STATUS] = status.name.lowercase()
    }

    // -- Consent ----------------------------------------------------------------------------------

    val consentStateFlow: Flow<ConsentState> =
        dataStore.data.map { ConsentState.fromRaw(it[Keys.CONSENT_STATE]) }
    suspend fun setConsentState(state: ConsentState) =
        dataStore.edit { it[Keys.CONSENT_STATE] = state.name.lowercase() }

    // -- Onboarding -------------------------------------------------------------------------------

    val onboardingStepFlow: Flow<String?> = dataStore.data.map { it[Keys.ONBOARDING_STEP] }
    suspend fun setOnboardingStep(step: String) = dataStore.edit { it[Keys.ONBOARDING_STEP] = step }

    val onboardingCompleteFlow: Flow<Boolean> =
        dataStore.data.map { it[Keys.ONBOARDING_COMPLETE] ?: false }
    suspend fun setOnboardingComplete(complete: Boolean) =
        dataStore.edit { it[Keys.ONBOARDING_COMPLETE] = complete }

    // -- Vertical / theme / reminders --------------------------------------------------------------

    val selectedVerticalFlow: Flow<String> =
        dataStore.data.map { it[Keys.SELECTED_VERTICAL] ?: "skin" }
    suspend fun selectedVertical(): String = selectedVerticalFlow.first()
    suspend fun setSelectedVertical(vertical: String) =
        dataStore.edit { it[Keys.SELECTED_VERTICAL] = vertical }

    /** Raw string ("light"/"dark"/"system") — [core.design] owns the actual enum/mapping. */
    val themePreferenceFlow: Flow<String> = dataStore.data.map { it[Keys.THEME_PREFERENCE] ?: "system" }
    suspend fun setThemePreference(theme: String) = dataStore.edit { it[Keys.THEME_PREFERENCE] = theme }

    data class ReminderSettings(
        val enabled: Boolean,
        val cadenceDays: Int?,
        val nextAt: String?,
        val windowStart: String?,
        val windowEnd: String?,
    )

    val reminderSettingsFlow: Flow<ReminderSettings> = dataStore.data.map { prefs ->
        ReminderSettings(
            enabled = prefs[Keys.REMINDER_ENABLED] ?: true,
            cadenceDays = prefs[Keys.REMINDER_CADENCE_DAYS],
            nextAt = prefs[Keys.REMINDER_NEXT_AT],
            windowStart = prefs[Keys.REMINDER_WINDOW_START],
            windowEnd = prefs[Keys.REMINDER_WINDOW_END],
        )
    }

    /**
     * Called by [com.glowup.ai.data.repository.HomeRepository] whenever a real (non-polling)
     * `GET /engagement` or `GET /capture-guide` response comes back, so [ReminderWorker] always
     * schedules from the server's own cadence/window rather than a client-invented interval.
     */
    suspend fun setReminderSchedule(
        cadenceDays: Int?,
        nextAt: String?,
        windowStart: String? = null,
        windowEnd: String? = null,
    ) = dataStore.edit {
        if (cadenceDays == null) it.remove(Keys.REMINDER_CADENCE_DAYS) else it[Keys.REMINDER_CADENCE_DAYS] = cadenceDays
        if (nextAt == null) it.remove(Keys.REMINDER_NEXT_AT) else it[Keys.REMINDER_NEXT_AT] = nextAt
        if (windowStart == null) it.remove(Keys.REMINDER_WINDOW_START) else it[Keys.REMINDER_WINDOW_START] = windowStart
        if (windowEnd == null) it.remove(Keys.REMINDER_WINDOW_END) else it[Keys.REMINDER_WINDOW_END] = windowEnd
    }

    suspend fun setRemindersEnabled(enabled: Boolean) = dataStore.edit { it[Keys.REMINDER_ENABLED] = enabled }

    /** True after the one-time Android 13+ notification permission prompt was shown. */
    val notificationPermissionPromptedFlow: Flow<Boolean> =
        dataStore.data.map { it[Keys.NOTIFICATION_PERMISSION_PROMPTED] ?: false }
    suspend fun setNotificationPermissionPrompted(prompted: Boolean) = dataStore.edit {
        it[Keys.NOTIFICATION_PERMISSION_PROMPTED] = prompted
    }

    // -- Session teardown -------------------------------------------------------------------------

    /**
     * Removes ONLY the keys this store owns ([Keys.ALL]). Used on `400 user not found` (stale
     * local id — frontend-api-map.md "Startup and session recovery"), account deletion, and
     * explicit sign-out. Never a blanket `clear()` — see class doc.
     */
    suspend fun clearSession() = dataStore.edit { prefs ->
        Keys.ALL.forEach { key -> prefs.remove(key) }
    }
}
