package com.glowup.ai.feature.routine

import com.glowup.ai.domain.model.DashboardRoutineEvent
import com.glowup.ai.domain.model.Product

/** State for [GlowDestination.Routine][com.glowup.ai.feature.shell.GlowDestination.Routine]:
 * product search/lookup/create entry points plus the routine-change timeline. */
data class RoutineUiState(
    val query: String = "",
    val searching: Boolean = false,
    val searchResults: List<Product> = emptyList(),
    val searchError: String? = null,
    val hasSearched: Boolean = false,

    val barcode: String = "",
    val barcodeLookupPending: Boolean = false,
    val barcodeLookupError: String? = null,

    val timelineLoading: Boolean = true,
    val timeline: List<DashboardRoutineEvent> = emptyList(),
    val timelineError: String? = null,

    val showAddProductSheet: Boolean = false,
    val addProductPrefillBarcode: String? = null,
    val addProductPending: Boolean = false,
    val addProductError: String? = null,

    /** Set right after a successful create/lookup so the screen can navigate once, then cleared. */
    val navigateToProductId: String? = null,
)
