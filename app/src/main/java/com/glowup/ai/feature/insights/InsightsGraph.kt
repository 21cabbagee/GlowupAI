package com.glowup.ai.feature.insights

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.glowup.ai.feature.shell.GlowDestination

/**
 * Phase 3.5 (feature/insights) — real implementation.
 *
 * Wires: [GlowDestination.Insights], [GlowDestination.QnaThread], [GlowDestination.ContextLog],
 * [GlowDestination.RootCause], [GlowDestination.BudgetOptimizer], [GlowDestination.DermExport].
 *
 * Labels and the reprocess job have no dedicated destination — [GlowDestination] is a fixed,
 * cross-feature contract this package must not extend — so both live as sections on
 * [InsightsHubScreen] instead of new screens.
 *
 * Do not rename this function or change its signature — [com.glowup.ai.feature.shell.GlowNavGraph]
 * calls it by this exact name.
 */
fun NavGraphBuilder.insightsGraph(navController: NavController) {
    composable<GlowDestination.Insights> {
        InsightsHubScreen(
            onOpenQna = { navController.navigate(GlowDestination.QnaThread()) },
            onOpenContextLog = { navController.navigate(GlowDestination.ContextLog) },
            onOpenRootCause = { navController.navigate(GlowDestination.RootCause) },
            onOpenBudgetOptimizer = { navController.navigate(GlowDestination.BudgetOptimizer) },
            onOpenDermExport = { navController.navigate(GlowDestination.DermExport) },
            onUpgrade = { navController.navigate(GlowDestination.Paywall) },
        )
    }
    composable<GlowDestination.QnaThread> {
        QnaScreen(
            onBack = { navController.popBackStack() },
            onUpgrade = { navController.navigate(GlowDestination.Paywall) },
        )
    }
    composable<GlowDestination.ContextLog> {
        ContextLogScreen(
            onBack = { navController.popBackStack() },
            onUpgrade = { navController.navigate(GlowDestination.Paywall) },
        )
    }
    composable<GlowDestination.RootCause> {
        RootCauseScreen(
            onBack = { navController.popBackStack() },
            onUpgrade = { navController.navigate(GlowDestination.Paywall) },
            onLogContext = { navController.navigate(GlowDestination.ContextLog) },
        )
    }
    composable<GlowDestination.BudgetOptimizer> {
        BudgetOptimizerScreen(
            onBack = { navController.popBackStack() },
            onUpgrade = { navController.navigate(GlowDestination.Paywall) },
        )
    }
    composable<GlowDestination.DermExport> {
        DermExportScreen(
            onBack = { navController.popBackStack() },
            onUpgrade = { navController.navigate(GlowDestination.Paywall) },
        )
    }
}
