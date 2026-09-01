package com.glowup.ai.data.remote.dto

import com.glowup.ai.data.remote.NetworkJson
import com.glowup.ai.domain.model.IngredientExplainer
import com.glowup.ai.domain.model.Offer
import com.glowup.ai.domain.model.Product
import com.glowup.ai.domain.model.ProductCreateRequest
import com.glowup.ai.domain.model.ProductDetail
import com.glowup.ai.domain.model.ProductOverlap
import com.glowup.ai.domain.model.ProductPrediction
import com.glowup.ai.domain.model.PurchaseGuidance
import com.glowup.ai.domain.model.PurchaseGuidanceRequest
import com.glowup.ai.domain.model.ReviewedIngredient
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer

/**
 * WRITE shape for `POST /api/products`. The backend field accepts either a
 * JSON array or a free-text string that it re-splits on `,`/`;`; this client
 * always sends the comma-joined string form on write. This is intentionally
 * NOT the same type as the READ shape ([ProductDto]/[Product]) which exposes
 * `ingredients_json` — see ANDROID_PLAN.md section 3, item 4.
 */
@Serializable
data class ProductCreateRequestDto(
    val name: String,
    val barcode: String? = null,
    val category: String = "other",
    val ingredients: String? = null,
    @SerialName("stabilization_days") val stabilizationDays: Int = 14,
)

fun ProductCreateRequest.toDto(): ProductCreateRequestDto =
    ProductCreateRequestDto(
        name = name,
        barcode = barcode,
        category = category,
        ingredients = ingredients.takeIf { it.isNotEmpty() }?.joinToString(","),
        stabilizationDays = stabilizationDays,
    )

@Serializable
data class ProductDto(
    val id: String,
    val name: String,
    val category: String = "other",
    val barcode: String? = null,
    @SerialName("ingredients_json") val ingredientsJson: String? = null,
    @SerialName("stabilization_days") val stabilizationDays: Int = 14,
    @SerialName("created_at") val createdAt: String? = null,
)

/** `ingredients_json` is a JSON-array-encoded STRING on read — parse it once,
 * here, and nowhere else. Falls back to an empty list for any malformed or
 * missing value instead of throwing. */
fun parseIngredientsJson(raw: String?): List<String> {
    if (raw.isNullOrBlank()) return emptyList()
    return runCatching {
        NetworkJson.decodeFromString(ListSerializer(String.serializer()), raw)
    }.getOrElse {
        raw.split(",", ";").map { it.trim() }.filter { it.isNotEmpty() }
    }
}

fun ProductDto.toDomain(): Product =
    Product(
        id = id,
        name = name,
        category = category,
        barcode = barcode,
        ingredients = parseIngredientsJson(ingredientsJson),
        stabilizationDays = stabilizationDays,
        createdAt = createdAt,
    )

@Serializable
data class OfferDto(
    val id: String,
    @SerialName("product_id") val productId: String? = null,
    @SerialName("product_name") val productName: String? = null,
    val merchant: String,
    val url: String,
    @SerialName("price_cents") val priceCents: Int? = null,
    val currency: String = "USD",
    val disclosed: Boolean = true,
    val active: Boolean = true,
)

fun OfferDto.toDomain(): Offer =
    Offer(
        id = id,
        productId = productId,
        productName = productName,
        merchant = merchant,
        url = url,
        priceCents = priceCents,
        currency = currency,
        disclosed = disclosed,
        active = active,
    )

@Serializable
data class ReviewedIngredientDto(
    val ingredient: String,
    val purpose: String? = null,
    val caution: String? = null,
)

@Serializable
data class IngredientExplainerDto(
    @SerialName("product_name") val productName: String,
    val reviewed: List<ReviewedIngredientDto> = emptyList(),
    val unknown: List<String> = emptyList(),
)

fun IngredientExplainerDto.toDomain(): IngredientExplainer =
    IngredientExplainer(
        productName = productName,
        reviewed = reviewed.map { ReviewedIngredient(it.ingredient, it.purpose, it.caution) },
        unknown = unknown,
    )

@Serializable
data class ProductDetailDto(
    val id: String,
    val name: String,
    val category: String = "other",
    val barcode: String? = null,
    @SerialName("ingredients_json") val ingredientsJson: String? = null,
    @SerialName("stabilization_days") val stabilizationDays: Int = 14,
    @SerialName("created_at") val createdAt: String? = null,
    val offers: List<OfferDto> = emptyList(),
    @SerialName("ingredient_analysis") val ingredientAnalysis: IngredientExplainerDto? = null,
)

fun ProductDetailDto.toDomain(): ProductDetail =
    ProductDetail(
        product =
            Product(
                id = id,
                name = name,
                category = category,
                barcode = barcode,
                ingredients = parseIngredientsJson(ingredientsJson),
                stabilizationDays = stabilizationDays,
                createdAt = createdAt,
            ),
        offers = offers.map { it.toDomain() },
        ingredientAnalysis = ingredientAnalysis?.toDomain(),
    )

@Serializable
data class ProductOverlapDto(
    @SerialName("product_name") val productName: String,
    @SerialName("shared_ingredients") val sharedIngredients: List<String> = emptyList(),
)

fun ProductOverlapDto.toDomain(): ProductOverlap = ProductOverlap(productName, sharedIngredients)

@Serializable
data class ProductPredictionDto(
    @SerialName("product_id") val productId: String,
    @SerialName("product_name") val productName: String,
    val ingredients: List<String> = emptyList(),
    @SerialName("overlap_with_investigate") val overlapWithInvestigate: List<ProductOverlapDto> = emptyList(),
    @SerialName("overlap_with_likely_useful") val overlapWithLikelyUseful: List<ProductOverlapDto> = emptyList(),
    @SerialName("cohort_overlap") val cohortOverlap: List<ProductOverlapDto> = emptyList(),
    val headline: String,
    val disclaimer: String,
)

fun ProductPredictionDto.toDomain(): ProductPrediction =
    ProductPrediction(
        productId = productId,
        productName = productName,
        ingredients = ingredients,
        overlapWithInvestigate = overlapWithInvestigate.map { it.toDomain() },
        overlapWithLikelyUseful = overlapWithLikelyUseful.map { it.toDomain() },
        cohortOverlap = cohortOverlap.map { it.toDomain() },
        headline = headline,
        disclaimer = disclaimer,
    )

@Serializable
data class PurchaseGuidanceRequestDto(
    val name: String? = null,
    val barcode: String? = null,
    val category: String = "other",
    val ingredients: String? = null,
    @SerialName("price_cents") val priceCents: Int? = null,
    val currency: String = "INR",
)

fun PurchaseGuidanceRequest.toDto(): PurchaseGuidanceRequestDto =
    PurchaseGuidanceRequestDto(
        name = name,
        barcode = barcode,
        category = category,
        ingredients = ingredients.takeIf { it.isNotEmpty() }?.joinToString(","),
        priceCents = priceCents,
        currency = currency,
    )

@Serializable
data class PurchaseGuidanceDto(
    @SerialName("product_id") val productId: String? = null,
    @SerialName("product_name") val productName: String,
    val barcode: String? = null,
    val ingredients: List<String> = emptyList(),
    val signal: String? = null,
    val headline: String,
    @SerialName("next_action") val nextAction: String? = null,
    @SerialName("overlap_with_investigate") val overlapWithInvestigate: List<ProductOverlapDto> = emptyList(),
    @SerialName("overlap_with_likely_useful") val overlapWithLikelyUseful: List<ProductOverlapDto> = emptyList(),
    @SerialName("cohort_overlap") val cohortOverlap: List<ProductOverlapDto> = emptyList(),
    @SerialName("estimated_annual_cost_cents") val estimatedAnnualCostCents: Int? = null,
    val currency: String = "INR",
    val disclaimer: String,
)

fun PurchaseGuidanceDto.toDomain(): PurchaseGuidance =
    PurchaseGuidance(
        productId = productId,
        productName = productName,
        barcode = barcode,
        ingredients = ingredients,
        signal = signal,
        headline = headline,
        nextAction = nextAction,
        overlapWithInvestigate = overlapWithInvestigate.map { it.toDomain() },
        overlapWithLikelyUseful = overlapWithLikelyUseful.map { it.toDomain() },
        cohortOverlap = cohortOverlap.map { it.toDomain() },
        estimatedAnnualCostCents = estimatedAnnualCostCents,
        currency = currency,
        disclaimer = disclaimer,
    )
