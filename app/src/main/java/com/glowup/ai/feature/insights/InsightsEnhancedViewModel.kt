package com.glowup.ai.feature.insights

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.glowup.ai.data.repository.CaptureRepository
import com.glowup.ai.domain.model.HistoryItem
import com.glowup.ai.domain.model.PrimaryMetric
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import kotlin.math.abs

/**
 * ViewModel for enhanced insights screen with trend analysis
 */
@HiltViewModel
class InsightsEnhancedViewModel
    @Inject
    constructor(
        private val captureRepository: CaptureRepository,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow<InsightsEnhancedUiState>(InsightsEnhancedUiState.Loading)
        val uiState: StateFlow<InsightsEnhancedUiState> = _uiState.asStateFlow()

        private val _selectedMetric = MutableStateFlow(PrimaryMetric.REDNESS_SCORE)
        val selectedMetric: StateFlow<PrimaryMetric> = _selectedMetric.asStateFlow()

        private val _selectedTimeRange = MutableStateFlow(TimeRange.MONTH)
        val selectedTimeRange: StateFlow<TimeRange> = _selectedTimeRange.asStateFlow()

        init {
            loadInsights()
        }

        fun selectMetric(metric: PrimaryMetric) {
            _selectedMetric.value = metric
        }

        fun selectTimeRange(range: TimeRange) {
            _selectedTimeRange.value = range
            loadInsights()
        }

        fun refresh() {
            loadInsights()
        }

        private fun loadInsights() {
            viewModelScope.launch {
                try {
                    _uiState.value = InsightsEnhancedUiState.Loading

                    // Get capture history - using mock data for now
                    // In real app, this would fetch from repository
                    val history = getMockHistory()

                    // Filter by time range
                    val now = Instant.now()
                    val cutoffDate = now.minus(_selectedTimeRange.value.days.toLong(), ChronoUnit.DAYS)
                    val filteredHistory =
                        history
                            .filter {
                                try {
                                    val captureTime = Instant.parse(it.capturedAt)
                                    captureTime.isAfter(cutoffDate)
                                } catch (e: Exception) {
                                    false
                                }
                            }.sortedBy { it.capturedAt }

                    // Build metric trends
                    val metricTrends = buildMetricTrends(filteredHistory)

                    // Generate summaries
                    val summaries = generateSummaries(filteredHistory, metricTrends)

                    // Generate recommendations
                    val recommendations = generateRecommendations(metricTrends, filteredHistory)

                    _uiState.value =
                        InsightsEnhancedUiState.Content(
                            data =
                                InsightsData(
                                    metricTrends = metricTrends,
                                    summaries = summaries,
                                    recommendations = recommendations,
                                ),
                            selectedTimeRange = _selectedTimeRange.value,
                        )
                } catch (e: Exception) {
                    _uiState.value =
                        InsightsEnhancedUiState.Error(
                            message = e.message ?: "Failed to load insights",
                        )
                }
            }
        }

        private fun getMockHistory(): List<HistoryItem> {
            // Mock data for demonstration
            val now = Instant.now()
            return (0..30).map { daysAgo ->
                HistoryItem(
                    id = "capture_$daysAgo",
                    capturedAt = now.minus(daysAgo.toLong(), ChronoUnit.DAYS).toString(),
                    isBaseline = daysAgo == 30,
                    rednessScore = 45.0 + (Math.random() * 20 - 10),
                    blemishCount = 3.0 + (Math.random() * 4 - 2),
                    rednessDelta = null,
                    darkspotArea = 12.0 + (Math.random() * 6 - 3),
                    textureScore = 60.0 + (Math.random() * 15 - 7.5),
                    confidence = 0.85,
                    modelVersion = "v1",
                    captureQuality = null,
                    noiseFloor = emptyMap(),
                    appearanceMetrics = emptyMap(),
                    confidenceLabel = "good",
                    baselineComparison = null,
                )
            }
        }

        private fun buildMetricTrends(history: List<HistoryItem>): Map<PrimaryMetric, List<MetricDataPoint>> =
            mapOf(
                PrimaryMetric.REDNESS_SCORE to
                    history.mapNotNull { capture ->
                        capture.rednessScore?.let {
                            MetricDataPoint(
                                timestamp = capture.capturedAt,
                                value = it.toFloat(),
                            )
                        }
                    },
                PrimaryMetric.TEXTURE_SCORE to
                    history.mapNotNull { capture ->
                        capture.textureScore?.let {
                            MetricDataPoint(
                                timestamp = capture.capturedAt,
                                value = it.toFloat(),
                            )
                        }
                    },
                PrimaryMetric.BLEMISH_COUNT to
                    history.mapNotNull { capture ->
                        capture.blemishCount?.let {
                            MetricDataPoint(
                                timestamp = capture.capturedAt,
                                value = it.toFloat(),
                            )
                        }
                    },
                PrimaryMetric.DARKSPOT_AREA to
                    history.mapNotNull { capture ->
                        capture.darkspotArea?.let {
                            MetricDataPoint(
                                timestamp = capture.capturedAt,
                                value = it.toFloat(),
                            )
                        }
                    },
            )

        private fun generateSummaries(
            history: List<HistoryItem>,
            trends: Map<PrimaryMetric, List<MetricDataPoint>>,
        ): List<InsightSummary> {
            val summaries = mutableListOf<InsightSummary>()

            // Analyze each metric
            trends.forEach { (metric, dataPoints) ->
                if (dataPoints.size >= 2) {
                    val first = dataPoints.first().value
                    val last = dataPoints.last().value
                    val change = ((last - first) / first * 100)

                    when {
                        abs(change) < 5 -> {
                            summaries.add(
                                InsightSummary(
                                    title = "${metric.displayName}: Stable",
                                    description = "Your ${metric.displayName.lowercase()} has remained consistent over this period.",
                                    type = SummaryType.NEUTRAL,
                                    period = _selectedTimeRange.value.label,
                                ),
                            )
                        }

                        change > 0 -> {
                            val type = SummaryType.NEGATIVE
                            summaries.add(
                                InsightSummary(
                                    title = "${metric.displayName}: Increasing",
                                    description = "${metric.displayName} has increased by ${String.format(
                                        "%.1f",
                                        abs(change),
                                    )}% over the last ${_selectedTimeRange.value.label}.",
                                    type = type,
                                    period = _selectedTimeRange.value.label,
                                ),
                            )
                        }

                        else -> {
                            val type = SummaryType.POSITIVE
                            summaries.add(
                                InsightSummary(
                                    title = "${metric.displayName}: Improving",
                                    description = "${metric.displayName} has decreased by ${String.format(
                                        "%.1f",
                                        abs(change),
                                    )}% over the last ${_selectedTimeRange.value.label}.",
                                    type = type,
                                    period = _selectedTimeRange.value.label,
                                ),
                            )
                        }
                    }
                }
            }

            // Add streak summary if applicable
            val captureCount = history.size
            if (captureCount > 0) {
                summaries.add(
                    InsightSummary(
                        title = "Best streak: $captureCount captures",
                        description = "You've been consistent with tracking your progress!",
                        type = SummaryType.POSITIVE,
                        period = _selectedTimeRange.value.label,
                    ),
                )
            }

            return summaries
        }

        private fun generateRecommendations(
            trends: Map<PrimaryMetric, List<MetricDataPoint>>,
            history: List<HistoryItem>,
        ): List<ProductRecommendation> {
            val recommendations = mutableListOf<ProductRecommendation>()

            // Check redness trend
            val rednessData = trends[PrimaryMetric.REDNESS_SCORE]
            if (rednessData != null && rednessData.size >= 2) {
                val first = rednessData.first().value
                val last = rednessData.last().value
                val change = ((last - first) / first * 100)

                if (change > 10) {
                    recommendations.add(
                        ProductRecommendation(
                            title = "Consider anti-redness products",
                            description =
                                "Your redness has increased by ${String.format("%.1f", change)}%. " +
                                    "Look for products with niacinamide, azelaic acid, or centella asiatica to help calm inflammation.",
                            reason = RecommendationReason.REDNESS_INCREASING,
                            actionable = true,
                        ),
                    )
                }
            }

            // Check texture trend
            val textureData = trends[PrimaryMetric.TEXTURE_SCORE]
            if (textureData != null && textureData.size >= 2) {
                val first = textureData.first().value
                val last = textureData.last().value
                val change = ((last - first) / first * 100)

                if (change < -10) {
                    recommendations.add(
                        ProductRecommendation(
                            title = "Your texture is improving!",
                            description =
                                "Texture has improved by ${String.format("%.1f", abs(change))}%. " +
                                    "Keep up with your current routine - something is working!",
                            reason = RecommendationReason.TEXTURE_IMPROVING,
                            actionable = false,
                        ),
                    )
                }
            }

            // General recommendation if no specific issues
            if (recommendations.isEmpty()) {
                recommendations.add(
                    ProductRecommendation(
                        title = "Keep tracking consistently",
                        description =
                            "Your skin metrics are looking stable. Continue with your current routine " +
                                "and capture regularly to spot trends early.",
                        reason = RecommendationReason.GENERAL,
                        actionable = false,
                    ),
                )
            }

            return recommendations
        }
    }
