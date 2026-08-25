package com.glowup.ai.feature.capture.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import com.glowup.ai.domain.model.CoachingTip

/**
 * Renders a server `400` quality rejection. Per `frontend-api-map.md` "Capture coaching (extends
 * `POST /api/captures`)" and trap #6: every `detail.quality.coaching[].message` is rendered as its
 * own actionable tip — the raw `failed_checks` codes are never shown to the user directly. If the
 * server ever omits `coaching` (older payload shape), [failedChecks] is used only as a readable
 * fallback label, not the primary UI.
 */
@Composable
fun CoachingTipsCard(
    modifier: Modifier = Modifier,
    coaching: List<CoachingTip>,
    failedChecks: List<String>,
    onRetake: () -> Unit,
) {
    val glow = LocalGlowColors.current
    GlowCard(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "Let's fix a few things",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = glow.ink900,
        )
        Text(
            text = "This photo didn't pass the quality check. Nothing was saved.",
            style = MaterialTheme.typography.bodySmall,
            color = glow.ink600,
            modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
        )
        if (coaching.isNotEmpty()) {
            coaching.forEach { tip ->
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Text("• ", color = glow.honey600, fontWeight = FontWeight.Bold)
                    Text(tip.message, style = MaterialTheme.typography.bodyMedium, color = glow.ink900)
                }
            }
        } else {
            failedChecks.forEach { check ->
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Text("• ", color = glow.honey600, fontWeight = FontWeight.Bold)
                    Text(
                        text = check.replace('_', ' ').replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.bodyMedium,
                        color = glow.ink900,
                    )
                }
            }
        }
        GlowButton(
            modifier = Modifier.padding(top = 16.dp),
            text = "Retake photo",
            onClick = onRetake,
            variant = GlowButtonVariant.Primary,
        )
    }
}
