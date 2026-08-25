package com.glowup.ai.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.glowup.ai.core.design.GlowUpTheme
import com.glowup.ai.core.design.LocalGlowColors

/**
 * The Premium upsell surface. This is deliberately NOT an [EmptyState] — the backend returns
 * real `403`s and `label == "locked"` verdicts for gated features, and that is a distinct
 * situation from "you have no data yet". A honey band and a lock glyph keep it visually
 * unmistakable from an empty state at a glance.
 */
@Composable
fun LockedCard(
    modifier: Modifier = Modifier,
    title: String,
    body: String,
    ctaLabel: String = "Unlock Premium",
    onUnlock: () -> Unit,
    unlockLoading: Boolean = false,
) {
    val glow = LocalGlowColors.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(glow.surfaceCard, RoundedCornerShape(18.dp))
            .semantics {
                contentDescription = "Premium required. $title. $body"
            },
    ) {
        // A single flat honey band — no gradient wash.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .background(glow.honey500),
        )
        Column(modifier = Modifier.padding(20.dp)) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(glow.honey500, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.Lock,
                    contentDescription = null,
                    tint = glow.ink900,
                    modifier = Modifier.size(20.dp),
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = glow.ink900,
                modifier = Modifier.padding(top = 12.dp),
            )
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = glow.ink600,
                modifier = Modifier.padding(top = 6.dp),
            )
            GlowButton(
                modifier = Modifier.padding(top = 16.dp),
                text = ctaLabel,
                onClick = onUnlock,
                variant = GlowButtonVariant.Primary,
                loading = unlockLoading,
            )
        }
    }
}

@Preview(name = "Light", showBackground = true)
@Composable
private fun LockedCardPreviewLight() {
    GlowUpTheme(darkTheme = false) { PreviewContent() }
}

@Preview(name = "Dark", showBackground = true)
@Composable
private fun LockedCardPreviewDark() {
    GlowUpTheme(darkTheme = true) { PreviewContent() }
}

@Composable
private fun PreviewContent() {
    LockedCard(
        modifier = Modifier.padding(16.dp),
        title = "Ingredient explainer",
        body = "See exactly why each ingredient earned its verdict, with cited sources.",
        onUnlock = {},
    )
}
