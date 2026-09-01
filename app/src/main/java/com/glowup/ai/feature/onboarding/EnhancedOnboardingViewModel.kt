package com.glowup.ai.feature.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.glowup.ai.core.util.GlowResult
import com.glowup.ai.data.local.SessionStore
import com.glowup.ai.data.repository.SessionRepository
import com.glowup.ai.domain.SessionState
import com.glowup.ai.domain.SessionStateMachine
import com.glowup.ai.feature.shell.GlowDestination
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface EnhancedOnboardingUiState {
    data object Loading : EnhancedOnboardingUiState

    data object Content : EnhancedOnboardingUiState

    data class Error(
        val message: String,
    ) : EnhancedOnboardingUiState
}

/**
 * ViewModel for the enhanced onboarding flow. Manages onboarding state and navigation
 * to the appropriate destination after completion.
 */
@HiltViewModel
class EnhancedOnboardingViewModel
    @Inject
    constructor(
        private val sessionRepository: SessionRepository,
        private val sessionStore: SessionStore,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow<EnhancedOnboardingUiState>(EnhancedOnboardingUiState.Loading)
        val uiState: StateFlow<EnhancedOnboardingUiState> = _uiState.asStateFlow()

        private val _navigationTarget = MutableStateFlow<GlowDestination?>(null)
        val navigationTarget: StateFlow<GlowDestination?> = _navigationTarget.asStateFlow()

        private var loadJob: Job? = null

        init {
            checkOnboardingStatus()
        }

        fun consumeNavigationTarget() {
            _navigationTarget.value = null
        }

        fun retry() {
            checkOnboardingStatus()
        }

        /**
         * Check if onboarding was already completed. If so, navigate to the appropriate
         * destination based on session state. Otherwise, show the onboarding content.
         */
        private fun checkOnboardingStatus() {
            loadJob?.cancel()
            loadJob =
                viewModelScope.launch {
                    _uiState.value = EnhancedOnboardingUiState.Loading

                    // Check if onboarding is already complete
                    sessionStore.onboardingCompleteFlow.collect { complete ->
                        if (complete) {
                            // Onboarding already done, navigate to appropriate destination
                            navigateBasedOnSessionState()
                        } else {
                            // Show onboarding content
                            _uiState.value = EnhancedOnboardingUiState.Content
                        }
                    }
                }
        }

        /**
         * Complete the onboarding flow and mark it as done in persistent storage.
         * Then navigate to the appropriate next destination based on consent state.
         */
        fun completeOnboarding() {
            viewModelScope.launch {
                try {
                    // Mark onboarding as complete
                    sessionStore.setOnboardingComplete(true)

                    // Navigate based on current session state
                    navigateBasedOnSessionState()
                } catch (e: Exception) {
                    _uiState.value =
                        EnhancedOnboardingUiState.Error(
                            "Failed to save onboarding status: ${e.message}",
                        )
                }
            }
        }

        /**
         * Skip onboarding (still marks as complete, but navigates immediately)
         */
        fun skipOnboarding() {
            viewModelScope.launch {
                try {
                    sessionStore.setOnboardingComplete(true)
                    navigateBasedOnSessionState()
                } catch (e: Exception) {
                    _uiState.value =
                        EnhancedOnboardingUiState.Error(
                            "Failed to skip onboarding: ${e.message}",
                        )
                }
            }
        }

        /**
         * Determine where to navigate based on the current session state.
         * Priority order:
         * 1. If consent is required or declined -> Consent screen
         * 2. If no captures -> Capture screen (baseline photo)
         * 3. Otherwise -> Home
         */
        private suspend fun navigateBasedOnSessionState() {
            when (val result = sessionRepository.refreshProfile()) {
                is GlowResult.Success -> {
                    val sessionState = SessionStateMachine.onProfileResult(result)

                    val destination =
                        when (sessionState) {
                            is SessionState.ConsentRequired,
                            is SessionState.ConsentDeclined,
                            -> {
                                GlowDestination.Consent
                            }

                            is SessionState.Ready -> {
                                // Check if user has any captures
                                // If not, suggest taking baseline photo
                                // For now, navigate to Home - user can navigate to Capture from there
                                GlowDestination.Home
                            }

                            else -> {
                                GlowDestination.Home
                            }
                        }

                    _navigationTarget.value = destination
                }

                is GlowResult.Failure -> {
                    _uiState.value =
                        EnhancedOnboardingUiState.Error(
                            "Failed to load profile: ${result.error}",
                        )
                }
            }
        }
    }
