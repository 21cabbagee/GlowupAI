package com.glowup.ai.feature.analytics.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.glowup.ai.core.design.GlowSpacing
import com.glowup.ai.core.design.LocalGlowColors

enum class TrendDirection {
    UP,
    DOWN,
    STABLE,
}

data class MetricTrend(
    val direction: TrendDirection,
    val changePercent: Double,
    val description: String,
)

/**
 * Primary metric card with prominent number display, chart integration, and trend indicator.
 * Numbers use monospace fonts for professionalism and scannability.
 */
@Composable
fun PrimaryMetricCard(
    title: String,
    value: String,
    trend: MetricTrend?,
    modifier: Modifier = Modifier,
    chartContent: @Composable () -> Unit,
) {
    val glow = LocalGlowColors.current

    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .background(glow.surfaceCard, RoundedCornerShape(16.dp))
                .padding(GlowSpacing.lg),
    ) {
        // Title
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = glow.ink600,
            modifier = Modifier.padding(bottom = GlowSpacing.sm),
        )

        // Big number - monospace for trustworthiness
        Text(
            text = value,
            fontFamily = FontFamily.Monospace,
            fontSize = 48.sp,
            fontWeight = FontWeight.ExtraBold,
            color = glow.ink900,
            modifier = Modifier.padding(bottom = GlowSpacing.xs),
        )

        // Trend indicator
        trend?.let { trendData ->
            TrendIndicator(trend = trendData)
        }

        Spacer(modifier = Modifier.height(GlowSpacing.md))

        // Chart
        chartContent()
    }
}

/**
 * Secondary metric card - smaller, grid-friendly format.
 * Still uses monospace for numbers but more compact.
 */
@Composable
fun SecondaryMetricCard(
    title: String,
    value: String,
    trend: MetricTrend?,
    modifier: Modifier = Modifier,
    color: Color? = null,
) {
    val glow = LocalGlowColors.current
    val cardColor = color ?: glow.surfaceCard

    Column(
        modifier =
            modifier
                .background(cardColor, RoundedCornerShape(12.dp))
                .padding(GlowSpacing.md),
    ) {
        // Title
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = glow.ink600,
            modifier = Modifier.padding(bottom = GlowSpacing.xs),
        )

        // Value - monospace
        Text(
            text = value,
            fontFamily = FontFamily.Monospace,
            fontSize = 28.sp,
            fontWeight = FontWeight.ExtraBold,
            color = glow.ink900,
            lineHeight = 32.sp,
            modifier = Modifier.padding(bottom = GlowSpacing.xs),
        )

        // Trend - compact version
        trend?.let { trendData ->
            CompactTrendIndicator(trend = trendData)
        }
    }
}

@Composable
private fun TrendIndicator(
    trend: MetricTrend,
    modifier: Modifier = Modifier,
) {
    val glow = LocalGlowColors.current

    val (icon, color) =
        when (trend.direction) {
            TrendDirection.UP -> Icons.Filled.TrendingUp to glow.success
            TrendDirection.DOWN -> Icons.Filled.TrendingDown to glow.danger
            TrendDirection.STABLE -> Icons.Filled.TrendingFlat to glow.ink600
        }

    Row(
        modifier =
            modifier
                .background(color.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
                .padding(horizontal = GlowSpacing.sm, vertical = GlowSpacing.xs),
        horizontalArrangement = Arrangement.spacedBy(GlowSpacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(20.dp),
        )

        Text(
            text = "${if (trend.changePercent > 0) "+" else ""}${String.format("%.1f", trend.changePercent)}%",
            fontFamily = FontFamily.Monospace,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = color,
        )

        Text(
            text = trend.description,
            style = MaterialTheme.typography.bodyMedium,
            color = glow.ink900,
        )
    }
}

@Composable
private fun CompactTrendIndicator(
    trend: MetricTrend,
    modifier: Modifier = Modifier,
) {
    val glow = LocalGlowColors.current

    val (icon, color) =
        when (trend.direction) {
            TrendDirection.UP -> "↑" to glow.success
            TrendDirection.DOWN -> "↓" to glow.danger
            TrendDirection.STABLE -> "→" to glow.ink600
        }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = icon,
            fontSize = 16.sp,
            color = color,
            fontWeight = FontWeight.Bold,
        )

        Text(
            text = "${if (trend.changePercent > 0) "+" else ""}${String.format("%.1f", trend.changePercent)}%",
            fontFamily = FontFamily.Monospace,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            color = color,
        )
    }
}

/**
 * Grid layout for secondary metrics - 2 columns on mobile.
 */
@Composable
fun MetricGrid(
    metrics: List<MetricGridItem>,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(GlowSpacing.sm),
    ) {
        metrics.chunked(2).forEach { rowMetrics ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(GlowSpacing.sm),
            ) {
                rowMetrics.forEach { metric ->
                    SecondaryMetricCard(
                        title = metric.title,
                        value = metric.value,
                        trend = metric.trend,
                        modifier = Modifier.weight(1f),
                    )
                }

                // Fill remaining space if odd number of items in last row
                if (rowMetrics.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

data class MetricGridItem(
    val title: String,
    val value: String,
    val trend: MetricTrend?,
)
