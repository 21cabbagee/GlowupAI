package com.glowup.ai.core.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.glowup.ai.core.design.GlowUpTheme
import com.glowup.ai.core.design.LocalGlowColors

/**
 * A themed wrapper over Material3's [ModalBottomSheet] so every sheet in the app shares the
 * same surface color, corner treatment and dismiss behaviour.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlowBottomSheet(
    modifier: Modifier = Modifier,
    onDismissRequest: () -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState(),
    content: @Composable () -> Unit,
) {
    val glow = LocalGlowColors.current
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        modifier = modifier,
        containerColor = glow.surfaceCard,
        contentColor = glow.ink900,
    ) {
        content()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(name = "Light", showBackground = true)
@Composable
private fun GlowBottomSheetPreviewLight() {
    GlowUpTheme(darkTheme = false) {
        Text("A GlowBottomSheet always opens over its calling screen.", modifier = Modifier.padding(16.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(name = "Dark", showBackground = true)
@Composable
private fun GlowBottomSheetPreviewDark() {
    GlowUpTheme(darkTheme = true) {
        Text("A GlowBottomSheet always opens over its calling screen.", modifier = Modifier.padding(16.dp))
    }
}
