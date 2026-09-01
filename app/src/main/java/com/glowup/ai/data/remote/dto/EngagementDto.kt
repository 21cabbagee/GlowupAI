package com.glowup.ai.data.remote.dto

import com.glowup.ai.domain.model.Analytics
import com.glowup.ai.domain.model.Engagement
import com.glowup.ai.domain.model.EngagementEventRequest
import com.glowup.ai.domain.model.MetricSummary
import com.glowup.ai.domain.model.Reminder
import com.glowup.ai.domain.model.WeeklyRecap
import com.glowup.ai.domain.model.WeeklyRecapPeriod
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

@Serializable
data class ReminderDto(
    val id: String = "",
    val kind: String = "capture",
    @SerialName("next_at") val nextAt: String? = null,
    @Serializable(with = IntBooleanSerializer::class) val enabled: Boolean = true,
    @SerialName("cadence_days") val cadenceDays: Int? = null,
    @SerialName("last_sent_at") val lastSentAt: String? = null,
)

fun ReminderDto.toDomain(): Reminder = Reminder(id, kind, nextAt, enabled, cadenceDays, lastSentAt)

/** `GET /engagement` writes a reminder row as a side effect — never poll it. */
@Serializable
data class EngagementDto(
    @SerialName("capture_streak") val captureStreak: Int = 0,
    @SerialName("capture_count") val captureCount: Int = 0,
    @SerialName("capture_days") val captureDays: List<String> = emptyList(),
    val guide: CaptureGuideDto? = null,
    val reminders: List<ReminderDto> = emptyList(),
)

fun EngagementDto.toDomain(): Engagement =
    Engagement(
        captureStreak = captureStreak,
        captureCount = captureCount,
        captureDays = captureDays,
        guide = guide?.toDomain(),
        reminders = reminders.map { it.toDomain() },
    )

@Serializable
data class EngagementEventRequestDto(
    @SerialName("event_type") val eventType: String,
    @SerialName("reference_id") val referenceId: String? = null,
    val metadata: Map<String, String>? = null,
)

fun EngagementEventRequest.toDto(): EngagementEventRequestDto = EngagementEventRequestDto(eventType, referenceId, metadata)

@Serializable
data class MetricSummaryDto(
    val metric: String = "",
    val label: String = "",
    val direction: String = "",
    val delta: Double? = null,
    @SerialName("noise_floor") val noiseFloor: Double? = null,
    val sentence: String? = null,
)

@Serializable
data class WeeklyRecapPeriodDto(
    val start: String? = null,
    val end: String? = null,
)

@Serializable
data class WeeklyRecapDto(
    val status: String = "baseline_needed",
    val headline: String = "",
    val body: String = "",
    @SerialName("next_action") val nextAction: String? = null,
    @SerialName("capture_count") val captureCount: Int = 0,
    @SerialName("total_capture_count") val totalCaptureCount: Int? = null,
    @SerialName("check_in_count") val checkInCount: Int = 0,
    @SerialName("comparison_mode") val comparisonMode: String? = null,
    @SerialName("confidence_label") val confidenceLabel: String = "",
    @SerialName("metric_summaries") val metricSummaries: List<MetricSummaryDto> = emptyList(),
    val period: WeeklyRecapPeriodDto = WeeklyRecapPeriodDto(),
    val disclaimer: String = "",
)

fun WeeklyRecapDto.toDomain(): WeeklyRecap =
    WeeklyRecap(
        status = status,
        headline = headline,
        body = body,
        nextAction = nextAction,
        captureCount = captureCount,
        totalCaptureCount = totalCaptureCount,
        checkInCount = checkInCount,
        comparisonMode = comparisonMode,
        confidenceLabel = confidenceLabel,
        metricSummaries = metricSummaries.map { MetricSummary(it.metric, it.label, it.direction, it.delta, it.noiseFloor, it.sentence) },
        period = WeeklyRecapPeriod(period.start, period.end),
        disclaimer = disclaimer,
    )

@Serializable
data class AnalyticsDto(
    val activation: Boolean? = null,
    @SerialName("baseline_capture") val baselineCapture: Boolean? = null,
    @SerialName("first_three_captures") val firstThreeCaptures: Boolean? = null,
    @SerialName("median_history_days") val medianHistoryDays: Double? = null,
    @SerialName("weekly_verdict_open_rate") val weeklyVerdictOpenRate: Double? = null,
    @SerialName("verdict_action_rate") val verdictActionRate: Double? = null,
    @SerialName("evidence_unclear_engagement_rate") val evidenceUnclearEngagementRate: Double? = null,
    /** Complete backend returns an integer count here, while older deployments
     * returned an array of raw event rows. Accept both shapes. */
    @SerialName("raw_events") val rawEvents: JsonElement? = null,
)

fun AnalyticsDto.toDomain(): Analytics =
    Analytics(
        activation = activation?.toString(),
        baselineCapture = baselineCapture,
        firstThreeCaptures = firstThreeCaptures,
        medianHistoryDays = medianHistoryDays,
        weeklyVerdictOpenRate = weeklyVerdictOpenRate,
        verdictActionRate = verdictActionRate,
        evidenceUnclearEngagementRate = evidenceUnclearEngagementRate,
        rawEvents = rawEvents.asEventRows(),
        rawEventCount =
            when (val value = rawEvents) {
                is JsonPrimitive -> value.contentOrNull?.toIntOrNull()
                is JsonArray -> value.size
                else -> null
            },
    )

private fun JsonElement?.asEventRows(): List<Map<String, String>> =
    (this as? JsonArray)?.mapNotNull { element ->
        (element as? kotlinx.serialization.json.JsonObject)?.mapValues { (_, value) ->
            (value as? JsonPrimitive)?.contentOrNull.orEmpty()
        }
    } ?: emptyList()
