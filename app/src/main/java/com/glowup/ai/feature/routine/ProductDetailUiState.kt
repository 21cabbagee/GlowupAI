package com.glowup.ai.feature.routine

import com.glowup.ai.domain.model.ConfoundCheck
import com.glowup.ai.domain.model.IngredientExplainer
import com.glowup.ai.domain.model.ProductDetail
import com.glowup.ai.domain.model.RoutineAction

sealed interface ProductDetailUiState {
    data object Loading : ProductDetailUiState
    data class Error(val message: String) : ProductDetailUiState
    data class Content(
        val detail: ProductDetail,

        val showEventSheet: Boolean = false,
        val eventInitialAction: RoutineAction = RoutineAction.START,
        val confoundCheck: ConfoundCheck? = null,
        val confoundCheckLoading: Boolean = false,
        val eventSubmitting: Boolean = false,
        val eventError: String? = null,
        /** Set after a successful submit whose response carried a non-null `confound_warning` —
         * shown as a persistent banner on the detail screen until dismissed. */
        val postSubmitWarning: ConfoundCheck? = null,
        val successMessage: String? = null,

        val explainerLoading: Boolean = false,
        val explainer: IngredientExplainer? = null,
        val explainerLocked: Boolean = false,
        val explainerError: String? = null,
    ) : ProductDetailUiState
}
