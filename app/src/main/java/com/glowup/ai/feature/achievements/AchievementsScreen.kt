package com.glowup.ai.feature.achievements

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.glowup.ai.core.ui.AchievementGrid
import com.glowup.ai.core.ui.ErrorState
import com.glowup.ai.core.ui.GlowTopBar
import com.glowup.ai.core.ui.ShimmerSkeleton
import com.glowup.ai.domain.model.UserAchievement

/**
 * Achievements Screen
 * Full-screen view of all achievements with filters and stats
 */
@Composable
fun AchievementsRoute(
    onBack: () -> Unit,
    viewModel: AchievementsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    AchievementsScreen(
        state = state,
        onBack = onBack,
        onRetry = viewModel::onRetryRequested,
        onAchievementClick = { achievement ->
            // Could add detail view in the future
        }
    )
}

@Composable
fun AchievementsScreen(
    state: AchievementsUiState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onAchievementClick: (UserAchievement) -> Unit
) {
    Scaffold(
        topBar = {
            GlowTopBar(
                title = "Achievements",
                onBack = onBack
            )
        }
    ) { padding ->
        when (state) {
            is AchievementsUiState.Loading -> {
                AchievementsLoadingSkeleton(padding)
            }
            is AchievementsUiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(24.dp)
                ) {
                    ErrorState(
                        message = state.message,
                        onRetry = onRetry
                    )
                }
            }
            is AchievementsUiState.Content -> {
                AchievementGrid(
                    achievements = state.achievements,
                    onAchievementClick = onAchievementClick,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                )
            }
        }
    }
}

@Composable
private fun AchievementsLoadingSkeleton(padding: PaddingValues) {
    androidx.compose.foundation.layout.Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(20.dp),
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(16.dp)
    ) {
        ShimmerSkeleton(height = 120.dp, cornerRadius = 16.dp)
        ShimmerSkeleton(height = 60.dp, cornerRadius = 12.dp)
        ShimmerSkeleton(height = 200.dp, cornerRadius = 16.dp)
        ShimmerSkeleton(height = 200.dp, cornerRadius = 16.dp)
    }
}
