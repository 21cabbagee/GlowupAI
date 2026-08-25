package com.glowup.ai.domain.model

data class ExperimentProduct(
    val name: String,
    val category: String?,
    val role: String?,
)

data class EarlyStop(
    val conclusive: Boolean,
    val recommendedStatus: ExperimentStatus?,
    val message: String,
)

/** Experiments are stored with `start_at`/`end_at`, never `started_at` — do
 * not rename these fields back to the web client's mistaken spelling. */
data class Experiment(
    val id: String,
    val name: String,
    val hypothesis: String?,
    val primaryMetric: PrimaryMetric,
    val status: ExperimentStatus,
    val targetDays: Int,
    val startAt: String?,
    val endAt: String?,
    val products: List<ExperimentProduct>,
    val events: List<RoutineEvent>,
    val captures: List<HistoryItem>,
    val earlyStop: EarlyStop?,
)

data class ExperimentCreateRequest(
    val userId: String,
    val name: String,
    val hypothesis: String? = null,
    val productId: String,
    val primaryMetric: PrimaryMetric = PrimaryMetric.REDNESS_SCORE,
    val targetDays: Int = 14,
)

data class ExperimentStatusRequest(
    val userId: String,
    val status: ExperimentStatus,
)
