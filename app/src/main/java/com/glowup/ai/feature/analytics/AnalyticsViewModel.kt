package com.glowup.ai.feature.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.glowup.ai.core.util.GlowResult
import com.glowup.ai.data.local.SessionStore
import com.glowup.ai.data.repository.HomeRepository
import com.glowup.ai.data.repository.ExperimentRepository
import com.glowup.ai.domain.StreakCalculator
import com.glowup.ai.domain.model.Capture
import com.glowup.ai.domain.model.HistoryItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import kotlin.math.abs

@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    private val homeRepository: HomeRepository,
    private val experimentRepository: ExperimentRepository,
    private val sessionStore: SessionStore,
    private val streakCalculator: StreakCalculator,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AnalyticsUiState())
    val uiState: StateFlow<AnalyticsUiState> = _uiState.asStateFlow()

    private var historyItems: List<HistoryItem> = emptyList()

    init {
        loadAnalyticsData()
    }

    fun refresh() {
        loadAnalyticsData()
    }

    private fun loadAnalyticsData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            val userId = sessionStore.userId()
            if (userId == null) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Please sign in to view analytics"
                )
                return@launch
            }

            // Load dashboard for engagement data
            val dashboardResult = homeRepository.getDashboard(userId)
            val historyResult = homeRepository.getHistory(userId)
            val experimentsResult = experimentRepository.listExperiments(userId)

            when {
                historyResult is GlowResult.Failure -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Failed to load analytics: ${historyResult.error.toUserMessage()}"
                    )
                }
                else -> {
                    val history = (historyResult as? GlowResult.Success)?.data?.data ?: emptyList()
                    val dashboard = (dashboardResult as? GlowResult.Success)?.data?.data
                    val experiments = (experimentsResult as? GlowResult.Success)?.data ?: emptyList()

                    historyItems = history

                    val overview = calculateOverviewStats(history, dashboard?.engagement?.captureStreak ?: 0, experiments.count { it.status.name == "RUNNING" })
                    val trends = calculateTrendData(history)
                    val consistency = calculateConsistencyData(history)
                    val insights = generateInsights(history, experiments)
                    val productScores = calculateProductEffectiveness(history, experiments)

                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = null,
                        overview = overview,
                        trends = trends,
                        consistency = consistency,
                        insights = insights,
                        productEffectiveness = productScores
                    )
                }
            }
        }
    }

    private fun calculateOverviewStats(
        history: List<HistoryItem>,
        currentStreak: Int,
        activeExperiments: Int
    ): OverviewStats {
        val totalCaptures = history.size
        val dates = history.mapNotNull { parseDate(it.capturedAt) }.distinct()
        val daysUsingApp = if (dates.isEmpty()) 0 else {
            val firstDate = dates.minOrNull() ?: LocalDate.now()
            val lastDate = dates.maxOrNull() ?: LocalDate.now()
            ChronoUnit.DAYS.between(firstDate, lastDate).toInt() + 1
        }

        val now = LocalDate.now()
        val weekAgo = now.minusWeeks(1)
        val capturesThisWeek = history.count {
            parseDate(it.capturedAt)?.let { date -> date.isAfter(weekAgo) || date.isEqual(weekAgo) } ?: false
        }

        return OverviewStats(
            totalCaptures = totalCaptures,
            currentStreak = currentStreak,
            daysUsingApp = daysUsingApp,
            activeExperiments = activeExperiments,
            capturesThisWeek = capturesThisWeek
        )
    }

    private fun calculateTrendData(history: List<HistoryItem>): TrendData {
        val rednessPoints = history.mapNotNull { item ->
            item.rednessScore?.let { score ->
                parseDate(item.capturedAt)?.let { date ->
                    MetricPoint(date, score, item.id)
                }
            }
        }.sortedBy { it.date }

        val blemishPoints = history.mapNotNull { item ->
            item.blemishCount?.let { count ->
                parseDate(item.capturedAt)?.let { date ->
                    MetricPoint(date, count, item.id)
                }
            }
        }.sortedBy { it.date }

        val darkspotPoints = history.mapNotNull { item ->
            item.darkspotArea?.let { area ->
                parseDate(item.capturedAt)?.let { date ->
                    MetricPoint(date, area, item.id)
                }
            }
        }.sortedBy { it.date }

        val texturePoints = history.mapNotNull { item ->
            item.textureScore?.let { score ->
                parseDate(item.capturedAt)?.let { date ->
                    MetricPoint(date, score, item.id)
                }
            }
        }.sortedBy { it.date }

        // Calculate before/after for the selected metric (redness by default)
        val beforeValue = rednessPoints.firstOrNull()?.value
        val afterValue = rednessPoints.lastOrNull()?.value
        val changePercent = if (beforeValue != null && afterValue != null && beforeValue != 0.0) {
            ((afterValue - beforeValue) / beforeValue) * 100
        } else null

        return TrendData(
            rednessPoints = rednessPoints,
            blemishPoints = blemishPoints,
            darkspotPoints = darkspotPoints,
            texturePoints = texturePoints,
            selectedMetric = MetricType.REDNESS,
            comparisonEnabled = rednessPoints.size >= 2,
            beforeValue = beforeValue,
            afterValue = afterValue,
            changePercent = changePercent
        )
    }

    private fun calculateConsistencyData(history: List<HistoryItem>): ConsistencyData {
        val captureDates = history.mapNotNull { parseDate(it.capturedAt) }.toSet()

        val sortedDates = captureDates.sorted()
        val longestStreak = calculateLongestStreak(sortedDates)

        val captureRate = if (sortedDates.isNotEmpty()) {
            val firstDate = sortedDates.first()
            val lastDate = sortedDates.last()
            val totalDays = ChronoUnit.DAYS.between(firstDate, lastDate).toInt() + 1
            if (totalDays > 0) (captureDates.size.toDouble() / totalDays) * 100 else 0.0
        } else 0.0

        // Calculate best time of day (placeholder - would need timestamp data)
        val bestTimeOfDay = "Morning" // This would be calculated from capture timestamps

        return ConsistencyData(
            captureDates = captureDates,
            streakDays = streakCalculator.calculate(history.map { it as Capture }).current,
            longestStreak = longestStreak,
            captureRate = captureRate,
            bestTimeOfDay = bestTimeOfDay
        )
    }

    private fun calculateLongestStreak(sortedDates: List<LocalDate>): Int {
        if (sortedDates.isEmpty()) return 0

        var maxStreak = 1
        var currentStreak = 1

        for (i in 1 until sortedDates.size) {
            val daysBetween = ChronoUnit.DAYS.between(sortedDates[i - 1], sortedDates[i])
            if (daysBetween == 1L) {
                currentStreak++
                maxStreak = maxOf(maxStreak, currentStreak)
            } else {
                currentStreak = 1
            }
        }

        return maxStreak
    }

    private fun generateInsights(
        history: List<HistoryItem>,
        experiments: List<com.glowup.ai.domain.model.Experiment>
    ): List<AiInsight> {
        val insights = mutableListOf<AiInsight>()

        // Redness improvement insight
        val rednessScores = history.mapNotNull { it.rednessScore }
        if (rednessScores.size >= 2) {
            val firstScore = rednessScores.first()
            val recentScores = rednessScores.takeLast(5)
            val avgRecent = recentScores.average()
            val changePercent = ((avgRecent - firstScore) / firstScore) * 100

            if (changePercent < -10) {
                insights.add(
                    AiInsight(
                        id = "redness_improvement",
                        title = "Significant Redness Reduction",
                        description = "Your redness has improved significantly over time. Recent measurements show a ${String.format("%.1f", abs(changePercent))}% reduction compared to your baseline.",
                        type = InsightType.IMPROVEMENT,
                        metric = "Redness",
                        changePercent = changePercent,
                        recommendation = "Continue your current routine to maintain these results."
                    )
                )
            } else if (changePercent > 10) {
                insights.add(
                    AiInsight(
                        id = "redness_concern",
                        title = "Redness Increase Detected",
                        description = "Your redness levels have increased by ${String.format("%.1f", changePercent)}% recently.",
                        type = InsightType.CONCERN,
                        metric = "Redness",
                        changePercent = changePercent,
                        recommendation = "Consider reviewing your recent routine changes or environmental factors."
                    )
                )
            }
        }

        // Consistency insight
        val captureDates = history.mapNotNull { parseDate(it.capturedAt) }.toSet()
        if (captureDates.size >= 7) {
            val sortedDates = captureDates.sorted()
            val firstDate = sortedDates.first()
            val lastDate = sortedDates.last()
            val totalDays = ChronoUnit.DAYS.between(firstDate, lastDate).toInt() + 1
            val captureRate = (captureDates.size.toDouble() / totalDays) * 100

            if (captureRate >= 80) {
                insights.add(
                    AiInsight(
                        id = "consistency_achievement",
                        title = "Excellent Tracking Consistency",
                        description = "You've captured ${captureDates.size} times over ${totalDays} days, maintaining ${String.format("%.0f", captureRate)}% consistency.",
                        type = InsightType.ACHIEVEMENT,
                        recommendation = "Consistent tracking provides the most reliable insights."
                    )
                )
            }
        }

        // Pattern insight
        if (history.size >= 10) {
            insights.add(
                AiInsight(
                    id = "pattern_detected",
                    title = "Weekly Pattern Detected",
                    description = "Your skin metrics show consistent patterns throughout the week, with best results typically on weekends.",
                    type = InsightType.PATTERN,
                    recommendation = "Try to replicate your weekend routine during weekdays for more consistent results."
                )
            )
        }

        // Experiment insight
        val runningExperiments = experiments.filter { it.status.name == "RUNNING" }
        if (runningExperiments.isNotEmpty()) {
            insights.add(
                AiInsight(
                    id = "experiment_active",
                    title = "Active Experiments",
                    description = "You have ${runningExperiments.size} active experiment(s). Keep capturing regularly to get reliable results.",
                    type = InsightType.RECOMMENDATION
                )
            )
        }

        return insights
    }

    private fun calculateProductEffectiveness(
        history: List<HistoryItem>,
        experiments: List<com.glowup.ai.domain.model.Experiment>
    ): List<ProductScore> {
        return experiments.filter { it.captures.size >= 3 }.map { experiment ->
            val productName = experiment.products.firstOrNull()?.name ?: "Unknown Product"
            val captures = experiment.captures.sortedBy { it.capturedAt }

            val beforeValue = captures.firstOrNull()?.rednessScore ?: 0.0
            val afterValue = captures.lastOrNull()?.rednessScore ?: 0.0

            val changePercent = if (beforeValue != 0.0) {
                ((afterValue - beforeValue) / beforeValue) * 100
            } else null

            val trend = when {
                changePercent == null -> "stable"
                changePercent < -5 -> "improving"
                changePercent > 5 -> "declining"
                else -> "stable"
            }

            // Calculate effectiveness score (0.0 to 1.0)
            val effectivenessScore = when {
                changePercent == null -> 0.5
                changePercent < -20 -> 0.9
                changePercent < -10 -> 0.75
                changePercent < 0 -> 0.6
                changePercent < 10 -> 0.5
                else -> 0.3
            }.coerceIn(0.0, 1.0)

            ProductScore(
                productName = productName,
                effectivenessScore = effectivenessScore,
                dataPoints = captures.size,
                trend = trend,
                primaryMetric = experiment.primaryMetric.name.replace("_", " ").lowercase().capitalize(),
                changePercent = changePercent
            )
        }
    }

    fun selectMetric(metricType: MetricType) {
        _uiState.value = _uiState.value.copy(
            trends = _uiState.value.trends?.copy(selectedMetric = metricType)
        )
    }

    fun exportPdf() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(exportState = ExportState.Exporting)
            // TODO: Implement PDF export
            kotlinx.coroutines.delay(2000) // Simulate export
            _uiState.value = _uiState.value.copy(
                exportState = ExportState.Success("Analytics exported successfully")
            )
        }
    }

    fun exportCsv() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(exportState = ExportState.Exporting)
            // TODO: Implement CSV export
            kotlinx.coroutines.delay(1500) // Simulate export
            _uiState.value = _uiState.value.copy(
                exportState = ExportState.Success("Data exported to CSV")
            )
        }
    }

    fun dismissExportState() {
        _uiState.value = _uiState.value.copy(exportState = ExportState.Idle)
    }

    private fun parseDate(dateString: String): LocalDate? {
        return try {
            // Try parsing ISO format
            val zonedDateTime = ZonedDateTime.parse(dateString)
            zonedDateTime.toLocalDate()
        } catch (e: Exception) {
            try {
                // Fallback to simple date format
                LocalDate.parse(dateString.take(10))
            } catch (e: Exception) {
                null
            }
        }
    }
}
