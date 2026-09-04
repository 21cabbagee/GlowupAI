package com.glowup.ai.feature.home.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.glowup.ai.core.design.LocalGlowColors
import com.glowup.ai.core.ui.GlowEasing
import java.time.LocalTime

/**
 * Personalized greeting with time-based message.
 * Creates emotional connection through personalization.
 */
@Composable
fun PersonalizedGreeting(
    displayName: String?,
    dayCount: Int?,
    modifier: Modifier = Modifier,
) {
    val glowColors = LocalGlowColors.current

    // Fade-in animation for emotional impact
    var visible by remember { mutableStateOf(false) }
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = 600),
        label = "greetingFade",
    )

    // Slide-in animation from left
    val offsetX by animateDpAsState(
        targetValue = if (visible) 0.dp else (-20).dp,
        animationSpec = tween(durationMillis = 800, easing = GlowEasing),
        label = "greetingSlide",
    )

    LaunchedEffect(Unit) {
        visible = true
    }

    val timeBasedGreeting =
        remember {
            when (LocalTime.now().hour) {
                in 0..4 -> "Still up"
                in 5..11 -> "Good morning"
                in 12..16 -> "Good afternoon"
                in 17..20 -> "Good evening"
                else -> "Good night"
            }
        }

    val name = displayName?.split(" ")?.firstOrNull() ?: "there"

    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .alpha(alpha),
    ) {
        Text(
            text = "$timeBasedGreeting, $name 👋",
            style = MaterialTheme.typography.headlineMedium.copy(fontSize = 36.sp),
            fontWeight = FontWeight.SemiBold,
            color = glowColors.ink900,
            modifier = Modifier.offset(x = offsetX),
        )

        if (dayCount != null && dayCount > 0) {
            Text(
                text = "Day $dayCount of your journey",
                style = MaterialTheme.typography.bodyMedium,
                color = glowColors.ink600,
            )
        }
    }
}
