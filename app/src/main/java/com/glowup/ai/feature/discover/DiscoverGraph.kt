package com.glowup.ai.feature.discover

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.glowup.ai.feature.shell.GlowDestination

/**
 * Wires [GlowDestination.Discover]. Reachable from Home and from the routine product picker —
 * deliberately NOT a bottom tab.
 *
 * The screen ([DiscoverScreen]) renders: cohort recommendations with `minimum_cohort_size` and
 * the disclaimer always visible (never phrased as a personal verdict), affiliate offers ungated
 * for every plan with disclosure/currency, click-then-open with a non-double-recording retry, and
 * a "Predict before you buy" + purchase-guidance panel, both framed as an ingredient-overlap
 * similarity signal, never an efficacy prediction.
 *
 * On any Premium-gated section's [SectionState.Locked], the section renders
 * [com.glowup.ai.core.ui.LockedCard] and its "Unlock Premium" CTA navigates to
 * [GlowDestination.Paywall] (owned by `feature/account`) — this screen never simulates an upgrade
 * itself, and never re-derives the Premium rule locally (see [DiscoverSupport.canUsePremiumFlow]).
 *
 * Do not rename this function or change its signature — [com.glowup.ai.feature.shell.GlowNavGraph]
 * calls it by this exact name.
 */
fun NavGraphBuilder.discoverGraph(navController: NavController) {
    composable<GlowDestination.Discover> {
        DiscoverScreen(
            onBack = { navController.popBackStack() },
            onNavigateToUpgrade = { navController.navigate(GlowDestination.Paywall) },
        )
    }
}
