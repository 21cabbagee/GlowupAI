package com.glowup.ai.feature.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.glowup.ai.core.util.GlowResult
import com.glowup.ai.data.repository.BillingRepository
import com.glowup.ai.data.repository.PrivacyRepository
import com.glowup.ai.data.repository.SessionRepository
import com.glowup.ai.domain.model.Analytics
import com.glowup.ai.domain.model.Profile
import com.glowup.ai.domain.model.Subscription
import com.glowup.ai.feature.auth.FirebaseAuthGateway
import com.glowup.ai.feature.auth.toMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Backs [com.glowup.ai.feature.shell.GlowDestination.Account]. */
sealed interface AccountUiState {
    data object Loading : AccountUiState
    data class Content(
        val profile: Profile,
        val subscription: Subscription,
        /** Null while `GET /analytics` is still loading — see [analyticsError] for a failure. */
        val analytics: Analytics? = null,
        val analyticsError: String? = null,
    ) : AccountUiState
    data class Error(val message: String) : AccountUiState
}

/**
 * Drives the confirm-then-cancel flow for `POST /subscription/cancel`. Kept out of
 * [AccountUiState.Content] so a cancel-in-flight never has to be threaded through every other
 * field update on that state.
 */
sealed interface CancelSubscriptionState {
    data object Hidden : CancelSubscriptionState
    data object Confirming : CancelSubscriptionState
    data object Cancelling : CancelSubscriptionState
    data class Failed(val message: String) : CancelSubscriptionState
}

/**
 * Profile + consent summary + the authoritative subscription state + the engagement-analytics
 * panel. `GET /subscription` (via [BillingRepository.getSubscription]) is the single source of
 * truth for [Subscription.isPremium] here — this ViewModel never infers plan state from a button
 * tap (ANDROID_PLAN.md 3.7 / frontend-api-map.md trap #9).
 *
 * Consent state is rendered from [Profile.user] but changed only from
 * [com.glowup.ai.feature.shell.GlowDestination.DataAndPrivacy] — see [DataAndPrivacyViewModel].
 */
@HiltViewModel
class AccountViewModel @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val billingRepository: BillingRepository,
    private val privacyRepository: PrivacyRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<AccountUiState>(AccountUiState.Loading)
    val uiState: StateFlow<AccountUiState> = _uiState.asStateFlow()

    private val _cancelState = MutableStateFlow<CancelSubscriptionState>(CancelSubscriptionState.Hidden)
    val cancelState: StateFlow<CancelSubscriptionState> = _cancelState.asStateFlow()

    val cancelPending: StateFlow<Set<String>> = billingRepository.pendingKeys

    private val _signedOut = MutableStateFlow(false)
    /** One-shot: the Account screen navigates to Welcome and clears the back stack when this
     * flips true. */
    val signedOut: StateFlow<Boolean> = _signedOut.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.value = AccountUiState.Loading
            val userId = sessionRepository.userIdFlow.first()
            if (userId == null) {
                _uiState.value = AccountUiState.Error("No active session. Please sign in again.")
                return@launch
            }
            when (val profileResult = sessionRepository.refreshProfile(userId)) {
                is GlowResult.Failure -> {
                    _uiState.value = AccountUiState.Error(profileResult.error.toMessage())
                    return@launch
                }
                is GlowResult.Success -> {
                    when (val subscriptionResult = billingRepository.getSubscription(userId)) {
                        is GlowResult.Failure -> _uiState.value = AccountUiState.Error(subscriptionResult.error.toMessage())
                        is GlowResult.Success -> {
                            _uiState.value = AccountUiState.Content(
                                profile = profileResult.data,
                                subscription = subscriptionResult.data,
                            )
                            loadAnalytics(userId)
                        }
                    }
                }
            }
        }
    }

    fun retry() = load()

    private fun loadAnalytics(userId: String) {
        viewModelScope.launch {
            when (val result = privacyRepository.getAnalytics(userId)) {
                is GlowResult.Success -> updateContent { it.copy(analytics = result.data, analyticsError = null) }
                is GlowResult.Failure -> updateContent { it.copy(analyticsError = result.error.toMessage()) }
            }
        }
    }

    fun requestCancelSubscription() {
        _cancelState.value = CancelSubscriptionState.Confirming
    }

    fun dismissCancelSubscription() {
        _cancelState.value = CancelSubscriptionState.Hidden
    }

    /**
     * `POST /subscription/cancel`. History is retained server-side — this never deletes local
     * data — and on success it refetches profile + subscription so every Premium-gated surface
     * that reads [SessionRepository]/[BillingRepository] state sees the downgrade immediately
     * (ANDROID_PLAN.md 3.7 point 4 / frontend-api-map.md's cancel contract).
     */
    fun confirmCancelSubscription() {
        if (_cancelState.value == CancelSubscriptionState.Cancelling) return
        val content = _uiState.value as? AccountUiState.Content ?: return
        viewModelScope.launch {
            _cancelState.value = CancelSubscriptionState.Cancelling
            when (val result = billingRepository.cancel(content.profile.user.id)) {
                is GlowResult.Success -> {
                    _cancelState.value = CancelSubscriptionState.Hidden
                    load()
                }
                is GlowResult.Failure -> _cancelState.value = CancelSubscriptionState.Failed(result.error.toMessage())
            }
        }
    }

    /** Firebase sign-out + clearing GlowUp's own [com.glowup.ai.data.local.SessionStore] keys
     * only (never a blanket wipe — see [SessionRepository.clearSession]). Does not touch the
     * server-side account at all. */
    fun signOut() {
        if (_signedOut.value) return
        viewModelScope.launch {
            sessionRepository.clearSession()
            FirebaseAuthGateway.signOut()
            _signedOut.value = true
        }
    }

    private inline fun updateContent(transform: (AccountUiState.Content) -> AccountUiState.Content) {
        val current = _uiState.value
        if (current is AccountUiState.Content) {
            _uiState.value = transform(current)
        }
    }
}
