package com.glowup.ai.core.design

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * "Honey" brand palette — see backend/docs/ui-revamp-plan.md §2.
 *
 * NON-NEGOTIABLE: yellow (any honey-3/4/5/6 shade) is never used as a text/icon foreground.
 * Honey is always a *surface*; ink-900 is the label on top of it. honey-700 ("clay") is the
 * only yellow-family value ever used as a foreground, and only on light backgrounds — see the
 * measured ratios in [GlowColorScheme].
 */
object HoneyPalette {
    val Honey300 = Color(0xFFFFE29A) // tints, chart fills
    val Honey400 = Color(0xFFFFD166) // hover on dark, secondary accent
    val Honey500 = Color(0xFFFFBE2E) // PRIMARY surface — CTAs, active nav, focus
    val Honey600 = Color(0xFFF0A400) // press state
    val Honey700 = Color(0xFFB87300) // "clay" — the one yellow permitted as text, light bg only

    val Ink900 = Color(0xFF14110B) // primary text; the label on every honey surface
    val Ink600 = Color(0xFF57503F) // secondary text

    val Paper = Color(0xFFFFFDF8) // warm white app background (light)
    val Surface = Color(0xFFFFFFFF) // cards (light)

    // Dark theme is a warm redefinition of the same brand, never a grey inversion.
    val Charcoal900 = Color(0xFF0F0D0A) // dark background
    val Charcoal800 = Color(0xFF1E1911) // dark surface / cards
    val Charcoal700 = Color(0xFF2A241A) // dark surfaceVariant
    val WarmWhite = Color(0xFFF7F1E4) // primary text on dark
    val WarmGrey = Color(0xFFC9BFA9) // secondary text on dark

    // Verdict semantics (backend labels: keep / likely_useful / evidence_unclear / investigate)
    val Sage = Color(0xFF3F7D5C) // likely_useful
    val Rust = Color(0xFFC2453F) // investigate
    // evidence_unclear reuses Honey700 ("clay") per the spec.
}

/**
 * Brand color extras that sit alongside [androidx.compose.material3.ColorScheme]. Every pair
 * below was measured against WCAG 2.1 contrast math (relative luminance formula); ratios are
 * from `(L_light + 0.05) / (L_dark + 0.05)`. AA body text needs >=4.5:1, AA large/bold text and
 * non-text graphical objects need >=3:1.
 */
@Immutable
data class GlowColors(
    val honey300: Color,
    val honey400: Color,
    val honey500: Color,
    val honey600: Color,
    val honey700: Color,
    val ink900: Color,
    val ink600: Color,
    val paper: Color,
    val surfaceCard: Color,

    // Verdict surfaces. Each is paired with its own dedicated `onVerdictX` foreground below —
    // these chips are self-contained (their contrast does not depend on page background).
    val verdictKeep: Color,
    val verdictLikelyUseful: Color,
    val verdictEvidenceUnclear: Color,
    val verdictInvestigate: Color,
    val verdictLocked: Color,

    val onVerdictKeep: Color,
    val onVerdictLikelyUseful: Color,
    val onVerdictEvidenceUnclear: Color,
    val onVerdictInvestigate: Color,
    val onVerdictLocked: Color,

    val success: Color,
    val warning: Color,
    val onWarning: Color,
    val danger: Color,

    val chartGrid: Color,
    val chartLine: Color,
    val chartFill: Color,
    val shimmer: Color,
) {
    /**
     * Maps a backend verdict label to its brand surface color. Tolerant of case, surrounding
     * whitespace, and unknown values (falls back to the neutral "locked" tone rather than a
     * yellow, so an unrecognised label never accidentally reads as "keep").
     */
    fun verdictColor(label: String): Color = when (label.trim().lowercase()) {
        "keep" -> verdictKeep
        "likely_useful" -> verdictLikelyUseful
        "evidence_unclear" -> verdictEvidenceUnclear
        "investigate" -> verdictInvestigate
        "locked" -> verdictLocked
        else -> verdictLocked
    }

    /** The correct foreground color for text/icons drawn on top of [verdictColor]'s result. */
    fun onVerdictColor(label: String): Color = when (label.trim().lowercase()) {
        "keep" -> onVerdictKeep
        "likely_useful" -> onVerdictLikelyUseful
        "evidence_unclear" -> onVerdictEvidenceUnclear
        "investigate" -> onVerdictInvestigate
        "locked" -> onVerdictLocked
        else -> onVerdictLocked
    }
}

/**
 * Light theme extras.
 *
 * | pair | ratio |
 * | --- | --- |
 * | ink900 on honey500 | 11.35:1 |
 * | paper on sage (verdictLikelyUseful) | 4.80:1 |
 * | ink900 on honey700 (verdictEvidenceUnclear) | 4.93:1 |
 * | paper on rust (verdictInvestigate) | 4.90:1 |
 * | paper on ink600 (verdictLocked) | 7.87:1 |
 * | ink900 on honey600 (warning) | 8.98:1 |
 */
val LightGlowColors = GlowColors(
    honey300 = HoneyPalette.Honey300,
    honey400 = HoneyPalette.Honey400,
    honey500 = HoneyPalette.Honey500,
    honey600 = HoneyPalette.Honey600,
    honey700 = HoneyPalette.Honey700,
    ink900 = HoneyPalette.Ink900,
    ink600 = HoneyPalette.Ink600,
    paper = HoneyPalette.Paper,
    surfaceCard = HoneyPalette.Surface,

    verdictKeep = HoneyPalette.Honey500,
    verdictLikelyUseful = HoneyPalette.Sage,
    verdictEvidenceUnclear = HoneyPalette.Honey700,
    verdictInvestigate = HoneyPalette.Rust,
    verdictLocked = HoneyPalette.Ink600,

    onVerdictKeep = HoneyPalette.Ink900, // 11.35:1
    onVerdictLikelyUseful = HoneyPalette.Paper, // 4.80:1
    onVerdictEvidenceUnclear = HoneyPalette.Ink900, // 4.93:1
    onVerdictInvestigate = HoneyPalette.Paper, // 4.90:1
    onVerdictLocked = HoneyPalette.Paper, // 7.87:1

    success = HoneyPalette.Sage,
    warning = HoneyPalette.Honey600,
    onWarning = HoneyPalette.Ink900, // 8.98:1
    danger = HoneyPalette.Rust,

    chartGrid = HoneyPalette.Ink600.copy(alpha = 0.15f), // decorative, non-text gridlines
    chartLine = HoneyPalette.Honey700, // 3.76:1 vs paper — honey500 alone is only 1.63:1, too low
    chartFill = HoneyPalette.Honey300.copy(alpha = 0.30f), // area fill, decorative
    shimmer = Color(0xFFECE3CF),
)

/**
 * Dark theme extras — a warm redefinition on charcoal, never a grey inversion.
 *
 * | pair | ratio |
 * | --- | --- |
 * | ink900 on honey500 | 11.35:1 |
 * | white on sage (verdictLikelyUseful) | 4.88:1 |
 * | ink900 on honey700 (verdictEvidenceUnclear) | 4.93:1 |
 * | paper on rust (verdictInvestigate) | 4.90:1 |
 * | paper on ink600 (verdictLocked) | 7.87:1 |
 * | ink900 on honey600 (warning) | 8.98:1 |
 */
val DarkGlowColors = GlowColors(
    honey300 = HoneyPalette.Honey300,
    honey400 = HoneyPalette.Honey400,
    honey500 = HoneyPalette.Honey500,
    honey600 = HoneyPalette.Honey600,
    honey700 = HoneyPalette.Honey700,
    ink900 = HoneyPalette.WarmWhite, // "ink" role inverts to the light warm text color on dark
    ink600 = HoneyPalette.WarmGrey,
    paper = HoneyPalette.Charcoal900,
    surfaceCard = HoneyPalette.Charcoal800,

    // Verdict chips are self-contained brand chips — kept identical to light theme so their
    // measured contrast ratios hold regardless of the surrounding page theme.
    verdictKeep = HoneyPalette.Honey500,
    verdictLikelyUseful = HoneyPalette.Sage,
    verdictEvidenceUnclear = HoneyPalette.Honey700,
    verdictInvestigate = HoneyPalette.Rust,
    verdictLocked = HoneyPalette.Ink600,

    onVerdictKeep = HoneyPalette.Ink900, // 11.35:1
    onVerdictLikelyUseful = Color.White, // 4.88:1 (paper text on sage falls to 4.34:1, too low)
    onVerdictEvidenceUnclear = HoneyPalette.Ink900, // 4.93:1
    onVerdictInvestigate = HoneyPalette.Paper, // 4.90:1
    onVerdictLocked = HoneyPalette.Paper, // 7.87:1

    success = HoneyPalette.Sage,
    warning = HoneyPalette.Honey600,
    onWarning = HoneyPalette.Ink900, // 8.98:1
    danger = HoneyPalette.Rust,

    chartGrid = HoneyPalette.WarmWhite.copy(alpha = 0.12f),
    chartLine = HoneyPalette.Honey400, // 13.46:1 vs charcoal — bright enough to read on dark
    chartFill = HoneyPalette.Honey400.copy(alpha = 0.20f),
    shimmer = Color(0xFF2A241A),
)

val LocalGlowColors = staticCompositionLocalOf { LightGlowColors }
