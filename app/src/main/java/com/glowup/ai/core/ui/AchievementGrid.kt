package com.glowup.ai.core.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.glowup.ai.domain.model.AchievementTier
import com.glowup.ai.domain.model.UserAchievement

/**
 * Achievement Grid Component
 * Displays all achievements in organized grid layout
 * Inspired by Xbox/PlayStation achievement systems
 */
@Composable
fun AchievementGrid(
    achievements: List<UserAchievement>,
    onAchievementClick: (UserAchievement) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
    ) {
        // Stats header
        AchievementStats(achievements)

        Spacer(modifier = Modifier.height(16.dp))

        // Filter tabs
        var selectedFilter by remember { mutableStateOf(AchievementFilter.ALL) }

        FilterTabs(
            selectedFilter = selectedFilter,
            onFilterSelected = { selectedFilter = it },
            achievements = achievements,
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Grid of achievements
        val filteredAchievements =
            when (selectedFilter) {
                AchievementFilter.ALL -> achievements
                AchievementFilter.UNLOCKED -> achievements.filter { it.isUnlocked }
                AchievementFilter.LOCKED -> achievements.filter { !it.isUnlocked }
                AchievementFilter.BRONZE -> achievements.filter { it.type.tier == AchievementTier.BRONZE }
                AchievementFilter.SILVER -> achievements.filter { it.type.tier == AchievementTier.SILVER }
                AchievementFilter.GOLD -> achievements.filter { it.type.tier == AchievementTier.GOLD }
                AchievementFilter.PLATINUM -> achievements.filter { it.type.tier == AchievementTier.PLATINUM }
            }

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(filteredAchievements) { achievement ->
                AchievementCard(
                    achievement = achievement,
                    onClick = { onAchievementClick(achievement) },
                )
            }
        }
    }
}

/**
 * Achievement stats summary
 */
@Composable
private fun AchievementStats(
    achievements: List<UserAchievement>,
    modifier: Modifier = Modifier,
) {
    val unlockedCount = achievements.count { it.isUnlocked }
    val totalCount = achievements.size
    val progressPercent =
        if (totalCount > 0) {
            (unlockedCount.toFloat() / totalCount.toFloat()) * 100
        } else {
            0f
        }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
            ),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.EmojiEvents,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp),
                    )
                    Column {
                        Text(
                            text = "$unlockedCount / $totalCount",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                        Text(
                            text = "Achievements Unlocked",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                        )
                    }
                }
            }

            // Progress circle
            Box(
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(
                    progress = progressPercent / 100f,
                    modifier = Modifier.size(60.dp),
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 6.dp,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                )
                Text(
                    text = "${progressPercent.toInt()}%",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

/**
 * Filter tabs for achievement categories
 */
@Composable
private fun FilterTabs(
    selectedFilter: AchievementFilter,
    onFilterSelected: (AchievementFilter) -> Unit,
    achievements: List<UserAchievement>,
    modifier: Modifier = Modifier,
) {
    ScrollableTabRow(
        selectedTabIndex = selectedFilter.ordinal,
        modifier = modifier,
        edgePadding = 0.dp,
        divider = {},
    ) {
        AchievementFilter.values().forEach { filter ->
            val count =
                when (filter) {
                    AchievementFilter.ALL -> achievements.size
                    AchievementFilter.UNLOCKED -> achievements.count { it.isUnlocked }
                    AchievementFilter.LOCKED -> achievements.count { !it.isUnlocked }
                    AchievementFilter.BRONZE -> achievements.count { it.type.tier == AchievementTier.BRONZE }
                    AchievementFilter.SILVER -> achievements.count { it.type.tier == AchievementTier.SILVER }
                    AchievementFilter.GOLD -> achievements.count { it.type.tier == AchievementTier.GOLD }
                    AchievementFilter.PLATINUM -> achievements.count { it.type.tier == AchievementTier.PLATINUM }
                }

            Tab(
                selected = selectedFilter == filter,
                onClick = { onFilterSelected(filter) },
                text = {
                    Text(
                        text = "${filter.displayName} ($count)",
                        style = MaterialTheme.typography.labelLarge,
                    )
                },
            )
        }
    }
}

/**
 * Achievement filter options
 */
enum class AchievementFilter(
    val displayName: String,
) {
    ALL("All"),
    UNLOCKED("Unlocked"),
    LOCKED("Locked"),
    BRONZE("Bronze"),
    SILVER("Silver"),
    GOLD("Gold"),
    PLATINUM("Platinum"),
}

/**
 * Compact achievement summary for dashboard
 */
@Composable
fun AchievementSummary(
    achievements: List<UserAchievement>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val recentAchievements =
        achievements
            .filter { it.isUnlocked }
            .sortedByDescending { it.unlockedAt }
            .take(3)

    Card(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface,
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Recent Achievements",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "View All",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (recentAchievements.isEmpty()) {
                Text(
                    text = "Complete actions to unlock achievements!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                )
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    recentAchievements.forEach { achievement ->
                        AchievementBadge(
                            achievement = achievement,
                            modifier = Modifier.weight(1f),
                            size = 48,
                        )
                    }
                    // Fill remaining slots with placeholders
                    repeat(3 - recentAchievements.size) {
                        Box(
                            modifier =
                                Modifier
                                    .weight(1f)
                                    .aspectRatio(1f),
                        )
                    }
                }
            }
        }
    }
}
