package com.glowup.ai.core.design

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * GlowUp AI Design Tokens
 *
 * Complete design token system following Cal.ai quality standards.
 * Implements 60/30/10 color rule, 8-point grid spacing, and health/wellness + AI/tech aesthetic.
 *
 * Reference: UI_REDESIGN_MASTER_PLAN.md
 */

// ================================================================================================
// COLOR SYSTEM (60/30/10 Rule)
// ================================================================================================

/**
 * Primary colors (60% - Neutral Base)
 * Used for backgrounds and surfaces to create the foundation of the interface.
 */
object GlowPrimaryColors {
    /** Warm white background - primary app background (not pure white for warmth) */
    val Background = Color(0xFFFAFAF9)

    /** Pure white surface - used for elevated cards and surfaces */
    val Surface = Color(0xFFFFFFFF)

    /** Slightly elevated surface - subtle distinction from pure white */
    val Elevated = Color(0xFFF5F5F4)
}

/**
 * Secondary colors (30% - Complementary)
 * Text colors with varying opacity levels for hierarchy.
 */
object GlowSecondaryColors {
    /** Primary text - 90% opacity on warm black */
    val TextPrimary = Color(0xE618181B) // #18181B at 90% opacity

    /** Secondary text - 70% opacity for supporting text */
    val TextSecondary = Color(0xB352525B) // #52525B at 70% opacity

    /** Tertiary text - 60% opacity for captions and hints */
    val TextTertiary = Color(0x99A1A1AA) // #A1A1AA at 60% opacity
}

/**
 * Accent colors (10% - Brand)
 * Brand colors used sparingly for CTAs, active states, and important UI elements.
 */
object GlowAccentColors {
    /** Primary gradient start - amber */
    val PrimaryStart = Color(0xFFF59E0B)

    /** Primary gradient end - orange */
    val PrimaryEnd = Color(0xFFF97316)

    /** Primary hover state - darker orange */
    val PrimaryHover = Color(0xFFEA580C)

    /** Primary light - 5% opacity for subtle backgrounds */
    val PrimaryLight = Color(0xFFFEF3C7)

    /** Success state - green */
    val Success = Color(0xFF10B981)

    /** Error state - red */
    val Error = Color(0xFFEF4444)

    /** Info state - blue */
    val Info = Color(0xFF3B82F6)
}

/**
 * Health/Wellness accent colors
 * Special colors for emotional moments and premium features.
 */
object GlowHealthColors {
    /** Soft green - for success states and positive metrics */
    val SoftGreen = Color(0xFF86EFAC)

    /** Calm purple - for premium features */
    val CalmPurple = Color(0xFFC084FC)

    /** Warm pink - for celebratory moments */
    val WarmPink = Color(0xFFFDA4AF)
}

/**
 * Dark mode colors
 * Complete dark theme palette maintaining the health/wellness aesthetic.
 */
object GlowDarkColors {
    /** Dark background - almost black with warm tone */
    val Background = Color(0xFF121212)

    /** Dark surface - slightly elevated from background */
    val Surface = Color(0xFF1E1E1E)

    /** Dark elevated - even more elevated */
    val Elevated = Color(0xFF2A2A2A)

    /** Dark text primary - warm white */
    val TextPrimary = Color(0xFFFAFAF9)

    /** Dark text secondary - muted warm white */
    val TextSecondary = Color(0xB3E5E5E5)

    /** Dark text tertiary - subtle warm grey */
    val TextTertiary = Color(0x99CCCCCC)
}

// ================================================================================================
// SPACING SYSTEM (8-Point Grid)
// ================================================================================================

/**
 * Spacing scale based on 8dp grid system.
 * All spacing should use these values for consistency.
 */
object GlowSpacing {
    /** Extra small - 4dp */
    val XS: Dp = 4.dp

    /** Small - 8dp (base unit) */
    val S: Dp = 8.dp

    /** Medium - 16dp */
    val M: Dp = 16.dp

    /** Large - 24dp */
    val L: Dp = 24.dp

    /** Extra large - 32dp */
    val XL: Dp = 32.dp

    /** 2X large - 48dp */
    val XXL: Dp = 48.dp

    /** 3X large - 64dp */
    val XXXL: Dp = 64.dp

    /** 4X large - 80dp */
    val XXXXL: Dp = 80.dp

    /** 5X large - 96dp */
    val XXXXXL: Dp = 96.dp

    /** Card internal padding - 24dp */
    val CardPadding: Dp = 24.dp

    /** Section vertical padding - 80dp */
    val SectionPadding: Dp = 80.dp

    /** Screen horizontal padding - 24dp */
    val ScreenPadding: Dp = 24.dp
}

// ================================================================================================
// ELEVATION & SHADOWS
// ================================================================================================

/**
 * Elevation levels for soft shadow system.
 * Creates depth without harsh shadows.
 */
object GlowElevation {
    /** Small elevation - subtle lift (2dp blur, 4% opacity) */
    val Small: Dp = 2.dp

    /** Medium elevation - moderate lift (4dp blur, 6% opacity) */
    val Medium: Dp = 4.dp

    /** Large elevation - prominent lift (8dp blur, 8% opacity) */
    val Large: Dp = 8.dp

    /** Button elevation - 2dp */
    val Button: Dp = 2.dp

    /** Card elevation - 2dp */
    val Card: Dp = 2.dp

    /** FAB elevation - 6dp */
    val FAB: Dp = 6.dp
}

/**
 * Shadow configuration for different elevation levels.
 */
@Immutable
data class ShadowConfig(
    val offsetY: Dp,
    val blurRadius: Dp,
    val color: Color
)

object GlowShadows {
    /** Small shadow - (0dp, 2dp, 8dp, rgba(0,0,0,0.04)) */
    val Small = ShadowConfig(
        offsetY = 2.dp,
        blurRadius = 8.dp,
        color = Color(0x0A000000) // 4% black
    )

    /** Medium shadow - (0dp, 4dp, 16dp, rgba(0,0,0,0.06)) */
    val Medium = ShadowConfig(
        offsetY = 4.dp,
        blurRadius = 16.dp,
        color = Color(0x0F000000) // 6% black
    )

    /** Large shadow - (0dp, 8dp, 24dp, rgba(0,0,0,0.08)) */
    val Large = ShadowConfig(
        offsetY = 8.dp,
        blurRadius = 24.dp,
        color = Color(0x14000000) // 8% black
    )
}

// ================================================================================================
// BORDER RADIUS
// ================================================================================================

/**
 * Border radius scale for consistent rounded corners.
 */
object GlowRadius {
    /** Small radius - 12dp */
    val Small: Dp = 12.dp

    /** Medium radius - 16dp */
    val Medium: Dp = 16.dp

    /** Large radius - 24dp */
    val Large: Dp = 24.dp

    /** Extra large radius - 32dp */
    val XLarge: Dp = 32.dp

    /** Button radius - 16dp */
    val Button: Dp = 16.dp

    /** Card radius - 24dp */
    val Card: Dp = 24.dp
}

// ================================================================================================
// COMPOSITION LOCALS
// ================================================================================================

/**
 * Complete color scheme for light theme.
 * Combines all color tokens into a single, accessible object.
 */
@Immutable
data class GlowColorScheme(
    // Primary (60%)
    val background: Color,
    val surface: Color,
    val elevated: Color,

    // Secondary (30%)
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,

    // Accent (10%)
    val primaryStart: Color,
    val primaryEnd: Color,
    val primaryHover: Color,
    val primaryLight: Color,
    val success: Color,
    val error: Color,
    val info: Color,

    // Health/Wellness
    val softGreen: Color,
    val calmPurple: Color,
    val warmPink: Color,
)

val LightColorScheme = GlowColorScheme(
    background = GlowPrimaryColors.Background,
    surface = GlowPrimaryColors.Surface,
    elevated = GlowPrimaryColors.Elevated,
    textPrimary = GlowSecondaryColors.TextPrimary,
    textSecondary = GlowSecondaryColors.TextSecondary,
    textTertiary = GlowSecondaryColors.TextTertiary,
    primaryStart = GlowAccentColors.PrimaryStart,
    primaryEnd = GlowAccentColors.PrimaryEnd,
    primaryHover = GlowAccentColors.PrimaryHover,
    primaryLight = GlowAccentColors.PrimaryLight,
    success = GlowAccentColors.Success,
    error = GlowAccentColors.Error,
    info = GlowAccentColors.Info,
    softGreen = GlowHealthColors.SoftGreen,
    calmPurple = GlowHealthColors.CalmPurple,
    warmPink = GlowHealthColors.WarmPink,
)

val DarkColorScheme = GlowColorScheme(
    background = GlowDarkColors.Background,
    surface = GlowDarkColors.Surface,
    elevated = GlowDarkColors.Elevated,
    textPrimary = GlowDarkColors.TextPrimary,
    textSecondary = GlowDarkColors.TextSecondary,
    textTertiary = GlowDarkColors.TextTertiary,
    primaryStart = GlowAccentColors.PrimaryStart,
    primaryEnd = GlowAccentColors.PrimaryEnd,
    primaryHover = GlowAccentColors.PrimaryHover,
    primaryLight = GlowAccentColors.PrimaryLight,
    success = GlowAccentColors.Success,
    error = GlowAccentColors.Error,
    info = GlowAccentColors.Info,
    softGreen = GlowHealthColors.SoftGreen,
    calmPurple = GlowHealthColors.CalmPurple,
    warmPink = GlowHealthColors.WarmPink,
)

/**
 * Composition local for accessing color tokens in composables.
 *
 * Usage:
 * ```kotlin
 * val colors = LocalGlowColorScheme.current
 * Text(text = "Hello", color = colors.textPrimary)
 * ```
 */
val LocalGlowColorScheme = staticCompositionLocalOf { LightColorScheme }
