package com.glowup.ai.feature.account

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.glowup.ai.BuildConfig
import com.glowup.ai.core.design.GlowSpacing
import com.glowup.ai.core.ui.GlowTopBar
import java.time.LocalTime

/**
 * Complete, professional Settings screen with all app configuration options.
 * Organized into logical sections: Account, Notifications, Data & Privacy, Display, About, and Debug.
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
        onFontSizeSelected = viewModel::setFontSize,
        onReduceAnimationsToggled = viewModel::setReduceAnimations,
        onDailyCaptureReminderToggled = viewModel::setDailyCaptureReminder,
        onDailyCaptureTimeSelected = viewModel::setDailyCaptureTime,
        onStreakWarningsToggled = viewModel::setStreakWarnings,
        onWeeklyRecapToggled = viewModel::setWeeklyRecap,
        onAchievementCelebrationsToggled = viewModel::setAchievementCelebrations,
        onCloudBackupToggled = viewModel::setCloudBackup,
        onExportDataClick = viewModel::exportData,
        onSignOutClick = viewModel::signOut,
        onDeleteAccountClick = viewModel::requestDeleteAccount,
        onClearCacheClick = viewModel::clearCache,
        onForceCrashClick = viewModel::forceCrash,
        onViewLogsClick = viewModel::viewLogs,
        onApiEndpointSelected = viewModel::setApiEndpoint,
    )
}

@Composable
private fun SettingsContent(
    uiState: SettingsUiState,
    onBack: () -> Unit,
    onThemeSelected: (ThemePreference) -> Unit,
    onFontSizeSelected: (FontSize) -> Unit,
    onReduceAnimationsToggled: (Boolean) -> Unit,
    onDailyCaptureReminderToggled: (Boolean) -> Unit,
    onDailyCaptureTimeSelected: (LocalTime) -> Unit,
    onStreakWarningsToggled: (Boolean) -> Unit,
    onWeeklyRecapToggled: (Boolean) -> Unit,
    onAchievementCelebrationsToggled: (Boolean) -> Unit,
    onCloudBackupToggled: (Boolean) -> Unit,
    onExportDataClick: () -> Unit,
    onSignOutClick: () -> Unit,
    onDeleteAccountClick: () -> Unit,
    onClearCacheClick: () -> Unit,
    onForceCrashClick: () -> Unit,
    onViewLogsClick: () -> Unit,
    onApiEndpointSelected: (String) -> Unit,
) {
    var showDeleteAccountDialog by remember { mutableStateOf(false) }
    var showTimePickerDialog by remember { mutableStateOf(false) }

    Scaffold(topBar = { GlowTopBar(title = "Settings", onBack = onBack) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(GlowSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(GlowSpacing.lg),
        ) {
            // Account Section
            AccountSection(
                email = uiState.userEmail,
                displayName = uiState.userDisplayName,
                onSignOutClick = onSignOutClick,
                onDeleteAccountClick = { showDeleteAccountDialog = true },
                signingOut = uiState.signingOut,
            )

            // Notifications Section
            NotificationsSection(
                dailyCaptureReminder = uiState.dailyCaptureReminder,
                dailyCaptureTime = uiState.dailyCaptureTime,
                streakWarnings = uiState.streakWarnings,
                weeklyRecap = uiState.weeklyRecap,
                achievementCelebrations = uiState.achievementCelebrations,
                onDailyCaptureReminderToggled = onDailyCaptureReminderToggled,
                onDailyCaptureTimeClick = { showTimePickerDialog = true },
                onStreakWarningsToggled = onStreakWarningsToggled,
                onWeeklyRecapToggled = onWeeklyRecapToggled,
                onAchievementCelebrationsToggled = onAchievementCelebrationsToggled,
            )

            // Data & Privacy Section
            DataPrivacySection(
                cloudBackupEnabled = uiState.cloudBackupEnabled,
                onCloudBackupToggled = onCloudBackupToggled,
                onExportDataClick = onExportDataClick,
                exportInProgress = uiState.exportInProgress,
            )

            // Display Section
            DisplaySection(
                theme = uiState.theme,
                fontSize = uiState.fontSize,
                reduceAnimations = uiState.reduceAnimations,
                onThemeSelected = onThemeSelected,
                onFontSizeSelected = onFontSizeSelected,
                onReduceAnimationsToggled = onReduceAnimationsToggled,
            )

            // About Section
            AboutSection()

            // Debug Section (only in debug builds)
            if (BuildConfig.DEBUG) {
                DebugSection(
                    currentEndpoint = uiState.apiEndpoint,
                    onClearCacheClick = onClearCacheClick,
                    onForceCrashClick = onForceCrashClick,
                    onViewLogsClick = onViewLogsClick,
                    onApiEndpointSelected = onApiEndpointSelected,
                )
            }
        }
    }

    // Delete Account Confirmation Dialog
    if (showDeleteAccountDialog) {
        DeleteAccountDialog(
            onDismiss = { showDeleteAccountDialog = false },
            onConfirm = {
                showDeleteAccountDialog = false
                onDeleteAccountClick()
            },
        )
    }

    // Time Picker Dialog
    if (showTimePickerDialog) {
        TimePickerDialog(
            currentTime = uiState.dailyCaptureTime,
            onDismiss = { showTimePickerDialog = false },
            onTimeSelected = { time ->
                showTimePickerDialog = false
                onDailyCaptureTimeSelected(time)
            },
        )
    }
}

