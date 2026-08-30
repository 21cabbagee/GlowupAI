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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
    warningMessage: String? = null
) {
    val glowColors = LocalGlowColors.current
    val honeyColor = glowColors.honey500
    val inkColor = glowColors.ink900

    // Animate streak number
    val scale by animateFloatAsState(
        targetValue = if (streak.currentStreak > 0) 1f else 0.8f,
        animationSpec = tween(300)
    )

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (showWarning) {
                MaterialTheme.colorScheme.errorContainer
            } else {
                honeyColor
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Warning message if at risk
            AnimatedVisibility(
                visible = showWarning && warningMessage != null,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = warningMessage ?: "",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Main streak display
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Flame icon + current streak
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .scale(scale)
                            .background(
                                color = inkColor.copy(alpha = 0.1f),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.LocalFireDepartment,
                            contentDescription = "Streak flame",
                            tint = if (streak.currentStreak > 0) inkColor else inkColor.copy(alpha = 0.3f),
                            modifier = Modifier.size(40.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "${streak.currentStreak}",
                        style = MaterialTheme.typography.displayLarge,
                        fontWeight = FontWeight.Bold,
                        color = inkColor
                    )
                    Text(
                        text = "day streak",
                        style = MaterialTheme.typography.bodyMedium,
                        color = inkColor.copy(alpha = 0.8f)
                    )
                }

                // Divider
                Divider(
                    modifier = Modifier
                        .height(80.dp)
                        .width(1.dp),
                    color = inkColor.copy(alpha = 0.2f)
                )

                // Longest streak
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Filled.EmojiEvents,
                        contentDescription = "Trophy",
                        tint = inkColor,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "${streak.longestStreak}",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = inkColor
                    )
                    Text(
                        text = "longest",
                        style = MaterialTheme.typography.bodyMedium,
                        color = inkColor.copy(alpha = 0.8f)
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
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.AcUnit,
                            contentDescription = "Freeze day",
                            tint = if (streak.canUseFreeze) inkColor else inkColor.copy(alpha = 0.3f),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Freeze Day",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = inkColor
                            )
                            Text(
                                text = if (streak.canUseFreeze) {
                                    "1 available this week"
                                } else {
                                    "Used this week"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = inkColor.copy(alpha = 0.7f)
                            )
                        }
                    }

                    if (streak.canUseFreeze && showWarning) {
                        FilledTonalButton(
                            onClick = onFreezeDayClick,
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = inkColor,
                                contentColor = honeyColor
                            )
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
    onClick: () -> Unit = {}
) {
    val glowColors = LocalGlowColors.current
    val honeyColor = glowColors.honey500
    val inkColor = glowColors.ink900

    Surface(
        modifier = modifier,
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = honeyColor,
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.LocalFireDepartment,
                contentDescription = "Streak",
                tint = inkColor,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = "${streak.currentStreak}",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = inkColor
            )
        }
    }
}
