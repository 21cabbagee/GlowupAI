package com.glowup.ai.core.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.glowup.ai.core.design.GlowShapes
import com.glowup.ai.core.design.GlowSpacing
import com.glowup.ai.core.design.LocalGlowColors
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Data point for trend chart
 */
data class TrendDataPoint(
    val timestamp: String, // ISO timestamp
    val value: Float,
    val label: String? = null,
)

/**
 * Trend chart component that renders the measured values locally.
 */
@Composable
fun TrendChart(
    title: String,
    dataPoints: List<TrendDataPoint>,
    metricLabel: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    showTrend: Boolean = true,
) {
    val glowColors = LocalGlowColors.current

    // Calculate trend
    val trend =
        if (dataPoints.size >= 2) {
            val first = dataPoints.first().value
            val last = dataPoints.last().value
            val denominator = kotlin.math.abs(first).coerceAtLeast(1f)
            val change = ((last - first) / denominator * 100)
            TrendInfo(
                percentage = change,
                direction =
                    when {
                        change > 0 -> TrendDirection.UP
                        change < 0 -> TrendDirection.DOWN
                        else -> TrendDirection.STABLE
                    },
            )
        } else {
            null
        }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = GlowShapes.md,
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface,
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(GlowSpacing.md),
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = glowColors.ink900,
                    )
                    if (subtitle != null) {
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = glowColors.ink600,
                        )
                    }
                }

                // Trend indicator
                if (showTrend && trend != null) {
                    TrendIndicator(
                        trend = trend,
                        metric = metricLabel,
                    )
                }
            }

            if (dataPoints.isNotEmpty()) {
                TrendPlot(
                    dataPoints = dataPoints,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .padding(top = GlowSpacing.sm),
                )
            } else {
                EmptyState(
                    title = "No data yet",
                    body = "Capture more photos to see trends",
                    ctaLabel = "View All Photos",
                    onCtaClick = {},
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                )
            }
        }
    }
}

@Composable
private fun TrendPlot(
    dataPoints: List<TrendDataPoint>,
    modifier: Modifier = Modifier,
) {
    val colors = LocalGlowColors.current
    val values = dataPoints.map { it.value }
    val minimum = values.minOrNull() ?: 0f
    val maximum = values.maxOrNull() ?: minimum
    val range = (maximum - minimum).coerceAtLeast(1f)
    val lineColor = colors.honey700
    val guideColor = colors.ink600.copy(alpha = 0.18f)

    Column(modifier = modifier) {
        Canvas(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .weight(1f),
        ) {
            val horizontalPadding = 10.dp.toPx()
            val verticalPadding = 12.dp.toPx()
            val usableWidth = (size.width - horizontalPadding * 2).coerceAtLeast(1f)
            val usableHeight = (size.height - verticalPadding * 2).coerceAtLeast(1f)
            repeat(3) { guide ->
                val y = verticalPadding + usableHeight * guide / 2f
                drawLine(
                    color = guideColor,
                    start = Offset(horizontalPadding, y),
                    end = Offset(size.width - horizontalPadding, y),
                    strokeWidth = 1.dp.toPx(),
                )
            }
            val points =
                dataPoints.mapIndexed { index, point ->
                    val fractionX =
                        if (dataPoints.size == 1) 0.5f else index.toFloat() / (dataPoints.size - 1)
                    val fractionY = (point.value - minimum) / range
                    Offset(
                        x = horizontalPadding + usableWidth * fractionX,
                        y = verticalPadding + usableHeight * (1f - fractionY),
                    )
                }
            points.zipWithNext().forEach { (start, end) ->
                drawLine(
                    color = lineColor,
                    start = start,
                    end = end,
                    strokeWidth = 3.dp.toPx(),
                    cap = StrokeCap.Round,
                )
            }
            points.forEach { point ->
                drawCircle(color = lineColor, radius = 4.dp.toPx(), center = point)
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = dataPoints.first().displayDate(),
                style = MaterialTheme.typography.labelSmall,
                color = colors.ink600,
            )
            if (dataPoints.size > 1) {
                Text(
                    text = dataPoints.last().displayDate(),
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.ink600,
                )
            }
        }
    }
}

private fun TrendDataPoint.displayDate(): String =
    label?.takeIf { it.isNotBlank() }
        ?: runCatching {
            DateTimeFormatter.ofPattern("MMM d")
                .withZone(ZoneId.systemDefault())
                .format(Instant.parse(timestamp))
        }.getOrDefault(timestamp.take(10))

/**
 * Trend information for a metric
 */
data class TrendInfo(
    val percentage: Float,
    val direction: TrendDirection,
)

enum class TrendDirection {
    UP,
    DOWN,
    STABLE,
}

/**
 * Compact trend indicator showing percentage change
 */
@Composable
private fun TrendIndicator(
    trend: TrendInfo,
    metric: String,
    modifier: Modifier = Modifier,
) {
    val glowColors = LocalGlowColors.current
    val reducedMotion = isReducedMotionEnabled()

    val color =
        when (trend.direction) {
            TrendDirection.UP -> glowColors.honey700
            TrendDirection.DOWN -> glowColors.success
            TrendDirection.STABLE -> glowColors.ink600
        }
    val arrow =
        when (trend.direction) {
            TrendDirection.UP -> "↑"
            TrendDirection.DOWN -> "↓"
            TrendDirection.STABLE -> "→"
        }

    // Animate appearance
    val alpha by animateFloatAsState(
        targetValue = 1f,
        animationSpec = if (reducedMotion) tween(0) else tween(durationMillis = 400, easing = GlowEasing),
        label = "trendIndicatorAlpha",
    )

    val scale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = if (reducedMotion) tween(0) else tween(durationMillis = 400, easing = GlowEasing),
        label = "trendIndicatorScale",
    )

    Column(
        horizontalAlignment = Alignment.End,
        modifier =
            modifier
                .alpha(alpha)
                .scale(scale),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(GlowSpacing.xs),
        ) {
            Text(
                text = arrow,
                style = MaterialTheme.typography.titleLarge,
                color = color,
            )
            Text(
                text = "${String.format("%.1f", kotlin.math.abs(trend.percentage))}%",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = color,
            )
        }
        Text(
            text = metric,
            style = MaterialTheme.typography.labelSmall,
            color = glowColors.ink600,
        )
    }
}
