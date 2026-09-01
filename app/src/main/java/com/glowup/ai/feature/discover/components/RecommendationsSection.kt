package com.glowup.ai.feature.discover.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.glowup.ai.core.design.GlowSpacing
import com.glowup.ai.core.design.LocalGlowColors
import com.glowup.ai.core.ui.DisclaimerNote
import com.glowup.ai.core.ui.EmptyState
import com.glowup.ai.core.ui.ErrorState
import com.glowup.ai.core.ui.GlowCard
import com.glowup.ai.core.ui.LockedCard
import com.glowup.ai.core.ui.SectionHeader
import com.glowup.ai.core.ui.ShimmerSkeleton
import com.glowup.ai.domain.model.Discover
import com.glowup.ai.domain.model.DiscoverRecommendation
import com.glowup.ai.feature.discover.SectionState

/**
 * Cohort context — NEVER a personal verdict, NEVER a medical recommendation
 * (ANDROID_PLAN.md §3.6). [DisclaimerNote] is rendered every time [state] is [SectionState.Content],
 * whether or not [Discover.recommendations] is empty, because the backend's own disclaimer text
 * must always accompany this data per `frontend-api-map.md`'s `GET /discover` ideal-UI-state note.
 */
@Composable
fun RecommendationsSection(
    modifier: Modifier = Modifier,
    state: SectionState<Discover>,
    onRetry: () -> Unit,
    onUnlock: () -> Unit,
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(GlowSpacing.sm)) {
        SectionHeader(title = "Discover for your routine")
        when (state) {
            is SectionState.Loading -> {
                ShimmerSkeleton(height = 96.dp, modifier = Modifier.padding(top = GlowSpacing.sm))
                ShimmerSkeleton(height = 96.dp, modifier = Modifier.padding(top = GlowSpacing.sm))
            }

            is SectionState.Locked -> {
                LockedCard(
                    title = "Cohort recommendations",
                    body = "See which products a consenting cohort of users found likely useful, with sample size and effect — never a personal verdict.",
                    onUnlock = onUnlock,
                )
            }

            is SectionState.Error -> {
                ErrorState(message = state.message, onRetry = onRetry)
            }

            is SectionState.Empty -> {
                EmptyState(
                    title = state.title,
                    body = state.body,
                    ctaLabel = "Refresh",
                    onCtaClick = onRetry,
                )
            }

            is SectionState.Content -> {
                val discover = state.value
                DisclaimerNote(text = discover.disclaimer)
                if (discover.recommendations.isEmpty()) {
                    // Cold start is the NORMAL early state, not an error: recommendations only
                    // appear once at least `minimumCohortSize` consenting users contribute a
                    // likely-useful verdict for the same product.
                    EmptyState(
                        modifier = Modifier.padding(top = GlowSpacing.sm),
                        title = "No cohort recommendations yet",
                        body = "Discover needs at least ${discover.minimumCohortSize} consenting users to mark the same product likely useful before it can show cohort context here. Keep logging routine events and verdicts — this fills in as the community does.",
                        ctaLabel = "Check again",
                        onCtaClick = onRetry,
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(GlowSpacing.sm)) {
                        discover.recommendations.forEach { recommendation ->
                            RecommendationCard(recommendation = recommendation)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RecommendationCard(recommendation: DiscoverRecommendation) {
    val glow = LocalGlowColors.current
    GlowCard(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = recommendation.name,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = glow.ink900,
        )
        recommendation.category?.let { category ->
            Text(
                text = category.replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.labelMedium,
                color = glow.ink600,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(top = GlowSpacing.sm),
            horizontalArrangement = Arrangement.spacedBy(GlowSpacing.lg),
        ) {
            LabelledValue(label = "Sample size", value = "${recommendation.sampleSize} users")
            LabelledValue(label = "Average effect", value = "%.2f".format(recommendation.averageEffect))
        }
        Text(
            text = recommendation.reason,
            style = MaterialTheme.typography.bodySmall,
            fontStyle = FontStyle.Italic,
            color = glow.ink600,
            modifier = Modifier.padding(top = GlowSpacing.sm),
        )
    }
}

@Composable
private fun LabelledValue(
    label: String,
    value: String,
) {
    val glow = LocalGlowColors.current
    Column {
        Text(text = value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = glow.ink900)
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = glow.ink600)
    }
}
