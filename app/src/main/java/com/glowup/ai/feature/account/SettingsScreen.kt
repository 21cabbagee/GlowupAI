package com.glowup.ai.feature.account

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.glowup.ai.core.design.GlowSpacing
import com.glowup.ai.core.design.LocalGlowColors
import com.glowup.ai.core.ui.GlowButton
import com.glowup.ai.core.ui.GlowButtonVariant
import com.glowup.ai.core.ui.GlowCard
import com.glowup.ai.core.ui.GlowTopBar
import com.glowup.ai.core.ui.SectionHeader

/**
 * [com.glowup.ai.feature.shell.GlowDestination.Settings]. Every row here actually persists to
 * [com.glowup.ai.data.local.SessionStore] — see [SettingsViewModel]'s class doc for the bug this
 * fixes (all four rows were `onClick = {}` in the previous app).
 */
@Composable
fun SettingsRoute(
    navController: NavController,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val signedOut by viewModel.signedOut.collectAsStateWithLifecycle()

    LaunchedEffect(signedOut) {
        if (signedOut) navController.routeToWelcomeAfterSessionEnd()
    }

    SettingsContent(
        uiState = uiState,
        onBack = { navController.popBackStack() },
        onThemeSelected = viewModel::setTheme,
        onRemindersToggled = viewModel::setRemindersEnabled,
        onCadenceSelected = viewModel::setReminderCadenceDays,
        onSignOutClick = viewModel::signOut,
    )
}

@Composable
private fun SettingsContent(
    uiState: SettingsUiState,
    onBack: () -> Unit,
    onThemeSelected: (ThemePreference) -> Unit,
    onRemindersToggled: (Boolean) -> Unit,
    onCadenceSelected: (Int) -> Unit,
    onSignOutClick: () -> Unit,
) {
    Scaffold(topBar = { GlowTopBar(title = "Settings", onBack = onBack) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(GlowSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(GlowSpacing.lg),
        ) {
            ThemeSection(selected = uiState.theme, onThemeSelected = onThemeSelected)
            NotificationsSection(
                remindersEnabled = uiState.remindersEnabled,
                cadenceDays = uiState.reminderCadenceDays,
                onRemindersToggled = onRemindersToggled,
                onCadenceSelected = onCadenceSelected,
            )
            GlowButton(
                modifier = Modifier.fillMaxWidth(),
                text = "Sign out",
                onClick = onSignOutClick,
                variant = GlowButtonVariant.Secondary,
                loading = uiState.signingOut,
            )
        }
    }
}

@Composable
private fun ThemeSection(selected: ThemePreference, onThemeSelected: (ThemePreference) -> Unit) {
    GlowCard(modifier = Modifier.fillMaxWidth()) {
        SectionHeader(title = "Appearance")
        Column(
            modifier = Modifier.fillMaxWidth().padding(top = GlowSpacing.sm),
            verticalArrangement = Arrangement.spacedBy(GlowSpacing.xs),
        ) {
            ThemePreference.entries.forEach { option ->
                SelectableRow(
                    label = option.label,
                    selected = option == selected,
                    onClick = { onThemeSelected(option) },
                )
            }
        }
    }
}

@Composable
private fun NotificationsSection(
    remindersEnabled: Boolean,
    cadenceDays: Int,
    onRemindersToggled: (Boolean) -> Unit,
    onCadenceSelected: (Int) -> Unit,
) {
    val glow = LocalGlowColors.current
    GlowCard(modifier = Modifier.fillMaxWidth()) {
        SectionHeader(title = "Reminders & notifications")
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = GlowSpacing.md)
                .semantics { contentDescription = "Capture reminder notifications, ${if (remindersEnabled) "on" else "off"}" },
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text("Capture reminders", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = glow.ink900)
                Text("Get notified when it's time for your next photo.", style = MaterialTheme.typography.bodySmall, color = glow.ink600)
            }
            Switch(
                checked = remindersEnabled,
                onCheckedChange = onRemindersToggled,
                colors = SwitchDefaults.colors(checkedThumbColor = glow.honey600, checkedTrackColor = glow.honey300),
            )
        }
        Text(
            text = "Reminder cadence",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = glow.ink900,
            modifier = Modifier.padding(top = GlowSpacing.lg),
        )
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = GlowSpacing.sm),
            horizontalArrangement = Arrangement.spacedBy(GlowSpacing.sm),
        ) {
            REMINDER_CADENCE_OPTIONS.forEach { days ->
                CadenceChip(
                    days = days,
                    selected = days == cadenceDays,
                    enabled = remindersEnabled,
                    onClick = { onCadenceSelected(days) },
                )
            }
        }
    }
}

@Composable
private fun SelectableRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = "$label, ${if (selected) "selected" else "not selected"}" },
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        GlowButton(
            text = label,
            onClick = onClick,
            variant = if (selected) GlowButtonVariant.Primary else GlowButtonVariant.Secondary,
        )
    }
}

@Composable
private fun CadenceChip(days: Int, selected: Boolean, enabled: Boolean, onClick: () -> Unit) {
    GlowButton(
        text = if (days == 1) "Daily" else "Every $days days",
        onClick = onClick,
        variant = if (selected) GlowButtonVariant.Primary else GlowButtonVariant.Secondary,
        enabled = enabled,
    )
}
