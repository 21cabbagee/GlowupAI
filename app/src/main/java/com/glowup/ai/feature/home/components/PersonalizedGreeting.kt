package com.glowup.ai.feature.home.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
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
import com.glowup.ai.core.design.LocalGlowColors
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
        label = "greetingFade"
    )

    LaunchedEffect(Unit) {
        visible = true
    }

    val timeBasedGreeting = remember {
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
        modifier = modifier
            .fillMaxWidth()
            .alpha(alpha)
    ) {
        Text(
            text = "$timeBasedGreeting, $name 👋",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.SemiBold,
            color = glowColors.ink900,
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
