package com.glowup.ai.feature.account.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.glowup.ai.core.design.GlowSpacing
import com.glowup.ai.core.design.LocalGlowColors
import com.glowup.ai.core.ui.DisclaimerNote
import com.glowup.ai.core.ui.GlowCard
import com.glowup.ai.core.ui.ShimmerSkeleton
import com.glowup.ai.domain.model.Analytics
import java.util.Locale

/**
 * `GET /analytics`. frontend-api-map.md: "Do not display the rates as clinical confidence; they
 * are engagement-derived product analytics." The [DisclaimerNote] here is that label, not the
 * cosmetic-tracking disclaimer — this panel never claims anything about skin outcomes at all,
 * only about how the person is using the app.
 */
@Composable
fun AnalyticsPanel(
    modifier: Modifier = Modifier,
    analytics: Analytics?,
    errorMessage: String?,
) {
    val topGap = Modifier.padding(top = GlowSpacing.sm)
    GlowCard(modifier = modifier) {
        Text(
            text = "Your app activity",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = LocalGlowColors.current.ink900,
        )
        DisclaimerNote(
            modifier = Modifier.fillMaxWidth().then(topGap),
            text = "These are engagement-derived product metrics about how you use GlowUp AI — " +
                "NOT a measure of clinical confidence in any skin result.",
        )
        when {
            analytics != null -> AnalyticsRows(analytics, topGap)
            errorMessage != null -> Text(
                text = "Couldn't load activity metrics right now.",
                style = MaterialTheme.typography.bodySmall,
                color = LocalGlowColors.current.ink600,
                modifier = topGap,
            )
            else -> Column(modifier = topGap, verticalArrangement = Arrangement.spacedBy(GlowSpacing.xs)) {
                ShimmerSkeleton()
                ShimmerSkeleton()
                ShimmerSkeleton()
            }
        }
    }
}

@Composable
private fun AnalyticsRows(analytics: Analytics, topGap: Modifier) {
    val glow = LocalGlowColors.current
    Column(modifier = topGap, verticalArrangement = Arrangement.spacedBy(GlowSpacing.xs)) {
        AnalyticsRow("Activation stage", analytics.activation ?: "Not started")
        AnalyticsRow("Baseline capture done", boolLabel(analytics.baselineCapture))
        AnalyticsRow("First three captures done", boolLabel(analytics.firstThreeCaptures))
        analytics.medianHistoryDays?.let { AnalyticsRow("Median days between captures", oneDecimal(it)) }
        analytics.weeklyVerdictOpenRate?.let { AnalyticsRow("Weekly verdict open rate", percent(it)) }
        analytics.verdictActionRate?.let { AnalyticsRow("Verdict action rate", percent(it)) }
        analytics.evidenceUnclearEngagementRate?.let {
            AnalyticsRow("Evidence-unclear engagement rate", percent(it))
        }
        if (analytics.activation == null && analytics.baselineCapture == null && analytics.medianHistoryDays == null) {
            Text(
                text = "Not enough activity yet to show these metrics.",
                style = MaterialTheme.typography.bodySmall,
                color = glow.ink600,
            )
        }
    }
}

@Composable
private fun AnalyticsRow(label: String, value: String) {
    val glow = LocalGlowColors.current
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = glow.ink600)
        Text(text = value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, color = glow.ink900)
    }
}

private fun boolLabel(value: Boolean?): String = when (value) {
    true -> "Yes"
    false -> "Not yet"
    null -> "Unknown"
}

private fun percent(fraction: Double): String = String.format(Locale.US, "%.0f%%", fraction * 100)
private fun oneDecimal(value: Double): String = String.format(Locale.US, "%.1f", value)
