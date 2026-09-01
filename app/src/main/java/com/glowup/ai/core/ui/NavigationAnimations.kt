package com.glowup.ai.core.ui

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.navigation.NavBackStackEntry
import com.glowup.ai.core.design.GlowMotion

/**
 * Navigation screen transitions following Material Motion guidelines.
 *
 * - Tab switches: Fade only (no slide) for fast, non-directional switches
 * - Forward navigation: Slide in from right + fade
 * - Back navigation: Slide out to right + fade
 * - Modal screens: Slide up from bottom
 *
 * Duration: 220ms (GlowMotion.slow) for screen transitions
 * Easing: cubic-bezier(0.2, 0.8, 0.2, 1)
 */
object NavigationAnimations {
    /**
     * Standard forward navigation: slide in from right + fade
     * Use for navigating deeper into the hierarchy
     */
    fun enterTransition(): EnterTransition =
        slideInHorizontally(
            initialOffsetX = { fullWidth -> fullWidth / 4 },
            animationSpec = tween(durationMillis = 220, easing = GlowMotion.easing),
        ) +
            fadeIn(
                animationSpec = tween(durationMillis = 220, easing = GlowMotion.easing),
            )

    /**
     * Standard exit when navigating forward: fade out + slight slide left
     * The current screen fades and shifts slightly as the new one enters
     */
    fun exitTransition(): ExitTransition =
        slideOutHorizontally(
            targetOffsetX = { fullWidth -> -fullWidth / 8 },
            animationSpec = tween(durationMillis = 220, easing = GlowMotion.easing),
        ) +
            fadeOut(
                animationSpec = tween(durationMillis = 220, easing = GlowMotion.easing),
            )

    /**
     * Return transition when popping back: slide in from left + fade
     * The previous screen slides back into view
     */
    fun popEnterTransition(): EnterTransition =
        slideInHorizontally(
            initialOffsetX = { fullWidth -> -fullWidth / 8 },
            animationSpec = tween(durationMillis = 220, easing = GlowMotion.easing),
        ) +
            fadeIn(
                animationSpec = tween(durationMillis = 220, easing = GlowMotion.easing),
            )

    /**
     * Exit when popping back: slide out to right + fade
     * Current screen slides away to the right
     */
    fun popExitTransition(): ExitTransition =
        slideOutHorizontally(
            targetOffsetX = { fullWidth -> fullWidth / 4 },
            animationSpec = tween(durationMillis = 220, easing = GlowMotion.easing),
        ) +
            fadeOut(
                animationSpec = tween(durationMillis = 220, easing = GlowMotion.easing),
            )

    /**
     * Tab switch transition: fade only, no directional slide
     * Use for bottom navigation tab switches
     */
    fun tabEnterTransition(): EnterTransition =
        fadeIn(
            animationSpec = tween(durationMillis = 180, easing = GlowMotion.easing),
        )

    /**
     * Tab exit: fade out
     */
    fun tabExitTransition(): ExitTransition =
        fadeOut(
            animationSpec = tween(durationMillis = 180, easing = GlowMotion.easing),
        )

    /**
     * Modal enter: slide up from bottom + fade
     * Use for bottom sheets, dialogs, or modal overlays
     */
    fun modalEnterTransition(): EnterTransition =
        slideInVertically(
            initialOffsetY = { fullHeight -> fullHeight / 3 },
            animationSpec = tween(durationMillis = 220, easing = GlowMotion.easing),
        ) +
            fadeIn(
                animationSpec = tween(durationMillis = 220, easing = GlowMotion.easing),
            )

    /**
     * Modal exit: slide down + fade
     */
    fun modalExitTransition(): ExitTransition =
        slideOutVertically(
            targetOffsetY = { fullHeight -> fullHeight / 3 },
            animationSpec = tween(durationMillis = 220, easing = GlowMotion.easing),
        ) +
            fadeOut(
                animationSpec = tween(durationMillis = 220, easing = GlowMotion.easing),
            )
}

/**
 * Extension to apply standard navigation animations to a composable destination.
 * Use in NavGraphBuilder.composable() calls.
 */
fun AnimatedContentTransitionScope<NavBackStackEntry>.defaultEnterTransition(): EnterTransition = NavigationAnimations.enterTransition()

fun AnimatedContentTransitionScope<NavBackStackEntry>.defaultExitTransition(): ExitTransition = NavigationAnimations.exitTransition()

fun AnimatedContentTransitionScope<NavBackStackEntry>.defaultPopEnterTransition(): EnterTransition =
    NavigationAnimations.popEnterTransition()

fun AnimatedContentTransitionScope<NavBackStackEntry>.defaultPopExitTransition(): ExitTransition = NavigationAnimations.popExitTransition()
