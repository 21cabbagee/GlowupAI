package com.glowup.ai.feature.account

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
    ) : PaywallUiState

    data class Error(
        val message: String,
    ) : PaywallUiState
}

/**
 * `POST /subscription/upgrade` is a LOCAL CHECKOUT SIMULATION, not a real payment provider —
 * frontend-api-map.md is explicit about this, and ANDROID_PLAN.md 3.7 forbids drawing fake
 * credit-card UI or implying a real charge. This ships for internal/closed testing only; real
 * paid distribution needs Google Play Billing (out of v1 scope).
 *
 * [upgrade] guards against the "each call records a billing event" trap (frontend-api-map.md
 * trap #9) two ways: the UI-level [PaywallUiState.Content.upgrading] flag disables the button
 * immediately on tap, and [BillingRepository]'s own `MutationLock` refuses a second concurrent
 * network call regardless. After success it refetches both the subscription AND the full
 * profile so every Premium-gated screen sees the new entitlement on its next read.
 */
@HiltViewModel
class PaywallViewModel
    @Inject
    constructor(
        private val sessionRepository: SessionRepository,
        private val billingRepository: BillingRepository,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow<PaywallUiState>(PaywallUiState.Loading)
        val uiState: StateFlow<PaywallUiState> = _uiState.asStateFlow()

        init {
            load()
        }

        fun load() {
            viewModelScope.launch {
                _uiState.value = PaywallUiState.Loading
                val userId = sessionRepository.userIdFlow.first()
                if (userId == null) {
                    _uiState.value = PaywallUiState.Error("No active session. Please sign in again.")
                    return@launch
                }
                when (val result = billingRepository.getSubscription(userId)) {
                    is GlowResult.Success -> _uiState.value = PaywallUiState.Content(result.data)
                    is GlowResult.Failure -> _uiState.value = PaywallUiState.Error(result.error.toMessage())
                }
            }
        }

        fun retry() = load()

        fun upgrade() {
            val content = _uiState.value as? PaywallUiState.Content ?: return
            if (content.upgrading) return // repeated-click guard — see class doc
            viewModelScope.launch {
                val userId = sessionRepository.userIdFlow.first() ?: return@launch
                _uiState.value = content.copy(upgrading = true, error = null, justUpgraded = false)
                when (val result = billingRepository.upgrade(userId)) {
                    is GlowResult.Success -> {
                        sessionRepository.refreshProfile(userId) // refetch profile per trap #9/#12
                        _uiState.value = PaywallUiState.Content(result.data, upgrading = false, justUpgraded = true)
                    }

                    is GlowResult.Failure -> {
                        _uiState.value = content.copy(upgrading = false, error = result.error.toMessage())
                    }
                }
            }
        }

        fun dismissJustUpgraded() {
            val content = _uiState.value as? PaywallUiState.Content ?: return
            _uiState.value = content.copy(justUpgraded = false)
        }
    }
