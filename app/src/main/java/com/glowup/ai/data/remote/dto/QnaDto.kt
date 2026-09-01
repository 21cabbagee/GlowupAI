package com.glowup.ai.data.remote.dto

import com.glowup.ai.domain.model.Citation
import com.glowup.ai.domain.model.QnaAnswer
import com.glowup.ai.domain.model.QnaMessage
import com.glowup.ai.domain.model.SafetyScope
import com.glowup.ai.domain.model.TriageResult
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class QnaCreateRequestDto(
    val question: String,
    @SerialName("thread_id") val threadId: String? = null,
)

@Serializable
data class CitationDto(
    val type: String = "capture",
    val date: String? = null,
    val id: String? = null,
)

fun CitationDto.toDomain(): Citation = Citation(type, date, id)

/** Backend field is `scope`, never `route` — do not resurrect the web
 * client's mistaken field name here. */
@Serializable
data class QnaResponseDto(
    @SerialName("thread_id") val threadId: String,
    val answer: String,
    val scope: String = "cosmetic_tracking",
    val citations: List<CitationDto> = emptyList(),
)

fun QnaResponseDto.toDomain(): QnaAnswer =
    QnaAnswer(
        threadId = threadId,
        answer = answer,
        scope = SafetyScope.fromRaw(scope),
        citations = citations.map { it.toDomain() },
    )

@Serializable
data class QnaMessageDto(
    val role: String = "assistant",
    val content: String = "",
    @SerialName("created_at") val createdAt: String? = null,
    val scope: String? = null,
    val citations: List<CitationDto> = emptyList(),
    @SerialName("thread_id") val threadId: String? = null,
)

fun QnaMessageDto.toDomain(): QnaMessage =
    QnaMessage(
        role = role,
        content = content,
        createdAt = createdAt,
        scope = scope?.let { SafetyScope.fromRaw(it) },
        citations = citations.map { it.toDomain() },
        threadId = threadId,
    )

@Serializable
data class TriageCreateRequestDto(
    val text: String,
)

@Serializable
data class TriageResultDto(
    val scope: String = "cosmetic_tracking",
    val message: String = "",
    @SerialName("matched_terms") val matchedTerms: List<String> = emptyList(),
)

fun TriageResultDto.toDomain(): TriageResult =
    TriageResult(
        scope = SafetyScope.fromRaw(scope),
        message = message,
        matchedTerms = matchedTerms,
    )
