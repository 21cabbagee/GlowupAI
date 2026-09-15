package com.glowup.ai.feature.account

import com.glowup.ai.BuildConfig
import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.glowup.ai.data.local.SessionStore
import com.glowup.ai.data.repository.SessionRepository
import com.glowup.ai.data.work.WorkScheduler
import com.glowup.ai.feature.auth.SupabaseAuthGateway
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalTime
import javax.inject.Inject

/** Theme preference options */
enum class ThemePreference(
    val storageValue: String,
    val label: String,
) {
    LIGHT("light", "Light"),
    DARK("dark", "Dark"),
    SYSTEM("system", "System"),
    ;

    companion object {
        fun fromStorage(raw: String): ThemePreference = entries.firstOrNull { it.storageValue == raw } ?: SYSTEM
    }
}

/** Font size preference options */
enum class FontSize(
    val storageValue: String,
    val label: String,
) {
    SMALL("small", "Small"),
    MEDIUM("medium", "Medium"),
    LARGE("large", "Large"),
    ;

    companion object {
        fun fromStorage(raw: String): FontSize = entries.firstOrNull { it.storageValue == raw } ?: MEDIUM
    }
}

/** Complete settings UI state */
data class SettingsUiState(
    // Account
    val userEmail: String? = null,
    val userDisplayName: String? = null,
    val signingOut: Boolean = false,
    // Notifications
    val dailyCaptureReminder: Boolean = true,
    val dailyCaptureTime: LocalTime = LocalTime.of(20, 0),
    val streakWarnings: Boolean = true,
    val weeklyRecap: Boolean = true,
    val achievementCelebrations: Boolean = true,
    // Data & Privacy
    // Display
    val theme: ThemePreference = ThemePreference.SYSTEM,
    val fontSize: FontSize = FontSize.MEDIUM,
    val reduceAnimations: Boolean = false,
    // Debug
    val apiEndpoint: String = "Local",
)

/**
 * Complete settings ViewModel managing all app configuration.
 * Persists all settings to DataStore and coordinates with WorkManager for notifications.
 */
@HiltViewModel
class SettingsViewModel
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val sessionStore: SessionStore,
        private val sessionRepository: SessionRepository,
        private val workScheduler: WorkScheduler,
        private val authGateway: SupabaseAuthGateway,
    ) : ViewModel() {
        private val _signingOut = MutableStateFlow(false)

        val uiState: StateFlow<SettingsUiState> =
            combine(
                sessionStore.themePreferenceFlow,
                sessionStore.reminderSettingsFlow,
                sessionStore.reminderTimeFlow,
                sessionStore.streakWarningsFlow,
                sessionStore.weeklyRecapFlow,
                sessionStore.achievementCelebrationsFlow,
                sessionStore.fontSizeFlow,
                sessionStore.reduceAnimationsFlow,
                _signingOut,
            ) { flows ->
                val theme = flows[0] as String
                val reminders = flows[1] as SessionStore.ReminderSettings
                val reminderTime = flows[2] as SessionStore.ReminderTime
                val streakWarnings = flows[3] as Boolean
                val weeklyRecap = flows[4] as Boolean
                val achievementCelebrations = flows[5] as Boolean
                val fontSize = flows[6] as String
                val reduceAnimations = flows[7] as Boolean
                val signingOut = flows[8] as Boolean

                // Get the current Supabase Auth user info.
                val supabaseUser = authGateway.currentUser()

                SettingsUiState(
                    userEmail = supabaseUser?.email,
                    userDisplayName = supabaseUser?.displayName,
                    signingOut = signingOut,
                    theme = ThemePreference.fromStorage(theme),
                    dailyCaptureReminder = reminders.enabled,
                    dailyCaptureTime = LocalTime.of(reminderTime.hour, reminderTime.minute),
                    streakWarnings = streakWarnings,
                    weeklyRecap = weeklyRecap,
                    achievementCelebrations = achievementCelebrations,
                    fontSize = FontSize.fromStorage(fontSize),
                    reduceAnimations = reduceAnimations,
                )
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

        private val _signedOut = MutableStateFlow(false)
        val signedOut: StateFlow<Boolean> = _signedOut.asStateFlow()

        // --- Theme ---

        fun setTheme(theme: ThemePreference) {
            viewModelScope.launch {
                sessionStore.setThemePreference(theme.storageValue)
            }
        }

        // --- Display Settings ---

        fun setFontSize(size: FontSize) {
            viewModelScope.launch {
                sessionStore.setFontSize(size.storageValue)
            }
        }

        fun setReduceAnimations(enabled: Boolean) {
            viewModelScope.launch {
                sessionStore.setReduceAnimations(enabled)
            }
        }

        // --- Notifications ---

        fun setDailyCaptureReminder(enabled: Boolean) {
            viewModelScope.launch {
                sessionStore.setRemindersEnabled(enabled)
                if (enabled) {
                    schedulePersistedReminder()
                } else {
                    workScheduler.cancelReminder()
                }
            }
        }

        fun setDailyCaptureTime(time: LocalTime) {
            viewModelScope.launch {
                sessionStore.setReminderTime(time.hour, time.minute)
                // Reschedule reminder with new time
                val settings = sessionStore.reminderSettingsFlow.first()
                if (settings.enabled) {
                    schedulePersistedReminder()
                }
            }
        }

        fun setStreakWarnings(enabled: Boolean) {
            viewModelScope.launch {
                sessionStore.setStreakWarnings(enabled)
            }
        }

        fun setWeeklyRecap(enabled: Boolean) {
            viewModelScope.launch {
                sessionStore.setWeeklyRecap(enabled)
            }
        }

        fun setAchievementCelebrations(enabled: Boolean) {
            viewModelScope.launch {
                sessionStore.setAchievementCelebrations(enabled)
            }
        }

        // --- Data & Privacy ---

        // --- Account Actions ---

        fun signOut() {
            if (_signingOut.value) return
            viewModelScope.launch {
                _signingOut.value = true
                sessionRepository.clearSession()
                authGateway.signOut()
                _signedOut.value = true
            }
        }

        // --- Debug Functions ---

        fun clearCache() {
            viewModelScope.launch {
                try {
                    context.cacheDir.deleteRecursively()
                    Log.d("SettingsViewModel", "Cache cleared")
                } catch (e: Exception) {
                    Log.e("SettingsViewModel", "Failed to clear cache", e)
                }
            }
        }

        fun forceCrash(): Unit = throw RuntimeException("Test crash from Settings")

        fun viewLogs() {
            Log.d("SettingsViewModel", "View logs requested")
        }

        fun setApiEndpoint(endpoint: String) {
            viewModelScope.launch {
                if (BuildConfig.DEBUG) Log.d("SettingsViewModel", "API endpoint is fixed per build; requested=$endpoint")
            }
        }

        // --- Private Helpers ---

        private suspend fun schedulePersistedReminder() {
            val settings = sessionStore.reminderSettingsFlow.first()
            val delayMillis =
                settings.nextAt
                    ?.let(::parseIsoToEpochMillis)
                    ?.let { it - System.currentTimeMillis() }
                    ?.takeIf { it > 0 }
                    ?: settings.cadenceDays?.let {
                        java.util.concurrent.TimeUnit.DAYS
                            .toMillis(it.toLong())
                    }

            if (delayMillis != null && delayMillis > 0) {
                workScheduler.scheduleReminder(delayMillis)
            } else {
                workScheduler.cancelReminder()
            }
        }

        private fun parseIsoToEpochMillis(iso: String): Long? =
            runCatching {
                java.text
                    .SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.US)
                    .apply {
                        timeZone = java.util.TimeZone.getTimeZone("UTC")
                    }.parse(iso.take(19))
                    ?.time
            }.getOrNull()
    }
