package com.glowup.ai.feature.home.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.glowup.ai.core.design.LocalGlowColors
import com.glowup.ai.core.ui.GlowCard

/**
 * Home's entry point into Discover — [com.glowup.ai.feature.shell.GlowDestination.Discover] is
 * deliberately not a bottom tab (reachable only from Home and the routine product picker per the
 * shell contract), so Home must offer an explicit way in.
 */
@Composable
fun DiscoverShortcutCard(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val glow = LocalGlowColors.current
    GlowCard(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick,
        contentDescription = "Open Discover",
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = Icons.Filled.Explore, contentDescription = null, tint = glow.honey700)
                Column(modifier = Modifier.padding(start = 12.dp)) {
                    Text(
                        text = "Discover",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = glow.ink900,
                    )
                    Text(
                        text = "Cohort picks and offers for people with similar skin",
                        style = MaterialTheme.typography.bodySmall,
                        color = glow.ink600,
                    )
                }
            }
            Icon(imageVector = Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = glow.ink600)
        }
    }
}
