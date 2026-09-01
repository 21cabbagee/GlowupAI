package com.glowup.ai.data.remote.dto

import com.glowup.ai.domain.model.JobStatus
import com.glowup.ai.domain.model.ReprocessJob
import com.glowup.ai.domain.model.ReprocessResult
import com.glowup.ai.domain.model.ShelfScanCandidate
import com.glowup.ai.domain.model.ShelfScanJob
import com.glowup.ai.domain.model.ShelfScanResult
import com.glowup.ai.domain.model.ShelfScanSelection
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ReprocessCreateRequestDto(
    @SerialName("model_version") val modelVersion: String,
)

/** Immediate response to `POST /reprocess` and `POST /shelf-scan` — always
 * `{"job_id","status":"queued"}`. Never treat this as the finished result;
 * poll the matching status route below. */
@Serializable
data class JobQueuedResponseDto(
    @SerialName("job_id") val jobId: String,
    val status: String = "queued",
)

@Serializable
data class ReprocessResultDto(
    @SerialName("processed_count") val processedCount: Int? = null,
    @SerialName("model_version") val modelVersion: String? = null,
)

@Serializable
data class ReprocessJobDto(
    @SerialName("job_id") val jobId: String? = null,
    val status: String = "queued",
    val result: ReprocessResultDto? = null,
    val error: String? = null,
)

fun ReprocessJobDto.toDomain(fallbackJobId: String): ReprocessJob =
    ReprocessJob(
        jobId = jobId ?: fallbackJobId,
        status = JobStatus.fromRaw(status),
        result = result?.let { ReprocessResult(it.processedCount, it.modelVersion) },
        error = error,
    )

@Serializable
data class ShelfScanCreateRequestDto(
    @SerialName("image_base64") val imageBase64: String,
)

@Serializable
data class ShelfScanCandidateDto(
    val name: String = "",
    val brand: String? = null,
    val category: String? = null,
    val ingredients: List<String> = emptyList(),
)

@Serializable
data class ShelfScanResultDto(
    val candidates: List<ShelfScanCandidateDto> = emptyList(),
    val message: String? = null,
)

@Serializable
data class ShelfScanJobDto(
    @SerialName("job_id") val jobId: String? = null,
    val status: String = "queued",
    val result: ShelfScanResultDto? = null,
    val error: String? = null,
)

fun ShelfScanJobDto.toDomain(fallbackJobId: String): ShelfScanJob =
    ShelfScanJob(
        jobId = jobId ?: fallbackJobId,
        status = JobStatus.fromRaw(status),
        result =
            result?.let { r ->
                ShelfScanResult(
                    candidates = r.candidates.map { ShelfScanCandidate(it.name, it.brand, it.category, it.ingredients) },
                    message = r.message,
                )
            },
        error = error,
    )

@Serializable
data class ShelfScanSelectionDto(
    val name: String,
    val category: String = "other",
    val ingredients: List<String> = emptyList(),
    @SerialName("stabilization_days") val stabilizationDays: Int = 14,
)

fun ShelfScanSelection.toDto(): ShelfScanSelectionDto = ShelfScanSelectionDto(name, category, ingredients, stabilizationDays)

@Serializable
data class ShelfScanConfirmRequestDto(
    val selections: List<ShelfScanSelectionDto>,
)
