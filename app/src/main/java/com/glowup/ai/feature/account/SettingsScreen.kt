package com.glowup.ai.feature.account

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Divider
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.glowup.ai.BuildConfig
import com.glowup.ai.core.design.GlowSpacing
import com.glowup.ai.core.design.LocalGlowColors
import com.glowup.ai.core.ui.GlowButton
import com.glowup.ai.core.ui.GlowButtonVariant
import com.glowup.ai.core.ui.GlowCard
import com.glowup.ai.core.ui.GlowTopBar
import com.glowup.ai.core.ui.SectionHeader
import java.time.LocalTime
import java.time.format.DateTimeFormatter

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
    val context = LocalContext.current
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

@Composable
private fun AccountSection(
    email: String?,
    displayName: String?,
    onSignOutClick: () -> Unit,
    onDeleteAccountClick: () -> Unit,
    signingOut: Boolean,
) {
    val glow = LocalGlowColors.current
    GlowCard(modifier = Modifier.fillMaxWidth()) {
        SectionHeader(title = "Account")

        Spacer(modifier = Modifier.height(GlowSpacing.md))

        // Profile Display
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(GlowSpacing.md),
        ) {
            // Profile Photo Placeholder
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(glow.honey300),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.AccountCircle,
                    contentDescription = "Profile photo",
                    modifier = Modifier.size(48.dp),
                    tint = glow.ink900,
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = displayName ?: "User",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = glow.ink900,
                )
                Text(
                    text = email ?: "Not available",
                    style = MaterialTheme.typography.bodyMedium,
                    color = glow.ink600,
                )
            }
        }

        Spacer(modifier = Modifier.height(GlowSpacing.lg))

        // Sign Out Button
        GlowButton(
            modifier = Modifier.fillMaxWidth(),
            text = "Sign out",
            onClick = onSignOutClick,
            variant = GlowButtonVariant.Secondary,
            loading = signingOut,
        )

        Spacer(modifier = Modifier.height(GlowSpacing.sm))

        // Delete Account Button
        GlowButton(
            modifier = Modifier.fillMaxWidth(),
            text = "Delete account",
            onClick = onDeleteAccountClick,
            variant = GlowButtonVariant.Danger,
            enabled = !signingOut,
        )
    }
}

@Composable
private fun NotificationsSection(
    dailyCaptureReminder: Boolean,
    dailyCaptureTime: LocalTime,
    streakWarnings: Boolean,
    weeklyRecap: Boolean,
    achievementCelebrations: Boolean,
    onDailyCaptureReminderToggled: (Boolean) -> Unit,
    onDailyCaptureTimeClick: () -> Unit,
    onStreakWarningsToggled: (Boolean) -> Unit,
    onWeeklyRecapToggled: (Boolean) -> Unit,
    onAchievementCelebrationsToggled: (Boolean) -> Unit,
) {
    val glow = LocalGlowColors.current
    val timeFormatter = DateTimeFormatter.ofPattern("h:mm a")

    GlowCard(modifier = Modifier.fillMaxWidth()) {
        SectionHeader(title = "Notifications")

        Spacer(modifier = Modifier.height(GlowSpacing.md))

        // Daily Capture Reminder
        SettingsToggleRow(
            title = "Daily capture reminder",
            description = "Get notified when it's time for your next photo",
            checked = dailyCaptureReminder,
            onCheckedChange = onDailyCaptureReminderToggled,
        )

        // Time Picker (only shown when reminder is enabled)
        if (dailyCaptureReminder) {
            Spacer(modifier = Modifier.height(GlowSpacing.sm))
            SettingsClickableRow(
                title = "Reminder time",
                value = dailyCaptureTime.format(timeFormatter),
                onClick = onDailyCaptureTimeClick,
            )
        }

        HorizontalDivider(
            modifier = Modifier.padding(vertical = GlowSpacing.md),
            color = glow.ink600.copy(alpha = 0.12f),
        )

        // Streak Warnings
        SettingsToggleRow(
            title = "Streak warnings",
            description = "Be notified when you're about to lose your streak",
            checked = streakWarnings,
            onCheckedChange = onStreakWarningsToggled,
        )

        HorizontalDivider(
            modifier = Modifier.padding(vertical = GlowSpacing.md),
            color = glow.ink600.copy(alpha = 0.12f),
        )

        // Weekly Recap
        SettingsToggleRow(
            title = "Weekly recap",
            description = "Receive a summary of your progress each week",
            checked = weeklyRecap,
            onCheckedChange = onWeeklyRecapToggled,
        )

        HorizontalDivider(
            modifier = Modifier.padding(vertical = GlowSpacing.md),
            color = glow.ink600.copy(alpha = 0.12f),
        )

        // Achievement Celebrations
        SettingsToggleRow(
            title = "Achievement celebrations",
            description = "Get notified when you unlock new achievements",
            checked = achievementCelebrations,
            onCheckedChange = onAchievementCelebrationsToggled,
        )
    }
}

@Composable
private fun DataPrivacySection(
    cloudBackupEnabled: Boolean,
    onCloudBackupToggled: (Boolean) -> Unit,
    onExportDataClick: () -> Unit,
    exportInProgress: Boolean,
) {
    val glow = LocalGlowColors.current
    val context = LocalContext.current

    GlowCard(modifier = Modifier.fillMaxWidth()) {
        SectionHeader(title = "Data & Privacy")

        Spacer(modifier = Modifier.height(GlowSpacing.md))

        // Cloud Backup Toggle
        SettingsToggleRow(
            title = "Cloud backup",
            description = "Automatically back up your data to the cloud",
            checked = cloudBackupEnabled,
            onCheckedChange = onCloudBackupToggled,
        )

        HorizontalDivider(
            modifier = Modifier.padding(vertical = GlowSpacing.md),
            color = glow.ink600.copy(alpha = 0.12f),
        )

        // Export All Data
        SettingsClickableRow(
            title = "Export all data",
            description = "Download your data as a JSON file",
            onClick = onExportDataClick,
            loading = exportInProgress,
        )

        HorizontalDivider(
            modifier = Modifier.padding(vertical = GlowSpacing.md),
            color = glow.ink600.copy(alpha = 0.12f),
        )

        // Privacy Policy Link
        SettingsClickableRow(
            title = "Privacy policy",
            onClick = {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://glowup.ai/privacy"))
                context.startActivity(intent)
            },
            showArrow = true,
        )

        HorizontalDivider(
            modifier = Modifier.padding(vertical = GlowSpacing.md),
            color = glow.ink600.copy(alpha = 0.12f),
        )

        // Terms of Service Link
        SettingsClickableRow(
            title = "Terms of service",
            onClick = {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://glowup.ai/terms"))
                context.startActivity(intent)
            },
            showArrow = true,
        )
    }
}

@Composable
private fun DisplaySection(
    theme: ThemePreference,
    fontSize: FontSize,
    reduceAnimations: Boolean,
    onThemeSelected: (ThemePreference) -> Unit,
    onFontSizeSelected: (FontSize) -> Unit,
    onReduceAnimationsToggled: (Boolean) -> Unit,
) {
    val glow = LocalGlowColors.current

    GlowCard(modifier = Modifier.fillMaxWidth()) {
        SectionHeader(title = "Display")

        Spacer(modifier = Modifier.height(GlowSpacing.md))

        // Dark Mode
        Text(
            text = "Theme",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = glow.ink900,
        )

        Spacer(modifier = Modifier.height(GlowSpacing.sm))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(GlowSpacing.sm),
        ) {
            ThemePreference.entries.forEach { option ->
                GlowButton(
                    modifier = Modifier.weight(1f),
                    text = option.label,
                    onClick = { onThemeSelected(option) },
                    variant = if (option == theme) GlowButtonVariant.Primary else GlowButtonVariant.Secondary,
                )
            }
        }

        HorizontalDivider(
            modifier = Modifier.padding(vertical = GlowSpacing.md),
            color = glow.ink600.copy(alpha = 0.12f),
        )

        // Font Size
        Text(
            text = "Font size",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = glow.ink900,
        )

        Spacer(modifier = Modifier.height(GlowSpacing.sm))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(GlowSpacing.sm),
        ) {
            FontSize.entries.forEach { size ->
                GlowButton(
                    modifier = Modifier.weight(1f),
                    text = size.label,
                    onClick = { onFontSizeSelected(size) },
                    variant = if (size == fontSize) GlowButtonVariant.Primary else GlowButtonVariant.Secondary,
                )
            }
        }

        HorizontalDivider(
            modifier = Modifier.padding(vertical = GlowSpacing.md),
            color = glow.ink600.copy(alpha = 0.12f),
        )

        // Reduce Animations
        SettingsToggleRow(
            title = "Reduce animations",
            description = "Minimize motion throughout the app",
            checked = reduceAnimations,
            onCheckedChange = onReduceAnimationsToggled,
        )
    }
}

@Composable
private fun AboutSection() {
    val glow = LocalGlowColors.current
    val context = LocalContext.current

    GlowCard(modifier = Modifier.fillMaxWidth()) {
        SectionHeader(title = "About")

        Spacer(modifier = Modifier.height(GlowSpacing.md))

        // App Version
        SettingsInfoRow(
            title = "App version",
            value = BuildConfig.VERSION_NAME,
        )

        HorizontalDivider(
            modifier = Modifier.padding(vertical = GlowSpacing.md),
            color = glow.ink600.copy(alpha = 0.12f),
        )

        // Build Number
        SettingsInfoRow(
            title = "Build number",
            value = BuildConfig.VERSION_CODE.toString(),
        )

        HorizontalDivider(
            modifier = Modifier.padding(vertical = GlowSpacing.md),
            color = glow.ink600.copy(alpha = 0.12f),
        )

        // Open Source Licenses
        SettingsClickableRow(
            title = "Open source licenses",
            onClick = {
                // TODO: Navigate to licenses screen or open system licenses
            },
            showArrow = true,
        )

        HorizontalDivider(
            modifier = Modifier.padding(vertical = GlowSpacing.md),
            color = glow.ink600.copy(alpha = 0.12f),
        )

        // Rate App
        SettingsClickableRow(
            title = "Rate app",
            onClick = {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=com.glowup.ai"))
                context.startActivity(intent)
            },
            showArrow = true,
        )

        HorizontalDivider(
            modifier = Modifier.padding(vertical = GlowSpacing.md),
            color = glow.ink600.copy(alpha = 0.12f),
        )

        // Send Feedback
        SettingsClickableRow(
            title = "Send feedback",
            onClick = {
                val intent = Intent(Intent.ACTION_SENDTO).apply {
                    data = Uri.parse("mailto:support@glowup.ai")
                    putExtra(Intent.EXTRA_SUBJECT, "GlowUp AI Feedback")
                }
                context.startActivity(intent)
            },
            showArrow = true,
        )

        HorizontalDivider(
            modifier = Modifier.padding(vertical = GlowSpacing.md),
            color = glow.ink600.copy(alpha = 0.12f),
        )

        // Help Center
        SettingsClickableRow(
            title = "Help center",
            onClick = {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://help.glowup.ai"))
                context.startActivity(intent)
            },
            showArrow = true,
        )
    }
}

@Composable
private fun DebugSection(
    currentEndpoint: String,
    onClearCacheClick: () -> Unit,
    onForceCrashClick: () -> Unit,
    onViewLogsClick: () -> Unit,
    onApiEndpointSelected: (String) -> Unit,
) {
    val glow = LocalGlowColors.current

    GlowCard(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(GlowSpacing.sm),
        ) {
            SectionHeader(title = "Debug")
            Box(
                modifier = Modifier
                    .background(glow.danger.copy(alpha = 0.12f), shape = MaterialTheme.shapes.small)
                    .padding(horizontal = GlowSpacing.xs, vertical = 2.dp),
            ) {
                Text(
                    text = "DEV ONLY",
                    style = MaterialTheme.typography.labelSmall,
                    color = glow.danger,
                    fontWeight = FontWeight.Bold,
                )
            }
        }

        Spacer(modifier = Modifier.height(GlowSpacing.md))

        // API Endpoint Switcher
        Text(
            text = "API Endpoint",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = glow.ink900,
        )

        Spacer(modifier = Modifier.height(GlowSpacing.sm))

        Column(verticalArrangement = Arrangement.spacedBy(GlowSpacing.xs)) {
            listOf("Local", "Staging", "Production").forEach { endpoint ->
                GlowButton(
                    modifier = Modifier.fillMaxWidth(),
                    text = endpoint,
                    onClick = { onApiEndpointSelected(endpoint) },
                    variant = if (currentEndpoint == endpoint) GlowButtonVariant.Primary else GlowButtonVariant.Secondary,
                )
            }
        }

        HorizontalDivider(
            modifier = Modifier.padding(vertical = GlowSpacing.md),
            color = glow.ink600.copy(alpha = 0.12f),
        )

        // Clear Cache
        SettingsClickableRow(
            title = "Clear cache",
            description = "Remove all cached data",
            onClick = onClearCacheClick,
        )

        HorizontalDivider(
            modifier = Modifier.padding(vertical = GlowSpacing.md),
            color = glow.ink600.copy(alpha = 0.12f),
        )

        // View Logs
        SettingsClickableRow(
            title = "View logs",
            description = "Open log viewer",
            onClick = onViewLogsClick,
        )

        HorizontalDivider(
            modifier = Modifier.padding(vertical = GlowSpacing.md),
            color = glow.ink600.copy(alpha = 0.12f),
        )

        // Force Crash
        SettingsClickableRow(
            title = "Force crash",
            description = "Test Crashlytics integration",
            onClick = onForceCrashClick,
        )
    }
}

// Helper Components

@Composable
private fun SettingsToggleRow(
    title: String,
    description: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    val glow = LocalGlowColors.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = "$title, ${if (checked) "on" else "off"}" },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = glow.ink900,
            )
            if (description != null) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = glow.ink600,
                )
            }
        }

        Spacer(modifier = Modifier.width(GlowSpacing.md))

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = glow.honey600,
                checkedTrackColor = glow.honey300,
            ),
        )
    }
}

@Composable
private fun SettingsClickableRow(
    title: String,
    description: String? = null,
    value: String? = null,
    onClick: () -> Unit,
    showArrow: Boolean = false,
    loading: Boolean = false,
) {
    val glow = LocalGlowColors.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick, enabled = !loading)
            .semantics { contentDescription = title },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = glow.ink900,
            )
            if (description != null) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = glow.ink600,
                )
            }
        }

        Spacer(modifier = Modifier.width(GlowSpacing.md))

        if (value != null) {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = glow.ink600,
            )
        }

        if (showArrow) {
            Icon(
                imageVector = Icons.Filled.ArrowForward,
                contentDescription = null,
                tint = glow.ink600,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun SettingsInfoRow(
    title: String,
    value: String,
) {
    val glow = LocalGlowColors.current

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = glow.ink900,
        )

        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = glow.ink600,
        )
    }
}

@Composable
private fun DeleteAccountDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete account?") },
        text = {
            Text(
                "This will permanently delete your account and all associated data. " +
                    "This action cannot be undone."
            )
        },
        confirmButton = {
            GlowButton(
                text = "Delete account",
                onClick = onConfirm,
                variant = GlowButtonVariant.Danger,
            )
        },
        dismissButton = {
            GlowButton(
                text = "Cancel",
                onClick = onDismiss,
                variant = GlowButtonVariant.Ghost,
            )
        },
    )
}

@Composable
private fun TimePickerDialog(
    currentTime: LocalTime,
    onDismiss: () -> Unit,
    onTimeSelected: (LocalTime) -> Unit,
) {
    val glow = LocalGlowColors.current
    var selectedHour by remember { mutableStateOf(currentTime.hour) }
    var selectedMinute by remember { mutableStateOf(currentTime.minute) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Set reminder time") },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(GlowSpacing.md),
            ) {
                Text(
                    text = "Choose when you'd like to receive daily capture reminders",
                    style = MaterialTheme.typography.bodyMedium,
                    color = glow.ink600,
                )

                // Simple time display - in a real implementation, you'd use a proper time picker
                Row(
                    horizontalArrangement = Arrangement.spacedBy(GlowSpacing.sm),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = String.format("%02d:%02d", selectedHour, selectedMinute),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = glow.ink900,
                    )
                }

                Text(
                    text = "Note: Use your device's system settings to pick a specific time",
                    style = MaterialTheme.typography.bodySmall,
                    color = glow.ink600,
                )
            }
        },
        confirmButton = {
            GlowButton(
                text = "Set time",
                onClick = {
                    onTimeSelected(LocalTime.of(selectedHour, selectedMinute))
                },
                variant = GlowButtonVariant.Primary,
            )
        },
        dismissButton = {
            GlowButton(
                text = "Cancel",
                onClick = onDismiss,
                variant = GlowButtonVariant.Ghost,
            )
        },
    )
}
