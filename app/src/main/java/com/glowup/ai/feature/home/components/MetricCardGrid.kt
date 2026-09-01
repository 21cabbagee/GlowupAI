package com.glowup.ai.feature.home.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import com.glowup.ai.core.design.GlowSpacing
import com.glowup.ai.core.design.LocalGlowColors
import com.glowup.ai.core.ui.StatDelta
import com.glowup.ai.core.ui.StatDeltaDirection
import com.glowup.ai.core.ui.StatTile
import com.glowup.ai.domain.model.HistoryItem
import com.glowup.ai.domain.model.PrimaryMetric
import com.glowup.ai.feature.home.formatMetricValue
import com.glowup.ai.feature.home.higherIsBetter

/**
 * Grid of metric cards showing current skin health metrics with trend indicators.
 * Uses 3-column layout on larger screens, 2-column on smaller screens.
 * Implements Peak-End Rule: Peak = metrics improving (green indicators).
 */
@Composable
fun MetricCardGrid(
    latest: HistoryItem?,
    previous: HistoryItem?,
    modifier: Modifier = Modifier,
) {
    val glowColors = LocalGlowColors.current

    // Staggered animation for visual interest
    var visible by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.95f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "metricScale"
    )

    LaunchedEffect(Unit) {
        visible = true
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(GlowSpacing.sm)
    ) {
        Text(
            text = "Your Metrics",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = glowColors.ink900,
            modifier = Modifier.padding(bottom = GlowSpacing.xs)
        )

        // Row 1: Redness, Blemishes, Dark Spots
        Row(
            modifier = Modifier.fillMaxWidth().scale(scale),
            horizontalArrangement = Arrangement.spacedBy(GlowSpacing.sm)
        ) {
            MetricTileWithAnimation(
                modifier = Modifier.weight(1f),
                metric = PrimaryMetric.REDNESS_SCORE,
                latest = latest,
                previous = previous
            )
            MetricTileWithAnimation(
                modifier = Modifier.weight(1f),
                metric = PrimaryMetric.BLEMISH_COUNT,
                latest = latest,
                previous = previous
            )
            MetricTileWithAnimation(
                modifier = Modifier.weight(1f),
                metric = PrimaryMetric.DARKSPOT_AREA,
                latest = latest,
                previous = previous
            )
        }

        // Row 2: Texture (full width for prominence)
        MetricTileWithAnimation(
            modifier = Modifier.fillMaxWidth().scale(scale),
            metric = PrimaryMetric.TEXTURE_SCORE,
            latest = latest,
            previous = previous
        )
    }
}

@Composable
private fun MetricTileWithAnimation(
    modifier: Modifier,
    metric: PrimaryMetric,
    latest: HistoryItem?,
    previous: HistoryItem?,
) {
    val value = latest?.let { valueOf(it, metric) }
    val previousValue = previous?.let { valueOf(it, metric) }
    val delta = if (value != null && previousValue != null) {
        val diff = value - previousValue
        if (kotlin.math.abs(diff) < 1e-9) {
            StatDelta("→ Stable", StatDeltaDirection.Flat)
        } else {
            val improved = if (metric.higherIsBetter()) diff > 0 else diff < 0
            val symbol = if (improved) "▼" else "▲"
            val text = "${formatMetricValue(metric, kotlin.math.abs(diff))}"
            StatDelta(
                text = "$symbol $text",
                direction = if (improved) StatDeltaDirection.Down else StatDeltaDirection.Up,
            )
        }
    } else null

    StatTile(
        modifier = modifier,
        label = metricLabel(metric),
        value = value?.let { formatMetricValue(metric, it) } ?: "—",
        delta = delta,
    )
}

private fun valueOf(item: HistoryItem, metric: PrimaryMetric): Double? = when (metric) {
    PrimaryMetric.REDNESS_SCORE -> item.rednessScore
    PrimaryMetric.BLEMISH_COUNT -> item.blemishCount
    PrimaryMetric.DARKSPOT_AREA -> item.darkspotArea
    PrimaryMetric.TEXTURE_SCORE -> item.textureScore
    PrimaryMetric.UNKNOWN -> null
}

private fun metricLabel(metric: PrimaryMetric): String = when (metric) {
    PrimaryMetric.REDNESS_SCORE -> "Redness"
    PrimaryMetric.BLEMISH_COUNT -> "Blemishes"
    PrimaryMetric.DARKSPOT_AREA -> "Dark Spots"
    PrimaryMetric.TEXTURE_SCORE -> "Texture"
    PrimaryMetric.UNKNOWN -> "Metric"
}
