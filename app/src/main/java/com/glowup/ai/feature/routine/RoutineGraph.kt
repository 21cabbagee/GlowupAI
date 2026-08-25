package com.glowup.ai.feature.routine

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.glowup.ai.feature.shell.GlowDestination

/**
 * Owned by Phase 3.4 (feature/routine). Wires: [GlowDestination.Routine],
 * [GlowDestination.ProductDetail], [GlowDestination.ShelfScan], [GlowDestination.Experiments],
 * [GlowDestination.ExperimentDetail].
 *
 * Do not rename this function or change its signature — [com.glowup.ai.feature.shell.GlowNavGraph]
 * calls it by this exact name.
 */
fun NavGraphBuilder.routineGraph(navController: NavController) {
    composable<GlowDestination.Routine> {
        RoutineRoute(
            onOpenProduct = { productId -> navController.navigate(GlowDestination.ProductDetail(productId)) },
            onOpenShelfScan = { navController.navigate(GlowDestination.ShelfScan) },
            onOpenExperiments = { navController.navigate(GlowDestination.Experiments) },
        )
    }
    composable<GlowDestination.ProductDetail> {
        ProductDetailRoute(
            onBack = { navController.popBackStack() },
            onNavigateToPaywall = { navController.navigate(GlowDestination.Paywall) },
        )
    }
    composable<GlowDestination.ShelfScan> {
        ShelfScanRoute(
            onBack = { navController.popBackStack() },
            onDone = { navController.popBackStack() },
        )
    }
    composable<GlowDestination.Experiments> {
        ExperimentsRoute(
            onBack = { navController.popBackStack() },
            onOpenExperiment = { experimentId -> navController.navigate(GlowDestination.ExperimentDetail(experimentId)) },
            onNavigateToPaywall = { navController.navigate(GlowDestination.Paywall) },
        )
    }
    composable<GlowDestination.ExperimentDetail> {
        ExperimentDetailRoute(onBack = { navController.popBackStack() })
    }
}
