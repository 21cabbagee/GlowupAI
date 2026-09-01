package com.glowup.ai.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.glowup.ai.core.design.GlowUpTheme
import com.glowup.ai.core.design.LocalGlowColors

/**
 * For the backend's async jobs (shelf-scan candidate extraction, reprocess) which the client
 * polls a status endpoint for. Shows a spinner and a status message; the caller owns the actual
 * polling loop and passes the current message down (e.g. "Reading shelf photo…",
 * "Recalculating history — values may change").
 */
@Composable
fun PollingIndicator(
    modifier: Modifier = Modifier,
    message: String,
) {
    val glow = LocalGlowColors.current
    val reducedMotion = isReducedMotionEnabled()

    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(glow.honey300.copy(alpha = 0.3f))
                .padding(horizontal = 14.dp, vertical = 12.dp)
                .semantics {
                    contentDescription = message
                    liveRegion = LiveRegionMode.Polite
                },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (reducedMotion) {
            // A static dot instead of an indeterminate spin, honouring reduced-motion.
            androidx.compose.foundation.layout.Box(
                modifier =
                    Modifier
                        .size(16.dp)
                        .clip(RoundedCornerShape(50))
                        .background(glow.honey600),
            )
        } else {
            CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                color = glow.honey600,
                strokeWidth = 2.dp,
            )
        }
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = glow.ink900,
            modifier = Modifier.padding(start = 10.dp),
        )
    }
}

@Preview(name = "Light", showBackground = true)
@Composable
private fun PollingIndicatorPreviewLight() {
    GlowUpTheme(darkTheme = false) {
        PollingIndicator(modifier = Modifier.padding(16.dp), message = "Reading your shelf photo…")
    }
}

@Preview(name = "Dark", showBackground = true)
@Composable
private fun PollingIndicatorPreviewDark() {
    GlowUpTheme(darkTheme = true) {
        PollingIndicator(modifier = Modifier.padding(16.dp), message = "Recalculating history — values may change")
    }
}
