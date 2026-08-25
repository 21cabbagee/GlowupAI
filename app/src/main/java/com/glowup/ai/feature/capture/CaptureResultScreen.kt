package com.glowup.ai.feature.capture

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.glowup.ai.core.design.LocalGlowColors
import com.glowup.ai.core.ui.DisclaimerNote
import com.glowup.ai.core.ui.EmptyState
import com.glowup.ai.core.ui.GlowButton
import com.glowup.ai.core.ui.GlowButtonVariant
import com.glowup.ai.core.ui.GlowCard
import com.glowup.ai.core.ui.GlowTextField
import com.glowup.ai.core.ui.GlowTopBar
import com.glowup.ai.core.ui.MetricBar
import com.glowup.ai.core.ui.StatTile
import com.glowup.ai.domain.model.AppearanceMetric
import com.glowup.ai.domain.model.CaptureResult
import com.glowup.ai.domain.model.MeasurementAgreement
import java.util.Locale

/**
 * `POST /api/captures`'s accepted response, rendered with the REAL metrics from the server
 * (ANDROID_PLAN.md 3.2 item 7). The legacy `ResultScreenNew` hardcoded 5 of 6 metrics as
 * `8/7/9/6/8`; every value below is `metric.<field>` off [CaptureResultCache] or an explicit
 * "not available yet" label — never a placeholder number.
 */
@Composable
fun CaptureResultRoute(
    onDone: () -> Unit,
    viewModel: CaptureResultViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val feedbackState by viewModel.feedbackState.collectAsStateWithLifecycle()

    CaptureResultScreen(
        uiState = uiState,
        feedbackState = feedbackState,
        onSubmitFeedback = viewModel::submitFeedback,
        onDone = onDone,
    )
}

@Composable
private fun CaptureResultScreen(
    uiState: CaptureResultUiState,
    feedbackState: MeasurementFeedbackUiState,
    onSubmitFeedback: (MeasurementAgreement, String?) -> Unit,
    onDone: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        GlowTopBar(title = "Capture result")
        when (uiState) {
            is CaptureResultUiState.Loading -> Column(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("Loading…", style = MaterialTheme.typography.bodyMedium)
            }

            is CaptureResultUiState.Unavailable -> Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
                EmptyState(
                    title = "Result no longer available",
                    body = "This can happen after the app restarts. Your capture was saved — check it from your history on Home.",
                    ctaLabel = "Back to Home",
                    onCtaClick = onDone,
                )
            }

            is CaptureResultUiState.Content -> CaptureResultContent(
                result = uiState.captureResult,
                feedbackState = feedbackState,
                onSubmitFeedback = onSubmitFeedback,
                onDone = onDone,
            )
        }
    }
}

@Composable
private fun CaptureResultContent(
    result: CaptureResult,
    feedbackState: MeasurementFeedbackUiState,
    onSubmitFeedback: (MeasurementAgreement, String?) -> Unit,
    onDone: () -> Unit,
) {
    val glow = LocalGlowColors.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
    ) {
        val statusLabel = if (result.captureQuality.accepted) "Accepted" else "Needs review"
        Text(
            text = if (result.isBaseline) "Baseline capture $statusLabel" else "Capture $statusLabel",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = glow.ink900,
        )
        Text(
            text = "Captured ${result.capturedAt}",
            style = MaterialTheme.typography.bodySmall,
            color = glow.ink600,
            modifier = Modifier.padding(top = 4.dp, bottom = 16.dp),
        )

        MetricsGrid(metric = result.metric)

        DisclaimerNote(
            modifier = Modifier.padding(top = 16.dp),
            text = "GlowUp AI tracks cosmetic skin appearance over time. This is not a diagnosis and " +
                "does not replace a dermatologist.",
        )

        result.metric.modelVersion?.let { version ->
            Text(
                text = "Model version: $version",
                style = MaterialTheme.typography.labelSmall,
                color = glow.ink600,
                modifier = Modifier.padding(top = 12.dp),
            )
        }

        MeasurementFeedbackSection(
            feedbackState = feedbackState,
            onSubmitFeedback = onSubmitFeedback,
            modifier = Modifier.padding(top = 24.dp),
        )

        GlowButton(
            modifier = Modifier.fillMaxWidth().padding(top = 24.dp, bottom = 12.dp),
            text = "Done",
            onClick = onDone,
            variant = GlowButtonVariant.Primary,
        )
    }
}

@Composable
private fun MetricsGrid(metric: AppearanceMetric) {
    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatTile(
                modifier = Modifier.weight(1f),
                label = "Redness",
                value = metric.rednessScore.formatOrPending(),
            )
            StatTile(
                modifier = Modifier.weight(1f),
                label = "Blemishes",
                value = metric.blemishCount?.let { formatWhole(it) } ?: "Pending",
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            StatTile(
                modifier = Modifier.weight(1f),
                label = "Dark spots",
                value = metric.darkspotArea.formatOrPending(),
            )
            StatTile(
                modifier = Modifier.weight(1f),
                label = "Texture",
                value = metric.textureScore.formatOrPending(),
            )
        }

        GlowCard(modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
            val confidence = metric.confidence
            if (confidence != null) {
                MetricBar(
                    label = "Confidence" + (metric.confidenceLabel?.let { " ($it)" } ?: ""),
                    value = confidence.toFloat(),
                    max = 1f,
                    valueText = String.format(Locale.US, "%.2f", confidence),
                )
            } else {
                Text(
                    text = "Confidence not available for this capture yet.",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

private fun Double?.formatOrPending(): String =
    this?.let { String.format(Locale.US, "%.2f", it) } ?: "Pending"

private fun formatWhole(value: Double): String = value.toInt().toString()

@Composable
private fun MeasurementFeedbackSection(
    feedbackState: MeasurementFeedbackUiState,
    onSubmitFeedback: (MeasurementAgreement, String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val glow = LocalGlowColors.current
    var note by remember { mutableStateOf("") }

    GlowCard(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "Does this reading look fair?",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = glow.ink900,
        )
        when (feedbackState) {
            is MeasurementFeedbackUiState.Submitted -> Text(
                text = "Thanks — your feedback was recorded.",
                style = MaterialTheme.typography.bodyMedium,
                color = glow.success,
                modifier = Modifier.padding(top = 8.dp),
            )
            else -> {
                GlowTextField(
                    modifier = Modifier.padding(top = 12.dp),
                    value = note,
                    onValueChange = { note = it },
                    label = "Add a note (optional)",
                    enabled = feedbackState !is MeasurementFeedbackUiState.Submitting,
                )
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    GlowButton(
                        modifier = Modifier.weight(1f),
                        text = "Fair",
                        variant = GlowButtonVariant.Secondary,
                        loading = feedbackState is MeasurementFeedbackUiState.Submitting,
                        onClick = { onSubmitFeedback(MeasurementAgreement.FAIR, note.ifBlank { null }) },
                    )
                    GlowButton(
                        modifier = Modifier.weight(1f),
                        text = "Unsure",
                        variant = GlowButtonVariant.Secondary,
                        loading = feedbackState is MeasurementFeedbackUiState.Submitting,
                        onClick = { onSubmitFeedback(MeasurementAgreement.UNCERTAIN, note.ifBlank { null }) },
                    )
                    GlowButton(
                        modifier = Modifier.weight(1f),
                        text = "Off",
                        variant = GlowButtonVariant.Secondary,
                        loading = feedbackState is MeasurementFeedbackUiState.Submitting,
                        onClick = { onSubmitFeedback(MeasurementAgreement.OFF, note.ifBlank { null }) },
                    )
                }
                if (feedbackState is MeasurementFeedbackUiState.Failed) {
                    Text(
                        text = feedbackState.message,
                        style = MaterialTheme.typography.bodySmall,
                        color = glow.danger,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }
        }
    }
}
