package com.glowup.ai.core.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.glowup.ai.core.design.GlowUpTheme
import com.glowup.ai.core.design.LocalGlowColors

/**
 * A labelled horizontal bar for a bounded metric, e.g. a 0..1 confidence score or a 0..N count
 * against a known ceiling.
 */
@Composable
fun MetricBar(
    modifier: Modifier = Modifier,
    label: String,
    value: Float,
    max: Float = 1f,
    valueText: String? = null,
) {
    val glow = LocalGlowColors.current
    val reducedMotion = isReducedMotionEnabled()
    val clamped = (value / max).coerceIn(0f, 1f)

    val animated by animateFloatAsState(
        targetValue = clamped,
        animationSpec = if (reducedMotion) tween(0) else tween(durationMillis = 400, easing = GlowEasing),
        label = "metricBar",
    )

    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .semantics {
                    contentDescription = "$label: ${valueText ?: value}"
                    progressBarRangeInfo = ProgressBarRangeInfo(current = value, range = 0f..max)
                },
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = glow.ink600,
            )
            Text(
                text = valueText ?: value.toString(),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = glow.ink900,
            )
        }
        Box(
            modifier =
                Modifier
                    .padding(top = 6.dp)
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(50))
                    .background(glow.ink600.copy(alpha = 0.15f)),
        ) {
            Box(
                modifier =
                    Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(animated.coerceIn(0f, 1f))
                        .clip(RoundedCornerShape(50))
                        .background(glow.honey500),
            )
        }
    }
}

@Preview(name = "Light", showBackground = true)
@Composable
private fun MetricBarPreviewLight() {
    GlowUpTheme(darkTheme = false) { PreviewContent() }
}

@Preview(name = "Dark", showBackground = true)
@Composable
private fun MetricBarPreviewDark() {
    GlowUpTheme(darkTheme = true) { PreviewContent() }
}

@Composable
private fun PreviewContent() {
    Column(modifier = Modifier.padding(16.dp)) {
        MetricBar(label = "Texture score", value = 0.62f, valueText = "0.62")
        MetricBar(
            modifier = Modifier.padding(top = 12.dp),
            label = "Blemish count",
            value = 3f,
            max = 10f,
            valueText = "3",
        )
    }
}
