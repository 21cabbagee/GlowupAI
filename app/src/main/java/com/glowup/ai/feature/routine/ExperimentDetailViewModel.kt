package com.glowup.ai.feature.routine

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.glowup.ai.core.util.GlowResult
import com.glowup.ai.data.local.SessionStore
import com.glowup.ai.data.repository.ExperimentRepository
import com.glowup.ai.domain.model.ExperimentStatus
import com.glowup.ai.domain.model.ExperimentStatusRequest
import com.glowup.ai.feature.shell.GlowDestination
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Owns [GlowDestination.ExperimentDetail]: the experiment timeline, manual status transitions
 * (each behind an explicit confirm — never applied optimistically per frontend-api-map.md
 * "Ideal UI state: status transition confirmation ... Do not mark it complete optimistically
 * before the response"), and the `early_stop` callout.
 */
@HiltViewModel
class ExperimentDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val experimentRepository: ExperimentRepository,
    private val sessionStore: SessionStore,
) : ViewModel() {

    private val experimentId: String = savedStateHandle.toRoute<GlowDestination.ExperimentDetail>().experimentId

    private val _uiState = MutableStateFlow<ExperimentDetailUiState>(ExperimentDetailUiState.Loading)
    val uiState: StateFlow<ExperimentDetailUiState> = _uiState.asStateFlow()

    private var userId: String? = null

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.value = ExperimentDetailUiState.Loading
            val id = sessionStore.userId()
            userId = id
            if (id == null) {
                _uiState.value = ExperimentDetailUiState.Error("Sign in to view this experiment.")
                return@launch
            }
            when (val result = experimentRepository.getExperiment(id, experimentId)) {
                is GlowResult.Success -> _uiState.value = ExperimentDetailUiState.Content(result.data)
                is GlowResult.Failure -> _uiState.value = ExperimentDetailUiState.Error(result.error.toDisplayMessage())
            }
        }
    }

    fun requestStatusChange(status: ExperimentStatus) {
        _uiState.update { current ->
            if (current is ExperimentDetailUiState.Content) current.copy(confirmTarget = status, statusChangeError = null) else current
        }
    }

    fun cancelStatusChange() {
        _uiState.update { current ->
            if (current is ExperimentDetailUiState.Content) current.copy(confirmTarget = null) else current
        }
    }

    fun confirmStatusChange() {
        val current = _uiState.value as? ExperimentDetailUiState.Content ?: return
        val target = current.confirmTarget ?: return
        applyStatus(target)
    }

    /** The `early_stop` callout's action — apply the backend's own recommendation directly,
     * still behind the same confirm-and-refetch discipline as a manual transition. */
    fun applyEarlyStopRecommendation() {
        val current = _uiState.value as? ExperimentDetailUiState.Content ?: return
        val recommended = current.experiment.earlyStop?.recommendedStatus ?: return
        applyStatus(recommended)
    }

    private fun applyStatus(status: ExperimentStatus) {
        val current = _uiState.value as? ExperimentDetailUiState.Content ?: return
        if (current.statusChangePending) return
        val id = userId ?: return
        viewModelScope.launch {
            _uiState.update { c -> (c as? ExperimentDetailUiState.Content)?.copy(statusChangePending = true, statusChangeError = null) ?: c }
            when (val result = experimentRepository.setExperimentStatus(experimentId, ExperimentStatusRequest(id, status))) {
                is GlowResult.Success -> _uiState.value = ExperimentDetailUiState.Content(result.data)
                is GlowResult.Failure -> _uiState.update { c ->
                    (c as? ExperimentDetailUiState.Content)?.copy(
                        statusChangePending = false,
                        confirmTarget = null,
                        statusChangeError = result.error.toDisplayMessage(),
                    ) ?: c
                }
            }
        }
    }
}
