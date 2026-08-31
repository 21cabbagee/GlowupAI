package com.glowup.ai.feature.analytics

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.glowup.ai.core.design.GlowSpacing
import com.glowup.ai.core.design.LocalGlowColors
import com.glowup.ai.core.ui.GlowTopBar
import com.glowup.ai.core.ui.SectionHeader
import com.glowup.ai.core.ui.StatTile

/**
 * Admin Analytics Screen
 *
 * Provides internal metrics for app performance, user engagement, and system health.
 * This screen is intended for internal use and monitoring.
 *
 * Note: This implementation uses placeholder data. In a production environment,
 * this would connect to backend analytics APIs and Firebase Analytics.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminAnalyticsScreen(
    onNavigateBack: () -> Unit,
) {
    val glow = LocalGlowColors.current

    Scaffold(
        topBar = {
            GlowTopBar(
                title = "Admin Analytics",
                onNavigateUp = onNavigateBack
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(GlowSpacing.md),
            verticalArrangement = Arrangement.spacedBy(GlowSpacing.lg)
        ) {
            // User Retention Metrics
            item {
                SectionHeader(title = "User Retention")
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(GlowSpacing.sm)
                ) {
                    StatTile(
                        label = "DAU",
                        value = "1.2K",
                        modifier = Modifier.weight(1f)
                    )
                    StatTile(
                        label = "WAU",
                        value = "5.4K",
                        modifier = Modifier.weight(1f)
                    )
                    StatTile(
                        label = "MAU",
                        value = "18.3K",
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                MetricCard(
                    title = "Retention Rates",
                    metrics = listOf(
                        MetricRow("Day 1 Retention", "68%", Color(0xFF4CAF50)),
                        MetricRow("Day 7 Retention", "42%", Color(0xFF4CAF50)),
                        MetricRow("Day 30 Retention", "28%", Color(0xFFFFC107)),
                        MetricRow("Churn Rate", "12%", Color(0xFFF44336))
                    )
                )
            }

            // Feature Usage Stats
            item {
                SectionHeader(
                    title = "Feature Usage",
                    modifier = Modifier.padding(top = GlowSpacing.md)
                )
            }

            item {
                MetricCard(
                    title = "Daily Active Features",
                    metrics = listOf(
                        MetricRow("Capture", "1,234 sessions", glow.honey700),
                        MetricRow("Home Dashboard", "892 views", glow.sage),
                        MetricRow("Routine Tracking", "567 logs", Color(0xFF9C27B0)),
                        MetricRow("Insights", "234 views", Color(0xFF2196F3)),
                        MetricRow("Experiments", "123 active", Color(0xFFFF5722))
                    )
                )
            }

            item {
                MetricCard(
                    title = "Premium Features",
                    metrics = listOf(
                        MetricRow("Premium Users", "342", glow.honey700),
                        MetricRow("Conversion Rate", "4.2%", Color(0xFF4CAF50)),
                        MetricRow("Avg. LTV", "$48", glow.honey700),
                        MetricRow("MRR", "$16,416", Color(0xFF4CAF50))
                    )
                )
            }

            // Error Rates & Performance
            item {
                SectionHeader(
                    title = "System Health",
                    modifier = Modifier.padding(top = GlowSpacing.md)
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(GlowSpacing.sm)
                ) {
                    HealthStatTile(
                        label = "Error Rate",
                        value = "0.8%",
                        icon = Icons.Filled.Error,
                        status = HealthStatus.WARNING,
                        modifier = Modifier.weight(1f)
                    )
                    HealthStatTile(
                        label = "API Success",
                        value = "99.2%",
                        icon = Icons.Filled.CheckCircle,
                        status = HealthStatus.GOOD,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                MetricCard(
                    title = "Performance Metrics",
                    metrics = listOf(
                        MetricRow("Avg. Load Time", "1.2s", Color(0xFF4CAF50)),
                        MetricRow("API Response Time", "320ms", Color(0xFF4CAF50)),
                        MetricRow("Crash Rate", "0.3%", Color(0xFF4CAF50)),
                        MetricRow("ANR Rate", "0.1%", Color(0xFF4CAF50))
                    )
                )
            }

            // Engagement Metrics
            item {
                SectionHeader(
                    title = "Engagement",
                    modifier = Modifier.padding(top = GlowSpacing.md)
                )
            }

            item {
                MetricCard(
                    title = "User Behavior",
                    metrics = listOf(
                        MetricRow("Avg. Session Duration", "4m 32s", glow.sage),
                        MetricRow("Captures per User", "2.4 / week", glow.honey700),
                        MetricRow("Avg. Streak", "8.3 days", Color(0xFF4CAF50)),
                        MetricRow("Feature Discovery", "72%", Color(0xFF2196F3))
                    )
                )
            }

            // Infrastructure
            item {
                SectionHeader(
                    title = "Infrastructure",
                    modifier = Modifier.padding(top = GlowSpacing.md)
                )
            }

            item {
                MetricCard(
                    title = "Backend Status",
                    metrics = listOf(
                        MetricRow("Server Uptime", "99.9%", Color(0xFF4CAF50)),
                        MetricRow("Database Size", "42.3 GB", Color(0xFF2196F3)),
                        MetricRow("Storage Used", "1.2 TB", Color(0xFFFFC107)),
                        MetricRow("CDN Hits", "98.7%", Color(0xFF4CAF50))
                    )
                )
            }

            // Bottom spacing
            item {
                Spacer(modifier = Modifier.height(GlowSpacing.lg))
            }
        }
    }
}

@Composable
private fun MetricCard(
    title: String,
    metrics: List<MetricRow>,
    modifier: Modifier = Modifier,
) {
    val glow = LocalGlowColors.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(glow.surfaceCard, RoundedCornerShape(12.dp))
            .padding(GlowSpacing.md),
        verticalArrangement = Arrangement.spacedBy(GlowSpacing.sm)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = glow.ink900
        )

        metrics.forEach { metric ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        glow.surface,
                        RoundedCornerShape(8.dp)
                    )
                    .padding(GlowSpacing.sm),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = metric.label,
                    style = MaterialTheme.typography.bodyMedium,
                    color = glow.ink900,
                    modifier = Modifier.weight(1f)
                )

                Text(
                    text = metric.value,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = metric.color
                )
            }
        }
    }
}

@Composable
private fun HealthStatTile(
    label: String,
    value: String,
    icon: ImageVector,
    status: HealthStatus,
    modifier: Modifier = Modifier,
) {
    val glow = LocalGlowColors.current

    val backgroundColor = when (status) {
        HealthStatus.GOOD -> Color(0xFF4CAF50).copy(alpha = 0.15f)
        HealthStatus.WARNING -> Color(0xFFFFC107).copy(alpha = 0.15f)
        HealthStatus.CRITICAL -> Color(0xFFF44336).copy(alpha = 0.15f)
    }

    val iconColor = when (status) {
        HealthStatus.GOOD -> Color(0xFF4CAF50)
        HealthStatus.WARNING -> Color(0xFFFFC107)
        HealthStatus.CRITICAL -> Color(0xFFF44336)
    }

    Column(
        modifier = modifier
            .background(backgroundColor, RoundedCornerShape(12.dp))
            .padding(GlowSpacing.md)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconColor,
            modifier = Modifier.size(32.dp)
        )

        Spacer(modifier = Modifier.height(GlowSpacing.sm))

        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = glow.ink900
        )

        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = glow.ink600,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

private data class MetricRow(
    val label: String,
    val value: String,
    val color: Color,
)

private enum class HealthStatus {
    GOOD, WARNING, CRITICAL
}
