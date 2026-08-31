package com.glowup.ai.feature.analytics

import com.glowup.ai.domain.model.HistoryItem
import com.glowup.ai.domain.model.Experiment
import java.time.LocalDate

data class AnalyticsUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val overview: OverviewStats? = null,
    val trends: TrendData? = null,
    val consistency: ConsistencyData? = null,
    val insights: List<AiInsight> = emptyList(),
    val productEffectiveness: List<ProductScore> = emptyList(),
    val exportState: ExportState = ExportState.Idle,
)

data class OverviewStats(
    val totalCaptures: Int,
    val currentStreak: Int,
    val daysUsingApp: Int,
    val activeExperiments: Int,
    val streakChange: Int? = null,
    val capturesThisWeek: Int = 0,
)

data class TrendData(
    val rednessPoints: List<MetricPoint>,
    val blemishPoints: List<MetricPoint>,
    val darkspotPoints: List<MetricPoint>,
    val texturePoints: List<MetricPoint>,
    val selectedMetric: MetricType = MetricType.REDNESS,
    val comparisonEnabled: Boolean = false,
    val beforeValue: Double? = null,
    val afterValue: Double? = null,
    val changePercent: Double? = null,
)

data class MetricPoint(
    val date: LocalDate,
    val value: Double,
    val captureId: String,
)

enum class MetricType {
    REDNESS, BLEMISH, DARKSPOT, TEXTURE
}

data class ConsistencyData(
    val captureDates: Set<LocalDate>,
    val streakDays: Int,
    val longestStreak: Int,
    val captureRate: Double, // percentage of days with captures
    val bestTimeOfDay: String? = null,
)

data class AiInsight(
    val id: String,
    val title: String,
    val description: String,
    val type: InsightType,
    val metric: String? = null,
    val changePercent: Double? = null,
    val recommendation: String? = null,
)

enum class InsightType {
    IMPROVEMENT, CONCERN, PATTERN, ACHIEVEMENT, RECOMMENDATION
}

data class ProductScore(
    val productName: String,
    val effectivenessScore: Double, // 0.0 to 1.0
    val dataPoints: Int,
    val trend: String, // "improving", "declining", "stable"
    val primaryMetric: String,
    val changePercent: Double?,
)

sealed interface ExportState {
    data object Idle : ExportState
    data object Exporting : ExportState
    data class Success(val message: String) : ExportState
    data class Error(val message: String) : ExportState
}
