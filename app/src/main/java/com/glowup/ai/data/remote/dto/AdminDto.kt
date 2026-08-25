package com.glowup.ai.data.remote.dto

import com.glowup.ai.domain.model.AdminAuditEntry
import com.glowup.ai.domain.model.AdminMeasurementFeedbackSummary
import com.glowup.ai.domain.model.AdminOfferCreateRequest
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * NOT FOR APP USE — see [com.glowup.ai.domain.model.AdminAuditEntry] doc.
 * These DTOs exist only for route-coverage completeness in [com.glowup.ai.data.remote.GlowUpApi];
 * no repository should ever reference them.
 */
@Serializable
data class AdminAuditEntryDto(
    val id: String,
    val action: String = "",
    @SerialName("actor_type") val actorType: String? = null,
    @SerialName("actor_id") val actorId: String? = null,
    @SerialName("subject_type") val subjectType: String? = null,
    @SerialName("subject_id") val subjectId: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
)

fun AdminAuditEntryDto.toDomain(): AdminAuditEntry = AdminAuditEntry(id, action, actorType, actorId, subjectType, subjectId, createdAt)

@Serializable
data class AdminOfferCreateRequestDto(
    @SerialName("product_id") val productId: String,
    val merchant: String,
    val url: String,
    @SerialName("price_cents") val priceCents: Int? = null,
    val currency: String = "USD",
)

fun AdminOfferCreateRequest.toDto(): AdminOfferCreateRequestDto = AdminOfferCreateRequestDto(productId, merchant, url, priceCents, currency)

@Serializable
data class AdminMeasurementFeedbackSummaryDto(
    val counts: Map<String, Int> = emptyMap(),
    val total: Int = 0,
    val note: String = "",
)

fun AdminMeasurementFeedbackSummaryDto.toDomain(): AdminMeasurementFeedbackSummary =
    AdminMeasurementFeedbackSummary(counts, total, note)
