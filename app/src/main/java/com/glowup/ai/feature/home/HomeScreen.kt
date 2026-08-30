package com.glowup.ai.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import com.glowup.ai.core.ui.AchievementCelebration
import com.glowup.ai.core.ui.AchievementSummary
import com.glowup.ai.core.ui.CompactCalendarHeatmap
import com.glowup.ai.core.ui.DisclaimerNote
import com.glowup.ai.core.ui.ErrorState
import com.glowup.ai.core.ui.GlowTopBar
import com.glowup.ai.core.ui.ShimmerSkeleton
import com.glowup.ai.core.ui.StreakCounter
import com.glowup.ai.domain.StreakCalculator
import com.glowup.ai.domain.SessionStateMachine
import com.glowup.ai.core.util.GlowResult
import com.glowup.ai.domain.model.CheckInRoutineState
import com.glowup.ai.domain.model.CheckInSkinFeel
import com.glowup.ai.domain.model.Dashboard
import com.glowup.ai.feature.home.components.CaptureGuideBanner
import com.glowup.ai.feature.home.components.CheckInSection
import com.glowup.ai.feature.home.components.CheckInSheet
import com.glowup.ai.feature.home.components.DiscoverShortcutCard
import com.glowup.ai.feature.home.components.ExperimentsSection
import com.glowup.ai.feature.home.components.HistoryTrendSection
import com.glowup.ai.feature.home.components.HomeStatsSection
import com.glowup.ai.feature.home.components.RoutineTimelineSection
import com.glowup.ai.feature.home.components.VerdictsSection
import com.glowup.ai.feature.home.components.WeeklyRecapCard
import com.glowup.ai.feature.shell.GlowDestination

/**
 * `feature/home` entry point. `GET /dashboard` is the single initial snapshot for this screen —
 * `weekly_recap` and `check_ins` are rendered straight from the fields it embeds, never from a
 * second round trip (task 3.3 deliverable #1). Trap #7: this screen never polls `GET /dashboard`
 * or `GET /engagement` — [HomeViewModel] only refetches on first composition, an explicit refresh
 * tap, or after the one mutation this screen owns (a check-in submit).
 */
@Composable
fun HomeRoute(
    onNavigate: (GlowDestination) -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    HomeScreen(
        state = state,
        onRefresh = viewModel::onRefreshRequested,
        onRetry = viewModel::onRetryRequested,
        onMetricSelected = viewModel::onMetricSelected,
        onCheckInClick = viewModel::onCheckInSheetRequested,
        onCheckInDismiss = viewModel::onCheckInSheetDismissed,
        onCheckInSubmit = viewModel::onCheckInSubmitted,
        onFreezeDayUsed = viewModel::onFreezeDayUsed,
        onAchievementCelebrationDismiss = viewModel::onAchievementCelebrationDismissed,
        onNavigate = onNavigate,
    )
}

@Composable
fun HomeScreen(
    state: HomeUiState,
    onRefresh: () -> Unit,
    onRetry: () -> Unit,
    onMetricSelected: (com.glowup.ai.domain.model.PrimaryMetric) -> Unit,
    onCheckInClick: () -> Unit,
    onCheckInDismiss: () -> Unit,
    onCheckInSubmit: (CheckInRoutineState, CheckInSkinFeel, String?) -> Unit,
    onFreezeDayUsed: () -> Unit,
    onAchievementCelebrationDismiss: () -> Unit,
    onNavigate: (GlowDestination) -> Unit,
) {
    Scaffold(
        topBar = {
            GlowTopBar(
                title = "Home",
                actions = {
                    IconButton(onClick = onRefresh) {
                        Icon(imageVector = Icons.Filled.Refresh, contentDescription = "Refresh dashboard")
                    }
                },
            )
        },
    ) { padding ->
        when (state) {
            is HomeUiState.Loading -> HomeLoadingSkeleton(padding)
            is HomeUiState.Error -> Box(modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp)) {
                ErrorState(message = state.message, onRetry = onRetry)
            }
            is HomeUiState.Content -> HomeContent(
                padding = padding,
                state = state,
                onMetricSelected = onMetricSelected,
                onCheckInClick = onCheckInClick,
                onFreezeDayUsed = onFreezeDayUsed,
                onNavigate = onNavigate,
            )
        }

        if (state is HomeUiState.Content && state.checkInSheetVisible) {
            CheckInSheet(
                onDismiss = onCheckInDismiss,
                submitting = state.checkInSubmitting,
                errorMessage = state.checkInError,
                onSubmit = onCheckInSubmit,
            )
        }

        // Achievement celebration dialog
        if (state is HomeUiState.Content && state.celebrationAchievement != null) {
            AchievementCelebration(
                achievement = state.celebrationAchievement,
                onDismiss = onAchievementCelebrationDismiss
            )
        }
    }
}

@Composable
private fun HomeLoadingSkeleton(padding: PaddingValues) {
    androidx.compose.foundation.layout.Column(
        modifier = Modifier.fillMaxSize().padding(padding).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        ShimmerSkeleton(height = 120.dp, cornerRadius = 24.dp)
        ShimmerSkeleton(height = 90.dp)
        ShimmerSkeleton(height = 180.dp, cornerRadius = 18.dp)
        ShimmerSkeleton(height = 220.dp, cornerRadius = 18.dp)
    }
}

@Composable
private fun HomeContent(
    padding: PaddingValues,
    state: HomeUiState.Content,
    onMetricSelected: (com.glowup.ai.domain.model.PrimaryMetric) -> Unit,
    onCheckInClick: () -> Unit,
    onFreezeDayUsed: () -> Unit,
    onNavigate: (GlowDestination) -> Unit,
) {
    val dashboard: Dashboard = state.dashboard
    // The ONE canonical place Premium is decided is SessionState.canUsePremium — never a local
    // `plan == "premium"` re-derivation (ANDROID_PLAN.md §3 bug #2 / trap #6/#11/#12). Home has
    // its own fresh Profile from the dashboard snapshot, so it runs it back through the same
    // state machine every other screen uses rather than reading `Entitlement.isPremium` loose.
    val sessionState = SessionStateMachine.onProfileResult(GlowResult.Success(dashboard.profile))
    val isPremium = sessionState.canUsePremium
    val captureEnabled = sessionState.canCapture

    // PERFORMANCE: Use derivedStateOf to avoid re-sorting on every recomposition
    // (PERFORMANCE_OPTIMIZATIONS.md §2.1 - sortedBy is O(n log n), expensive for large history)
    val sortedHistory = remember(state.history) {
        derivedStateOf { state.history.sortedBy { it.capturedAt } }
    }.value
    val latest = remember(sortedHistory) {
        derivedStateOf { sortedHistory.lastOrNull() }
    }.value
    val previous = remember(sortedHistory) {
        derivedStateOf { sortedHistory.getOrNull(sortedHistory.size - 2) }
    }.value

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 20.dp,
            end = 20.dp,
            top = padding.calculateTopPadding() + 16.dp,
            bottom = padding.calculateBottomPadding() + 32.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        item {
            DisclaimerNote(text = dashboard.disclaimer)
        }

        if (state.dashboardStale) {
            item {
                com.glowup.ai.core.ui.PollingIndicator(
                    message = state.dashboardRefreshError?.let { "Showing your last saved dashboard — $it" }
                        ?: "Showing your last saved dashboard.",
                )
            }
        }

        item {
            StreakCounter(
                streak = state.streak,
                onFreezeDayClick = onFreezeDayUsed,
                showWarning = StreakCalculator.wouldStreakBreak(state.streak),
                warningMessage = StreakCalculator.getStreakWarning(state.streak),
            )
        }

        item {
            HomeStatsSection(
                engagement = dashboard.engagement,
                latest = latest,
                previous = previous,
            )
        }

        item {
            // PERFORMANCE: Use derivedStateOf to avoid re-parsing dates on every recomposition
            // (PERFORMANCE_OPTIMIZATIONS.md §2.1 - Instant.parse is expensive, runs 30+ times for typical user)
            val captureDates = remember(sortedHistory) {
                derivedStateOf {
                    sortedHistory.mapNotNull { capture ->
                        try {
                            capture.capturedAt?.let { isoString ->
                                Instant.parse(isoString)
                                    .atZone(ZoneId.systemDefault())
                                    .toLocalDate()
                            }
                        } catch (e: Exception) {
                            null
                        }
                    }.toSet()
                }
            }.value

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Capture Calendar",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = com.glowup.ai.core.design.LocalGlowColors.current.ink
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    CompactCalendarHeatmap(
                        captureDates = captureDates,
                        onDateClick = { date ->
                            // Find the capture for this date and navigate
                            val captureForDate = sortedHistory.find { capture ->
                                try {
                                    capture.capturedAt?.let { isoString ->
                                        Instant.parse(isoString)
                                            .atZone(ZoneId.systemDefault())
                                            .toLocalDate() == date
                                    } ?: false
                                } catch (e: Exception) {
                                    false
                                }
                            }
                            // For now, navigate to Capture screen
                            // TODO: Navigate to specific capture detail when captureForDate.id is available
                            onNavigate(GlowDestination.Capture)
                        }
                    )
                }
            }
        }

        // Achievements section
        if (state.achievements.isNotEmpty()) {
            item {
                AchievementSummary(
                    achievements = state.achievements,
                    onClick = { onNavigate(GlowDestination.Achievements) }
                )
            }
        }

        item {
            CaptureGuideBanner(
                guide = dashboard.engagement?.guide,
                onCaptureClick = { onNavigate(GlowDestination.Capture) },
                captureEnabled = captureEnabled,
            )
        }

        item {
            DiscoverShortcutCard(onClick = { onNavigate(GlowDestination.Discover) })
        }

        item {
            WeeklyRecapCard(recap = dashboard.weeklyRecap)
        }

        item {
            VerdictsSection(
                verdicts = dashboard.verdicts,
                features = dashboard.features,
                isPremium = isPremium,
                onLogRoutine = { onNavigate(GlowDestination.Routine) },
                onUnlockPremium = { onNavigate(GlowDestination.Paywall) },
            )
        }

        item {
            HistoryTrendSection(
                history = sortedHistory,
                selectedMetric = state.selectedMetric,
                onMetricSelected = onMetricSelected,
                onCaptureAgain = { onNavigate(GlowDestination.Capture) },
                captureEnabled = captureEnabled,
            )
            if (state.historyError != null) {
                androidx.compose.material3.Text(
                    text = state.historyError,
                    style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
                    color = com.glowup.ai.core.design.LocalGlowColors.current.danger,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }

        item {
            ExperimentsSection(
                experiments = dashboard.experiments,
                isPremium = isPremium,
                onStartExperiment = { onNavigate(GlowDestination.Experiments) },
                onUnlockPremium = { onNavigate(GlowDestination.Paywall) },
                onOpenExperiment = { id -> onNavigate(GlowDestination.ExperimentDetail(id)) },
            )
        }

        item {
            RoutineTimelineSection(
                events = dashboard.routineEvents,
                onLogRoutine = { onNavigate(GlowDestination.Routine) },
            )
        }

        item {
            CheckInSection(
                checkIns = dashboard.checkIns,
                onCheckInClick = onCheckInClick,
            )
        }
    }
}
