package com.glowup.ai.core.design

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

/**
 * Full type scale. System font stack only ([FontFamily.Default] resolves to the platform system
 * font — Roboto on stock Android, the OEM system font elsewhere — with no webfont/downloadable
 * font dependency, matching the "no webfont" decision in ui-revamp-plan.md §1).
 *
 * Two weights only: 800 (ExtraBold) for display, 400/600 for everything else. Display tracks at
 * -0.04em with 1.02x line-height; body sits at 1.55x line-height, both expressed with Compose's
 * `.em` [androidx.compose.ui.unit.TextUnit] unit so the ratio from the spec is literal in code.
 */
private val DisplayWeight = FontWeight.ExtraBold // 800
private val EmphasisWeight = FontWeight.SemiBold // 600
private val RegularWeight = FontWeight.Normal // 400

private fun display(size: androidx.compose.ui.unit.TextUnit) =
    TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = DisplayWeight,
        fontSize = size,
        lineHeight = size * 1.02f,
        letterSpacing = (-0.04).em,
    )

private fun headline(size: androidx.compose.ui.unit.TextUnit) =
    TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = EmphasisWeight,
        fontSize = size,
        lineHeight = size * 1.2f,
        letterSpacing = (-0.02).em,
    )

private fun title(size: androidx.compose.ui.unit.TextUnit) =
    TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = EmphasisWeight,
        fontSize = size,
        lineHeight = size * 1.25f,
        letterSpacing = 0.em,
    )

private fun body(size: androidx.compose.ui.unit.TextUnit) =
    TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = RegularWeight,
        fontSize = size,
        lineHeight = size * 1.55f,
        letterSpacing = 0.02.em,
    )

private fun label(size: androidx.compose.ui.unit.TextUnit) =
    TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = EmphasisWeight,
        fontSize = size,
        lineHeight = size * 1.35f,
        letterSpacing = 0.02.em,
    )

// Legacy typography - replaced by GlowTypography.kt
@Deprecated("Use GlowTypography from GlowTypography.kt instead")
val LegacyTypography =
    Typography(
        displayLarge = display(57.sp),
        displayMedium = display(45.sp),
        displaySmall = display(36.sp),
        headlineLarge = headline(32.sp),
        headlineMedium = headline(28.sp),
        headlineSmall = headline(24.sp),
        titleLarge = title(22.sp),
        titleMedium = title(16.sp),
        titleSmall = title(14.sp),
        bodyLarge = body(16.sp),
        bodyMedium = body(14.sp),
        bodySmall = body(12.sp),
        labelLarge = label(14.sp),
        labelMedium = label(12.sp),
        labelSmall = label(11.sp),
    )
