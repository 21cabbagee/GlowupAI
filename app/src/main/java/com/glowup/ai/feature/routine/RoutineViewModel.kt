package com.glowup.ai.feature.routine

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.glowup.ai.core.util.GlowResult
import com.glowup.ai.data.local.SessionStore
import com.glowup.ai.data.repository.HomeRepository
import com.glowup.ai.data.repository.RoutineRepository
import com.glowup.ai.domain.model.ProductCreateRequest
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
 * Owns the [com.glowup.ai.feature.shell.GlowDestination.Routine] screen: debounced product
 * search, barcode lookup, product creation, and the routine-change timeline.
 *
 * The timeline is sourced from `GET /dashboard`'s `routine_events` field (there is no dedicated
 * "list routine events" route) via [HomeRepository] — a read-only consumer of a repository this
 * task does not own. Per frontend-api-map.md trap #4/#7, this is fetched once per screen visit
 * (never polled); [HomeRepository] itself dedupes/caches it and goes stale automatically via
 * [com.glowup.ai.data.repository.support.CacheInvalidationBus] whenever [RoutineRepository]
 * publishes a routine-event mutation, so a manual refresh after logging an event just re-reads a
 * cache that is already invalid.
 */
@HiltViewModel
class RoutineViewModel @Inject constructor(
    private val routineRepository: RoutineRepository,
    private val homeRepository: HomeRepository,
    private val sessionStore: SessionStore,
) : ViewModel() {

    private val _uiState = MutableStateFlow(RoutineUiState())
    val uiState: StateFlow<RoutineUiState> = _uiState.asStateFlow()

    /** Mirrors [RoutineRepository.pendingKeys] so the screen can disable "Add product" while a
     * create is outstanding without duplicating mutation-guard logic. */
    val pendingKeys: StateFlow<Set<String>> = routineRepository.pendingKeys

    private var searchJob: Job? = null

    init {
        loadTimeline()
    }

    fun loadTimeline() {
        viewModelScope.launch {
            _uiState.update { it.copy(timelineLoading = true, timelineError = null) }
            val userId = sessionStore.userId()
            if (userId == null) {
                _uiState.update { it.copy(timelineLoading = false, timelineError = "Sign in to see your routine history.") }
                return@launch
            }
            when (val result = homeRepository.getDashboard(userId)) {
                is GlowResult.Success -> _uiState.update {
                    it.copy(timelineLoading = false, timeline = result.data.data.routineEvents, timelineError = null)
                }
                is GlowResult.Failure -> _uiState.update {
                    it.copy(timelineLoading = false, timelineError = result.error.toDisplayMessage())
                }
            }
        }
    }

    fun onQueryChange(query: String) {
        _uiState.update { it.copy(query = query) }
        searchJob?.cancel()
        if (query.isBlank()) {
            _uiState.update { it.copy(searchResults = emptyList(), searching = false, searchError = null, hasSearched = false) }
            return
        }
        searchJob = viewModelScope.launch {
            delay(SEARCH_DEBOUNCE_MS)
            _uiState.update { it.copy(searching = true, searchError = null) }
            when (val result = routineRepository.searchProducts(query)) {
                is GlowResult.Success -> _uiState.update {
                    it.copy(searching = false, searchResults = result.data, hasSearched = true)
                }
                is GlowResult.Failure -> _uiState.update {
                    it.copy(searching = false, searchError = result.error.toDisplayMessage(), hasSearched = true)
                }
            }
        }
    }

    fun onBarcodeChange(barcode: String) {
        _uiState.update { it.copy(barcode = barcode, barcodeLookupError = null) }
    }

    fun lookupBarcode() {
        val barcode = _uiState.value.barcode.trim()
        if (barcode.isEmpty() || _uiState.value.barcodeLookupPending) return
        viewModelScope.launch {
            _uiState.update { it.copy(barcodeLookupPending = true, barcodeLookupError = null) }
            when (val result = routineRepository.lookupProduct(barcode)) {
                is GlowResult.Success -> _uiState.update {
                    it.copy(barcodeLookupPending = false, navigateToProductId = result.data.id)
                }
                is GlowResult.Failure -> _uiState.update {
                    it.copy(
                        barcodeLookupPending = false,
                        barcodeLookupError = result.error.toDisplayMessage(),
                    )
                }
            }
        }
    }

    fun consumeNavigation() {
        _uiState.update { it.copy(navigateToProductId = null) }
    }

    fun openAddProductSheet(prefillBarcode: String? = null) {
        _uiState.update {
            it.copy(showAddProductSheet = true, addProductPrefillBarcode = prefillBarcode, addProductError = null)
        }
    }

    fun dismissAddProductSheet() {
        _uiState.update { it.copy(showAddProductSheet = false, addProductError = null) }
    }

    /** Guarded against double-submit both here (button disables on [addProductPending]) and
     * inside [RoutineRepository.createProduct] itself ([com.glowup.ai.data.repository.support.MutationLock]) —
     * product rows are global and this route is not idempotent (frontend-api-map.md trap #7). */
    fun submitAddProduct(request: ProductCreateRequest) {
        if (_uiState.value.addProductPending) return
        viewModelScope.launch {
            _uiState.update { it.copy(addProductPending = true, addProductError = null) }
            when (val result = routineRepository.createProduct(request)) {
                is GlowResult.Success -> _uiState.update {
                    it.copy(
                        addProductPending = false,
                        showAddProductSheet = false,
                        navigateToProductId = result.data.id,
                    )
                }
                is GlowResult.Failure -> _uiState.update {
                    it.copy(addProductPending = false, addProductError = result.error.toDisplayMessage())
                }
            }
        }
    }
}
