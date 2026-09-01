package com.glowup.ai.feature.achievements

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.glowup.ai.feature.shell.GlowDestination

/**
 * Wires [GlowDestination.Achievements] into the navigation graph.
 *
 * Do not rename this function or change its signature —
 * [com.glowup.ai.feature.shell.GlowNavGraph] calls it by convention.
 */
fun NavGraphBuilder.achievementsGraph(navController: NavController) {
    composable<GlowDestination.Achievements> {
        AchievementsRoute(
            onBack = { navController.popBackStack() },
        )
    }
}
