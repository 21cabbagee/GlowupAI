package com.glowup.ai.feature.capture

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.glowup.ai.core.util.GlowResult
import com.glowup.ai.data.repository.HomeRepository
import com.glowup.ai.data.repository.SessionRepository
import com.glowup.ai.feature.auth.toMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PhotoHistoryViewModel
    @Inject
    constructor(
        private val homeRepository: HomeRepository,
        private val sessionRepository: SessionRepository,
    ) : ViewModel() {
        private val _state = MutableStateFlow<PhotoHistoryUiState>(PhotoHistoryUiState.Loading)
        val state: StateFlow<PhotoHistoryUiState> = _state.asStateFlow()

        init {
            reload()
        }

        fun reload() {
            viewModelScope.launch {
                _state.value = PhotoHistoryUiState.Loading
                val userId = sessionRepository.userIdFlow.first()
                if (userId == null) {
                    _state.value = PhotoHistoryUiState.Error("Please sign in to see your captures.")
                    return@launch
                }
                when (val result = homeRepository.getHistory(userId)) {
                    is GlowResult.Success -> _state.value = PhotoHistoryUiState.Content(result.data.data)
                    is GlowResult.Failure -> _state.value = PhotoHistoryUiState.Error(result.error.toMessage())
                }
            }
        }
    }
