package com.glowup.ai.feature.routine

import com.glowup.ai.domain.model.Experiment
import com.glowup.ai.domain.model.ExperimentStatus

sealed interface ExperimentDetailUiState {
    data object Loading : ExperimentDetailUiState
    data class Error(val message: String) : ExperimentDetailUiState
    data class Content(
        val experiment: Experiment,
        /** Status the user tapped, pending an explicit confirm dialog — never applied optimistically. */
        val confirmTarget: ExperimentStatus? = null,
        val statusChangePending: Boolean = false,
        val statusChangeError: String? = null,
    ) : ExperimentDetailUiState
}
