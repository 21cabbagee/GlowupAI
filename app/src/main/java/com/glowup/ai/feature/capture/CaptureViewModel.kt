package com.glowup.ai.feature.capture

import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.glowup.ai.core.util.GlowResult
import com.glowup.ai.data.local.CaptureOutboxEntity
import com.glowup.ai.data.remote.ApiError
import com.glowup.ai.data.repository.CaptureRepository
import com.glowup.ai.data.repository.HomeRepository
import com.glowup.ai.data.repository.SessionRepository
import com.glowup.ai.data.telemetry.Telemetry
import com.glowup.ai.data.telemetry.TelemetryEvent
import com.glowup.ai.domain.SessionState
import com.glowup.ai.domain.SessionStateMachine
import com.glowup.ai.domain.model.CaptureCreateRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class CaptureViewModel @Inject constructor(
    private val captureRepository: CaptureRepository,
    private val sessionRepository: SessionRepository,
    private val homeRepository: HomeRepository,
    private val resultCache: CaptureResultCache,
    private val telemetry: Telemetry,
) : ViewModel() {
    private val _gateState = MutableStateFlow<CaptureGateState>(CaptureGateState.Loading)
    val gateState: StateFlow<CaptureGateState> = _gateState.asStateFlow()
    private val _permissionState = MutableStateFlow<CameraPermissionUiState>(CameraPermissionUiState.Unknown)
    val permissionState: StateFlow<CameraPermissionUiState> = _permissionState.asStateFlow()
    private val _livePose = MutableStateFlow<LiveFaceQuality?>(null)
    val livePose: StateFlow<LiveFaceQuality?> = _livePose.asStateFlow()
    private val _phase = MutableStateFlow<CapturePhase>(CapturePhase.Framing)
    val phase: StateFlow<CapturePhase> = _phase.asStateFlow()
    private val _userId = MutableStateFlow<String?>(null)
    private var gateJob: Job? = null

    val pendingOutbox: StateFlow<List<CaptureOutboxEntity>> = _userId
        .flatMapLatest { id -> if (id == null) flowOf(emptyList()) else captureRepository.outboxForUser(id) }
        .map { rows -> rows.filter { it.status == "pending" } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init { loadGate() }

    fun loadGate() {
        gateJob?.cancel()
        gateJob = viewModelScope.launch {
            _gateState.value = CaptureGateState.Loading
            val userId = sessionRepository.userIdFlow.first()
            if (userId == null) {
                _gateState.value = CaptureGateState.GateError(ApiError.Unauthorized)
                return@launch
            }
            _userId.value = userId
            when (val state = SessionStateMachine.onProfileResult(sessionRepository.refreshProfile(userId))) {
                is SessionState.ConsentRequired -> _gateState.value = CaptureGateState.ConsentLocked(state.nextAction)
                is SessionState.ConsentDeclined -> _gateState.value = CaptureGateState.ConsentLocked(state.nextAction)
                is SessionState.BaselineNeeded, is SessionState.Ready -> {
                    when (val guide = homeRepository.getCaptureGuide(userId)) {
                        is GlowResult.Success -> _gateState.value = CaptureGateState.Ready(guide.data)
                        is GlowResult.Failure -> _gateState.value = CaptureGateState.Ready(null)
                    }
                }
                is SessionState.Unrecoverable -> _gateState.value = CaptureGateState.GateError(state.reason)
                SessionState.NoUser -> _gateState.value = CaptureGateState.GateError(ApiError.Unauthorized)
                SessionState.Authenticating, SessionState.ProfileLoading ->
                    _gateState.value = CaptureGateState.GateError(ApiError.Unknown(IllegalStateException("Session is still loading")))
            }
        }
    }

    /** [requestWasMade] distinguishes first launch from a real permanent denial. */
    fun onPermissionResult(granted: Boolean, shouldShowRationale: Boolean, requestWasMade: Boolean = true) {
        _permissionState.value = when {
            granted -> CameraPermissionUiState.Granted
            shouldShowRationale -> CameraPermissionUiState.ShouldShowRationale
            !requestWasMade -> CameraPermissionUiState.Request
            else -> CameraPermissionUiState.PermanentlyDenied
        }
    }
    fun onLiveQuality(quality: LiveFaceQuality) { _livePose.value = quality }

    fun submitCameraJpeg(jpegBytes: ByteArray, rotationDegrees: Int, isBaseline: Boolean) {
        val userId = _userId.value ?: return
        submitProcessed(userId, isBaseline) {
            withContext(Dispatchers.Default) { CaptureImageProcessor.processCameraJpeg(jpegBytes, rotationDegrees) }
        }
    }

    fun submitGalleryUri(uri: Uri, decode: suspend (Uri) -> Bitmap, isBaseline: Boolean) {
        val userId = _userId.value ?: return
        // A camera pose is not valid for a different gallery image.
        _livePose.value = null
        submitProcessed(userId, isBaseline) {
            withContext(Dispatchers.Default) { CaptureImageProcessor.normalizeBitmap(decode(uri)) }
        }
    }

    private fun submitProcessed(userId: String, isBaseline: Boolean, decode: suspend () -> Bitmap) {
        if (_phase.value != CapturePhase.Framing || _gateState.value !is CaptureGateState.Ready) return
        // Reserve the input synchronously before launching decode work. Two quick taps otherwise
        // both observe Framing and can enqueue duplicate uploads.
        _phase.value = CapturePhase.Processing
        viewModelScope.launch {
            val bitmap = runCatching { decode() }.getOrElse {
                _phase.value = CapturePhase.Failed("Couldn't process that photo. Please choose another.")
                return@launch
            }
            submitBitmap(bitmap, userId, isBaseline, _livePose.value?.pose)
        }
    }

    private suspend fun submitBitmap(bitmap: Bitmap, userId: String, isBaseline: Boolean, pose: com.glowup.ai.domain.model.CapturePose?) {
        if (!CaptureImageProcessor.meetsMinimumDimensions(bitmap)) {
            bitmap.recycle()
            _phase.value = CapturePhase.Failed("That image is too small — please use a photo at least 160×160 pixels.")
            return
        }
        val base64 = try {
            withContext(Dispatchers.Default) { CaptureImageProcessor.encodeToBase64Jpeg(bitmap) }
        } catch (_: Throwable) {
            _phase.value = CapturePhase.Failed("Couldn't prepare that photo. Please try again.")
            return
        } finally {
            if (!bitmap.isRecycled) bitmap.recycle()
        }
        val request = CaptureCreateRequest(
            userId = userId,
            imageBase64 = base64,
            pose = pose,
            isBaseline = isBaseline,
            vertical = "skin",
            deviceMeta = mapOf("platform" to "android", "device_model" to Build.MODEL.orEmpty()),
        )
        _phase.value = CapturePhase.Uploading
        val result = runCatching { captureRepository.submitCapture(request) }.getOrElse {
            _phase.value = CapturePhase.Failed("Couldn't save that capture. Please try again.")
            return
        }
        _phase.value = when (result) {
            is GlowResult.Success -> {
                resultCache.put(result.data)
                CapturePhase.Accepted(result.data.id)
            }
            is GlowResult.Failure -> when (val error = result.error) {
                is ApiError.CaptureQualityRejected -> {
                    telemetry.track(TelemetryEvent.CAPTURE_REJECTED, metadata = mapOf("reason" to "quality"))
                    CapturePhase.Rejected(error.coaching, error.quality.failedChecks)
                }
                is ApiError.Network -> CapturePhase.QueuedOffline
                else -> CapturePhase.Failed(messageFor(error))
            }
        }
    }

    fun retakeAfterRejectionOrFailure() {
        if (_phase.value is CapturePhase.Rejected || _phase.value is CapturePhase.Failed) {
            _phase.value = CapturePhase.Framing
        }
    }

    private fun messageFor(error: ApiError): String = when (error) {
        is ApiError.ConsentRequired -> "Facial-data consent is required before capturing."
        is ApiError.Validation -> error.fields.values.firstOrNull() ?: "Please check your photo and try again."
        is ApiError.NotFound -> "That account couldn't be found. Please sign in again."
        is ApiError.Conflict -> "That capture already exists."
        is ApiError.Server -> "Something went wrong on our end. Please try again shortly."
        is ApiError.Unauthorized -> "Please sign in again to continue."
        is ApiError.PremiumRequired -> "This feature requires Premium."
        else -> "Something went wrong. Please try again."
    }
}
