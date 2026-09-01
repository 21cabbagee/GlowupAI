package com.glowup.ai.feature.routine

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.glowup.ai.core.design.LocalGlowColors
import com.glowup.ai.core.ui.EmptyState
import com.glowup.ai.core.ui.ErrorState
import com.glowup.ai.core.ui.GlowBottomSheet
import com.glowup.ai.core.ui.GlowButton
import com.glowup.ai.core.ui.GlowButtonVariant
import com.glowup.ai.core.ui.GlowCard
import com.glowup.ai.core.ui.GlowTextField
import com.glowup.ai.core.ui.GlowTopBar
import com.glowup.ai.core.ui.LockedCard
import com.glowup.ai.core.ui.ShimmerSkeleton
import com.glowup.ai.domain.model.Experiment
import com.glowup.ai.domain.model.ExperimentStatus
import com.glowup.ai.domain.model.PrimaryMetric
import com.glowup.ai.domain.model.Product

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExperimentsRoute(
    onBack: () -> Unit,
    onOpenExperiment: (String) -> Unit,
    onNavigateToPaywall: () -> Unit,
    viewModel: ExperimentsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(state.navigateToExperimentId) {
        state.navigateToExperimentId?.let {
            onOpenExperiment(it)
            viewModel.consumeNavigation()
        }
    }

    Scaffold(topBar = { GlowTopBar(title = "Experiments", onBack = onBack) }) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            when (val status = state.listStatus) {
                is ExperimentsListStatus.Loading -> {
                    ShimmerSkeleton(height = 200.dp)
                }

                is ExperimentsListStatus.Error -> {
                    ErrorState(message = status.message, onRetry = viewModel::loadExperiments)
                }

                is ExperimentsListStatus.Locked -> {
                    LockedCard(
                        title = "Measure one change at a time",
                        body = "Experiments track a single product against your own before/after captures — a Premium feature.",
                        onUnlock = onNavigateToPaywall,
                    )
                }

                is ExperimentsListStatus.Content -> {
                    if (status.experiments.isEmpty()) {
                        EmptyState(
                            title = "No experiments yet",
                            body = "Pick a product you've already started and measure whether it's actually working.",
                            ctaLabel = "Start an experiment",
                            onCtaClick = viewModel::openCreateSheet,
                        )
                    } else {
                        Row(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp), horizontalArrangement = Arrangement.End) {
                            GlowButton(text = "New experiment", variant = GlowButtonVariant.Secondary, onClick = viewModel::openCreateSheet)
                        }
                        LazyColumn {
                            items(status.experiments, key = { it.id }) { experiment ->
                                ExperimentRow(
                                    experiment = experiment,
                                    onClick = { onOpenExperiment(experiment.id) },
                                    modifier = Modifier.padding(bottom = 12.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (state.create.visible) {
        GlowBottomSheet(onDismissRequest = viewModel::dismissCreateSheet) {
            CreateExperimentSheet(
                state = state.create,
                onNameChange = viewModel::onNameChange,
                onHypothesisChange = viewModel::onHypothesisChange,
                onPrimaryMetricChange = viewModel::onPrimaryMetricChange,
                onTargetDaysChange = viewModel::onTargetDaysChange,
                onProductQueryChange = viewModel::onProductQueryChange,
                onSelectProduct = viewModel::onSelectProduct,
                onSubmit = viewModel::submitCreate,
            )
        }
    }
}

@Composable
private fun ExperimentRow(
    experiment: Experiment,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val glow = LocalGlowColors.current
    // Each (surface, onSurface, label) triple is self-contained so its contrast never depends on
    // the surrounding card background — same discipline as VerdictChip.
    val (statusColor, onStatusColor, statusText) =
        when (experiment.status) {
            ExperimentStatus.RUNNING -> Triple(glow.honey500, glow.ink900, "Running")
            ExperimentStatus.PLANNED -> Triple(glow.ink600, glow.paper, "Planned")
            ExperimentStatus.PAUSED -> Triple(glow.warning, glow.onWarning, "Paused")
            ExperimentStatus.COMPLETED -> Triple(glow.success, Color.White, "Completed")
            ExperimentStatus.CANCELLED -> Triple(glow.danger, glow.paper, "Cancelled")
            ExperimentStatus.UNKNOWN -> Triple(glow.ink600, glow.paper, "Unknown")
        }
    GlowCard(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick,
        contentDescription = "Open experiment ${experiment.name}, $statusText",
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column(modifier = Modifier.weight(1f)) {
                Text(experiment.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = glow.ink900)
                Text(
                    experiment.products.firstOrNull()?.name ?: "No product",
                    style = MaterialTheme.typography.bodySmall,
                    color = glow.ink600,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
            Text(
                statusText,
                color = onStatusColor,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                modifier =
                    Modifier
                        .background(statusColor, RoundedCornerShape(50))
                        .padding(horizontal = 10.dp, vertical = 4.dp),
            )
        }
    }
}

@Composable
private fun CreateExperimentSheet(
    state: CreateExperimentState,
    onNameChange: (String) -> Unit,
    onHypothesisChange: (String) -> Unit,
    onPrimaryMetricChange: (PrimaryMetric) -> Unit,
    onTargetDaysChange: (String) -> Unit,
    onProductQueryChange: (String) -> Unit,
    onSelectProduct: (Product) -> Unit,
    onSubmit: () -> Unit,
) {
    val glow = LocalGlowColors.current
    Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
        Text("New experiment", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = glow.ink900)
        Text(
            "Starts as \"running\" — it needs captures over time before it can conclude anything.",
            style = MaterialTheme.typography.bodySmall,
            color = glow.ink600,
            modifier = Modifier.padding(top = 4.dp, bottom = 16.dp),
        )
        GlowTextField(
            value = state.productQuery,
            onValueChange = onProductQueryChange,
            label = "Product",
            supportingText = state.selectedProduct?.let { "Selected: ${it.name}" } ?: "Search for the product you're testing.",
            modifier = Modifier.padding(bottom = 8.dp),
        )
        if (state.productResults.isNotEmpty() && state.selectedProduct == null) {
            Column(modifier = Modifier.padding(bottom = 12.dp)) {
                state.productResults.take(5).forEach { product ->
                    GlowCard(modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp), onClick = { onSelectProduct(product) }) {
                        Text(product.name, color = glow.ink900, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
        GlowTextField(
            value = state.name,
            onValueChange = onNameChange,
            label = "Experiment name",
            modifier = Modifier.padding(bottom = 12.dp),
        )
        GlowTextField(
            value = state.hypothesis,
            onValueChange = onHypothesisChange,
            label = "Hypothesis (optional)",
            singleLine = false,
            modifier = Modifier.padding(bottom = 12.dp),
        )
        Text("Primary metric", style = MaterialTheme.typography.labelLarge, color = glow.ink900, modifier = Modifier.padding(bottom = 8.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 12.dp)) {
            listOf(
                PrimaryMetric.REDNESS_SCORE to "Redness",
                PrimaryMetric.BLEMISH_COUNT to "Blemishes",
                PrimaryMetric.DARKSPOT_AREA to "Dark spots",
                PrimaryMetric.TEXTURE_SCORE to "Texture",
            ).forEach { (metric, label) ->
                GlowButton(
                    text = label,
                    variant = if (state.primaryMetric == metric) GlowButtonVariant.Primary else GlowButtonVariant.Secondary,
                    onClick = { onPrimaryMetricChange(metric) },
                )
            }
        }
        GlowTextField(
            value = state.targetDays,
            onValueChange = onTargetDaysChange,
            label = "Target days",
            supportingText = "1-180.",
            modifier = Modifier.padding(bottom = 12.dp),
        )
        if (state.error != null) {
            Text(state.error, color = glow.danger, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(bottom = 8.dp))
        }
        GlowButton(
            text = "Start experiment",
            loading = state.pending,
            enabled = !state.pending && state.selectedProduct != null && state.name.isNotBlank(),
            onClick = onSubmit,
        )
    }
}
