package com.glowup.ai.feature.discover.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.glowup.ai.core.design.GlowSpacing
import com.glowup.ai.core.design.LocalGlowColors
import com.glowup.ai.core.ui.DisclaimerNote
import com.glowup.ai.core.ui.ErrorState
import com.glowup.ai.core.ui.GlowButton
import com.glowup.ai.core.ui.GlowCard
import com.glowup.ai.core.ui.GlowTextField
import com.glowup.ai.core.ui.LockedCard
import com.glowup.ai.core.ui.SectionHeader
import com.glowup.ai.domain.model.ProductOverlap
import com.glowup.ai.domain.model.PurchaseGuidance
import com.glowup.ai.feature.discover.PurchaseGuidanceFormState
import com.glowup.ai.feature.discover.PurchaseGuidancePanelState
import com.glowup.ai.feature.discover.SectionState

/**
 * Manual purchase guidance — same "similarity signal, never efficacy" framing as
 * [PredictSection], for a product the user hasn't added to the catalog yet (by name, barcode, or
 * pasted ingredient list). `POST /purchase-guidance`.
 */
@Composable
fun PurchaseGuidanceSection(
    modifier: Modifier = Modifier,
    state: PurchaseGuidancePanelState,
    onNameChange: (String) -> Unit,
    onBarcodeChange: (String) -> Unit,
    onCategoryChange: (String) -> Unit,
    onIngredientsChange: (String) -> Unit,
    onPriceChange: (String) -> Unit,
    onCurrencyChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onUnlock: () -> Unit,
    onRetry: () -> Unit,
) {
    val glow = LocalGlowColors.current
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(GlowSpacing.sm)) {
        SectionHeader(title = "Purchase guidance")
        Text(
            text = "Not sure whether to add a product to your catalog yet? Check it here first.",
            style = MaterialTheme.typography.bodySmall,
            color = glow.ink600,
        )

        GuidanceForm(
            form = state.form,
            submitting = state.submitting,
            onNameChange = onNameChange,
            onBarcodeChange = onBarcodeChange,
            onCategoryChange = onCategoryChange,
            onIngredientsChange = onIngredientsChange,
            onPriceChange = onPriceChange,
            onCurrencyChange = onCurrencyChange,
            onSubmit = onSubmit,
        )

        when (val result = state.result) {
            null -> Unit
            is SectionState.Loading -> Text(
                text = "Checking…",
                modifier = Modifier.padding(top = GlowSpacing.sm),
                color = glow.ink600,
            )

            is SectionState.Locked -> LockedCard(
                modifier = Modifier.padding(top = GlowSpacing.md),
                title = "Purchase guidance",
                body = "Get a similarity check against your own and cohort history before you buy a new product.",
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

            is SectionState.Content -> GuidanceResultCard(result.value)
        }
    }
}

@Composable
private fun GuidanceForm(
    form: PurchaseGuidanceFormState,
    submitting: Boolean,
    onNameChange: (String) -> Unit,
    onBarcodeChange: (String) -> Unit,
    onCategoryChange: (String) -> Unit,
    onIngredientsChange: (String) -> Unit,
    onPriceChange: (String) -> Unit,
    onCurrencyChange: (String) -> Unit,
    onSubmit: () -> Unit,
) {
    GlowCard(modifier = Modifier.fillMaxWidth()) {
        GlowTextField(
            value = form.name,
            onValueChange = onNameChange,
            label = "Product name",
            supportingText = "Name or barcode is required.",
        )
        GlowTextField(
            modifier = Modifier.padding(top = GlowSpacing.sm),
            value = form.barcode,
            onValueChange = onBarcodeChange,
            label = "Barcode (optional)",
            keyboardType = KeyboardType.Number,
        )
        GlowTextField(
            modifier = Modifier.padding(top = GlowSpacing.sm),
            value = form.category,
            onValueChange = onCategoryChange,
            label = "Category",
        )
        GlowTextField(
            modifier = Modifier.padding(top = GlowSpacing.sm),
            value = form.ingredientsText,
            onValueChange = onIngredientsChange,
            label = "Ingredients (comma-separated, optional)",
            singleLine = false,
        )
        GlowTextField(
            modifier = Modifier.padding(top = GlowSpacing.sm),
            value = form.priceText,
            onValueChange = onPriceChange,
            label = "Price (optional)",
            keyboardType = KeyboardType.Decimal,
        )
        GlowTextField(
            modifier = Modifier.padding(top = GlowSpacing.sm),
            value = form.currency,
            onValueChange = onCurrencyChange,
            label = "Currency",
            supportingText = "3-letter code, e.g. INR",
        )
        GlowButton(
            modifier = Modifier.padding(top = GlowSpacing.md),
            text = "Check this product",
            onClick = onSubmit,
            enabled = form.canSubmit && !submitting,
            loading = submitting,
        )
    }
}

@Composable
private fun GuidanceResultCard(guidance: PurchaseGuidance) {
    val glow = LocalGlowColors.current
    GlowCard(modifier = Modifier.fillMaxWidth().padding(top = GlowSpacing.md)) {
        Text(text = guidance.headline, style = MaterialTheme.typography.bodyLarge, color = glow.ink900)
        guidance.nextAction?.let { nextAction ->
            Text(
                text = nextAction,
                style = MaterialTheme.typography.bodyMedium,
                color = glow.ink600,
                modifier = Modifier.padding(top = GlowSpacing.sm),
            )
        }
        DisclaimerNote(modifier = Modifier.padding(top = GlowSpacing.sm), text = guidance.disclaimer)

        guidance.estimatedAnnualCostCents?.let { cents ->
            Text(
                text = "Estimated annual cost: ${guidance.currency} %.2f".format(cents / 100.0),
                style = MaterialTheme.typography.bodySmall,
                color = glow.ink600,
                modifier = Modifier.padding(top = GlowSpacing.sm),
            )
        }

        OverlapGroup("Overlaps with products you're investigating", guidance.overlapWithInvestigate)
        OverlapGroup("Overlaps with products likely useful for you", guidance.overlapWithLikelyUseful)
        OverlapGroup("Overlaps across the cohort", guidance.cohortOverlap)
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
