package com.glowup.ai.feature.comparison

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.glowup.ai.core.util.GlowResult
import com.glowup.ai.data.local.SessionStore
import com.glowup.ai.data.remote.dto.ComparisonResponseDto
import com.glowup.ai.data.repository.HomeRepository
import com.glowup.ai.domain.model.HistoryItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the comparison screen.
 * Fetches user's history and asks the server for a qualitative comparison.
 */
@HiltViewModel
class ComparisonViewModel
    @Inject
    constructor(
        private val homeRepository: HomeRepository,
        private val sessionStore: SessionStore,
    ) : ViewModel() {
        private val _state = MutableStateFlow<ComparisonUiState>(ComparisonUiState.Loading)
        val state: StateFlow<ComparisonUiState> = _state.asStateFlow()

        init {
            viewModelScope.launch { loadHistory() }
        }

        fun onRetry() {
            viewModelScope.launch { loadHistory() }
        }

        fun onComparisonSelected(
            baselineIndex: Int,
            currentIndex: Int,
        ) {
            val currentState = _state.value
            if (currentState is ComparisonUiState.Content) {
                _state.value =
                    currentState.copy(
                        selectedBaselineIndex = baselineIndex,
                        selectedCurrentIndex = currentIndex,
                        comparison = null,
                        comparisonLoading = true,
                        comparisonError = null,
                    )
                if (baselineIndex == currentIndex) {
                    _state.value = currentState.copy(
                        selectedBaselineIndex = baselineIndex,
                        selectedCurrentIndex = currentIndex,
                        comparison = null,
                        comparisonLoading = false,
                        comparisonError = "Choose two different captures to compare.",
                    )
                    return
                }
                viewModelScope.launch {
                    requestComparison(currentState.history, baselineIndex, currentIndex)
                }
            }
        }

        private suspend fun loadHistory() {
            val userId = sessionStore.userId()
            if (userId == null) {
                _state.value = ComparisonUiState.Error("Sign in to see your captures.")
                return
            }

            _state.value = ComparisonUiState.Loading

            when (val result = homeRepository.getHistory(userId)) {
                is GlowResult.Success -> {
                    val sortedHistory = result.data.data.sortedBy { it.capturedAt }
                    if (sortedHistory.size < 2) {
                        _state.value = ComparisonUiState.Error("You need at least 2 captures to compare.")
                    } else {
                        // Default: baseline (first) vs latest (last)
                        _state.value =
                            ComparisonUiState.Content(
                                history = sortedHistory,
                                selectedBaselineIndex = 0,
                                selectedCurrentIndex = sortedHistory.size - 1,
                                comparisonLoading = true,
                            )
                        requestComparison(sortedHistory, 0, sortedHistory.size - 1)
                    }
                }

                is GlowResult.Failure -> {
                    _state.value = ComparisonUiState.Error(result.error.toString())
                }
            }
        }

        private suspend fun requestComparison(history: List<HistoryItem>, baselineIndex: Int, currentIndex: Int) {
            val userId = sessionStore.userId() ?: return
            val baseline = history.getOrNull(baselineIndex) ?: return
            val current = history.getOrNull(currentIndex) ?: return
            if (baseline.id == current.id) return
            when (val result = homeRepository.compareCaptures(userId, baseline.id, current.id)) {
                is GlowResult.Success -> {
                    val state = _state.value
                    if (state is ComparisonUiState.Content &&
                        state.selectedBaselineIndex == baselineIndex &&
                        state.selectedCurrentIndex == currentIndex
                    ) {
                        _state.value = state.copy(comparison = result.data, comparisonLoading = false)
                    }
                }
                is GlowResult.Failure -> {
                    val state = _state.value
                    if (state is ComparisonUiState.Content &&
                        state.selectedBaselineIndex == baselineIndex &&
                        state.selectedCurrentIndex == currentIndex
                    ) {
                        _state.value = state.copy(comparisonLoading = false, comparisonError = result.error.toString())
                    }
                }
            }
        }
    }

sealed interface ComparisonUiState {
    data object Loading : ComparisonUiState

    data class Error(
        val message: String,
    ) : ComparisonUiState

    data class Content(
        val history: List<HistoryItem>,
        val selectedBaselineIndex: Int,
        val selectedCurrentIndex: Int,
        val comparison: ComparisonResponseDto? = null,
        val comparisonLoading: Boolean = false,
        val comparisonError: String? = null,
    ) : ComparisonUiState
}
