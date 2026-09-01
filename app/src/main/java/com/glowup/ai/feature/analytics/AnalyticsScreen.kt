package com.glowup.ai.feature.analytics

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.glowup.ai.core.design.GlowSpacing
import com.glowup.ai.core.design.LocalGlowColors
import com.glowup.ai.core.ui.*
import com.glowup.ai.feature.analytics.components.*
import com.glowup.ai.feature.analytics.components.TrendDirection
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    onNavigateBack: () -> Unit,
    viewModel: AnalyticsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val glow = LocalGlowColors.current

    Scaffold(
        topBar = {
            GlowTopBar(
                title = "Analytics",
                onBack = onNavigateBack,
                actions = {
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = "Refresh",
                            tint = glow.ink900,
                        )
                    }
                },
            )
        },
        floatingActionButton = {
            if (!uiState.isLoading && uiState.error == null) {
                ExportFab(
                    onExportPdf = viewModel::exportPdf,
                    onExportCsv = viewModel::exportCsv,
                )
            }
        },
    ) { padding ->
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding),
        ) {
            when {
                uiState.isLoading -> {
                    LoadingState()
                }

                uiState.error != null -> {
                    ErrorState(
                        message = uiState.error ?: "Unknown error",
                        onRetry = viewModel::refresh,
                    )
                }

                else -> {
                    AnalyticsContent(
                        uiState = uiState,
                        onMetricSelected = viewModel::selectMetric,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }

            // Export status snackbar
            AnimatedVisibility(
                visible = uiState.exportState is ExportState.Success || uiState.exportState is ExportState.Error,
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                Box(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(GlowSpacing.md),
                    contentAlignment = Alignment.BottomCenter,
                ) {
                    when (val state = uiState.exportState) {
                        is ExportState.Success -> {
                            SuccessState(
                                message = state.message,
                                onDismiss = viewModel::dismissExportState,
                            )
                        }

                        is ExportState.Error -> {
                            ErrorState(
                                message = state.message,
                                onRetry = viewModel::dismissExportState,
                            )
                        }

                        else -> {}
                    }
                }
            }
        }
    }
}

@Composable
private fun AnalyticsContent(
    uiState: AnalyticsUiState,
    onMetricSelected: (MetricType) -> Unit,
    modifier: Modifier = Modifier,
) {
    val glow = LocalGlowColors.current

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(GlowSpacing.md),
        verticalArrangement = Arrangement.spacedBy(GlowSpacing.md),
    ) {
        // Primary Metric - Hero Section
        item {
            uiState.trends?.let { trends ->
                val points =
                    when (trends.selectedMetric) {
                        MetricType.REDNESS -> trends.rednessPoints
                        MetricType.BLEMISH -> trends.blemishPoints
                        MetricType.DARKSPOT -> trends.darkspotPoints
                        MetricType.TEXTURE -> trends.texturePoints
                    }

                if (points.isNotEmpty()) {
                    val currentValue = points.lastOrNull()?.value
                    val trend =
                        trends.changePercent?.let { change ->
                            MetricTrend(
                                direction =
                                    when {
                                        change > 0 -> TrendDirection.UP
                                        change < 0 -> TrendDirection.DOWN
                                        else -> TrendDirection.STABLE
                                    },
                                changePercent = change,
                                description = "vs last week",
                            )
                        }

                    PrimaryMetricCard(
                        title =
                            trends.selectedMetric.name
                                .lowercase()
                                .capitalize(),
                        value = currentValue?.let { String.format("%.2f", it) } ?: "—",
                        trend = trend,
                    ) {
                        LineChart(
                            points = points,
                            label = "",
                            color =
                                when (trends.selectedMetric) {
                                    MetricType.REDNESS -> glow.danger
                                    MetricType.BLEMISH -> glow.honey700
                                    MetricType.DARKSPOT -> Color(0xFF8B4513)
                                    MetricType.TEXTURE -> glow.success
                                },
                            showGrid = true,
                            animated = true,
                        )
                    }
                } else {
                    EmptyMetricCard(
                        title = "No data yet",
                        description = "Take your first photo to start tracking",
                    )
                }
            }
        }

        // Metric Selector
        item {
            uiState.trends?.let { trends ->
                MetricSelector(
                    selectedMetric = trends.selectedMetric,
                    onMetricSelected = onMetricSelected,
                )
            }
        }

        // Overview Metrics Grid
        item {
            Spacer(modifier = Modifier.height(GlowSpacing.md))
        }

        item {
            SectionHeader(title = "Overview")
        }

        item {
            uiState.overview?.let { overview ->
                OverviewMetricGrid(overview = overview)
            } ?: run {
                EmptyMetricCard(
                    title = "No overview data",
                    description = "Start using the app to see your progress",
                )
            }
        }

        // Before/After Comparison
        item {
            uiState.trends?.let { trends ->
                if (trends.comparisonEnabled && trends.beforeValue != null && trends.afterValue != null) {
                    Spacer(modifier = Modifier.height(GlowSpacing.sm))
                    BeforeAfterCard(
                        beforeValue = trends.beforeValue,
                        afterValue = trends.afterValue,
                        changePercent = trends.changePercent,
                        metric =
                            trends.selectedMetric.name
                                .lowercase()
                                .capitalize(),
                    )
                }
            }
        }

        // Consistency Section
        item {
            Spacer(modifier = Modifier.height(GlowSpacing.md))
        }

        item {
            SectionHeader(title = "Routine Consistency")
        }

        item {
            uiState.consistency?.let { consistency ->
                ConsistencySection(consistency = consistency)
            } ?: run {
                EmptyMetricCard(
                    title = "Build your routine",
                    description = "Capture daily to track consistency",
                )
            }
        }

        // AI Insights Section
        item {
            Spacer(modifier = Modifier.height(GlowSpacing.md))
        }

        item {
            SectionHeader(title = "AI-Generated Insights")
        }

        item {
            if (uiState.insights.isEmpty()) {
                EmptyState(
                    icon = Icons.Filled.Psychology,
                    title = "No insights yet",
                    description = "Keep capturing regularly to unlock personalized insights",
                )
            } else {
                InsightsList(insights = uiState.insights)
            }
        }

        // Product Effectiveness Section
        item {
            Spacer(modifier = Modifier.height(GlowSpacing.md))
        }

        item {
            SectionHeader(title = "Product Effectiveness")
        }

        item {
            if (uiState.productEffectiveness.isEmpty()) {
                EmptyMetricCard(
                    title = "No product data",
                    description = "Add products to your routine to track effectiveness",
                )
            } else {
                ProductEffectivenessList(products = uiState.productEffectiveness)
            }
        }

        // Bottom spacing for FAB
        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
private fun OverviewMetricGrid(overview: OverviewStats) {
    val glow = LocalGlowColors.current

    val metrics =
        buildList {
            add(
                MetricGridItem(
                    title = "Total Captures",
                    value = overview.totalCaptures.toString(),
                    trend = null,
                ),
            )
            add(
                MetricGridItem(
                    title = "Current Streak",
                    value = "${overview.currentStreak}",
                    trend =
                        overview.streakChange?.let {
                            MetricTrend(
                                direction =
                                    if (it > 0) {
                                        TrendDirection.UP
                                    } else if (it < 0) {
                                        TrendDirection.DOWN
                                    } else {
                                        TrendDirection.STABLE
                                    },
                                changePercent = it.toDouble(),
                                description = "days",
                            )
                        },
                ),
            )
            add(
                MetricGridItem(
                    title = "Days Active",
                    value = overview.daysUsingApp.toString(),
                    trend = null,
                ),
            )
            add(
                MetricGridItem(
                    title = "Experiments",
                    value = overview.activeExperiments.toString(),
                    trend = null,
                ),
            )
        }

    MetricGrid(metrics = metrics)
}

@Composable
private fun MetricSelector(
    selectedMetric: MetricType,
    onMetricSelected: (MetricType) -> Unit,
) {
    val glow = LocalGlowColors.current

    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(GlowSpacing.sm),
        contentPadding = PaddingValues(vertical = GlowSpacing.xs),
    ) {
        items(MetricType.values()) { metric ->
            FilterChip(
                selected = metric == selectedMetric,
                onClick = { onMetricSelected(metric) },
                label = {
                    Text(
                        text = metric.name.lowercase().capitalize(),
                        fontWeight = if (metric == selectedMetric) FontWeight.SemiBold else FontWeight.Medium,
                    )
                },
                colors =
                    FilterChipDefaults.filterChipColors(
                        selectedContainerColor = glow.honey500,
                        selectedLabelColor = glow.ink900,
                        containerColor = glow.surfaceCard,
                        labelColor = glow.ink600,
                    ),
                border =
                    FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = metric == selectedMetric,
                        borderColor = if (metric == selectedMetric) glow.honey500 else glow.ink600.copy(alpha = 0.2f),
                        selectedBorderColor = glow.honey500,
                        borderWidth = 1.dp,
                        selectedBorderWidth = 2.dp,
                    ),
            )
        }
    }
}

@Composable
private fun BeforeAfterCard(
    beforeValue: Double,
    afterValue: Double,
    changePercent: Double?,
    metric: String,
) {
    val glow = LocalGlowColors.current

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(glow.surfaceCard, RoundedCornerShape(16.dp))
                .padding(GlowSpacing.lg),
    ) {
        Text(
            text = "Before & After",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = glow.ink900,
            modifier = Modifier.padding(bottom = GlowSpacing.md),
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = String.format("%.2f", beforeValue),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = glow.ink900,
                )
                Text(
                    text = "Before",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = glow.ink600,
                    modifier = Modifier.padding(top = GlowSpacing.xs),
                )
            }

            Icon(
                imageVector = Icons.Filled.ArrowForward,
                contentDescription = null,
                tint = glow.honey700,
                modifier = Modifier.size(32.dp),
            )

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = String.format("%.2f", afterValue),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = glow.ink900,
                )
                Text(
                    text = "After",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = glow.ink600,
                    modifier = Modifier.padding(top = GlowSpacing.xs),
                )
            }
        }

        changePercent?.let { change ->
            Spacer(modifier = Modifier.height(GlowSpacing.md))

            val isImprovement = change < 0 // Lower is better for skin metrics
            val trendColor = if (isImprovement) glow.success else glow.danger
            val trendIcon = if (isImprovement) Icons.Filled.TrendingDown else Icons.Filled.TrendingUp

            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .background(trendColor.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
                        .padding(GlowSpacing.md),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = trendIcon,
                    contentDescription = null,
                    tint = trendColor,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(modifier = Modifier.width(GlowSpacing.xs))
                Text(
                    text = "${if (change > 0) "+" else ""}${String.format("%.1f", change)}%",
                    fontFamily = FontFamily.Monospace,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = trendColor,
                )
                Spacer(modifier = Modifier.width(GlowSpacing.xs))
                Text(
                    text = "change in $metric",
                    style = MaterialTheme.typography.bodyMedium,
                    color = glow.ink900,
                )
            }
        }
    }
}

@Composable
private fun EmptyMetricCard(
    title: String,
    description: String,
    modifier: Modifier = Modifier,
) {
    val glow = LocalGlowColors.current

    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .background(glow.surfaceCard, RoundedCornerShape(16.dp))
                .padding(GlowSpacing.xl),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(GlowSpacing.xs),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = glow.ink900,
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = glow.ink600,
            )
        }
    }
}

@Composable
private fun ConsistencySection(consistency: ConsistencyData) {
    val glow = LocalGlowColors.current

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(GlowSpacing.md),
    ) {
        // Calendar Heatmap
        CalendarHeatmap(
            captureDates = consistency.captureDates,
            currentMonth = java.time.YearMonth.now(),
        )

        // Consistency Metrics Grid
        val metrics =
            buildList {
                add(
                    MetricGridItem(
                        title = "Capture Rate",
                        value = "${String.format("%.0f", consistency.captureRate)}%",
                        trend = null,
                    ),
                )
                add(
                    MetricGridItem(
                        title = "Longest Streak",
                        value = "${consistency.longestStreak}",
                        trend = null,
                    ),
                )
            }

        MetricGrid(metrics = metrics)

        // Best Time of Day (if available)
        consistency.bestTimeOfDay?.let { time ->
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .background(glow.honey500.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                        .padding(GlowSpacing.md),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(GlowSpacing.sm),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Filled.WbSunny,
                        contentDescription = null,
                        tint = glow.honey700,
                        modifier = Modifier.size(24.dp),
                    )
                    Column {
                        Text(
                            text = "Best Time to Capture",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = glow.ink600,
                        )
                        Text(
                            text = time,
                            fontFamily = FontFamily.Monospace,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = glow.ink900,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ExportFab(
    onExportPdf: () -> Unit,
    onExportCsv: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    Column(
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        AnimatedVisibility(visible = expanded) {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SmallFloatingActionButton(
                    onClick = {
                        onExportPdf()
                        expanded = false
                    },
                ) {
                    Icon(
                        imageVector = Icons.Filled.PictureAsPdf,
                        contentDescription = "Export PDF",
                    )
                }

                SmallFloatingActionButton(
                    onClick = {
                        onExportCsv()
                        expanded = false
                    },
                ) {
                    Icon(
                        imageVector = Icons.Filled.TableChart,
                        contentDescription = "Export CSV",
                    )
                }
            }
        }

        FloatingActionButton(
            onClick = { expanded = !expanded },
        ) {
            Icon(
                imageVector = if (expanded) Icons.Filled.Close else Icons.Filled.FileDownload,
                contentDescription = "Export options",
            )
        }
    }
}

@Composable
private fun LoadingState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(GlowSpacing.md),
        ) {
            CircularProgressIndicator()
            Text(
                text = "Loading analytics...",
                style = MaterialTheme.typography.bodyMedium,
                color = LocalGlowColors.current.ink600,
            )
        }
    }
}
