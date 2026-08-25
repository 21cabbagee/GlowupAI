package com.glowup.ai.feature.account

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.glowup.ai.core.ui.GlowTopBar
import com.glowup.ai.core.ui.ShimmerSkeleton
import com.glowup.ai.feature.account.components.PremiumFeatureList

/**
 * [com.glowup.ai.feature.shell.GlowDestination.Paywall]. Lists the REAL Premium features the
 * backend gates and offers the simulated local-checkout upgrade — see [PaywallViewModel]'s class
 * doc for why this deliberately never draws payment-card UI.
 */
@Composable
fun PaywallRoute(
    navController: NavController,
    viewModel: PaywallViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    PaywallContent(
        uiState = uiState,
        onBack = { navController.popBackStack() },
        onRetry = viewModel::retry,
        onUpgradeClick = viewModel::upgrade,
        onDismissJustUpgraded = viewModel::dismissJustUpgraded,
    )
}

@Composable
private fun PaywallContent(
    uiState: PaywallUiState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onUpgradeClick: () -> Unit,
    onDismissJustUpgraded: () -> Unit,
) {
    Scaffold(topBar = { GlowTopBar(title = "GlowUp Premium", onBack = onBack) }) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (uiState) {
                PaywallUiState.Loading -> Column(
                    modifier = Modifier.fillMaxSize().padding(GlowSpacing.lg),
                    verticalArrangement = Arrangement.spacedBy(GlowSpacing.md),
                ) {
                    ShimmerSkeleton(height = 120.dp, cornerRadius = 18.dp)
                    ShimmerSkeleton(height = 240.dp, cornerRadius = 18.dp)
                }
                is PaywallUiState.Error -> Box(Modifier.fillMaxSize().padding(GlowSpacing.lg), contentAlignment = Alignment.Center) {
                    ErrorState(message = uiState.message, onRetry = onRetry)
                }
                is PaywallUiState.Content -> Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(GlowSpacing.lg),
                    verticalArrangement = Arrangement.spacedBy(GlowSpacing.lg),
                ) {
                    if (uiState.subscription.isPremium) {
                        AlreadyPremiumCard()
                    } else {
                        UpgradeCard(uiState = uiState, onUpgradeClick = onUpgradeClick, onDismissJustUpgraded = onDismissJustUpgraded)
                    }
                    PremiumFeaturesCard()
                }
            }
        }
    }
}

@Composable
private fun AlreadyPremiumCard() {
    val glow = LocalGlowColors.current
    GlowCard(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "You're already on Premium",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = glow.ink900,
        )
        Text(
            text = "Manage or cancel your plan from the Account screen.",
            style = MaterialTheme.typography.bodyMedium,
            color = glow.ink600,
            modifier = Modifier.padding(top = GlowSpacing.xs),
        )
    }
}

@Composable
private fun UpgradeCard(
    uiState: PaywallUiState.Content,
    onUpgradeClick: () -> Unit,
    onDismissJustUpgraded: () -> Unit,
) {
    val glow = LocalGlowColors.current
    GlowCard(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Unlock Premium",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = glow.ink900,
        )
        Text(
            text = "Everything below unlocks the moment checkout completes.",
            style = MaterialTheme.typography.bodyMedium,
            color = glow.ink600,
            modifier = Modifier.padding(top = GlowSpacing.xs),
        )
        DisclaimerNote(
            modifier = Modifier.fillMaxWidth().padding(top = GlowSpacing.md),
            text = "This is a local checkout simulation for internal/closed testing — it is not " +
                "a real payment provider and no card is charged. Real paid distribution will use " +
                "Google Play Billing in a future release.",
        )
        if (uiState.justUpgraded) {
            Text(
                text = "You're now on Premium. Every locked screen refreshes automatically.",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = glow.success,
                modifier = Modifier.padding(top = GlowSpacing.md),
            )
            GlowButton(
                modifier = Modifier.padding(top = GlowSpacing.sm),
                text = "Got it",
                onClick = onDismissJustUpgraded,
                variant = GlowButtonVariant.Ghost,
            )
        }
        uiState.error?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = glow.danger,
                modifier = Modifier.padding(top = GlowSpacing.md),
            )
        }
        GlowButton(
            modifier = Modifier.fillMaxWidth().padding(top = GlowSpacing.lg),
            text = if (uiState.justUpgraded) "Upgraded" else "Simulate checkout",
            onClick = onUpgradeClick,
            enabled = !uiState.upgrading && !uiState.justUpgraded,
            loading = uiState.upgrading,
        )
    }
}

@Composable
private fun PremiumFeaturesCard() {
    val glow = LocalGlowColors.current
    GlowCard(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "What Premium unlocks",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = glow.ink900,
        )
        PremiumFeatureList(modifier = Modifier.fillMaxWidth().padding(top = GlowSpacing.md))
    }
}
