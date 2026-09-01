package com.glowup.ai.core.ui

import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.glowup.ai.core.design.GlowUpTheme
import com.glowup.ai.core.design.LocalGlowColors

/**
 * The app's top bar. [onBack] is only rendered as a back affordance when non-null; screens at
 * the root of a tab pass null.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlowTopBar(
    modifier: Modifier = Modifier,
    title: String,
    onBack: (() -> Unit)? = null,
    actions: @Composable () -> Unit = {},
) {
    val glow = LocalGlowColors.current
    CenterAlignedTopAppBar(
        modifier = modifier,
        title = { Text(title, fontWeight = FontWeight.Bold) },
        navigationIcon = {
            if (onBack != null) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                    )
                }
            }
        },
        actions = { actions() },
        colors =
            TopAppBarDefaults.centerAlignedTopAppBarColors(
                containerColor = glow.paper,
                titleContentColor = glow.ink900,
                navigationIconContentColor = glow.ink900,
                actionIconContentColor = glow.ink900,
            ),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(name = "Light", showBackground = true)
@Composable
private fun GlowTopBarPreviewLight() {
    GlowUpTheme(darkTheme = false) {
        GlowTopBar(title = "Home")
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(name = "Dark", showBackground = true)
@Composable
private fun GlowTopBarPreviewDark() {
    GlowUpTheme(darkTheme = true) {
        GlowTopBar(title = "Capture", onBack = {})
    }
}
