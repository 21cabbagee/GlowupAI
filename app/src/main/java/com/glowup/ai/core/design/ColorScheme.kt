package com.glowup.ai.core.design

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * Full Material 3 [ColorScheme] for both themes — every role assigned, not just
 * primary/secondary/tertiary. `dynamicColor` is intentionally NOT wired anywhere in this file or
 * in [GlowUpTheme]: Material You must never override the brand.
 *
 * WARNING for consumers: `MaterialTheme.colorScheme.primary` is honey-500, a bright yellow. Per
 * the non-negotiable contrast rule, never draw text/icons directly in `primary` or
 * `primaryContainer` color over an arbitrary background (e.g. `Text(color = colorScheme.primary)`
 * on `background`/`surface`) — some default M3 components (`TextButton`, `OutlinedButton`,
 * links) use `primary` as their *content* color, which would put yellow text on a light/dark
 * background. Always pair a container role with its matching `onXxx` role, or use
 * `GlowColors.honey700` for the one yellow permitted as text on a light background (3.76:1).
 *
 * Every foreground/background pair below is commented with its measured WCAG contrast ratio.
 * AA body text requires >=4.5:1; AA large/bold text and non-text UI components require >=3:1.
 */

// ---- Light ----------------------------------------------------------------------------------

private val LightPrimary = HoneyPalette.Honey500
private val LightOnPrimary = HoneyPalette.Ink900 // 11.35:1
private val LightPrimaryContainer = HoneyPalette.Honey300
private val LightOnPrimaryContainer = HoneyPalette.Ink900 // 14.89:1
private val LightInversePrimary = HoneyPalette.Honey400 // 13.06:1 vs inverseSurface (ink900)

private val LightSecondary = HoneyPalette.Sage
private val LightOnSecondary = HoneyPalette.Paper // 4.80:1
private val LightSecondaryContainer = Color(0xFFDCEEE2)
private val LightOnSecondaryContainer = HoneyPalette.Ink900 // 15.58:1

private val LightTertiary = HoneyPalette.Honey700
private val LightOnTertiary = HoneyPalette.Ink900 // 4.93:1
private val LightTertiaryContainer = Color(0xFFF5DFC0)
private val LightOnTertiaryContainer = HoneyPalette.Ink900 // 14.53:1

private val LightError = HoneyPalette.Rust
private val LightOnError = HoneyPalette.Paper // 4.90:1
private val LightErrorContainer = Color(0xFFF7D9D6)
private val LightOnErrorContainer = HoneyPalette.Ink900 // 14.22:1

private val LightBackground = HoneyPalette.Paper
private val LightOnBackground = HoneyPalette.Ink900 // 18.53:1
private val LightSurface = HoneyPalette.Surface
private val LightOnSurface = HoneyPalette.Ink900 // 18.84:1
private val LightSurfaceVariant = Color(0xFFF3ECDD)
private val LightOnSurfaceVariant = HoneyPalette.Ink600 // 6.80:1
private val LightOutline = Color(0xFF8A8065) // 3.86:1 vs paper — meets non-text 3:1
private val LightOutlineVariant = Color(0xFFE4DCC8) // decorative divider, not text
private val LightInverseSurface = HoneyPalette.Ink900
private val LightInverseOnSurface = HoneyPalette.Paper // 18.53:1
private val LightScrim = Color(0xFF000000)

val GlowLightColorScheme: ColorScheme =
    lightColorScheme(
        primary = LightPrimary,
        onPrimary = LightOnPrimary,
        primaryContainer = LightPrimaryContainer,
        onPrimaryContainer = LightOnPrimaryContainer,
        inversePrimary = LightInversePrimary,
        secondary = LightSecondary,
        onSecondary = LightOnSecondary,
        secondaryContainer = LightSecondaryContainer,
        onSecondaryContainer = LightOnSecondaryContainer,
        tertiary = LightTertiary,
        onTertiary = LightOnTertiary,
        tertiaryContainer = LightTertiaryContainer,
        onTertiaryContainer = LightOnTertiaryContainer,
        background = LightBackground,
        onBackground = LightOnBackground,
        surface = LightSurface,
        onSurface = LightOnSurface,
        surfaceVariant = LightSurfaceVariant,
        onSurfaceVariant = LightOnSurfaceVariant,
        surfaceTint = LightPrimary,
        inverseSurface = LightInverseSurface,
        inverseOnSurface = LightInverseOnSurface,
        error = LightError,
        onError = LightOnError,
        errorContainer = LightErrorContainer,
        onErrorContainer = LightOnErrorContainer,
        outline = LightOutline,
        outlineVariant = LightOutlineVariant,
        scrim = LightScrim,
        surfaceBright = Color(0xFFFFFFFF),
        surfaceDim = Color(0xFFE8E1D2),
        surfaceContainerLowest = Color(0xFFFFFFFF),
        surfaceContainerLow = Color(0xFFFBF6EC),
        surfaceContainer = Color(0xFFF6EFE0),
        surfaceContainerHigh = Color(0xFFF0E8D5),
        surfaceContainerHighest = Color(0xFFEBE2CB),
    )

// ---- Dark -------------------------------------------------------------------------------------
// Warm charcoal, never a grey inversion of the light theme.

private val DarkPrimary = HoneyPalette.Honey500
private val DarkOnPrimary = HoneyPalette.Ink900 // 11.35:1
private val DarkPrimaryContainer = HoneyPalette.Honey400
private val DarkOnPrimaryContainer = HoneyPalette.Ink900 // 13.06:1
private val DarkInversePrimary = HoneyPalette.Honey700 // 3.40:1 vs inverseSurface — large/bold text only (e.g. Snackbar action label), not body copy

private val DarkSecondary = HoneyPalette.Sage
private val DarkOnSecondary = Color(0xFFFFFFFF) // 4.88:1 (warm-white text falls to 4.34:1, too low)
private val DarkSecondaryContainer = Color(0xFF2B5A41)
private val DarkOnSecondaryContainer = HoneyPalette.WarmWhite // 7.06:1

private val DarkTertiary = Color(0xFFE3A455)
private val DarkOnTertiary = HoneyPalette.Ink900 // 8.71:1
private val DarkTertiaryContainer = HoneyPalette.Honey700
private val DarkOnTertiaryContainer = HoneyPalette.Ink900 // 4.93:1

private val DarkError = Color(0xFFE17872)
private val DarkOnError = HoneyPalette.Ink900 // 6.40:1 (warm-white text is only 2.62:1 on this lighter rust — must use dark text)
private val DarkErrorContainer = Color(0xFF7A2C26)
private val DarkOnErrorContainer = HoneyPalette.WarmWhite // 8.42:1

private val DarkBackground = HoneyPalette.Charcoal900
private val DarkOnBackground = HoneyPalette.WarmWhite // 17.24:1
private val DarkSurface = HoneyPalette.Charcoal800
private val DarkOnSurface = HoneyPalette.WarmWhite // 15.52:1
private val DarkSurfaceVariant = HoneyPalette.Charcoal700
private val DarkOnSurfaceVariant = HoneyPalette.WarmGrey // 8.43:1
private val DarkOutline = Color(0xFF9A8F73) // 6.05:1 vs background
private val DarkOutlineVariant = Color(0xFF3A362B) // decorative divider, not text
private val DarkInverseSurface = HoneyPalette.WarmWhite
private val DarkInverseOnSurface = HoneyPalette.Ink900 // 16.74:1 — dark ink on the light inverse surface
private val DarkScrim = Color(0xFF000000)

val GlowDarkColorScheme: ColorScheme =
    darkColorScheme(
        primary = DarkPrimary,
        onPrimary = DarkOnPrimary,
        primaryContainer = DarkPrimaryContainer,
        onPrimaryContainer = DarkOnPrimaryContainer,
        inversePrimary = DarkInversePrimary,
        secondary = DarkSecondary,
        onSecondary = DarkOnSecondary,
        secondaryContainer = DarkSecondaryContainer,
        onSecondaryContainer = DarkOnSecondaryContainer,
        tertiary = DarkTertiary,
        onTertiary = DarkOnTertiary,
        tertiaryContainer = DarkTertiaryContainer,
        onTertiaryContainer = DarkOnTertiaryContainer,
        background = DarkBackground,
        onBackground = DarkOnBackground,
        surface = DarkSurface,
        onSurface = DarkOnSurface,
        surfaceVariant = DarkSurfaceVariant,
        onSurfaceVariant = DarkOnSurfaceVariant,
        surfaceTint = DarkPrimary,
        inverseSurface = DarkInverseSurface,
        inverseOnSurface = DarkInverseOnSurface,
        error = DarkError,
        onError = DarkOnError,
        errorContainer = DarkErrorContainer,
        onErrorContainer = DarkOnErrorContainer,
        outline = DarkOutline,
        outlineVariant = DarkOutlineVariant,
        scrim = DarkScrim,
        surfaceBright = Color(0xFF352E22),
        surfaceDim = HoneyPalette.Charcoal900,
        surfaceContainerLowest = Color(0xFF0A0806),
        surfaceContainerLow = Color(0xFF161209),
        surfaceContainer = Color(0xFF1B160E),
        surfaceContainerHigh = Color(0xFF252014),
        surfaceContainerHighest = Color(0xFF302A1B),
    )
