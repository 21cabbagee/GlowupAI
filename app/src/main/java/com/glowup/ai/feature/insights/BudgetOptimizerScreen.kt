package com.glowup.ai.feature.insights

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.glowup.ai.core.design.GlowSpacing
import com.glowup.ai.core.design.LocalGlowColors
import com.glowup.ai.core.ui.DisclaimerNote
import com.glowup.ai.core.ui.EmptyState
import com.glowup.ai.core.ui.ErrorState
import com.glowup.ai.core.ui.GlowCard
import com.glowup.ai.core.ui.GlowTopBar
import com.glowup.ai.core.ui.LockedCard
import com.glowup.ai.core.ui.ShimmerSkeleton
import com.glowup.ai.domain.model.BudgetFlaggedProduct
import com.glowup.ai.domain.model.BudgetOptimizer
import java.util.Locale

@Composable
fun BudgetOptimizerScreen(
    onBack: () -> Unit,
    onUpgrade: () -> Unit,
    viewModel: BudgetOptimizerViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(topBar = { GlowTopBar(title = "Budget optimizer", onBack = onBack) }) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (val current = state) {
                ScreenState.Loading -> Column(modifier = Modifier.padding(GlowSpacing.md)) {
                    ShimmerSkeleton(height = 64.dp)
                    ShimmerSkeleton(height = 96.dp, modifier = Modifier.padding(top = GlowSpacing.sm))
                }
                ScreenState.Locked -> Box(modifier = Modifier.padding(GlowSpacing.md)) {
                    LockedCard(
                        title = "Budget optimizer is Premium",
                        body = "Find products you keep buying that don't seem to be earning their spot in your routine.",
                        onUnlock = onUpgrade,
                    )
                }
                is ScreenState.Error -> Box(modifier = Modifier.padding(GlowSpacing.md)) {
                    ErrorState(message = current.message, onRetry = viewModel::load)
                }
                is ScreenState.Empty -> Box(modifier = Modifier.padding(GlowSpacing.md)) {
                    EmptyState(title = current.title, body = current.body, ctaLabel = "Refresh", onCtaClick = viewModel::load)
                }
                is ScreenState.Content -> BudgetContent(current.value)
            }
        }
    }
}

@Composable
private fun BudgetContent(optimizer: BudgetOptimizer) {
    val glow = LocalGlowColors.current
    LazyColumn(
        contentPadding = PaddingValues(GlowSpacing.md),
        verticalArrangement = Arrangement.spacedBy(GlowSpacing.sm),
    ) {
        item {
            GlowCard {
                Text("Estimated annual waste", style = MaterialTheme.typography.labelMedium, color = glow.ink600)
                Text(
                    text = formatCents(optimizer.estimatedAnnualWasteCents, optimizer.currency),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = glow.ink900,
                )
            }
        }
        item {
            DisclaimerNote(text = optimizer.disclaimer.ifBlank {
                "Based on products you've kept stable without a clear benefit — not a guarantee you should stop using them."
            })
        }
        items(optimizer.flagged, key = { it.productId }) { product -> FlaggedProductCard(product) }
    }
}

@Composable
private fun FlaggedProductCard(product: BudgetFlaggedProduct) {
    val glow = LocalGlowColors.current
    GlowCard {
        Text(product.productName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = glow.ink900)
        Text("${product.daysStable} days stable", style = MaterialTheme.typography.labelMedium, color = glow.ink600)
        Text(product.reason, style = MaterialTheme.typography.bodyMedium, color = glow.ink900, modifier = Modifier.padding(top = GlowSpacing.xs))
        // `estimated_annual_cost_cents` can be null when no offer price is on file — the product
        // still renders as flagged, just without a figure, per ANDROID_PLAN.md §3.5.
        Text(
            text = product.estimatedAnnualCostCents?.let { formatCents(it, product.currency) } ?: "Cost unknown",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = if (product.estimatedAnnualCostCents != null) glow.honey700 else glow.ink600,
            modifier = Modifier.padding(top = GlowSpacing.xs),
        )
    }
}

private fun formatCents(cents: Int, currency: String): String {
    val amount = cents / 100.0
    val symbol = when (currency.uppercase(Locale.US)) {
        "USD" -> "$"
        "EUR" -> "€"
        "GBP" -> "£"
        else -> "$currency "
    }
    return "$symbol${"%.2f".format(amount)}"
}
