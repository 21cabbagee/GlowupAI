package com.glowup.ai.feature.onboarding

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.glowup.ai.feature.shell.GlowDestination

/**
 * Wires [GlowDestination.Onboarding] (enhanced onboarding flow with tutorial screens, permissions,
 * and guided setup) and [GlowDestination.Consent] (explicit accept/decline, `POST /consent`) to
 * their real screens.
 *
 * Do not rename this function or change its signature — [com.glowup.ai.feature.shell.GlowNavGraph]
 * calls it by this exact name.
 */
fun NavGraphBuilder.onboardingGraph(navController: NavController) {
    composable<GlowDestination.Onboarding> {
        // Use the enhanced onboarding flow with comprehensive tutorial screens
        EnhancedOnboardingRoute(navController = navController)
    }
    composable<GlowDestination.Consent> {
        ConsentRoute(navController = navController)
    }
}
