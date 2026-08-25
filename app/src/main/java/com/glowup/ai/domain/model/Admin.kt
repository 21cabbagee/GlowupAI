package com.glowup.ai.domain.model

/**
 * NOT FOR APP USE. The `/api/admin` routes require `Authorization: Bearer
 * <SKINPROOF_ADMIN_TOKEN>` — a secret the app must never hold. These models
 * exist only so [com.glowup.ai.data.remote.GlowUpApi] can declare complete
 * route coverage per ANDROID_PLAN.md Task 2.3. No repository may call the
 * corresponding endpoints; there is deliberately no repository-facing mapper
 * for them beyond this file.
 */
data class AdminAuditEntry(
    val id: String,
    val action: String,
    val actorType: String?,
    val actorId: String?,
    val subjectType: String?,
    val subjectId: String?,
    val createdAt: String?,
)

data class AdminOfferCreateRequest(
    val productId: String,
    val merchant: String,
    val url: String,
    val priceCents: Int? = null,
    val currency: String = "USD",
)

data class AdminMeasurementFeedbackSummary(
    val counts: Map<String, Int>,
    val total: Int,
    val note: String,
)
