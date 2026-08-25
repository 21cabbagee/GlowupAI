package com.glowup.ai.feature.insights

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.glowup.ai.core.util.GlowResult
import com.glowup.ai.data.local.SessionStore
import com.glowup.ai.data.repository.InsightsRepository
import com.glowup.ai.domain.model.ContextEvent
import com.glowup.ai.domain.model.ContextEventCreateRequest
import com.glowup.ai.domain.model.ContextEventType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ContextLogFormState(
    val eventType: ContextEventType = ContextEventType.SLEEP,
    val value: String = "",
    val occurredAt: String = "",
    val notes: String = "",
    val submitting: Boolean = false,
    val error: String? = null,
)

/**
 * Context events (Premium): a simple date + type + free-text log next to the root-cause
 * correlations panel (frontend-api-map.md "Context events + root-cause search").
 */
@HiltViewModel
class ContextLogViewModel @Inject constructor(
    private val repository: InsightsRepository,
    private val sessionStore: SessionStore,
) : ViewModel() {

    private val _uiState = MutableStateFlow<ScreenState<List<ContextEvent>>>(ScreenState.Loading)
    val uiState: StateFlow<ScreenState<List<ContextEvent>>> = _uiState.asStateFlow()

    private val _form = MutableStateFlow(ContextLogFormState())
    val form: StateFlow<ContextLogFormState> = _form.asStateFlow()

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
                _uiState.value = ScreenState.Error("Sign in to view your context log.")
                return@launch
            }
            when (val result = repository.getContextEvents(userId)) {
                is GlowResult.Success -> _uiState.value = if (result.data.isEmpty()) {
                    ScreenState.Empty(
                        title = "No context events yet",
                        body = "Log sleep, travel, stress, or other context to see if it correlates with your metrics.",
                    )
                } else {
                    ScreenState.Content(result.data.sortedByDescending { it.occurredAt })
                }
                is GlowResult.Failure -> _uiState.value = if (result.error.isPremiumGate) {
                    ScreenState.Locked
                } else {
                    ScreenState.Error(result.error.toUserMessage())
                }
            }
        }
    }

    fun onTypeChange(type: ContextEventType) {
        _form.value = _form.value.copy(eventType = type)
    }

    fun onValueChange(value: String) {
        _form.value = _form.value.copy(value = value)
    }

    fun onDateChange(date: String) {
        _form.value = _form.value.copy(occurredAt = date)
    }

    fun onNotesChange(notes: String) {
        _form.value = _form.value.copy(notes = notes)
    }

    fun submit() {
        val form = _form.value
        if (form.submitting) return
        viewModelScope.launch {
            _form.value = form.copy(submitting = true, error = null)
            val userId = sessionStore.userId()
            if (userId == null) {
                _form.value = form.copy(submitting = false, error = "Sign in to continue.")
                return@launch
            }
            val request = ContextEventCreateRequest(
                eventType = form.eventType,
                value = form.value.trim().ifBlank { null },
                occurredAt = form.occurredAt.trim().ifBlank { null },
                notes = form.notes.trim().ifBlank { null },
            )
            when (val result = repository.addContextEvent(userId, request)) {
                is GlowResult.Success -> {
                    _form.value = ContextLogFormState()
                    load()
                }
                is GlowResult.Failure -> {
                    _form.value = form.copy(submitting = false, error = result.error.toUserMessage())
                }
            }
        }
    }
}
