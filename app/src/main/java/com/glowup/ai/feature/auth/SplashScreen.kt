package com.glowup.ai.feature.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.glowup.ai.core.design.GlowSpacing
import com.glowup.ai.core.design.LocalGlowColors
import com.glowup.ai.core.ui.ErrorState
import com.glowup.ai.feature.shell.GlowDestination

/**
 * [GlowDestination.Splash]. The system `SplashScreen` API (installed in `MainActivity.onCreate`
 * via `installSplashScreen()`) covers the icon/brand flash before Compose ever draws; this
 * composable is what a user sees for the (usually sub-second, occasionally longer on a slow
 * network) time it takes to resolve `GET /api/health` and, if a Firebase session exists,
 * `POST /api/auth/session` — see [AuthViewModel.bootstrap].
 */
@Composable
fun SplashRoute(
    navController: NavController,
    viewModel: AuthViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val target by viewModel.navigationTarget.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { viewModel.bootstrap() }

    LaunchedEffect(target) {
        val destination = target ?: return@LaunchedEffect
        navController.navigate(destination) {
            popUpTo(GlowDestination.Splash) { inclusive = true }
            launchSingleTop = true
        }
        viewModel.consumeNavigationTarget()
    }

    SplashContent(uiState = uiState, onRetry = viewModel::retryBootstrap)
}

@Composable
private fun SplashContent(uiState: AuthUiState, onRetry: () -> Unit) {
    val glow = LocalGlowColors.current
    Box(
        modifier = Modifier.fillMaxSize().padding(GlowSpacing.lg),
        contentAlignment = Alignment.Center,
    ) {
        when (uiState) {
            is AuthUiState.Offline -> ErrorState(
                message = "GlowUp AI can't reach the server right now. Check your connection and try again.",
                onRetry = onRetry,
            )
            is AuthUiState.Error -> ErrorState(message = uiState.message, onRetry = onRetry)
            AuthUiState.CheckingSession, AuthUiState.Idle, AuthUiState.Authenticating -> Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(GlowSpacing.md),
                modifier = Modifier.semantics {
                    contentDescription = "Loading your GlowUp AI session"
                },
            ) {
                Text(
                    text = "GlowUp AI",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = glow.ink900,
                )
                CircularProgressIndicator(color = glow.honey600, modifier = Modifier.padding(top = 4.dp))
            }
        }
    }
}
