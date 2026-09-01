package com.glowup.ai.feature.home.components

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
import androidx.compose.ui.unit.dp
import com.glowup.ai.core.design.LocalGlowColors
import com.glowup.ai.core.ui.EmptyState
import com.glowup.ai.core.ui.GlowCard
import com.glowup.ai.core.ui.LockedCard
import com.glowup.ai.core.ui.SectionHeader
import com.glowup.ai.core.ui.VerdictChip
import com.glowup.ai.domain.model.DashboardFeatures
import com.glowup.ai.domain.model.Verdict
import com.glowup.ai.domain.model.VerdictLabel

/**
 * Renders `dashboard.verdicts`. Trap #5, the subtle one: a free plan's empty `verdicts[]` is BY
 * DESIGN, not "no data" — this branches on [features]/[isPremium] FIRST, before ever treating an
 * empty array as a true empty state. A `label == "locked"` entry inside a non-empty array (the
 * one-free-lifetime-unlock upsell shape — frontend-api-map.md lines ~50-57) is always rendered via
 * [LockedCard], never mixed into the normal verdict list or collapsed into a boolean.
 */
@Composable
fun VerdictsSection(
    modifier: Modifier = Modifier,
    verdicts: List<Verdict>,
    features: DashboardFeatures,
    isPremium: Boolean,
    onLogRoutine: () -> Unit,
    onUnlockPremium: () -> Unit,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        SectionHeader(title = "Product verdicts")

        when {
            // Branch on entitlement/features FIRST (trap #5) — an empty array under free, before
            // the one-time unlock has ever fired, is the free gate, not "no evidence yet".
            verdicts.isEmpty() && !isPremium && !features.productVerdictsUnlocked -> {
                LockedCard(
                    modifier = Modifier.padding(top = 12.dp),
                    title = "Unlock product verdicts",
                    body = "Log a routine and keep capturing — your first verdict unlocks free once there's enough evidence. Go Premium for unlimited verdicts on every product.",
                    ctaLabel = "See Premium",
                    onUnlock = onUnlockPremium,
                )
            }

            // A genuine no-evidence-yet state: Premium (or already past the free unlock) with
            // nothing to show — a real empty state that names the next action.
            verdicts.isEmpty() -> {
                EmptyState(
                    modifier = Modifier.padding(top = 12.dp),
                    title = "No verdicts yet",
                    body = "Verdicts appear once a product has enough before/after evidence. Log what you're using to start building it.",
                    ctaLabel = "Log your routine",
                    onCtaClick = onLogRoutine,
                )
            }

            else -> {
                Column(
                    modifier = Modifier.padding(top = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    verdicts.forEach { verdict ->
                        if (verdict.label == VerdictLabel.LOCKED) {
                            LockedCard(
                                title = verdict.productName ?: "Locked verdict",
                                body =
                                    verdict.generatedText.ifBlank {
                                        "Upgrade to Premium to see this verdict and get unlimited product verdicts."
                                    },
                                onUnlock = onUnlockPremium,
                            )
                        } else {
                            VerdictCard(verdict = verdict)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun VerdictCard(verdict: Verdict) {
    val glow = LocalGlowColors.current
    GlowCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = verdict.productName ?: "Product",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = glow.ink900,
                modifier = Modifier.weight(1f).padding(end = 8.dp),
            )
            VerdictChip(label = verdict.label.toWireOrRaw())
        }
        Text(
            text = verdict.generatedText,
            style = MaterialTheme.typography.bodyMedium,
            color = glow.ink600,
            modifier = Modifier.padding(top = 8.dp),
        )
        val evidence = verdict.evidence
        if (evidence != null && (evidence.nAfter != null || evidence.confidence != null)) {
            Text(
                text =
                    buildString {
                        evidence.nAfter?.let { append("$it captures after") }
                        evidence.confidence?.let {
                            if (isNotEmpty()) append(" · ")
                            append("confidence ${String.format("%.0f", it * 100)}%")
                        }
                    },
                style = MaterialTheme.typography.labelSmall,
                color = glow.ink600,
                modifier = Modifier.padding(top = 6.dp),
            )
        }
    }
}

/** [VerdictChip] takes the backend's raw wire string, not the enum — map back for the handful of
 * known values, falling back to a lowercase snake copy for [VerdictLabel.UNKNOWN]. */
private fun VerdictLabel.toWireOrRaw(): String =
    when (this) {
        VerdictLabel.KEEP -> "keep"
        VerdictLabel.LIKELY_USEFUL -> "likely_useful"
        VerdictLabel.EVIDENCE_UNCLEAR -> "evidence_unclear"
        VerdictLabel.INVESTIGATE -> "investigate"
        VerdictLabel.LOCKED -> "locked"
        VerdictLabel.UNKNOWN -> "evidence_unclear"
    }
