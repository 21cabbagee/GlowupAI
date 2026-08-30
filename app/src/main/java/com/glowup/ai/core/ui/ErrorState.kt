package com.glowup.ai.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.glowup.ai.core.design.GlowUpTheme
import com.glowup.ai.core.design.LocalGlowColors
import com.glowup.ai.core.design.GlowShapes
import com.glowup.ai.core.design.GlowSpacing

/**
 * Something failed and the user can retry. There is no spinner-only failure state in this app —
 * every [ErrorState] carries an explicit retry affordance.
 *
 * Animates with a subtle shake on first appearance to draw attention.
 */
@Composable
fun ErrorState(
    modifier: Modifier = Modifier,
    message: String,
    retryLabel: String = "Retry",
    onRetry: () -> Unit,
) {
    val glow = LocalGlowColors.current

    // Trigger shake animation on first appearance
    var shakeKey by remember { mutableStateOf(0) }
    LaunchedEffect(Unit) {
        shakeKey = 1
    }

    val shakeOffset = rememberShakeAnimation(trigger = shakeKey > 0)

    Column(
        modifier = modifier
            .offset { IntOffset(shakeOffset.toInt(), 0) }
            .fillMaxWidth()
            .clip(GlowShapes.md)
            .background(glow.danger.copy(alpha = 0.08f))
            .padding(GlowSpacing.lg)
            .semantics {
                contentDescription = "Error: $message"
                liveRegion = LiveRegionMode.Assertive
            },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = glow.danger,
            textAlign = TextAlign.Center,
        )
        GlowButton(
            modifier = Modifier.padding(top = 16.dp),
            text = retryLabel,
            onClick = onRetry,
            variant = GlowButtonVariant.Secondary,
        )
    }
}

@Preview(name = "Light", showBackground = true)
@Composable
private fun ErrorStatePreviewLight() {
    GlowUpTheme(darkTheme = false) { PreviewContent() }
}

@Preview(name = "Dark", showBackground = true)
@Composable
private fun ErrorStatePreviewDark() {
    GlowUpTheme(darkTheme = true) { PreviewContent() }
}

@Composable
private fun PreviewContent() {
    ErrorState(
        modifier = Modifier.padding(16.dp),
        message = "Couldn't load your dashboard. Check your connection and try again.",
        onRetry = {},
    )
}
