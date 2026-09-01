package com.glowup.ai.feature.auth

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.glowup.ai.core.util.GlowResult
import com.glowup.ai.data.remote.ApiError
import com.glowup.ai.data.repository.SessionRepository
import com.glowup.ai.domain.SessionState
import com.glowup.ai.domain.SessionStateMachine
import com.glowup.ai.domain.model.Profile
import com.glowup.ai.feature.shell.GlowDestination
import com.google.firebase.auth.FirebaseUser
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface AuthUiState {
    data object CheckingSession : AuthUiState

    data object Idle : AuthUiState

    data object Offline : AuthUiState

    data object Authenticating : AuthUiState

    data class Error(
        val message: String,
    ) : AuthUiState
}

sealed interface PasswordResetState {
    data object Idle : PasswordResetState

    data object Sending : PasswordResetState

    data object Sent : PasswordResetState

    data class Failed(
        val message: String,
    ) : PasswordResetState
}

@HiltViewModel
class AuthViewModel
    @Inject
    constructor(
        private val sessionRepository: SessionRepository,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.CheckingSession)
        val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()
        private val _sessionState = MutableStateFlow<SessionState>(SessionStateMachine.initial())
        val sessionState: StateFlow<SessionState> = _sessionState.asStateFlow()
        private val _navigationTarget = MutableStateFlow<GlowDestination?>(null)
        val navigationTarget: StateFlow<GlowDestination?> = _navigationTarget.asStateFlow()
        private val _resetState = MutableStateFlow<PasswordResetState>(PasswordResetState.Idle)
        val resetState: StateFlow<PasswordResetState> = _resetState.asStateFlow()
        private var activeJob: Job? = null

        fun consumeNavigationTarget() {
            _navigationTarget.value = null
        }

        fun bootstrap() {
            activeJob?.cancel()
            activeJob =
                viewModelScope.launch {
                    _navigationTarget.value = null
                    _uiState.value = AuthUiState.CheckingSession
                    when (val health = sessionRepository.health()) {
                        is GlowResult.Failure -> {
                            _uiState.value =
                                if (health.error is ApiError.Network) {
                                    AuthUiState.Offline
                                } else {
                                    AuthUiState.Error(health.error.toMessage())
                                }
                        }

                        is GlowResult.Success -> {
                            if (FirebaseAuthGateway.currentUser() == null) {
                                // A stored API id is only a candidate. Without a live Firebase identity it
                                // must never be used to open the shell or create an orphaned session.
                                runCatching { sessionRepository.clearSession() }
                                _sessionState.value = SessionState.NoUser
                                _uiState.value = AuthUiState.Idle
                                _navigationTarget.value = GlowDestination.Welcome
                            } else {
                                resolveSession()
                            }
                        }
                    }
                }
        }

        fun retryBootstrap() = bootstrap()

        fun signInWithGoogle(activity: Activity?) {
            if (activity == null) {
                _uiState.value = AuthUiState.Error("Sign-in isn't available right now. Please try again.")
                return
            }
            authenticate { FirebaseAuthGateway.signInWithGoogle(activity) }
        }

        fun signInWithEmail(
            email: String,
            password: String,
        ) = authenticate {
            FirebaseAuthGateway.signInWithEmail(email.trim(), password)
        }

        fun createAccount(
            email: String,
            password: String,
        ) = authenticate {
            FirebaseAuthGateway.createAccountWithEmail(email.trim(), password)
        }

        fun sendPasswordReset(email: String) {
            activeJob?.cancel()
            activeJob =
                viewModelScope.launch {
                    _resetState.value = PasswordResetState.Sending
                    FirebaseAuthGateway.sendPasswordReset(email.trim()).fold(
                        onSuccess = { _resetState.value = PasswordResetState.Sent },
                        onFailure = { _resetState.value = PasswordResetState.Failed(FirebaseAuthGateway.friendlyMessage(it)) },
                    )
                }
        }

        fun dismissResetState() {
            _resetState.value = PasswordResetState.Idle
        }

        private fun authenticate(action: suspend () -> Result<FirebaseUser>) {
            activeJob?.cancel()
            activeJob =
                viewModelScope.launch {
                    _navigationTarget.value = null
                    _uiState.value = AuthUiState.Authenticating
                    _sessionState.value = SessionStateMachine.onSignInRequested(_sessionState.value)
                    action().fold(
                        onSuccess = {
                            _sessionState.value = SessionStateMachine.onAuthenticationSucceeded(_sessionState.value)
                            resolveSession()
                        },
                        onFailure = { failure ->
                            _sessionState.value = SessionStateMachine.onAuthenticationFailed(_sessionState.value)
                            _uiState.value = AuthUiState.Error(FirebaseAuthGateway.friendlyMessage(failure))
                        },
                    )
                }
        }

        private suspend fun resolveSession() {
            _sessionState.value = SessionStateMachine.onProfileRefreshRequested(_sessionState.value)
            val state = SessionStateMachine.onProfileResult(sessionRepository.authenticateWithFirebase())
            _sessionState.value = state
            if (state is SessionState.NoUser) {
                sessionRepository.clearSession()
                FirebaseAuthGateway.signOut()
            }
            _uiState.value =
                if (state is SessionState.Unrecoverable) {
                    AuthUiState.Error(state.reason.toMessage())
                } else {
                    AuthUiState.Idle
                }
            _navigationTarget.value = destinationFor(state)
        }
    }

fun destinationFor(state: SessionState): GlowDestination? =
    when (state) {
        SessionState.NoUser -> GlowDestination.Welcome
        SessionState.Authenticating, SessionState.ProfileLoading -> null
        is SessionState.ConsentRequired -> onboardingOrConsent(state.profile)
        is SessionState.ConsentDeclined -> onboardingOrConsent(state.profile)
        is SessionState.BaselineNeeded -> GlowDestination.Capture
        is SessionState.Ready -> GlowDestination.Home
        is SessionState.Unrecoverable -> null
    }

private fun onboardingOrConsent(profile: Profile): GlowDestination =
    if (profile.experienceProfile?.onboardingComplete == true) {
        GlowDestination.Consent
    } else {
        GlowDestination.Onboarding
    }
