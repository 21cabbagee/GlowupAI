package com.glowup.ai.feature.insights

import com.glowup.ai.domain.model.Citation
import com.glowup.ai.domain.model.SafetyScope

/** One rendered chat turn. [isSafetyHandoff] marks a `scope == dermatology_review` turn — the
 * screen renders it as a clinician hand-off, never as an ordinary answer. */
data class ChatMessage(
    val id: String,
    val role: String,
    val content: String,
    val scope: SafetyScope? = null,
    val citations: List<Citation> = emptyList(),
    val pending: Boolean = false,
    val isSafetyHandoff: Boolean = false,
    val isError: Boolean = false,
)

sealed interface QnaUiState {
    data object Loading : QnaUiState

    data object Locked : QnaUiState

    data class Error(
        val message: String,
    ) : QnaUiState

    /**
     * @param threadBlocked True once any turn in this thread has been triaged (locally or by the
     * server) as `dermatology_review`. While true, the composer is disabled and only "start a new
     * conversation" is offered — the fix for the non-negotiable constraint that a dermatology
     * hand-off must never become a continued diagnostic conversation in the same thread.
     */
    data class Content(
        val messages: List<ChatMessage>,
        val input: String,
        val sending: Boolean,
        val threadBlocked: Boolean,
        val threadId: String?,
    ) : QnaUiState
}
