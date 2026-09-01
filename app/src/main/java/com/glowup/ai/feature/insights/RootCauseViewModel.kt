package com.glowup.ai.feature.insights

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.glowup.ai.core.util.GlowResult
import com.glowup.ai.data.local.SessionStore
import com.glowup.ai.data.repository.InsightsRepository
import com.glowup.ai.domain.model.PrimaryMetric
import com.glowup.ai.domain.model.RootCauseInsight
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * `GET /root-cause?metric=` (Premium). Every [RootCauseInsight.message] is rendered verbatim —
 * it already carries the correlation-not-causation caveat and must never be paraphrased or
 * dropped (ANDROID_PLAN.md §3.5).
 */
@HiltViewModel
class RootCauseViewModel
    @Inject
    constructor(
        private val repository: InsightsRepository,
        private val sessionStore: SessionStore,
    ) : ViewModel() {
        private val _metric = MutableStateFlow(PrimaryMetric.TEXTURE_SCORE)
        val metric: StateFlow<PrimaryMetric> = _metric.asStateFlow()

        private val _uiState = MutableStateFlow<ScreenState<List<RootCauseInsight>>>(ScreenState.Loading)
        val uiState: StateFlow<ScreenState<List<RootCauseInsight>>> = _uiState.asStateFlow()

        init {
            load()
        }

        fun onMetricChange(metric: PrimaryMetric) {
            _metric.value = metric
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
                    _uiState.value = ScreenState.Error("Sign in to view root-cause correlations.")
                    return@launch
                }
                when (val result = repository.getRootCause(userId, _metric.value.toWire())) {
                    is GlowResult.Success -> {
                        _uiState.value =
                            if (result.data.isEmpty()) {
                                ScreenState.Empty(
                                    title = "Not enough context yet",
                                    body = "Log a few context events (sleep, travel, stress…) to surface correlations here.",
                                )
                            } else {
                                ScreenState.Content(result.data.sortedByDescending { it.normalizedEffect })
                            }
                    }

                    is GlowResult.Failure -> {
                        _uiState.value =
                            if (result.error.isPremiumGate) {
                                ScreenState.Locked
                            } else {
                                ScreenState.Error(result.error.toUserMessage())
                            }
                    }
                }
            }
        }
    }
