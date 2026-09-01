package com.glowup.ai.data.remote.dto

import com.glowup.ai.domain.model.Discover
import com.glowup.ai.domain.model.DiscoverRecommendation
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DiscoverRecommendationDto(
    @SerialName("product_id") val productId: String,
    val name: String,
    val category: String? = null,
    @SerialName("sample_size") val sampleSize: Int = 0,
    @SerialName("average_effect") val averageEffect: Double = 0.0,
    val reason: String = "",
)

@Serializable
data class DiscoverDto(
    val recommendations: List<DiscoverRecommendationDto> = emptyList(),
    @SerialName("minimum_cohort_size") val minimumCohortSize: Int = 3,
    val disclaimer: String = "",
)

fun DiscoverDto.toDomain(): Discover =
    Discover(
        recommendations =
            recommendations.map {
                DiscoverRecommendation(it.productId, it.name, it.category, it.sampleSize, it.averageEffect, it.reason)
            },
        minimumCohortSize = minimumCohortSize,
        disclaimer = disclaimer,
    )
