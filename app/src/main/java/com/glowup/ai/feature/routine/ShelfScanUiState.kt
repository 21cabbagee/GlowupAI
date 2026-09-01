package com.glowup.ai.feature.routine

import com.glowup.ai.domain.model.Product

data class ShelfScanCandidateUi(
    val name: String,
    val category: String,
    val ingredients: List<String>,
    val checked: Boolean,
)

sealed interface ShelfScanUiState {
    /** Nothing submitted yet — waiting for the user to pick a shelf photo. */
    data object Idle : ShelfScanUiState

    data object Uploading : ShelfScanUiState

    /** `~1.5s` poll of `GET /shelf-scan/{jobId}` while `status` is `queued`/`running`. */
    data class Polling(
        val message: String,
    ) : ShelfScanUiState

    /**
     * Job completed. [candidates] is empty (with [message] explaining why — the vision provider
     * is unconfigured server-side) exactly when the backend has nothing to offer; the screen must
     * always show the manual "add product" fallback in that case, never a dead end.
     */
    data class Ready(
        val jobId: String,
        val candidates: List<ShelfScanCandidateUi>,
        val message: String?,
        val confirming: Boolean = false,
        val confirmError: String? = null,
        val showManualAdd: Boolean = false,
        val manualAddPending: Boolean = false,
        val manualAddError: String? = null,
    ) : ShelfScanUiState

    data class Done(
        val createdProducts: List<Product>,
    ) : ShelfScanUiState

    data class Error(
        val message: String,
    ) : ShelfScanUiState
}
