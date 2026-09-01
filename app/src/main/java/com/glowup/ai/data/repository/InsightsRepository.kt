package com.glowup.ai.data.repository

import com.glowup.ai.core.util.GlowResult
import com.glowup.ai.core.util.map
import com.glowup.ai.core.util.onSuccess
import com.glowup.ai.data.remote.GlowUpApi
import com.glowup.ai.data.remote.apiCall
import com.glowup.ai.data.remote.dto.QnaCreateRequestDto
import com.glowup.ai.data.remote.dto.TriageCreateRequestDto
import com.glowup.ai.data.remote.dto.toDomain
import com.glowup.ai.data.remote.dto.toDto
import com.glowup.ai.domain.model.BudgetOptimizer
import com.glowup.ai.domain.model.ContextEvent
import com.glowup.ai.domain.model.ContextEventCreateRequest
import com.glowup.ai.domain.model.DermExport
import com.glowup.ai.domain.model.Label
import com.glowup.ai.domain.model.LabelCreateRequest
import com.glowup.ai.domain.model.QnaAnswer
import com.glowup.ai.domain.model.QnaMessage
import com.glowup.ai.domain.model.ReprocessJob
import com.glowup.ai.domain.model.RootCauseInsight
import com.glowup.ai.domain.model.TriageResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Owns: `POST|GET /qna`, `POST /api/triage`, `GET|POST /labels`, `POST /reprocess` +
 * `GET /reprocess/{jobId}`, `GET|POST /context-events`, `GET /root-cause`,
 * `GET /budget-optimizer`, `GET /derm-export`.
 *
 * [threadId] is the fix for frontend-api-map.md trap #10 / ANDROID_PLAN.md §3 bug #5: the web
 * client discards `thread_id` and starts a new thread on every question. This repository holds
 * the current thread as state and [ask] sends it automatically unless the caller explicitly wants
 * a new conversation ([startNewThread]).
 */
@Singleton
class InsightsRepository
    @Inject
    constructor(
        private val api: GlowUpApi,
    ) {
        private val _threadId = MutableStateFlow<String?>(null)
        val threadId: StateFlow<String?> = _threadId.asStateFlow()

        fun startNewThread() {
            _threadId.value = null
        }

        /**
         * Re-seeds the in-memory [threadId] without an API round-trip. This repository is a
         * `@Singleton` — its state lives only as long as the process, so it is lost on OS-initiated
         * process death just like any other in-memory singleton. The fix lives one layer up: the Q&A
         * screen's `ViewModel` persists the active `thread_id` into its `SavedStateHandle` (which
         * Android restores from the saved-instance-state bundle across process death) and calls this
         * on init to restore it here before the first message is sent or history is grouped. Also used
         * to open a specific past thread from navigation (`GlowDestination.QnaThread(threadId)`).
         */
        fun restoreThread(threadId: String?) {
            _threadId.value = threadId
        }

        suspend fun ask(
            userId: String,
            question: String,
        ): GlowResult<QnaAnswer> =
            apiCall { api.askQna(userId, QnaCreateRequestDto(question, _threadId.value)).toDomain() }
                .onSuccess { _threadId.value = it.threadId }

        suspend fun history(userId: String): GlowResult<List<QnaMessage>> = apiCall { api.getQnaHistory(userId).map { it.toDomain() } }

        /** Open route (no `user_id`, no Premium gate). Run BEFORE continuing a Q&A conversation —
         * `scope == dermatology_review` is a clinician hand-off, never something to keep chatting
         * about diagnostically. */
        suspend fun triage(text: String): GlowResult<TriageResult> = apiCall { api.triage(TriageCreateRequestDto(text)).toDomain() }

        suspend fun getLabels(userId: String): GlowResult<List<Label>> = apiCall { api.getLabels(userId).map { it.toDomain() } }

        suspend fun addLabel(
            userId: String,
            request: LabelCreateRequest,
        ): GlowResult<Label> = apiCall { api.addLabel(userId, request.toDto()).toDomain() }

        /** Queued job — `processed_count` must never be read off this POST's response (ANDROID_PLAN.md
         * §3 bug #3); always poll [getReprocessStatus]. */
        suspend fun reprocess(
            userId: String,
            modelVersion: String,
        ): GlowResult<String> =
            apiCall {
                api
                    .reprocess(
                        userId,
                        com.glowup.ai.data.remote.dto
                            .ReprocessCreateRequestDto(modelVersion),
                    ).jobId
            }

        suspend fun getReprocessStatus(
            userId: String,
            jobId: String,
        ): GlowResult<ReprocessJob> = apiCall { api.getReprocessStatus(userId, jobId).toDomain(jobId) }

        suspend fun getContextEvents(userId: String): GlowResult<List<ContextEvent>> =
            apiCall { api.getContextEvents(userId).map { it.toDomain() } }

        suspend fun addContextEvent(
            userId: String,
            request: ContextEventCreateRequest,
        ): GlowResult<ContextEvent> = apiCall { api.addContextEvent(userId, request.toDto()).toDomain() }

        suspend fun getRootCause(
            userId: String,
            metric: String = "texture_score",
        ): GlowResult<List<RootCauseInsight>> = apiCall { api.getRootCause(userId, metric).map { it.toDomain() } }

        suspend fun getBudgetOptimizer(userId: String): GlowResult<BudgetOptimizer> = apiCall { api.getBudgetOptimizer(userId).toDomain() }

        suspend fun getDermExport(userId: String): GlowResult<DermExport> = apiCall { api.getDermExport(userId).toDomain() }
    }
