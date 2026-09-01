package com.glowup.ai.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.glowup.ai.core.util.GlowResult
import com.glowup.ai.data.local.SessionStore
import com.glowup.ai.data.remote.ApiError
import com.glowup.ai.data.repository.AchievementRepository
import com.glowup.ai.data.repository.HomeRepository
import com.glowup.ai.data.telemetry.Telemetry
import com.glowup.ai.data.telemetry.TelemetryEvent
import com.glowup.ai.domain.StreakCalculator
import com.glowup.ai.domain.calculator.AchievementCalculator
import com.glowup.ai.domain.model.CheckInCreateRequest
import com.glowup.ai.domain.model.CheckInRoutineState
import com.glowup.ai.domain.model.CheckInSkinFeel
import com.glowup.ai.domain.model.PrimaryMetric
import com.glowup.ai.domain.model.Streak
import com.glowup.ai.domain.model.UserAchievement
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * `GET /dashboard` is fetched as the single initial snapshot — it already embeds `weekly_recap`
 * and `check_ins` (verified against `backend/glowupai/complete_api.py` / `backend/web/lib/api.ts`
 * `Dashboard` shape), so this ViewModel never makes separate `weekly-recap`/`check-ins` round
 * trips just to render Home. `GET /history` is fetched alongside it purely for the trend chart's
 * extra fields (`noise_floor`, `redness_delta`, per-item `capture_quality`) that the embedded
 * `dashboard.history` does not carry as richly; if it fails, the embedded copy is the fallback.
 *
 * Trap #7: `GET /dashboard` and `GET /engagement` mutate server state (recalculated verdict copy,
 * a written reminder row) — [load] therefore only ever runs from [init], an explicit
 * [onRefreshRequested]/[onRetryRequested] tap, or after a mutation this screen owns
 * ([onCheckInSubmitted]). Nothing here is on a timer, and nothing calls `GET /engagement`
 * directly — [HomeRepository] already folds its cadence into the embedded
 * `dashboard.engagement`/`capture-guide` fields, so Home does not need a second side-effecting
 * call just to render a streak ring.
 */
@HiltViewModel
class HomeViewModel
    @Inject
    constructor(
        private val homeRepository: HomeRepository,
        private val achievementRepository: AchievementRepository,
        private val sessionStore: SessionStore,
        private val telemetry: Telemetry,
    ) : ViewModel() {
        private val _state = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
        val state: StateFlow<HomeUiState> = _state.asStateFlow()

        private var freezeDayUsedThisWeek = false

        init {
            viewModelScope.launch { load(initial = true) }
        }

        /** The one explicit, user-visible trigger this screen offers for re-hitting `GET /dashboard`
         * (a top-bar refresh action) — never called from a timer. */
        fun onRefreshRequested() {
            viewModelScope.launch { load(initial = false) }
        }

        fun onRetryRequested() {
            viewModelScope.launch { load(initial = true) }
        }

        fun onMetricSelected(metric: PrimaryMetric) {
            _state.update { current -> (current as? HomeUiState.Content)?.copy(selectedMetric = metric) ?: current }
        }

        fun onCheckInSheetRequested() {
            _state.update { current -> (current as? HomeUiState.Content)?.copy(checkInSheetVisible = true, checkInError = null) ?: current }
        }

        fun onCheckInSheetDismissed() {
            _state.update { current ->
                (current as? HomeUiState.Content)?.copy(checkInSheetVisible = false, checkInError = null) ?: current
            }
        }

        fun onCheckInSubmitted(
            routineState: CheckInRoutineState,
            skinFeel: CheckInSkinFeel,
            note: String?,
        ) {
            if (_state.value !is HomeUiState.Content) return
            viewModelScope.launch {
                val userId = sessionStore.userId() ?: return@launch
                _state.update { s -> (s as? HomeUiState.Content)?.copy(checkInSubmitting = true, checkInError = null) ?: s }

                val request =
                    CheckInCreateRequest(
                        routineState = routineState,
                        skinFeel = skinFeel,
                        note = note?.trim()?.ifBlank { null },
                    )
                when (val result = homeRepository.createCheckIn(userId, request)) {
                    is GlowResult.Success -> {
                        load(initial = false)
                    }

                    is GlowResult.Failure -> {
                        _state.update { s ->
                            (s as? HomeUiState.Content)?.copy(checkInSubmitting = false, checkInError = result.error.toHomeMessage()) ?: s
                        }
                    }
                }
            }
        }

        fun onFreezeDayUsed() {
            freezeDayUsedThisWeek = true
            telemetry.track(TelemetryEvent.STREAK_FREEZE_DAY_USED)
            viewModelScope.launch { load(initial = false) }
        }

        private suspend fun load(initial: Boolean) {
            val userId = sessionStore.userId()
            if (userId == null) {
                _state.value = HomeUiState.Error("Sign in to see your dashboard.")
                return
            }

            if (initial) {
                _state.value = HomeUiState.Loading
            } else {
                _state.update { s -> (s as? HomeUiState.Content)?.copy(isRefreshing = true) ?: s }
            }

            val dashboardResult = homeRepository.getDashboard(userId)
            val historyResult = homeRepository.getHistory(userId)

            val dashboardCached = (dashboardResult as? GlowResult.Success)?.data
            if (dashboardCached == null) {
                val error = (dashboardResult as GlowResult.Failure).error
                _state.value = HomeUiState.Error(error.toHomeMessage())
                return
            }

            val historyCached = (historyResult as? GlowResult.Success)?.data
            val previousMetric = (_state.value as? HomeUiState.Content)?.selectedMetric ?: PrimaryMetric.REDNESS_SCORE

            // Calculate streak from history items (Capture is typealias for HistoryItem)
            val historyForStreak = historyCached?.data ?: dashboardCached.data.history
            val streak =
                StreakCalculator.calculateStreak(
                    captures = historyForStreak,
                    freezeDayUsedThisWeek = freezeDayUsedThisWeek,
                )

            // Check achievements
            val previouslyUnlocked = achievementRepository.getUnlockedIds(userId)
            val achievements =
                AchievementCalculator.calculateAchievements(
                    dashboard = dashboardCached.data,
                    previouslyUnlocked = previouslyUnlocked,
                )
            achievementRepository.saveAchievements(userId, achievements)

            // Find newly unlocked achievements for celebration
            val newlyUnlocked = achievements.filter { it.isNew }
            val celebrationAchievement = newlyUnlocked.firstOrNull()

            _state.value =
                HomeUiState.Content(
                    dashboard = dashboardCached.data,
                    dashboardStale = dashboardCached.stale,
                    dashboardRefreshError = dashboardCached.refreshError?.toHomeMessage(),
                    // GET /history failing is not fatal to the screen: the dashboard snapshot embeds its
                    // own history[] (frontend-api-map.md "GET /dashboard" response fields), used here as
                    // the fallback so the trend chart still has something to draw.
                    history = historyCached?.data ?: dashboardCached.data.history,
                    historyStale = historyCached?.stale ?: false,
                    historyError = (historyResult as? GlowResult.Failure)?.error?.toHomeMessage(),
                    isRefreshing = false,
                    selectedMetric = previousMetric,
                    checkInSheetVisible = false,
                    checkInSubmitting = false,
                    checkInError = null,
                    streak = streak,
                    achievements = achievements,
                    celebrationAchievement = celebrationAchievement,
                )
        }

        fun onAchievementCelebrationDismissed() {
            _state.update { current ->
                (current as? HomeUiState.Content)?.copy(celebrationAchievement = null) ?: current
            }
        }
    }

/** Home-specific copy for a normalised [ApiError] — not exhaustive UX for every error family
 * (that lives closer to the mutation that can actually fail each way), just legible fallback text
 * for whatever this screen's two GETs can plausibly return. */
internal fun ApiError.toHomeMessage(): String =
    when (this) {
        is ApiError.Unauthorized -> "Please sign in again to see your dashboard."
        is ApiError.ConsentRequired -> "Photo consent is required before this can show capture data."
        is ApiError.PremiumRequired -> "$feature requires Premium."
        is ApiError.CaptureQualityRejected -> "Your last capture needs a retake before this can update."
        is ApiError.Validation -> fields.values.firstOrNull() ?: "That request wasn't valid."
        is ApiError.NotFound -> "We couldn't find your account data. Try signing in again."
        is ApiError.Conflict -> message
        is ApiError.Network -> "You're offline. Check your connection and try again."
        is ApiError.Server -> "Our servers had a problem loading this. Try again shortly."
        is ApiError.Unknown -> "Something unexpected happened loading your dashboard."
    }
