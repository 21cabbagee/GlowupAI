package com.glowup.ai.core.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.glowup.ai.core.design.GlowShapes
import com.glowup.ai.core.design.GlowSpacing
import com.glowup.ai.core.design.LocalGlowColors
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberBottomAxis
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberStartAxis
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoZoomState
import com.patrykandpatrick.vico.compose.common.component.rememberTextComponent
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.lineSeries
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
 * Trend Chart Component using Vico
 * Displays metric trends over time with interactive data points
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
    val chartColor = glowColors.honey500

    // Convert data points to chart data
    val modelProducer =
        remember(dataPoints) {
            CartesianChartModelProducer.build {
                lineSeries {
                    series(dataPoints.map { it.value.toDouble() })
                }
            }
        }

    // Calculate trend
    val trend =
        remember(dataPoints) {
            if (dataPoints.size < 2) {
                null
            } else {
                val first = dataPoints.first().value
                val last = dataPoints.last().value
                val change = ((last - first) / first * 100)
                TrendInfo(
                    percentage = change,
                    direction =
                        when {
                            change > 0 -> TrendDirection.UP
                            change < 0 -> TrendDirection.DOWN
                            else -> TrendDirection.STABLE
                        },
                )
            }
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

            // Chart
            if (dataPoints.isNotEmpty()) {
                CartesianChartHost(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .padding(top = GlowSpacing.sm),
                    chart =
                        rememberCartesianChart(
                            rememberLineCartesianLayer(),
                            startAxis =
                                rememberStartAxis(
                                    label =
                                        rememberTextComponent(
                                            color = glowColors.ink600,
                                            textSize = 12.sp,
                                        ),
                                ),
                            bottomAxis =
                                rememberBottomAxis(
                                    label =
                                        rememberTextComponent(
                                            color = glowColors.ink600,
                                            textSize = 12.sp,
                                        ),
                                    valueFormatter = { value, _, _ ->
                                        // Format date labels
                                        try {
                                            val index = value.toInt()
                                            if (index in dataPoints.indices) {
                                                val timestamp = dataPoints[index].timestamp
                                                Instant
                                                    .parse(timestamp)
                                                    .atZone(ZoneId.systemDefault())
                                                    .format(DateTimeFormatter.ofPattern("MM/dd"))
                                            } else {
                                                ""
                                            }
                                        } catch (e: Exception) {
                                            ""
                                        }
                                    },
                                ),
                        ),
                    modelProducer = modelProducer,
                    zoomState = rememberVicoZoomState(zoomEnabled = true),
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
