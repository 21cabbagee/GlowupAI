package com.glowup.ai.core.ui

import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.animation.core.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import com.glowup.ai.core.design.GlowMotion
import com.glowup.ai.core.design.rememberReducedMotion
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Animation utilities for delightful micro-interactions
 * Following Material Motion guidelines and accessibility best practices
 */

/**
 * Performs haptic feedback on the view
 */
fun View.performHaptic(feedbackType: Int = HapticFeedbackConstants.KEYBOARD_TAP) {
    performHapticFeedback(feedbackType)
}

/**
 * Scale animation modifier that responds to press events
 * Creates a subtle "press down" effect
 */
fun Modifier.pressScale(
    targetScale: Float = 0.95f,
    enableHaptic: Boolean = true,
): Modifier =
    composed {
        val interactionSource = remember { MutableInteractionSource() }
        val isPressed by interactionSource.collectIsPressedAsState()
        val reducedMotion = rememberReducedMotion()
        val view = LocalView.current

        val scale by animateFloatAsState(
            targetValue = if (isPressed) targetScale else 1f,
            animationSpec =
                GlowMotion.respectingReducedMotion(
                    GlowMotion.fast,
                    reducedMotion,
                ) as AnimationSpec<Float>,
            label = "pressScale",
        )

        LaunchedEffect(isPressed) {
            if (isPressed && enableHaptic) {
                view.performHaptic()
            }
        }

        this
            .scale(scale)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
            ) { }
    }

/**
 * Ripple effect animation for tap interactions
 */
@Composable
fun rememberRippleAnimation(onTap: () -> Unit = {}): RippleState {
    val scope = rememberCoroutineScope()
    val view = LocalView.current
    val state = remember { RippleState() }

    return state.copy(
        onTrigger = {
            scope.launch {
                state.isAnimating = true
                view.performHaptic(HapticFeedbackConstants.KEYBOARD_TAP)
                onTap()
                delay(300)
                state.isAnimating = false
            }
        },
    )
}

data class RippleState(
    var isAnimating: Boolean = false,
    val onTrigger: () -> Unit = {},
)

/**
 * Pulse animation for breathing effects (like flame icon)
 */
@Composable
fun rememberPulseAnimation(
    enabled: Boolean = true,
    minScale: Float = 0.95f,
    maxScale: Float = 1.05f,
    durationMillis: Int = 1500,
): Float {
    val reducedMotion = rememberReducedMotion()

    if (!enabled || reducedMotion) {
        return 1f
    }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    return infiniteTransition
        .animateFloat(
            initialValue = minScale,
            targetValue = maxScale,
            animationSpec =
                infiniteRepeatable(
                    animation = tween(durationMillis, easing = GlowMotion.easing),
                    repeatMode = RepeatMode.Reverse,
                ),
            label = "pulseScale",
        ).value
}

/**
 * Shake animation for warning states
 */
@Composable
fun rememberShakeAnimation(trigger: Boolean = false): Float {
    var animationPlayed by remember { mutableStateOf(false) }
    val reducedMotion = rememberReducedMotion()

    LaunchedEffect(trigger) {
        if (trigger && !reducedMotion) {
            animationPlayed = !animationPlayed
        }
    }

    if (reducedMotion) return 0f

    val transition =
        updateTransition(
            targetState = animationPlayed,
            label = "shake",
        )

    return transition
        .animateFloat(
            transitionSpec = {
                keyframes {
                    durationMillis = 600
                    0f at 0
                    -15f at 100
                    15f at 200
                    -15f at 300
                    15f at 400
                    -10f at 500
                    0f at 600
                }
            },
            label = "shakeOffset",
            targetValueByState = { state -> if (state) 0f else 0f },
        ).value
}

/**
 * Shine effect animation for unlocked achievements
 */
@Composable
fun rememberShineAnimation(trigger: Boolean = false): Float {
    var animationPlayed by remember { mutableStateOf(false) }
    val reducedMotion = rememberReducedMotion()

    LaunchedEffect(trigger) {
        if (trigger && !reducedMotion) {
            animationPlayed = true
            delay(1000)
            animationPlayed = false
        }
    }

    if (reducedMotion || !trigger) return -1f

    val transition = rememberInfiniteTransition(label = "shine")
    return transition
        .animateFloat(
            initialValue = -1f,
            targetValue = 2f,
            animationSpec =
                infiniteRepeatable(
                    animation = tween(2000, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart,
                ),
            label = "shinePosition",
        ).value
}

/**
 * Celebration animation for milestone events
 */
@Composable
fun rememberCelebrationAnimation(trigger: Boolean = false): CelebrationState {
    var isAnimating by remember { mutableStateOf(false) }
    val reducedMotion = rememberReducedMotion()
    val view = LocalView.current

    LaunchedEffect(trigger) {
        if (trigger && !reducedMotion) {
            isAnimating = true
            view.performHaptic(HapticFeedbackConstants.LONG_PRESS)
            delay(1000)
            isAnimating = false
        }
    }

    val scale by animateFloatAsState(
        targetValue = if (isAnimating) 1.2f else 1f,
        animationSpec =
            spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow,
            ),
        label = "celebrationScale",
    )

    val rotation by animateFloatAsState(
        targetValue = if (isAnimating) 360f else 0f,
        animationSpec = tween(1000, easing = GlowMotion.easing),
        label = "celebrationRotation",
    )

    return CelebrationState(
        isAnimating = isAnimating,
        scale = scale,
        rotation = rotation,
    )
}

data class CelebrationState(
    val isAnimating: Boolean = false,
    val scale: Float = 1f,
    val rotation: Float = 0f,
)

/**
 * Fade-in animation for list items
 */
@Composable
fun rememberFadeInAnimation(delay: Int = 0): Float {
    val reducedMotion = rememberReducedMotion()
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(delay.toLong())
        visible = true
    }

    return animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec =
            GlowMotion.respectingReducedMotion(
                tween<Float>(300, easing = GlowMotion.easing),
                reducedMotion,
            ) as AnimationSpec<Float>,
        label = "fadeIn",
    ).value
}

/**
 * Highlight pulse animation for "today" indicator
 */
@Composable
fun rememberHighlightAnimation(enabled: Boolean = true): Float {
    val reducedMotion = rememberReducedMotion()

    if (!enabled || reducedMotion) {
        return 1f
    }

    val infiniteTransition = rememberInfiniteTransition(label = "highlight")
    return infiniteTransition
        .animateFloat(
            initialValue = 0.7f,
            targetValue = 1f,
            animationSpec =
                infiniteRepeatable(
                    animation = tween(1000, easing = GlowMotion.easing),
                    repeatMode = RepeatMode.Reverse,
                ),
            label = "highlightAlpha",
        ).value
}

/**
 * Progress animation for progress bars
 */
@Composable
fun rememberProgressAnimation(
    targetProgress: Float,
    durationMillis: Int = 600,
): Float {
    val reducedMotion = rememberReducedMotion()

    return animateFloatAsState(
        targetValue = targetProgress,
        animationSpec =
            GlowMotion.respectingReducedMotion(
                tween<Float>(durationMillis, easing = GlowMotion.easing),
                reducedMotion,
            ) as AnimationSpec<Float>,
        label = "progress",
    ).value
}

/**
 * Bounce effect for successful actions
 */
@Composable
fun rememberBounceAnimation(trigger: Boolean = false): Float {
    var animationPlayed by remember { mutableStateOf(false) }
    val reducedMotion = rememberReducedMotion()
    val view = LocalView.current

    LaunchedEffect(trigger) {
        if (trigger && !reducedMotion) {
            view.performHaptic(HapticFeedbackConstants.CONFIRM)
            animationPlayed = !animationPlayed
        }
    }

    if (reducedMotion) return 1f

    val transition =
        updateTransition(
            targetState = animationPlayed,
            label = "bounce",
        )

    return transition
        .animateFloat(
            transitionSpec = {
                keyframes {
                    durationMillis = 400
                    1f at 0
                    1.15f at 100 with FastOutSlowInEasing
                    0.95f at 200 with FastOutSlowInEasing
                    1.05f at 300 with FastOutSlowInEasing
                    1f at 400
                }
            },
            label = "bounceScale",
            targetValueByState = { state -> if (state) 1f else 1f },
        ).value
}

/**
 * Modifier for clickable items with scale animation and haptic feedback
 */
fun Modifier.animatedClickable(
    enabled: Boolean = true,
    enableHaptic: Boolean = true,
    onClick: () -> Unit,
): Modifier =
    composed {
        val interactionSource = remember { MutableInteractionSource() }
        val isPressed by interactionSource.collectIsPressedAsState()
        val reducedMotion = rememberReducedMotion()
        val view = LocalView.current

        val scale by animateFloatAsState(
            targetValue = if (isPressed) 0.96f else 1f,
            animationSpec =
                GlowMotion.respectingReducedMotion(
                    GlowMotion.fast,
                    reducedMotion,
                ) as AnimationSpec<Float>,
            label = "clickScale",
        )

        this
            .scale(scale)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
            ) {
                if (enableHaptic) {
                    view.performHaptic()
                }
                onClick()
            }
    }
