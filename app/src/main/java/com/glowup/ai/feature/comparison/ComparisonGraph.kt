package com.glowup.ai.feature.comparison

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.glowup.ai.feature.shell.GlowDestination

/**
 * Wires: [GlowDestination.Comparison]
 *
 * Photo comparison feature that allows users to compare two captures side-by-side
 * with metrics comparison and trend indicators.
 */
fun NavGraphBuilder.comparisonGraph(navController: NavController) {
    composable<GlowDestination.Comparison> {
        ComparisonRoute(
            onBack = { navController.popBackStack() }
        )
    }
}
