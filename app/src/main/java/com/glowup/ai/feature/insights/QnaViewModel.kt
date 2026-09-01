package com.glowup.ai.feature.insights

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.glowup.ai.core.util.GlowResult
import com.glowup.ai.data.local.SessionStore
import com.glowup.ai.data.repository.InsightsRepository
import com.glowup.ai.data.telemetry.Telemetry
import com.glowup.ai.data.telemetry.TelemetryEvent
import com.glowup.ai.domain.model.Citation
import com.glowup.ai.domain.model.SafetyScope
import com.glowup.ai.feature.shell.GlowDestination
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

/**
 * Owns the Q&A chat thread. The single most important behaviour here (ANDROID_PLAN.md §3.5,
 * frontend-api-map.md trap #10): a `thread_id` is persisted and reused across every follow-up
 * question, and survives OS-initiated process death by round-tripping through [savedStateHandle]
 * — see [activeThreadId].
 *
 * Safety gate: every free-form question runs through `POST /api/triage` BEFORE it is ever sent to
 * `POST /qna`. A `dermatology_review` scope — whether caught here or echoed back in an answer's
 * own `scope` — flips [QnaUiState.Content.threadBlocked] and the question is never forwarded to
 * the model for that turn; the composer stays disabled until the user explicitly starts a new
 * thread.
 */
@HiltViewModel
class QnaViewModel
    @Inject
    constructor(
        private val repository: InsightsRepository,
        private val sessionStore: SessionStore,
        private val savedStateHandle: SavedStateHandle,
        private val telemetry: Telemetry,
    ) : ViewModel() {
        private companion object {
            const val KEY_ACTIVE_THREAD = "insights_qna_active_thread_id"
            const val HANDOFF_FALLBACK =
                "This reads as something a licensed dermatologist should evaluate directly. " +
                    "GlowUp AI tracks cosmetic appearance only and cannot help further with this question."
        }

        /** Survives process death: Android restores [SavedStateHandle] from the saved-instance-state
         * bundle, so a freshly recreated ViewModel re-seeds [InsightsRepository]'s in-memory
         * `thread_id` from here instead of silently starting a new thread. */
        private var activeThreadId: String?
            get() = savedStateHandle[KEY_ACTIVE_THREAD]
            set(value) {
                savedStateHandle[KEY_ACTIVE_THREAD] = value
            }

        private val _uiState = MutableStateFlow<QnaUiState>(QnaUiState.Loading)
        val uiState: StateFlow<QnaUiState> = _uiState.asStateFlow()

        init {
            val navThreadId = runCatching { savedStateHandle.toRoute<GlowDestination.QnaThread>().threadId }.getOrNull()
            val restoreId = activeThreadId ?: navThreadId
            repository.restoreThread(restoreId)
            activeThreadId = restoreId
            load()
        }

        fun load() {
            viewModelScope.launch {
                val canUsePremium = sessionStore.canUsePremiumFlow().first()
                if (!canUsePremium) {
                    _uiState.value = QnaUiState.Locked
                    return@launch
                }
                val userId = sessionStore.userId()
                if (userId == null) {
                    _uiState.value = QnaUiState.Error("Sign in to use Data Q&A.")
                    return@launch
                }
                when (val result = repository.history(userId)) {
                    is GlowResult.Success -> {
                        val threadId = repository.threadId.value
                        // No active thread yet -> a brand-new conversation starts empty rather than
                        // showing every past thread mashed together; a restored/opened thread id (via
                        // SavedStateHandle across process death, or GlowDestination.QnaThread's nav
                        // arg) shows exactly that thread's messages.
                        val messages =
                            if (threadId == null) {
                                emptyList()
                            } else {
                                result.data
                                    .filter { it.threadId == threadId }
                                    .map { msg ->
                                        ChatMessage(
                                            id = UUID.randomUUID().toString(),
                                            role = msg.role,
                                            content = msg.content,
                                            scope = msg.scope,
                                            citations = msg.citations,
                                            isSafetyHandoff = msg.scope == SafetyScope.DERMATOLOGY_REVIEW,
                                        )
                                    }
                            }
                        _uiState.value =
                            QnaUiState.Content(
                                messages = messages,
                                input = "",
                                sending = false,
                                threadBlocked = messages.any { it.isSafetyHandoff },
                                threadId = threadId,
                            )
                    }

                    is GlowResult.Failure -> {
                        if (result.error.isPremiumGate) {
                            _uiState.value = QnaUiState.Locked
                        } else {
                            _uiState.value = QnaUiState.Error(result.error.toUserMessage())
                        }
                    }
                }
            }
        }

        fun onInputChange(text: String) {
            val current = _uiState.value as? QnaUiState.Content ?: return
            _uiState.value = current.copy(input = text)
        }

        fun startNewConversation() {
            repository.startNewThread()
            activeThreadId = null
            _uiState.value =
                QnaUiState.Content(
                    messages = emptyList(),
                    input = "",
                    sending = false,
                    threadBlocked = false,
                    threadId = null,
                )
        }

        /** Runs `POST /api/triage` on [question] first, always — this is non-negotiable per
         * ANDROID_PLAN.md's product constraints, not just a nicety for the happy path. */
        fun send() {
            val state = _uiState.value as? QnaUiState.Content ?: return
            val question = state.input.trim()
            if (question.isEmpty() || state.sending || state.threadBlocked) return

            val userMessage = ChatMessage(id = UUID.randomUUID().toString(), role = "user", content = question)
            val pendingId = UUID.randomUUID().toString()
            telemetry.track(TelemetryEvent.QNA_ASKED)

            val pendingMessage = ChatMessage(id = pendingId, role = "assistant", content = "", pending = true)

            _uiState.update { current ->
                (current as? QnaUiState.Content)?.copy(
                    messages = current.messages + userMessage + pendingMessage,
                    input = "",
                    sending = true,
                ) ?: current
            }

            viewModelScope.launch {
                when (val triageResult = repository.triage(question)) {
                    is GlowResult.Success -> {
                        val triage = triageResult.data
                        if (triage.scope == SafetyScope.DERMATOLOGY_REVIEW) {
                            telemetry.track(TelemetryEvent.QNA_HANDOFF)
                            resolvePending(
                                pendingId,
                                triage.message.ifBlank { HANDOFF_FALLBACK },
                                isSafetyHandoff = true,
                                blockThread = true,
                            )
                            return@launch
                        }
                    }

                    is GlowResult.Failure -> {
                        // Triage is an open, no-prerequisite route; if it is unreachable we fail safe by
                        // still attempting the question rather than blocking Q&A on a triage outage —
                        // but the dermatology-review path above always wins when triage does answer.
                    }
                }

                val userId = sessionStore.userId()
                if (userId == null) {
                    resolvePending(pendingId, "Sign in to continue.", isError = true)
                    return@launch
                }

                when (val result = repository.ask(userId, question)) {
                    is GlowResult.Success -> {
                        val answer = result.data
                        val isHandoff = answer.scope == SafetyScope.DERMATOLOGY_REVIEW
                        if (isHandoff) telemetry.track(TelemetryEvent.QNA_HANDOFF)
                        resolvePending(
                            pendingId,
                            answer.answer,
                            scope = answer.scope,
                            citations = answer.citations,
                            isSafetyHandoff = isHandoff,
                            blockThread = isHandoff,
                        )
                        activeThreadId = answer.threadId
                    }

                    is GlowResult.Failure -> {
                        if (result.error.isPremiumGate) {
                            _uiState.value = QnaUiState.Locked
                        } else {
                            resolvePending(pendingId, result.error.toUserMessage(), isError = true)
                        }
                    }
                }
            }
        }

        private fun resolvePending(
            pendingId: String,
            text: String,
            scope: SafetyScope? = null,
            citations: List<Citation> = emptyList(),
            isSafetyHandoff: Boolean = false,
            isError: Boolean = false,
            blockThread: Boolean = false,
        ) {
            _uiState.update { current ->
                val content = current as? QnaUiState.Content ?: return@update current
                content.copy(
                    messages =
                        content.messages.map { message ->
                            if (message.id == pendingId) {
                                message.copy(
                                    content = text,
                                    pending = false,
                                    scope = scope,
                                    citations = citations,
                                    isSafetyHandoff = isSafetyHandoff,
                                    isError = isError,
                                )
                            } else {
                                message
                            }
                        },
                    sending = false,
                    threadBlocked = content.threadBlocked || blockThread,
                    threadId = repository.threadId.value,
                )
            }
        }
    }
