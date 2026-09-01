package com.glowup.ai.feature.capture

import com.glowup.ai.data.remote.ApiError
import com.glowup.ai.domain.NextAction
import com.glowup.ai.domain.model.CaptureGuide
import com.glowup.ai.domain.model.CoachingTip

sealed interface CaptureGateState {
    data object Loading : CaptureGateState

    data class ConsentLocked(
        val nextAction: NextAction,
    ) : CaptureGateState

    data class Ready(
        val guide: CaptureGuide?,
    ) : CaptureGateState

    data class GateError(
        val error: ApiError,
    ) : CaptureGateState
}

sealed interface CameraPermissionUiState {
    data object Unknown : CameraPermissionUiState

    /** The user has not been asked yet; show context and let them choose. */
    data object Request : CameraPermissionUiState

    data object Granted : CameraPermissionUiState

    data object ShouldShowRationale : CameraPermissionUiState

    data object PermanentlyDenied : CameraPermissionUiState
}

sealed interface CapturePhase {
    data object Framing : CapturePhase

    data object Processing : CapturePhase

    data object Uploading : CapturePhase

    data class Accepted(
        val captureId: String,
    ) : CapturePhase

    data class Rejected(
        val coaching: List<CoachingTip>,
        val failedChecks: List<String>,
    ) : CapturePhase

    data object QueuedOffline : CapturePhase

    data class Failed(
        val message: String,
    ) : CapturePhase
}
