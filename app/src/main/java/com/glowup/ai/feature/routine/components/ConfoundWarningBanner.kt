package com.glowup.ai.feature.routine.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.glowup.ai.core.design.LocalGlowColors
import com.glowup.ai.domain.model.ConfoundCheck

/**
 * A dismissible warning naming the at-risk product(s) from `GET /confound-check`, and explaining
 * that proceeding means the evidence for this routine change will come back `evidence_unclear`.
 * Used both as a pre-submit check (frontend-api-map.md "Growth features" section) and to surface
 * the inline `confound_warning` a `POST /routine-events` response can carry.
 */
@Composable
fun ConfoundWarningBanner(
    modifier: Modifier = Modifier,
    confound: ConfoundCheck,
    onDismiss: () -> Unit,
) {
    if (!confound.confounded) return
    val glow = LocalGlowColors.current
    val names = confound.activeWindows.joinToString(", ") { it.productName }.ifBlank { "another active product" }

    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(glow.warning.copy(alpha = 0.9f))
                .padding(14.dp)
                .semantics {
                    contentDescription = "Warning: overlapping routine change with $names. ${confound.message.orEmpty()}"
                    liveRegion = LiveRegionMode.Polite
                },
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween) {
            Text(
                text = "This may overlap with $names",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = glow.onWarning,
                modifier = Modifier.padding(end = 8.dp),
            )
            IconButton(onClick = onDismiss, modifier = Modifier.padding(0.dp)) {
                Text("Dismiss", color = glow.onWarning, style = MaterialTheme.typography.labelMedium)
            }
        }
        Text(
            text =
                confound.message
                    ?: "Starting or changing this now makes it harder to tell which product caused a change. If you proceed, evidence for this window will come back as \"evidence unclear\" instead of a confident verdict.",
            style = MaterialTheme.typography.bodySmall,
            color = glow.onWarning,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}
