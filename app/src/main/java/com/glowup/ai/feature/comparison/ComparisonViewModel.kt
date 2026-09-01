package com.glowup.ai.feature.comparison

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.glowup.ai.core.util.GlowResult
import com.glowup.ai.data.local.SessionStore
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
 * Fetches user's history and allows comparing two captures with metrics.
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
                    )
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
                            )
                    }
                }

                is GlowResult.Failure -> {
                    _state.value = ComparisonUiState.Error(result.error.toString())
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
    ) : ComparisonUiState
}
