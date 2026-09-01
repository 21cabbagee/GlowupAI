package com.glowup.ai.feature.account.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.glowup.ai.core.design.GlowSpacing
import com.glowup.ai.core.design.LocalGlowColors

/**
 * The REAL Premium feature set the backend actually gates with a `403`, per
 * frontend-api-map.md — not an aspirational marketing list. Every entry here corresponds to a
 * route this app calls that returns `PremiumRequired` for a free plan.
 */
data class PremiumFeature(
    val title: String,
    val description: String,
)

val PREMIUM_FEATURES: List<PremiumFeature> =
    listOf(
        PremiumFeature("Experiments", "Run structured before/after tests on a product or routine change."),
        PremiumFeature("Data Q&A", "Ask questions about your own history and get cited, threaded answers."),
        PremiumFeature("Discover recommendations", "Personalized product discovery beyond manual search."),
        PremiumFeature("Ingredient explainer", "See why an ingredient earned its verdict, with cited sources."),
        PremiumFeature("Root-cause search", "Trace a metric change back to the routine events around it."),
        PremiumFeature("Budget optimizer", "Get a lower-cost routine that keeps the same active ingredients."),
        PremiumFeature("Derm export", "A clinician-ready summary of your appearance history."),
        PremiumFeature("Historical reprocessing", "Re-run past captures through the latest measurement model."),
        PremiumFeature("Pre-purchase prediction", "A similarity-based read on a product before you buy it."),
    )

/**
 * Lists [PREMIUM_FEATURES]. Framed strictly as a similarity/analytics upsell — never phrased as
 * a medical guarantee, consistent with the disclaimer requirement threaded through every other
 * surface in the app.
 */
@Composable
fun PremiumFeatureList(modifier: Modifier = Modifier) {
    val glow = LocalGlowColors.current
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(GlowSpacing.sm)) {
        PREMIUM_FEATURES.forEach { feature ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(GlowSpacing.sm),
            ) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = glow.honey700,
                    modifier = Modifier.size(20.dp),
                )
                Column {
                    Text(
                        text = feature.title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = glow.ink900,
                    )
                    Text(
                        text = feature.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = glow.ink600,
                    )
                }
            }
        }
    }
}
