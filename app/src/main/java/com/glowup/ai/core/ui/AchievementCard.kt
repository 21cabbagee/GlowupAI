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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.glowup.ai.core.design.HoneyTheme
import com.glowup.ai.domain.model.Achievement
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
    onClick: () -> Unit = {}
) {
    val honeyColor = HoneyTheme.colors.primary
    val inkColor = HoneyTheme.colors.onPrimary

    // Animate unlock state
    val scale by animateFloatAsState(
        targetValue = if (achievement.isNew) 1.1f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        )
    )

    val alpha = if (achievement.isUnlocked) 1f else 0.6f
    val tierColor = getTierColor(achievement.type.tier)

    Card(
        modifier = modifier
            .scale(scale)
            .alpha(alpha),
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (achievement.isUnlocked) 4.dp else 1.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Achievement icon with tier background
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(tierColor.copy(alpha = 0.2f))
                    .then(
                        if (achievement.isUnlocked) {
                            Modifier.border(
                                width = 3.dp,
                                color = tierColor,
                                shape = CircleShape
                            )
                        } else Modifier
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = achievement.type.icon,
                    contentDescription = achievement.type.title,
                    tint = if (achievement.isUnlocked) tierColor else Color.Gray,
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Title
            Text(
                text = achievement.type.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = if (achievement.isUnlocked) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                }
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Description
            Text(
                text = achievement.type.description,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Progress or unlock date
            if (achievement.isUnlocked) {
                // Tier badge
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = tierColor.copy(alpha = 0.2f)
                ) {
                    Text(
                        text = achievement.type.tier.displayName,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = tierColor,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }
            } else {
                // Progress bar
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    LinearProgressIndicator(
                        progress = achievement.progress,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = honeyColor,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = achievement.getProgressText(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
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
    size: Int = 40
) {
    val tierColor = getTierColor(achievement.type.tier)

    Box(
        modifier = modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(tierColor.copy(alpha = 0.2f))
            .border(
                width = 2.dp,
                color = tierColor,
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = achievement.type.icon,
            contentDescription = achievement.type.title,
            tint = tierColor,
            modifier = Modifier.size((size * 0.6).dp)
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
    modifier: Modifier = Modifier
) {
    val honeyColor = HoneyTheme.colors.primary
    val tierColor = getTierColor(achievement.type.tier)

    // Scale animation for icon
    val scale by rememberInfiniteTransition().animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        )
    )

    // Rotation for confetti effect
    val rotation by rememberInfiniteTransition().animateFloat(
        initialValue = -10f,
        targetValue = 10f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        )
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "🎉 Achievement Unlocked! 🎉",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Animated icon
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .scale(scale)
                        .clip(CircleShape)
                        .background(tierColor.copy(alpha = 0.2f))
                        .border(
                            width = 4.dp,
                            color = tierColor,
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = achievement.type.icon,
                        contentDescription = null,
                        tint = tierColor,
                        modifier = Modifier.size(60.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = achievement.type.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = achievement.type.description,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Tier badge
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = tierColor.copy(alpha = 0.2f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = achievement.type.icon,
                            contentDescription = null,
                            tint = tierColor,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "${achievement.type.tier.displayName} Achievement",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = tierColor
                        )
                    }
                }
            }
        },
        confirmButton = {
            FilledTonalButton(
                onClick = onDismiss,
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = honeyColor,
                    contentColor = HoneyTheme.colors.onPrimary
                )
            ) {
                Text("Awesome!")
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(24.dp)
    )
}

/**
 * Get color for achievement tier
 */
@Composable
private fun getTierColor(tier: AchievementTier): Color {
    return Color(tier.color)
}
