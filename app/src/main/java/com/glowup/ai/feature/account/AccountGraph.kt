package com.glowup.ai.feature.account

import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.glowup.ai.feature.shell.GlowDestination

/**
 * Wires [GlowDestination.Account], [GlowDestination.Paywall], [GlowDestination.Settings], and
 * [GlowDestination.DataAndPrivacy] to their real screens — profile + consent summary + the
 * authoritative subscription state, the real Premium feature paywall with the simulated
 * checkout, working settings (theme/reminders/notifications + sign-out), and data export /
 * typed-`DELETE` account deletion.
 *
 * Do not rename this function or change its signature — [com.glowup.ai.feature.shell.GlowNavGraph]
 * calls it by this exact name.
 */
fun NavGraphBuilder.accountGraph(navController: NavController) {
    composable<GlowDestination.Account> {
        AccountRoute(navController = navController)
    }
    composable<GlowDestination.Paywall> {
        PaywallRoute(navController = navController)
    }
    composable<GlowDestination.Settings> {
        SettingsRoute(navController = navController)
    }
    composable<GlowDestination.DataAndPrivacy> {
        DataAndPrivacyRoute(navController = navController)
    }
}

/** Signs out / deletes account both end here: pop the ENTIRE back stack to Welcome so a back
 * press can never return to a now-invalid authenticated screen. */
internal fun NavController.routeToWelcomeAfterSessionEnd() {
    navigate(GlowDestination.Welcome) {
        popUpTo(graph.findStartDestination().id) { inclusive = true }
        launchSingleTop = true
    }
}
