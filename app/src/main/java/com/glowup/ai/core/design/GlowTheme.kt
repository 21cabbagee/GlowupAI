package com.glowup.ai.core.design

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * GlowUp AI Theme
 *
 * Complete theme implementation using design tokens from DesignTokens.kt and
 * typography from GlowTypography.kt. Provides light/dark mode support with
 * automatic system theme detection.
 *
 * Features:
 * - Light and dark theme support
 * - Health/Wellness + AI/Tech aesthetic
 * - Cal.ai-level polish
 * - Material 3 integration
 * - System bars styling
 *
 * Usage:
 * ```kotlin
 * GlowTheme {
 *     // Your content
 * }
 * ```
 *
 * To force a theme:
 * ```kotlin
 * GlowTheme(darkTheme = false) {
 *     // Light theme content
 * }
 * ```
 *
 * To access custom tokens:
 * ```kotlin
 * val colors = LocalGlowColorScheme.current
 * Box(backgroundColor = colors.background)
 * ```
 *
 * Reference: UI_REDESIGN_MASTER_PLAN.md
 */

// ================================================================================================
// MATERIAL 3 COLOR SCHEMES
// ================================================================================================

/**
 * Light theme Material 3 color scheme.
 * Maps our design tokens to Material 3's semantic color roles.
 */
private val LightMaterialColorScheme = lightColorScheme(
    // Primary brand colors
    primary = GlowAccentColors.PrimaryEnd, // Orange
    onPrimary = GlowSecondaryColors.TextPrimary,
    primaryContainer = GlowAccentColors.PrimaryLight,
    onPrimaryContainer = GlowSecondaryColors.TextPrimary,

    // Secondary colors
    secondary = GlowAccentColors.PrimaryStart, // Amber
    onSecondary = GlowSecondaryColors.TextPrimary,
    secondaryContainer = GlowAccentColors.PrimaryLight,
    onSecondaryContainer = GlowSecondaryColors.TextPrimary,

    // Tertiary (health/wellness accent)
    tertiary = GlowHealthColors.SoftGreen,
    onTertiary = GlowSecondaryColors.TextPrimary,
    tertiaryContainer = GlowHealthColors.SoftGreen.copy(alpha = 0.2f),
    onTertiaryContainer = GlowSecondaryColors.TextPrimary,

    // Background & Surface
    background = GlowPrimaryColors.Background,
    onBackground = GlowSecondaryColors.TextPrimary,
    surface = GlowPrimaryColors.Surface,
    onSurface = GlowSecondaryColors.TextPrimary,
    surfaceVariant = GlowPrimaryColors.Elevated,
    onSurfaceVariant = GlowSecondaryColors.TextSecondary,

    // Error
    error = GlowAccentColors.Error,
    onError = GlowPrimaryColors.Surface,
    errorContainer = GlowAccentColors.Error.copy(alpha = 0.12f),
    onErrorContainer = GlowAccentColors.Error,

    // Outline
    outline = GlowSecondaryColors.TextTertiary,
    outlineVariant = GlowPrimaryColors.Elevated,
)

/**
 * Dark theme Material 3 color scheme.
 * Maintains the warm, health-focused aesthetic in dark mode.
 */
private val DarkMaterialColorScheme = darkColorScheme(
    // Primary brand colors (keep vibrant in dark mode)
    primary = GlowAccentColors.PrimaryEnd,
    onPrimary = GlowSecondaryColors.TextPrimary,
    primaryContainer = GlowAccentColors.PrimaryHover,
    onPrimaryContainer = GlowDarkColors.TextPrimary,

    // Secondary colors
    secondary = GlowAccentColors.PrimaryStart,
    onSecondary = GlowSecondaryColors.TextPrimary,
    secondaryContainer = GlowAccentColors.PrimaryStart.copy(alpha = 0.3f),
    onSecondaryContainer = GlowDarkColors.TextPrimary,

    // Tertiary (health/wellness accent)
    tertiary = GlowHealthColors.SoftGreen,
    onTertiary = GlowSecondaryColors.TextPrimary,
    tertiaryContainer = GlowHealthColors.SoftGreen.copy(alpha = 0.2f),
    onTertiaryContainer = GlowDarkColors.TextPrimary,

    // Background & Surface
    background = GlowDarkColors.Background,
    onBackground = GlowDarkColors.TextPrimary,
    surface = GlowDarkColors.Surface,
    onSurface = GlowDarkColors.TextPrimary,
    surfaceVariant = GlowDarkColors.Elevated,
    onSurfaceVariant = GlowDarkColors.TextSecondary,

    // Error
    error = GlowAccentColors.Error,
    onError = GlowPrimaryColors.Surface,
    errorContainer = GlowAccentColors.Error.copy(alpha = 0.12f),
    onErrorContainer = GlowAccentColors.Error,

    // Outline
    outline = GlowDarkColors.TextTertiary,
    outlineVariant = GlowDarkColors.Elevated,
)

// ================================================================================================
// THEME COMPOSABLE
// ================================================================================================

/**
 * Main theme composable for GlowUp AI.
 *
 * Provides:
 * - Material 3 theme with custom color scheme
 * - Custom design tokens via CompositionLocal
 * - Typography system
 * - System bars styling (status bar, navigation bar)
 * - Automatic light/dark theme detection
 *
 * @param darkTheme Whether to use dark theme. Defaults to system theme.
 * @param dynamicColor Whether to use Material You dynamic colors. Always false for brand consistency.
 * @param content The content to be themed.
 */
@Composable
fun GlowTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Disabled for brand consistency
    content: @Composable () -> Unit,
) {
    // Select color scheme based on theme
    val materialColorScheme = if (darkTheme) {
        DarkMaterialColorScheme
    } else {
        LightMaterialColorScheme
    }

    // Select custom color tokens
    val glowColorScheme = if (darkTheme) {
        DarkColorScheme
    } else {
        LightColorScheme
    }

    // Update system bars
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect

            // Set status bar color
            window.statusBarColor = if (darkTheme) {
                GlowDarkColors.Background.toArgb()
            } else {
                GlowPrimaryColors.Background.toArgb()
            }

            // Set navigation bar color
            window.navigationBarColor = if (darkTheme) {
                GlowDarkColors.Background.toArgb()
            } else {
                GlowPrimaryColors.Background.toArgb()
            }

            // Set system bar icon colors (light icons on dark background, dark icons on light background)
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    // Provide theme
    CompositionLocalProvider(LocalGlowColorScheme provides glowColorScheme) {
        MaterialTheme(
            colorScheme = materialColorScheme,
            typography = GlowTypography,
            content = content,
        )
    }
}

// ================================================================================================
// THEME PREVIEW COMPOSABLES
// ================================================================================================

/**
 * Preview helper for light theme.
 * Use in @Preview annotations for consistent light theme previews.
 *
 * Example:
 * ```kotlin
 * @Preview
 * @Composable
 * fun MyComponentPreview() {
 *     GlowThemePreview {
 *         MyComponent()
 *     }
 * }
 * ```
 */
@Composable
fun GlowThemePreview(content: @Composable () -> Unit) {
    GlowTheme(darkTheme = false, content = content)
}

/**
 * Preview helper for dark theme.
 * Use in @Preview annotations for consistent dark theme previews.
 *
 * Example:
 * ```kotlin
 * @Preview
 * @Composable
 * fun MyComponentDarkPreview() {
 *     GlowThemeDarkPreview {
 *         MyComponent()
 *     }
 * }
 * ```
 */
@Composable
fun GlowThemeDarkPreview(content: @Composable () -> Unit) {
    GlowTheme(darkTheme = true, content = content)
}

// ================================================================================================
// USAGE GUIDELINES
// ================================================================================================

/**
 * Theme Usage Guidelines:
 *
 * **Accessing Colors:**
 * ```kotlin
 * // Material 3 colors (preferred for standard components)
 * val primaryColor = MaterialTheme.colorScheme.primary
 * val backgroundColor = MaterialTheme.colorScheme.background
 *
 * // Custom design tokens (for brand-specific colors)
 * val colors = LocalGlowColorScheme.current
 * val gradientStart = colors.primaryStart
 * val gradientEnd = colors.primaryEnd
 * val healthGreen = colors.softGreen
 * ```
 *
 * **Accessing Typography:**
 * ```kotlin
 * // Material 3 typography (preferred)
 * Text(
 *     text = "Hello",
 *     style = MaterialTheme.typography.displayMedium // 32sp Display
 * )
 *
 * // Custom text styles (for specific use cases)
 * Text(
 *     text = "0.42",
 *     style = GlowTextStyles.MonospaceDisplay // For numbers
 * )
 * ```
 *
 * **Creating Gradients:**
 * ```kotlin
 * val colors = LocalGlowColorScheme.current
 * val gradient = Brush.horizontalGradient(
 *     colors = listOf(colors.primaryStart, colors.primaryEnd)
 * )
 * Box(
 *     modifier = Modifier.background(gradient)
 * )
 * ```
 *
 * **Theme-Aware Components:**
 * Components automatically adapt to light/dark theme through MaterialTheme.
 * For custom theme-aware logic:
 * ```kotlin
 * val isDark = isSystemInDarkTheme()
 * if (isDark) {
 *     // Dark theme specific code
 * } else {
 *     // Light theme specific code
 * }
 * ```
 *
 * **Testing Both Themes:**
 * ```kotlin
 * @Preview(name = "Light")
 * @Preview(name = "Dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
 * @Composable
 * fun MyComponentPreview() {
 *     GlowTheme {
 *         MyComponent()
 *     }
 * }
 * ```
 *
 * **Color Contrast:**
 * All color pairs have been verified for WCAG AA compliance:
 * - Body text: 4.5:1 minimum contrast ratio
 * - Large text: 3:1 minimum contrast ratio
 * - Text opacity values (90%, 70%, 60%) ensure readability
 *
 * **Brand Consistency:**
 * - Never override primary brand colors with dynamic colors
 * - Use gradient (primaryStart → primaryEnd) for CTAs
 * - Reserve health colors (softGreen, calmPurple, warmPink) for emotional moments
 * - Follow 60/30/10 rule: 60% neutral, 30% text, 10% accent
 */
