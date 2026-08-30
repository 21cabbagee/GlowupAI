package com.glowup.ai.core.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.glowup.ai.core.design.GlowShapes
import com.glowup.ai.core.design.GlowSpacing
import com.glowup.ai.core.design.GlowUpTheme
import com.glowup.ai.core.design.LocalGlowColors
import com.glowup.ai.core.design.rememberReducedMotion

/**
 * Success feedback component with animated check mark.
 * Appears with a satisfying bounce animation to celebrate successful actions.
 *
 * Use for:
 * - Form submissions
 * - Data saves
 * - Completed actions
 * - Positive confirmations
 */
@Composable
fun SuccessState(
    modifier: Modifier = Modifier,
    message: String,
    dismissLabel: String? = null,
    onDismiss: (() -> Unit)? = null,
) {
    val glow = LocalGlowColors.current
    val reducedMotion = rememberReducedMotion()

    // Bounce animation on appearance
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        visible = true
    }

    val scale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.8f,
        animationSpec = if (reducedMotion) {
            spring(stiffness = Spring.StiffnessHigh)
        } else {
            spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMediumLow
            )
        },
        label = "successScale"
    )

    Column(
        modifier = modifier
            .scale(scale)
            .fillMaxWidth()
            .clip(GlowShapes.md)
            .background(glow.honey500.copy(alpha = 0.12f))
            .padding(GlowSpacing.lg)
            .semantics {
                contentDescription = "Success: $message"
                liveRegion = LiveRegionMode.Polite
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(GlowSpacing.sm)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = null,
                tint = glow.honey600,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(GlowSpacing.sm))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = glow.ink900,
                textAlign = TextAlign.Center,
            )
        }

        if (dismissLabel != null && onDismiss != null) {
            GlowButton(
                modifier = Modifier.padding(top = GlowSpacing.xs),
                text = dismissLabel,
                onClick = onDismiss,
                variant = GlowButtonVariant.Ghost,
            )
        }
    }
}

/**
 * Compact inline success indicator with just an icon and message.
 * For situations where a full state card is too heavy.
 */
@Composable
fun SuccessIndicator(
    message: String,
    modifier: Modifier = Modifier,
) {
    val glow = LocalGlowColors.current
    val reducedMotion = rememberReducedMotion()

    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        visible = true
    }

    val scale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.9f,
        animationSpec = if (reducedMotion) {
            spring(stiffness = Spring.StiffnessHigh)
        } else {
            spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium
            )
        },
        label = "successIndicatorScale"
    )

    Row(
        modifier = modifier.scale(scale),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Filled.CheckCircle,
            contentDescription = null,
            tint = glow.honey600,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(GlowSpacing.xs))
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = glow.ink600,
        )
    }
}

@Preview(name = "Light", showBackground = true)
@Composable
private fun SuccessStatePreviewLight() {
    GlowUpTheme(darkTheme = false) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SuccessState(
                message = "Your routine has been saved successfully!",
                dismissLabel = "Got it",
                onDismiss = {}
            )
            SuccessIndicator(message = "Changes saved")
        }
    }
}

@Preview(name = "Dark", showBackground = true)
@Composable
private fun SuccessStatePreviewDark() {
    GlowUpTheme(darkTheme = true) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SuccessState(
                message = "Your routine has been saved successfully!",
                dismissLabel = "Got it",
                onDismiss = {}
            )
            SuccessIndicator(message = "Changes saved")
        }
    }
}
