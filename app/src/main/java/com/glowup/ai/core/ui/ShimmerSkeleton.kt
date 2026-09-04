package com.glowup.ai.core.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.invisibleToUser
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.glowup.ai.core.design.GlowUpTheme
import com.glowup.ai.core.design.LocalGlowColors

/**
 * A loading placeholder block. Animates a soft shimmer sweep unless the system's reduced-motion
 * setting is on, in which case it renders as a static tinted block. Marked invisible to
 * accessibility services — the real content's own loading announcement should carry the
 * semantics, not this decorative placeholder.
 */
@Composable
fun ShimmerSkeleton(
    modifier: Modifier = Modifier,
    height: Dp = 16.dp,
    cornerRadius: Dp = 8.dp,
) {
    val glow = LocalGlowColors.current
    val reducedMotion = isReducedMotionEnabled()

    if (reducedMotion) {
        androidx.compose.foundation.layout.Box(
            modifier =
                modifier
                    .fillMaxWidth()
                    .height(height)
                    .clip(RoundedCornerShape(cornerRadius))
                    .background(glow.shimmer.copy(alpha = 0.4f))
                    .border(
                        width = 1.dp,
                        color = glow.ink600.copy(alpha = 0.05f),
                        shape = RoundedCornerShape(cornerRadius)
                    )
                    .semantics { invisibleToUser() },
        )
        return
    }

    val transition = rememberInfiniteTransition(label = "shimmer")
    val translate by transition.animateFloat(
        initialValue = -1f,
        targetValue = 2f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(durationMillis = 1100, easing = GlowEasing),
                repeatMode = RepeatMode.Restart,
            ),
        label = "shimmerTranslate",
    )

    val brush =
        Brush.linearGradient(
            colors =
                listOf(
                    glow.shimmer.copy(alpha = 0.25f),
                    glow.shimmer.copy(alpha = 0.6f),
                    glow.shimmer.copy(alpha = 0.25f),
                ),
            start =
                androidx.compose.ui.geometry
                    .Offset(translate * 300f, 0f),
            end =
                androidx.compose.ui.geometry
                    .Offset(translate * 300f + 300f, 0f),
        )

    androidx.compose.foundation.layout.Box(
        modifier =
            modifier
                .fillMaxWidth()
                .height(height)
                .clip(RoundedCornerShape(cornerRadius))
                .background(brush)
                .border(
                    width = 1.dp,
                    color = glow.ink600.copy(alpha = 0.05f),
                    shape = RoundedCornerShape(cornerRadius)
                )
                .semantics { invisibleToUser() },
    )
}

@Preview(name = "Light", showBackground = true)
@Composable
private fun ShimmerSkeletonPreviewLight() {
    GlowUpTheme(darkTheme = false) { PreviewContent() }
}

@Preview(name = "Dark", showBackground = true)
@Composable
private fun ShimmerSkeletonPreviewDark() {
    GlowUpTheme(darkTheme = true) { PreviewContent() }
}

@Composable
private fun PreviewContent() {
    Column(modifier = Modifier.padding(16.dp)) {
        ShimmerSkeleton(modifier = Modifier.padding(bottom = 8.dp))
        ShimmerSkeleton(height = 120.dp, cornerRadius = 18.dp)
    }
}
