package com.glowup.ai.feature.routine

import com.glowup.ai.domain.model.Experiment
import com.glowup.ai.domain.model.PrimaryMetric
import com.glowup.ai.domain.model.Product

sealed interface ExperimentsListStatus {
    data object Loading : ExperimentsListStatus
    data class Content(val experiments: List<Experiment>) : ExperimentsListStatus
    /** `403 Experiments requires Premium` — a distinct upsell, never rendered as an empty list. */
    data object Locked : ExperimentsListStatus
    data class Error(val message: String) : ExperimentsListStatus
}

data class CreateExperimentState(
    val visible: Boolean = false,
    val name: String = "",
    val hypothesis: String = "",
    val primaryMetric: PrimaryMetric = PrimaryMetric.REDNESS_SCORE,
    val targetDays: String = "14",
    val productQuery: String = "",
    val productSearching: Boolean = false,
    val productResults: List<Product> = emptyList(),
    val selectedProduct: Product? = null,
    val pending: Boolean = false,
    val error: String? = null,
)

data class ExperimentsUiState(
    val listStatus: ExperimentsListStatus = ExperimentsListStatus.Loading,
    val create: CreateExperimentState = CreateExperimentState(),
    val navigateToExperimentId: String? = null,
)
