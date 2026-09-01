package com.glowup.ai.feature.routine

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.glowup.ai.core.design.LocalGlowColors
import com.glowup.ai.core.ui.ErrorState
import com.glowup.ai.core.ui.GlowBottomSheet
import com.glowup.ai.core.ui.GlowButton
import com.glowup.ai.core.ui.GlowButtonVariant
import com.glowup.ai.core.ui.GlowCard
import com.glowup.ai.core.ui.GlowTopBar
import com.glowup.ai.core.ui.LockedCard
import com.glowup.ai.core.ui.SectionHeader
import com.glowup.ai.core.ui.ShimmerSkeleton
import com.glowup.ai.domain.model.IngredientExplainer
import com.glowup.ai.domain.model.RoutineAction
import com.glowup.ai.feature.routine.components.ConfoundWarningBanner
import com.glowup.ai.feature.routine.components.RoutineEventSheet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductDetailRoute(
    onBack: () -> Unit,
    onNavigateToPaywall: () -> Unit,
    viewModel: ProductDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(topBar = { GlowTopBar(title = "Product", onBack = onBack) }) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (val s = state) {
                is ProductDetailUiState.Loading -> {
                    ShimmerSkeleton(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        height = 200.dp,
                    )
                }

                is ProductDetailUiState.Error -> {
                    ErrorState(
                        modifier = Modifier.padding(16.dp),
                        message = s.message,
                        onRetry = viewModel::load,
                    )
                }

                is ProductDetailUiState.Content -> {
                    ProductDetailContent(
                        state = s,
                        onStart = { viewModel.openEventSheet(RoutineAction.START) },
                        onStop = { viewModel.openEventSheet(RoutineAction.STOP) },
                        onChange = { viewModel.openEventSheet(RoutineAction.CHANGE) },
                        onDismissSheet = viewModel::dismissEventSheet,
                        onEventActionChanged = viewModel::onEventActionChanged,
                        onDismissConfound = viewModel::dismissConfoundBanner,
                        onDismissPostSubmitWarning = viewModel::dismissPostSubmitWarning,
                        onSubmitEvent = viewModel::submitEvent,
                        onLoadExplainer = viewModel::loadIngredientExplainer,
                        onUnlockExplainer = onNavigateToPaywall,
                        onConsumeSuccess = viewModel::consumeSuccessMessage,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProductDetailContent(
    state: ProductDetailUiState.Content,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onChange: () -> Unit,
    onDismissSheet: () -> Unit,
    onEventActionChanged: (RoutineAction) -> Unit,
    onDismissConfound: () -> Unit,
    onDismissPostSubmitWarning: () -> Unit,
    onSubmitEvent: (RoutineAction, String, String?, String?, String?) -> Unit,
    onLoadExplainer: () -> Unit,
    onUnlockExplainer: () -> Unit,
    onConsumeSuccess: () -> Unit,
) {
    val glow = LocalGlowColors.current
    val product = state.detail.product

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Text(product.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = glow.ink900)
        Text(
            product.category.replaceFirstChar { it.uppercase() },
            style = MaterialTheme.typography.bodyMedium,
            color = glow.ink600,
            modifier = Modifier.padding(top = 4.dp, bottom = 4.dp),
        )
        Text(
            "Stabilizes in about ${product.stabilizationDays} days",
            style = MaterialTheme.typography.bodySmall,
            color = glow.ink600,
        )

        if (state.postSubmitWarning != null) {
            ConfoundWarningBanner(
                modifier = Modifier.padding(top = 16.dp),
                confound = state.postSubmitWarning,
                onDismiss = onDismissPostSubmitWarning,
            )
        }
        if (state.successMessage != null) {
            GlowCard(modifier = Modifier.padding(top = 16.dp), onClick = onConsumeSuccess, contentDescription = state.successMessage) {
                Text(state.successMessage, style = MaterialTheme.typography.bodyMedium, color = glow.ink900)
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            GlowButton(modifier = Modifier.weight(1f), text = "Start", onClick = onStart)
            GlowButton(modifier = Modifier.weight(1f), text = "Change", variant = GlowButtonVariant.Secondary, onClick = onChange)
            GlowButton(modifier = Modifier.weight(1f), text = "Stop", variant = GlowButtonVariant.Danger, onClick = onStop)
        }

        if (product.ingredients.isNotEmpty()) {
            SectionHeader(title = "Ingredients", modifier = Modifier.padding(top = 24.dp, bottom = 8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                product.ingredients.forEach { ingredient ->
                    Text(
                        ingredient,
                        style = MaterialTheme.typography.bodySmall,
                        color = glow.ink900,
                        modifier =
                            Modifier
                                .padding(bottom = 8.dp)
                                .background(glow.surfaceCard, RoundedCornerShape(50))
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                    )
                }
            }
        }

        SectionHeader(title = "Ingredient explainer", modifier = Modifier.padding(top = 24.dp, bottom = 8.dp))
        IngredientExplainerSection(
            loading = state.explainerLoading,
            locked = state.explainerLocked,
            explainer = state.explainer,
            error = state.explainerError,
            onLoad = onLoadExplainer,
            onUnlock = onUnlockExplainer,
        )
    }

    if (state.showEventSheet) {
        GlowBottomSheet(onDismissRequest = onDismissSheet) {
            RoutineEventSheet(
                productName = product.name,
                initialAction = state.eventInitialAction,
                confoundCheck = state.confoundCheck,
                confoundCheckLoading = state.confoundCheckLoading,
                onDismissConfound = onDismissConfound,
                onActionChange = onEventActionChanged,
                submitting = state.eventSubmitting,
                errorText = state.eventError,
                onSubmit = onSubmitEvent,
            )
        }
    }
}

@Composable
private fun IngredientExplainerSection(
    loading: Boolean,
    locked: Boolean,
    explainer: IngredientExplainer?,
    error: String?,
    onLoad: () -> Unit,
    onUnlock: () -> Unit,
) {
    val glow = LocalGlowColors.current
    LaunchedEffect(Unit) { onLoad() }

    when {
        loading -> {
            ShimmerSkeleton(height = 100.dp)
        }

        locked -> {
            LockedCard(
                title = "See why each ingredient earned its verdict",
                body = "Ingredient explainers with reviewed purpose and caution notes are a Premium feature.",
                onUnlock = onUnlock,
            )
        }

        error != null -> {
            ErrorState(message = error, onRetry = onLoad)
        }

        explainer != null -> {
            Column {
                explainer.reviewed.forEach { reviewed ->
                    GlowCard(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                        Text(
                            reviewed.ingredient,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = glow.ink900,
                        )
                        reviewed.purpose?.let {
                            Text(
                                it,
                                style = MaterialTheme.typography.bodySmall,
                                color = glow.ink600,
                                modifier = Modifier.padding(top = 4.dp),
                            )
                        }
                        reviewed.caution?.let {
                            Text(
                                it,
                                style = MaterialTheme.typography.bodySmall,
                                color = glow.danger,
                                modifier = Modifier.padding(top = 4.dp),
                            )
                        }
                    }
                }
                if (explainer.unknown.isNotEmpty()) {
                    Text(
                        "Not yet reviewed (neither safe nor unsafe — just no catalog entry): ${explainer.unknown.joinToString(", ")}",
                        style = MaterialTheme.typography.bodySmall,
                        color = glow.ink600,
                    )
                }
            }
        }

        else -> {
            ShimmerSkeleton(height = 100.dp)
        }
    }
}
