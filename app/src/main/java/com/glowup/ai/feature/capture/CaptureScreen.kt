package com.glowup.ai.feature.capture

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview as CameraPreviewUseCase
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.glowup.ai.core.design.LocalGlowColors
import com.glowup.ai.core.ui.ErrorState
import com.glowup.ai.core.ui.GlowButton
import com.glowup.ai.core.ui.GlowButtonVariant
import com.glowup.ai.core.ui.GlowCard
import com.glowup.ai.domain.NextAction
import com.glowup.ai.domain.model.CaptureGuideState
import com.glowup.ai.feature.capture.components.CoachingTipsCard
import com.glowup.ai.feature.capture.components.OvalFramingGuide
import com.glowup.ai.feature.capture.components.QualityHudCard
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@Composable
fun CaptureRoute(
    onNavigateToResult: (captureId: String) -> Unit,
    onClose: () -> Unit,
    viewModel: CaptureViewModel = androidx.hilt.navigation.compose.hiltViewModel(),
) {
    val gateState by viewModel.gateState.collectAsStateWithLifecycle()
    val permissionState by viewModel.permissionState.collectAsStateWithLifecycle()
    val livePose by viewModel.livePose.collectAsStateWithLifecycle()
    val phase by viewModel.phase.collectAsStateWithLifecycle()
    val pendingOutbox by viewModel.pendingOutbox.collectAsStateWithLifecycle()
    val appContext = LocalContext.current.applicationContext
    LaunchedEffect(phase) {
        (phase as? CapturePhase.Accepted)?.let { onNavigateToResult(it.captureId) }
    }
    CaptureScreen(
        gateState = gateState,
        permissionState = permissionState,
        livePose = livePose,
        phase = phase,
        hasQueuedOfflineFromBefore = pendingOutbox.isNotEmpty(),
        onRetryGate = viewModel::loadGate,
        onPermissionResult = viewModel::onPermissionResult,
        onLiveQuality = viewModel::onLiveQuality,
        onCameraJpegCaptured = { bytes, rotation, baseline -> viewModel.submitCameraJpeg(bytes, rotation, baseline) },
        onGalleryPicked = { uri, baseline ->
            viewModel.submitGalleryUri(uri, { CaptureImageProcessor.processGalleryUri(appContext, it) }, baseline)
        },
        onRetake = viewModel::retakeAfterRejectionOrFailure,
        onClose = onClose,
    )
}

@Composable
private fun CaptureScreen(
    gateState: CaptureGateState,
    permissionState: CameraPermissionUiState,
    livePose: LiveFaceQuality?,
    phase: CapturePhase,
    hasQueuedOfflineFromBefore: Boolean,
    onRetryGate: () -> Unit,
    onPermissionResult: (Boolean, Boolean, Boolean) -> Unit,
    onLiveQuality: (LiveFaceQuality) -> Unit,
    onCameraJpegCaptured: (ByteArray, Int, Boolean) -> Unit,
    onGalleryPicked: (Uri, Boolean) -> Unit,
    onRetake: () -> Unit,
    onClose: () -> Unit,
) {
    val glow = LocalGlowColors.current
    Box(Modifier.fillMaxSize()) {
        when (gateState) {
            CaptureGateState.Loading -> LoadingScaffold()
            is CaptureGateState.GateError -> ErrorScaffold(messageForGate(gateState.error), onRetryGate)
            is CaptureGateState.ConsentLocked -> ConsentLockedScaffold(gateState.nextAction)
            is CaptureGateState.Ready -> {
                val baseline = gateState.guide?.state == CaptureGuideState.BASELINE_NEEDED
                CaptureReadyContent(
                    guideState = gateState.guide?.state,
                    guideMessage = gateState.guide?.message,
                    permissionState = permissionState,
                    livePose = livePose,
                    phase = phase,
                    isBaseline = baseline,
                    hasQueuedOfflineFromBefore = hasQueuedOfflineFromBefore,
                    onPermissionResult = onPermissionResult,
                    onLiveQuality = onLiveQuality,
                    onCameraJpegCaptured = onCameraJpegCaptured,
                    onGalleryPicked = onGalleryPicked,
                    onRetake = onRetake,
                    onClose = onClose,
                )
            }
        }
        IconButton(
            onClick = onClose,
            modifier = Modifier.align(Alignment.TopStart).statusBarsPadding().padding(8.dp).size(48.dp)
                .semantics { contentDescription = "Close capture" },
        ) { Icon(Icons.Filled.Close, contentDescription = null, tint = if (gateState is CaptureGateState.Ready) Color.White else glow.ink900) }
    }
}

@Composable private fun LoadingScaffold() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(modifier = Modifier.semantics { contentDescription = "Loading capture" })
    }
}

@Composable private fun ErrorScaffold(message: String, onRetry: () -> Unit) {
    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        ErrorState(message = message, onRetry = onRetry)
    }
}

@Composable private fun ConsentLockedScaffold(nextAction: NextAction) {
    val glow = LocalGlowColors.current
    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        GlowCard(modifier = Modifier.fillMaxWidth()) {
            Text("Photo tracking is locked", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = glow.ink900)
            Text(
                when (nextAction) {
                    NextAction.RequestConsent -> "Review and accept facial-data consent before taking a photo."
                    NextAction.ReviewConsent -> "You declined facial-data consent. Review it again from Account to unlock capture."
                    NextAction.SignIn -> "Please sign in to continue."
                    else -> "Capture is not available right now."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = glow.ink600,
                modifier = Modifier.padding(top = 8.dp),
            )
            Text("No camera or photo is opened while capture is locked.", style = MaterialTheme.typography.bodySmall, color = glow.ink600, modifier = Modifier.padding(top = 12.dp))
        }
    }
}

@Composable
private fun CaptureReadyContent(
    guideState: CaptureGuideState?, guideMessage: String?, permissionState: CameraPermissionUiState,
    livePose: LiveFaceQuality?, phase: CapturePhase, isBaseline: Boolean, hasQueuedOfflineFromBefore: Boolean,
    onPermissionResult: (Boolean, Boolean, Boolean) -> Unit, onLiveQuality: (LiveFaceQuality) -> Unit,
    onCameraJpegCaptured: (ByteArray, Int, Boolean) -> Unit, onGalleryPicked: (Uri, Boolean) -> Unit,
    onRetake: () -> Unit, onClose: () -> Unit,
) {
    val context = LocalContext.current
    val activity = LocalActivity.current
    var permissionWasRequested by rememberSaveable { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        permissionWasRequested = true
        val rationale = activity?.let { ActivityCompat.shouldShowRequestPermissionRationale(it, Manifest.permission.CAMERA) } == true
        onPermissionResult(granted, rationale, true)
    }
    fun refreshPermission() {
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        val rationale = activity?.let { ActivityCompat.shouldShowRequestPermissionRationale(it, Manifest.permission.CAMERA) } == true
        onPermissionResult(granted, rationale, permissionWasRequested)
    }
    LaunchedEffect(Unit) { refreshPermission() }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, permissionWasRequested) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_RESUME) refreshPermission() }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri -> uri?.let { onGalleryPicked(it, isBaseline) } }
    when (permissionState) {
        CameraPermissionUiState.Unknown, CameraPermissionUiState.Request, CameraPermissionUiState.ShouldShowRationale ->
            PermissionRationaleScaffold(onRequest = { permissionLauncher.launch(Manifest.permission.CAMERA) }, onGallery = { galleryLauncher.launch("image/*") }, onClose = onClose)
        CameraPermissionUiState.PermanentlyDenied -> PermissionDeniedScaffold(
            onOpenSettings = { context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))) },
            onGallery = { galleryLauncher.launch("image/*") }, onClose = onClose,
        )
        CameraPermissionUiState.Granted -> when (phase) {
            CapturePhase.Framing -> FramingContent(guideState, guideMessage, livePose, hasQueuedOfflineFromBefore, onLiveQuality, { b, r -> onCameraJpegCaptured(b, r, isBaseline) }, { galleryLauncher.launch("image/*") }, onRetake)
            CapturePhase.Processing -> ProgressScaffold("Preparing your photo…")
            CapturePhase.Uploading -> ProgressScaffold("Uploading securely…")
            is CapturePhase.Rejected -> Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) { CoachingTipsCard(coaching = phase.coaching, failedChecks = phase.failedChecks, onRetake = onRetake) }
            CapturePhase.QueuedOffline -> QueuedOfflineScaffold(onClose)
            is CapturePhase.Failed -> Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) { ErrorState(message = phase.message, retryLabel = "Try again", onRetry = onRetake) }
            is CapturePhase.Accepted -> ProgressScaffold("Saved!")
        }
    }
}

@Composable private fun PermissionRationaleScaffold(onRequest: () -> Unit, onGallery: () -> Unit, onClose: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(32.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Camera access needed", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text("Use your front camera for a guided cosmetic skin-tracking photo. You can also import an existing photo instead.", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 12.dp, bottom = 24.dp))
        GlowButton(text = "Allow camera access", onClick = onRequest)
        GlowButton(text = "Import from gallery", onClick = onGallery, variant = GlowButtonVariant.Secondary, modifier = Modifier.padding(top = 8.dp))
        GlowButton(text = "Not now", onClick = onClose, variant = GlowButtonVariant.Ghost, modifier = Modifier.padding(top = 8.dp))
    }
}

@Composable private fun PermissionDeniedScaffold(onOpenSettings: () -> Unit, onGallery: () -> Unit, onClose: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(32.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Camera access is off", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text("Turn camera access back on in Settings, or import a photo from your gallery.", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 12.dp, bottom = 24.dp))
        GlowButton(text = "Open settings", onClick = onOpenSettings)
        GlowButton(text = "Import from gallery", onClick = onGallery, variant = GlowButtonVariant.Secondary, modifier = Modifier.padding(top = 8.dp))
        GlowButton(text = "Cancel", onClick = onClose, variant = GlowButtonVariant.Ghost, modifier = Modifier.padding(top = 8.dp))
    }
}

@Composable private fun ProgressScaffold(label: String) {
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) { CircularProgressIndicator(); Text(label, modifier = Modifier.padding(top = 16.dp)) }
}
@Composable private fun QueuedOfflineScaffold(onDone: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(32.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Status unknown", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text("We couldn't confirm the upload. The photo is stored on this device and will retry automatically when online; don't retake it.", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 12.dp, bottom = 24.dp))
        GlowButton(text = "Done", onClick = onDone)
    }
}

@Composable private fun FramingContent(
    guideState: CaptureGuideState?, guideMessage: String?, livePose: LiveFaceQuality?, queued: Boolean,
    onLiveQuality: (LiveFaceQuality) -> Unit, onShutter: (ByteArray, Int) -> Unit, onGallery: () -> Unit, onRetake: () -> Unit,
) {
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var taking by remember { mutableStateOf(false) }
    var cameraError by remember { mutableStateOf<String?>(null) }
    val glow = LocalGlowColors.current
    val context = LocalContext.current
    if (cameraError != null) {
        Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) { ErrorState(message = cameraError!!, retryLabel = "Retry camera", onRetry = { cameraError = null; onRetake() }) }
        return
    }
    Box(Modifier.fillMaxSize()) {
        CameraPreview(onImageCaptureReady = { imageCapture = it }, onLiveQuality = onLiveQuality, onError = { cameraError = it })
        OvalFramingGuide(isFramingGood = livePose?.isFramingGood == true)
        Column(Modifier.fillMaxSize().statusBarsPadding().padding(16.dp)) {
            val label = when (guideState) { CaptureGuideState.BASELINE_NEEDED -> "Baseline capture"; CaptureGuideState.DUE -> "Capture due"; CaptureGuideState.OVERDUE -> "Capture overdue"; CaptureGuideState.SCHEDULED -> "Next capture scheduled"; else -> null }
            label?.let { Text(it, color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.CenterHorizontally)) }
            if (!guideMessage.isNullOrBlank()) Text(guideMessage, color = Color.White.copy(alpha = .85f), style = MaterialTheme.typography.bodySmall, modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 4.dp))
            if (queued) Text("A previous capture is still uploading in the background.", color = glow.honey300, style = MaterialTheme.typography.labelSmall, modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 4.dp))
        }
        Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth().navigationBarsPadding().padding(bottom = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            QualityHudCard(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp), livePose)
            Row(Modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onGallery, modifier = Modifier.size(48.dp).semantics { contentDescription = "Import photo from gallery" }) { Icon(Icons.Filled.PhotoLibrary, null, tint = Color.White) }
                ShutterButton(enabled = !taking && imageCapture != null) {
                    val capture = imageCapture ?: return@ShutterButton
                    taking = true
                    capture.takeJpegFile(context, onCaptured = { bytes -> taking = false; onShutter(bytes, 0) }, onError = { taking = false; cameraError = "Couldn't take that photo. Please try again." })
                }
                Box(Modifier.size(48.dp))
            }
        }
    }
}

@Composable private fun ShutterButton(enabled: Boolean, onClick: () -> Unit) {
    val glow = LocalGlowColors.current
    Box(Modifier.size(72.dp).background(if (enabled) Color.White else Color.White.copy(alpha = .4f), CircleShape).semantics { contentDescription = "Take photo" }, contentAlignment = Alignment.Center) {
        IconButton(onClick = onClick, enabled = enabled, modifier = Modifier.size(64.dp)) { Box(Modifier.size(56.dp).background(glow.honey500, CircleShape)) }
    }
}

@Composable private fun CameraPreview(onImageCaptureReady: (ImageCapture) -> Unit, onLiveQuality: (LiveFaceQuality) -> Unit, onError: (String) -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val analysisExecutor = remember { Executors.newSingleThreadExecutor() }
    val analyzer = remember { FaceQualityAnalyzer(onLiveQuality) }
    val providerFuture = remember(context) { ProcessCameraProvider.getInstance(context) }
    var previewView by remember { mutableStateOf<PreviewView?>(null) }
    AndroidView(modifier = Modifier.fillMaxSize(), factory = { ctx -> PreviewView(ctx).also { previewView = it } })
    DisposableEffect(providerFuture, lifecycleOwner, previewView) {
        val view = previewView
        if (view == null) {
            onDispose { }
        } else {
            var active = true
            var provider: ProcessCameraProvider? = null
            providerFuture.addListener({
                if (!active) return@addListener
                runCatching {
                    provider = providerFuture.get()
                    val preview = CameraPreviewUseCase.Builder().build().also { it.surfaceProvider = view.surfaceProvider }
                    val imageCapture = ImageCapture.Builder().setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY).build()
                    val analysis = ImageAnalysis.Builder().setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build().also { it.setAnalyzer(analysisExecutor, analyzer) }
                    provider?.unbindAll()
                    provider?.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_FRONT_CAMERA, preview, imageCapture, analysis)
                    onImageCaptureReady(imageCapture)
                }.onFailure { if (active) onError("Camera couldn't start. Check camera access and try again.") }
            }, ContextCompat.getMainExecutor(context))
            onDispose { active = false; provider?.unbindAll(); analyzer.close(); analysisExecutor.shutdown() }
        }
    }
}

private fun ImageCapture.takeJpegFile(context: android.content.Context, onCaptured: (ByteArray) -> Unit, onError: (String) -> Unit) {
    val output = runCatching { File.createTempFile("glowup_capture_", ".jpg", context.cacheDir) }.getOrElse {
        onError("Couldn't prepare the camera. Please try again.")
        return
    }
    val executor = Executors.newSingleThreadExecutor()
    takePicture(ImageCapture.OutputFileOptions.Builder(output).build(), executor, object : ImageCapture.OnImageSavedCallback {
        override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
            try { onCaptured(output.readBytes()) } catch (_: Throwable) { onError("Couldn't read the captured photo.") }
            finally { output.delete(); executor.shutdown() }
        }
        override fun onError(exception: ImageCaptureException) { output.delete(); executor.shutdown(); onError("Couldn't take that photo.") }
    })
}
private fun messageForGate(error: com.glowup.ai.data.remote.ApiError): String = when (error) {
    is com.glowup.ai.data.remote.ApiError.Network -> "No connection. Check your network and try again."
    is com.glowup.ai.data.remote.ApiError.Unauthorized -> "Your session expired. Please sign in again."
    is com.glowup.ai.data.remote.ApiError.Server -> "Something went wrong on our end. Please try again."
    else -> "Couldn't load capture right now. Please try again."
}
