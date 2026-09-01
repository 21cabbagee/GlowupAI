package com.glowup.ai.core.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.glowup.ai.core.design.GlowShapes
import com.glowup.ai.core.design.GlowSpacing
import com.glowup.ai.core.design.GlowUpTheme
import com.glowup.ai.core.design.LocalGlowColors

/**
 * Trend direction for metric display.
 * - UP: Metric increased (can be good or bad depending on metric)
 * - DOWN: Metric decreased
 * - STABLE: No significant change
 */
enum class Trend {
    UP,
    DOWN,
    STABLE
}

/**
 * Metric card showing a single metric with trend indicator.
 * Follows the design system with 24dp rounded corners, soft elevation, and 8-point grid spacing.
 *
 * @param title The metric name (e.g., "Redness", "Texture")
 * @param value The metric value displayed in monospace font
 * @param change The change indicator text (e.g., "12%", "8 points")
 * @param trend The trend direction (UP/DOWN/STABLE)
 * @param isImprovement Whether the trend direction is positive (affects color)
 * @param isLoading Show shimmer loading state
 * @param modifier Optional modifier
 */
@Composable
fun MetricCard(
    title: String,
    value: String,
    change: String,
    trend: Trend,
    isImprovement: Boolean,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
) {
    val glow = LocalGlowColors.current

    Card(
        modifier = modifier,
        shape = GlowShapes.lg,
        colors = CardDefaults.cardColors(
            containerColor = glow.surfaceCard
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {
        if (isLoading) {
            Column(
                modifier = Modifier.padding(GlowSpacing.lg),
                verticalArrangement = Arrangement.spacedBy(GlowSpacing.sm)
            ) {
                ShimmerSkeleton(height = 14.dp, cornerRadius = 4.dp)
                ShimmerSkeleton(height = 32.dp, cornerRadius = 8.dp)
                ShimmerSkeleton(height = 14.dp, cornerRadius = 4.dp, modifier = Modifier.width(100.dp))
            }
        } else {
            Column(
                modifier = Modifier.padding(GlowSpacing.lg),
                verticalArrangement = Arrangement.spacedBy(GlowSpacing.sm)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = glow.ink600
                )

                Text(
                    text = value,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily.Monospace,
                    color = glow.ink900
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(GlowSpacing.xs)
                ) {
                    val (icon, trendColor) = when {
                        trend == Trend.STABLE -> Pair(Icons.Default.Remove, glow.ink600)
                        isImprovement -> Pair(
                            if (trend == Trend.DOWN) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                            glow.success
                        )
                        else -> Pair(
                            if (trend == Trend.UP) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                            glow.danger
                        )
                    }

                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = trendColor,
                        modifier = Modifier.size(16.dp)
                    )

                    Text(
                        text = change,
                        style = MaterialTheme.typography.bodyMedium,
                        color = glow.ink600
                    )
                }
            }
        }
    }
}

/**
 * Streak card with fire emoji, gradient background, and progress bar.
 * Shows the current streak count and progress toward the next milestone.
 *
 * @param streakCount Current streak days
 * @param nextMilestone Next milestone target (e.g., 14 for 2 weeks)
 * @param encouragementText Optional encouraging message
 * @param isLoading Show shimmer loading state
 * @param modifier Optional modifier
 */
@Composable
fun StreakCard(
    streakCount: Int,
    nextMilestone: Int,
    encouragementText: String = "Keep it up! You're building momentum.",
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
) {
    val glow = LocalGlowColors.current
    val progress by animateFloatAsState(
        targetValue = (streakCount.toFloat() / nextMilestone.toFloat()).coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 800),
        label = "streakProgress"
    )

    Card(
        modifier = modifier,
        shape = GlowShapes.lg,
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            glow.honey400.copy(alpha = 0.3f),
                            glow.honey500.copy(alpha = 0.2f)
                        )
                    )
                )
        ) {
            if (isLoading) {
                Column(
                    modifier = Modifier.padding(GlowSpacing.lg),
                    verticalArrangement = Arrangement.spacedBy(GlowSpacing.sm)
                ) {
                    ShimmerSkeleton(height = 32.dp, cornerRadius = 8.dp)
                    ShimmerSkeleton(height = 14.dp, cornerRadius = 4.dp)
                    Spacer(modifier = Modifier.height(GlowSpacing.sm))
                    ShimmerSkeleton(height = 8.dp, cornerRadius = 4.dp)
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(GlowSpacing.lg),
                    verticalArrangement = Arrangement.spacedBy(GlowSpacing.sm)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(GlowSpacing.sm)
                    ) {
                        Text(
                            text = "🔥",
                            fontSize = 32.sp
                        )
                        Text(
                            text = "$streakCount Day Streak",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = glow.ink900
                        )
                    }

                    Text(
                        text = encouragementText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = glow.ink600
                    )

                    Spacer(modifier = Modifier.height(GlowSpacing.sm))

                    Column(
                        verticalArrangement = Arrangement.spacedBy(GlowSpacing.xs)
                    ) {
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = glow.honey600,
                            trackColor = glow.honey300.copy(alpha = 0.3f),
                        )

                        Text(
                            text = "$streakCount/$nextMilestone to next milestone",
                            style = MaterialTheme.typography.labelSmall,
                            color = glow.ink600
                        )
                    }
                }
            }
        }
    }
}

/**
 * Product card showing product image, name, routine info, and effectiveness.
 * Includes a mini sparkline chart showing the product's correlation with skin metrics.
 *
 * @param productName Product name (e.g., "CeraVe Moisturizer")
 * @param productType Product type/category (e.g., "Moisturizer")
 * @param routineInfo Routine frequency (e.g., "2× daily", "AM only")
 * @param effectiveness Effectiveness verdict text (e.g., "Working well", "No clear effect")
 * @param isEffective Whether the product is showing positive results
 * @param chartData Data points for the mini effectiveness chart (0.0 to 1.0 range)
 * @param imageContent Optional composable for product image
 * @param isLoading Show shimmer loading state
 * @param modifier Optional modifier
 */
@Composable
fun ProductCard(
    productName: String,
    productType: String,
    routineInfo: String,
    effectiveness: String,
    isEffective: Boolean,
    chartData: List<Float>,
    modifier: Modifier = Modifier,
    imageContent: @Composable (() -> Unit)? = null,
    isLoading: Boolean = false,
) {
    val glow = LocalGlowColors.current

    Card(
        modifier = modifier,
        shape = GlowShapes.lg,
        colors = CardDefaults.cardColors(
            containerColor = glow.surfaceCard
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {
        if (isLoading) {
            Column(
                modifier = Modifier.padding(GlowSpacing.lg),
                verticalArrangement = Arrangement.spacedBy(GlowSpacing.md)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(GlowSpacing.md)
                ) {
                    ShimmerSkeleton(
                        height = 64.dp,
                        cornerRadius = 12.dp,
                        modifier = Modifier.width(64.dp)
                    )
                    Column(
                        verticalArrangement = Arrangement.spacedBy(GlowSpacing.xs)
                    ) {
                        ShimmerSkeleton(height = 16.dp, cornerRadius = 4.dp, modifier = Modifier.width(120.dp))
                        ShimmerSkeleton(height = 14.dp, cornerRadius = 4.dp, modifier = Modifier.width(80.dp))
                        ShimmerSkeleton(height = 14.dp, cornerRadius = 4.dp, modifier = Modifier.width(60.dp))
                    }
                }
                ShimmerSkeleton(height = 60.dp, cornerRadius = 8.dp)
                ShimmerSkeleton(height = 14.dp, cornerRadius = 4.dp, modifier = Modifier.width(100.dp))
            }
        } else {
            Column(
                modifier = Modifier.padding(GlowSpacing.lg),
                verticalArrangement = Arrangement.spacedBy(GlowSpacing.md)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(GlowSpacing.md),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Product image placeholder or actual image
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(glow.honey300.copy(alpha = 0.3f)),
                        contentAlignment = Alignment.Center
                    ) {
                        imageContent?.invoke() ?: Text(
                            text = productName.take(2).uppercase(),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = glow.ink900
                        )
                    }

                    Column(
                        verticalArrangement = Arrangement.spacedBy(GlowSpacing.xs)
                    ) {
                        Text(
                            text = productName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = glow.ink900
                        )
                        Text(
                            text = productType,
                            style = MaterialTheme.typography.bodySmall,
                            color = glow.ink600
                        )
                        Text(
                            text = routineInfo,
                            style = MaterialTheme.typography.labelSmall,
                            color = glow.ink600
                        )
                    }
                }

                // Mini effectiveness chart
                if (chartData.isNotEmpty()) {
                    MiniSparklineChart(
                        data = chartData,
                        color = if (isEffective) glow.success else glow.honey700,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp)
                    )
                }

                // Effectiveness verdict
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(GlowSpacing.xs)
                ) {
                    Text(
                        text = if (isEffective) "✓" else "?",
                        fontSize = 14.sp,
                        color = if (isEffective) glow.success else glow.ink600
                    )
                    Text(
                        text = effectiveness,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isEffective) glow.success else glow.ink600
                    )
                }
            }
        }
    }
}

/**
 * Mini sparkline chart for displaying trend data in a compact format.
 * Used internally by ProductCard to show product effectiveness over time.
 *
 * @param data List of data points (normalized 0.0 to 1.0)
 * @param color Line and fill color
 * @param modifier Optional modifier
 */
@Composable
private fun MiniSparklineChart(
    data: List<Float>,
    color: Color,
    modifier: Modifier = Modifier,
) {
    val glow = LocalGlowColors.current

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(glow.paper)
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            if (data.size < 2) return@Canvas

            val width = size.width
            val height = size.height
            val spacing = width / (data.size - 1)

            // Calculate points
            val points = data.mapIndexed { index, value ->
                val x = index * spacing
                val y = height * (1 - value.coerceIn(0f, 1f))
                Offset(x, y)
            }

            // Draw fill path
            val fillPath = Path().apply {
                moveTo(points.first().x, height)
                points.forEach { lineTo(it.x, it.y) }
                lineTo(points.last().x, height)
                close()
            }

            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        color.copy(alpha = 0.3f),
                        color.copy(alpha = 0.05f)
                    )
                )
            )

            // Draw line path
            val linePath = Path().apply {
                moveTo(points.first().x, points.first().y)
                points.drop(1).forEach { lineTo(it.x, it.y) }
            }

            drawPath(
                path = linePath,
                color = color,
                style = Stroke(
                    width = 2.5f,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )
        }
    }
}

// ============================================================================
// PREVIEW SECTION
// ============================================================================

@Preview(name = "MetricCard - Light", showBackground = true)
@Composable
private fun MetricCardPreviewLight() {
    GlowUpTheme(darkTheme = false) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            MetricCard(
                title = "Redness",
                value = "0.42",
                change = "12% improvement",
                trend = Trend.DOWN,
                isImprovement = true,
                modifier = Modifier.width(160.dp)
            )
            MetricCard(
                title = "Texture",
                value = "4.2",
                change = "8 points",
                trend = Trend.UP,
                isImprovement = false,
                modifier = Modifier.width(160.dp)
            )
            MetricCard(
                title = "Clarity",
                value = "78",
                change = "No change",
                trend = Trend.STABLE,
                isImprovement = true,
                modifier = Modifier.width(160.dp)
            )
            MetricCard(
                title = "Loading",
                value = "",
                change = "",
                trend = Trend.STABLE,
                isImprovement = true,
                isLoading = true,
                modifier = Modifier.width(160.dp)
            )
        }
    }
}

@Preview(name = "MetricCard - Dark", showBackground = true)
@Composable
private fun MetricCardPreviewDark() {
    GlowUpTheme(darkTheme = true) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            MetricCard(
                title = "Redness",
                value = "0.42",
                change = "12% improvement",
                trend = Trend.DOWN,
                isImprovement = true,
                modifier = Modifier.width(160.dp)
            )
        }
    }
}

@Preview(name = "StreakCard - Light", showBackground = true)
@Composable
private fun StreakCardPreviewLight() {
    GlowUpTheme(darkTheme = false) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            StreakCard(
                streakCount = 8,
                nextMilestone = 14,
                encouragementText = "Keep it up! You're building momentum."
            )
            StreakCard(
                streakCount = 0,
                nextMilestone = 7,
                isLoading = true
            )
        }
    }
}

@Preview(name = "StreakCard - Dark", showBackground = true)
@Composable
private fun StreakCardPreviewDark() {
    GlowUpTheme(darkTheme = true) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            StreakCard(
                streakCount = 8,
                nextMilestone = 14
            )
        }
    }
}

@Preview(name = "ProductCard - Light", showBackground = true)
@Composable
private fun ProductCardPreviewLight() {
    GlowUpTheme(darkTheme = false) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ProductCard(
                productName = "CeraVe Moisturizer",
                productType = "Moisturizer",
                routineInfo = "2× daily",
                effectiveness = "Working well",
                isEffective = true,
                chartData = listOf(0.7f, 0.6f, 0.5f, 0.4f, 0.35f, 0.3f, 0.25f, 0.2f)
            )
            ProductCard(
                productName = "Vitamin C Serum",
                productType = "Serum",
                routineInfo = "AM only",
                effectiveness = "No clear effect",
                isEffective = false,
                chartData = listOf(0.5f, 0.55f, 0.5f, 0.52f, 0.48f, 0.5f, 0.51f, 0.49f)
            )
            ProductCard(
                productName = "Loading Product",
                productType = "",
                routineInfo = "",
                effectiveness = "",
                isEffective = false,
                chartData = emptyList(),
                isLoading = true
            )
        }
    }
}

@Preview(name = "ProductCard - Dark", showBackground = true)
@Composable
private fun ProductCardPreviewDark() {
    GlowUpTheme(darkTheme = true) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            ProductCard(
                productName = "CeraVe Moisturizer",
                productType = "Moisturizer",
                routineInfo = "2× daily",
                effectiveness = "Working well",
                isEffective = true,
                chartData = listOf(0.7f, 0.6f, 0.5f, 0.4f, 0.35f, 0.3f, 0.25f, 0.2f)
            )
        }
    }
}
