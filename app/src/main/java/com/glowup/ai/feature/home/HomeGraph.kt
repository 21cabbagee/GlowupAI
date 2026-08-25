package com.glowup.ai.feature.home

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.glowup.ai.feature.shell.GlowDestination

/**
 * Wires [GlowDestination.Home]. `GET /dashboard` is the single initial snapshot for this screen —
 * see [HomeViewModel]/[HomeScreen] for the full task 3.3 implementation (streak + real stat
 * tiles, verdict/experiment cards including the `locked` upsell variant, the `GET /history` trend
 * chart, the daily check-in sheet, the weekly recap card, and the capture-guide banner).
 *
 * Home also navigates out to [GlowDestination.Discover] (reachable from Home, not a bottom tab),
 * [GlowDestination.Capture], [GlowDestination.Routine], [GlowDestination.Experiments],
 * [GlowDestination.ExperimentDetail] and [GlowDestination.Paywall] — all destinations already
 * declared by `feature/shell` per the Phase 3 concurrency contract.
 *
 * Do not rename this function or change its signature —
 * [com.glowup.ai.feature.shell.GlowNavGraph] calls it by this exact name.
 */
fun NavGraphBuilder.homeGraph(navController: NavController) {
    composable<GlowDestination.Home> {
        HomeRoute(onNavigate = { destination -> navController.navigate(destination) })
    }
}
