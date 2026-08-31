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
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.glowup.ai.core.design.GlowSpacing
import com.glowup.ai.core.design.LocalGlowColors
import com.glowup.ai.core.ui.*
import com.glowup.ai.feature.analytics.components.*
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
                            tint = glow.ink900
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            if (!uiState.isLoading && uiState.error == null) {
                ExportFab(
                    onExportPdf = viewModel::exportPdf,
                    onExportCsv = viewModel::exportCsv
                )
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                uiState.isLoading -> {
                    LoadingState()
                }
                uiState.error != null -> {
                    ErrorState(
                        message = uiState.error ?: "Unknown error",
                        onRetry = viewModel::refresh
                    )
                }
                else -> {
                    AnalyticsContent(
                        uiState = uiState,
                        onMetricSelected = viewModel::selectMetric,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            // Export status snackbar
            AnimatedVisibility(
                visible = uiState.exportState is ExportState.Success || uiState.exportState is ExportState.Error,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(GlowSpacing.md),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    when (val state = uiState.exportState) {
                        is ExportState.Success -> {
                            SuccessState(
                                message = state.message,
                                onDismiss = viewModel::dismissExportState
                            )
                        }
                        is ExportState.Error -> {
                            ErrorState(
                                message = state.message,
                                onRetry = viewModel::dismissExportState
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
        verticalArrangement = Arrangement.spacedBy(GlowSpacing.lg)
    ) {
        // Overview Section
        item {
            SectionHeader(title = "Overview")
        }

        item {
            uiState.overview?.let { overview ->
                OverviewSection(overview = overview)
            }
        }

        // Trend Charts Section
        item {
            SectionHeader(
                title = "Progress Trends",
                modifier = Modifier.padding(top = GlowSpacing.md)
            )
        }

        item {
            uiState.trends?.let { trends ->
                MetricSelector(
                    selectedMetric = trends.selectedMetric,
                    onMetricSelected = onMetricSelected
                )
            }
        }

        item {
            uiState.trends?.let { trends ->
                val points = when (trends.selectedMetric) {
                    MetricType.REDNESS -> trends.rednessPoints
                    MetricType.BLEMISH -> trends.blemishPoints
                    MetricType.DARKSPOT -> trends.darkspotPoints
                    MetricType.TEXTURE -> trends.texturePoints
                }

                LineChart(
                    points = points,
                    label = "${trends.selectedMetric.name.lowercase().capitalize()} over time",
                    color = when (trends.selectedMetric) {
                        MetricType.REDNESS -> glow.danger
                        MetricType.BLEMISH -> glow.honey700
                        MetricType.DARKSPOT -> Color(0xFF8B4513)
                        MetricType.TEXTURE -> glow.success
                    }
                )
            }
        }

        // Before/After Comparison
        item {
            uiState.trends?.let { trends ->
                if (trends.comparisonEnabled && trends.beforeValue != null && trends.afterValue != null) {
                    BeforeAfterCard(
                        beforeValue = trends.beforeValue,
                        afterValue = trends.afterValue,
                        changePercent = trends.changePercent,
                        metric = trends.selectedMetric.name.lowercase().capitalize()
                    )
                }
            }
        }

        // Consistency Section
        item {
            SectionHeader(
                title = "Routine Consistency",
                modifier = Modifier.padding(top = GlowSpacing.md)
            )
        }

        item {
            uiState.consistency?.let { consistency ->
                ConsistencySection(consistency = consistency)
            }
        }

        // AI Insights Section
        item {
            SectionHeader(
                title = "AI-Generated Insights",
                modifier = Modifier.padding(top = GlowSpacing.md)
            )
        }

        item {
            if (uiState.insights.isEmpty()) {
                EmptyState(
                    title = "No insights yet",
                    body = "Keep capturing regularly to unlock personalized insights",
                    icon = Icons.Filled.Psychology
                )
            } else {
                InsightsList(insights = uiState.insights)
            }
        }

        // Product Effectiveness Section
        item {
            SectionHeader(
                title = "Product Effectiveness",
                modifier = Modifier.padding(top = GlowSpacing.md)
            )
        }

        item {
            ProductEffectivenessList(products = uiState.productEffectiveness)
        }

        // Bottom spacing
        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
private fun OverviewSection(overview: OverviewStats) {
    val glow = LocalGlowColors.current

    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(GlowSpacing.sm),
        contentPadding = PaddingValues(horizontal = 2.dp)
    ) {
        item {
            StatTile(
                label = "Total Captures",
                value = overview.totalCaptures.toString(),
                modifier = Modifier.width(160.dp)
            )
        }

        item {
            StatTile(
                label = "Current Streak",
                value = "${overview.currentStreak} days",
                delta = overview.streakChange?.let {
                    StatDelta(
                        text = "${if (it > 0) "+" else ""}$it days",
                        direction = if (it > 0) StatDeltaDirection.Up else StatDeltaDirection.Down
                    )
                },
                accent = true,
                modifier = Modifier.width(160.dp)
            )
        }

        item {
            StatTile(
                label = "Days Using App",
                value = overview.daysUsingApp.toString(),
                modifier = Modifier.width(160.dp)
            )
        }

        item {
            StatTile(
                label = "Active Experiments",
                value = overview.activeExperiments.toString(),
                modifier = Modifier.width(160.dp)
            )
        }
    }
}

@Composable
private fun MetricSelector(
    selectedMetric: MetricType,
    onMetricSelected: (MetricType) -> Unit,
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(GlowSpacing.xs),
        contentPadding = PaddingValues(vertical = GlowSpacing.xs)
    ) {
        items(MetricType.values()) { metric ->
            FilterChip(
                selected = metric == selectedMetric,
                onClick = { onMetricSelected(metric) },
                label = {
                    Text(text = metric.name.lowercase().capitalize())
                }
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
        modifier = Modifier
            .fillMaxWidth()
            .background(glow.surfaceCard, RoundedCornerShape(12.dp))
            .padding(GlowSpacing.md)
    ) {
        Text(
            text = "Before & After",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = glow.ink900,
            modifier = Modifier.padding(bottom = GlowSpacing.sm)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = String.format("%.2f", beforeValue),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = glow.ink900
                )
                Text(
                    text = "Before",
                    style = MaterialTheme.typography.bodySmall,
                    color = glow.ink600
                )
            }

            Icon(
                imageVector = Icons.Filled.ArrowForward,
                contentDescription = null,
                tint = glow.honey700,
                modifier = Modifier
                    .align(Alignment.CenterVertically)
                    .size(32.dp)
            )

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = String.format("%.2f", afterValue),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = glow.ink900
                )
                Text(
                    text = "After",
                    style = MaterialTheme.typography.bodySmall,
                    color = glow.ink600
                )
            }
        }

        changePercent?.let { change ->
            Spacer(modifier = Modifier.height(GlowSpacing.sm))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        if (change < 0) glow.success.copy(alpha = 0.15f)
                        else glow.danger.copy(alpha = 0.15f),
                        RoundedCornerShape(8.dp)
                    )
                    .padding(GlowSpacing.sm),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${if (change > 0) "+" else ""}${String.format("%.1f", change)}% change in $metric",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (change < 0) glow.success else glow.danger
                )
            }
        }
    }
}

@Composable
private fun ConsistencySection(consistency: ConsistencyData) {
    Column(
        verticalArrangement = Arrangement.spacedBy(GlowSpacing.sm)
    ) {
        CalendarHeatmap(
            captureDates = consistency.captureDates,
            currentMonth = java.time.YearMonth.now()
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(GlowSpacing.sm),
            contentPadding = PaddingValues(horizontal = 2.dp)
        ) {
            item {
                StatTile(
                    label = "Capture Rate",
                    value = "${String.format("%.0f", consistency.captureRate)}%",
                    modifier = Modifier.width(140.dp)
                )
            }

            item {
                StatTile(
                    label = "Longest Streak",
                    value = "${consistency.longestStreak} days",
                    modifier = Modifier.width(140.dp)
                )
            }

            consistency.bestTimeOfDay?.let { time ->
                item {
                    StatTile(
                        label = "Best Time",
                        value = time,
                        modifier = Modifier.width(140.dp)
                    )
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
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        AnimatedVisibility(visible = expanded) {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SmallFloatingActionButton(
                    onClick = {
                        onExportPdf()
                        expanded = false
                    }
                ) {
                    Icon(
                        imageVector = Icons.Filled.PictureAsPdf,
                        contentDescription = "Export PDF"
                    )
                }

                SmallFloatingActionButton(
                    onClick = {
                        onExportCsv()
                        expanded = false
                    }
                ) {
                    Icon(
                        imageVector = Icons.Filled.TableChart,
                        contentDescription = "Export CSV"
                    )
                }
            }
        }

        FloatingActionButton(
            onClick = { expanded = !expanded }
        ) {
            Icon(
                imageVector = if (expanded) Icons.Filled.Close else Icons.Filled.FileDownload,
                contentDescription = "Export options"
            )
        }
    }
}

@Composable
private fun LoadingState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(GlowSpacing.md)
        ) {
            CircularProgressIndicator()
            Text(
                text = "Loading analytics...",
                style = MaterialTheme.typography.bodyMedium,
                color = LocalGlowColors.current.ink600
            )
        }
    }
}
