package com.glowup.ai.feature.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import com.glowup.ai.core.ui.DisclaimerNote
import com.glowup.ai.core.ui.ErrorState
import com.glowup.ai.core.ui.GlowButton
import com.glowup.ai.core.ui.GlowButtonVariant
import com.glowup.ai.core.ui.GlowCard
import com.glowup.ai.domain.SessionState
import com.glowup.ai.feature.shell.GlowDestination

/**
 * [GlowDestination.Consent]: `POST /api/users/{userId}/consent` with an explicit accept/decline
 * choice and a `policy_version` — never inferred, never silently granted (the previous app
 * hardcoded `facialData = true` with no UI at all). Renders three distinct states depending on
 * the authoritative [SessionState] ([ConsentViewModel] never lets the screen guess):
 *
 * - [SessionState.ConsentRequired]: the first-time choice.
 * - [SessionState.ConsentDeclined]: a real, non-error state — the profile stays usable for
 *   non-photo features, capture stays visibly locked, and the user can either change their mind
 *   or continue into the app as-is.
 * - Anything else: this screen navigates itself away (see [ConsentViewModel.applyResult]).
 */
@Composable
fun ConsentRoute(
    navController: NavController,
    viewModel: ConsentViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val target by viewModel.navigationTarget.collectAsStateWithLifecycle()

    LaunchedEffect(target) {
        val destination = target ?: return@LaunchedEffect
        navController.navigate(destination) {
            popUpTo(GlowDestination.Consent) { inclusive = true }
            launchSingleTop = true
        }
        viewModel.consumeNavigationTarget()
    }

    ConsentContent(
        uiState = uiState,
        onAccept = { viewModel.decide(accept = true) },
        onDecline = { viewModel.decide(accept = false) },
        onContinueToApp = viewModel::continueToApp,
        onRetry = viewModel::retry,
    )
}

@Composable
private fun ConsentContent(
    uiState: ConsentUiState,
    onAccept: () -> Unit,
    onDecline: () -> Unit,
    onContinueToApp: () -> Unit,
    onRetry: () -> Unit,
) {
    Scaffold { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            when (uiState) {
                is ConsentUiState.Loading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            "Loading…",
                            modifier = Modifier.semantics { contentDescription = "Loading consent status" },
                        )
                    }
                }

                is ConsentUiState.Error -> {
                    Box(Modifier.fillMaxSize().padding(GlowSpacing.lg), contentAlignment = Alignment.Center) {
                        ErrorState(message = uiState.message, onRetry = onRetry)
                    }
                }

                is ConsentUiState.Saving -> {
                    ConsentChoiceBody(
                        saving = true,
                        onAccept = onAccept,
                        onDecline = onDecline,
                    )
                }

                is ConsentUiState.Content -> {
                    when (val session = uiState.session) {
                        is SessionState.ConsentDeclined -> {
                            ConsentDeclinedBody(
                                onReconsider = onAccept,
                                onContinueToApp = onContinueToApp,
                            )
                        }

                        else -> {
                            ConsentChoiceBody(saving = false, onAccept = onAccept, onDecline = onDecline)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ConsentChoiceBody(
    saving: Boolean,
    onAccept: () -> Unit,
    onDecline: () -> Unit,
) {
    val glow = LocalGlowColors.current
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(GlowSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(GlowSpacing.lg),
    ) {
        Icon(
            imageVector = Icons.Filled.CameraAlt,
            contentDescription = null,
            tint = glow.honey600,
            modifier = Modifier.padding(top = GlowSpacing.md),
        )
        Text(
            text = "Can GlowUp AI analyze your photos?",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = glow.ink900,
        )
        Text(
            text =
                "To track your skin over time we need your explicit consent to process facial " +
                    "photos you capture in this app. We use them only to measure cosmetic appearance " +
                    "— redness, texture, tone — never to identify you, and never to diagnose a " +
                    "medical condition.",
            style = MaterialTheme.typography.bodyMedium,
            color = glow.ink600,
        )

        GlowCard {
            Text(
                text = "What this means",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = glow.ink900,
            )
            listOf(
                "Photos are used only for your own skin-tracking dashboard.",
                "You can decline and still use routine tracking, insights, and Q&A.",
                "You can change your mind at any time from your account settings.",
            ).forEach { line ->
                Text(
                    text = "•  $line",
                    style = MaterialTheme.typography.bodySmall,
                    color = glow.ink600,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
        }

        DisclaimerNote(
            text =
                "GlowUp AI tracks cosmetic skin appearance over time. It is not a diagnosis and " +
                    "does not replace a dermatologist.",
        )

        Column(verticalArrangement = Arrangement.spacedBy(GlowSpacing.sm)) {
            GlowButton(
                modifier = Modifier.fillMaxWidth(),
                text = "I agree — enable photo tracking",
                loading = saving,
                enabled = !saving,
                onClick = onAccept,
                contentDescription = "Agree and enable photo tracking",
            )
            GlowButton(
                modifier = Modifier.fillMaxWidth(),
                text = "Not now",
                variant = GlowButtonVariant.Secondary,
                enabled = !saving,
                onClick = onDecline,
                contentDescription = "Decline photo tracking for now",
            )
        }
    }
}

@Composable
private fun ConsentDeclinedBody(
    onReconsider: () -> Unit,
    onContinueToApp: () -> Unit,
) {
    val glow = LocalGlowColors.current
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(GlowSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(GlowSpacing.lg),
    ) {
        Text(
            text = "Photo tracking is off",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = glow.ink900,
        )
        Text(
            text =
                "You previously chose not to let GlowUp AI analyze your photos. Capture stays " +
                    "locked, but routine tracking, insights, and Q&A remain fully available.",
            style = MaterialTheme.typography.bodyMedium,
            color = glow.ink600,
            modifier =
                Modifier.semantics {
                    contentDescription = "Photo tracking is off. Capture is locked. Other features remain available."
                },
        )

        GlowButton(
            modifier = Modifier.fillMaxWidth(),
            text = "Turn on photo tracking",
            onClick = onReconsider,
        )
        GlowButton(
            modifier = Modifier.fillMaxWidth(),
            text = "Continue without photos",
            variant = GlowButtonVariant.Secondary,
            onClick = onContinueToApp,
        )
    }
}
