package com.glowup.ai.core.design

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

/**
 * App-wide theme root. Wires the honey brand palette into [MaterialTheme] and exposes the brand
 * extras via [LocalGlowColors].
 *
 * `dynamicColor` is deliberately NOT a parameter here. Material You wallpaper-derived color
 * (`dynamicLightColorScheme`/`dynamicDarkColorScheme`) must never override the brand on
 * Android 12+ — see ANDROID_PLAN.md Phase 2.1. If a future task needs it, it must be opt-in and
 * default `false`, never default `true`.
 */
@Composable
fun GlowUpTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) GlowDarkColorScheme else GlowLightColorScheme
    val glowColors = if (darkTheme) DarkGlowColors else LightGlowColors

    CompositionLocalProvider(LocalGlowColors provides glowColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = GlowTypography,
            shapes = GlowMaterialShapes,
            content = content,
        )
    }
}
