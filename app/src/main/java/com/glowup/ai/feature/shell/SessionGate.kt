package com.glowup.ai.feature.shell

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import com.glowup.ai.data.local.SessionStore
import com.glowup.ai.data.repository.SessionRepository
import com.glowup.ai.domain.SessionState
import com.glowup.ai.domain.SessionStateMachine
import com.glowup.ai.feature.auth.SupabaseAuthGateway

/**
 * Shell-owned admission gate.
 *
 * Auth screens own Supabase sign-in and the initial health check. Once navigation leaves those
 * entry screens, this gate obtains a fresh profile-shaped response before the workspace can render
 * or expose its bottom bar. A locally cached user id is only a candidate: it is revalidated and a
 * stale id clears GlowUp's keys and returns to Welcome.
 */
@Composable
fun SessionGate(
    navController: NavHostController,
    sessionRepository: SessionRepository,
    sessionStore: SessionStore,
    authGateway: SupabaseAuthGateway,
    currentRoute: String?,
    content: @Composable (SessionState, onRetry: () -> Unit) -> Unit,
) {
    var sessionState by remember { mutableStateOf<SessionState>(SessionState.NoUser) }
    var retryNonce by remember { mutableStateOf(0) }
    var completedRequest by remember { mutableStateOf(-1) }

    LaunchedEffect(currentRoute, retryNonce) {
        // SessionGate stays composed around the whole graph. Reset its authoritative shell state
        // while auth routes are visible so a later user cannot inherit the previous user's
        // admitted shell after logout -> login. The next workspace route must therefore perform a
        // fresh session/profile admission even when retryNonce has not changed.
        if (shouldResetSessionGate(currentRoute)) {
            sessionState = SessionState.NoUser
            completedRequest = -1
            return@LaunchedEffect
        }

        // Once an authoritative profile has admitted the shell, navigation between feature
        // screens must not turn the gate into a hidden polling loop. Mutations and explicit retry
        // actions are the events that should cause a new profile read.
        if (completedRequest == retryNonce) return@LaunchedEffect

        if (!authGateway.hasSession()) {
            sessionRepository.clearSession()
            sessionState = SessionState.NoUser
            completedRequest = retryNonce
            routeToWelcome(navController)
            return@LaunchedEffect
        }

        sessionState = SessionState.ProfileLoading
        val result =
            if (sessionStore.userId() == null) {
                sessionRepository.authenticateWithSupabase()
            } else {
                sessionRepository.refreshProfile()
            }
        val resolved = SessionStateMachine.onProfileResult(result)
        sessionState = resolved
        completedRequest = retryNonce

        if (resolved is SessionState.NoUser) {
            // NotFound and Unauthorized both mean this local identity is no longer valid. Keep
            // Supabase and GlowUp cleanup explicit and limited to this app's own session state.
            sessionRepository.clearSession()
            authGateway.signOut()
            routeToWelcome(navController)
        }
    }

    content(sessionState) { retryNonce++ }
}

internal fun shouldResetSessionGate(currentRoute: String?): Boolean =
    currentRoute == null || currentRoute.isAuthEntryRoute()

private fun String.isAuthEntryRoute(): Boolean =
    matchesRoute(GlowDestination.Splash::class) ||
        matchesRoute(GlowDestination.Welcome::class) ||
        matchesRoute(GlowDestination.SignIn::class) ||
        matchesRoute(GlowDestination.ResetPassword::class) ||
        matchesRoute(GlowDestination.Onboarding::class) ||
        matchesRoute(GlowDestination.Consent::class) ||
        matchesRoute(GlowDestination.PrivacyPolicy::class) ||
        matchesRoute(GlowDestination.TermsOfService::class) ||
        matchesRoute(GlowDestination.MedicalDisclaimer::class)

private fun String.matchesRoute(routeClass: kotlin.reflect.KClass<out GlowDestination>): Boolean {
    val actual = substringBefore('?')
    val expected = routeClass.qualifiedName ?: return false
    return actual == expected || actual.startsWith("$expected/")
}

private fun routeToWelcome(navController: NavHostController) {
    navController.navigate(GlowDestination.Welcome) {
        popUpTo(navController.graph.findStartDestination().id) { inclusive = true }
        launchSingleTop = true
    }
}
