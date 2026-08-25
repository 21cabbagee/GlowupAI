package com.glowup.ai.core.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.glowup.ai.core.design.GlowUpTheme
import com.glowup.ai.core.design.LocalGlowColors

/** A section title with an optional trailing text action (e.g. "See all"). */
@Composable
fun SectionHeader(
    modifier: Modifier = Modifier,
    title: String,
    actionLabel: String? = null,
    onActionClick: (() -> Unit)? = null,
) {
    val glow = LocalGlowColors.current
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = glow.ink900,
        )
        if (actionLabel != null && onActionClick != null) {
            TextButton(
                onClick = onActionClick,
                modifier = Modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp),
            ) {
                Text(
                    text = actionLabel,
                    color = glow.honey700,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@Preview(name = "Light", showBackground = true)
@Composable
private fun SectionHeaderPreviewLight() {
    GlowUpTheme(darkTheme = false) {
        SectionHeader(title = "Recent captures", actionLabel = "See all", onActionClick = {})
    }
}

@Preview(name = "Dark", showBackground = true)
@Composable
private fun SectionHeaderPreviewDark() {
    GlowUpTheme(darkTheme = true) {
        SectionHeader(title = "Recent captures", actionLabel = "See all", onActionClick = {})
    }
}
