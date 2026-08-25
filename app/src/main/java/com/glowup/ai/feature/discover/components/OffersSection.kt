package com.glowup.ai.feature.discover.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Verified
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.glowup.ai.core.design.GlowSpacing
import com.glowup.ai.core.design.LocalGlowColors
import com.glowup.ai.core.ui.EmptyState
import com.glowup.ai.core.ui.ErrorState
import com.glowup.ai.core.ui.GlowButton
import com.glowup.ai.core.ui.GlowButtonVariant
import com.glowup.ai.core.ui.GlowCard
import com.glowup.ai.core.ui.SectionHeader
import com.glowup.ai.core.ui.ShimmerSkeleton
import com.glowup.ai.domain.model.Offer
import com.glowup.ai.feature.discover.SectionState
import java.util.Locale

/**
 * Affiliate offers — free for EVERY plan (ANDROID_PLAN.md non-negotiable constraint). This
 * composable never checks `canUsePremium` and never renders [com.glowup.ai.core.ui.LockedCard];
 * that is intentional, not an oversight — commerce must show on a free account exactly as it does
 * on Premium.
 */
@Composable
fun OffersSection(
    modifier: Modifier = Modifier,
    state: SectionState<List<Offer>>,
    pendingOfferIds: Set<String>,
    offerErrors: Map<String, String>,
    openFailedOfferId: String?,
    onOfferCtaClick: (Offer) -> Unit,
    onRetryOpen: () -> Unit,
    onRetry: () -> Unit,
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(GlowSpacing.sm)) {
        SectionHeader(title = "Where to buy")
        when (state) {
            is SectionState.Loading -> {
                ShimmerSkeleton(height = 84.dp)
                ShimmerSkeleton(height = 84.dp, modifier = Modifier.padding(top = GlowSpacing.sm))
            }

            is SectionState.Error -> ErrorState(message = state.message, onRetry = onRetry)

            is SectionState.Empty -> EmptyState(
                title = state.title,
                body = state.body,
                ctaLabel = "Refresh",
                onCtaClick = onRetry,
            )

            // Offers are never locked — see class doc — but the exhaustive `when` still needs a
            // branch; treat it identically to Error so a bug elsewhere can never silently hide
            // commerce.
            is SectionState.Locked -> ErrorState(
                message = "Couldn't load offers right now.",
                onRetry = onRetry,
            )

            is SectionState.Content -> {
                val offers = state.value
                if (offers.isEmpty()) {
                    EmptyState(
                        title = "No offers listed yet",
                        body = "Merchant offers for products in your history will show up here once they're added.",
                        ctaLabel = "Refresh",
                        onCtaClick = onRetry,
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(GlowSpacing.sm)) {
                        offers.forEach { offer ->
                            OfferCard(
                                offer = offer,
                                pending = offer.id in pendingOfferIds,
                                errorMessage = offerErrors[offer.id],
                                openFailed = openFailedOfferId == offer.id,
                                onCtaClick = { onOfferCtaClick(offer) },
                                onRetryOpen = onRetryOpen,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OfferCard(
    offer: Offer,
    pending: Boolean,
    errorMessage: String?,
    openFailed: Boolean,
    onCtaClick: () -> Unit,
    onRetryOpen: () -> Unit,
) {
    val glow = LocalGlowColors.current
    GlowCard(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text(
                    text = offer.productName ?: "Product",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = glow.ink900,
                )
                Text(
                    text = offer.merchant,
                    style = MaterialTheme.typography.bodyMedium,
                    color = glow.ink600,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
            Text(
                text = formatPrice(offer.priceCents, offer.currency),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = glow.ink900,
            )
        }

        if (offer.disclosed) {
            Row(
                modifier = Modifier.padding(top = GlowSpacing.sm),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.Verified,
                    contentDescription = null,
                    tint = glow.ink600,
                    modifier = Modifier.padding(end = 0.dp),
                )
                Text(
                    text = "Affiliate link disclosed — placement never changes your verdict.",
                    style = MaterialTheme.typography.bodySmall,
                    color = glow.ink600,
                )
            }
        }

        if (errorMessage != null) {
            Text(
                text = errorMessage,
                style = MaterialTheme.typography.bodySmall,
                color = glow.danger,
                modifier = Modifier.padding(top = GlowSpacing.sm),
            )
        }

        if (openFailed) {
            Column(modifier = Modifier.padding(top = GlowSpacing.sm)) {
                Text(
                    text = "Couldn't open the merchant link.",
                    style = MaterialTheme.typography.bodySmall,
                    color = glow.danger,
                )
                GlowButton(
                    modifier = Modifier.padding(top = GlowSpacing.sm),
                    text = "Retry opening",
                    onClick = onRetryOpen,
                    variant = GlowButtonVariant.Secondary,
                )
            }
        } else {
            GlowButton(
                modifier = Modifier.padding(top = GlowSpacing.sm),
                text = "View offer",
                onClick = onCtaClick,
                enabled = !pending,
                loading = pending,
                variant = GlowButtonVariant.Primary,
            )
        }
    }
}

private fun formatPrice(priceCents: Int?, currency: String): String {
    if (priceCents == null) return "Price varies"
    val amount = priceCents / 100.0
    return String.format(Locale.getDefault(), "%s %.2f", currency, amount)
}
