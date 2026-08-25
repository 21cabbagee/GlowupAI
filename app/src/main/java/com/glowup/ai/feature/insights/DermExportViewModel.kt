package com.glowup.ai.feature.insights

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.glowup.ai.core.util.GlowResult
import com.glowup.ai.data.local.SessionStore
import com.glowup.ai.data.repository.InsightsRepository
import com.glowup.ai.domain.model.DermExport
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * `GET /derm-export` (Premium). [DermExport.printableHtml] is a plain HTML string meant to be
 * rendered for print/share — never a downloadable file the backend generates
 * (ANDROID_PLAN.md §3.5 / frontend-api-map.md "Ideal UI state").
 */
@HiltViewModel
class DermExportViewModel @Inject constructor(
    private val repository: InsightsRepository,
    private val sessionStore: SessionStore,
) : ViewModel() {

    private val _uiState = MutableStateFlow<ScreenState<DermExport>>(ScreenState.Loading)
    val uiState: StateFlow<ScreenState<DermExport>> = _uiState.asStateFlow()

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
                _uiState.value = ScreenState.Error("Sign in to generate a dermatologist export.")
                return@launch
            }
            when (val result = repository.getDermExport(userId)) {
                is GlowResult.Success -> _uiState.value = if (result.data.captureCount == 0) {
                    ScreenState.Empty(
                        title = "Nothing to export yet",
                        body = "Take a few captures first so there is tracked history to summarize.",
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
