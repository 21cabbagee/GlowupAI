package com.glowup.ai.feature.insights

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.glowup.ai.core.util.GlowResult
import com.glowup.ai.data.local.SessionStore
import com.glowup.ai.data.repository.InsightsRepository
import com.glowup.ai.domain.model.BudgetOptimizer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * `GET /budget-optimizer` (Premium). [com.glowup.ai.domain.model.BudgetFlaggedProduct.estimatedAnnualCostCents]
 * is nullable when no offer price is on file — the flagged product must still render, just
 * without a cost figure, per ANDROID_PLAN.md §3.5.
 */
@HiltViewModel
class BudgetOptimizerViewModel @Inject constructor(
    private val repository: InsightsRepository,
    private val sessionStore: SessionStore,
) : ViewModel() {

    private val _uiState = MutableStateFlow<ScreenState<BudgetOptimizer>>(ScreenState.Loading)
    val uiState: StateFlow<ScreenState<BudgetOptimizer>> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.value = ScreenState.Loading
            if (!sessionStore.canUsePremiumFlow().first()) {
                _uiState.value = ScreenState.Locked
                return@launch
            }
            val userId = sessionStore.userId()
            if (userId == null) {
                _uiState.value = ScreenState.Error("Sign in to view your routine budget.")
                return@launch
            }
            when (val result = repository.getBudgetOptimizer(userId)) {
                is GlowResult.Success -> _uiState.value = if (result.data.flagged.isEmpty()) {
                    ScreenState.Empty(
                        title = "Nothing flagged",
                        body = "We'll flag stable, unused-looking products here as your routine history grows.",
                    )
                } else {
                    ScreenState.Content(result.data)
                }
                is GlowResult.Failure -> _uiState.value = if (result.error.isPremiumGate) {
                    ScreenState.Locked
                } else {
                    ScreenState.Error(result.error.toUserMessage())
                }
            }
        }
    }
}
