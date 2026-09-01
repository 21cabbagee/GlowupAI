package com.glowup.ai.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.glowup.ai.core.design.GlowColors
import com.glowup.ai.core.design.GlowUpTheme
import com.glowup.ai.core.design.LocalGlowColors

/** Human-readable copy for the backend's verdict labels. Falls back to the raw label. */
private fun verdictCopy(label: String): String =
    when (label) {
        "keep" -> "Keep using"
        "likely_useful" -> "Likely useful"
        "evidence_unclear" -> "Evidence unclear"
        "investigate" -> "Investigate"
        "locked" -> "Locked"
        else -> label.replace('_', ' ').replaceFirstChar { it.uppercase() }
    }

/**
 * Renders a backend verdict label (`keep`, `likely_useful`, `evidence_unclear`, `investigate`,
 * `locked`) as a colored chip. Color always comes from [GlowColors.verdictColor] — never hardcode
 * a verdict color at a call site.
 */
@Composable
fun VerdictChip(
    modifier: Modifier = Modifier,
    label: String,
) {
    val glow = LocalGlowColors.current
    // The verdict color is a SURFACE, and onVerdictColor is the only correct thing to draw on
    // top of it. Using verdictColor for the text puts honey-500 (yellow) on a honey tint for
    // `keep` — a direct violation of the contrast rule, at roughly 1.6:1.
    val surface: Color = glow.verdictColor(label)
    val content: Color = glow.onVerdictColor(label)
    val text = verdictCopy(label)

    Text(
        text = text,
        modifier =
            modifier
                .background(surface, RoundedCornerShape(50))
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .semantics { contentDescription = "Verdict: $text" },
        color = content,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
    )
}

@Preview(name = "Light", showBackground = true)
@Composable
private fun VerdictChipPreviewLight() {
    GlowUpTheme(darkTheme = false) { PreviewRow() }
}

@Preview(name = "Dark", showBackground = true)
@Composable
private fun VerdictChipPreviewDark() {
    GlowUpTheme(darkTheme = true) { PreviewRow() }
}

@Composable
private fun PreviewRow() {
    androidx.compose.foundation.layout.Row(
        modifier = Modifier.padding(16.dp),
        horizontalArrangement =
            androidx.compose.foundation.layout.Arrangement
                .spacedBy(8.dp),
    ) {
        VerdictChip(label = "keep")
        VerdictChip(label = "likely_useful")
        VerdictChip(label = "evidence_unclear")
        VerdictChip(label = "investigate")
        VerdictChip(label = "locked")
    }
}
