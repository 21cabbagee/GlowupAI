package com.glowup.ai.feature.routine

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.glowup.ai.core.util.GlowResult
import com.glowup.ai.data.local.SessionStore
import com.glowup.ai.data.remote.ApiError
import com.glowup.ai.data.repository.RoutineRepository
import com.glowup.ai.domain.model.RoutineAction
import com.glowup.ai.domain.model.RoutineEventRequest
import com.glowup.ai.feature.shell.GlowDestination
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Owns [GlowDestination.ProductDetail]: product detail, the Premium-gated ingredient explainer,
 * and the `start`/`stop`/`change` routine-event form with its pre-submit confound check.
 */
@HiltViewModel
class ProductDetailViewModel
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        private val routineRepository: RoutineRepository,
        private val sessionStore: SessionStore,
    ) : ViewModel() {
        private val productId: String = savedStateHandle.toRoute<GlowDestination.ProductDetail>().productId

        private val _uiState = MutableStateFlow<ProductDetailUiState>(ProductDetailUiState.Loading)
        val uiState: StateFlow<ProductDetailUiState> = _uiState.asStateFlow()

        init {
            load()
        }

        fun load() {
            viewModelScope.launch {
                _uiState.value = ProductDetailUiState.Loading
                val userId = sessionStore.userId()
                if (userId == null) {
                    _uiState.value = ProductDetailUiState.Error("Sign in to view product details.")
                    return@launch
                }
                when (val result = routineRepository.getProduct(productId, userId)) {
                    is GlowResult.Success -> _uiState.value = ProductDetailUiState.Content(detail = result.data)
                    is GlowResult.Failure -> _uiState.value = ProductDetailUiState.Error(result.error.toDisplayMessage())
                }
            }
        }

        private inline fun updateContent(block: (ProductDetailUiState.Content) -> ProductDetailUiState.Content) {
            _uiState.update { current -> if (current is ProductDetailUiState.Content) block(current) else current }
        }

        fun openEventSheet(action: RoutineAction) {
            updateContent {
                it.copy(
                    showEventSheet = true,
                    eventInitialAction = action,
                    eventError = null,
                    confoundCheck = null,
                )
            }
        }

        fun dismissEventSheet() {
            updateContent { it.copy(showEventSheet = false, eventError = null) }
        }

        fun dismissConfoundBanner() {
            updateContent { it.copy(confoundCheck = null) }
        }

        fun dismissPostSubmitWarning() {
            updateContent { it.copy(postSubmitWarning = null) }
        }

        fun consumeSuccessMessage() {
            updateContent { it.copy(successMessage = null) }
        }

        /** Called by [com.glowup.ai.feature.routine.components.RoutineEventSheet] whenever the user
         * toggles the action segment — `start`/`change` trigger a fresh `GET /confound-check`;
         * `stop` clears any shown warning since it cannot itself confound a window. */
        fun onEventActionChanged(action: RoutineAction) {
            if (action == RoutineAction.STOP) {
                updateContent { it.copy(confoundCheck = null, confoundCheckLoading = false) }
                return
            }
            viewModelScope.launch {
                val userId = sessionStore.userId() ?: return@launch
                updateContent { it.copy(confoundCheckLoading = true) }
                when (val result = routineRepository.confoundCheck(userId, excludeProductId = productId)) {
                    is GlowResult.Success -> {
                        updateContent {
                            it.copy(confoundCheckLoading = false, confoundCheck = result.data.takeIf { c -> c.confounded })
                        }
                    }

                    is GlowResult.Failure -> {
                        updateContent { it.copy(confoundCheckLoading = false) }
                    }
                }
            }
        }

        fun submitEvent(
            action: RoutineAction,
            slot: String,
            dose: String?,
            frequency: String?,
            notes: String?,
        ) {
            val current = _uiState.value as? ProductDetailUiState.Content ?: return
            if (current.eventSubmitting) return
            viewModelScope.launch {
                val userId = sessionStore.userId()
                if (userId == null) {
                    updateContent { it.copy(eventError = "Sign in to log a routine change.") }
                    return@launch
                }
                updateContent { it.copy(eventSubmitting = true, eventError = null) }
                val request =
                    RoutineEventRequest(
                        userId = userId,
                        productId = productId,
                        action = action,
                        slot = slot,
                        dose = dose,
                        frequency = frequency,
                        notes = notes,
                    )
                when (val result = routineRepository.logRoutineEvent(request)) {
                    is GlowResult.Success -> {
                        updateContent {
                            it.copy(
                                eventSubmitting = false,
                                showEventSheet = false,
                                confoundCheck = null,
                                postSubmitWarning = result.data.confoundWarning?.takeIf { c -> c.confounded },
                                successMessage = "Logged: ${action.name.lowercase()} ${it.detail.product.name}.",
                            )
                        }
                    }

                    is GlowResult.Failure -> {
                        // Trap #9: this route is not idempotent. A `Network` failure means the
                        // request's outcome is unknown server-side — never auto-retry; tell the user
                        // to check the timeline (which a manual refresh will reconcile) before
                        // logging the same change again.
                        val message =
                            if (result.error is ApiError.Network) {
                                "Connection lost while saving. Check the routine timeline before logging this again — it may have already gone through."
                            } else {
                                result.error.toDisplayMessage()
                            }
                        updateContent { it.copy(eventSubmitting = false, eventError = message) }
                    }
                }
            }
        }

        fun loadIngredientExplainer() {
            val current = _uiState.value as? ProductDetailUiState.Content ?: return
            if (current.explainerLoading || current.explainer != null) return
            viewModelScope.launch {
                val userId = sessionStore.userId() ?: return@launch
                updateContent { it.copy(explainerLoading = true, explainerError = null, explainerLocked = false) }
                when (val result = routineRepository.getIngredientExplainer(productId, userId)) {
                    is GlowResult.Success -> {
                        updateContent {
                            it.copy(explainerLoading = false, explainer = result.data)
                        }
                    }

                    is GlowResult.Failure -> {
                        updateContent {
                            it.copy(
                                explainerLoading = false,
                                explainerLocked = result.error.isPremiumRequired(),
                                explainerError = if (result.error.isPremiumRequired()) null else result.error.toDisplayMessage(),
                            )
                        }
                    }
                }
            }
        }
    }
