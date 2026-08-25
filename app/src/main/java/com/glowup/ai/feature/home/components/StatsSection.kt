package com.glowup.ai.feature.home.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.glowup.ai.core.ui.StatDelta
import com.glowup.ai.core.ui.StatDeltaDirection
import com.glowup.ai.core.ui.StatTile
import com.glowup.ai.core.ui.StreakRing
import com.glowup.ai.domain.model.Engagement
import com.glowup.ai.domain.model.HistoryItem
import com.glowup.ai.domain.model.PrimaryMetric
import com.glowup.ai.feature.home.formatMetricValue
import com.glowup.ai.feature.home.higherIsBetter

/**
 * The streak ring + real-metric stat tiles for Home. Every value here comes from the backend —
 * ANDROID_PLAN.md's headline complaint about the old app is `val streak = 7 // Mock streak data`
 * plus 5 of 6 hardcoded metric tiles; nothing in this file may fall back to a literal number.
 */
@Composable
fun HomeStatsSection(
    modifier: Modifier = Modifier,
    engagement: Engagement?,
    latest: HistoryItem?,
    previous: HistoryItem?,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            StreakRing(streak = engagement?.captureStreak ?: 0, target = 7)
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                StatTile(
                    label = "Captures",
                    value = (engagement?.captureCount ?: 0).toString(),
                    accent = true,
                )
                StatTile(
                    label = "Confidence",
                    value = latest?.confidenceLabel ?: "Not enough data yet",
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            MetricTile(Modifier.weight(1f), PrimaryMetric.REDNESS_SCORE, latest, previous)
            MetricTile(Modifier.weight(1f), PrimaryMetric.BLEMISH_COUNT, latest, previous)
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            MetricTile(Modifier.weight(1f), PrimaryMetric.DARKSPOT_AREA, latest, previous)
            MetricTile(Modifier.weight(1f), PrimaryMetric.TEXTURE_SCORE, latest, previous)
        }
    }
}

@Composable
private fun RowScope.MetricTile(
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
            StatDelta("No change vs previous", StatDeltaDirection.Flat)
        } else {
            val improved = if (metric.higherIsBetter()) diff > 0 else diff < 0
            StatDelta(
                text = "${if (diff > 0) "+" else ""}${formatMetricValue(metric, diff)} vs previous",
                direction = if (improved) StatDeltaDirection.Up else StatDeltaDirection.Down,
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
    PrimaryMetric.DARKSPOT_AREA -> "Dark spots"
    PrimaryMetric.TEXTURE_SCORE -> "Texture"
    PrimaryMetric.UNKNOWN -> "Metric"
}
