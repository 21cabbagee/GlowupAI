package com.glowup.ai.feature.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.glowup.ai.core.util.GlowResult
import com.glowup.ai.data.repository.SessionRepository
import com.glowup.ai.domain.SessionState
import com.glowup.ai.domain.SessionStateMachine
import com.glowup.ai.domain.model.Profile
import com.glowup.ai.domain.model.ProfileUpdateRequest
import com.glowup.ai.feature.auth.destinationFor
import com.glowup.ai.feature.auth.toMessage
import com.glowup.ai.feature.shell.GlowDestination
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface OnboardingUiState {
    data object Loading : OnboardingUiState
    data object Content : OnboardingUiState
    data object Saving : OnboardingUiState
    data class Error(val message: String) : OnboardingUiState
}

data class OnboardingFormState(
    val displayName: String = "",
    val skinType: String? = null,
    val goals: Set<String> = emptySet(),
    val experienceLevel: String? = null,
)

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val sessionRepository: SessionRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow<OnboardingUiState>(OnboardingUiState.Loading)
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()
    private val _form = MutableStateFlow(OnboardingFormState())
    val form: StateFlow<OnboardingFormState> = _form.asStateFlow()
    private val _navigationTarget = MutableStateFlow<GlowDestination?>(null)
    val navigationTarget: StateFlow<GlowDestination?> = _navigationTarget.asStateFlow()
    private var loadJob: Job? = null

    init { loadProfile() }
    fun consumeNavigationTarget() { _navigationTarget.value = null }
    fun retry() = loadProfile()

    private fun loadProfile() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _uiState.value = OnboardingUiState.Loading
            when (val result = sessionRepository.refreshProfile()) {
                is GlowResult.Success -> {
                    val state = SessionStateMachine.onProfileResult(result)
                    val complete = result.data.experienceProfile?.onboardingComplete == true
                    if (complete) {
                        _navigationTarget.value = destinationFor(state) ?: GlowDestination.Consent
                    } else {
                        prefill(result.data)
                        _uiState.value = OnboardingUiState.Content
                    }
                }
                is GlowResult.Failure -> _uiState.value = OnboardingUiState.Error(result.error.toMessage())
            }
        }
    }

    private fun prefill(profile: Profile) {
        val experience = profile.experienceProfile
        _form.value = OnboardingFormState(
            displayName = experience?.displayName.orEmpty().take(MAX_DISPLAY_NAME_LENGTH),
            skinType = experience?.skinType ?: profile.user.skinType,
            goals = experience?.goals.orEmpty().toSet(),
            experienceLevel = experience?.experienceLevel,
        )
    }

    fun updateDisplayName(value: String) {
        _form.value = _form.value.copy(displayName = value.take(MAX_DISPLAY_NAME_LENGTH))
    }
    fun setSkinType(value: String) {
        _form.value = _form.value.copy(skinType = value.takeIf { it != _form.value.skinType })
    }
    fun toggleGoal(goal: String) {
        val current = _form.value
        val next = if (goal in current.goals) current.goals - goal else current.goals + goal
        _form.value = current.copy(goals = next)
    }
    fun setExperienceLevel(value: String) {
        _form.value = _form.value.copy(experienceLevel = value.takeIf { it != _form.value.experienceLevel })
    }

    fun submit() {
        if (_uiState.value == OnboardingUiState.Saving) return
        val current = _form.value
        viewModelScope.launch {
            _uiState.value = OnboardingUiState.Saving
            val result = sessionRepository.updateProfile(
                ProfileUpdateRequest(
                    displayName = current.displayName.trim().takeIf { it.isNotEmpty() },
                    skinType = current.skinType,
                    goals = current.goals.sorted(),
                    experienceLevel = current.experienceLevel,
                    onboardingComplete = true,
                ),
            )
            when (result) {
                is GlowResult.Success -> {
                    val state = SessionStateMachine.onProfileResult(result)
                    _uiState.value = OnboardingUiState.Content
                    _navigationTarget.value = destinationFor(state) ?: GlowDestination.Consent
                }
                is GlowResult.Failure -> _uiState.value = OnboardingUiState.Error(result.error.toMessage())
            }
        }
    }

    companion object { private const val MAX_DISPLAY_NAME_LENGTH = 40 }
}