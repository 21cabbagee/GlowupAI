package com.glowup.ai.domain.model

data class Citation(
    val type: String,
    val date: String?,
    val id: String?,
)

/** Persist [threadId] across turns and send it on the next question — the
 * web client discards it and starts a new thread every time (bug carried
 * forward from ANDROID_PLAN.md section 3, item 5). */
data class QnaAnswer(
    val threadId: String,
    val answer: String,
    val scope: SafetyScope,
    val citations: List<Citation>,
)

data class QnaMessage(
    val role: String,
    val content: String,
    val createdAt: String?,
    val scope: SafetyScope?,
    val citations: List<Citation>,
    val threadId: String?,
)

data class TriageResult(
    val scope: SafetyScope,
    val message: String,
    val matchedTerms: List<String>,
)

data class DiscoverRecommendation(
    val productId: String,
    val name: String,
    val category: String?,
    val sampleSize: Int,
    val averageEffect: Double,
    val reason: String,
)

data class Discover(
    val recommendations: List<DiscoverRecommendation>,
    val minimumCohortSize: Int,
    val disclaimer: String,
)

data class Label(
    val id: String,
    val photoId: String,
    val labelType: String,
    val value: String,
    val confidence: Double?,
    val notes: String?,
    val createdAt: String?,
)

data class LabelCreateRequest(
    val photoId: String,
    val labelType: String,
    val value: String,
    val confidence: Double? = null,
    val notes: String? = null,
)

data class ContextEvent(
    val id: String,
    val eventType: ContextEventType,
    val value: String?,
    val occurredAt: String,
    val notes: String?,
)

data class ContextEventCreateRequest(
    val eventType: ContextEventType,
    val value: String? = null,
    val occurredAt: String? = null,
    val notes: String? = null,
)

data class RootCauseInsight(
    val eventType: ContextEventType,
    val occurrences: Int,
    val normalizedEffect: Double,
    val metric: String,
    val message: String,
)

data class BudgetFlaggedProduct(
    val productId: String,
    val productName: String,
    val daysStable: Int,
    /** `null` when no offer price is on file — show the product flagged
     * without a figure rather than hiding it. */
    val estimatedAnnualCostCents: Int?,
    val currency: String,
    val reason: String,
)

data class BudgetOptimizer(
    val flagged: List<BudgetFlaggedProduct>,
    val estimatedAnnualWasteCents: Int,
    val currency: String,
    val disclaimer: String,
)

data class DermExport(
    val generatedAt: String,
    val captureCount: Int,
    val modelVersions: List<String>,
    val verdicts: List<Verdict>,
    /** Plain HTML string meant to be opened for print/share — not a
     * downloadable file the backend generates. */
    val printableHtml: String,
    val disclaimer: String,
)
