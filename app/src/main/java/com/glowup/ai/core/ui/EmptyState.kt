package com.glowup.ai.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.glowup.ai.core.design.GlowUpTheme
import com.glowup.ai.core.design.LocalGlowColors
import com.glowup.ai.core.design.GlowShapes
import com.glowup.ai.core.design.GlowSpacing

/**
 * An empty state that ALWAYS names the next action. There is no overload without [ctaLabel] /
 * [onCtaClick] — a screen with nothing to show still owes the user a way forward, and skipping
 * that is a bug, not a style choice.
 */
@Composable
fun EmptyState(
    modifier: Modifier = Modifier,
    title: String,
    body: String? = null,
    ctaLabel: String,
    onCtaClick: () -> Unit,
    enabled: Boolean = true,
) {
    val glow = LocalGlowColors.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(GlowShapes.md)
            .background(glow.surfaceCard)
            .padding(GlowSpacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = glow.ink900,
            textAlign = TextAlign.Center,
        )
        if (body != null) {
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = glow.ink600,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = GlowSpacing.sm),
            )
        }
        GlowButton(
            modifier = Modifier.padding(top = 16.dp),
            text = ctaLabel,
            onClick = onCtaClick,
            enabled = enabled,
            variant = GlowButtonVariant.Primary,
        )
    }
}

@Preview(name = "Light", showBackground = true)
@Composable
private fun EmptyStatePreviewLight() {
    GlowUpTheme(darkTheme = false) { PreviewContent() }
}

@Preview(name = "Dark", showBackground = true)
@Composable
private fun EmptyStatePreviewDark() {
    GlowUpTheme(darkTheme = true) { PreviewContent() }
}

@Composable
private fun PreviewContent() {
    EmptyState(
        modifier = Modifier.padding(16.dp),
        title = "No captures yet",
        body = "Take your first photo to start tracking your skin.",
        ctaLabel = "Start baseline capture",
        onCtaClick = {},
    )
}
