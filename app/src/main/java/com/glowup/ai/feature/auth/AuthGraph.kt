package com.glowup.ai.feature.auth

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.glowup.ai.feature.legal.PrivacyPolicyRoute
import com.glowup.ai.feature.shell.GlowDestination

/**
 * Wires [GlowDestination.Splash], [GlowDestination.Welcome], [GlowDestination.SignIn] to their
 * real screens: the `SplashScreen` API startup gate, Firebase Google + email/password sign-in,
 * and `POST /api/auth/session`.
 *
 * Do not rename this function or change its signature — [com.glowup.ai.feature.shell.GlowNavGraph]
 * calls it by this exact name.
 */
fun NavGraphBuilder.authGraph(navController: NavController) {
    composable<GlowDestination.Splash> {
        SplashRoute(navController = navController)
    }
    composable<GlowDestination.Welcome> {
        WelcomeRoute(navController = navController)
    }
    composable<GlowDestination.SignIn> {
        SignInRoute(navController = navController)
    }
    composable<GlowDestination.PrivacyPolicy> {
        PrivacyPolicyRoute(
            onBack = { navController.navigateUp() }
        )
    }
}
