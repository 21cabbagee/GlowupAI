package com.glowup.ai.feature.discover.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.glowup.ai.core.design.GlowSpacing
import com.glowup.ai.core.design.LocalGlowColors
import com.glowup.ai.core.ui.DisclaimerNote
import com.glowup.ai.core.ui.ErrorState
import com.glowup.ai.core.ui.GlowButton
import com.glowup.ai.core.ui.GlowButtonVariant
import com.glowup.ai.core.ui.GlowCard
import com.glowup.ai.core.ui.GlowTextField
import com.glowup.ai.core.ui.LockedCard
import com.glowup.ai.core.ui.SectionHeader
import com.glowup.ai.domain.model.Product
import com.glowup.ai.domain.model.ProductOverlap
import com.glowup.ai.domain.model.ProductPrediction
import com.glowup.ai.feature.discover.PredictPanelState
import com.glowup.ai.feature.discover.SectionState

/**
 * "Predict before you buy" — a similarity signal from ingredient overlap with your own and cohort
 * history, NEVER an efficacy prediction (ANDROID_PLAN.md §3.6 / frontend-api-map.md
 * `GET /products/{id}/predict`). [ProductPrediction.headline] is rendered as plain body text, never
 * as a title/hero claim, and [ProductPrediction.disclaimer] is always shown alongside it.
 */
@Composable
fun PredictSection(
    modifier: Modifier = Modifier,
    state: PredictPanelState,
    onQueryChange: (String) -> Unit,
    onProductSelected: (Product) -> Unit,
    onClearSelection: () -> Unit,
    onUnlock: () -> Unit,
    onRetry: () -> Unit,
) {
    val glow = LocalGlowColors.current
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(GlowSpacing.sm)) {
        SectionHeader(title = "Predict before you buy")

        if (state.selectedProduct == null) {
            GlowTextField(
                value = state.query,
                onValueChange = onQueryChange,
                label = "Search a product",
                supportingText = "Find a product to check its ingredient overlap with your history.",
            )
            if (state.searching) {
                CircularProgressIndicator(modifier = Modifier.padding(top = GlowSpacing.sm))
            }
            state.searchError?.let { error ->
                Text(text = error, color = glow.danger, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = GlowSpacing.sm))
            }
            if (state.searchResults.isNotEmpty()) {
                Column(
                    modifier = Modifier.padding(top = GlowSpacing.sm),
                    verticalArrangement = Arrangement.spacedBy(GlowSpacing.xs),
                ) {
                    state.searchResults.forEach { product ->
                        GlowCard(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { onProductSelected(product) },
                            contentDescription = "Select ${product.name} for prediction",
                        ) {
                            Text(text = product.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = glow.ink900)
                            Text(text = product.category.replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.labelSmall, color = glow.ink600)
                        }
                    }
                }
            }
        } else {
            Text(
                text = "Selected: ${state.selectedProduct.name}",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = glow.ink900,
            )
            GlowButton(
                modifier = Modifier.padding(top = GlowSpacing.sm),
                text = "Choose a different product",
                onClick = onClearSelection,
                variant = GlowButtonVariant.Ghost,
            )

            when (val result = state.result) {
                null, is SectionState.Loading -> CircularProgressIndicator(modifier = Modifier.padding(top = GlowSpacing.md))
                is SectionState.Locked -> LockedCard(
                    modifier = Modifier.padding(top = GlowSpacing.md),
                    title = "Predict before you buy",
                    body = "See how this product's ingredients overlap with products your history flagged for investigation or found likely useful.",
                    onUnlock = onUnlock,
                )

                is SectionState.Error -> ErrorState(
                    modifier = Modifier.padding(top = GlowSpacing.md),
                    message = result.message,
                    onRetry = onRetry,
                )

                is SectionState.Empty -> Text(
                    text = result.title,
                    modifier = Modifier.padding(top = GlowSpacing.md),
                    color = glow.ink600,
                )

                is SectionState.Content -> PredictionResultCard(result.value)
            }
        }
    }
}

@Composable
private fun PredictionResultCard(prediction: ProductPrediction) {
    val glow = LocalGlowColors.current
    GlowCard(modifier = Modifier.fillMaxWidth().padding(top = GlowSpacing.md)) {
        // Deliberately plain body text, never a headline/hero style — this is a similarity signal,
        // never phrased as a guarantee of how the product will perform.
        Text(text = prediction.headline, style = MaterialTheme.typography.bodyLarge, color = glow.ink900)
        DisclaimerNote(modifier = Modifier.padding(top = GlowSpacing.sm), text = prediction.disclaimer)

        OverlapGroup(title = "Overlaps with products you're investigating", overlaps = prediction.overlapWithInvestigate)
        OverlapGroup(title = "Overlaps with products likely useful for you", overlaps = prediction.overlapWithLikelyUseful)
        OverlapGroup(title = "Overlaps across the cohort", overlaps = prediction.cohortOverlap)
    }
}

@Composable
private fun OverlapGroup(title: String, overlaps: List<ProductOverlap>) {
    if (overlaps.isEmpty()) return
    val glow = LocalGlowColors.current
    Column(modifier = Modifier.padding(top = GlowSpacing.md)) {
        Text(text = title, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = glow.ink900)
        overlaps.forEach { overlap ->
            Text(
                text = "${overlap.productName} — shared: ${overlap.sharedIngredients.joinToString(", ")}",
                style = MaterialTheme.typography.bodySmall,
                color = glow.ink600,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}
