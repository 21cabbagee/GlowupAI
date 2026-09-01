package com.glowup.ai.core.ui

import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.glowup.ai.core.design.GlowMotion
import com.glowup.ai.core.design.GlowShapes
import com.glowup.ai.core.design.GlowSpacing
import com.glowup.ai.core.design.GlowUpTheme
import com.glowup.ai.core.design.rememberReducedMotion

/**
 * Polished button components for GlowUp AI, following Cal.ai-level quality standards.
 *
 * These buttons implement the design specifications from UI_REDESIGN_MASTER_PLAN.md:
 * - Amber to orange gradient for primary actions
 * - Soft shadows with proper elevation
 * - Smooth press animations (scale + alpha)
 * - Full state support (enabled, disabled, loading)
 * - Icon support with proper spacing
 * - Haptic feedback on interaction
 * - Accessibility-compliant with reduced motion support
 */

// ---- Color Constants from Master Plan ----

/** Primary gradient: amber (#F59E0B) to orange (#F97316) */
private val GradientAmber = Color(0xFFF59E0B)
private val GradientOrange = Color(0xFFF97316)

/** Primary text color: ink (#18181B) */
private val TextPrimary = Color(0xFF18181B)

/** Border color for secondary button: zinc-200 (#E4E4E7) */
private val BorderSecondary = Color(0xFFE4E4E7)

// ---- Primary Button ----

/**
 * Primary button with amber-to-orange gradient background and elevated appearance.
 *
 * Features:
 * - Gradient background (amber → orange)
 * - 2dp elevation with soft shadow
 * - Press animation: scales to 96% and reduces opacity to 80%
 * - Optional icon with 8dp spacing
 * - Loading state with spinner
 * - Disabled state with reduced opacity
 * - Haptic feedback on press
 * - Ripple effect built-in
 *
 * @param text Button label text
 * @param onClick Action to perform when clicked
 * @param modifier Modifier applied to the button container
 * @param icon Optional leading icon (Material Icon)
 * @param enabled Whether the button is interactive
 * @param loading Whether to show loading spinner instead of content
 * @param contentDescription Accessibility description (defaults to text)
 */
@Composable
fun GlowPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    loading: Boolean = false,
    contentDescription: String? = null,
) {
    val isInteractive = enabled && !loading
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val reducedMotion = rememberReducedMotion()
    val view = LocalView.current

    // Haptic feedback on press
    LaunchedEffect(isPressed) {
        if (isPressed && isInteractive) {
            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        }
    }

    // Press scale animation: 96% scale when pressed, 80% alpha
    val scale by animateFloatAsState(
        targetValue = if (isPressed && isInteractive) 0.96f else 1f,
        animationSpec =
            GlowMotion.respectingReducedMotion(
                GlowMotion.fast,
                reducedMotion,
            ) as androidx.compose.animation.core.AnimationSpec<Float>,
        label = "primaryButtonPressScale",
    )

    val alpha by animateFloatAsState(
        targetValue = if (isPressed && isInteractive) 0.8f else 1f,
        animationSpec =
            GlowMotion.respectingReducedMotion(
                GlowMotion.fast,
                reducedMotion,
            ) as androidx.compose.animation.core.AnimationSpec<Float>,
        label = "primaryButtonPressAlpha",
    )

    Button(
        onClick = onClick,
        modifier =
            modifier
                .fillMaxWidth()
                .height(56.dp)
                .scale(scale)
                .alpha(alpha)
                .shadow(
                    elevation = if (isPressed) 0.dp else 2.dp,
                    shape = GlowShapes.md,
                    clip = false,
                ).semantics {
                    this.contentDescription = contentDescription ?: text
                    if (!isInteractive) disabled()
                },
        enabled = isInteractive,
        shape = GlowShapes.md,
        colors =
            ButtonDefaults.buttonColors(
                containerColor = Color.Transparent,
                contentColor = TextPrimary,
                disabledContainerColor = Color.Transparent,
                disabledContentColor = TextPrimary.copy(alpha = 0.5f),
            ),
        contentPadding = PaddingValues(horizontal = GlowSpacing.lg, vertical = 20.dp),
        interactionSource = interactionSource,
        elevation =
            ButtonDefaults.buttonElevation(
                defaultElevation = 0.dp,
                pressedElevation = 0.dp,
                disabledElevation = 0.dp,
            ),
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(
                        brush =
                            Brush.horizontalGradient(
                                colors = listOf(GradientAmber, GradientOrange),
                            ),
                        shape = GlowShapes.md,
                        alpha = if (enabled) 1f else 0.5f,
                    ),
            contentAlignment = Alignment.Center,
        ) {
            if (loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = TextPrimary,
                    strokeWidth = 2.5.dp,
                )
            } else {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    icon?.let {
                        Icon(
                            imageVector = it,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = if (enabled) TextPrimary else TextPrimary.copy(alpha = 0.5f),
                        )
                    }
                    Text(
                        text = text,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (enabled) TextPrimary else TextPrimary.copy(alpha = 0.5f),
                    )
                }
            }
        }
    }
}

// ---- Secondary Button ----

/**
 * Secondary button with white background and subtle border.
 *
 * Features:
 * - White background with 1.5dp border
 * - Press animation: scales to 96% and reduces opacity to 80%
 * - Optional icon with 8dp spacing
 * - Loading state with spinner
 * - Disabled state with reduced opacity
 * - Haptic feedback on press
 * - Ripple effect built-in
 *
 * @param text Button label text
 * @param onClick Action to perform when clicked
 * @param modifier Modifier applied to the button container
 * @param icon Optional leading icon (Material Icon)
 * @param enabled Whether the button is interactive
 * @param loading Whether to show loading spinner instead of content
 * @param contentDescription Accessibility description (defaults to text)
 */
@Composable
fun GlowSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    loading: Boolean = false,
    contentDescription: String? = null,
) {
    val isInteractive = enabled && !loading
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val reducedMotion = rememberReducedMotion()
    val view = LocalView.current

    // Haptic feedback on press
    LaunchedEffect(isPressed) {
        if (isPressed && isInteractive) {
            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        }
    }

    // Press scale animation: 96% scale when pressed, 80% alpha
    val scale by animateFloatAsState(
        targetValue = if (isPressed && isInteractive) 0.96f else 1f,
        animationSpec =
            GlowMotion.respectingReducedMotion(
                GlowMotion.fast,
                reducedMotion,
            ) as androidx.compose.animation.core.AnimationSpec<Float>,
        label = "secondaryButtonPressScale",
    )

    val alpha by animateFloatAsState(
        targetValue = if (isPressed && isInteractive) 0.8f else 1f,
        animationSpec =
            GlowMotion.respectingReducedMotion(
                GlowMotion.fast,
                reducedMotion,
            ) as androidx.compose.animation.core.AnimationSpec<Float>,
        label = "secondaryButtonPressAlpha",
    )

    OutlinedButton(
        onClick = onClick,
        modifier =
            modifier
                .fillMaxWidth()
                .height(56.dp)
                .scale(scale)
                .alpha(alpha)
                .semantics {
                    this.contentDescription = contentDescription ?: text
                    if (!isInteractive) disabled()
                },
        enabled = isInteractive,
        shape = GlowShapes.md,
        border =
            BorderStroke(
                width = 1.5.dp,
                color = if (enabled) BorderSecondary else BorderSecondary.copy(alpha = 0.5f),
            ),
        colors =
            ButtonDefaults.outlinedButtonColors(
                containerColor = Color.White,
                contentColor = TextPrimary,
                disabledContainerColor = Color.White.copy(alpha = 0.5f),
                disabledContentColor = TextPrimary.copy(alpha = 0.5f),
            ),
        contentPadding = PaddingValues(horizontal = GlowSpacing.lg, vertical = 20.dp),
        interactionSource = interactionSource,
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = TextPrimary,
                strokeWidth = 2.5.dp,
            )
        } else {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                icon?.let {
                    Icon(
                        imageVector = it,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                    )
                }
                Text(
                    text = text,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
    }
}

// ---- Floating Action Button ----

/**
 * Floating Action Button with gradient background and elevated appearance.
 *
 * Designed for bottom-right positioning with prominent visual presence.
 *
 * Features:
 * - Gradient background (amber → orange)
 * - 6dp elevation with pronounced shadow
 * - Press animation: scales to 96% with slight rotation
 * - 56dp diameter (Material Design standard)
 * - Icon-only design (no text label)
 * - Loading state with spinner
 * - Disabled state with reduced opacity
 * - Haptic feedback on press
 *
 * @param onClick Action to perform when clicked
 * @param icon Icon to display (Material Icon, typically Add or Edit)
 * @param modifier Modifier applied to the FAB container
 * @param enabled Whether the button is interactive
 * @param loading Whether to show loading spinner instead of icon
 * @param contentDescription Accessibility description for the icon
 */
@Composable
fun GlowFloatingActionButton(
    onClick: () -> Unit,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    contentDescription: String? = null,
) {
    val isInteractive = enabled && !loading
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val reducedMotion = rememberReducedMotion()
    val view = LocalView.current

    // Haptic feedback on press
    LaunchedEffect(isPressed) {
        if (isPressed && isInteractive) {
            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        }
    }

    // Press scale animation: 96% scale when pressed
    val scale by animateFloatAsState(
        targetValue = if (isPressed && isInteractive) 0.96f else 1f,
        animationSpec =
            GlowMotion.respectingReducedMotion(
                GlowMotion.fast,
                reducedMotion,
            ) as androidx.compose.animation.core.AnimationSpec<Float>,
        label = "fabPressScale",
    )

    // Subtle rotation on press (2 degrees)
    val rotation by animateFloatAsState(
        targetValue = if (isPressed && isInteractive) 2f else 0f,
        animationSpec =
            GlowMotion.respectingReducedMotion(
                GlowMotion.fast,
                reducedMotion,
            ) as androidx.compose.animation.core.AnimationSpec<Float>,
        label = "fabPressRotation",
    )

    FloatingActionButton(
        onClick = onClick,
        modifier =
            modifier
                .size(56.dp)
                .scale(scale)
                .graphicsLayer {
                    rotationZ = rotation
                }.semantics {
                    this.contentDescription = contentDescription ?: "Add"
                    if (!isInteractive) disabled()
                },
        shape = RoundedCornerShape(16.dp),
        containerColor = Color.Transparent,
        contentColor = TextPrimary,
        elevation =
            FloatingActionButtonDefaults.elevation(
                defaultElevation = if (enabled) 6.dp else 2.dp,
                pressedElevation = if (enabled) 8.dp else 2.dp,
                hoveredElevation = if (enabled) 8.dp else 2.dp,
            ),
        interactionSource = interactionSource,
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(
                        brush =
                            Brush.horizontalGradient(
                                colors = listOf(GradientAmber, GradientOrange),
                            ),
                        shape = RoundedCornerShape(16.dp),
                        alpha = if (enabled) 1f else 0.5f,
                    ),
            contentAlignment = Alignment.Center,
        ) {
            if (loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = TextPrimary,
                    strokeWidth = 2.5.dp,
                )
            } else {
                Icon(
                    imageVector = icon,
                    contentDescription = contentDescription,
                    modifier = Modifier.size(24.dp),
                    tint = if (enabled) TextPrimary else TextPrimary.copy(alpha = 0.5f),
                )
            }
        }
    }
}

// ---- Preview Components ----

@Preview(name = "Light Theme Buttons", showBackground = true, backgroundColor = 0xFFFAFAF9)
@Composable
private fun GlowButtonsPreviewLight() {
    GlowUpTheme(darkTheme = false) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = "Primary Buttons",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF52525B),
            )

            GlowPrimaryButton(
                text = "Continue with Google",
                onClick = {},
                icon = Icons.Default.Star,
            )

            GlowPrimaryButton(
                text = "Loading",
                onClick = {},
                loading = true,
            )

            GlowPrimaryButton(
                text = "Disabled",
                onClick = {},
                enabled = false,
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Secondary Buttons",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF52525B),
            )

            GlowSecondaryButton(
                text = "Continue with email",
                onClick = {},
            )

            GlowSecondaryButton(
                text = "Cancel",
                onClick = {},
                icon = Icons.Default.Close,
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Floating Action Button",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF52525B),
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                GlowFloatingActionButton(
                    onClick = {},
                    icon = Icons.Default.Add,
                    contentDescription = "Add product",
                )

                GlowFloatingActionButton(
                    onClick = {},
                    icon = Icons.Default.Edit,
                    loading = true,
                    contentDescription = "Edit",
                )

                GlowFloatingActionButton(
                    onClick = {},
                    icon = Icons.Default.Add,
                    enabled = false,
                    contentDescription = "Add (disabled)",
                )
            }
        }
    }
}

@Preview(name = "Dark Theme Buttons", showBackground = true, backgroundColor = 0xFF0F0D0A)
@Composable
private fun GlowButtonsPreviewDark() {
    GlowUpTheme(darkTheme = true) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = "Primary Button",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFFC9BFA9),
            )

            GlowPrimaryButton(
                text = "Continue",
                onClick = {},
            )

            Text(
                text = "Secondary Button",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFFC9BFA9),
            )

            GlowSecondaryButton(
                text = "Cancel",
                onClick = {},
            )

            Text(
                text = "Floating Action Button",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFFC9BFA9),
            )

            GlowFloatingActionButton(
                onClick = {},
                icon = Icons.Default.Add,
                contentDescription = "Add",
            )
        }
    }
}
