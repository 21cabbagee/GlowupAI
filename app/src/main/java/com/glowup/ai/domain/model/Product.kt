package com.glowup.ai.domain.model

/**
 * Backend rows return `ingredients_json` — a JSON-encoded string on READ.
 * The mapper parses it once here. On WRITE (`POST /api/products`), the
 * backend instead wants a comma-joined string, which is why the write path
 * uses [ProductCreateRequest] and never this type.
 */
data class Product(
    val id: String,
    val name: String,
    val category: String,
    val barcode: String?,
    val ingredients: List<String>,
    val stabilizationDays: Int,
    val createdAt: String?,
)

data class ProductCreateRequest(
    val name: String,
    val barcode: String? = null,
    val category: String = "other",
    /** Comma-joined on the wire — see [Product] doc for the read/write asymmetry. */
    val ingredients: List<String> = emptyList(),
    val stabilizationDays: Int = 14,
)

data class Offer(
    val id: String,
    val productId: String?,
    val productName: String?,
    val merchant: String,
    val url: String,
    val priceCents: Int?,
    val currency: String,
    val disclosed: Boolean,
    val active: Boolean,
)

data class ProductDetail(
    val product: Product,
    val offers: List<Offer>,
    val ingredientAnalysis: IngredientExplainer?,
)

data class ReviewedIngredient(
    val ingredient: String,
    val purpose: String?,
    val caution: String?,
)

data class IngredientExplainer(
    val productName: String,
    val reviewed: List<ReviewedIngredient>,
    val unknown: List<String>,
)

data class ProductOverlap(
    val productName: String,
    val sharedIngredients: List<String>,
)

data class ProductPrediction(
    val productId: String,
    val productName: String,
    val ingredients: List<String>,
    val overlapWithInvestigate: List<ProductOverlap>,
    val overlapWithLikelyUseful: List<ProductOverlap>,
    val cohortOverlap: List<ProductOverlap>,
    val headline: String,
    val disclaimer: String,
)

data class PurchaseGuidanceRequest(
    val name: String? = null,
    val barcode: String? = null,
    val category: String = "other",
    val ingredients: List<String> = emptyList(),
    val priceCents: Int? = null,
    val currency: String = "INR",
)

/** `signal == "missing_ingredients"` means the ingredient list must be
 * supplied before any overlap comparison can run — render the [nextAction]. */
data class PurchaseGuidance(
    val productId: String?,
    val productName: String,
    val barcode: String?,
    val ingredients: List<String>,
    val signal: String?,
    val headline: String,
    val nextAction: String?,
    val overlapWithInvestigate: List<ProductOverlap>,
    val overlapWithLikelyUseful: List<ProductOverlap>,
    val cohortOverlap: List<ProductOverlap>,
    val estimatedAnnualCostCents: Int?,
    val currency: String,
    val disclaimer: String,
)
