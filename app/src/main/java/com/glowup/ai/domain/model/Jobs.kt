package com.glowup.ai.domain.model

/**
 * `POST /reprocess` and `POST /shelf-scan` are async — they return
 * `{job_id, status:"queued"}` immediately. Never read a result off the POST
 * response; always poll the matching status route until a terminal state.
 */
data class ReprocessResult(
    val processedCount: Int?,
    val modelVersion: String?,
)

data class ReprocessJob(
    val jobId: String,
    val status: JobStatus,
    val result: ReprocessResult?,
    val error: String?,
)

data class ShelfScanCandidate(
    val name: String,
    val brand: String?,
    val category: String?,
    val ingredients: List<String>,
)

data class ShelfScanResult(
    val candidates: List<ShelfScanCandidate>,
    /** Explains an empty [candidates] list when the vision provider is not
     * configured server-side — always show a manual "add product" fallback. */
    val message: String?,
)

data class ShelfScanJob(
    val jobId: String,
    val status: JobStatus,
    val result: ShelfScanResult?,
    val error: String?,
)

data class ShelfScanSelection(
    val name: String,
    val category: String = "other",
    val ingredients: List<String> = emptyList(),
    val stabilizationDays: Int = 14,
)
