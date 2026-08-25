package com.glowup.ai.feature.insights

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.glowup.ai.core.design.GlowSpacing
import com.glowup.ai.core.design.LocalGlowColors
import com.glowup.ai.core.ui.DisclaimerNote
import com.glowup.ai.core.ui.EmptyState
import com.glowup.ai.core.ui.ErrorState
import com.glowup.ai.core.ui.GlowButton
import com.glowup.ai.core.ui.GlowButtonVariant
import com.glowup.ai.core.ui.GlowCard
import com.glowup.ai.core.ui.GlowTextField
import com.glowup.ai.core.ui.GlowTopBar
import com.glowup.ai.core.ui.LockedCard
import com.glowup.ai.core.ui.PollingIndicator
import com.glowup.ai.core.ui.SectionHeader
import com.glowup.ai.core.ui.ShimmerSkeleton
import com.glowup.ai.domain.model.JobStatus
import com.glowup.ai.domain.model.Label
import com.glowup.ai.feature.insights.components.LabelCard

private const val INSIGHTS_DISCLAIMER =
    "GlowUp AI tracks cosmetic skin appearance over time. It is not a diagnosis and does not " +
        "replace a dermatologist."

@Composable
fun InsightsHubScreen(
    onOpenQna: () -> Unit,
    onOpenContextLog: () -> Unit,
    onOpenRootCause: () -> Unit,
    onOpenBudgetOptimizer: () -> Unit,
    onOpenDermExport: () -> Unit,
    onUpgrade: () -> Unit,
    viewModel: InsightsHubViewModel = hiltViewModel(),
) {
    val canUsePremium by viewModel.canUsePremium.collectAsState()
    val labelsState by viewModel.labelsState.collectAsState()
    val labelForm by viewModel.labelForm.collectAsState()
    val reprocessState by viewModel.reprocessState.collectAsState()
    val reprocessModelVersion by viewModel.reprocessModelVersion.collectAsState()

    Scaffold(topBar = { GlowTopBar(title = "Insights") }) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxWidth().padding(padding),
            contentPadding = PaddingValues(GlowSpacing.md),
            verticalArrangement = Arrangement.spacedBy(GlowSpacing.sm),
        ) {
            item { DisclaimerNote(text = INSIGHTS_DISCLAIMER) }

            item {
                NavCard(
                    icon = Icons.Filled.Insights,
                    title = "Ask about your skin",
                    body = "Cited, grounded answers from your own tracked history.",
                    onClick = onOpenQna,
                )
            }
            item {
                NavCard(
                    icon = Icons.Filled.Timeline,
                    title = "Context log",
                    body = "Log sleep, travel, stress, and other context.",
                    onClick = onOpenContextLog,
                )
            }
            item {
                NavCard(
                    icon = Icons.Filled.QueryStats,
                    title = "Root-cause correlations",
                    body = "See what your logged context correlates with.",
                    onClick = onOpenRootCause,
                )
            }
            item {
                NavCard(
                    icon = Icons.Filled.AttachMoney,
                    title = "Budget optimizer",
                    body = "Flag routine products that may not be earning their spot.",
                    onClick = onOpenBudgetOptimizer,
                )
            }
            item {
                NavCard(
                    icon = Icons.Filled.Description,
                    title = "Dermatologist export",
                    body = "A printable summary to bring to an appointment.",
                    onClick = onOpenDermExport,
                )
            }

            item { SectionHeader(title = "Annotations", modifier = Modifier.padding(top = GlowSpacing.md)) }
            item { LabelForm(labelForm, viewModel) }
            item {
                when (val current = labelsState) {
                    ScreenState.Loading -> ShimmerSkeleton(height = 64.dp)
                    is ScreenState.Error -> ErrorState(message = current.message, onRetry = viewModel::loadLabels)
                    is ScreenState.Empty -> EmptyState(title = current.title, body = current.body, ctaLabel = "Refresh", onCtaClick = viewModel::loadLabels)
                    is ScreenState.Content -> LabelsList(current.value)
                    ScreenState.Locked -> Unit
                }
            }

            item { SectionHeader(title = "Reprocess history", modifier = Modifier.padding(top = GlowSpacing.md)) }
            item {
                ReprocessSection(
                    state = reprocessState,
                    modelVersion = reprocessModelVersion,
                    canUsePremium = canUsePremium,
                    onModelVersionChange = viewModel::onModelVersionChange,
                    onStart = viewModel::startReprocess,
                    onUpgrade = onUpgrade,
                    onDismiss = viewModel::dismissReprocessResult,
                )
            }
        }
    }
}

@Composable
private fun NavCard(icon: ImageVector, title: String, body: String, onClick: () -> Unit) {
    val glow = LocalGlowColors.current
    GlowCard(onClick = onClick, contentDescription = title) {
        androidx.compose.foundation.layout.Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = glow.honey700)
            Column(modifier = Modifier.weight(1f).padding(start = GlowSpacing.sm)) {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = glow.ink900)
                Text(body, style = MaterialTheme.typography.bodySmall, color = glow.ink600)
            }
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = glow.ink600)
        }
    }
}

@Composable
private fun LabelForm(form: LabelFormState, viewModel: InsightsHubViewModel) {
    val glow = LocalGlowColors.current
    GlowCard {
        Text(
            "Labels are your own notes — never an automated classification.",
            style = MaterialTheme.typography.bodySmall,
            color = glow.ink600,
        )
        GlowTextField(
            modifier = Modifier.padding(top = GlowSpacing.sm),
            value = form.photoId,
            onValueChange = viewModel::onLabelPhotoIdChange,
            label = "Capture ID",
            placeholder = "cap_xxxxxxxx",
        )
        GlowTextField(
            modifier = Modifier.padding(top = GlowSpacing.sm),
            value = form.labelType,
            onValueChange = viewModel::onLabelTypeChange,
            label = "Type",
            supportingText = "e.g. user_note",
        )
        GlowTextField(
            modifier = Modifier.padding(top = GlowSpacing.sm),
            value = form.value,
            onValueChange = viewModel::onLabelValueChange,
            label = "Note",
        )
        GlowTextField(
            modifier = Modifier.padding(top = GlowSpacing.sm),
            value = form.notes,
            onValueChange = viewModel::onLabelNotesChange,
            label = "Notes (optional)",
        )
        if (form.error != null) {
            Text(form.error, color = glow.danger, modifier = Modifier.padding(top = GlowSpacing.xs))
        }
        GlowButton(
            modifier = Modifier.padding(top = GlowSpacing.sm),
            text = "Save note",
            onClick = viewModel::submitLabel,
            loading = form.submitting,
            enabled = form.photoId.isNotBlank() && form.value.isNotBlank() && !form.submitting,
        )
    }
}

@Composable
private fun LabelsList(labels: List<Label>) {
    Column(verticalArrangement = Arrangement.spacedBy(GlowSpacing.sm)) {
        labels.forEach { label ->
            LabelCard(
                labelType = label.labelType,
                value = label.value,
                photoId = label.photoId,
                notes = label.notes,
                createdAt = label.createdAt,
            )
        }
    }
}

@Composable
private fun ReprocessSection(
    state: ReprocessUiState,
    modelVersion: String,
    canUsePremium: Boolean,
    onModelVersionChange: (String) -> Unit,
    onStart: () -> Unit,
    onUpgrade: () -> Unit,
    onDismiss: () -> Unit,
) {
    val glow = LocalGlowColors.current

    if (state is ReprocessUiState.Locked || !canUsePremium) {
        LockedCard(
            title = "Historical reprocessing is Premium",
            body = "Recompute your history against a newer model version.",
            onUnlock = onUpgrade,
        )
        return
    }

    GlowCard {
        DisclaimerNote(text = "Recalculating your history may change metric values you've already seen.")
        when (state) {
            is ReprocessUiState.Idle -> {
                GlowTextField(
                    modifier = Modifier.padding(top = GlowSpacing.sm),
                    value = modelVersion,
                    onValueChange = onModelVersionChange,
                    label = "Model version",
                )
                GlowButton(
                    modifier = Modifier.padding(top = GlowSpacing.sm),
                    text = "Reprocess history",
                    onClick = onStart,
                    variant = GlowButtonVariant.Secondary,
                )
            }
            is ReprocessUiState.Polling -> PollingIndicator(
                modifier = Modifier.padding(top = GlowSpacing.sm),
                message = when (state.status) {
                    JobStatus.QUEUED -> "Queued — recalculating history…"
                    else -> "Recalculating history — values may change…"
                },
            )
            is ReprocessUiState.Completed -> {
                Text(
                    text = "Done. ${state.processedCount ?: 0} captures reprocessed" +
                        (state.modelVersion?.let { " with $it" } ?: "") + ".",
                    color = glow.ink900,
                    modifier = Modifier.padding(top = GlowSpacing.sm),
                )
                GlowButton(
                    modifier = Modifier.padding(top = GlowSpacing.sm),
                    text = "Dismiss",
                    onClick = onDismiss,
                    variant = GlowButtonVariant.Ghost,
                )
            }
            is ReprocessUiState.Failed -> {
                ErrorState(
                    modifier = Modifier.padding(top = GlowSpacing.sm),
                    message = state.message,
                    onRetry = onStart,
                )
            }
            ReprocessUiState.Locked -> Unit
        }
    }
}
