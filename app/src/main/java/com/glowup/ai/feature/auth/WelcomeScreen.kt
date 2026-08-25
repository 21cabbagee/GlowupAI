package com.glowup.ai.feature.auth

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.glowup.ai.core.design.GlowSpacing
import com.glowup.ai.core.design.LocalGlowColors
import com.glowup.ai.core.ui.DisclaimerNote
import com.glowup.ai.core.ui.ErrorState
import com.glowup.ai.core.ui.GlowButton
import com.glowup.ai.core.ui.GlowButtonVariant
import com.glowup.ai.feature.shell.GlowDestination

/**
 * [GlowDestination.Welcome]. The identity choice screen: "Continue with Google" (Firebase
 * generic-OAuth flow, see [FirebaseAuthGateway.signInWithGoogle]) or "Continue with email", which
 * hands off to [GlowDestination.SignIn] for the email/password + create-account flow.
 */
@Composable
fun WelcomeRoute(
    navController: NavController,
    viewModel: AuthViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val target by viewModel.navigationTarget.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(target) {
        val destination = target ?: return@LaunchedEffect
        navController.navigate(destination) {
            popUpTo(GlowDestination.Welcome) { inclusive = true }
            launchSingleTop = true
        }
        viewModel.consumeNavigationTarget()
    }

    WelcomeContent(
        uiState = uiState,
        onContinueWithGoogle = {
            context.findActivity()?.let(viewModel::signInWithGoogle)
        },
        onContinueWithEmail = { navController.navigate(GlowDestination.SignIn) },
    )
}

@Composable
private fun WelcomeContent(
    uiState: AuthUiState,
    onContinueWithGoogle: () -> Unit,
    onContinueWithEmail: () -> Unit,
) {
    val glow = LocalGlowColors.current
    val isBusy = uiState is AuthUiState.Authenticating

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(GlowSpacing.lg),
        verticalArrangement = Arrangement.Bottom,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(bottom = GlowSpacing.xl),
            horizontalAlignment = Alignment.Start,
        ) {
            Text(
                text = "Track your skin,\nwith evidence.",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = glow.ink900,
            )
            Text(
                text = "Guided photo tracking, routine testing, and honest verdicts — never a diagnosis.",
                style = MaterialTheme.typography.bodyLarge,
                color = glow.ink600,
                modifier = Modifier.padding(top = GlowSpacing.sm),
            )
        }

        if (uiState is AuthUiState.Error) {
            ErrorState(
                modifier = Modifier.padding(bottom = GlowSpacing.md),
                message = uiState.message,
                retryLabel = "Try again",
                onRetry = onContinueWithGoogle,
            )
        }

        GlowButton(
            modifier = Modifier.fillMaxWidth(),
            text = "Continue with Google",
            onClick = onContinueWithGoogle,
            enabled = !isBusy,
            loading = isBusy,
            contentDescription = "Continue with Google",
        )
        GlowButton(
            modifier = Modifier.fillMaxWidth().padding(top = GlowSpacing.sm),
            text = "Continue with email",
            onClick = onContinueWithEmail,
            variant = GlowButtonVariant.Secondary,
            enabled = !isBusy,
            contentDescription = "Continue with email",
        )

        DisclaimerNote(
            modifier = Modifier.padding(top = GlowSpacing.md),
            text = "GlowUp AI tracks cosmetic skin appearance over time. It is not a diagnosis and " +
                "does not replace a dermatologist.",
        )

        Text(
            text = "By continuing you agree we'll ask for your explicit consent before analyzing any photo.",
            style = MaterialTheme.typography.labelSmall,
            color = glow.ink600,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(top = GlowSpacing.sm),
        )
    }
}

/** Standard Compose recipe for recovering the hosting [Activity] from a (possibly wrapped)
 * [Context] — needed because Firebase's `startActivityForSignInWithProvider` requires one. */
internal fun Context.findActivity(): Activity? {
    var context = this
    while (context is ContextWrapper) {
        if (context is Activity) return context
        context = context.baseContext
    }
    return null
}
