package com.glowup.ai.feature.shell

import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import com.glowup.ai.feature.account.accountGraph
import com.glowup.ai.feature.achievements.achievementsGraph
import com.glowup.ai.feature.auth.authGraph
import com.glowup.ai.feature.capture.captureGraph
import com.glowup.ai.feature.comparison.comparisonGraph
import com.glowup.ai.feature.discover.discoverGraph
import com.glowup.ai.feature.home.homeGraph
import com.glowup.ai.feature.insights.insightsGraph
import com.glowup.ai.feature.onboarding.onboardingGraph
import com.glowup.ai.feature.routine.routineGraph

/**
 * The single top-level NavHost. Assembles every feature package's graph entry point.
 *
 * This is the concurrency contract for Phase 3: each `<feature>Graph` function below is owned
 * and implemented entirely inside its own `feature/&lt;name&gt;` package. This file only wires them
 * together — it must not gain feature-specific composables of its own.
 *
 * [GlowDestination.Splash] is the start destination; the session gate (owned separately as
 * Phase 2.6's `domain/SessionStateMachine.kt` + `feature/shell/SessionGate.kt`) is expected to
 * decide, from there, whether to route onward to Welcome/SignIn, Onboarding/Consent, or straight
 * to Home, based on the authoritative session state — it is not a navigation concern of this
 * file.
 */
@Composable
fun GlowNavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = GlowDestination.Splash,
    ) {
        authGraph(navController)
        onboardingGraph(navController)
        homeGraph(navController)
        achievementsGraph(navController)
        captureGraph(navController)
        comparisonGraph(navController)
        routineGraph(navController)
        insightsGraph(navController)
        discoverGraph(navController)
        accountGraph(navController)
    }
}

/**
 * Navigates to a top-level tab destination, preserving each tab's own back stack per the
 * standard Navigation Compose bottom-nav recipe.
 */
fun NavController.navigateToTab(destination: GlowDestination) {
    navigate(destination) {
        popUpTo(graph.findStartDestination().id) {
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }
}
