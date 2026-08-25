package com.glowup.ai.feature.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.glowup.ai.data.local.SessionStore
import com.glowup.ai.data.repository.SessionRepository
import com.glowup.ai.data.work.WorkScheduler
import com.glowup.ai.feature.auth.FirebaseAuthGateway
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit

/** Raw values persisted via [SessionStore.themePreferenceFlow] — light/dark/system. */
enum class ThemePreference(val storageValue: String, val label: String) {
    LIGHT("light", "Light"),
    DARK("dark", "Dark"),
    SYSTEM("system", "System default");

    companion object {
        fun fromStorage(raw: String): ThemePreference = entries.firstOrNull { it.storageValue == raw } ?: SYSTEM
    }
}

/** Backs [com.glowup.ai.feature.shell.GlowDestination.Settings]. */
data class SettingsUiState(
    val theme: ThemePreference = ThemePreference.SYSTEM,
    val remindersEnabled: Boolean = true,
    val reminderCadenceDays: Int = 3,
    val signingOut: Boolean = false,
)

/** Selectable reminder cadences shown as chips. */
val REMINDER_CADENCE_OPTIONS: List<Int> = listOf(1, 3, 7, 14)

/**
 * Every row here actually persists to [SessionStore] and is read back on the next launch — the
 * bug this task fixes is ANDROID_PLAN.md 3.7's "today all four settings rows are `onClick = {}`".
 * Sign-out is the one addition beyond the four settings rows (theme, reminder cadence,
 * notifications) that the task calls out explicitly.
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val sessionStore: SessionStore,
    private val sessionRepository: SessionRepository,
    private val workScheduler: WorkScheduler,
) : ViewModel() {

    private val _signingOut = MutableStateFlow(false)

    val uiState: StateFlow<SettingsUiState> = combine(
        sessionStore.themePreferenceFlow,
        sessionStore.reminderSettingsFlow,
        _signingOut,
    ) { theme, reminders, signingOut ->
        SettingsUiState(
            theme = ThemePreference.fromStorage(theme),
            remindersEnabled = reminders.enabled,
            reminderCadenceDays = reminders.cadenceDays ?: 3,
            signingOut = signingOut,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    private val _signedOut = MutableStateFlow(false)
    /** One-shot: the Settings screen navigates to Welcome and clears the back stack when this
     * flips true. */
    val signedOut: StateFlow<Boolean> = _signedOut.asStateFlow()

    fun setTheme(theme: ThemePreference) {
        viewModelScope.launch { sessionStore.setThemePreference(theme.storageValue) }
    }

    fun setRemindersEnabled(enabled: Boolean) {
        viewModelScope.launch {
            sessionStore.setRemindersEnabled(enabled)
            if (enabled) {
                schedulePersistedReminder()
            } else {
                workScheduler.cancelReminder()
            }
        }
    }

    fun setReminderCadenceDays(days: Int) {
        viewModelScope.launch {
            sessionStore.setReminderSchedule(cadenceDays = days, nextAt = null)
            if (sessionStore.reminderSettingsFlow.first().enabled) {
                // The user explicitly changed the cadence, so do not retain an older server
                // timestamp for this next reminder. The next Home refresh can replace it with a
                // newer server-authoritative timestamp.
                workScheduler.scheduleReminder(TimeUnit.DAYS.toMillis(days.toLong()))
            }
        }
    }

    /**
     * Uses the server-provided next timestamp when available and falls back to the server-provided
     * cadence. Settings changes must never leave a disabled reminder in WorkManager or invent a
     * client-only cadence when no schedule has been supplied.
     */
    private suspend fun schedulePersistedReminder() {
        val settings = sessionStore.reminderSettingsFlow.first()
        val delayMillis = settings.nextAt
            ?.let(::parseIsoToEpochMillis)
            ?.let { it - System.currentTimeMillis() }
            ?.takeIf { it > 0 }
            ?: settings.cadenceDays?.let { TimeUnit.DAYS.toMillis(it.toLong()) }

        if (delayMillis != null && delayMillis > 0) {
            workScheduler.scheduleReminder(delayMillis)
        } else {
            workScheduler.cancelReminder()
        }
    }

    private fun parseIsoToEpochMillis(iso: String): Long? = runCatching {
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }.parse(iso.take(19))?.time
    }.getOrNull()

    /** Firebase sign-out + clearing only GlowUp's own session keys — see
     * [SessionRepository.clearSession] / [SessionStore.clearSession]. */
    fun signOut() {
        if (_signingOut.value) return
        viewModelScope.launch {
            _signingOut.value = true
            sessionRepository.clearSession()
            FirebaseAuthGateway.signOut()
            _signedOut.value = true
        }
    }
}
