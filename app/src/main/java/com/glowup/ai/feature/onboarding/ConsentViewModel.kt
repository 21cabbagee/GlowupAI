package com.glowup.ai.feature.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.glowup.ai.core.util.GlowResult
import com.glowup.ai.data.repository.SessionRepository
import com.glowup.ai.domain.SessionState
import com.glowup.ai.domain.SessionStateMachine
import com.glowup.ai.domain.model.Profile
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

sealed interface ConsentUiState {
    data object Loading : ConsentUiState
    data class Content(val session: SessionState) : ConsentUiState
    data object Saving : ConsentUiState
    data class Error(val message: String) : ConsentUiState
}

@HiltViewModel
class ConsentViewModel @Inject constructor(
    private val sessionRepository: SessionRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow<ConsentUiState>(ConsentUiState.Loading)
    val uiState: StateFlow<ConsentUiState> = _uiState.asStateFlow()
    private val _navigationTarget = MutableStateFlow<GlowDestination?>(null)
    val navigationTarget: StateFlow<GlowDestination?> = _navigationTarget.asStateFlow()
    private var actionJob: Job? = null

    init { load() }
    fun consumeNavigationTarget() { _navigationTarget.value = null }
    fun retry() = load()

    private fun load() {
        actionJob?.cancel()
        actionJob = viewModelScope.launch {
            _uiState.value = ConsentUiState.Loading
            applyResult(sessionRepository.refreshProfile())
        }
    }

    fun decide(accept: Boolean) {
        if (_uiState.value == ConsentUiState.Saving) return
        actionJob?.cancel()
        actionJob = viewModelScope.launch {
            _uiState.value = ConsentUiState.Saving
            applyResult(sessionRepository.grantConsent(accept, POLICY_VERSION))
        }
    }

    fun continueToApp() {
        if ((_uiState.value as? ConsentUiState.Content)?.session is SessionState.ConsentDeclined) {
            _navigationTarget.value = GlowDestination.Home
        }
    }

    private fun applyResult(result: GlowResult<Profile>) {
        when (result) {
            is GlowResult.Success -> {
                val state = SessionStateMachine.onProfileResult(result)
                _uiState.value = ConsentUiState.Content(state)
                if (state !is SessionState.ConsentDeclined && state !is SessionState.ConsentRequired) {
                    _navigationTarget.value = destinationFor(state)
                }
            }
            is GlowResult.Failure -> _uiState.value = ConsentUiState.Error(result.error.toMessage())
        }
    }

    companion object { const val POLICY_VERSION = "2026-08-24" }
}