package com.glowup.ai.feature.auth

import android.util.Patterns
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.glowup.ai.core.design.GlowSpacing
import com.glowup.ai.core.design.LocalGlowColors
import com.glowup.ai.core.ui.ErrorState
import com.glowup.ai.core.ui.GlowButton
import com.glowup.ai.core.ui.GlowButtonVariant
import com.glowup.ai.core.ui.GlowTextField
import com.glowup.ai.core.ui.GlowTopBar
import com.glowup.ai.feature.shell.GlowDestination

private enum class AuthMode(
    val title: String,
    val ctaLabel: String,
) {
    SIGN_IN(title = "Sign in", ctaLabel = "Sign in"),
    CREATE_ACCOUNT(title = "Create your account", ctaLabel = "Create account"),
}

/** [GlowDestination.SignIn]: email/password sign-in, create-account, and password reset — the
 * three flows `ANDROID_PLAN.md` Task 3.1 calls out explicitly for this screen. */
@Composable
fun SignInRoute(
    navController: NavController,
    viewModel: AuthViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val resetState by viewModel.resetState.collectAsStateWithLifecycle()
    val target by viewModel.navigationTarget.collectAsStateWithLifecycle()

    LaunchedEffect(target) {
        val destination = target ?: return@LaunchedEffect
        navController.navigate(destination) {
            popUpTo(GlowDestination.Welcome) { inclusive = true }
            launchSingleTop = true
        }
        viewModel.consumeNavigationTarget()
    }

    SignInContent(
        uiState = uiState,
        resetState = resetState,
        onBack = { navController.popBackStack() },
        onSignIn = viewModel::signInWithEmail,
        onCreateAccount = viewModel::createAccount,
        onForgotPassword = viewModel::sendPasswordReset,
        onDismissReset = viewModel::dismissResetState,
    )
}

@Composable
private fun SignInContent(
    uiState: AuthUiState,
    resetState: PasswordResetState,
    onBack: () -> Unit,
    onSignIn: (String, String) -> Unit,
    onCreateAccount: (String, String) -> Unit,
    onForgotPassword: (String) -> Unit,
    onDismissReset: () -> Unit,
) {
    val glow = LocalGlowColors.current
    var mode by rememberSaveable { mutableStateOf(AuthMode.SIGN_IN) }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var validationError by remember { mutableStateOf<String?>(null) }

    val isBusy = uiState is AuthUiState.Authenticating

    fun validateAndSubmit() {
        val trimmedEmail = email.trim()
        validationError =
            when {
                trimmedEmail.isEmpty() -> "Enter your email address."
                !Patterns.EMAIL_ADDRESS.matcher(trimmedEmail).matches() -> "Enter a valid email address."
                password.length < 6 -> "Password must be at least 6 characters."
                else -> null
            }
        if (validationError != null) return
        when (mode) {
            AuthMode.SIGN_IN -> onSignIn(trimmedEmail, password)
            AuthMode.CREATE_ACCOUNT -> onCreateAccount(trimmedEmail, password)
        }
    }

    Scaffold(
        topBar = { GlowTopBar(title = mode.title, onBack = onBack) },
    ) { innerPadding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(GlowSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(GlowSpacing.md),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(GlowSpacing.sm),
                modifier = Modifier.fillMaxWidth(),
            ) {
                AuthMode.entries.forEach { candidate ->
                    GlowButton(
                        modifier = Modifier.weight(1f),
                        text = if (candidate == AuthMode.SIGN_IN) "Sign in" else "Create account",
                        variant = if (candidate == mode) GlowButtonVariant.Primary else GlowButtonVariant.Ghost,
                        enabled = !isBusy,
                        onClick = {
                            mode = candidate
                            validationError = null
                        },
                    )
                }
            }

            GlowTextField(
                value = email,
                onValueChange = {
                    email = it
                    validationError = null
                },
                label = "Email",
                keyboardType = KeyboardType.Email,
                enabled = !isBusy,
            )
            GlowTextField(
                value = password,
                onValueChange = {
                    password = it
                    validationError = null
                },
                label = "Password",
                supportingText = if (mode == AuthMode.CREATE_ACCOUNT) "At least 6 characters." else null,
                errorText = validationError,
                keyboardType = KeyboardType.Password,
                visualTransformation = PasswordVisualTransformation(),
                enabled = !isBusy,
            )

            if (mode == AuthMode.SIGN_IN) {
                TextButton(
                    onClick = { onForgotPassword(email) },
                    enabled = !isBusy,
                ) {
                    Text("Forgot password?", color = glow.honey700)
                }
            }

            when (resetState) {
                is PasswordResetState.Sent -> {
                    Text(
                        text = "Password reset email sent — check your inbox.",
                        style = MaterialTheme.typography.bodySmall,
                        color = glow.success,
                        modifier =
                            Modifier.semantics {
                                contentDescription = "Password reset email sent"
                                liveRegion = LiveRegionMode.Polite
                            },
                    )
                }

                is PasswordResetState.Failed -> {
                    Text(
                        text = resetState.message,
                        style = MaterialTheme.typography.bodySmall,
                        color = glow.danger,
                    )
                }

                is PasswordResetState.Sending -> {
                    Text(
                        text = "Sending reset email…",
                        style = MaterialTheme.typography.bodySmall,
                        color = glow.ink600,
                    )
                }

                PasswordResetState.Idle -> {
                    Unit
                }
            }

            if (uiState is AuthUiState.Error) {
                ErrorState(message = uiState.message, retryLabel = "Try again", onRetry = ::validateAndSubmit)
            }

            GlowButton(
                modifier = Modifier.fillMaxWidth(),
                text = mode.ctaLabel,
                onClick = ::validateAndSubmit,
                loading = isBusy,
                enabled = !isBusy,
            )
        }
    }

    // Reset one-shot reset-email feedback if the user changes screens/mode entirely.
    LaunchedEffect(mode) { onDismissReset() }
}
