package com.glowup.ai.feature.home.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.glowup.ai.core.design.LocalGlowColors
import com.glowup.ai.core.ui.EmptyState
import com.glowup.ai.core.ui.GlowCard
import com.glowup.ai.core.ui.SectionHeader
import com.glowup.ai.domain.model.DashboardRoutineEvent
import com.glowup.ai.domain.model.RoutineAction

/**
 * A compact recent-activity feed from `dashboard.routine_events` — full routine management lives
 * in `feature/routine` (task 3.4). `RoutineAction` is display-only here; this never writes a
 * `POST /api/routine-events` call, so `UNKNOWN`'s no-wire-string restriction never applies.
 */
@Composable
fun RoutineTimelineSection(
    modifier: Modifier = Modifier,
    events: List<DashboardRoutineEvent>,
    onLogRoutine: () -> Unit,
) {
    val glow = LocalGlowColors.current
    Column(modifier = modifier.fillMaxWidth()) {
        SectionHeader(title = "Recent routine activity")
        if (events.isEmpty()) {
            EmptyState(
                modifier = Modifier.padding(top = 12.dp),
                title = "Nothing logged yet",
                body = "Log when you start, stop, or change a product so verdicts have something to explain.",
                ctaLabel = "Log your routine",
                onCtaClick = onLogRoutine,
            )
        } else {
            GlowCard(modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
                events.take(5).forEachIndexed { index, event ->
                    if (index > 0) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = glow.ink600.copy(alpha = 0.12f))
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column {
                            Text(
                                text = event.productName ?: "Product",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = glow.ink900,
                            )
                            Text(
                                text = event.action.displayLabel() + (event.slot?.let { " · $it" } ?: ""),
                                style = MaterialTheme.typography.labelSmall,
                                color = glow.ink600,
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun RoutineAction.displayLabel(): String = when (this) {
    RoutineAction.START -> "Started"
    RoutineAction.STOP -> "Stopped"
    RoutineAction.CHANGE -> "Changed"
    RoutineAction.UNKNOWN -> "Logged"
}
