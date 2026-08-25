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
import com.glowup.ai.domain.model.Experiment
import com.glowup.ai.domain.model.ExperimentStatus

/**
 * A compact Home summary of `dashboard.experiments` — full CRUD lives in `feature/routine`
 * (task 3.4). Free plans never receive a populated `experiments[]` at all (it is a Premium-only
 * dashboard section per frontend-api-map.md), so — same trap #5 shape as verdicts — this branches
 * on [isPremium] before ever treating an empty array as "no experiments running".
 */
@Composable
fun ExperimentsSection(
    modifier: Modifier = Modifier,
    experiments: List<Experiment>,
    isPremium: Boolean,
    onStartExperiment: () -> Unit,
    onUnlockPremium: () -> Unit,
    onOpenExperiment: (String) -> Unit,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        SectionHeader(title = "Experiments")
        when {
            !isPremium -> LockedCard(
                modifier = Modifier.padding(top = 12.dp),
                title = "Run controlled experiments",
                body = "Premium experiments isolate one product change at a time so verdicts aren't confounded by everything else in your routine.",
                ctaLabel = "See Premium",
                onUnlock = onUnlockPremium,
            )
            experiments.isEmpty() -> EmptyState(
                modifier = Modifier.padding(top = 12.dp),
                title = "No experiments running",
                body = "Start one to test whether a single product is really doing anything.",
                ctaLabel = "Start an experiment",
                onCtaClick = onStartExperiment,
            )
            else -> Column(
                modifier = Modifier.padding(top = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                experiments.take(3).forEach { experiment ->
                    ExperimentRow(experiment = experiment, onClick = { onOpenExperiment(experiment.id) })
                }
            }
        }
    }
}

@Composable
private fun ExperimentRow(experiment: Experiment, onClick: () -> Unit) {
    val glow = LocalGlowColors.current
    GlowCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        contentDescription = "Open experiment ${experiment.name}",
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = experiment.name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = glow.ink900,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = experiment.status.displayLabel(),
                style = MaterialTheme.typography.labelMedium,
                color = glow.ink600,
            )
        }
        if (experiment.earlyStop?.conclusive == true) {
            Text(
                text = experiment.earlyStop.message,
                style = MaterialTheme.typography.bodySmall,
                color = glow.honey700,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

/** `running`, never `active` — ANDROID_PLAN.md trap #11 / bug #1. Display copy only; never used
 * to filter. */
private fun ExperimentStatus.displayLabel(): String = when (this) {
    ExperimentStatus.PLANNED -> "Planned"
    ExperimentStatus.RUNNING -> "Running"
    ExperimentStatus.PAUSED -> "Paused"
    ExperimentStatus.COMPLETED -> "Completed"
    ExperimentStatus.CANCELLED -> "Cancelled"
    ExperimentStatus.UNKNOWN -> "Unknown"
}
