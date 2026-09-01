package com.glowup.ai.data.remote.dto

import com.glowup.ai.domain.model.EarlyStop
import com.glowup.ai.domain.model.Experiment
import com.glowup.ai.domain.model.ExperimentCreateRequest
import com.glowup.ai.domain.model.ExperimentProduct
import com.glowup.ai.domain.model.ExperimentStatus
import com.glowup.ai.domain.model.ExperimentStatusRequest
import com.glowup.ai.domain.model.PrimaryMetric
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ExperimentCreateRequestDto(
    @SerialName("user_id") val userId: String,
    val name: String,
    val hypothesis: String? = null,
    @SerialName("product_id") val productId: String,
    @SerialName("primary_metric") val primaryMetric: String = "redness_score",
    @SerialName("target_days") val targetDays: Int = 14,
)

fun ExperimentCreateRequest.toDto(): ExperimentCreateRequestDto = ExperimentCreateRequestDto(
    userId = userId,
    name = name,
    hypothesis = hypothesis,
    productId = productId,
    primaryMetric = primaryMetric.toWire(),
    targetDays = targetDays,
)

/** `status` uses the SAME path user id as a body field, purely so the
 * backend can 400 on a client-side mismatch before touching the DB. */
@Serializable
data class ExperimentStatusRequestDto(
    @SerialName("user_id") val userId: String,
    val status: String,
)

fun ExperimentStatusRequest.toDto(): ExperimentStatusRequestDto = ExperimentStatusRequestDto(
    userId = userId,
    status = when (status) {
        ExperimentStatus.PLANNED -> "planned"
        ExperimentStatus.RUNNING -> "running"
        ExperimentStatus.PAUSED -> "paused"
        ExperimentStatus.COMPLETED -> "completed"
        ExperimentStatus.CANCELLED -> "cancelled"
        ExperimentStatus.UNKNOWN -> throw IllegalArgumentException("ExperimentStatus.UNKNOWN cannot be sent to the server")
    },
)

@Serializable
data class ExperimentProductDto(
    val name: String = "",
    val category: String? = null,
    val role: String? = null,
)

@Serializable
data class EarlyStopDto(
    val conclusive: Boolean = false,
    @SerialName("recommended_status") val recommendedStatus: String? = null,
    val message: String = "",
)

fun EarlyStopDto.toDomain(): EarlyStop = EarlyStop(
    conclusive = conclusive,
    recommendedStatus = recommendedStatus?.let { ExperimentStatus.fromRaw(it) },
    message = message,
)

/** `start_at`/`end_at` are the real column names — never `started_at`. */
@Serializable
data class ExperimentDto(
    val id: String = "",
    val name: String = "",
    val hypothesis: String? = null,
    @SerialName("primary_metric") val primaryMetric: String = "redness_score",
    val status: String = "running",
    @SerialName("target_days") val targetDays: Int = 14,
    @SerialName("start_at") val startAt: String? = null,
    @SerialName("end_at") val endAt: String? = null,
    val products: List<ExperimentProductDto> = emptyList(),
    val events: List<RoutineEventDto> = emptyList(),
    val captures: List<HistoryItemDto> = emptyList(),
    @SerialName("early_stop") val earlyStop: EarlyStopDto? = null,
)

fun ExperimentDto.toDomain(): Experiment = Experiment(
    id = id,
    name = name,
    hypothesis = hypothesis,
    primaryMetric = PrimaryMetric.fromRaw(primaryMetric),
    status = ExperimentStatus.fromRaw(status),
    targetDays = targetDays,
    startAt = startAt,
    endAt = endAt,
    products = products.map { ExperimentProduct(it.name, it.category, it.role) },
    events = events.map { it.toDomain() },
    captures = captures.map { it.toDomain() },
    earlyStop = earlyStop?.toDomain(),
)
