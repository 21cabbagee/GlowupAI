package com.glowup.ai.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.vector.ImageVector
import com.glowup.ai.core.design.GlowUpTheme
import com.glowup.ai.core.design.LocalGlowColors

/** One destination in [GlowBottomBar]. */
data class GlowBottomBarItem(
    val label: String,
    val icon: ImageVector,
    val contentDescription: String = label,
)

/**
 * A 4-tab bottom bar with a centre overlapping capture FAB (Bumble-style). The FAB floats above
 * the bar's top edge so it reads as a fifth, primary action rather than a fifth tab.
 *
 * [items] must contain exactly 4 entries — 2 render left of the FAB, 2 render right of it.
 */
@Composable
fun GlowBottomBar(
    modifier: Modifier = Modifier,
    items: List<GlowBottomBarItem>,
    selectedIndex: Int,
    onItemSelected: (Int) -> Unit,
    onFabClick: () -> Unit,
    enabled: Boolean = true,
    fabContentDescription: String = "Capture",
) {
    val glow = LocalGlowColors.current
    val left = items.take(2)
    val right = items.drop(2).take(2)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars),
    ) {
        androidx.compose.foundation.layout.Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .background(glow.surfaceCard),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            androidx.compose.foundation.layout.Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                left.forEachIndexed { index, item ->
                    BottomBarTab(item, selected = selectedIndex == index) { onItemSelected(index) }
                }
            }
            // Reserve space for the overlapping FAB.
            Box(modifier = Modifier.size(64.dp))
            androidx.compose.foundation.layout.Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                right.forEachIndexed { index, item ->
                    val actualIndex = index + 2
                    BottomBarTab(item, selected = selectedIndex == actualIndex) { onItemSelected(actualIndex) }
                }
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = (-24).dp)
                .size(56.dp)
                .background(glow.honey500, CircleShape)
                .alpha(if (enabled) 1f else 0.45f)
                .clickable(
                    enabled = enabled,
                    onClickLabel = fabContentDescription,
                    role = Role.Button,
                    onClick = onFabClick,
                )
                .semantics {
                    contentDescription = fabContentDescription
                    if (!enabled) disabled()
                },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.CameraAlt,
                contentDescription = null,
                tint = glow.ink900,
            )
        }
    }
}

@Composable
private fun BottomBarTab(
    item: GlowBottomBarItem,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val glow = LocalGlowColors.current
    val isSelected = selected
    val tint = if (isSelected) glow.honey600 else glow.ink600
    val interactionSource = remember { MutableInteractionSource() }

    val tabDescription = buildString {
        append(item.contentDescription)
        if (isSelected) append(", selected")
    }

    Column(
        modifier = Modifier
            .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Tab,
                onClick = onClick,
            )
            .semantics {
                contentDescription = tabDescription
                this.selected = isSelected
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(imageVector = item.icon, contentDescription = null, tint = tint)
    }
}

@Preview(name = "Light", showBackground = true)
@Composable
private fun GlowBottomBarPreviewLight() {
    GlowUpTheme(darkTheme = false) { PreviewContent() }
}

@Preview(name = "Dark", showBackground = true)
@Composable
private fun GlowBottomBarPreviewDark() {
    GlowUpTheme(darkTheme = true) { PreviewContent() }
}

@Composable
private fun PreviewContent() {
    val items = listOf(
        GlowBottomBarItem("Home", Icons.Filled.CameraAlt),
        GlowBottomBarItem("Routine", Icons.Filled.CameraAlt),
        GlowBottomBarItem("Insights", Icons.Filled.CameraAlt),
        GlowBottomBarItem("You", Icons.Filled.CameraAlt),
    )
    GlowBottomBar(items = items, selectedIndex = 0, onItemSelected = {}, onFabClick = {})
}
