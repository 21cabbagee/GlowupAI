package com.glowup.ai.feature.discover

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.glowup.ai.core.util.GlowResult
import com.glowup.ai.data.local.SessionStore
import com.glowup.ai.data.repository.DiscoverRepository
import com.glowup.ai.data.repository.RoutineRepository
import com.glowup.ai.domain.model.PurchaseGuidanceRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import javax.inject.Inject

private const val SEARCH_DEBOUNCE_MS = 400L

/**
 * Owns the [com.glowup.ai.feature.shell.GlowDestination.Discover] screen.
 *
 * Reads [DiscoverRepository] (owned by this task) for cohort recommendations and affiliate
 * offers/click, and [RoutineRepository] (owned by `feature/routine`, only ever CALLED here —
 * never edited) for `GET /products/{id}/predict` and `POST /purchase-guidance`, which that
 * repository's own class doc claims ownership of. `GET /products/search` (also on
 * [RoutineRepository]) backs the inline product picker for the prediction panel; it carries no
 * Premium requirement.
 *
 * Every Premium-gated section (recommendations, prediction, purchase guidance) consults
 * [canUsePremiumFlow] as a fast pre-flight hint ONLY to skip a doomed round trip and show
 * [SectionState.Locked] immediately; a real `403` (`ApiError.PremiumRequired`) from any of those
 * three calls is what actually flips the section to [SectionState.Locked] — the hint is never
 * trusted on its own to unlock anything. Affiliate offers and the click route never consult this
 * at all: they are free for every plan.
 */
@HiltViewModel
class DiscoverViewModel @Inject constructor(
    private val discoverRepository: DiscoverRepository,
    private val routineRepository: RoutineRepository,
    private val sessionStore: SessionStore,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DiscoverUiState())
    val uiState: StateFlow<DiscoverUiState> = _uiState.asStateFlow()

    /** Mirrors [DiscoverRepository.pendingKeys] so an offer card can disable its own CTA while
     * its click is outstanding without duplicating mutation-guard logic. */
    val pendingOfferClicks: StateFlow<Set<String>> = discoverRepository.pendingKeys

    private var predictSearchJob: Job? = null

    init {
        viewModelScope.launch {
            val canUsePremium = sessionStore.canUsePremiumFlow().first()
            _uiState.update { it.copy(canUsePremium = canUsePremium) }
        }
        loadRecommendations()
        loadOffers()
    }

    // -- Cohort recommendations (Premium) --------------------------------------------------------

    fun loadRecommendations() {
        viewModelScope.launch {
            val canUsePremium = sessionStore.canUsePremiumFlow().first()
            _uiState.update { it.copy(canUsePremium = canUsePremium) }
            if (!canUsePremium) {
                _uiState.update { it.copy(recommendations = SectionState.Locked) }
                return@launch
            }
            val userId = sessionStore.userId()
            if (userId == null) {
                _uiState.update { it.copy(recommendations = SectionState.Error("Sign in to see Discover.")) }
                return@launch
            }
            _uiState.update { it.copy(recommendations = SectionState.Loading) }
            when (val result = discoverRepository.getDiscover(userId)) {
                is GlowResult.Success -> _uiState.update {
                    it.copy(recommendations = SectionState.Content(result.data))
                }
                is GlowResult.Failure -> _uiState.update {
                    it.copy(
                        recommendations = if (result.error.isPremiumGate) {
                            SectionState.Locked
                        } else {
                            SectionState.Error(result.error.toDisplayMessage())
                        },
                    )
                }
            }
        }
    }

    // -- Affiliate offers (free for every plan — never gated) ------------------------------------

    fun loadOffers(productId: String? = null) {
        viewModelScope.launch {
            val userId = sessionStore.userId()
            if (userId == null) {
                _uiState.update { it.copy(offers = SectionState.Error("Sign in to see offers.")) }
                return@launch
            }
            _uiState.update { it.copy(offers = SectionState.Loading) }
            when (val result = discoverRepository.getOffers(userId, productId)) {
                is GlowResult.Success -> _uiState.update {
                    it.copy(offers = SectionState.Content(result.data))
                }
                is GlowResult.Failure -> _uiState.update {
                    it.copy(offers = SectionState.Error(result.error.toDisplayMessage()))
                }
            }
        }
    }

    /**
     * Records the click, then hands the returned URL to [DiscoverUiState.offerOpenRequest] for the
     * screen to open externally. This is the ONLY place that calls
     * [DiscoverRepository.clickOffer] — [onRetryOpenUrl] deliberately never does, because the
     * click is not idempotent.
     */
    fun onOfferCtaClicked(offerId: String) {
        if (discoverRepository.pendingKeys.value.contains("click_offer:$offerId")) return
        viewModelScope.launch {
            val userId = sessionStore.userId() ?: return@launch
            _uiState.update { it.copy(offerClickErrors = it.offerClickErrors - offerId) }
            when (val result = discoverRepository.clickOffer(userId, offerId)) {
                is GlowResult.Success -> _uiState.update {
                    it.copy(offerOpenRequest = OfferOpenRequest(offerId = offerId, url = result.data.url))
                }
                is GlowResult.Failure -> _uiState.update {
                    it.copy(offerClickErrors = it.offerClickErrors + (offerId to result.error.toDisplayMessage()))
                }
            }
        }
    }

    /** The screen calls this after [androidx.compose.ui.platform.LocalUriHandler] successfully
     * launched the URL. */
    fun onOfferUrlOpened(offerId: String) {
        _uiState.update { current ->
            if (current.offerOpenRequest?.offerId == offerId) current.copy(offerOpenRequest = null) else current
        }
    }

    /** The screen calls this when launching the URL threw (no browser/handler, cancelled intent,
     * etc). Keeps the offer and its already-fetched URL so [onRetryOpenUrl] can try again WITHOUT
     * recording a second click. */
    fun onOfferUrlOpenFailed(offerId: String) {
        _uiState.update { current ->
            val request = current.offerOpenRequest
            if (request?.offerId == offerId) current.copy(offerOpenRequest = request.copy(openFailed = true)) else current
        }
    }

    /** Retries OPENING the same URL from the same click response — never re-calls
     * [DiscoverRepository.clickOffer]. */
    fun onRetryOpenUrl() {
        _uiState.update { current ->
            val request = current.offerOpenRequest ?: return@update current
            current.copy(offerOpenRequest = request.copy(attempt = request.attempt + 1, openFailed = false))
        }
    }

    // -- Pre-purchase prediction (Premium; similarity signal, never efficacy) --------------------

    fun onPredictQueryChange(query: String) {
        _uiState.update { it.copy(predict = it.predict.copy(query = query, searchError = null)) }
        predictSearchJob?.cancel()
        if (query.isBlank()) {
            _uiState.update { it.copy(predict = it.predict.copy(searchResults = emptyList(), searching = false)) }
            return
        }
        predictSearchJob = viewModelScope.launch {
            delay(SEARCH_DEBOUNCE_MS)
            _uiState.update { it.copy(predict = it.predict.copy(searching = true)) }
            when (val result = routineRepository.searchProducts(query)) {
                is GlowResult.Success -> _uiState.update {
                    it.copy(predict = it.predict.copy(searching = false, searchResults = result.data))
                }
                is GlowResult.Failure -> _uiState.update {
                    it.copy(predict = it.predict.copy(searching = false, searchError = result.error.toDisplayMessage()))
                }
            }
        }
    }

    fun onPredictProductSelected(product: com.glowup.ai.domain.model.Product) {
        _uiState.update {
            it.copy(
                predict = it.predict.copy(
                    selectedProduct = product,
                    query = product.name,
                    searchResults = emptyList(),
                    result = null,
                ),
            )
        }
        runPredict()
    }

    fun onClearPredictSelection() {
        _uiState.update {
            it.copy(predict = PredictPanelState())
        }
    }

    private fun runPredict() {
        val product = _uiState.value.predict.selectedProduct ?: return
        viewModelScope.launch {
            val canUsePremium = sessionStore.canUsePremiumFlow().first()
            if (!canUsePremium) {
                _uiState.update { it.copy(predict = it.predict.copy(result = SectionState.Locked)) }
                return@launch
            }
            val userId = sessionStore.userId()
            if (userId == null) {
                _uiState.update { it.copy(predict = it.predict.copy(result = SectionState.Error("Sign in to use Predict before you buy."))) }
                return@launch
            }
            _uiState.update { it.copy(predict = it.predict.copy(result = SectionState.Loading)) }
            when (val result = routineRepository.predictProduct(product.id, userId)) {
                is GlowResult.Success -> _uiState.update {
                    it.copy(predict = it.predict.copy(result = SectionState.Content(result.data)))
                }
                is GlowResult.Failure -> _uiState.update {
                    it.copy(
                        predict = it.predict.copy(
                            result = if (result.error.isPremiumGate) SectionState.Locked else SectionState.Error(result.error.toDisplayMessage()),
                        ),
                    )
                }
            }
        }
    }

    // -- Purchase guidance (Premium; similarity signal, never efficacy) ---------------------------

    fun onPurchaseGuidanceNameChange(name: String) = updateGuidanceForm { it.copy(name = name) }
    fun onPurchaseGuidanceBarcodeChange(barcode: String) = updateGuidanceForm { it.copy(barcode = barcode) }
    fun onPurchaseGuidanceCategoryChange(category: String) = updateGuidanceForm { it.copy(category = category) }
    fun onPurchaseGuidanceIngredientsChange(text: String) = updateGuidanceForm { it.copy(ingredientsText = text) }
    fun onPurchaseGuidancePriceChange(text: String) = updateGuidanceForm { it.copy(priceText = text) }
    fun onPurchaseGuidanceCurrencyChange(currency: String) = updateGuidanceForm { it.copy(currency = currency) }

    private inline fun updateGuidanceForm(transform: (PurchaseGuidanceFormState) -> PurchaseGuidanceFormState) {
        _uiState.update { it.copy(purchaseGuidance = it.purchaseGuidance.copy(form = transform(it.purchaseGuidance.form))) }
    }

    fun submitPurchaseGuidance() {
        val state = _uiState.value.purchaseGuidance
        if (state.submitting || !state.form.canSubmit) return
        viewModelScope.launch {
            val canUsePremium = sessionStore.canUsePremiumFlow().first()
            if (!canUsePremium) {
                _uiState.update { it.copy(purchaseGuidance = it.purchaseGuidance.copy(result = SectionState.Locked)) }
                return@launch
            }
            val userId = sessionStore.userId()
            if (userId == null) {
                _uiState.update {
                    it.copy(purchaseGuidance = it.purchaseGuidance.copy(result = SectionState.Error("Sign in to use purchase guidance.")))
                }
                return@launch
            }
            _uiState.update { it.copy(purchaseGuidance = it.purchaseGuidance.copy(submitting = true, result = SectionState.Loading)) }
            val form = state.form
            val request = PurchaseGuidanceRequest(
                name = form.name.trim().ifBlank { null },
                barcode = form.barcode.trim().ifBlank { null },
                category = form.category.trim().ifBlank { "other" },
                ingredients = form.ingredientsText.split(",", ";")
                    .map { it.trim() }
                    .filter { it.isNotEmpty() },
                priceCents = form.priceText.trim().toDoubleOrNull()?.let { (it * 100).roundToInt() },
                currency = form.currency.trim().ifBlank { "INR" },
            )
            when (val result = routineRepository.purchaseGuidance(userId, request)) {
                is GlowResult.Success -> _uiState.update {
                    it.copy(purchaseGuidance = it.purchaseGuidance.copy(submitting = false, result = SectionState.Content(result.data)))
                }
                is GlowResult.Failure -> _uiState.update {
                    it.copy(
                        purchaseGuidance = it.purchaseGuidance.copy(
                            submitting = false,
                            result = if (result.error.isPremiumGate) SectionState.Locked else SectionState.Error(result.error.toDisplayMessage()),
                        ),
                    )
                }
            }
        }
    }
}
