package com.glowup.ai.feature.comparison

import android.content.Intent
import androidx.compose.ui.platform.LocalContext
import com.glowup.ai.core.ui.CapturePhoto
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
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
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
 * Side-by-side comparison of two captures with server-owned qualitative
 * observations. Legacy numeric metrics are shown only when available.
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
    val context = LocalContext.current
    val share = {
        if (state is ComparisonUiState.Content) {
            val before = state.history[state.selectedBaselineIndex]
            val after = state.history[state.selectedCurrentIndex]
            val text = "My GlowUp AI progress: ${before.capturedAt.take(10)} to ${after.capturedAt.take(10)}\n" +
                "Redness: ${before.rednessScore ?: "unavailable"} → ${after.rednessScore ?: "unavailable"}\n" +
                "Blemishes: ${before.blemishCount ?: "unavailable"} → ${after.blemishCount ?: "unavailable"}\n" +
                "Cosmetic tracking only, not a diagnosis."
            context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, text)
            }, "Share progress"))
        }
    }
    Scaffold(
        topBar = {
            GlowTopBar(
                title = "Compare Progress",
                onBack = onBack,
                actions = {
                    if (state is ComparisonUiState.Content) {
                        IconButton(onClick = share) {
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
                    onShare = share,
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
    onShare: () -> Unit,
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
        item {
            CapturePairPicker(
                history = state.history,
                baselineIndex = state.selectedBaselineIndex,
                currentIndex = state.selectedCurrentIndex,
                onSelected = onComparisonSelected,
            )
        }

        // Header info card
        item {
            ComparisonHeaderCard(baseline = baseline, current = current)
        }

        // Side-by-side photos
        item {
            PhotoComparisonCard(baseline = baseline, current = current)
        }

        item {
            QualitativeComparisonCard(
                comparison = state.comparison,
                loading = state.comparisonLoading,
                error = state.comparisonError,
            )
        }

        // Keep legacy numeric metrics only for older captures that actually
        // have them; Luna comparisons are categorical and never fabricate
        // percentage deltas.
        if (listOf(
                baseline.rednessScore, baseline.blemishCount, baseline.textureScore, baseline.darkspotArea,
                current.rednessScore, current.blemishCount, current.textureScore, current.darkspotArea,
            ).any { it != null }
        ) {
            item {
                MetricsComparisonCard(baseline = baseline, current = current)
            }
        }

        // Share button
        item {
            GlowButton(
                text = "Share Progress",
                onClick = onShare,
                modifier = Modifier.fillMaxWidth(),
                variant = GlowButtonVariant.Primary,
            )
        }
    }
}

@Composable
private fun CapturePairPicker(
    history: List<HistoryItem>,
    baselineIndex: Int,
    currentIndex: Int,
    onSelected: (Int, Int) -> Unit,
) {
    var baselineOpen by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    var currentOpen by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    val glow = LocalGlowColors.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = glow.surfaceCard),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = GlowSpacing.sm, vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text("Compare", style = MaterialTheme.typography.labelSmall, color = glow.ink600)
                TextButton(onClick = { baselineOpen = true }) {
                    Text("Before: ${formatCaptureDate(history[baselineIndex].capturedAt)}")
                }
                DropdownMenu(expanded = baselineOpen, onDismissRequest = { baselineOpen = false }) {
                    history.forEachIndexed { index, item ->
                        DropdownMenuItem(
                            text = { Text(formatCaptureDate(item.capturedAt)) },
                            onClick = {
                                baselineOpen = false
                                onSelected(index, currentIndex)
                            },
                        )
                    }
                }
            }
            Column {
                Text("Against", style = MaterialTheme.typography.labelSmall, color = glow.ink600)
                TextButton(onClick = { currentOpen = true }) {
                    Text("After: ${formatCaptureDate(history[currentIndex].capturedAt)}")
                }
                DropdownMenu(expanded = currentOpen, onDismissRequest = { currentOpen = false }) {
                    history.forEachIndexed { index, item ->
                        DropdownMenuItem(
                            text = { Text(formatCaptureDate(item.capturedAt)) },
                            onClick = {
                                currentOpen = false
                                onSelected(baselineIndex, index)
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun QualitativeComparisonCard(
    comparison: com.glowup.ai.data.remote.dto.ComparisonResponseDto?,
    loading: Boolean,
    error: String?,
) {
    val glowColors = LocalGlowColors.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = glowColors.surfaceCard),
    ) {
        Column(modifier = Modifier.padding(GlowSpacing.md), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("AI observation comparison", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            when {
                loading -> Text("Comparing visible appearance…", color = glowColors.ink600)
                comparison == null -> Text(
                    error ?: "No qualitative comparison is available yet.",
                    color = glowColors.ink600,
                )
                comparison.status != "completed" -> Text(
                    comparison.reasons.firstOrNull() ?: "Comparison is unavailable for these captures.",
                    color = glowColors.ink600,
                )
                else -> {
                    comparison.language?.answer?.takeIf { it.isNotBlank() }?.let {
                        Text(it, style = MaterialTheme.typography.bodyMedium)
                    }
                    val changes = if (comparison.changes.isNotEmpty()) comparison.changes else comparison.comparison?.changes.orEmpty()
                    if (changes.isEmpty()) {
                        Text(
                            "No reliable visible change was identified. This is cosmetic tracking, not a diagnosis.",
                            color = glowColors.ink600,
                        )
                    } else {
                        changes.take(6).forEach { change ->
                            Text(
                                "${change.region.replace('_', ' ')} · ${change.concern.replace('_', ' ')}: ${change.change.replace('_', ' ')} — ${change.description}",
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                    val limitations = comparison.comparison?.limitations.orEmpty()
                    limitations.firstOrNull()?.let { Text("Limit: $it", style = MaterialTheme.typography.bodySmall, color = glowColors.ink600) }
                }
            }
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
                        CapturePhoto(baseline.photoPath, "Baseline photo from ${baseline.capturedAt.take(10)}", Modifier.fillMaxWidth().weight(1f))
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
                        CapturePhoto(current.photoPath, "Current photo from ${current.capturedAt.take(10)}", Modifier.fillMaxWidth().weight(1f))
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
                text = "Legacy numeric metrics",
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
