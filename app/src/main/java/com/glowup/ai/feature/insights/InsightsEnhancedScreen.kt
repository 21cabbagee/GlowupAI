package com.glowup.ai.feature.insights

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.glowup.ai.core.design.GlowShapes
import com.glowup.ai.core.design.GlowSpacing
import com.glowup.ai.core.design.LocalGlowColors
import com.glowup.ai.core.ui.*
import com.glowup.ai.domain.model.PrimaryMetric
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/**
 * Enhanced Insights Screen with trend charts, summaries, and recommendations
 * Shows metric trends over time with actionable insights
 */
@Composable
fun InsightsEnhancedRoute(
    onNavigateBack: () -> Unit,
    onNavigateToRoutine: () -> Unit,
    onNavigateToCapture: () -> Unit,
    viewModel: InsightsEnhancedViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val selectedMetric by viewModel.selectedMetric.collectAsStateWithLifecycle()

    InsightsEnhancedScreen(
        uiState = uiState,
        selectedMetric = selectedMetric,
        onMetricSelected = viewModel::selectMetric,
        onTimeRangeSelected = viewModel::selectTimeRange,
        onNavigateBack = onNavigateBack,
        onNavigateToRoutine = onNavigateToRoutine,
        onNavigateToCapture = onNavigateToCapture,
        onRefresh = viewModel::refresh,
    )
}

@Composable
private fun InsightsEnhancedScreen(
    uiState: InsightsEnhancedUiState,
    selectedMetric: PrimaryMetric,
    onMetricSelected: (PrimaryMetric) -> Unit,
    onTimeRangeSelected: (TimeRange) -> Unit,
    onNavigateBack: () -> Unit,
    onNavigateToRoutine: () -> Unit,
    onNavigateToCapture: () -> Unit,
    onRefresh: () -> Unit,
) {
    Scaffold(
        topBar = {
            GlowTopBar(
                title = "Insights & Trends",
                onBack = onNavigateBack,
                actions = {
                    IconButton(onClick = onRefresh) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Refresh")
                    }
                },
            )
        },
    ) { padding ->
        when (uiState) {
            is InsightsEnhancedUiState.Loading -> {
                Box(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(padding),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }

            is InsightsEnhancedUiState.Error -> {
                Box(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(padding)
                            .padding(GlowSpacing.lg),
                ) {
                    ErrorState(
                        message = uiState.message,
                        onRetry = onRefresh,
                    )
                }
            }

            is InsightsEnhancedUiState.Content -> {
                InsightsContent(
                    padding = padding,
                    data = uiState.data,
                    selectedMetric = selectedMetric,
                    selectedTimeRange = uiState.selectedTimeRange,
                    onMetricSelected = onMetricSelected,
                    onTimeRangeSelected = onTimeRangeSelected,
                    onNavigateToRoutine = onNavigateToRoutine,
                    onNavigateToCapture = onNavigateToCapture,
                )
            }
        }
    }
}

@Composable
private fun InsightsContent(
    padding: PaddingValues,
    data: InsightsData,
    selectedMetric: PrimaryMetric,
    selectedTimeRange: TimeRange,
    onMetricSelected: (PrimaryMetric) -> Unit,
    onTimeRangeSelected: (TimeRange) -> Unit,
    onNavigateToRoutine: () -> Unit,
    onNavigateToCapture: () -> Unit,
) {
    val glowColors = LocalGlowColors.current

    // Check if there's any data to display
    val hasCaptures = data.metricTrends.values.any { it.isNotEmpty() }
    val hasSummaries = data.summaries.isNotEmpty()
    val hasRecommendations = data.recommendations.isNotEmpty()
    val isEmpty = !hasCaptures && !hasSummaries && !hasRecommendations

    // Show empty state if no data at all
    if (isEmpty) {
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(GlowSpacing.lg),
            contentAlignment = Alignment.Center,
        ) {
            EmptyState(
                title = "No insights yet",
                body = "Capture your first photo to start tracking your skin progress and get personalized insights.",
                ctaLabel = "Take Photo",
                onCtaClick = onNavigateToCapture,
            )
        }
        return
    }

    LazyColumn(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(padding),
        contentPadding = PaddingValues(GlowSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(GlowSpacing.lg),
    ) {
        // Metric selector
        item {
            MetricSelector(
                selectedMetric = selectedMetric,
                onMetricSelected = onMetricSelected,
            )
        }

        // Time range selector
        item {
            TimeRangeSelector(
                selectedRange = selectedTimeRange,
                onRangeSelected = onTimeRangeSelected,
            )
        }

        // Trend chart for selected metric
        item {
            val metricData = data.metricTrends[selectedMetric] ?: emptyList()
            TrendChart(
                title = selectedMetric.displayName,
                dataPoints =
                    metricData.map {
                        TrendDataPoint(
                            timestamp = it.timestamp,
                            value = it.value,
                            label = it.label,
                        )
                    },
                metricLabel = selectedMetric.displayName,
                subtitle = "Last ${selectedTimeRange.days} days",
            )
        }

        // Weekly/Monthly summaries
        item {
            SectionHeader(title = "Summary")
        }

        if (data.summaries.isEmpty()) {
            item {
                EmptyState(
                    title = "No summaries yet",
                    body = "Keep capturing photos to generate weekly and monthly summaries of your progress.",
                    ctaLabel = "Take Photo",
                    onCtaClick = onNavigateToCapture,
                )
            }
        } else {
            items(data.summaries) { summary ->
                SummaryCard(summary = summary)
            }
        }

        // Product recommendations
        item {
            SectionHeader(title = "Recommendations")
        }

        if (data.recommendations.isEmpty()) {
            item {
                EmptyState(
                    title = "No recommendations yet",
                    body = "As you track your progress, we'll provide personalized recommendations to help improve your skin.",
                    ctaLabel = "Take Photo",
                    onCtaClick = onNavigateToCapture,
                )
            }
        } else {
            items(data.recommendations) { recommendation ->
                RecommendationCard(
                    recommendation = recommendation,
                    onNavigateToRoutine = onNavigateToRoutine,
                )
            }
        }

        // Disclaimer
        item {
            DisclaimerNote(
                text =
                    "Insights are based on tracked data and general patterns. " +
                        "Always consult with a dermatologist for medical advice.",
            )
        }
    }
}

/**
 * Metric selector chips
 */
@Composable
private fun MetricSelector(
    selectedMetric: PrimaryMetric,
    onMetricSelected: (PrimaryMetric) -> Unit,
    modifier: Modifier = Modifier,
) {
    val glowColors = LocalGlowColors.current

    Column(modifier = modifier) {
        Text(
            text = "Select Metric",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = glowColors.ink900,
        )

        Spacer(modifier = Modifier.height(GlowSpacing.sm))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(GlowSpacing.sm),
        ) {
            PrimaryMetric.values().forEach { metric ->
                FilterChip(
                    selected = metric == selectedMetric,
                    onClick = { onMetricSelected(metric) },
                    label = { Text(metric.displayName) },
                )
            }
        }
    }
}

/**
 * Time range selector
 */
@Composable
private fun TimeRangeSelector(
    selectedRange: TimeRange,
    onRangeSelected: (TimeRange) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(GlowSpacing.sm),
    ) {
        TimeRange.values().forEach { range ->
            FilterChip(
                selected = range == selectedRange,
                onClick = { onRangeSelected(range) },
                label = { Text(range.label) },
            )
        }
    }
}

/**
 * Summary card showing weekly/monthly insights
 */
@Composable
private fun SummaryCard(
    summary: InsightSummary,
    modifier: Modifier = Modifier,
) {
    val glowColors = LocalGlowColors.current
    val reducedMotion = isReducedMotionEnabled()

    // Animate card appearance
    val alpha by animateFloatAsState(
        targetValue = 1f,
        animationSpec = if (reducedMotion) tween(0) else tween(durationMillis = 300, easing = GlowEasing),
        label = "summaryCardAlpha",
    )

    val scale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = if (reducedMotion) tween(0) else tween(durationMillis = 300, easing = GlowEasing),
        label = "summaryCardScale",
    )

    Card(
        modifier =
            modifier
                .fillMaxWidth()
                .alpha(alpha)
                .scale(scale),
        shape = GlowShapes.md,
        colors =
            CardDefaults.cardColors(
                containerColor =
                    when (summary.type) {
                        SummaryType.POSITIVE -> glowColors.success.copy(alpha = 0.1f)
                        SummaryType.NEGATIVE -> glowColors.danger.copy(alpha = 0.1f)
                        SummaryType.NEUTRAL -> MaterialTheme.colorScheme.surface
                    },
            ),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(GlowSpacing.md),
            horizontalArrangement = Arrangement.spacedBy(GlowSpacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector =
                    when (summary.type) {
                        SummaryType.POSITIVE -> Icons.Filled.TrendingUp
                        SummaryType.NEGATIVE -> Icons.Filled.TrendingDown
                        SummaryType.NEUTRAL -> Icons.Filled.Info
                    },
                contentDescription =
                    when (summary.type) {
                        SummaryType.POSITIVE -> "Positive trend"
                        SummaryType.NEGATIVE -> "Negative trend"
                        SummaryType.NEUTRAL -> "Neutral information"
                    },
                tint =
                    when (summary.type) {
                        SummaryType.POSITIVE -> glowColors.success
                        SummaryType.NEGATIVE -> glowColors.danger
                        SummaryType.NEUTRAL -> glowColors.ink600
                    },
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = summary.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = glowColors.ink900,
                )
                Text(
                    text = summary.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = glowColors.ink600,
                )
            }
        }
    }
}

/**
 * Recommendation card with actionable advice
 */
@Composable
private fun RecommendationCard(
    recommendation: ProductRecommendation,
    onNavigateToRoutine: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val glowColors = LocalGlowColors.current
    val reducedMotion = isReducedMotionEnabled()

    // Animate card appearance
    val alpha by animateFloatAsState(
        targetValue = 1f,
        animationSpec = if (reducedMotion) tween(0) else tween(durationMillis = 300, easing = GlowEasing),
        label = "recommendationCardAlpha",
    )

    val scale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = if (reducedMotion) tween(0) else tween(durationMillis = 300, easing = GlowEasing),
        label = "recommendationCardScale",
    )

    Card(
        modifier =
            modifier
                .fillMaxWidth()
                .alpha(alpha)
                .scale(scale),
        shape = GlowShapes.md,
        colors =
            CardDefaults.cardColors(
                containerColor = glowColors.honey500.copy(alpha = 0.1f),
            ),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(GlowSpacing.md),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(GlowSpacing.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Filled.Lightbulb,
                    contentDescription = "Recommendation",
                    tint = glowColors.honey700,
                )
                Text(
                    text = recommendation.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = glowColors.ink900,
                )
            }

            Spacer(modifier = Modifier.height(GlowSpacing.xs))

            Text(
                text = recommendation.description,
                style = MaterialTheme.typography.bodyMedium,
                color = glowColors.ink600,
            )

            if (recommendation.actionable) {
                Spacer(modifier = Modifier.height(GlowSpacing.sm))

                GlowButton(
                    text = "Update Routine",
                    onClick = onNavigateToRoutine,
                    variant = GlowButtonVariant.Secondary,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

// Domain models for insights

sealed class InsightsEnhancedUiState {
    object Loading : InsightsEnhancedUiState()

    data class Error(
        val message: String,
    ) : InsightsEnhancedUiState()

    data class Content(
        val data: InsightsData,
        val selectedTimeRange: TimeRange,
    ) : InsightsEnhancedUiState()
}

data class InsightsData(
    val metricTrends: Map<PrimaryMetric, List<MetricDataPoint>>,
    val summaries: List<InsightSummary>,
    val recommendations: List<ProductRecommendation>,
)

data class MetricDataPoint(
    val timestamp: String,
    val value: Float,
    val label: String? = null,
)

data class InsightSummary(
    val title: String,
    val description: String,
    val type: SummaryType,
    val period: String,
)

enum class SummaryType {
    POSITIVE,
    NEGATIVE,
    NEUTRAL,
}

data class ProductRecommendation(
    val title: String,
    val description: String,
    val reason: RecommendationReason,
    val actionable: Boolean = true,
)

enum class RecommendationReason {
    REDNESS_INCREASING,
    TEXTURE_IMPROVING,
    TONE_DECLINING,
    HYDRATION_LOW,
    GENERAL,
}

enum class TimeRange(
    val label: String,
    val days: Int,
) {
    WEEK("7 days", 7),
    TWO_WEEKS("14 days", 14),
    MONTH("30 days", 30),
    THREE_MONTHS("90 days", 90),
}
