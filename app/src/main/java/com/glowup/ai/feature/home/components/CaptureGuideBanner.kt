package com.glowup.ai.feature.home.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.glowup.ai.core.design.LocalGlowColors
import com.glowup.ai.core.ui.GlowButton
import com.glowup.ai.core.ui.GlowButtonVariant
import com.glowup.ai.core.ui.GlowCard
import com.glowup.ai.domain.model.CaptureGuide
import com.glowup.ai.domain.model.CaptureGuideState

/**
 * "Next capture window" banner. Uses the SERVER's guide state/window rather than a client-only
 * interval (frontend-api-map.md `GET /capture-guide`: "Use the server window rather than a
 * client-only interval"), sourced from the dashboard's embedded `engagement.guide`.
 */
@Composable
fun CaptureGuideBanner(
    modifier: Modifier = Modifier,
    guide: CaptureGuide?,
    onCaptureClick: () -> Unit,
    captureEnabled: Boolean = true,
) {
    val glow = LocalGlowColors.current
    val (title, ctaLabel, urgent) =
        if (!captureEnabled) {
            Triple("Photo tracking is off", "Capture unavailable", false)
        } else {
            when (guide?.state) {
                CaptureGuideState.BASELINE_NEEDED -> Triple("Set your baseline", "Take baseline capture", true)
                CaptureGuideState.DUE -> Triple("Capture due", "Capture now", true)
                CaptureGuideState.OVERDUE -> Triple("Capture overdue", "Capture now", true)
                CaptureGuideState.SCHEDULED -> Triple("Next capture scheduled", "Open camera", false)
                else -> Triple("Track your skin", "Capture now", false)
            }
        }

    GlowCard(modifier = modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = glow.ink900,
        )
        Text(
            text =
                if (!captureEnabled) {
                    "Re-enable facial-photo consent in Account → Data & Privacy to resume tracking."
                } else {
                    guide?.message ?: "Take a guided photo to keep your tracking accurate."
                },
            style = MaterialTheme.typography.bodyMedium,
            color = glow.ink600,
            modifier = Modifier.padding(top = 4.dp),
        )
        GlowButton(
            modifier = Modifier.padding(top = 12.dp),
            text = ctaLabel,
            onClick = onCaptureClick,
            enabled = captureEnabled,
            variant = if (urgent) GlowButtonVariant.Primary else GlowButtonVariant.Secondary,
        )
    }
}
