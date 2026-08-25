package com.glowup.ai.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.glowup.ai.core.design.GlowUpTheme
import com.glowup.ai.core.design.LocalGlowColors

/** Visual treatments for [GlowButton]. Brand yellow is a surface only — never text. */
enum class GlowButtonVariant { Primary, Secondary, Ghost, Danger }

private data class ButtonColors(
    val background: Color,
    val content: Color,
    val border: Color?,
)

@Composable
private fun colorsFor(variant: GlowButtonVariant): ButtonColors {
    val glow = LocalGlowColors.current
    return when (variant) {
        GlowButtonVariant.Primary -> ButtonColors(
            background = glow.honey500,
            content = glow.ink900,
            border = null,
        )
        GlowButtonVariant.Secondary -> ButtonColors(
            background = glow.surfaceCard,
            content = glow.ink900,
            border = glow.ink600.copy(alpha = 0.35f),
        )
        GlowButtonVariant.Ghost -> ButtonColors(
            background = Color.Transparent,
            content = glow.ink600,
            border = null,
        )
        GlowButtonVariant.Danger -> ButtonColors(
            background = glow.danger.copy(alpha = 0.12f),
            content = glow.danger,
            border = null,
        )
    }
}

/**
 * Primary button family for the app. Loading state swaps the label for an inline spinner
 * while keeping the button's measured width stable (the label stays laid out, just invisible).
 *
 * [modifier] is applied to the outer touch target, which is never smaller than 48dp tall.
 */
@Composable
fun GlowButton(
    modifier: Modifier = Modifier,
    text: String,
    onClick: () -> Unit,
    variant: GlowButtonVariant = GlowButtonVariant.Primary,
    enabled: Boolean = true,
    loading: Boolean = false,
    contentDescription: String? = null,
) {
    val colors = colorsFor(variant)
    val isInteractive = enabled && !loading
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
            .background(
                color = if (isInteractive) colors.background else colors.background.copy(alpha = 0.5f),
                shape = RoundedCornerShape(14.dp),
            )
            .then(
                if (colors.border != null) {
                    Modifier.border(1.dp, colors.border, RoundedCornerShape(14.dp))
                } else Modifier,
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = isInteractive,
                onClick = onClick,
            )
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .semantics {
                this.contentDescription = contentDescription ?: text
                if (!isInteractive) disabled()
            },
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = text,
                color = if (isInteractive) colors.content else colors.content.copy(alpha = 0.5f),
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.alpha(if (loading) 0f else 1f),
            )
        }
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                color = colors.content,
                strokeWidth = 2.dp,
            )
        }
    }
}

@Preview(name = "Light", showBackground = true)
@Composable
private fun GlowButtonPreviewLight() {
    GlowUpTheme(darkTheme = false) {
        ButtonPreviewColumn()
    }
}

@Preview(name = "Dark", showBackground = true)
@Composable
private fun GlowButtonPreviewDark() {
    GlowUpTheme(darkTheme = true) {
        ButtonPreviewColumn()
    }
}

@Composable
private fun ButtonPreviewColumn() {
    androidx.compose.foundation.layout.Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp),
    ) {
        GlowButton(text = "Continue", onClick = {}, variant = GlowButtonVariant.Primary)
        GlowButton(text = "Cancel", onClick = {}, variant = GlowButtonVariant.Secondary)
        GlowButton(text = "Skip", onClick = {}, variant = GlowButtonVariant.Ghost)
        GlowButton(text = "Delete account", onClick = {}, variant = GlowButtonVariant.Danger)
        GlowButton(text = "Disabled", onClick = {}, enabled = false)
        GlowButton(text = "Uploading", onClick = {}, loading = true)
    }
}
