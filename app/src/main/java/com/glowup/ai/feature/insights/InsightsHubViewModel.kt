package com.glowup.ai.feature.insights

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.glowup.ai.core.util.GlowResult
import com.glowup.ai.data.local.SessionStore
import com.glowup.ai.data.repository.InsightsRepository
import com.glowup.ai.domain.model.JobStatus
import com.glowup.ai.domain.model.Label
import com.glowup.ai.domain.model.LabelCreateRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LabelFormState(
    val photoId: String = "",
    val labelType: String = "user_note",
    val value: String = "",
    val notes: String = "",
    val submitting: Boolean = false,
    val error: String? = null,
)

/** `POST/GET /reprocess` is an async job (ANDROID_PLAN.md §3.5 / bug #3): the POST response only
 * ever carries `{job_id, status:"queued"}` — never read a result off it. This state machine polls
 * `GET /reprocess/{jobId}` every ~1.5s until a terminal status. */
sealed interface ReprocessUiState {
    data object Locked : ReprocessUiState

    data object Idle : ReprocessUiState

    data class Polling(
        val status: JobStatus,
    ) : ReprocessUiState

    data class Completed(
        val processedCount: Int?,
        val modelVersion: String?,
    ) : ReprocessUiState

    data class Failed(
        val message: String,
    ) : ReprocessUiState
}

/**
 * Backs the Insights hub screen. Labels and reprocessing have no dedicated
 * [com.glowup.ai.feature.shell.GlowDestination] entries — [com.glowup.ai.feature.shell.GlowDestination]
 * is a fixed, cross-feature contract this package must not extend — so both live as sections on
 * the hub itself rather than as new screens.
 */
@HiltViewModel
class InsightsHubViewModel
    @Inject
    constructor(
        private val repository: InsightsRepository,
        private val sessionStore: SessionStore,
    ) : ViewModel() {
        val canUsePremium: StateFlow<Boolean> =
            sessionStore
                .canUsePremiumFlow()
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

        // -- Labels (free feature, no Premium requirement) --------------------------------------

        private val _labelsState = MutableStateFlow<ScreenState<List<Label>>>(ScreenState.Loading)
        val labelsState: StateFlow<ScreenState<List<Label>>> = _labelsState.asStateFlow()

        private val _labelForm = MutableStateFlow(LabelFormState())
        val labelForm: StateFlow<LabelFormState> = _labelForm.asStateFlow()

        // -- Reprocess (Premium, async job) ------------------------------------------------------

        private val _reprocessState = MutableStateFlow<ReprocessUiState>(ReprocessUiState.Idle)
        val reprocessState: StateFlow<ReprocessUiState> = _reprocessState.asStateFlow()

        private val _reprocessModelVersion = MutableStateFlow("deterministic-3.1")
        val reprocessModelVersion: StateFlow<String> = _reprocessModelVersion.asStateFlow()

        init {
            loadLabels()
        }

        fun loadLabels() {
            viewModelScope.launch {
                _labelsState.value = ScreenState.Loading
                val userId = sessionStore.userId()
                if (userId == null) {
                    _labelsState.value = ScreenState.Error("Sign in to view annotations.")
                    return@launch
                }
                when (val result = repository.getLabels(userId)) {
                    is GlowResult.Success -> {
                        _labelsState.value =
                            if (result.data.isEmpty()) {
                                ScreenState.Empty(
                                    title = "No annotations yet",
                                    body = "Add a note on a capture below — user notes are never treated as an automated classification.",
                                )
                            } else {
                                ScreenState.Content(result.data.sortedByDescending { it.createdAt ?: "" })
                            }
                    }

                    is GlowResult.Failure -> {
                        _labelsState.value = ScreenState.Error(result.error.toUserMessage())
                    }
                }
            }
        }

        fun onLabelPhotoIdChange(value: String) {
            _labelForm.value = _labelForm.value.copy(photoId = value)
        }

        fun onLabelTypeChange(value: String) {
            _labelForm.value = _labelForm.value.copy(labelType = value)
        }

        fun onLabelValueChange(value: String) {
            _labelForm.value = _labelForm.value.copy(value = value)
        }

        fun onLabelNotesChange(value: String) {
            _labelForm.value = _labelForm.value.copy(notes = value)
        }

        fun submitLabel() {
            val form = _labelForm.value
            if (form.submitting || form.photoId.isBlank() || form.value.isBlank()) return
            viewModelScope.launch {
                _labelForm.value = form.copy(submitting = true, error = null)
                val userId = sessionStore.userId()
                if (userId == null) {
                    _labelForm.value = form.copy(submitting = false, error = "Sign in to continue.")
                    return@launch
                }
                val request =
                    LabelCreateRequest(
                        photoId = form.photoId.trim(),
                        labelType = form.labelType.trim().ifBlank { "user_note" },
                        value = form.value.trim(),
                        notes = form.notes.trim().ifBlank { null },
                    )
                when (val result = repository.addLabel(userId, request)) {
                    is GlowResult.Success -> {
                        _labelForm.value = LabelFormState()
                        loadLabels()
                    }

                    is GlowResult.Failure -> {
                        _labelForm.value = form.copy(submitting = false, error = result.error.toUserMessage())
                    }
                }
            }
        }

        fun onModelVersionChange(value: String) {
            _reprocessModelVersion.value = value
        }

        /** Fires `POST /reprocess`, then polls `GET /reprocess/{jobId}` every 1.5s until a terminal
         * state. Never reads `processed_count` off the POST response — see [ReprocessUiState] doc. */
        fun startReprocess() {
            viewModelScope.launch {
                if (!canUsePremium.first()) {
                    _reprocessState.value = ReprocessUiState.Locked
                    return@launch
                }
                val userId = sessionStore.userId()
                if (userId == null) {
                    _reprocessState.value = ReprocessUiState.Failed("Sign in to continue.")
                    return@launch
                }
                val modelVersion = _reprocessModelVersion.value.trim().ifBlank { "deterministic-3.1" }
                _reprocessState.value = ReprocessUiState.Polling(JobStatus.QUEUED)
                when (val submitted = repository.reprocess(userId, modelVersion)) {
                    is GlowResult.Success -> {
                        pollReprocess(userId, submitted.data)
                    }

                    is GlowResult.Failure -> {
                        _reprocessState.value =
                            if (submitted.error.isPremiumGate) {
                                ReprocessUiState.Locked
                            } else {
                                ReprocessUiState.Failed(submitted.error.toUserMessage())
                            }
                    }
                }
            }
        }

        private suspend fun pollReprocess(
            userId: String,
            jobId: String,
        ) {
            while (true) {
                when (val result = repository.getReprocessStatus(userId, jobId)) {
                    is GlowResult.Success -> {
                        val job = result.data
                        when (job.status) {
                            JobStatus.QUEUED, JobStatus.RUNNING -> {
                                _reprocessState.value = ReprocessUiState.Polling(job.status)
                                delay(1_500)
                            }

                            JobStatus.COMPLETED -> {
                                _reprocessState.value =
                                    ReprocessUiState.Completed(
                                        processedCount = job.result?.processedCount,
                                        modelVersion = job.result?.modelVersion,
                                    )
                                return
                            }

                            JobStatus.FAILED, JobStatus.UNKNOWN -> {
                                _reprocessState.value = ReprocessUiState.Failed(job.error ?: "Reprocessing failed.")
                                return
                            }
                        }
                    }

                    is GlowResult.Failure -> {
                        _reprocessState.value = ReprocessUiState.Failed(result.error.toUserMessage())
                        return
                    }
                }
            }
        }

        fun dismissReprocessResult() {
            _reprocessState.value = ReprocessUiState.Idle
        }
    }
