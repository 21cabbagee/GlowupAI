package com.glowup.ai.feature.discover

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.hilt.navigation.compose.hiltViewModel
import com.glowup.ai.core.design.GlowSpacing
import com.glowup.ai.core.ui.GlowTopBar
import com.glowup.ai.feature.discover.components.OffersSection
import com.glowup.ai.feature.discover.components.PredictSection
import com.glowup.ai.feature.discover.components.PurchaseGuidanceSection
import com.glowup.ai.feature.discover.components.RecommendationsSection

/**
 * `feature/discover`'s single screen. Reachable from Home and the routine product picker — NOT a
 * bottom tab (`GlowDestination.Discover` carries no arguments; the "Predict before you buy" and
 * "Purchase guidance" panels have their own self-serve product search rather than requiring a
 * navigation argument).
 */
@Composable
fun DiscoverScreen(
    onBack: (() -> Unit)? = null,
    onNavigateToUpgrade: () -> Unit,
    viewModel: DiscoverViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val pendingOfferKeys by viewModel.pendingOfferClicks.collectAsState()
    // [DiscoverRepository.pendingKeys] tracks mutation-lock keys of the form
    // "click_offer:{offerId}" — unwrap to the bare offer id the offer cards compare against.
    val pendingOfferIds = pendingOfferKeys.mapNotNullTo(mutableSetOf()) { key ->
        key.removePrefix("click_offer:").takeIf { key.startsWith("click_offer:") }
    }
    val uriHandler = LocalUriHandler.current

    // Click-then-open: once a click is recorded and the backend hands back a URL, attempt to open
    // it externally exactly once per `attempt`. A failure here does NOT retry the click — it just
    // flips `openFailed` so the offer card can offer a manual retry of the SAME url
    // (frontend-api-map.md: offer clicks are not idempotent).
    LaunchedEffect(uiState.offerOpenRequest) {
        val request = uiState.offerOpenRequest ?: return@LaunchedEffect
        if (request.openFailed) return@LaunchedEffect
        try {
            uriHandler.openUri(request.url)
            viewModel.onOfferUrlOpened(request.offerId)
        } catch (throwable: Throwable) {
            viewModel.onOfferUrlOpenFailed(request.offerId)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        GlowTopBar(title = "Discover", onBack = onBack)
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(GlowSpacing.md),
            verticalArrangement = Arrangement.spacedBy(GlowSpacing.xl),
        ) {
            item {
                RecommendationsSection(
                    state = uiState.recommendations,
                    onRetry = viewModel::loadRecommendations,
                    onUnlock = onNavigateToUpgrade,
                )
            }
            item {
                OffersSection(
                    state = uiState.offers,
                    pendingOfferIds = pendingOfferIds,
                    offerErrors = uiState.offerClickErrors,
                    openFailedOfferId = uiState.offerOpenRequest?.takeIf { it.openFailed }?.offerId,
                    onOfferCtaClick = { offer -> viewModel.onOfferCtaClicked(offer.id) },
                    onRetryOpen = viewModel::onRetryOpenUrl,
                    onRetry = { viewModel.loadOffers() },
                )
            }
            item {
                PredictSection(
                    state = uiState.predict,
                    onQueryChange = viewModel::onPredictQueryChange,
                    onProductSelected = viewModel::onPredictProductSelected,
                    onClearSelection = viewModel::onClearPredictSelection,
                    onUnlock = onNavigateToUpgrade,
                    onRetry = { uiState.predict.selectedProduct?.let { viewModel.onPredictProductSelected(it) } },
                )
            }
            item {
                PurchaseGuidanceSection(
                    state = uiState.purchaseGuidance,
                    onNameChange = viewModel::onPurchaseGuidanceNameChange,
                    onBarcodeChange = viewModel::onPurchaseGuidanceBarcodeChange,
                    onCategoryChange = viewModel::onPurchaseGuidanceCategoryChange,
                    onIngredientsChange = viewModel::onPurchaseGuidanceIngredientsChange,
                    onPriceChange = viewModel::onPurchaseGuidancePriceChange,
                    onCurrencyChange = viewModel::onPurchaseGuidanceCurrencyChange,
                    onSubmit = viewModel::submitPurchaseGuidance,
                    onUnlock = onNavigateToUpgrade,
                    onRetry = viewModel::submitPurchaseGuidance,
                )
            }
        }
    }
}
