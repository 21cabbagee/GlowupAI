package com.glowup.ai.core.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.glowup.ai.core.design.GlowSpacing
import com.glowup.ai.core.design.LocalGlowColors
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Milestone celebration dialog with confetti animation
 * Shown when users reach streak milestones (7, 14, 30, 60, 90 days)
 */
@Composable
fun MilestoneDialog(
    milestone: Int,
    message: String,
    onDismiss: () -> Unit,
    onShare: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val glowColors = LocalGlowColors.current
    val reducedMotion = isReducedMotionEnabled()

    // Scale animation for icon (respecting reduced motion)
    val scale by if (reducedMotion) {
        remember { mutableStateOf(1f) }
    } else {
        rememberInfiniteTransition(label = "milestoneScale").animateFloat(
            initialValue = 0.9f,
            targetValue = 1.1f,
            animationSpec = infiniteRepeatable(
                animation = tween(1000, easing = EaseInOut),
                repeatMode = RepeatMode.Reverse
            ),
            label = "scale"
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier,
        title = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Confetti canvas (only if not reduced motion)
                if (!reducedMotion) {
                    ConfettiCanvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                    )
                }

                Text(
                    text = "🎉 Milestone Reached! 🎉",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(GlowSpacing.md)
            ) {
                // Milestone badge
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .scale(scale)
                        .background(
                            color = glowColors.honey500.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(60.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "$milestone",
                        style = MaterialTheme.typography.displayLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = glowColors.honey700
                    )
                }

                Text(
                    text = "Day Streak!",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Progress to next milestone
                val nextMilestone = getNextMilestone(milestone)
                if (nextMilestone != null) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Next milestone: $nextMilestone days",
                            style = MaterialTheme.typography.labelMedium,
                            color = glowColors.ink600
                        )
                        Text(
                            text = "${nextMilestone - milestone} days to go!",
                            style = MaterialTheme.typography.labelSmall,
                            color = glowColors.ink600
                        )
                    }
                }
            }
        },
        confirmButton = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (onShare != null) {
                    OutlinedButton(onClick = onShare) {
                        Text("Share")
                    }
                }
                FilledTonalButton(
                    onClick = onDismiss,
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = glowColors.honey500,
                        contentColor = glowColors.ink900
                    )
                ) {
                    Text("Continue")
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(24.dp)
    )
}

/**
 * Get next milestone after current streak
 */
private fun getNextMilestone(current: Int): Int? {
    val milestones = listOf(3, 7, 14, 30, 60, 90, 180, 365)
    return milestones.firstOrNull { it > current }
}

/**
 * Animated confetti canvas
 */
@Composable
private fun ConfettiCanvas(
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "confetti")

    val animationProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "confettiProgress"
    )

    // Generate random confetti pieces
    val confettiPieces = remember {
        List(30) {
            ConfettiPiece(
                x = Random.nextFloat(),
                y = Random.nextFloat() * -0.2f,
                color = listOf(
                    Color(0xFFFFD700), // Gold
                    Color(0xFFFFA500), // Orange
                    Color(0xFFFF69B4), // Pink
                    Color(0xFF87CEEB), // Sky blue
                    Color(0xFF90EE90)  // Light green
                ).random(),
                rotation = Random.nextFloat() * 360f,
                size = Random.nextFloat() * 8f + 4f
            )
        }
    }

    Canvas(modifier = modifier) {
        confettiPieces.forEach { piece ->
            val yOffset = (animationProgress * size.height * 1.5f + piece.y * size.height)
            val xOffset = piece.x * size.width +
                sin(animationProgress * 4f + piece.x * 10f) * 20f

            if (yOffset < size.height) {
                val angle = (animationProgress * 360f + piece.rotation) % 360f
                val path = Path().apply {
                    val centerX = xOffset
                    val centerY = yOffset
                    val radius = piece.size

                    // Draw a star shape
                    for (i in 0 until 5) {
                        val outerAngle = Math.toRadians((angle + i * 72).toDouble())
                        val innerAngle = Math.toRadians((angle + i * 72 + 36).toDouble())

                        val outerX = centerX + (radius * cos(outerAngle)).toFloat()
                        val outerY = centerY + (radius * sin(outerAngle)).toFloat()
                        val innerX = centerX + (radius * 0.5f * cos(innerAngle)).toFloat()
                        val innerY = centerY + (radius * 0.5f * sin(innerAngle)).toFloat()

                        if (i == 0) {
                            moveTo(outerX, outerY)
                        } else {
                            lineTo(outerX, outerY)
                        }
                        lineTo(innerX, innerY)
                    }
                    close()
                }

                drawPath(
                    path = path,
                    color = piece.color,
                    style = Stroke(width = 2f)
                )
            }
        }
    }
}

/**
 * Data class for confetti piece
 */
private data class ConfettiPiece(
    val x: Float,
    val y: Float,
    val color: Color,
    val rotation: Float,
    val size: Float
)

/**
 * Progress card showing days to next milestone
 * Used on Home screen
 */
@Composable
fun MilestoneProgressCard(
    currentStreak: Int,
    nextMilestone: Int,
    modifier: Modifier = Modifier
) {
    val glowColors = LocalGlowColors.current
    val progress = currentStreak.toFloat() / nextMilestone.toFloat()
    val daysRemaining = nextMilestone - currentStreak

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = glowColors.honey500.copy(alpha = 0.1f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(GlowSpacing.md)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Next milestone",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = glowColors.ink900
                )
                Text(
                    text = "$daysRemaining days to go",
                    style = MaterialTheme.typography.bodySmall,
                    color = glowColors.ink600
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            LinearProgressIndicator(
                progress = { progress.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp),
                color = glowColors.honey600,
                trackColor = glowColors.ink300,
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "$currentStreak / $nextMilestone days",
                style = MaterialTheme.typography.labelSmall,
                color = glowColors.ink600
            )
        }
    }
}
