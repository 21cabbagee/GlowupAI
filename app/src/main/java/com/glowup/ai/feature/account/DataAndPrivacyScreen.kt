package com.glowup.ai.feature.account

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.glowup.ai.core.design.GlowSpacing
import com.glowup.ai.core.design.LocalGlowColors
import com.glowup.ai.core.ui.DisclaimerNote
import com.glowup.ai.core.ui.ErrorState
import com.glowup.ai.core.ui.GlowButton
import com.glowup.ai.core.ui.GlowButtonVariant
import com.glowup.ai.core.ui.GlowCard
import com.glowup.ai.core.ui.GlowTextField
import com.glowup.ai.core.ui.GlowTopBar
import com.glowup.ai.domain.model.ConsentState

/**
 * [com.glowup.ai.feature.shell.GlowDestination.DataAndPrivacy]: consent management, `GET /export`
 * to a shareable file, and the typed-`DELETE` account-deletion flow. See
 * [DataAndPrivacyViewModel]'s class doc for how each of the three maps to
 * frontend-api-map.md's traps.
 */
@Composable
fun DataAndPrivacyRoute(
    navController: NavController,
    viewModel: DataAndPrivacyViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val deleted by viewModel.deleted.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(deleted) {
        if (deleted) navController.routeToWelcomeAfterSessionEnd()
    }

    LaunchedEffect(uiState.export) {
        val export = uiState.export
        if (export is ExportState.Success) {
            runCatching { launchShareSheet(context, export.uri) }
                .onSuccess { viewModel.dismissExportState() }
                .onFailure { viewModel.onExportShareFailed() }
        }
    }

    DataAndPrivacyContent(
        uiState = uiState,
        onBack = { navController.popBackStack() },
        onRetry = viewModel::retry,
        onSetConsent = viewModel::setConsent,
        onExportClick = viewModel::exportData,
        onDismissExportError = viewModel::dismissExportState,
        onBeginDelete = viewModel::beginDeleteConfirmation,
        onCancelDelete = viewModel::cancelDeleteConfirmation,
        onDeleteTextChange = viewModel::updateDeleteConfirmationText,
        onConfirmDelete = viewModel::confirmDelete,
    )
}

private fun launchShareSheet(context: Context, uri: android.net.Uri) {
    context.startActivity(ExportFileWriter.shareIntent(uri))
}

@Composable
private fun DataAndPrivacyContent(
    uiState: DataAndPrivacyUiState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onSetConsent: (Boolean) -> Unit,
    onExportClick: () -> Unit,
    onDismissExportError: () -> Unit,
    onBeginDelete: () -> Unit,
    onCancelDelete: () -> Unit,
    onDeleteTextChange: (String) -> Unit,
    onConfirmDelete: () -> Unit,
) {
    Scaffold(topBar = { GlowTopBar(title = "Data & Privacy", onBack = onBack) }) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when {
                uiState.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Loading your privacy settings…", style = MaterialTheme.typography.bodyMedium)
                }
                uiState.loadError != null -> Box(Modifier.fillMaxSize().padding(GlowSpacing.lg), contentAlignment = Alignment.Center) {
                    ErrorState(message = uiState.loadError, onRetry = onRetry)
                }
                else -> Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(GlowSpacing.lg),
                    verticalArrangement = Arrangement.spacedBy(GlowSpacing.lg),
                ) {
                    ConsentCard(
                        consentState = uiState.consentState,
                        updating = uiState.consentUpdating,
                        error = uiState.consentError,
                        onSetConsent = onSetConsent,
                    )
                    ExportCard(exportState = uiState.export, onExportClick = onExportClick, onDismissError = onDismissExportError)
                    DangerZoneCard(
                        deleteState = uiState.delete,
                        confirmationText = uiState.deleteConfirmationText,
                        onBeginDelete = onBeginDelete,
                        onCancelDelete = onCancelDelete,
                        onDeleteTextChange = onDeleteTextChange,
                        onConfirmDelete = onConfirmDelete,
                    )
                }
            }
        }
    }
}

@Composable
private fun ConsentCard(
    consentState: ConsentState,
    updating: Boolean,
    error: String?,
    onSetConsent: (Boolean) -> Unit,
) {
    val glow = LocalGlowColors.current
    val isActive = consentState == ConsentState.ACTIVE
    GlowCard(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Facial-photo consent",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = glow.ink900,
        )
        androidx.compose.foundation.layout.Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = GlowSpacing.sm)
                .semantics { contentDescription = "Facial photo consent, ${if (isActive) "on" else "off"}" },
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(
                    text = if (isActive) "Consent active" else "Consent not active",
                    style = MaterialTheme.typography.bodyMedium,
                    color = glow.ink900,
                )
                Text(
                    text = "Required for photo capture. Declining locks capture but keeps the " +
                        "rest of your account usable.",
                    style = MaterialTheme.typography.bodySmall,
                    color = glow.ink600,
                )
            }
            Switch(
                checked = isActive,
                enabled = !updating,
                onCheckedChange = onSetConsent,
                colors = SwitchDefaults.colors(checkedThumbColor = glow.honey600, checkedTrackColor = glow.honey300),
            )
        }
        if (error != null) {
            Text(
                text = error,
                style = MaterialTheme.typography.bodySmall,
                color = glow.danger,
                modifier = Modifier.padding(top = GlowSpacing.sm),
            )
        }
    }
}

@Composable
private fun ExportCard(
    exportState: ExportState,
    onExportClick: () -> Unit,
    onDismissError: () -> Unit,
) {
    val glow = LocalGlowColors.current
    GlowCard(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Export your data",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = glow.ink900,
        )
        Text(
            text = "Downloads a JSON file of your profile, consent history, routine events, " +
                "experiments, captures and their metrics, verdicts, Q&A (Premium only), and " +
                "engagement history.",
            style = MaterialTheme.typography.bodySmall,
            color = glow.ink600,
            modifier = Modifier.padding(top = GlowSpacing.xs),
        )
        DisclaimerNote(
            modifier = Modifier.fillMaxWidth().padding(top = GlowSpacing.sm),
            text = "Raw photo bytes are NOT included in this export — only the measurements " +
                "and metadata derived from them.",
        )
        if (exportState is ExportState.Failed) {
            Text(
                text = exportState.message,
                style = MaterialTheme.typography.bodySmall,
                color = glow.danger,
                modifier = Modifier.padding(top = GlowSpacing.sm).semantics {
                    liveRegion = LiveRegionMode.Assertive
                },
            )
        }
        GlowButton(
            modifier = Modifier.fillMaxWidth().padding(top = GlowSpacing.md),
            text = if (exportState == ExportState.Exporting) "Preparing export…" else "Export my data",
            onClick = {
                if (exportState is ExportState.Failed) onDismissError()
                onExportClick()
            },
            enabled = exportState != ExportState.Exporting,
            loading = exportState == ExportState.Exporting,
        )
    }
}

@Composable
private fun DangerZoneCard(
    deleteState: DeleteAccountState,
    confirmationText: String,
    onBeginDelete: () -> Unit,
    onCancelDelete: () -> Unit,
    onDeleteTextChange: (String) -> Unit,
    onConfirmDelete: () -> Unit,
) {
    val glow = LocalGlowColors.current
    GlowCard(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Delete account",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = glow.danger,
        )
        Text(
            text = "This permanently deletes your GlowUp AI account, all captures, photos, " +
                "routine history, experiments, and Q&A. This cannot be undone.",
            style = MaterialTheme.typography.bodySmall,
            color = glow.ink600,
            modifier = Modifier.padding(top = GlowSpacing.xs),
        )

        when (deleteState) {
            DeleteAccountState.Idle -> GlowButton(
                modifier = Modifier.fillMaxWidth().padding(top = GlowSpacing.md),
                text = "Delete my account",
                onClick = onBeginDelete,
                variant = GlowButtonVariant.Danger,
            )
            else -> Column(modifier = Modifier.fillMaxWidth().padding(top = GlowSpacing.md)) {
                Text(
                    text = "Type DELETE to confirm. This is your final warning — nothing can " +
                        "reverse this action once you submit it.",
                    style = MaterialTheme.typography.bodySmall,
                    color = glow.danger,
                    fontWeight = FontWeight.SemiBold,
                )
                GlowTextField(
                    modifier = Modifier.fillMaxWidth().padding(top = GlowSpacing.sm),
                    value = confirmationText,
                    onValueChange = onDeleteTextChange,
                    label = "Type DELETE",
                    enabled = deleteState != DeleteAccountState.Deleting,
                    errorText = (deleteState as? DeleteAccountState.Failed)?.message,
                )
                androidx.compose.foundation.layout.Row(
                    modifier = Modifier.fillMaxWidth().padding(top = GlowSpacing.sm),
                    horizontalArrangement = Arrangement.spacedBy(GlowSpacing.sm),
                ) {
                    GlowButton(
                        modifier = Modifier.weight(1f),
                        text = "Cancel",
                        onClick = onCancelDelete,
                        variant = GlowButtonVariant.Ghost,
                        enabled = deleteState != DeleteAccountState.Deleting,
                    )
                    GlowButton(
                        modifier = Modifier.weight(1f),
                        text = "Delete forever",
                        onClick = onConfirmDelete,
                        variant = GlowButtonVariant.Danger,
                        enabled = confirmationText == DELETE_CONFIRMATION_TOKEN && deleteState != DeleteAccountState.Deleting,
                        loading = deleteState == DeleteAccountState.Deleting,
                    )
                }
            }
        }
    }
}
