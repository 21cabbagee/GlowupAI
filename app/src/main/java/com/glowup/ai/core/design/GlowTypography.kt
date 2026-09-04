package com.glowup.ai.core.design

import androidx.compose.material3.Typography
import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * GlowUp AI Typography System
 *
 * Simplified 4-size typography scale following Cal.ai quality standards.
 * Uses native system fonts (SF Pro Display on iOS, Roboto on Android) for optimal performance.
 *
 * Typography Scale (4 sizes max):
 * - Display: 32sp, SemiBold - Used for main headings
 * - Title: 24sp, SemiBold - Used for screen titles
 * - Body: 16sp, Regular - Used for body text
 * - Caption: 14sp, Regular - Used for secondary/caption text
 *
 * Line heights follow optimal readability ratios:
 * - Display: 40sp (1.25x)
 * - Title: 32sp (1.33x)
 * - Body: 24sp (1.5x)
 * - Caption: 20sp (1.43x)
 *
 * Reference: UI_REDESIGN_MASTER_PLAN.md § Typography System
 */

// ================================================================================================
// FONT FAMILIES
// ================================================================================================

/**
 * Primary font family - System default
 * Resolves to SF Pro Display on iOS and Roboto on Android.
 * No webfont dependencies for optimal performance.
 */
private val PrimaryFontFamily = FontFamily.Default

/**
 * Monospace font family - Used for metrics and numbers
 * Resolves to SF Mono on iOS and Roboto Mono on Android.
 */
val MonospaceFontFamily = FontFamily.Monospace

// ================================================================================================
// FONT WEIGHTS
// ================================================================================================

/**
 * Font weights used throughout the app.
 * Only two weights to maintain consistency:
 * - SemiBold (600) for emphasis and headings
 * - Regular (400) for body text
 */
object GlowFontWeight {
    /** SemiBold - 600 weight for headings and emphasis */
    val SemiBold = FontWeight.SemiBold

    /** Medium - 500 weight for secondary buttons and medium emphasis */
    val Medium = FontWeight.Medium

    /** Regular - 400 weight for body text */
    val Regular = FontWeight.Normal
}

// ================================================================================================
// TEXT STYLES
// ================================================================================================

/**
 * Display text style - 32sp, SemiBold, 40sp line height
 * Used for main headings and hero text.
 *
 * Example: "Track your skin, with evidence."
 */
val DisplayTextStyle =
    TextStyle(
        fontFamily = PrimaryFontFamily,
        fontWeight = GlowFontWeight.SemiBold,
        fontSize = 32.sp,
        lineHeight = 40.sp,
        letterSpacing = 0.sp,
    )

/**
 * Title text style - 24sp, SemiBold, 32sp line height
 * Used for screen titles and section headers.
 *
 * Example: "Good morning, User 👋"
 */
val TitleTextStyle =
    TextStyle(
        fontFamily = PrimaryFontFamily,
        fontWeight = GlowFontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 32.sp,
        letterSpacing = 0.sp,
    )

/**
 * Body text style - 16sp, Regular, 24sp line height
 * Used for body text and descriptions.
 *
 * Example: "Guided photo tracking, routine testing, and honest verdicts"
 */
val BodyTextStyle =
    TextStyle(
        fontFamily = PrimaryFontFamily,
        fontWeight = GlowFontWeight.Regular,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.sp,
    )

/**
 * Caption text style - 14sp, Regular, 20sp line height
 * Used for secondary text, captions, and metadata.
 *
 * Example: "Day 8 of your journey"
 */
val CaptionTextStyle =
    TextStyle(
        fontFamily = PrimaryFontFamily,
        fontWeight = GlowFontWeight.Regular,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.sp,
    )

// ================================================================================================
// TYPOGRAPHY VARIANTS
// ================================================================================================

/**
 * Additional text style variants for specific use cases.
 */
object GlowTextStyles {
    /** Display variant for even larger hero text */
    val DisplayLarge = DisplayTextStyle.copy(fontSize = 36.sp, lineHeight = 44.sp)

    /** Display hero variant for splash and onboarding screens - 48sp, SemiBold */
    val DisplayHero =
        TextStyle(
            fontFamily = PrimaryFontFamily,
            fontWeight = GlowFontWeight.SemiBold,
            fontSize = 48.sp,
            lineHeight = 60.sp,
            letterSpacing = (-0.5).sp,
        )

    /** Title variant with medium weight for less emphasis */
    val TitleMedium = TitleTextStyle.copy(fontWeight = GlowFontWeight.Medium)

    /** Body variant with semibold weight for emphasis */
    val BodySemiBold = BodyTextStyle.copy(fontWeight = GlowFontWeight.SemiBold)

    /** Body variant with medium weight */
    val BodyMedium = BodyTextStyle.copy(fontWeight = GlowFontWeight.Medium)

    /** Caption variant with semibold weight for labels */
    val CaptionSemiBold = CaptionTextStyle.copy(fontWeight = GlowFontWeight.SemiBold)

    /** Caption variant for small text (12sp) */
    val CaptionSmall = CaptionTextStyle.copy(fontSize = 12.sp, lineHeight = 16.sp)

    /** Monospace variant for numbers and metrics */
    val MonospaceDisplay = DisplayTextStyle.copy(fontFamily = MonospaceFontFamily)

    /** Monospace variant for body-sized numbers */
    val MonospaceBody = BodyTextStyle.copy(fontFamily = MonospaceFontFamily)

    /** Large metric display - 40sp with tabular numbers for data visualization */
    val MetricLarge =
        TextStyle(
            fontFamily = MonospaceFontFamily,
            fontWeight = GlowFontWeight.SemiBold,
            fontSize = 40.sp,
            lineHeight = 50.sp,
            letterSpacing = 0.sp,
        )

    /** Button text style - 16sp, SemiBold */
    val Button =
        TextStyle(
            fontFamily = PrimaryFontFamily,
            fontWeight = GlowFontWeight.SemiBold,
            fontSize = 16.sp,
            lineHeight = 24.sp,
            letterSpacing = 0.sp,
        )

    /** Secondary button text style - 16sp, Medium */
    val ButtonSecondary =
        TextStyle(
            fontFamily = PrimaryFontFamily,
            fontWeight = GlowFontWeight.Medium,
            fontSize = 16.sp,
            lineHeight = 24.sp,
            letterSpacing = 0.sp,
        )
}

// ================================================================================================
// MATERIAL 3 TYPOGRAPHY
// ================================================================================================

/**
 * Complete Material 3 Typography object mapping our simplified scale.
 * Maps our 4-size system to Material 3's typography roles.
 *
 * This allows seamless integration with Material 3 components while
 * maintaining our simplified typography scale.
 */
val GlowTypography =
    Typography(
        // Display styles (largest text)
        displayLarge = DisplayTextStyle.copy(fontSize = 36.sp, lineHeight = 44.sp),
        displayMedium = DisplayTextStyle,
        displaySmall = TitleTextStyle.copy(fontSize = 28.sp, lineHeight = 36.sp),
        // Headline styles (section headers)
        headlineLarge = TitleTextStyle.copy(fontSize = 28.sp, lineHeight = 36.sp),
        headlineMedium = TitleTextStyle,
        headlineSmall = TitleTextStyle.copy(fontSize = 20.sp, lineHeight = 28.sp),
        // Title styles (screen titles)
        titleLarge = TitleTextStyle.copy(fontSize = 22.sp, lineHeight = 28.sp),
        titleMedium = BodyTextStyle.copy(fontWeight = GlowFontWeight.SemiBold),
        titleSmall = CaptionTextStyle.copy(fontWeight = GlowFontWeight.SemiBold),
        // Body styles (main content)
        bodyLarge = BodyTextStyle,
        bodyMedium = CaptionTextStyle.copy(fontSize = 14.sp, lineHeight = 20.sp),
        bodySmall = CaptionTextStyle.copy(fontSize = 12.sp, lineHeight = 16.sp),
        // Label styles (buttons, tabs, chips)
        labelLarge = GlowTextStyles.Button,
        labelMedium = CaptionTextStyle.copy(fontWeight = GlowFontWeight.Medium),
        labelSmall =
            CaptionTextStyle.copy(
                fontSize = 11.sp,
                lineHeight = 16.sp,
                fontWeight = GlowFontWeight.Medium,
            ),
    )

// ================================================================================================
// USAGE GUIDELINES
// ================================================================================================

/**
 * Typography Usage Guidelines:
 *
 * 1. **Display (32sp)** - Main headings, hero text
 *    Example: Welcome screen main title
 *    Usage: Text(text = "Track your skin", style = MaterialTheme.typography.displayMedium)
 *
 * 2. **Title (24sp)** - Screen titles, section headers
 *    Example: "Analytics", "Today's Metrics"
 *    Usage: Text(text = "Analytics", style = MaterialTheme.typography.headlineMedium)
 *
 * 3. **Body (16sp)** - Body text, descriptions
 *    Example: Feature descriptions, instructions
 *    Usage: Text(text = "Description", style = MaterialTheme.typography.bodyLarge)
 *
 * 4. **Caption (14sp)** - Secondary text, metadata
 *    Example: "Day 8 of your journey", timestamps
 *    Usage: Text(text = "Day 8", style = MaterialTheme.typography.bodyMedium)
 *
 * **For Numbers/Metrics:**
 * Use monospace variants for better alignment and readability:
 *    Text(text = "0.42", style = GlowTextStyles.MonospaceDisplay)
 *
 * **For Buttons:**
 * Primary: GlowTextStyles.Button
 * Secondary: GlowTextStyles.ButtonSecondary
 *
 * **Accessibility:**
 * - Minimum touch target: 48dp
 * - Minimum contrast ratio: 4.5:1 for body text, 3:1 for large text
 * - Line heights are optimized for readability (1.25x - 1.5x)
 */
