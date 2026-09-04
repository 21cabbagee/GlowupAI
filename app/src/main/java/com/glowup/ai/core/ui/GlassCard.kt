package com.glowup.ai.core.ui

import android.os.Build
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.glowup.ai.core.design.GlowShapes
import com.glowup.ai.core.design.GlowSpacing
import com.glowup.ai.core.design.GlowUpTheme
import com.glowup.ai.core.design.LocalGlowColors

/**
 * Glassmorphism card component with semi-transparent background and border.
 *
 * Creates a modern "frosted glass" effect suitable for premium features, modals,
 * and elevated content. On Android 12+, uses native blur for enhanced effect.
 *
 * Design characteristics:
 * - Semi-transparent white background (light theme) or dark background (dark theme)
 * - Subtle border with 20% opacity
 * - Elevated appearance with soft shadow
 * - Optional blur effect on Android 12+
 *
 * @param modifier Optional modifier for the card
 * @param backgroundAlpha Alpha value for the glass background (0.0 to 1.0). Default is 0.7.
 * @param borderAlpha Alpha value for the glass border (0.0 to 1.0). Default is 0.2.
 * @param enableBlur Whether to enable blur effect on supported devices (Android 12+). Default is true.
 * @param blurRadius Blur radius in dp when blur is enabled. Default is 16dp.
 * @param elevation Card elevation. Default is 4dp.
 * @param content The content inside the glass card
 *
 * @sample GlassCardPreview
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    backgroundAlpha: Float = 0.7f,
    borderAlpha: Float = 0.2f,
    enableBlur: Boolean = true,
    blurRadius: Dp = 16.dp,
    elevation: Dp = 4.dp,
    content: @Composable () -> Unit,
) {
    val glow = LocalGlowColors.current

    // Determine glass background color based on theme
    val glassBackgroundColor = glow.surfaceCard.copy(alpha = backgroundAlpha)

    // Border color with specified alpha
    val borderColor = glow.ink900.copy(alpha = borderAlpha)

    Card(
        modifier = modifier
            .then(
                // Apply blur effect only on Android 12+ if enabled
                if (enableBlur && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    Modifier.blur(blurRadius)
                } else {
                    Modifier
                }
            ),
        shape = GlowShapes.lg,
        colors = CardDefaults.cardColors(
            containerColor = glassBackgroundColor,
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = elevation,
        ),
        border = BorderStroke(
            width = 1.dp,
            color = borderColor,
        ),
    ) {
        content()
    }
}

/**
 * Glassmorphism card with gradient background.
 *
 * Creates a premium glass effect with a subtle gradient overlay,
 * perfect for highlighting premium features or special content sections.
 *
 * @param modifier Optional modifier for the card
 * @param backgroundAlpha Alpha value for the glass background (0.0 to 1.0). Default is 0.7.
 * @param borderAlpha Alpha value for the glass border (0.0 to 1.0). Default is 0.2.
 * @param gradientColors List of colors for the gradient overlay. If empty, no gradient is applied.
 * @param enableBlur Whether to enable blur effect on supported devices (Android 12+). Default is true.
 * @param blurRadius Blur radius in dp when blur is enabled. Default is 16dp.
 * @param elevation Card elevation. Default is 4dp.
 * @param content The content inside the glass card
 */
@Composable
fun GlassCardWithGradient(
    modifier: Modifier = Modifier,
    backgroundAlpha: Float = 0.7f,
    borderAlpha: Float = 0.2f,
    gradientColors: List<Color> = emptyList(),
    enableBlur: Boolean = true,
    blurRadius: Dp = 16.dp,
    elevation: Dp = 4.dp,
    content: @Composable () -> Unit,
) {
    GlassCard(
        modifier = modifier,
        backgroundAlpha = backgroundAlpha,
        borderAlpha = borderAlpha,
        enableBlur = enableBlur,
        blurRadius = blurRadius,
        elevation = elevation,
    ) {
        Box(
            modifier = Modifier
                .then(
                    if (gradientColors.isNotEmpty()) {
                        Modifier.background(
                            brush = Brush.verticalGradient(
                                colors = gradientColors.map { it.copy(alpha = 0.1f) }
                            )
                        )
                    } else {
                        Modifier
                    }
                )
        ) {
            content()
        }
    }
}

/**
 * Compact glass card variant for small premium features or badges.
 *
 * A smaller, more subtle variant of [GlassCard] with reduced padding and elevation,
 * ideal for inline premium badges, status indicators, or compact overlays.
 *
 * @param modifier Optional modifier for the card
 * @param backgroundAlpha Alpha value for the glass background (0.0 to 1.0). Default is 0.8.
 * @param content The content inside the compact glass card
 */
@Composable
fun CompactGlassCard(
    modifier: Modifier = Modifier,
    backgroundAlpha: Float = 0.8f,
    content: @Composable () -> Unit,
) {
    GlassCard(
        modifier = modifier,
        backgroundAlpha = backgroundAlpha,
        borderAlpha = 0.15f,
        enableBlur = false,
        elevation = 2.dp,
    ) {
        Box(modifier = Modifier.padding(GlowSpacing.sm)) {
            content()
        }
    }
}

// ============================================================================
// PREVIEW SECTION
// ============================================================================

@Preview(name = "GlassCard - Light", showBackground = true)
@Composable
private fun GlassCardPreviewLight() {
    GlowUpTheme(darkTheme = false) {
        Box(
            modifier = Modifier
                .size(400.dp, 300.dp)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFFFFBE2E),
                            Color(0xFFFF9A3E),
                        )
                    )
                )
                .padding(GlowSpacing.lg)
        ) {
            GlassCard(
                modifier = Modifier.size(360.dp, 200.dp),
            ) {
                Column(
                    modifier = Modifier.padding(GlowSpacing.lg)
                ) {
                    Text(
                        text = "Premium Feature",
                        style = MaterialTheme.typography.headlineSmall,
                        color = LocalGlowColors.current.ink900,
                    )
                    Text(
                        text = "This is a glassmorphism card with a semi-transparent background and subtle border.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = LocalGlowColors.current.ink600,
                        modifier = Modifier.padding(top = GlowSpacing.sm)
                    )
                }
            }
        }
    }
}

@Preview(name = "GlassCard - Dark", showBackground = true)
@Composable
private fun GlassCardPreviewDark() {
    GlowUpTheme(darkTheme = true) {
        Box(
            modifier = Modifier
                .size(400.dp, 300.dp)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF1E1911),
                            Color(0xFF2A241A),
                        )
                    )
                )
                .padding(GlowSpacing.lg)
        ) {
            GlassCard(
                modifier = Modifier.size(360.dp, 200.dp),
            ) {
                Column(
                    modifier = Modifier.padding(GlowSpacing.lg)
                ) {
                    Text(
                        text = "Premium Feature",
                        style = MaterialTheme.typography.headlineSmall,
                        color = LocalGlowColors.current.ink900,
                    )
                    Text(
                        text = "This is a glassmorphism card in dark theme.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = LocalGlowColors.current.ink600,
                        modifier = Modifier.padding(top = GlowSpacing.sm)
                    )
                }
            }
        }
    }
}

@Preview(name = "GlassCardWithGradient - Light", showBackground = true)
@Composable
private fun GlassCardWithGradientPreviewLight() {
    GlowUpTheme(darkTheme = false) {
        Box(
            modifier = Modifier
                .size(400.dp, 300.dp)
                .background(Color(0xFFFAFAF9))
                .padding(GlowSpacing.lg)
        ) {
            GlassCardWithGradient(
                modifier = Modifier.size(360.dp, 200.dp),
                gradientColors = listOf(
                    Color(0xFFFFBE2E),
                    Color(0xFFFF9A3E),
                ),
            ) {
                Column(
                    modifier = Modifier.padding(GlowSpacing.lg)
                ) {
                    Text(
                        text = "Premium Gradient",
                        style = MaterialTheme.typography.headlineSmall,
                        color = LocalGlowColors.current.ink900,
                    )
                    Text(
                        text = "Glass card with gradient overlay for premium features.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = LocalGlowColors.current.ink600,
                        modifier = Modifier.padding(top = GlowSpacing.sm)
                    )
                }
            }
        }
    }
}

@Preview(name = "CompactGlassCard - Light", showBackground = true)
@Composable
private fun CompactGlassCardPreviewLight() {
    GlowUpTheme(darkTheme = false) {
        Box(
            modifier = Modifier
                .size(300.dp, 200.dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFFFD166),
                            Color(0xFFFFBE2E),
                        )
                    )
                )
                .padding(GlowSpacing.lg)
        ) {
            CompactGlassCard {
                Text(
                    text = "PRO",
                    style = MaterialTheme.typography.labelMedium,
                    color = LocalGlowColors.current.ink900,
                )
            }
        }
    }
}

@Preview(name = "GlassCard Variants", showBackground = true)
@Composable
private fun GlassCardVariantsPreview() {
    GlowUpTheme(darkTheme = false) {
        Box(
            modifier = Modifier
                .size(400.dp, 600.dp)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFFFFBE2E),
                            Color(0xFFFF9A3E),
                            Color(0xFFFFD166),
                        )
                    )
                )
                .padding(GlowSpacing.lg)
        ) {
            Column(
                modifier = Modifier.padding(GlowSpacing.md)
            ) {
                // High transparency
                GlassCard(
                    modifier = Modifier
                        .size(360.dp, 120.dp)
                        .padding(bottom = GlowSpacing.md),
                    backgroundAlpha = 0.5f,
                ) {
                    Box(modifier = Modifier.padding(GlowSpacing.lg)) {
                        Text(
                            text = "50% Transparency",
                            style = MaterialTheme.typography.titleMedium,
                            color = LocalGlowColors.current.ink900,
                        )
                    }
                }

                // Medium transparency
                GlassCard(
                    modifier = Modifier
                        .size(360.dp, 120.dp)
                        .padding(bottom = GlowSpacing.md),
                    backgroundAlpha = 0.7f,
                ) {
                    Box(modifier = Modifier.padding(GlowSpacing.lg)) {
                        Text(
                            text = "70% Transparency",
                            style = MaterialTheme.typography.titleMedium,
                            color = LocalGlowColors.current.ink900,
                        )
                    }
                }

                // Low transparency
                GlassCard(
                    modifier = Modifier
                        .size(360.dp, 120.dp),
                    backgroundAlpha = 0.9f,
                ) {
                    Box(modifier = Modifier.padding(GlowSpacing.lg)) {
                        Text(
                            text = "90% Transparency",
                            style = MaterialTheme.typography.titleMedium,
                            color = LocalGlowColors.current.ink900,
                        )
                    }
                }
            }
        }
    }
}
