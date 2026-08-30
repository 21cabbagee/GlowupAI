package com.glowup.ai.core.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import com.glowup.ai.core.design.GlowMotion
import com.glowup.ai.core.design.rememberReducedMotion
import kotlinx.coroutines.delay

/**
 * List item animations for smooth, staggered appearances.
 *
 * Usage:
 * ```
 * LazyColumn {
 *   itemsIndexed(items) { index, item ->
 *     AnimatedListItem(index = index) {
 *       // Item content
 *     }
 *   }
 * }
 * ```
 */

/**
 * Wraps a list item with fade-in + slide-up animation.
 * Staggered by index for a cascading effect.
 *
 * @param index Item position in the list (used for stagger delay)
 * @param staggerDelayMs Delay between each item (default 50ms)
 * @param content The item content
 */
@Composable
fun AnimatedListItem(
    index: Int,
    staggerDelayMs: Int = 50,
    content: @Composable AnimatedVisibilityScope.() -> Unit
) {
    val reducedMotion = rememberReducedMotion()
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (!reducedMotion) {
            delay((index * staggerDelayMs).toLong())
        }
        visible = true
    }

    AnimatedVisibility(
        visible = visible,
        enter = if (reducedMotion) {
            fadeIn(animationSpec = tween(0))
        } else {
            slideInVertically(
                initialOffsetY = { it / 4 },
                animationSpec = tween(durationMillis = 220, easing = GlowMotion.easing)
            ) + fadeIn(
                animationSpec = tween(durationMillis = 220, easing = GlowMotion.easing)
            )
        },
        exit = fadeOut(animationSpec = tween(durationMillis = 180, easing = GlowMotion.easing)),
        content = content
    )
}

/**
 * Modifier for list items that fade in as they appear.
 * Lighter weight alternative to AnimatedListItem when you don't need AnimatedVisibility.
 *
 * @param index Item position (for stagger)
 * @param staggerDelayMs Delay between items
 */
fun Modifier.listItemFadeIn(
    index: Int,
    staggerDelayMs: Int = 50
): Modifier = composed {
    val reducedMotion = rememberReducedMotion()
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (!reducedMotion) {
            delay((index * staggerDelayMs).toLong())
        }
        visible = true
    }

    val alpha by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = if (reducedMotion) {
            tween(0)
        } else {
            tween(durationMillis = 220, easing = GlowMotion.easing)
        },
        label = "listItemFadeIn"
    )

    this.alpha(alpha)
}

/**
 * Modifier for list items that scale in slightly as they appear.
 * Subtle "pop" effect for engagement.
 *
 * @param index Item position (for stagger)
 * @param staggerDelayMs Delay between items
 */
fun Modifier.listItemScaleIn(
    index: Int,
    staggerDelayMs: Int = 50
): Modifier = composed {
    val reducedMotion = rememberReducedMotion()
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (!reducedMotion) {
            delay((index * staggerDelayMs).toLong())
        }
        visible = true
    }

    val scale by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (visible) 1f else 0.92f,
        animationSpec = if (reducedMotion) {
            tween(0)
        } else {
            tween(durationMillis = 220, easing = GlowMotion.easing)
        },
        label = "listItemScaleIn"
    )

    val alpha by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = if (reducedMotion) {
            tween(0)
        } else {
            tween(durationMillis = 220, easing = GlowMotion.easing)
        },
        label = "listItemAlphaIn"
    )

    this
        .scale(scale)
        .alpha(alpha)
}

/**
 * Animated removal for list items being deleted.
 * Slides out to the left and fades.
 */
@Composable
fun AnimatedListItemRemoval(
    visible: Boolean,
    content: @Composable AnimatedVisibilityScope.() -> Unit
) {
    val reducedMotion = rememberReducedMotion()

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = if (reducedMotion) {
            fadeOut(animationSpec = tween(0))
        } else {
            slideOutVertically(
                targetOffsetY = { -it / 4 },
                animationSpec = tween(durationMillis = 180, easing = GlowMotion.easing)
            ) + fadeOut(
                animationSpec = tween(durationMillis = 180, easing = GlowMotion.easing)
            )
        },
        content = content
    )
}
