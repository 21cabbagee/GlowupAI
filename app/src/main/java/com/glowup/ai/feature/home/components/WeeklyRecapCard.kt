package com.glowup.ai.feature.home.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.glowup.ai.core.design.LocalGlowColors
import com.glowup.ai.core.ui.DisclaimerNote
import com.glowup.ai.core.ui.GlowCard
import com.glowup.ai.core.ui.SectionHeader
import com.glowup.ai.domain.model.WeeklyRecap

/**
 * The dashboard's embedded `weekly_recap` (task 3.3 deliverable #7). Rendered straight from
 * `dashboard.weeklyRecap` — never a second `GET /weekly-recap` round trip for the Home screen,
 * since the dashboard snapshot already carries it (verified in `complete_api.py` /
 * `backend/web/lib/api.ts`'s `Dashboard` interface).
 */
@Composable
fun WeeklyRecapCard(
    modifier: Modifier = Modifier,
    recap: WeeklyRecap?,
) {
    if (recap == null) return
    val glow = LocalGlowColors.current

    Column(modifier = modifier.fillMaxWidth()) {
        SectionHeader(title = "This week")
        GlowCard(modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
            Text(
                text = recap.headline,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = glow.ink900,
            )
            Text(
                text = recap.body,
                style = MaterialTheme.typography.bodyMedium,
                color = glow.ink600,
                modifier = Modifier.padding(top = 6.dp),
            )
            if (!recap.nextAction.isNullOrBlank()) {
                Text(
                    text = "Next: ${recap.nextAction}",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = glow.honey700,
                    modifier = Modifier.padding(top = 10.dp),
                )
            }
            Text(
                text = "${recap.captureCount} captures · ${recap.checkInCount} check-ins this period · ${recap.confidenceLabel}",
                style = MaterialTheme.typography.labelSmall,
                color = glow.ink600,
                modifier = Modifier.padding(top = 8.dp),
            )
            DisclaimerNote(
                modifier = Modifier.padding(top = 10.dp),
                text = recap.disclaimer,
            )
        }
    }
}
