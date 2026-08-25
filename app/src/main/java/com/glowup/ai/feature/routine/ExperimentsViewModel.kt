package com.glowup.ai.feature.routine

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.glowup.ai.core.util.GlowResult
import com.glowup.ai.data.local.SessionStore
import com.glowup.ai.data.repository.ExperimentRepository
import com.glowup.ai.data.repository.RoutineRepository
import com.glowup.ai.domain.model.ExperimentCreateRequest
import com.glowup.ai.domain.model.PrimaryMetric
import com.glowup.ai.domain.model.Product
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val SEARCH_DEBOUNCE_MS = 400L

/**
 * Owns [com.glowup.ai.feature.shell.GlowDestination.Experiments]: list, create (with an inline
 * product picker), and the `403 Experiments requires Premium` gate — rendered as
 * [com.glowup.ai.core.ui.LockedCard], never as an ordinary empty list
 * (frontend-api-map.md "GET /api/users/{user_id}/experiments").
 */
@HiltViewModel
class ExperimentsViewModel @Inject constructor(
    private val experimentRepository: ExperimentRepository,
    private val routineRepository: RoutineRepository,
    private val sessionStore: SessionStore,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExperimentsUiState())
    val uiState: StateFlow<ExperimentsUiState> = _uiState.asStateFlow()

    val pendingKeys: StateFlow<Set<String>> = experimentRepository.pendingKeys

    private var productSearchJob: Job? = null

    init {
        loadExperiments()
    }

    fun loadExperiments() {
        viewModelScope.launch {
            _uiState.update { it.copy(listStatus = ExperimentsListStatus.Loading) }
            val userId = sessionStore.userId()
            if (userId == null) {
                _uiState.update { it.copy(listStatus = ExperimentsListStatus.Error("Sign in to see your experiments.")) }
                return@launch
            }
            when (val result = experimentRepository.listExperiments(userId)) {
                is GlowResult.Success -> _uiState.update { it.copy(listStatus = ExperimentsListStatus.Content(result.data)) }
                is GlowResult.Failure -> _uiState.update {
                    it.copy(
                        listStatus = if (result.error.isPremiumRequired()) {
                            ExperimentsListStatus.Locked
                        } else {
                            ExperimentsListStatus.Error(result.error.toDisplayMessage())
                        },
                    )
                }
            }
        }
    }

    fun openCreateSheet() {
        _uiState.update { it.copy(create = CreateExperimentState(visible = true)) }
    }

    fun dismissCreateSheet() {
        _uiState.update { it.copy(create = it.create.copy(visible = false)) }
    }

    fun onNameChange(name: String) = _uiState.update { it.copy(create = it.create.copy(name = name)) }
    fun onHypothesisChange(text: String) = _uiState.update { it.copy(create = it.create.copy(hypothesis = text)) }
    fun onPrimaryMetricChange(metric: PrimaryMetric) = _uiState.update { it.copy(create = it.create.copy(primaryMetric = metric)) }
    fun onTargetDaysChange(text: String) = _uiState.update { it.copy(create = it.create.copy(targetDays = text.filter(Char::isDigit))) }

    fun onProductQueryChange(query: String) {
        _uiState.update { it.copy(create = it.create.copy(productQuery = query, selectedProduct = null)) }
        productSearchJob?.cancel()
        if (query.isBlank()) {
            _uiState.update { it.copy(create = it.create.copy(productResults = emptyList(), productSearching = false)) }
            return
        }
        productSearchJob = viewModelScope.launch {
            delay(SEARCH_DEBOUNCE_MS)
            _uiState.update { it.copy(create = it.create.copy(productSearching = true)) }
            when (val result = routineRepository.searchProducts(query)) {
                is GlowResult.Success -> _uiState.update { it.copy(create = it.create.copy(productSearching = false, productResults = result.data)) }
                is GlowResult.Failure -> _uiState.update { it.copy(create = it.create.copy(productSearching = false, productResults = emptyList())) }
            }
        }
    }

    fun onSelectProduct(product: Product) {
        _uiState.update { it.copy(create = it.create.copy(selectedProduct = product, productResults = emptyList(), productQuery = product.name)) }
    }

    fun submitCreate() {
        val create = _uiState.value.create
        val product = create.selectedProduct ?: return
        if (create.pending || create.name.isBlank()) return
        viewModelScope.launch {
            val userId = sessionStore.userId()
            if (userId == null) {
                _uiState.update { it.copy(create = it.create.copy(error = "Sign in to start an experiment.")) }
                return@launch
            }
            _uiState.update { it.copy(create = it.create.copy(pending = true, error = null)) }
            val request = ExperimentCreateRequest(
                userId = userId,
                name = create.name.trim(),
                hypothesis = create.hypothesis.trim().ifBlank { null },
                productId = product.id,
                primaryMetric = create.primaryMetric,
                targetDays = create.targetDays.toIntOrNull()?.coerceIn(1, 180) ?: 14,
            )
            when (val result = experimentRepository.createExperiment(request)) {
                is GlowResult.Success -> _uiState.update {
                    it.copy(
                        create = CreateExperimentState(),
                        navigateToExperimentId = result.data.id,
                    )
                }
                is GlowResult.Failure -> _uiState.update {
                    it.copy(
                        create = it.create.copy(
                            pending = false,
                            error = if (result.error.isPremiumRequired()) {
                                "Experiments require Premium. Upgrade to start one."
                            } else {
                                result.error.toDisplayMessage()
                            },
                        ),
                    )
                }
            }
        }
    }

    fun consumeNavigation() {
        _uiState.update { it.copy(navigateToExperimentId = null) }
    }
}
