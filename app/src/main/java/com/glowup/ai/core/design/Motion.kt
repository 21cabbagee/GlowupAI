package com.glowup.ai.core.design

import android.provider.Settings
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.animation.core.spring as composeSpring

/**
 * Motion communicates state, never decorates: 140-220ms, cubic-bezier(0.2, 0.8, 0.2, 1). Every
 * spec below honours the platform's "remove animations" setting via [rememberReducedMotion] —
 * callers should branch on that flag and swap to [snap] (or an equivalent zero-duration spec)
 * rather than skip the state change entirely.
 */
object GlowMotion {
    /** cubic-bezier(0.2, 0.8, 0.2, 1) */
    val easing: Easing = CubicBezierEasing(0.2f, 0.8f, 0.2f, 1f)

    val fast: AnimationSpec<Float> = tween(durationMillis = 140, easing = easing)
    val standard: AnimationSpec<Float> = tween(durationMillis = 180, easing = easing)
    val slow: AnimationSpec<Float> = tween(durationMillis = 220, easing = easing)

    /** A spring tuned to settle within the same 140-220ms envelope as [fast]/[standard]/[slow]. */
    fun <T> spring(
        dampingRatio: Float = Spring.DampingRatioNoBouncy,
        stiffness: Float = Spring.StiffnessMediumLow,
    ): FiniteAnimationSpec<T> = composeSpring(dampingRatio = dampingRatio, stiffness = stiffness)

    /**
     * Returns [spec] unchanged, or an instant [snap] when [reducedMotion] is true. Use this to
     * wrap any [fast]/[standard]/[slow]/[spring] value before handing it to `animate*AsState`,
     * `AnimatedVisibility`, etc.
     */
    fun <T> respectingReducedMotion(
        spec: AnimationSpec<T>,
        reducedMotion: Boolean,
    ): AnimationSpec<T> = if (reducedMotion) snap() else spec
}

/**
 * Reads the platform "Remove animations" / animator-duration-scale accessibility setting, which
 * is Android's closest equivalent to web's `prefers-reduced-motion`. Falls back to `false`
 * (motion enabled) if the setting cannot be read, and re-checks whenever recomposition occurs
 * with a changed [LocalView] (i.e. on window/config changes) rather than caching forever.
 */
@Composable
fun rememberReducedMotion(): Boolean {
    val context = LocalContext.current
    return remember(LocalView.current) {
        runCatching {
            Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
        }.getOrDefault(1f) == 0f
    }
}
