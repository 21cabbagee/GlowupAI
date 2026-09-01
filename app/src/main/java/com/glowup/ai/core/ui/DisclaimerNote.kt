package com.glowup.ai.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.glowup.ai.core.design.GlowUpTheme
import com.glowup.ai.core.design.LocalGlowColors

/**
 * The mandatory "cosmetic tracking, not diagnosis" copy the backend requires on every metric,
 * verdict and Q&A surface. Visually quiet by design (small type, muted tone) but there is
 * deliberately no dismiss affordance anywhere in this composable — it must always be visible
 * wherever it is placed, never something a user can permanently close.
 */
@Composable
fun DisclaimerNote(
    modifier: Modifier = Modifier,
    text: String,
) {
    val glow = LocalGlowColors.current
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(glow.ink600.copy(alpha = 0.06f))
                .padding(horizontal = 12.dp, vertical = 10.dp)
                .semantics { contentDescription = "Disclaimer: $text" },
        verticalAlignment = Alignment.Top,
    ) {
        Icon(
            imageVector = Icons.Filled.Info,
            contentDescription = null,
            tint = glow.ink600,
            modifier = Modifier.size(16.dp),
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = glow.ink600,
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}

@Preview(name = "Light", showBackground = true)
@Composable
private fun DisclaimerNotePreviewLight() {
    GlowUpTheme(darkTheme = false) { PreviewContent() }
}

@Preview(name = "Dark", showBackground = true)
@Composable
private fun DisclaimerNotePreviewDark() {
    GlowUpTheme(darkTheme = true) { PreviewContent() }
}

@Composable
private fun PreviewContent() {
    DisclaimerNote(
        modifier = Modifier.padding(16.dp),
        text = "GlowUp AI tracks cosmetic skin appearance over time. It is not a diagnosis and does not replace a dermatologist.",
    )
}
