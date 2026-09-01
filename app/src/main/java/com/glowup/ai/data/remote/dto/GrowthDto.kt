package com.glowup.ai.data.remote.dto

import com.glowup.ai.domain.model.BudgetFlaggedProduct
import com.glowup.ai.domain.model.BudgetOptimizer
import com.glowup.ai.domain.model.ContextEvent
import com.glowup.ai.domain.model.ContextEventCreateRequest
import com.glowup.ai.domain.model.ContextEventType
import com.glowup.ai.domain.model.DermExport
import com.glowup.ai.domain.model.RootCauseInsight
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ContextEventCreateRequestDto(
    @SerialName("event_type") val eventType: String,
    val value: String? = null,
    @SerialName("occurred_at") val occurredAt: String? = null,
    val notes: String? = null,
)

fun ContextEventCreateRequest.toDto(): ContextEventCreateRequestDto =
    ContextEventCreateRequestDto(
        eventType = eventType.toWire(),
        value = value,
        occurredAt = occurredAt,
        notes = notes,
    )

@Serializable
data class ContextEventDto(
    val id: String,
    @SerialName("event_type") val eventType: String = "custom",
    val value: String? = null,
    @SerialName("occurred_at") val occurredAt: String = "",
    val notes: String? = null,
)

fun ContextEventDto.toDomain(): ContextEvent =
    ContextEvent(
        id = id,
        eventType = ContextEventType.fromRaw(eventType),
        value = value,
        occurredAt = occurredAt,
        notes = notes,
    )

@Serializable
data class RootCauseInsightDto(
    @SerialName("event_type") val eventType: String = "custom",
    val occurrences: Int = 0,
    @SerialName("normalized_effect") val normalizedEffect: Double = 0.0,
    val metric: String = "",
    val message: String = "",
)

fun RootCauseInsightDto.toDomain(): RootCauseInsight =
    RootCauseInsight(
        eventType = ContextEventType.fromRaw(eventType),
        occurrences = occurrences,
        normalizedEffect = normalizedEffect,
        metric = metric,
        message = message,
    )

@Serializable
data class BudgetFlaggedProductDto(
    @SerialName("product_id") val productId: String,
    @SerialName("product_name") val productName: String,
    @SerialName("days_stable") val daysStable: Int = 0,
    @SerialName("estimated_annual_cost_cents") val estimatedAnnualCostCents: Int? = null,
    val currency: String = "USD",
    val reason: String = "",
)

@Serializable
data class BudgetOptimizerDto(
    val flagged: List<BudgetFlaggedProductDto> = emptyList(),
    @SerialName("estimated_annual_waste_cents") val estimatedAnnualWasteCents: Int = 0,
    val currency: String = "USD",
    val disclaimer: String = "",
)

fun BudgetOptimizerDto.toDomain(): BudgetOptimizer =
    BudgetOptimizer(
        flagged =
            flagged.map {
                BudgetFlaggedProduct(it.productId, it.productName, it.daysStable, it.estimatedAnnualCostCents, it.currency, it.reason)
            },
        estimatedAnnualWasteCents = estimatedAnnualWasteCents,
        currency = currency,
        disclaimer = disclaimer,
    )

@Serializable
data class DermExportDto(
    @SerialName("generated_at") val generatedAt: String = "",
    @SerialName("capture_count") val captureCount: Int = 0,
    @SerialName("model_versions") val modelVersions: List<String> = emptyList(),
    val verdicts: List<VerdictDto> = emptyList(),
    @SerialName("printable_html") val printableHtml: String = "",
    val disclaimer: String = "",
)

fun DermExportDto.toDomain(): DermExport =
    DermExport(
        generatedAt = generatedAt,
        captureCount = captureCount,
        modelVersions = modelVersions,
        verdicts = verdicts.map { it.toDomain() },
        printableHtml = printableHtml,
        disclaimer = disclaimer,
    )
