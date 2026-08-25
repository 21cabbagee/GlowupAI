package com.glowup.ai.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.glowup.ai.core.design.GlowUpTheme
import com.glowup.ai.core.design.LocalGlowColors

/** Direction of change for a [StatTile] delta. Color is derived unless a caller overrides it. */
enum class StatDeltaDirection { Up, Down, Flat }

data class StatDelta(
    val text: String,
    val direction: StatDeltaDirection,
)

/**
 * A single real metric with an optional trend delta. Never render this with placeholder or
 * hardcoded numbers — the backend is the source of truth for every value shown here.
 */
@Composable
fun StatTile(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    delta: StatDelta? = null,
    accent: Boolean = false,
) {
    val glow = LocalGlowColors.current
    val background = if (accent) glow.honey500 else glow.surfaceCard
    val contentColor = glow.ink900
    val labelColor = if (accent) glow.ink900.copy(alpha = 0.7f) else glow.ink600

    val deltaColor = delta?.let {
        when (it.direction) {
            StatDeltaDirection.Up -> glow.success
            StatDeltaDirection.Down -> glow.danger
            StatDeltaDirection.Flat -> glow.ink600
        }
    }

    Column(
        modifier = modifier
            .defaultMinSize(minHeight = 48.dp)
            .background(background, RoundedCornerShape(18.dp))
            .padding(20.dp)
            .semantics {
                contentDescription = buildString {
                    append(label)
                    append(": ")
                    append(value)
                    if (delta != null) {
                        append(", ")
                        append(delta.text)
                    }
                }
            },
    ) {
        Text(
            text = value,
            color = contentColor,
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.ExtraBold,
        )
        Text(
            text = label.uppercase(),
            color = labelColor,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(top = 4.dp),
        )
        if (delta != null && deltaColor != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.padding(top = 8.dp),
            ) {
                Text(
                    text = when (delta.direction) {
                        StatDeltaDirection.Up -> "↑"
                        StatDeltaDirection.Down -> "↓"
                        StatDeltaDirection.Flat -> "→"
                    },
                    color = deltaColor,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = delta.text,
                    color = deltaColor,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Preview(name = "Light", showBackground = true)
@Composable
private fun StatTilePreviewLight() {
    GlowUpTheme(darkTheme = false) { PreviewRow() }
}

@Preview(name = "Dark", showBackground = true)
@Composable
private fun StatTilePreviewDark() {
    GlowUpTheme(darkTheme = true) { PreviewRow() }
}

@Composable
private fun PreviewRow() {
    Row(
        modifier = Modifier.padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        StatTile(
            label = "Streak",
            value = "12 days",
            delta = StatDelta("+3 vs last week", StatDeltaDirection.Up),
            accent = true,
        )
        StatTile(
            label = "Redness",
            value = "0.14",
            delta = StatDelta("-0.02", StatDeltaDirection.Down),
        )
    }
}
