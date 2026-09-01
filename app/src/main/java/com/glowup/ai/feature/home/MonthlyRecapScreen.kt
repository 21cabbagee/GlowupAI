package com.glowup.ai.feature.home

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.glowup.ai.core.design.LocalGlowColors
import com.glowup.ai.core.ui.GlowButton
import com.glowup.ai.core.ui.GlowTopBar
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.*

/**
 * Monthly Recap Screen
 * Celebratory summary of user's progress over the month
 * Inspired by Spotify Wrapped, Strava Year in Review
 * Key retention feature - makes progress feel meaningful
 */
@Composable
fun MonthlyRecapScreen(
    month: YearMonth,
    stats: MonthlyStats,
    onShareClick: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val glowColors = LocalGlowColors.current
    val honeyColor = glowColors.honey500
    val inkColor = glowColors.ink900

    Scaffold(
        topBar = {
            GlowTopBar(
                title = "Your ${month.month.getDisplayName(TextStyle.FULL, Locale.getDefault())} Journey",
                onBack = onBackClick,
            )
        },
    ) { padding ->
        Column(
            modifier =
                modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            // Hero card - main achievement
            HeroCard(stats, honeyColor, inkColor)

            // Stats grid
            StatsGrid(stats)

            // Before/After comparison (if available)
            if (stats.firstCaptureUrl != null && stats.lastCaptureUrl != null) {
                BeforeAfterComparison(
                    beforeUrl = stats.firstCaptureUrl,
                    afterUrl = stats.lastCaptureUrl,
                    daysBetween = stats.daysBetween,
                )
            }

            // Key insights
            KeyInsights(stats)

            // Achievements unlocked
            if (stats.achievementsUnlocked.isNotEmpty()) {
                AchievementsSection(stats.achievementsUnlocked)
            }

            // Products discovered
            if (stats.productsAdded > 0) {
                ProductsSection(stats)
            }

            // Experiments run
            if (stats.experimentsCompleted > 0) {
                ExperimentsSection(stats)
            }

            // Share button
            Spacer(modifier = Modifier.height(8.dp))

            GlowButton(
                text = "Share Your Progress",
                onClick = onShareClick,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

/**
 * Hero card with main stat
 */
@Composable
private fun HeroCard(
    stats: MonthlyStats,
    honeyColor: Color,
    inkColor: Color,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors =
            CardDefaults.cardColors(
                containerColor = honeyColor,
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Main number with animation
            var animatedCaptures by remember { mutableStateOf(0) }
            LaunchedEffect(stats.totalCaptures) {
                animate(
                    initialValue = 0f,
                    targetValue = stats.totalCaptures.toFloat(),
                    animationSpec = tween(1500, easing = EaseOutCubic),
                ) { value, _ ->
                    animatedCaptures = value.toInt()
                }
            }

            Text(
                text = "$animatedCaptures",
                style =
                    MaterialTheme.typography.displayLarge.copy(
                        fontSize = MaterialTheme.typography.displayLarge.fontSize * 1.5f,
                    ),
                fontWeight = FontWeight.ExtraBold,
                color = inkColor,
            )

            Text(
                text = "Captures This Month",
                style = MaterialTheme.typography.titleLarge,
                color = inkColor.copy(alpha = 0.9f),
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Streak maintained
            if (stats.streakMaintained) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.LocalFireDepartment,
                        contentDescription = null,
                        tint = inkColor,
                        modifier = Modifier.size(24.dp),
                    )
                    Text(
                        text = "Streak maintained all month!",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = inkColor,
                    )
                }
            }
        }
    }
}

/**
 * Grid of secondary stats
 */
@Composable
private fun StatsGrid(stats: MonthlyStats) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            StatCard(
                icon = Icons.Filled.CheckCircle,
                value = "${stats.daysActive}",
                label = "Active Days",
                modifier = Modifier.weight(1f),
            )
            StatCard(
                icon = Icons.Filled.Percent,
                value = "${stats.consistencyPercent}%",
                label = "Consistency",
                modifier = Modifier.weight(1f),
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            StatCard(
                icon = Icons.Filled.TrendingUp,
                value = "${stats.improvementMetrics.size}",
                label = "Metrics Improved",
                modifier = Modifier.weight(1f),
            )
            StatCard(
                icon = Icons.Filled.Science,
                value = "${stats.experimentsCompleted}",
                label = "Experiments Done",
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun StatCard(
    icon: ImageVector,
    value: String,
    label: String,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
            ),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp),
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/**
 * Before/After comparison section
 */
@Composable
private fun BeforeAfterComparison(
    beforeUrl: String,
    afterUrl: String,
    daysBetween: Int,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface,
            ),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
        ) {
            Text(
                text = "Your Progress",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // Before
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .aspectRatio(0.75f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                    ) {
                        // TODO: Load actual image with Coil
                        Text(
                            text = "Before",
                            modifier = Modifier.align(Alignment.Center),
                        )
                    }
                    Text(
                        text = "Start of Month",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }

                // After
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .aspectRatio(0.75f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                    ) {
                        // TODO: Load actual image with Coil
                        Text(
                            text = "After",
                            modifier = Modifier.align(Alignment.Center),
                        )
                    }
                    Text(
                        text = "End of Month",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "$daysBetween days of consistent tracking",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/**
 * Key insights section
 */
@Composable
private fun KeyInsights(stats: MonthlyStats) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            ),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.Lightbulb,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onTertiaryContainer,
                )
                Text(
                    text = "Key Insights",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            stats.insights.forEach { insight ->
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(
                        modifier =
                            Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                                .align(Alignment.CenterVertically),
                    )
                    Text(
                        text = insight,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

/**
 * Achievements section
 */
@Composable
private fun AchievementsSection(achievements: List<String>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface,
            ),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
        ) {
            Text(
                text = "🏆 Achievements Unlocked",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )

            Spacer(modifier = Modifier.height(12.dp))

            achievements.forEach { achievement ->
                Text(
                    text = "✨ $achievement",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(vertical = 4.dp),
                )
            }
        }
    }
}

/**
 * Products section
 */
@Composable
private fun ProductsSection(stats: MonthlyStats) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface,
            ),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
        ) {
            Text(
                text = "🧴 Skincare Journey",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Added ${stats.productsAdded} new products to your routine",
                style = MaterialTheme.typography.bodyMedium,
            )

            Text(
                text = "Logged ${stats.routineEvents} routine events",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

/**
 * Experiments section
 */
@Composable
private fun ExperimentsSection(stats: MonthlyStats) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface,
            ),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
        ) {
            Text(
                text = "🔬 Skin Science",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Completed ${stats.experimentsCompleted} A/B tests",
                style = MaterialTheme.typography.bodyMedium,
            )

            if (stats.experimentsCompleted > 0) {
                Text(
                    text = "You're using evidence-based skincare!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
    }
}

/**
 * Monthly stats data model
 */
data class MonthlyStats(
    val totalCaptures: Int,
    val daysActive: Int,
    val consistencyPercent: Int,
    val streakMaintained: Boolean,
    val improvementMetrics: List<String>,
    val experimentsCompleted: Int,
    val productsAdded: Int,
    val routineEvents: Int,
    val achievementsUnlocked: List<String>,
    val insights: List<String>,
    val firstCaptureUrl: String? = null,
    val lastCaptureUrl: String? = null,
    val daysBetween: Int = 0,
)
