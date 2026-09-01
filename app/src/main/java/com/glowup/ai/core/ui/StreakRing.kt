package com.glowup.ai.core.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.glowup.ai.core.design.GlowUpTheme
import com.glowup.ai.core.design.LocalGlowColors

/**
 * A Canvas-drawn progress ring showing capture streak against its target-day cadence
 * (e.g. a 7-day window). Fill animates from its previous value unless reduced-motion is on.
 */
@Composable
fun StreakRing(
    modifier: Modifier = Modifier,
    streak: Int,
    target: Int = 7,
    size: Dp = 104.dp,
) {
    val glow = LocalGlowColors.current
    val reducedMotion = isReducedMotionEnabled()
    val rawFraction = if (target > 0) (streak.toFloat() / target).coerceIn(0f, 1f) else 0f

    val fraction by animateFloatAsState(
        targetValue = rawFraction,
        animationSpec = if (reducedMotion) tween(0) else tween(durationMillis = 500, easing = GlowEasing),
        label = "streakFraction",
    )

    val strokeWidthDp = 9.dp
    val trackColor = glow.ink600.copy(alpha = 0.18f)

    Box(
        modifier =
            modifier
                .size(size)
                .semantics {
                    contentDescription = "$streak day streak, out of a $target day goal"
                },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val strokeWidthPx = strokeWidthDp.toPx()
            val diameter = kotlin.math.min(this.size.width, this.size.height) - strokeWidthPx
            val topLeft =
                androidx.compose.ui.geometry.Offset(
                    (this.size.width - diameter) / 2f,
                    (this.size.height - diameter) / 2f,
                )
            val arcSize = Size(diameter, diameter)

            drawArc(
                color = trackColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round),
            )
            drawArc(
                color = glow.honey500,
                startAngle = -90f,
                sweepAngle = 360f * fraction,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round),
            )
        }
        Box(contentAlignment = Alignment.Center) {
            androidx.compose.foundation.layout.Column(
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "$streak",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = glow.ink900,
                )
                Text(
                    text = "DAY STREAK",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = glow.ink600,
                )
            }
        }
    }
}

@Preview(name = "Light", showBackground = true)
@Composable
private fun StreakRingPreviewLight() {
    GlowUpTheme(darkTheme = false) {
        StreakRing(modifier = Modifier.padding(16.dp), streak = 4, target = 7)
    }
}

@Preview(name = "Dark", showBackground = true)
@Composable
private fun StreakRingPreviewDark() {
    GlowUpTheme(darkTheme = true) {
        StreakRing(modifier = Modifier.padding(16.dp), streak = 4, target = 7)
    }
}
