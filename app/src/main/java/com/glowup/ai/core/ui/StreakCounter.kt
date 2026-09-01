package com.glowup.ai.core.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.glowup.ai.core.design.GlowShapes
import com.glowup.ai.core.design.GlowSpacing
import com.glowup.ai.core.design.LocalGlowColors
import com.glowup.ai.domain.model.Streak

/**
 * Streak Counter Component
 * Displays user's capture streak with visual prominence
 * Inspired by Duolingo/Strava streak mechanics with loss aversion psychology
 */
@Composable
fun StreakCounter(
    streak: Streak,
    modifier: Modifier = Modifier,
    onFreezeDayClick: () -> Unit = {},
    showWarning: Boolean = false,
    warningMessage: String? = null,
) {
    val glowColors = LocalGlowColors.current
    val honeyColor = glowColors.honey500
    val inkColor = glowColors.ink900

    // Animate streak number
    val scale by animateFloatAsState(
        targetValue = if (streak.currentStreak > 0) 1f else 0.8f,
        animationSpec = tween(300),
    )

    // Pulse animation for flame icon (breathing effect)
    val flamePulse =
        rememberPulseAnimation(
            enabled = streak.currentStreak > 0,
            minScale = 0.97f,
            maxScale = 1.03f,
            durationMillis = 2000,
        )

    // Shake animation when at risk
    val shakeOffset = rememberShakeAnimation(trigger = showWarning)

    // Track freeze day usage for celebration
    var freezeDayUsed by remember { mutableStateOf(false) }
    val celebration = rememberCelebrationAnimation(trigger = freezeDayUsed)

    Card(
        modifier =
            modifier
                .fillMaxWidth()
                .graphicsLayer {
                    translationX = shakeOffset
                },
        shape = GlowShapes.md,
        colors =
            CardDefaults.cardColors(
                containerColor =
                    if (showWarning) {
                        MaterialTheme.colorScheme.errorContainer
                    } else {
                        honeyColor
                    },
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(GlowSpacing.md),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Warning message if at risk
            AnimatedVisibility(
                visible = showWarning && warningMessage != null,
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = warningMessage ?: "",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }

            // Main streak display
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Flame icon + current streak
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        modifier =
                            Modifier
                                .size(80.dp)
                                .scale(scale)
                                .background(
                                    color = inkColor.copy(alpha = 0.1f),
                                    shape = CircleShape,
                                ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.LocalFireDepartment,
                            contentDescription = "Streak flame",
                            tint = if (streak.currentStreak > 0) inkColor else inkColor.copy(alpha = 0.3f),
                            modifier =
                                Modifier
                                    .size(40.dp)
                                    .scale(flamePulse),
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "${streak.currentStreak}",
                        style = MaterialTheme.typography.displayLarge,
                        fontWeight = FontWeight.Bold,
                        color = inkColor,
                    )
                    Text(
                        text = "day streak",
                        style = MaterialTheme.typography.bodyMedium,
                        color = inkColor.copy(alpha = 0.8f),
                    )
                }

                // Divider
                Divider(
                    modifier =
                        Modifier
                            .height(80.dp)
                            .width(1.dp),
                    color = inkColor.copy(alpha = 0.2f),
                )

                // Longest streak
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(
                        imageVector = Icons.Filled.EmojiEvents,
                        contentDescription = "Trophy",
                        tint = inkColor,
                        modifier = Modifier.size(32.dp),
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "${streak.longestStreak}",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = inkColor,
                    )
                    Text(
                        text = "longest",
                        style = MaterialTheme.typography.bodyMedium,
                        color = inkColor.copy(alpha = 0.8f),
                    )
                }
            }

            // Freeze day indicator
            if (streak.currentStreak > 0) {
                Spacer(modifier = Modifier.height(16.dp))
                Divider(color = inkColor.copy(alpha = 0.2f))
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.AcUnit,
                            contentDescription = "Freeze day",
                            tint = if (streak.canUseFreeze) inkColor else inkColor.copy(alpha = 0.3f),
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Freeze Day",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = inkColor,
                            )
                            Text(
                                text =
                                    if (streak.canUseFreeze) {
                                        "1 available this week"
                                    } else {
                                        "Used this week"
                                    },
                                style = MaterialTheme.typography.bodySmall,
                                color = inkColor.copy(alpha = 0.7f),
                            )
                        }
                    }

                    if (streak.canUseFreeze && showWarning) {
                        FilledTonalButton(
                            onClick = {
                                freezeDayUsed = true
                                onFreezeDayClick()
                            },
                            colors =
                                ButtonDefaults.filledTonalButtonColors(
                                    containerColor = inkColor,
                                    contentColor = honeyColor,
                                ),
                            modifier =
                                Modifier
                                    .scale(celebration.scale)
                                    .graphicsLayer {
                                        rotationZ = celebration.rotation
                                    },
                        ) {
                            Text("Use Freeze")
                        }
                    }
                }
            }
        }
    }
}

/**
 * Compact streak indicator for top bar
 */
@Composable
fun CompactStreakIndicator(
    streak: Streak,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    val glowColors = LocalGlowColors.current
    val honeyColor = glowColors.honey500
    val inkColor = glowColors.ink900

    // Subtle pulse for active streaks
    val flamePulse =
        rememberPulseAnimation(
            enabled = streak.currentStreak > 0,
            minScale = 0.98f,
            maxScale = 1.02f,
            durationMillis = 2500,
        )

    Surface(
        modifier = modifier.animatedClickable(onClick = onClick),
        onClick = onClick,
        shape = GlowShapes.md,
        color = honeyColor,
        shadowElevation = 2.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                imageVector = Icons.Filled.LocalFireDepartment,
                contentDescription = "Streak",
                tint = inkColor,
                modifier =
                    Modifier
                        .size(18.dp)
                        .scale(flamePulse),
            )
            Text(
                text = "${streak.currentStreak}",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = inkColor,
            )
        }
    }
}
