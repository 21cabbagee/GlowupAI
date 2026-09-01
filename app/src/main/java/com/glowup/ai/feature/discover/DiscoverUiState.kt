package com.glowup.ai.feature.discover

import com.glowup.ai.domain.model.Discover
import com.glowup.ai.domain.model.Offer
import com.glowup.ai.domain.model.Product
import com.glowup.ai.domain.model.ProductPrediction
import com.glowup.ai.domain.model.PurchaseGuidance

/**
 * The shared `Loading | Content | Empty | Error | Locked` shape (ANDROID_PLAN.md §1), scoped to
 * `feature/discover` so this package has no import dependency on any other feature's copy of the
 * same idea.
 *
 * [Empty] is a first-class, non-error outcome here on purpose: cohort recommendations are only
 * emitted once at least three consenting users contribute a `likely_useful` verdict for the same
 * product (`GET /discover` prerequisites), so an empty `recommendations[]` on a brand-new backend
 * is the NORMAL early state, never a failure to surface as [Error].
 */
sealed interface SectionState<out T> {
    data object Loading : SectionState<Nothing>

    data class Content<T>(
        val value: T,
    ) : SectionState<T>

    data class Empty(
        val title: String,
        val body: String? = null,
    ) : SectionState<Nothing>

    data class Error(
        val message: String,
    ) : SectionState<Nothing>

    /** The backend 403'd this call as Premium-only. Rendered as [com.glowup.ai.core.ui.LockedCard],
     * never folded into [Error]. Offers/click are NEVER put in this state — see class docs on
     * [DiscoverRepository][com.glowup.ai.data.repository.DiscoverRepository]. */
    data object Locked : SectionState<Nothing>
}

/**
 * One in-flight/attempted "record click, then open externally" cycle for a single affiliate
 * offer. [attempt] is bumped on every retry so a [androidx.compose.runtime.LaunchedEffect] keyed
 * on this whole object re-fires without re-calling `POST .../offers/{id}/click` — the click itself
 * is NOT idempotent (frontend-api-map.md), so only [DiscoverViewModel.onOfferCtaClicked] (a fresh
 * user tap on the offer card, not a retry) may invoke it again.
 */
data class OfferOpenRequest(
    val offerId: String,
    val url: String,
    val attempt: Int = 0,
    /** True once opening the browser/intent has thrown once for this [attempt]; the screen shows
     * a retry affordance instead of looping automatically. */
    val openFailed: Boolean = false,
)

data class PredictPanelState(
    val query: String = "",
    val searching: Boolean = false,
    val searchResults: List<Product> = emptyList(),
    val searchError: String? = null,
    val selectedProduct: Product? = null,
    /** `null` = no prediction attempted yet for the currently selected product. */
    val result: SectionState<ProductPrediction>? = null,
)

data class PurchaseGuidanceFormState(
    val name: String = "",
    val barcode: String = "",
    val category: String = "other",
    val ingredientsText: String = "",
    val priceText: String = "",
    val currency: String = "INR",
) {
    /** Mirrors the backend's own requirement ("product name or a known barcode is required") so
     * the submit control can disable before ever making a doomed request. */
    val canSubmit: Boolean
        get() = name.isNotBlank() || barcode.isNotBlank()
}

data class PurchaseGuidancePanelState(
    val form: PurchaseGuidanceFormState = PurchaseGuidanceFormState(),
    val submitting: Boolean = false,
    val result: SectionState<PurchaseGuidance>? = null,
)

/**
 * `feature/discover`'s single-screen state. The screen has four independently-gated sections
 * (cohort recommendations, affiliate offers, pre-purchase prediction, purchase guidance) rather
 * than one screen-wide `Loading|Content|...` — offers must NEVER be gated even while the other
 * three sections show [SectionState.Locked] on a free account (ANDROID_PLAN.md non-negotiable
 * constraint: "Commerce offers are free for every plan — never gate them").
 */
data class DiscoverUiState(
    val canUsePremium: Boolean = false,
    val recommendations: SectionState<Discover> = SectionState.Loading,
    val offers: SectionState<List<Offer>> = SectionState.Loading,
    val offerClickErrors: Map<String, String> = emptyMap(),
    val offerOpenRequest: OfferOpenRequest? = null,
    val predict: PredictPanelState = PredictPanelState(),
    val purchaseGuidance: PurchaseGuidancePanelState = PurchaseGuidancePanelState(),
)
