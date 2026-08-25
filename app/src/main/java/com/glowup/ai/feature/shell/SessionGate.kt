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
import com.glowup.ai.feature.auth.FirebaseAuthGateway

/**
 * Shell-owned admission gate.
 *
 * Auth screens own Firebase sign-in and the initial health check. Once navigation leaves those
 * entry screens, this gate obtains a fresh profile-shaped response before the workspace can render
 * or expose its bottom bar. A locally cached user id is only a candidate: it is revalidated and a
 * stale id clears GlowUp's keys and returns to Welcome.
 */
@Composable
fun SessionGate(
    navController: NavHostController,
    sessionRepository: SessionRepository,
    sessionStore: SessionStore,
    currentRoute: String?,
    content: @Composable (SessionState, onRetry: () -> Unit) -> Unit,
) {
    var sessionState by remember { mutableStateOf<SessionState>(SessionState.NoUser) }
    var retryNonce by remember { mutableStateOf(0) }
    var completedRequest by remember { mutableStateOf(-1) }

    LaunchedEffect(currentRoute, retryNonce) {
        if (currentRoute == null || currentRoute.isAuthEntryRoute()) return@LaunchedEffect

        // Once an authoritative profile has admitted the shell, navigation between feature
        // screens must not turn the gate into a hidden polling loop. Mutations and explicit retry
        // actions are the events that should cause a new profile read.
        if (completedRequest == retryNonce) return@LaunchedEffect

        val firebaseUser = FirebaseAuthGateway.currentUser()
        if (firebaseUser == null) {
            sessionRepository.clearSession()
            sessionState = SessionState.NoUser
            completedRequest = retryNonce
            routeToWelcome(navController)
            return@LaunchedEffect
        }

        sessionState = SessionState.ProfileLoading
        val result = if (sessionStore.userId() == null) {
            sessionRepository.authenticateWithFirebase()
        } else {
            sessionRepository.refreshProfile()
        }
        val resolved = SessionStateMachine.onProfileResult(result)
        sessionState = resolved
        completedRequest = retryNonce

        if (resolved is SessionState.NoUser) {
            // NotFound and Unauthorized both mean this local identity is no longer valid. Keep
            // Firebase and GlowUp cleanup explicit and limited to this app's own session state.
            sessionRepository.clearSession()
            FirebaseAuthGateway.signOut()
            routeToWelcome(navController)
        }
    }

    content(sessionState) { retryNonce++ }
}

private fun String.isAuthEntryRoute(): Boolean =
    matchesRoute(GlowDestination.Splash::class) ||
        matchesRoute(GlowDestination.Welcome::class) ||
        matchesRoute(GlowDestination.SignIn::class)

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