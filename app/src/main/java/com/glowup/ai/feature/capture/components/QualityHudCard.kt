package com.glowup.ai.feature.capture.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.glowup.ai.core.design.LocalGlowColors
import com.glowup.ai.feature.capture.FaceQualityAnalyzer
import com.glowup.ai.feature.capture.LiveFaceQuality
import kotlin.math.abs

/**
 * Live, on-device quality readout shown BEFORE the shutter is pressed (ANDROID_PLAN.md 3.2 item 2:
 * "Show a live quality HUD so the user can correct framing BEFORE capture"). Every line here comes
 * from [FaceQualityAnalyzer]'s real ML Kit measurement for this frame — never a hardcoded value —
 * but this HUD is coaching only; it never claims the frame is "accepted" (trap #6).
 */
@Composable
fun QualityHudCard(
    modifier: Modifier = Modifier,
    quality: LiveFaceQuality?,
) {
    val glow = LocalGlowColors.current
    val lines = buildHudLines(quality)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(glow.ink900.copy(alpha = 0.55f), RoundedCornerShape(14.dp))
            .padding(14.dp)
            .semantics {
                contentDescription = lines.joinToString(". ") { it.text }
                liveRegion = LiveRegionMode.Polite
            },
    ) {
        lines.forEach { line ->
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                Text(
                    text = (if (line.ok) "✓ " else "• ") + line.text,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = if (line.ok) FontWeight.Normal else FontWeight.SemiBold,
                    color = if (line.ok) glow.honey300 else androidx.compose.ui.graphics.Color.White,
                )
            }
        }
    }
}

private data class HudLine(val text: String, val ok: Boolean)

private fun buildHudLines(quality: LiveFaceQuality?): List<HudLine> {
    if (quality == null || !quality.pose.facePresent) {
        return listOf(HudLine("Center your face in the oval", ok = false))
    }
    val pose = quality.pose
    val lines = mutableListOf<HudLine>()

    lines += if (abs(pose.yawDegrees) <= FaceQualityAnalyzer.MAX_YAW_DEGREES) {
        HudLine("Facing forward", ok = true)
    } else {
        val direction = if (pose.yawDegrees > 0) "left" else "right"
        HudLine("Turn slightly $direction to face the camera", ok = false)
    }

    lines += if (abs(pose.pitchDegrees) <= FaceQualityAnalyzer.MAX_PITCH_DEGREES) {
        HudLine("Head level", ok = true)
    } else {
        val direction = if (pose.pitchDegrees > 0) "down" else "up"
        HudLine("Tilt your head slightly $direction", ok = false)
    }

    lines += when {
        pose.distanceCm < FaceQualityAnalyzer.MIN_DISTANCE_CM -> HudLine("Move a little further away", ok = false)
        pose.distanceCm > FaceQualityAnalyzer.MAX_DISTANCE_CM -> HudLine("Move a little closer", ok = false)
        else -> HudLine("Good distance (~${pose.distanceCm.toInt()} cm)", ok = true)
    }

    lines += if (pose.expressionNeutral) {
        HudLine("Neutral expression", ok = true)
    } else {
        HudLine("Relax your expression — no smiling", ok = false)
    }

    return lines
}
