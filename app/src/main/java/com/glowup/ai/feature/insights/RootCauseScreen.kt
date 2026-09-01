package com.glowup.ai.feature.insights

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.glowup.ai.core.design.GlowSpacing
import com.glowup.ai.core.design.LocalGlowColors
import com.glowup.ai.core.ui.EmptyState
import com.glowup.ai.core.ui.ErrorState
import com.glowup.ai.core.ui.GlowCard
import com.glowup.ai.core.ui.GlowTopBar
import com.glowup.ai.core.ui.LockedCard
import com.glowup.ai.core.ui.ShimmerSkeleton
import com.glowup.ai.domain.model.PrimaryMetric
import com.glowup.ai.domain.model.RootCauseInsight

@Composable
fun RootCauseScreen(
    onBack: () -> Unit,
    onUpgrade: () -> Unit,
    onLogContext: () -> Unit,
    viewModel: RootCauseViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val metric by viewModel.metric.collectAsState()

    Scaffold(topBar = { GlowTopBar(title = "Root-cause correlations", onBack = onBack) }) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (val current = state) {
                ScreenState.Loading -> {
                    Column(modifier = Modifier.padding(GlowSpacing.md)) {
                        ShimmerSkeleton(height = 48.dp)
                        ShimmerSkeleton(height = 96.dp, modifier = Modifier.padding(top = GlowSpacing.sm))
                    }
                }

                ScreenState.Locked -> {
                    Box(modifier = Modifier.padding(GlowSpacing.md)) {
                        LockedCard(
                            title = "Root-cause search is Premium",
                            body = "See which logged context (sleep, travel, stress…) correlates with your metric changes.",
                            onUnlock = onUpgrade,
                        )
                    }
                }

                is ScreenState.Error -> {
                    Box(modifier = Modifier.padding(GlowSpacing.md)) {
                        ErrorState(message = current.message, onRetry = viewModel::load)
                    }
                }

                is ScreenState.Empty -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        MetricSelector(metric, viewModel::onMetricChange)
                        EmptyState(
                            modifier = Modifier.padding(GlowSpacing.md),
                            title = current.title,
                            body = current.body,
                            ctaLabel = "Log a context event",
                            onCtaClick = onLogContext,
                        )
                    }
                }

                is ScreenState.Content -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        MetricSelector(metric, viewModel::onMetricChange)
                        LazyColumn(
                            contentPadding = PaddingValues(GlowSpacing.md),
                            verticalArrangement = Arrangement.spacedBy(GlowSpacing.sm),
                        ) {
                            items(current.value, key = { "${it.eventType}-${it.metric}" }) { insight ->
                                RootCauseCard(insight)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricSelector(
    selected: PrimaryMetric,
    onSelect: (PrimaryMetric) -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = GlowSpacing.md, vertical = GlowSpacing.sm)
                .horizontalScroll(androidx.compose.foundation.rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(GlowSpacing.xs),
    ) {
        listOf(
            PrimaryMetric.TEXTURE_SCORE,
            PrimaryMetric.REDNESS_SCORE,
            PrimaryMetric.BLEMISH_COUNT,
            PrimaryMetric.DARKSPOT_AREA,
        ).forEach { metric ->
            FilterChip(
                modifier = Modifier.defaultMinSize(minHeight = 48.dp),
                selected = selected == metric,
                onClick = { onSelect(metric) },
                label = { Text(metric.toWire().replace('_', ' ')) },
            )
        }
    }
}

@Composable
private fun RootCauseCard(insight: RootCauseInsight) {
    val glow = LocalGlowColors.current
    GlowCard {
        Text(
            text =
                insight.eventType.name
                    .lowercase()
                    .replaceFirstChar { it.uppercase() },
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = glow.ink900,
        )
        Text(
            text = "${insight.occurrences} occurrences · effect ${"%.2f".format(insight.normalizedEffect)}",
            style = MaterialTheme.typography.labelMedium,
            color = glow.ink600,
        )
        // Rendered verbatim — this already contains the correlation-not-causation caveat and
        // must never be paraphrased (ANDROID_PLAN.md §3.5 / frontend-api-map.md "Ideal UI state").
        Text(
            text = insight.message,
            style = MaterialTheme.typography.bodyMedium,
            color = glow.ink900,
            modifier = Modifier.padding(top = GlowSpacing.xs),
        )
    }
}
