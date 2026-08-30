package com.glowup.ai.core.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Shimmer Loading States
 * Provides better perceived performance than spinners
 * Following best practices from Material Design and iOS HIG
 */

/**
 * Shimmer effect composable
 * Creates animated gradient that sweeps across
 */
@Composable
fun ShimmerEffect(
    modifier: Modifier = Modifier,
    content: @Composable (Brush) -> Unit
) {
    val shimmerColors = listOf(
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
    )

    val transition = rememberInfiniteTransition()
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 1200,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Restart
        )
    )

    val brush = Brush.linearGradient(
        colors = shimmerColors,
        start = Offset(translateAnim - 1000f, translateAnim - 1000f),
        end = Offset(translateAnim, translateAnim)
    )

    content(brush)
}

/**
 * Shimmer skeleton for text lines
 */
@Composable
fun TextShimmer(
    modifier: Modifier = Modifier,
    lines: Int = 3,
    lineHeight: Dp = 16.dp
) {
    ShimmerEffect { brush ->
        Column(
            modifier = modifier,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            repeat(lines) { index ->
                val width = when (index) {
                    lines - 1 -> 0.7f // Last line shorter
                    else -> 1f
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth(width)
                        .height(lineHeight)
                        .clip(RoundedCornerShape(4.dp))
                        .background(brush)
                )
            }
        }
    }
}

/**
 * Shimmer skeleton for cards
 */
@Composable
fun CardShimmer(
    modifier: Modifier = Modifier,
    height: Dp = 120.dp
) {
    ShimmerEffect { brush ->
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(height)
                .clip(RoundedCornerShape(16.dp))
                .background(brush)
        )
    }
}

/**
 * Shimmer skeleton for circular avatar/image
 */
@Composable
fun CircleShimmer(
    modifier: Modifier = Modifier,
    size: Dp = 48.dp
) {
    ShimmerEffect { brush ->
        Box(
            modifier = modifier
                .size(size)
                .clip(CircleShape)
                .background(brush)
        )
    }
}

/**
 * Shimmer skeleton for photo/image
 */
@Composable
fun PhotoShimmer(
    modifier: Modifier = Modifier,
    aspectRatio: Float = 0.75f
) {
    ShimmerEffect { brush ->
        Box(
            modifier = modifier
                .fillMaxWidth()
                .aspectRatio(aspectRatio)
                .clip(RoundedCornerShape(12.dp))
                .background(brush)
        )
    }
}

/**
 * Shimmer skeleton for dashboard stats
 */
@Composable
fun DashboardShimmer(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Stats cards
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CardShimmer(modifier = Modifier.weight(1f), height = 100.dp)
            CardShimmer(modifier = Modifier.weight(1f), height = 100.dp)
        }

        // Main content card
        CardShimmer(height = 200.dp)

        // List items
        repeat(3) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CircleShimmer(size = 48.dp)
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextShimmer(lines = 2)
                }
            }
        }
    }
}

/**
 * Shimmer skeleton for home screen
 */
@Composable
fun HomeScreenShimmer(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Streak counter
        CardShimmer(height = 140.dp)

        // Calendar heatmap
        CardShimmer(height = 220.dp)

        // Stats section
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CardShimmer(modifier = Modifier.weight(1f), height = 100.dp)
            CardShimmer(modifier = Modifier.weight(1f), height = 100.dp)
        }

        // Recent captures
        TextShimmer(lines = 1)
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            PhotoShimmer(modifier = Modifier.weight(1f))
            PhotoShimmer(modifier = Modifier.weight(1f))
            PhotoShimmer(modifier = Modifier.weight(1f))
        }
    }
}

/**
 * Shimmer skeleton for capture history
 */
@Composable
fun CaptureHistoryShimmer(
    modifier: Modifier = Modifier,
    itemCount: Int = 5
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        repeat(itemCount) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                PhotoShimmer(
                    modifier = Modifier.width(80.dp),
                    aspectRatio = 1f
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextShimmer(lines = 2)
                }
            }
        }
    }
}

/**
 * Shimmer skeleton for product list
 */
@Composable
fun ProductListShimmer(
    modifier: Modifier = Modifier,
    itemCount: Int = 4
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        repeat(itemCount) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                CircleShimmer(size = 56.dp)
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextShimmer(lines = 2)
                }
            }
        }
    }
}

/**
 * Full-screen loading shimmer
 */
@Composable
fun FullScreenShimmer(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(20.dp)
    ) {
        DashboardShimmer()
    }
}
