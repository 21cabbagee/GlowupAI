package com.glowup.ai.feature.achievements

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.glowup.ai.core.util.GlowResult
import com.glowup.ai.data.local.SessionStore
import com.glowup.ai.data.repository.AchievementRepository
import com.glowup.ai.data.repository.HomeRepository
import com.glowup.ai.domain.calculator.AchievementCalculator
import com.glowup.ai.domain.model.UserAchievement
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Achievements ViewModel
 * Manages full-screen achievement view
 */
@HiltViewModel
class AchievementsViewModel
    @Inject
    constructor(
        private val achievementRepository: AchievementRepository,
        private val homeRepository: HomeRepository,
        private val sessionStore: SessionStore,
    ) : ViewModel() {
        private val _state = MutableStateFlow<AchievementsUiState>(AchievementsUiState.Loading)
        val state: StateFlow<AchievementsUiState> = _state.asStateFlow()

        init {
            viewModelScope.launch { load() }
        }

        fun onRetryRequested() {
            viewModelScope.launch { load() }
        }

        private suspend fun load() {
            val userId = sessionStore.userId()
            if (userId == null) {
                _state.value = AchievementsUiState.Error("Sign in to see your achievements.")
                return
            }

            _state.value = AchievementsUiState.Loading

            // Get current dashboard to calculate achievements
            val dashboardResult = homeRepository.getDashboard(userId)
            val dashboardCached = (dashboardResult as? GlowResult.Success)?.data

            if (dashboardCached == null) {
                _state.value =
                    AchievementsUiState.Error(
                        "Failed to load achievements. Please try again.",
                    )
                return
            }

            // Calculate achievements
            val previouslyUnlocked = achievementRepository.getUnlockedIds(userId)
            val achievements =
                AchievementCalculator.calculateAchievements(
                    dashboard = dashboardCached.data,
                    previouslyUnlocked = previouslyUnlocked,
                )

            // Save achievements (without isNew flag for this view)
            val achievementsWithoutNew = achievements.map { it.copy(isNew = false) }
            achievementRepository.saveAchievements(userId, achievementsWithoutNew)

            _state.value =
                AchievementsUiState.Content(
                    achievements = achievementsWithoutNew,
                )
        }
    }

/**
 * UI State for Achievements Screen
 */
sealed interface AchievementsUiState {
    data object Loading : AchievementsUiState

    data class Error(
        val message: String,
    ) : AchievementsUiState

    data class Content(
        val achievements: List<UserAchievement>,
    ) : AchievementsUiState
}
