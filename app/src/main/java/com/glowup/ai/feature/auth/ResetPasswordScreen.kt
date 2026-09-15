package com.glowup.ai.feature.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.glowup.ai.core.design.GlowSpacing
import com.glowup.ai.core.design.LocalGlowColors
import com.glowup.ai.core.ui.GlowButton
import com.glowup.ai.core.ui.GlowTextField
import com.glowup.ai.core.ui.GlowTopBar
import com.glowup.ai.feature.shell.GlowDestination

@Composable
fun ResetPasswordRoute(
    navController: NavController,
    viewModel: AuthViewModel = hiltViewModel(),
) {
    val state by viewModel.resetState.collectAsStateWithLifecycle()
    var password by rememberSaveable { mutableStateOf("") }
    var confirmation by rememberSaveable { mutableStateOf("") }
    var localError by rememberSaveable { mutableStateOf<String?>(null) }
    val busy = state is PasswordResetState.Sending
    val complete = state is PasswordResetState.Sent
    val glow = LocalGlowColors.current

    Scaffold(topBar = { GlowTopBar(title = "Choose a new password") }) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(GlowSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(GlowSpacing.md),
        ) {
            Text("Set a new password for your GlowUpAI account.", style = MaterialTheme.typography.bodyLarge)
            if (!complete) {
                GlowTextField(
                    value = password,
                    onValueChange = { password = it; localError = null },
                    label = "New password",
                    supportingText = "At least 8 characters.",
                    keyboardType = KeyboardType.Password,
                    visualTransformation = PasswordVisualTransformation(),
                    enabled = !busy,
                )
                GlowTextField(
                    value = confirmation,
                    onValueChange = { confirmation = it; localError = null },
                    label = "Confirm password",
                    errorText = localError,
                    keyboardType = KeyboardType.Password,
                    visualTransformation = PasswordVisualTransformation(),
                    enabled = !busy,
                )
                if (state is PasswordResetState.Failed) {
                    Text((state as PasswordResetState.Failed).message, color = glow.danger)
                }
                GlowButton(
                    modifier = Modifier.fillMaxWidth(),
                    text = "Update password",
                    loading = busy,
                    enabled = !busy,
                    onClick = {
                        localError = when {
                            password.length < 8 -> "Use at least 8 characters."
                            password != confirmation -> "Passwords do not match."
                            else -> null
                        }
                        if (localError == null) viewModel.updatePassword(password)
                    },
                )
            } else {
                Text("Your password was updated. Sign in with the new password.", color = glow.success)
                GlowButton(
                    modifier = Modifier.fillMaxWidth(),
                    text = "Return to sign in",
                    onClick = {
                        navController.navigate(GlowDestination.SignIn) {
                            popUpTo(GlowDestination.ResetPassword) { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                )
            }
        }
    }
}
