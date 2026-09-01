package com.glowup.ai.feature.comparison

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.glowup.ai.core.design.GlowSpacing
import com.glowup.ai.core.design.LocalGlowColors
import com.glowup.ai.core.ui.ErrorState
import com.glowup.ai.core.ui.GlowButton
import com.glowup.ai.core.ui.GlowButtonVariant
import com.glowup.ai.core.ui.GlowTopBar
import com.glowup.ai.core.ui.ShimmerSkeleton
import com.glowup.ai.domain.model.HistoryItem
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.abs

/**
 * Photo Comparison Screen
 * Side-by-side comparison of two captures with metrics comparison
 * Shows baseline vs current with trend indicators
 */
@Composable
fun ComparisonRoute(
    onBack: () -> Unit,
    viewModel: ComparisonViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    ComparisonScreen(
        state = state,
        onBack = onBack,
        onRetry = viewModel::onRetry,
        onComparisonSelected = viewModel::onComparisonSelected,
    )
}

@Composable
fun ComparisonScreen(
    state: ComparisonUiState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onComparisonSelected: (Int, Int) -> Unit,
) {
    Scaffold(
        topBar = {
            GlowTopBar(
                title = "Compare Progress",
                onBack = onBack,
                actions = {
                    if (state is ComparisonUiState.Content) {
                        IconButton(onClick = { /* TODO: Share functionality */ }) {
                            Icon(
                                imageVector = Icons.Filled.Share,
                                contentDescription = "Share comparison",
                            )
                        }
                    }
                },
            )
        },
    ) { padding ->
        when (state) {
            is ComparisonUiState.Loading -> {
                ComparisonLoadingSkeleton(padding)
            }

            is ComparisonUiState.Error -> {
                Box(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(padding)
                            .padding(24.dp),
                ) {
                    ErrorState(message = state.message, onRetry = onRetry)
                }
            }

            is ComparisonUiState.Content -> {
                ComparisonContent(
                    padding = padding,
                    state = state,
                    onComparisonSelected = onComparisonSelected,
                )
            }
        }
    }
}

@Composable
private fun ComparisonLoadingSkeleton(padding: PaddingValues) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(GlowSpacing.md),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        ShimmerSkeleton(height = 60.dp, cornerRadius = 16.dp)
        ShimmerSkeleton(height = 300.dp, cornerRadius = 16.dp)
        ShimmerSkeleton(height = 200.dp, cornerRadius = 16.dp)
    }
}

@Composable
private fun ComparisonContent(
    padding: PaddingValues,
    state: ComparisonUiState.Content,
    onComparisonSelected: (Int, Int) -> Unit,
) {
    val baseline = state.history[state.selectedBaselineIndex]
    val current = state.history[state.selectedCurrentIndex]
    val glowColors = LocalGlowColors.current

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding =
            PaddingValues(
                start = GlowSpacing.md,
                end = GlowSpacing.md,
                top = padding.calculateTopPadding() + GlowSpacing.md,
                bottom = padding.calculateBottomPadding() + GlowSpacing.xl,
            ),
        verticalArrangement = Arrangement.spacedBy(GlowSpacing.md),
    ) {
        // Header info card
        item {
            ComparisonHeaderCard(baseline = baseline, current = current)
        }

        // Side-by-side photos
        item {
            PhotoComparisonCard(baseline = baseline, current = current)
        }

        // Metrics comparison table
        item {
            MetricsComparisonCard(baseline = baseline, current = current)
        }

        // Share button
        item {
            GlowButton(
                text = "Share Progress",
                onClick = { /* TODO: Share functionality */ },
                modifier = Modifier.fillMaxWidth(),
                variant = GlowButtonVariant.Primary,
            )
        }
    }
}

@Composable
private fun ComparisonHeaderCard(
    baseline: HistoryItem,
    current: HistoryItem,
) {
    val glowColors = LocalGlowColors.current
    val baselineDate = formatCaptureDate(baseline.capturedAt)
    val currentDate = formatCaptureDate(current.capturedAt)
    val daysBetween = calculateDaysBetween(baseline.capturedAt, current.capturedAt)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors =
            CardDefaults.cardColors(
                containerColor = glowColors.surfaceCard,
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(GlowSpacing.md),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Baseline
            Column(horizontalAlignment = Alignment.Start) {
                Text(
                    text = "BASELINE",
                    style = MaterialTheme.typography.labelSmall,
                    color = glowColors.ink600,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = baselineDate,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = glowColors.ink900,
                )
            }

            // Arrow with days
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Filled.ArrowForward,
                    contentDescription = null,
                    tint = glowColors.honey500,
                    modifier = Modifier.size(24.dp),
                )
                Text(
                    text = "$daysBetween days",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = glowColors.honey500,
                )
            }

            // Current
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "CURRENT",
                    style = MaterialTheme.typography.labelSmall,
                    color = glowColors.ink600,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = currentDate,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = glowColors.ink900,
                )
            }
        }
    }
}

@Composable
private fun PhotoComparisonCard(
    baseline: HistoryItem,
    current: HistoryItem,
) {
    val glowColors = LocalGlowColors.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors =
            CardDefaults.cardColors(
                containerColor = glowColors.surfaceCard,
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(GlowSpacing.md),
        ) {
            Text(
                text = "Photo Comparison",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = glowColors.ink900,
            )
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // Baseline photo
                Box(
                    modifier =
                        Modifier
                            .weight(1f)
                            .height(200.dp)
                            .background(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(12.dp),
                            ).border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.outline,
                                shape = RoundedCornerShape(12.dp),
                            ),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "BASELINE",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = glowColors.ink600,
                        )
                        Text(
                            text = formatCaptureDate(baseline.capturedAt),
                            style = MaterialTheme.typography.bodySmall,
                            color = glowColors.ink600,
                        )
                    }
                }

                // Current photo
                Box(
                    modifier =
                        Modifier
                            .weight(1f)
                            .height(200.dp)
                            .background(
                                color = glowColors.honey300.copy(alpha = 0.3f),
                                shape = RoundedCornerShape(12.dp),
                            ).border(
                                width = 1.dp,
                                color = glowColors.honey300,
                                shape = RoundedCornerShape(12.dp),
                            ),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "CURRENT",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = glowColors.honey700,
                        )
                        Text(
                            text = formatCaptureDate(current.capturedAt),
                            style = MaterialTheme.typography.bodySmall,
                            color = glowColors.honey700,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Note: Photo images will be loaded from server in future update",
                style = MaterialTheme.typography.bodySmall,
                color = glowColors.ink600,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun MetricsComparisonCard(
    baseline: HistoryItem,
    current: HistoryItem,
) {
    val glowColors = LocalGlowColors.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors =
            CardDefaults.cardColors(
                containerColor = glowColors.surfaceCard,
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(GlowSpacing.md),
        ) {
            Text(
                text = "Metrics Comparison",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = glowColors.ink900,
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Redness
            MetricComparisonRow(
                label = "Redness",
                baselineValue = baseline.rednessScore,
                currentValue = current.rednessScore,
                lowerIsBetter = true,
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // Blemishes
            MetricComparisonRow(
                label = "Blemishes",
                baselineValue = baseline.blemishCount,
                currentValue = current.blemishCount,
                lowerIsBetter = true,
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // Texture
            MetricComparisonRow(
                label = "Texture",
                baselineValue = baseline.textureScore,
                currentValue = current.textureScore,
                lowerIsBetter = true,
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // Dark Spots
            MetricComparisonRow(
                label = "Dark Spots",
                baselineValue = baseline.darkspotArea,
                currentValue = current.darkspotArea,
                lowerIsBetter = true,
            )
        }
    }
}

@Composable
private fun MetricComparisonRow(
    label: String,
    baselineValue: Double?,
    currentValue: Double?,
    lowerIsBetter: Boolean,
) {
    val glowColors = LocalGlowColors.current

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            color = glowColors.ink900,
            modifier = Modifier.weight(1f),
        )

        if (baselineValue != null && currentValue != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                // Baseline value
                Text(
                    text = formatMetricValue(baselineValue),
                    style = MaterialTheme.typography.bodyMedium,
                    color = glowColors.ink600,
                )

                Icon(
                    imageVector = Icons.Filled.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = glowColors.ink600,
                )

                // Current value
                Text(
                    text = formatMetricValue(currentValue),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = glowColors.ink900,
                )

                // Trend indicator
                TrendIndicator(
                    baseline = baselineValue,
                    current = currentValue,
                    lowerIsBetter = lowerIsBetter,
                )
            }
        } else {
            Text(
                text = "N/A",
                style = MaterialTheme.typography.bodyMedium,
                color = glowColors.ink600,
            )
        }
    }
}

@Composable
private fun TrendIndicator(
    baseline: Double,
    current: Double,
    lowerIsBetter: Boolean,
) {
    val glowColors = LocalGlowColors.current
    val delta = current - baseline
    val threshold = 0.01 // Consider values within 1% as "same"

    val (icon, color, contentDescription) =
        when {
            abs(delta) < threshold -> {
                Triple(Icons.Filled.ArrowForward, glowColors.ink600, "No change")
            }

            (delta < 0 && lowerIsBetter) || (delta > 0 && !lowerIsBetter) -> {
                Triple(Icons.Filled.ArrowUpward, Color(0xFF10B981), "Improved")
            }

            else -> {
                Triple(Icons.Filled.ArrowDownward, Color(0xFFEF4444), "Worsened")
            }
        }

    Box(
        modifier =
            Modifier
                .size(28.dp)
                .background(color = color.copy(alpha = 0.1f), shape = RoundedCornerShape(6.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = color,
            modifier = Modifier.size(16.dp),
        )
    }
}

// Helper functions
private fun formatCaptureDate(isoDate: String): String =
    try {
        val instant = Instant.parse(isoDate)
        val formatter =
            DateTimeFormatter
                .ofPattern("MMM d, yyyy")
                .withZone(ZoneId.systemDefault())
        formatter.format(instant)
    } catch (e: Exception) {
        "Unknown"
    }

private fun calculateDaysBetween(
    startDate: String,
    endDate: String,
): Int =
    try {
        val start = Instant.parse(startDate)
        val end = Instant.parse(endDate)
        val days =
            java.time.Duration
                .between(start, end)
                .toDays()
        days.toInt()
    } catch (e: Exception) {
        0
    }

private fun formatMetricValue(value: Double): String =
    when {
        value >= 10 -> String.format("%.0f", value)
        value >= 1 -> String.format("%.1f", value)
        else -> String.format("%.2f", value)
    }
