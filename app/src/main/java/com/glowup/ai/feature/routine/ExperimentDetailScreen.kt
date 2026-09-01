package com.glowup.ai.feature.routine

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.glowup.ai.core.design.LocalGlowColors
import com.glowup.ai.core.ui.ErrorState
import com.glowup.ai.core.ui.GlowButton
import com.glowup.ai.core.ui.GlowButtonVariant
import com.glowup.ai.core.ui.GlowCard
import com.glowup.ai.core.ui.GlowTopBar
import com.glowup.ai.core.ui.SectionHeader
import com.glowup.ai.core.ui.ShimmerSkeleton
import com.glowup.ai.domain.model.ExperimentStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExperimentDetailRoute(
    onBack: () -> Unit,
    viewModel: ExperimentDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(topBar = { GlowTopBar(title = "Experiment", onBack = onBack) }) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (val s = state) {
                is ExperimentDetailUiState.Loading -> {
                    ShimmerSkeleton(modifier = Modifier.padding(16.dp), height = 200.dp)
                }

                is ExperimentDetailUiState.Error -> {
                    ErrorState(modifier = Modifier.padding(16.dp), message = s.message, onRetry = viewModel::load)
                }

                is ExperimentDetailUiState.Content -> {
                    ExperimentDetailContent(
                        state = s,
                        onRequestStatusChange = viewModel::requestStatusChange,
                        onCancelStatusChange = viewModel::cancelStatusChange,
                        onConfirmStatusChange = viewModel::confirmStatusChange,
                        onApplyEarlyStop = viewModel::applyEarlyStopRecommendation,
                    )
                }
            }
        }
    }
}

@Composable
private fun ExperimentDetailContent(
    state: ExperimentDetailUiState.Content,
    onRequestStatusChange: (ExperimentStatus) -> Unit,
    onCancelStatusChange: () -> Unit,
    onConfirmStatusChange: () -> Unit,
    onApplyEarlyStop: () -> Unit,
) {
    val glow = LocalGlowColors.current
    val experiment = state.experiment

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Text(experiment.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = glow.ink900)
        experiment.hypothesis?.let {
            Text(it, style = MaterialTheme.typography.bodyMedium, color = glow.ink600, modifier = Modifier.padding(top = 6.dp))
        }
        Text(
            "Primary metric: ${experiment.primaryMetric.name.lowercase().replace('_', ' ')} • Target: ${experiment.targetDays} days",
            style = MaterialTheme.typography.bodySmall,
            color = glow.ink600,
            modifier = Modifier.padding(top = 8.dp),
        )

        val earlyStop = experiment.earlyStop
        if (earlyStop != null && earlyStop.conclusive) {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                        .background(glow.warning.copy(alpha = 0.18f), RoundedCornerShape(18.dp))
                        .padding(20.dp),
            ) {
                Text(
                    "Early result available",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = glow.ink900,
                )
                Text(
                    earlyStop.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = glow.ink900,
                    modifier = Modifier.padding(top = 6.dp, bottom = 12.dp),
                )
                if (earlyStop.recommendedStatus != null && earlyStop.recommendedStatus != ExperimentStatus.UNKNOWN) {
                    GlowButton(
                        text = "Mark as ${earlyStop.recommendedStatus.name.lowercase()} now",
                        loading = state.statusChangePending,
                        enabled = !state.statusChangePending,
                        onClick = onApplyEarlyStop,
                    )
                }
            }
        }

        SectionHeader(title = "Status", modifier = Modifier.padding(top = 24.dp, bottom = 8.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ExperimentStatus.entries.filter { it != ExperimentStatus.UNKNOWN }.forEach { candidate ->
                GlowButton(
                    text = candidate.name.lowercase().replaceFirstChar(Char::uppercase),
                    variant = if (candidate == experiment.status) GlowButtonVariant.Primary else GlowButtonVariant.Secondary,
                    enabled = !state.statusChangePending && candidate != experiment.status,
                    onClick = { onRequestStatusChange(candidate) },
                )
            }
        }
        if (state.statusChangeError != null) {
            Text(
                state.statusChangeError,
                color = glow.danger,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 8.dp),
            )
        }

        if (experiment.products.isNotEmpty()) {
            SectionHeader(title = "Products", modifier = Modifier.padding(top = 24.dp, bottom = 8.dp))
            experiment.products.forEach { product ->
                GlowCard(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                    Text(product.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = glow.ink900)
                    Text(
                        listOfNotNull(product.role, product.category).joinToString(" • "),
                        style = MaterialTheme.typography.bodySmall,
                        color = glow.ink600,
                    )
                }
            }
        }

        SectionHeader(title = "Events", modifier = Modifier.padding(top = 24.dp, bottom = 8.dp))
        if (experiment.events.isEmpty()) {
            Text("No routine events logged against this experiment yet.", style = MaterialTheme.typography.bodySmall, color = glow.ink600)
        } else {
            experiment.events.forEach { event ->
                GlowCard(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                    Text(
                        "${event.action.name.lowercase().replaceFirstChar(Char::uppercase)} • ${event.productName ?: "Unknown product"}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = glow.ink900,
                    )
                    event.timestamp?.let {
                        Text(it, style = MaterialTheme.typography.bodySmall, color = glow.ink600, modifier = Modifier.padding(top = 2.dp))
                    }
                }
            }
        }

        Text(
            "${experiment.captures.size} capture(s) counted toward this experiment so far. It cannot conclude anything until enough have come in.",
            style = MaterialTheme.typography.bodySmall,
            color = glow.ink600,
            modifier = Modifier.padding(top = 24.dp),
        )
    }

    if (state.confirmTarget != null) {
        AlertDialog(
            onDismissRequest = onCancelStatusChange,
            title = { Text("Change status to ${state.confirmTarget.name.lowercase()}?") },
            text = { Text("This updates the experiment immediately. You can change it again later.") },
            confirmButton = {
                TextButton(onClick = onConfirmStatusChange, enabled = !state.statusChangePending) { Text("Confirm") }
            },
            dismissButton = {
                TextButton(onClick = onCancelStatusChange) { Text("Cancel") }
            },
        )
    }
}
