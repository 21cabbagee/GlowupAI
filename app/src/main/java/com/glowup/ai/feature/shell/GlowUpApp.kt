package com.glowup.ai.feature.shell

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Science
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.glowup.ai.core.design.LocalGlowColors
import com.glowup.ai.core.ui.GlowBottomBar
import com.glowup.ai.core.ui.GlowBottomBarItem
import com.glowup.ai.data.local.SessionStore
import com.glowup.ai.data.repository.SessionRepository
import com.glowup.ai.data.telemetry.Telemetry
import com.glowup.ai.data.telemetry.TelemetryEvent
import com.glowup.ai.domain.SessionState
import kotlin.reflect.KClass

/** Honey/Bumble shell: four persistent destinations and one clearly-labelled capture FAB. */
@Composable
fun GlowUpApp(
    navController: NavHostController = rememberNavController(),
    sessionStore: SessionStore,
    sessionRepository: SessionRepository,
    telemetry: Telemetry,
    pendingDestination: GlowDestination? = null,
    onPendingDestinationConsumed: (GlowDestination) -> Unit = {},
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination

    SessionGate(
        navController = navController,
        sessionRepository = sessionRepository,
        sessionStore = sessionStore,
        currentRoute = currentDestination?.route,
    ) { sessionState, retry ->
        val isWorkspaceRoute = currentDestination.isWorkspaceDestination()
        val isAuthoritative = sessionState.isAuthoritative
        val showWorkspaceChrome = isWorkspaceRoute && isAuthoritative &&
            (sessionState is SessionState.BaselineNeeded ||
                sessionState is SessionState.Ready ||
                sessionState is SessionState.ConsentDeclined)
        val selectedTab = currentDestination.selectedTabIndex()

        LaunchedEffect(Unit) {
            telemetry.track(TelemetryEvent.APP_OPEN)
        }
        LaunchedEffect(currentDestination?.route, isAuthoritative) {
            if (currentDestination.isHomeDestination() && isAuthoritative) {
                telemetry.track(TelemetryEvent.HOME_VIEWED)
            }
        }
        NotificationPermissionEffect(
            currentDestination = currentDestination,
            sessionStore = sessionStore,
            canEnterWorkspace = showWorkspaceChrome,
        )
        DeepLinkEffect(
            destination = pendingDestination,
            sessionState = sessionState,
            navController = navController,
            onConsumed = onPendingDestinationConsumed,
        )

        Scaffold(
            bottomBar = {
                if (showWorkspaceChrome) {
                    GlowBottomBar(
                        items = tabs.map { GlowBottomBarItem(label = it.label, icon = it.icon) },
                        selectedIndex = selectedTab.coerceAtLeast(0),
                        onItemSelected = { index ->
                            navController.navigateToTab(tabs[index].destination)
                        },
                        onFabClick = {
                            if (sessionState.canCapture) {
                                navController.navigate(GlowDestination.Capture)
                            }
                        },
                        enabled = sessionState.canCapture,
                        fabContentDescription = if (sessionState.canCapture) {
                            "Capture a skin progress photo"
                        } else {
                            "Capture locked until facial-data consent is active"
                        },
                    )
                }
            },
        ) { innerPadding ->
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                if (isWorkspaceRoute && !isAuthoritative) {
                    SessionBlockingSurface(sessionState = sessionState, onRetry = retry)
                } else {
                    GlowNavGraph(navController = navController)
                }
            }
        }
    }
}

@Composable
private fun SessionBlockingSurface(sessionState: SessionState, onRetry: () -> Unit) {
    val glow = LocalGlowColors.current
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = glow.paper,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .semantics { contentDescription = "Checking your GlowUp AI session" },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            when (sessionState) {
                is SessionState.Unrecoverable -> {
                    Text("We couldn't verify this session", style = MaterialTheme.typography.titleLarge)
                    Text(
                        "Your workspace is paused until we can confirm your profile.",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                    Button(onClick = onRetry, modifier = Modifier.padding(top = 20.dp)) {
                        Text("Try again")
                    }
                }
                else -> {
                    CircularProgressIndicator(
                        color = glow.honey600,
                        modifier = Modifier.semantics {
                            contentDescription = "Loading your authoritative profile"
                        },
                    )
                    Text(
                        "Checking your private profile…",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(top = 16.dp),
                    )
                }
            }
        }
    }
}

private data class ShellTab(
    val destination: GlowDestination,
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val routes: Set<KClass<out GlowDestination>>,
)

private val tabs = listOf(
    ShellTab(
        GlowDestination.Home,
        "Home",
        Icons.Filled.Home,
        setOf(GlowDestination.Home::class),
    ),
    ShellTab(
        GlowDestination.Routine,
        "Routine",
        Icons.Filled.Science,
        setOf(
            GlowDestination.Routine::class,
            GlowDestination.ProductDetail::class,
            GlowDestination.ShelfScan::class,
            GlowDestination.Experiments::class,
            GlowDestination.ExperimentDetail::class,
        ),
    ),
    ShellTab(
        GlowDestination.Insights,
        "Insights",
        Icons.Filled.Insights,
        setOf(
            GlowDestination.Insights::class,
            GlowDestination.QnaThread::class,
            GlowDestination.ContextLog::class,
            GlowDestination.RootCause::class,
            GlowDestination.BudgetOptimizer::class,
            GlowDestination.DermExport::class,
        ),
    ),
    ShellTab(
        GlowDestination.Account,
        "You",
        Icons.Filled.Person,
        setOf(
            GlowDestination.Account::class,
            GlowDestination.Paywall::class,
            GlowDestination.Settings::class,
            GlowDestination.DataAndPrivacy::class,
        ),
    ),
)

private fun NavDestination?.selectedTabIndex(): Int =
    tabs.indexOfFirst { tab ->
        tab.routes.any { route -> this?.hierarchy?.any { it.matchesRoute(route) } == true }
    }

private fun NavDestination?.isWorkspaceDestination(): Boolean =
    this != null && WORKSPACE_ROUTE_CLASSES.any { matchesRoute(it) }

private fun NavDestination?.isHomeDestination(): Boolean =
    this != null && matchesRoute(GlowDestination.Home::class)

private fun NavDestination.matchesRoute(routeClass: KClass<out GlowDestination>): Boolean {
    val actual = route?.substringBefore('?') ?: return false
    val expected = routeClass.qualifiedName ?: return false
    return actual == expected || actual.startsWith("$expected/")
}

private val WORKSPACE_ROUTE_CLASSES = setOf(
    GlowDestination.Home::class,
    GlowDestination.Routine::class,
    GlowDestination.Capture::class,
    GlowDestination.CaptureResult::class,
    GlowDestination.ProductDetail::class,
    GlowDestination.ShelfScan::class,
    GlowDestination.Experiments::class,
    GlowDestination.ExperimentDetail::class,
    GlowDestination.Insights::class,
    GlowDestination.QnaThread::class,
    GlowDestination.ContextLog::class,
    GlowDestination.RootCause::class,
    GlowDestination.BudgetOptimizer::class,
    GlowDestination.DermExport::class,
    GlowDestination.Discover::class,
    GlowDestination.Account::class,
    GlowDestination.Paywall::class,
    GlowDestination.Settings::class,
    GlowDestination.DataAndPrivacy::class,
)

@Composable
private fun DeepLinkEffect(
    destination: GlowDestination?,
    sessionState: SessionState,
    navController: NavHostController,
    onConsumed: (GlowDestination) -> Unit,
) {
    LaunchedEffect(destination, sessionState) {
        val target = destination ?: return@LaunchedEffect
        if (!sessionState.isAuthoritative) return@LaunchedEffect

        val admittedTarget = if (target == GlowDestination.Capture && !sessionState.canCapture) {
            when (sessionState) {
                is SessionState.ConsentRequired,
                is SessionState.ConsentDeclined -> com.glowup.ai.feature.auth.destinationFor(sessionState) ?: GlowDestination.Home
                else -> GlowDestination.Home
            }
        } else {
            target
        }
        navController.navigate(admittedTarget) { launchSingleTop = true }
        onConsumed(target)
    }
}

@Composable
private fun NotificationPermissionEffect(
    currentDestination: NavDestination?,
    sessionStore: SessionStore,
    canEnterWorkspace: Boolean,
) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
    val context = androidx.compose.ui.platform.LocalContext.current
    val prompted by sessionStore.notificationPermissionPromptedFlow
        .collectAsStateWithLifecycle(initialValue = false)
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    val isHome = currentDestination.isHomeDestination()
    val alreadyGranted = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.POST_NOTIFICATIONS,
    ) == PackageManager.PERMISSION_GRANTED

    LaunchedEffect(isHome, prompted, canEnterWorkspace, alreadyGranted) {
        if (isHome && canEnterWorkspace && !prompted && !alreadyGranted) {
            sessionStore.setNotificationPermissionPrompted(true)
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
