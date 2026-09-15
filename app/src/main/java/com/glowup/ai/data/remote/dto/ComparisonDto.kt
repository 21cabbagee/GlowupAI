package com.glowup.ai.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Request and response contract for the server-owned qualitative comparison.
 * The server may add enum values over time, so the app keeps them as strings
 * and renders unknown values safely instead of failing deserialization. */
@Serializable
data class ComparisonCreateRequestDto(
    @SerialName("earlier_capture_id") val earlierCaptureId: String,
    @SerialName("later_capture_id") val laterCaptureId: String,
    val vertical: String = "skin",
)

@Serializable
data class ComparisonChangeDto(
    val region: String = "",
    val concern: String = "",
    val change: String = "",
    val description: String = "",
)

@Serializable
data class ComparisonResultDto(
    @SerialName("schema_version")
    val schemaVersion: String? = null,
    val comparable: Boolean = false,
    val reasons: List<String> = emptyList(),
    val changes: List<ComparisonChangeDto> = emptyList(),
    val limitations: List<String> = emptyList(),
)

@Serializable
data class ComparisonDatesDto(
    val earlier: String? = null,
    val later: String? = null,
)

@Serializable
data class ComparisonLanguageDto(
    val answer: String? = null,
    @SerialName("evidence_ids") val evidenceIds: List<String> = emptyList(),
    val limitations: List<String> = emptyList(),
)

@Serializable
data class ComparisonResponseDto(
    @SerialName("comparison_id") val comparisonId: String? = null,
    val status: String = "unavailable",
    @SerialName("earlier_capture_id") val earlierCaptureId: String? = null,
    @SerialName("later_capture_id") val laterCaptureId: String? = null,
    @SerialName("captured_at") val capturedAt: ComparisonDatesDto? = null,
    val comparison: ComparisonResultDto? = null,
    val comparable: Boolean = false,
    val reasons: List<String> = emptyList(),
    val changes: List<ComparisonChangeDto> = emptyList(),
    val language: ComparisonLanguageDto? = null,
    @SerialName("language_mode") val languageMode: String? = null,
)
