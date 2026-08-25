package com.glowup.ai.feature.routine.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.glowup.ai.core.design.LocalGlowColors
import com.glowup.ai.core.ui.GlowButton
import com.glowup.ai.core.ui.GlowButtonVariant
import com.glowup.ai.core.ui.GlowTextField
import com.glowup.ai.domain.model.ProductCreateRequest

/**
 * Minimal-payload product creation form (`POST /api/products` needs only `name`; barcode,
 * category, ingredients and stabilization days are optional). This route is global — not
 * per-user and not idempotent (frontend-api-map.md trap #7) — so the confirm button disables
 * itself the instant [pending] flips true and never re-enables mid-flight.
 */
@Composable
fun AddProductSheet(
    modifier: Modifier = Modifier,
    prefillBarcode: String?,
    pending: Boolean,
    errorText: String?,
    onSubmit: (ProductCreateRequest) -> Unit,
) {
    val glow = LocalGlowColors.current
    var name by remember { mutableStateOf("") }
    var barcode by remember { mutableStateOf(prefillBarcode.orEmpty()) }
    var category by remember { mutableStateOf("") }
    var ingredientsText by remember { mutableStateOf("") }
    var stabilizationDaysText by remember { mutableStateOf("14") }

    Column(modifier = modifier.fillMaxWidth().padding(20.dp)) {
        Text(
            text = "Add a product",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = glow.ink900,
        )
        Text(
            text = "Product rows are shared across everyone using GlowUp AI — search first to avoid a duplicate.",
            style = MaterialTheme.typography.bodySmall,
            color = glow.ink600,
            modifier = Modifier.padding(top = 4.dp, bottom = 16.dp),
        )
        GlowTextField(
            value = name,
            onValueChange = { name = it },
            label = "Product name",
            supportingText = "Required.",
            enabled = !pending,
            modifier = Modifier.padding(bottom = 12.dp),
        )
        GlowTextField(
            value = barcode,
            onValueChange = { barcode = it },
            label = "Barcode (optional)",
            enabled = !pending,
            modifier = Modifier.padding(bottom = 12.dp),
        )
        GlowTextField(
            value = category,
            onValueChange = { category = it },
            label = "Category (optional)",
            placeholder = "e.g. serum, moisturizer, sunscreen",
            enabled = !pending,
            modifier = Modifier.padding(bottom = 12.dp),
        )
        GlowTextField(
            value = ingredientsText,
            onValueChange = { ingredientsText = it },
            label = "Key ingredients (optional, comma-separated)",
            enabled = !pending,
            modifier = Modifier.padding(bottom = 12.dp),
        )
        GlowTextField(
            value = stabilizationDaysText,
            onValueChange = { stabilizationDaysText = it.filter(Char::isDigit) },
            label = "Stabilization days",
            supportingText = "How long this product typically takes to show an effect. 0-180.",
            keyboardType = KeyboardType.Number,
            enabled = !pending,
            modifier = Modifier.padding(bottom = 12.dp),
        )
        if (errorText != null) {
            Text(
                text = errorText,
                style = MaterialTheme.typography.bodySmall,
                color = glow.danger,
                modifier = Modifier.padding(bottom = 8.dp),
            )
        }
        GlowButton(
            text = "Add product",
            loading = pending,
            enabled = name.isNotBlank() && !pending,
            onClick = {
                onSubmit(
                    ProductCreateRequest(
                        name = name.trim(),
                        barcode = barcode.trim().ifBlank { null },
                        category = category.trim().ifBlank { "other" },
                        ingredients = ingredientsText.split(",").map { it.trim() }.filter { it.isNotEmpty() },
                        stabilizationDays = stabilizationDaysText.toIntOrNull()?.coerceIn(0, 180) ?: 14,
                    ),
                )
            },
        )
    }
}
