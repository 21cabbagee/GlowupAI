package com.glowup.ai.feature.account

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AlertDialog
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
import com.glowup.ai.core.ui.ErrorState
import com.glowup.ai.core.ui.GlowButton
import com.glowup.ai.core.ui.GlowButtonVariant
import com.glowup.ai.core.ui.GlowCard
import com.glowup.ai.core.ui.GlowTopBar
import com.glowup.ai.core.ui.ShimmerSkeleton
import com.glowup.ai.domain.model.ConsentState
import com.glowup.ai.domain.model.Profile
import com.glowup.ai.domain.model.Subscription
import com.glowup.ai.feature.account.components.AnalyticsPanel
import com.glowup.ai.feature.shell.GlowDestination

/** [GlowDestination.Account]: profile + consent summary, the authoritative subscription state,
 * the engagement-analytics panel, and links out to Settings / Data & Privacy / sign-out. */
@Composable
fun AccountRoute(
    navController: NavController,
    viewModel: AccountViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val cancelState by viewModel.cancelState.collectAsStateWithLifecycle()
    val signedOut by viewModel.signedOut.collectAsStateWithLifecycle()

    LaunchedEffect(signedOut) {
        if (signedOut) navController.routeToWelcomeAfterSessionEnd()
    }

    AccountContent(
        uiState = uiState,
        cancelState = cancelState,
        onRetry = viewModel::retry,
        onUpgradeClick = { navController.navigate(GlowDestination.Paywall) },
        onManageSubscriptionClick = viewModel::requestCancelSubscription,
        onDismissCancel = viewModel::dismissCancelSubscription,
        onConfirmCancel = viewModel::confirmCancelSubscription,
        onSettingsClick = { navController.navigate(GlowDestination.Settings) },
        onPrivacyClick = { navController.navigate(GlowDestination.DataAndPrivacy) },
        onSignOutClick = viewModel::signOut,
    )
}

@Composable
private fun AccountContent(
    uiState: AccountUiState,
    cancelState: CancelSubscriptionState,
    onRetry: () -> Unit,
    onUpgradeClick: () -> Unit,
    onManageSubscriptionClick: () -> Unit,
    onDismissCancel: () -> Unit,
    onConfirmCancel: () -> Unit,
    onSettingsClick: () -> Unit,
    onPrivacyClick: () -> Unit,
    onSignOutClick: () -> Unit,
) {
    Scaffold(topBar = { GlowTopBar(title = "Account") }) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (uiState) {
                AccountUiState.Loading -> {
                    LoadingBody()
                }

                is AccountUiState.Error -> {
                    Box(Modifier.fillMaxSize().padding(GlowSpacing.lg), contentAlignment = Alignment.Center) {
                        ErrorState(message = uiState.message, onRetry = onRetry)
                    }
                }

                is AccountUiState.Content -> {
                    Column(
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(GlowSpacing.lg),
                        verticalArrangement = Arrangement.spacedBy(GlowSpacing.lg),
                    ) {
                        ProfileSummaryCard(uiState.profile)
                        SubscriptionCard(
                            subscription = uiState.subscription,
                            onUpgradeClick = onUpgradeClick,
                            onManageClick = onManageSubscriptionClick,
                        )
                        AnalyticsPanel(
                            analytics = uiState.analytics,
                            errorMessage = uiState.analyticsError,
                        )
                        AccountLinksCard(onSettingsClick = onSettingsClick, onPrivacyClick = onPrivacyClick)
                        GlowButton(
                            modifier = Modifier.fillMaxWidth(),
                            text = "Sign out",
                            onClick = onSignOutClick,
                            variant = GlowButtonVariant.Secondary,
                        )
                    }

                    if (cancelState != CancelSubscriptionState.Hidden) {
                        CancelSubscriptionDialog(
                            state = cancelState,
                            onDismiss = onDismissCancel,
                            onConfirm = onConfirmCancel,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LoadingBody() {
    Column(
        modifier = Modifier.fillMaxSize().padding(GlowSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(GlowSpacing.md),
    ) {
        ShimmerSkeleton(height = 96.dp, cornerRadius = 18.dp)
        ShimmerSkeleton(height = 140.dp, cornerRadius = 18.dp)
        ShimmerSkeleton(height = 160.dp, cornerRadius = 18.dp)
    }
}

@Composable
private fun ProfileSummaryCard(profile: Profile) {
    val glow = LocalGlowColors.current
    GlowCard(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = profile.experienceProfile?.displayName ?: "Your profile",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = glow.ink900,
        )
        profile.user.skinType?.let {
            Text(
                text = "Skin type: ${it.replaceFirstChar { c -> c.uppercase() }}",
                style = MaterialTheme.typography.bodyMedium,
                color = glow.ink600,
                modifier = Modifier.padding(top = GlowSpacing.xs),
            )
        }
        ConsentSummaryRow(profile.user.consentState)
    }
}

@Composable
private fun ConsentSummaryRow(consentState: ConsentState) {
    val glow = LocalGlowColors.current
    val (label, description) =
        when (consentState) {
            ConsentState.ACTIVE -> "Consent: Active" to "You've allowed facial-photo tracking. Manage this in Data & Privacy."
            ConsentState.PENDING -> "Consent: Not yet given" to "Photo capture is locked until you grant consent in Data & Privacy."
            ConsentState.DECLINED -> "Consent: Declined" to "Photo capture is locked. You can change this any time in Data & Privacy."
            ConsentState.UNKNOWN -> "Consent: Unknown" to "We couldn't read your consent state. Check Data & Privacy."
        }
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(top = GlowSpacing.md)
                .semantics { contentDescription = "$label. $description" },
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = glow.ink900)
        Text(text = description, style = MaterialTheme.typography.bodySmall, color = glow.ink600)
    }
}

@Composable
private fun SubscriptionCard(
    subscription: Subscription,
    onUpgradeClick: () -> Unit,
    onManageClick: () -> Unit,
) {
    val glow = LocalGlowColors.current
    GlowCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(GlowSpacing.sm)) {
            if (!subscription.isPremium) {
                Icon(imageVector = Icons.Filled.Lock, contentDescription = null, tint = glow.honey700)
            }
            Text(
                text = if (subscription.isPremium) "Premium" else "Free plan",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = glow.ink900,
            )
        }
        Text(
            text = "Status: ${subscription.status.name.lowercase().replaceFirstChar { it.uppercase() }}",
            style = MaterialTheme.typography.bodySmall,
            color = glow.ink600,
            modifier = Modifier.padding(top = GlowSpacing.xs),
        )
        subscription.renewsAt?.let {
            Text(
                text = "Renews: $it",
                style = MaterialTheme.typography.bodySmall,
                color = glow.ink600,
            )
        }
        if (subscription.isPremium) {
            GlowButton(
                modifier = Modifier.fillMaxWidth().padding(top = GlowSpacing.md),
                text = "Cancel Premium",
                onClick = onManageClick,
                variant = GlowButtonVariant.Danger,
            )
        } else {
            GlowButton(
                modifier = Modifier.fillMaxWidth().padding(top = GlowSpacing.md),
                text = "Unlock Premium",
                onClick = onUpgradeClick,
                variant = GlowButtonVariant.Primary,
            )
        }
    }
}

@Composable
private fun AccountLinksCard(
    onSettingsClick: () -> Unit,
    onPrivacyClick: () -> Unit,
) {
    GlowCard(modifier = Modifier.fillMaxWidth()) {
        AccountLinkRow(label = "Settings", onClick = onSettingsClick)
        AccountLinkRow(label = "Data & Privacy", onClick = onPrivacyClick, isLast = true)
    }
}

@Composable
private fun AccountLinkRow(
    label: String,
    onClick: () -> Unit,
    isLast: Boolean = false,
) {
    val glow = LocalGlowColors.current
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = GlowSpacing.sm)
                .then(if (!isLast) Modifier.padding(bottom = GlowSpacing.xs) else Modifier)
                .semantics { contentDescription = label },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        GlowButton(
            text = label,
            onClick = onClick,
            variant = GlowButtonVariant.Ghost,
            contentDescription = label,
        )
        Icon(
            imageVector = Icons.Filled.ArrowForward,
            contentDescription = null,
            tint = glow.ink600,
        )
    }
}

@Composable
private fun CancelSubscriptionDialog(
    state: CancelSubscriptionState,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = { if (state != CancelSubscriptionState.Cancelling) onDismiss() },
        title = { Text("Cancel Premium?") },
        text = {
            Column {
                Text(
                    "Your capture history and past data stay exactly as they are. Premium-only " +
                        "features (experiments, Data Q&A, discover recommendations, ingredient " +
                        "explainer, root-cause search, budget optimizer, derm export, historical " +
                        "reprocessing, and pre-purchase prediction) will lock immediately.",
                )
                if (state is CancelSubscriptionState.Failed) {
                    Text(
                        text = state.message,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = GlowSpacing.sm),
                    )
                }
            }
        },
        confirmButton = {
            GlowButton(
                text = "Cancel Premium",
                onClick = onConfirm,
                variant = GlowButtonVariant.Danger,
                loading = state == CancelSubscriptionState.Cancelling,
                enabled = state != CancelSubscriptionState.Cancelling,
            )
        },
        dismissButton = {
            GlowButton(
                text = "Keep Premium",
                onClick = onDismiss,
                variant = GlowButtonVariant.Ghost,
                enabled = state != CancelSubscriptionState.Cancelling,
            )
        },
    )
}
