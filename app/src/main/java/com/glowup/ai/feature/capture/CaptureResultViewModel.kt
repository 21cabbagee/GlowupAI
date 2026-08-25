package com.glowup.ai.feature.capture

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.glowup.ai.core.util.GlowResult
import com.glowup.ai.data.remote.ApiError
import com.glowup.ai.data.repository.CaptureRepository
import com.glowup.ai.data.repository.SessionRepository
import com.glowup.ai.domain.model.CaptureResult
import com.glowup.ai.domain.model.MeasurementAgreement
import com.glowup.ai.domain.model.MeasurementFeedbackRequest
import com.glowup.ai.feature.shell.GlowDestination
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * `POST /api/captures`'s response fields used here — `id`, `captured_at`, `is_baseline`, `status`,
 * `capture_quality.accepted/score/failed_checks`, and `metric.confidence/redness_score/
 * blemish_count/darkspot_area/texture_score/model_version` — must all render as the REAL values
 * from the response ([CaptureResultCache]). The legacy `ResultScreenNew` hardcoded 5 of 6 metrics
 * as `8/7/9/6/8`; every number here comes from [captureResult] instead, and is `null`-safe because
 * the backend does not guarantee every metric field is populated.
 */
sealed interface CaptureResultUiState {
    data object Loading : CaptureResultUiState
    data class Content(val captureResult: CaptureResult) : CaptureResultUiState

    /** No `GET /captures/{id}` exists on the backend, so if the in-memory
     * [CaptureResultCache] misses (process death between accept and viewing this screen), there is
     * nothing to re-fetch — this is a real, honest state, not an error to retry. */
    data object Unavailable : CaptureResultUiState
}

sealed interface MeasurementFeedbackUiState {
    data object Idle : MeasurementFeedbackUiState
    data object Submitting : MeasurementFeedbackUiState
    data object Submitted : MeasurementFeedbackUiState
    data class Failed(val message: String) : MeasurementFeedbackUiState
}

@HiltViewModel
class CaptureResultViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val captureRepository: CaptureRepository,
    private val sessionRepository: SessionRepository,
    private val resultCache: CaptureResultCache,
) : ViewModel() {

    private val captureId: String = savedStateHandle.toRoute<GlowDestination.CaptureResult>().captureId

    private val _uiState = MutableStateFlow<CaptureResultUiState>(CaptureResultUiState.Loading)
    val uiState: StateFlow<CaptureResultUiState> = _uiState.asStateFlow()

    private val _feedbackState = MutableStateFlow<MeasurementFeedbackUiState>(MeasurementFeedbackUiState.Idle)
    val feedbackState: StateFlow<MeasurementFeedbackUiState> = _feedbackState.asStateFlow()

    init {
        val cached = resultCache.get(captureId)
        _uiState.value = if (cached != null) CaptureResultUiState.Content(cached) else CaptureResultUiState.Unavailable
    }

    /** `POST /measurement-feedback` — "does this reading look fair?" */
    fun submitFeedback(agreement: MeasurementAgreement, note: String?) {
        if (_feedbackState.value == MeasurementFeedbackUiState.Submitting) return
        viewModelScope.launch {
            _feedbackState.value = MeasurementFeedbackUiState.Submitting
            val userId = sessionRepository.userIdFlow.first()
            if (userId == null) {
                _feedbackState.value = MeasurementFeedbackUiState.Failed("Please sign in again to continue.")
                return@launch
            }
            val request = MeasurementFeedbackRequest(captureId = captureId, agreement = agreement, note = note)
            _feedbackState.value = when (val result = captureRepository.addMeasurementFeedback(userId, request)) {
                is GlowResult.Success -> MeasurementFeedbackUiState.Submitted
                is GlowResult.Failure -> MeasurementFeedbackUiState.Failed(messageFor(result.error))
            }
        }
    }

    private fun messageFor(error: ApiError): String = when (error) {
        is ApiError.Validation -> error.fields.values.firstOrNull() ?: "Please check your feedback and try again."
        is ApiError.NotFound -> "That capture couldn't be found."
        is ApiError.Network -> "You appear to be offline. Please try again once connected."
        else -> "Couldn't submit feedback right now. Please try again."
    }
}
