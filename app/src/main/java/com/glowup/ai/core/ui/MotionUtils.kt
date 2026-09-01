package com.glowup.ai.core.ui

import android.provider.Settings
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/**
 * The easing curve specified for GlowUp motion: cubic-bezier(0.2, 0.8, 0.2, 1).
 * Component-local until `core.design.GlowMotion` exposes a stable, named accessor for it.
 */
internal val GlowEasing = CubicBezierEasing(0.2f, 0.8f, 0.2f, 1f)

/**
 * True when the system's "remove animations" accessibility setting is on
 * (`Settings.Global.ANIMATOR_DURATION_SCALE == 0`). Every animated component in this package
 * checks this and falls back to a static presentation instead of looping or fading.
 */
@Composable
internal fun isReducedMotionEnabled(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        val scale =
            try {
                Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
            } catch (_: Exception) {
                1f
            }
        scale == 0f
    }
}
