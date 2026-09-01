package com.glowup.ai.core.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.glowup.ai.core.design.GlowUpTheme
import com.glowup.ai.core.design.LocalGlowColors

/**
 * A labelled text input with room for an error message and/or supporting hint text below it.
 * When [errorText] is non-null the field is marked invalid for TalkBack and the error copy
 * replaces the supporting text.
 */
@Composable
fun GlowTextField(
    modifier: Modifier = Modifier,
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String? = null,
    supportingText: String? = null,
    errorText: String? = null,
    enabled: Boolean = true,
    singleLine: Boolean = true,
    keyboardType: KeyboardType = KeyboardType.Text,
    visualTransformation: VisualTransformation = VisualTransformation.None,
) {
    val glow = LocalGlowColors.current
    val isError = errorText != null

    Column(modifier = modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 48.dp)
                    .semantics {
                        contentDescription = label
                        if (isError) error(errorText ?: "Invalid value")
                    },
            enabled = enabled,
            singleLine = singleLine,
            isError = isError,
            label = { Text(label) },
            placeholder = placeholder?.let { { Text(it) } },
            keyboardOptions =
                androidx.compose.foundation.text
                    .KeyboardOptions(keyboardType = keyboardType),
            visualTransformation = visualTransformation,
            colors =
                OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = glow.honey600,
                    cursorColor = glow.honey600,
                    errorBorderColor = glow.danger,
                ),
        )
        val caption = errorText ?: supportingText
        if (caption != null) {
            Text(
                text = caption,
                style = MaterialTheme.typography.bodySmall,
                color = if (isError) glow.danger else glow.ink600,
                modifier = Modifier.padding(start = 4.dp, top = 4.dp),
            )
        }
    }
}

@Preview(name = "Light", showBackground = true)
@Composable
private fun GlowTextFieldPreviewLight() {
    GlowUpTheme(darkTheme = false) { PreviewContent() }
}

@Preview(name = "Dark", showBackground = true)
@Composable
private fun GlowTextFieldPreviewDark() {
    GlowUpTheme(darkTheme = true) { PreviewContent() }
}

@Composable
private fun PreviewContent() {
    Column(modifier = Modifier.padding(16.dp)) {
        GlowTextField(
            value = "",
            onValueChange = {},
            label = "Display name",
            supportingText = "Shown on your profile only.",
            modifier = Modifier.padding(bottom = 12.dp),
        )
        GlowTextField(
            value = "jane",
            onValueChange = {},
            label = "Email",
            errorText = "Enter a valid email address.",
        )
    }
}
