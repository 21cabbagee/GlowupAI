package com.glowup.ai.core.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.glowup.ai.core.design.GlowShapes
import com.glowup.ai.core.design.LocalGlowColors
import com.glowup.ai.domain.model.AchievementTier
import com.glowup.ai.domain.model.UserAchievement

/**
 * Achievement Card Component
 * Displays individual achievement with progress and unlock state
 * Implements variable rewards psychology from research
 */
@Composable
fun AchievementCard(
    achievement: UserAchievement,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    val glowColors = LocalGlowColors.current
    val honeyColor = glowColors.honey500
    val inkColor = glowColors.ink900
    val reducedMotion = isReducedMotionEnabled()

    // Animate unlock state (respecting reduced motion)
    val scale by animateFloatAsState(
        targetValue = if (achievement.isNew && !reducedMotion) 1.1f else 1f,
        animationSpec =
            if (reducedMotion) {
                tween(0)
            } else {
                spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow,
                )
            },
        label = "achievementScale",
    )

    val alpha = if (achievement.isUnlocked) 1f else 0.6f
    val tierColor = getTierColor(achievement.type.tier)

    // Shine effect for unlocked achievements
    val shinePosition = rememberShineAnimation(trigger = achievement.isUnlocked && achievement.isNew)

    // Animated progress
    val animatedProgress = rememberProgressAnimation(achievement.progress)

    val cardDescription =
        buildString {
            append(achievement.type.title)
            append(". ")
            append(achievement.type.description)
            append(". ")
            if (achievement.isUnlocked) {
                append("Unlocked. ${achievement.type.tier.displayName} tier.")
            } else {
                append("Locked. Progress: ${achievement.getProgressText()}")
            }
        }

    Card(
        modifier =
            modifier
                .scale(scale)
                .alpha(alpha)
                .semantics {
                    contentDescription = cardDescription
                },
        onClick = onClick,
        shape = GlowShapes.md,
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface,
            ),
        elevation =
            CardDefaults.cardElevation(
                defaultElevation = if (achievement.isUnlocked) 4.dp else 1.dp,
            ),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Achievement icon with tier background
            Box(
                modifier =
                    Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(tierColor.copy(alpha = 0.2f))
                        .then(
                            if (achievement.isUnlocked) {
                                Modifier.border(
                                    width = 4.dp,
                                    color = tierColor,
                                    shape = CircleShape,
                                )
                            } else {
                                Modifier
                            },
                        ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = achievement.type.icon,
                    contentDescription = achievement.type.title,
                    tint = if (achievement.isUnlocked) tierColor else Color.Gray,
                    modifier = Modifier.size(32.dp),
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Title
            Text(
                text = achievement.type.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color =
                    if (achievement.isUnlocked) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    },
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Description
            Text(
                text = achievement.type.description,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Progress or unlock date
            if (achievement.isUnlocked) {
                // Tier badge with shine effect
                Surface(
                    shape = GlowShapes.sm,
                    color = tierColor.copy(alpha = 0.2f),
                    modifier =
                        Modifier.drawWithContent {
                            drawContent()
                            // Draw shine effect overlay
                            if (shinePosition >= 0f) {
                                val shineGradient =
                                    Brush.linearGradient(
                                        colors =
                                            listOf(
                                                Color.Transparent,
                                                Color.White.copy(alpha = 0.3f),
                                                Color.Transparent,
                                            ),
                                        start = Offset(shinePosition * size.width - size.width, 0f),
                                        end = Offset(shinePosition * size.width, size.height),
                                    )
                                drawRect(brush = shineGradient)
                            }
                        },
                ) {
                    Text(
                        text = achievement.type.tier.displayName,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = tierColor,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    )
                }
            } else {
                // Animated progress bar
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    LinearProgressIndicator(
                        progress = { animatedProgress },
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                        color = honeyColor,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = achievement.getProgressText(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

/**
 * Compact achievement badge (for profile/nav)
 */
@Composable
fun AchievementBadge(
    achievement: UserAchievement,
    modifier: Modifier = Modifier,
    size: Int = 40,
) {
    val tierColor = getTierColor(achievement.type.tier)

    Box(
        modifier =
            modifier
                .size(size.dp)
                .clip(CircleShape)
                .background(tierColor.copy(alpha = 0.2f))
                .border(
                    width = 2.dp,
                    color = tierColor,
                    shape = CircleShape,
                ).semantics {
                    contentDescription = "${achievement.type.title} achievement badge, ${achievement.type.tier.displayName} tier"
                },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = achievement.type.icon,
            contentDescription = null,
            tint = tierColor,
            modifier = Modifier.size((size * 0.6).dp),
        )
    }
}

/**
 * Achievement unlock celebration animation
 */
@Composable
fun AchievementCelebration(
    achievement: UserAchievement,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val glowColors = LocalGlowColors.current
    val honeyColor = glowColors.honey500
    val tierColor = getTierColor(achievement.type.tier)
    val reducedMotion = isReducedMotionEnabled()

    // Scale animation for icon (disabled with reduced motion)
    val scale by if (reducedMotion) {
        remember { mutableStateOf(1f) }
    } else {
        rememberInfiniteTransition(label = "celebrationScale").animateFloat(
            initialValue = 0.8f,
            targetValue = 1.2f,
            animationSpec =
                infiniteRepeatable(
                    animation = tween(1000, easing = EaseInOut),
                    repeatMode = RepeatMode.Reverse,
                ),
            label = "scale",
        )
    }

    // Rotation for confetti effect (disabled with reduced motion)
    val rotation by if (reducedMotion) {
        remember { mutableStateOf(0f) }
    } else {
        rememberInfiniteTransition(label = "celebrationRotation").animateFloat(
            initialValue = -10f,
            targetValue = 10f,
            animationSpec =
                infiniteRepeatable(
                    animation = tween(500, easing = EaseInOut),
                    repeatMode = RepeatMode.Reverse,
                ),
            label = "rotation",
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "🎉 Achievement Unlocked! 🎉",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // Animated icon
                Box(
                    modifier =
                        Modifier
                            .size(120.dp)
                            .scale(scale)
                            .clip(CircleShape)
                            .background(tierColor.copy(alpha = 0.2f))
                            .border(
                                width = 4.dp,
                                color = tierColor,
                                shape = CircleShape,
                            ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = achievement.type.icon,
                        contentDescription = null,
                        tint = tierColor,
                        modifier = Modifier.size(60.dp),
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = achievement.type.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = achievement.type.description,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Tier badge
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = tierColor.copy(alpha = 0.2f),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            imageVector = achievement.type.icon,
                            contentDescription = null,
                            tint = tierColor,
                            modifier = Modifier.size(20.dp),
                        )
                        Text(
                            text = "${achievement.type.tier.displayName} Achievement",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = tierColor,
                        )
                    }
                }
            }
        },
        confirmButton = {
            FilledTonalButton(
                onClick = onDismiss,
                colors =
                    ButtonDefaults.filledTonalButtonColors(
                        containerColor = honeyColor,
                        contentColor = glowColors.ink900,
                    ),
            ) {
                Text("Awesome!")
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(24.dp),
    )
}

/**
 * Get color for achievement tier
 */
@Composable
private fun getTierColor(tier: AchievementTier): Color = Color(tier.color)
