package com.glowup.ai.feature.routine

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.glowup.ai.core.design.LocalGlowColors
import com.glowup.ai.core.ui.EmptyState
import com.glowup.ai.core.ui.ErrorState
import com.glowup.ai.core.ui.GlowBottomSheet
import com.glowup.ai.core.ui.GlowButton
import com.glowup.ai.core.ui.GlowButtonVariant
import com.glowup.ai.core.ui.GlowCard
import com.glowup.ai.core.ui.GlowTextField
import com.glowup.ai.core.ui.GlowTopBar
import com.glowup.ai.core.ui.PollingIndicator
import com.glowup.ai.feature.capture.CaptureImageProcessor
import com.glowup.ai.feature.routine.components.AddProductSheet
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShelfScanRoute(
    onBack: () -> Unit,
    onDone: () -> Unit,
    viewModel: ShelfScanViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val pickImage =
        rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
            if (uri == null) return@rememberLauncherForActivityResult
            scope.launch {
                try {
                    val base64 =
                        withContext(Dispatchers.Default) {
                            CaptureImageProcessor.processGalleryUriToBase64(context.applicationContext, uri)
                        }
                    viewModel.submitPhoto(base64)
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (error: Throwable) {
                    viewModel.onPhotoProcessingFailed(shelfScanPhotoError(error))
                }
            }
        }

    Scaffold(topBar = { GlowTopBar(title = "Scan shelf", onBack = onBack) }) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            when (val s = state) {
                is ShelfScanUiState.Idle -> {
                    ShelfScanIntro(
                        onPickPhoto = {
                            pickImage.launch(
                                PickVisualMediaRequest(
                                    ActivityResultContracts.PickVisualMedia.ImageOnly,
                                ),
                            )
                        },
                    )
                }

                is ShelfScanUiState.Uploading -> {
                    PollingIndicator(message = "Uploading your photo…")
                }

                is ShelfScanUiState.Polling -> {
                    PollingIndicator(message = s.message)
                }

                is ShelfScanUiState.Error -> {
                    ErrorState(message = s.message, onRetry = viewModel::reset)
                }

                is ShelfScanUiState.Ready -> {
                    ShelfScanReadyContent(
                        state = s,
                        onToggle = viewModel::toggleCandidate,
                        onEditName = viewModel::editCandidateName,
                        onEditCategory = viewModel::editCandidateCategory,
                        onConfirm = viewModel::confirmSelected,
                        onOpenManualAdd = viewModel::openManualAdd,
                        onDismissManualAdd = viewModel::dismissManualAdd,
                        onSubmitManualAdd = viewModel::submitManualAdd,
                    )
                }

                is ShelfScanUiState.Done -> {
                    ShelfScanDoneContent(count = s.createdProducts.size, onDone = onDone)
                }
            }
        }
    }
}

private fun shelfScanPhotoError(error: Throwable): String =
    when {
        error.message?.contains("too small", ignoreCase = true) == true ->
            "That image is too small. Choose a photo at least 160×160 pixels."
        error.message?.contains("empty", ignoreCase = true) == true ->
            "That photo was empty. Choose another image and try again."
        error.message?.contains("large", ignoreCase = true) == true ->
            "That photo is too large to process on this device. Choose a smaller image."
        error.message?.contains("corrupt", ignoreCase = true) == true ||
            error.message?.contains("unsupported", ignoreCase = true) == true ||
            error.message?.contains("decode", ignoreCase = true) == true ->
            "That image couldn't be read. Choose another photo and try again."
        else -> "That photo couldn't be prepared. Choose another image and try again."
    }

@Composable
private fun ShelfScanIntro(onPickPhoto: () -> Unit) {
    val glow = LocalGlowColors.current
    Column {
        Text(
            "Take or choose a photo of your shelf",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = glow.ink900,
        )
        Text(
            "GlowUp AI reads product names off the photo and lets you review each one before anything is added.",
            style = MaterialTheme.typography.bodyMedium,
            color = glow.ink600,
            modifier = Modifier.padding(top = 8.dp, bottom = 20.dp),
        )
        GlowButton(text = "Choose photo", onClick = onPickPhoto)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ShelfScanReadyContent(
    state: ShelfScanUiState.Ready,
    onToggle: (Int, Boolean) -> Unit,
    onEditName: (Int, String) -> Unit,
    onEditCategory: (Int, String) -> Unit,
    onConfirm: () -> Unit,
    onOpenManualAdd: () -> Unit,
    onDismissManualAdd: () -> Unit,
    onSubmitManualAdd: (com.glowup.ai.domain.model.ProductCreateRequest) -> Unit,
) {
    val glow = LocalGlowColors.current

    if (state.candidates.isEmpty()) {
        EmptyState(
            title = "Nothing found in this photo",
            body =
                state.message
                    ?: "Automatic shelf reading isn't available right now. You can still add products by hand.",
            ctaLabel = "Add product manually",
            onCtaClick = onOpenManualAdd,
        )
    } else {
        Column(modifier = Modifier.fillMaxSize()) {
            state.explanation?.takeIf { it.isNotBlank() }?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = glow.ink600,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            }
            Text(
                "Review before adding",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = glow.ink900,
                modifier = Modifier.padding(bottom = 4.dp),
            )
            Text(
                "Uncheck anything that isn't right, or fix the name and category.",
                style = MaterialTheme.typography.bodySmall,
                color = glow.ink600,
                modifier = Modifier.padding(bottom = 12.dp),
            )
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(state.candidates.size) { index ->
                    val candidate = state.candidates[index]
                    GlowCard(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = candidate.checked, onCheckedChange = { onToggle(index, it) })
                            Column(modifier = Modifier.weight(1f)) {
                                GlowTextField(
                                    value = candidate.name,
                                    onValueChange = { onEditName(index, it) },
                                    label = "Name",
                                    enabled = candidate.checked,
                                    modifier = Modifier.padding(bottom = 8.dp),
                                )
                                GlowTextField(
                                    value = candidate.category,
                                    onValueChange = { onEditCategory(index, it) },
                                    label = "Category",
                                    enabled = candidate.checked,
                                )
                            }
                        }
                    }
                }
            }
            if (state.confirmError != null) {
                Text(
                    state.confirmError,
                    color = glow.danger,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GlowButton(
                    modifier = Modifier.weight(1f),
                    text = "Confirm selected",
                    loading = state.confirming,
                    enabled = !state.confirming && state.candidates.any { it.checked },
                    onClick = onConfirm,
                )
                GlowButton(
                    modifier = Modifier.weight(1f),
                    text = "Add another",
                    variant = GlowButtonVariant.Ghost,
                    onClick = onOpenManualAdd,
                )
            }
        }
    }

    if (state.showManualAdd) {
        GlowBottomSheet(onDismissRequest = onDismissManualAdd) {
            AddProductSheet(
                prefillBarcode = null,
                pending = state.manualAddPending,
                errorText = state.manualAddError,
                onSubmit = onSubmitManualAdd,
            )
        }
    }
}

@Composable
private fun ShelfScanDoneContent(
    count: Int,
    onDone: () -> Unit,
) {
    val glow = LocalGlowColors.current
    Column {
        Text(
            if (count == 1) "1 product added" else "$count products added",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = glow.ink900,
        )
        Text(
            "Log when you started using them from the Routine tab to begin tracking evidence.",
            style = MaterialTheme.typography.bodyMedium,
            color = glow.ink600,
            modifier = Modifier.padding(top = 8.dp, bottom = 20.dp),
        )
        GlowButton(text = "Back to routine", onClick = onDone)
    }
}
