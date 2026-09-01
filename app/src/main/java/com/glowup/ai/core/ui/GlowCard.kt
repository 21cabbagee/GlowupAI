package com.glowup.ai.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.glowup.ai.core.design.GlowShapes
import com.glowup.ai.core.design.GlowSpacing
import com.glowup.ai.core.design.GlowUpTheme
import com.glowup.ai.core.design.LocalGlowColors

/**
 * The base surface for grouped content. Optionally interactive (pass [onClick]) — when it is,
 * the whole card becomes a single 48dp+ touch target with a click role for TalkBack.
 */
@Composable
fun GlowCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    contentDescription: String? = null,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit,
) {
    val glow = LocalGlowColors.current
    var cardModifier =
        modifier
            .background(color = glow.surfaceCard, shape = GlowShapes.md)

    cardModifier =
        if (onClick != null) {
            cardModifier.clickable(
                onClickLabel = contentDescription,
                role = Role.Button,
                onClick = onClick,
            )
        } else {
            cardModifier
        }

    Column(
        modifier = cardModifier.padding(GlowSpacing.md),
        content = content,
    )
}

@Preview(name = "Light", showBackground = true)
@Composable
private fun GlowCardPreviewLight() {
    GlowUpTheme(darkTheme = false) { PreviewContent() }
}

@Preview(name = "Dark", showBackground = true)
@Composable
private fun GlowCardPreviewDark() {
    GlowUpTheme(darkTheme = true) { PreviewContent() }
}

@Composable
private fun PreviewContent() {
    GlowCard(modifier = Modifier.padding(16.dp)) {
        Text("Skin health card", style = MaterialTheme.typography.titleMedium)
        Text("Redness trending down over the last 7 captures.", style = MaterialTheme.typography.bodyMedium)
    }
}
