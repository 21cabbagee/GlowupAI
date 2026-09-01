package com.glowup.ai.feature.onboarding

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.glowup.ai.core.design.GlowMotion
import com.glowup.ai.core.design.GlowSpacing
import com.glowup.ai.core.design.LocalGlowColors
import com.glowup.ai.core.design.rememberReducedMotion
import com.glowup.ai.core.ui.ErrorState
import com.glowup.ai.core.ui.GlowButton
import com.glowup.ai.feature.shell.GlowDestination
import kotlinx.coroutines.launch

/**
 * Enhanced onboarding flow with comprehensive tutorial screens, permission requests,
 * and smooth animations. Implements the complete onboarding experience as per
 * GlowUp AI design requirements.
 */
@Composable
fun EnhancedOnboardingRoute(
    navController: NavController,
    viewModel: EnhancedOnboardingViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val target by viewModel.navigationTarget.collectAsStateWithLifecycle()

    LaunchedEffect(target) {
        val destination = target ?: return@LaunchedEffect
        navController.navigate(destination) {
            popUpTo(GlowDestination.Onboarding) { inclusive = true }
            launchSingleTop = true
        }
        viewModel.consumeNavigationTarget()
    }

    when (uiState) {
        is EnhancedOnboardingUiState.Loading -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Text("Loading...")
            }
        }

        is EnhancedOnboardingUiState.Error -> {
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(GlowSpacing.lg),
                contentAlignment = Alignment.Center,
            ) {
                ErrorState(
                    message = (uiState as EnhancedOnboardingUiState.Error).message,
                    onRetry = viewModel::retry,
                )
            }
        }

        is EnhancedOnboardingUiState.Content -> {
            EnhancedOnboardingContent(
                onComplete = viewModel::completeOnboarding,
                onSkip = viewModel::skipOnboarding,
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun EnhancedOnboardingContent(
    onComplete: () -> Unit,
    onSkip: () -> Unit,
) {
    val glow = LocalGlowColors.current
    val pagerState = rememberPagerState(pageCount = { 9 }) // 9 total screens
    val scope = rememberCoroutineScope()
    val reducedMotion = rememberReducedMotion()

    Box(modifier = Modifier.fillMaxSize()) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            userScrollEnabled = false, // Control navigation via buttons
        ) { page ->
            when (page) {
                0 -> WelcomeScreen()
                1 -> TutorialStreaksScreen()
                2 -> TutorialPhotosScreen()
                3 -> TutorialMetricsScreen()
                4 -> TutorialExperimentsScreen()
                5 -> CameraPermissionScreen()
                6 -> NotificationPermissionScreen()
                7 -> BaselinePhotoScreen()
                8 -> RoutineSetupScreen()
            }
        }

        // Page indicators
        Row(
            modifier =
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 120.dp)
                    .semantics {
                        contentDescription = "Page ${pagerState.currentPage + 1} of ${pagerState.pageCount}"
                    },
            horizontalArrangement = Arrangement.Center,
        ) {
            repeat(pagerState.pageCount) { index ->
                val active = index == pagerState.currentPage
                val width by animateDpAsState(
                    targetValue = if (active) 24.dp else 8.dp,
                    animationSpec =
                        GlowMotion.respectingReducedMotion(
                            GlowMotion.standard,
                            reducedMotion,
                        ) as androidx.compose.animation.core.AnimationSpec<androidx.compose.ui.unit.Dp>,
                    label = "indicatorWidth",
                )
                Box(
                    modifier =
                        Modifier
                            .padding(horizontal = 4.dp)
                            .height(8.dp)
                            .width(width)
                            .clip(CircleShape)
                            .background(
                                if (active) glow.honey500 else glow.ink600.copy(alpha = 0.25f),
                            ),
                )
            }
        }

        // Navigation buttons
        Column(
            modifier =
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(GlowSpacing.lg)
                    .padding(bottom = GlowSpacing.md),
        ) {
            val isFirstPage = pagerState.currentPage == 0
            val isLastPage = pagerState.currentPage == pagerState.pageCount - 1

            GlowButton(
                modifier = Modifier.fillMaxWidth(),
                text =
                    when {
                        isLastPage -> "Get Started"
                        isFirstPage -> "Start Tour"
                        else -> "Continue"
                    },
                onClick = {
                    if (isLastPage) {
                        onComplete()
                    } else {
                        scope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    }
                },
            )

            // Skip button (hide on last page)
            AnimatedVisibility(
                visible = !isLastPage,
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                TextButton(
                    onClick = onSkip,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(top = GlowSpacing.sm),
                ) {
                    Text(
                        text = "Skip",
                        style = MaterialTheme.typography.labelLarge,
                        color = glow.ink600,
                    )
                }
            }
        }
    }
}
