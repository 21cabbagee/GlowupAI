package com.glowup.ai.feature.account

import android.app.Activity
import com.android.billingclient.api.Purchase
import com.glowup.ai.data.repository.PlayBillingClient
import kotlinx.coroutines.CancellationException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.glowup.ai.core.util.GlowResult
import com.glowup.ai.data.repository.BillingRepository
import com.glowup.ai.data.repository.SessionRepository
import com.glowup.ai.domain.model.Subscription
import com.glowup.ai.feature.auth.toMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.Job
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Backs [com.glowup.ai.feature.shell.GlowDestination.Paywall]. */
sealed interface PaywallUiState {
    data object Loading : PaywallUiState

    data class Content(
        val subscription: Subscription,
        val upgrading: Boolean = false,
        val error: String? = null,
        /** True right after a successful upgrade, so the screen can show a brief confirmation
         * instead of silently re-rendering the same layout with a different plan. */
        val justUpgraded: Boolean = false,
        val plans: List<PlayPlan> = emptyList(),
    ) : PaywallUiState

    data class Error(
        val message: String,
    ) : PaywallUiState
}

data class PlayPlan(
    val productId: String,
    val offerToken: String,
    val label: String,
)

/**
 * Loads real Play prices and verifies purchase tokens on the backend before
 * updating entitlement. Pending payments never unlock Premium; cancelled
 * subscriptions retain access only until Google Play reports their expiry.
 */
@HiltViewModel
class PaywallViewModel
    @Inject
    constructor(
        private val sessionRepository: SessionRepository,
        private val billingRepository: BillingRepository,
        private val play: PlayBillingClient,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow<PaywallUiState>(PaywallUiState.Loading)
        val uiState: StateFlow<PaywallUiState> = _uiState.asStateFlow()
        private val purchaseMutex = Mutex()
        private var loadJob: Job? = null

        init {
            play.onPurchases = ::verifyPurchases
            play.onMessage = { message ->
                val content = _uiState.value as? PaywallUiState.Content
                if (content != null) _uiState.value = content.copy(upgrading = false, error = message)
            }
            load()
        }

        fun load() {
            loadJob?.cancel()
            loadJob = viewModelScope.launch {
                _uiState.value = PaywallUiState.Loading
                val userId = sessionRepository.userIdFlow.first()
                if (userId == null) {
                    _uiState.value = PaywallUiState.Error("No active session. Please sign in again.")
                    return@launch
                }
                when (val result = billingRepository.getSubscription(userId)) {
                    is GlowResult.Success -> {
                        _uiState.value = PaywallUiState.Content(result.data)
                        try {
                            val products = play.products(billingRepository.productIds())
                            val plans = products.flatMap { product ->
                                product.subscriptionOfferDetails.orEmpty().filter { it.offerId == null }.map { offer ->
                                    val phase = offer.pricingPhases.pricingPhaseList.last()
                                    val period = when (phase.billingPeriod) {
                                        "P1M" -> "month"
                                        "P1Y" -> "year"
                                        "P1W" -> "week"
                                        "P3M" -> "3 months"
                                        "P6M" -> "6 months"
                                        "P1D" -> "day"
                                        else -> phase.billingPeriod
                                    }
                                    PlayPlan(product.productId, offer.offerToken, "${phase.formattedPrice} / $period")
                                }
                            }
                            _uiState.value = PaywallUiState.Content(
                                result.data,
                                plans = plans,
                                error = if (plans.isEmpty() && !result.data.isPremium) {
                                    "Subscriptions are not available in Google Play yet."
                                } else {
                                    null
                                },
                            )
                            if (plans.isNotEmpty()) {
                                try {
                                    processPurchases(play.restore())
                                } catch (cancelled: CancellationException) {
                                    throw cancelled
                                } catch (_: Exception) {
                                    // A plan catalog can still be displayed when restore is
                                    // temporarily unavailable. The explicit Restore action retries it.
                                }
                            }
                        } catch (cancelled: CancellationException) {
                            throw cancelled
                        } catch (_: Exception) {
                            _uiState.value = PaywallUiState.Content(result.data, error = "Google Play plans could not be loaded. Please retry.")
                        }
                    }
                    is GlowResult.Failure -> _uiState.value = PaywallUiState.Error(result.error.toMessage())
                }
            }
        }

        fun retry() = load()

        fun upgrade(activity: Activity, offerToken: String) {
            val content = _uiState.value as? PaywallUiState.Content ?: return
            if (content.upgrading) return // repeated-click guard — see class doc
            val plan = content.plans.firstOrNull { it.offerToken == offerToken } ?: return
            _uiState.value = content.copy(upgrading = true, error = null)
            viewModelScope.launch {
                val userId = sessionRepository.userIdFlow.first()
                if (userId == null) {
                    _uiState.value = content.copy(upgrading = false, error = "Please sign in again.")
                    return@launch
                }
                try {
                    play.launch(activity, userId, plan.productId, plan.offerToken)
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    _uiState.value = content.copy(
                        upgrading = false,
                        error = "Google Play plans could not be loaded. Please retry.",
                    )
                }
            }
        }

        /** Reconcile purchases whenever the paywall returns to the foreground. */
        fun refreshPurchases() {
            viewModelScope.launch {
                try {
                    processPurchases(play.restore())
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    showError("Purchases could not be restored. Please retry.")
                }
            }
        }

        fun restore() = refreshPurchases()

        private fun verifyPurchases(purchases: List<Purchase>) {
            viewModelScope.launch { processPurchases(purchases) }
        }

        private suspend fun processPurchases(purchases: List<Purchase>) {
            purchaseMutex.withLock {
                val userId = sessionRepository.userIdFlow.first() ?: return@withLock
                for (purchase in purchases.distinctBy { it.purchaseToken }) {
                    val content = _uiState.value as? PaywallUiState.Content ?: return@withLock
                    when (purchase.purchaseState) {
                        Purchase.PurchaseState.PENDING -> {
                            _uiState.value = content.copy(
                                upgrading = false,
                                error = "Payment is pending. Premium will unlock after Google Play confirms it.",
                            )
                        }

                        Purchase.PurchaseState.PURCHASED -> {
                            when (val result = billingRepository.verifyPurchase(userId, purchase.purchaseToken)) {
                                is GlowResult.Success -> {
                                    sessionRepository.refreshProfile(userId)
                                    _uiState.value = content.copy(
                                        subscription = result.data,
                                        upgrading = false,
                                        justUpgraded = result.data.isPremium,
                                        error = null,
                                    )
                                }

                                is GlowResult.Failure -> {
                                    _uiState.value = content.copy(
                                        upgrading = false,
                                        error = result.error.toMessage(),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        private fun showError(message: String) {
            val content = _uiState.value as? PaywallUiState.Content ?: return
            _uiState.value = content.copy(upgrading = false, error = message)
        }

        override fun onCleared() {
            // PlayBillingClient is application-scoped. Detach this screen's callbacks, but keep
            // the shared client alive so reopening the paywall can reconnect/restore purchases.
            play.onPurchases = {}
            play.onMessage = {}
            super.onCleared()
        }

        fun dismissJustUpgraded() {
            val content = _uiState.value as? PaywallUiState.Content ?: return
            _uiState.value = content.copy(justUpgraded = false)
        }
    }
