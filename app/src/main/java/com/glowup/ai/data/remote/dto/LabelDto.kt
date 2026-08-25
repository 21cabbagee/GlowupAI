package com.glowup.ai.data.remote.dto

import com.glowup.ai.domain.model.Label
import com.glowup.ai.domain.model.LabelCreateRequest
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LabelCreateRequestDto(
    @SerialName("photo_id") val photoId: String,
    @SerialName("label_type") val labelType: String,
    val value: String,
    val confidence: Double? = null,
    val notes: String? = null,
)

fun LabelCreateRequest.toDto(): LabelCreateRequestDto = LabelCreateRequestDto(photoId, labelType, value, confidence, notes)

@Serializable
data class LabelDto(
    val id: String,
    @SerialName("photo_id") val photoId: String,
    @SerialName("label_type") val labelType: String = "user_note",
    val value: String = "",
    val confidence: Double? = null,
    val notes: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
)

fun LabelDto.toDomain(): Label = Label(id, photoId, labelType, value, confidence, notes, createdAt)
