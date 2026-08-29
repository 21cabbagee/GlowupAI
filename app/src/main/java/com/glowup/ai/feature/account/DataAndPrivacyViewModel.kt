package com.glowup.ai.feature.account

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.glowup.ai.core.util.GlowResult
import com.glowup.ai.data.repository.PrivacyRepository
import com.glowup.ai.data.repository.SessionRepository
import com.glowup.ai.domain.model.ConsentState
import com.glowup.ai.feature.auth.FirebaseAuthGateway
import com.glowup.ai.feature.auth.toMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/** The exact string a user must type to enable the delete-account submit button. */
const val DELETE_CONFIRMATION_TOKEN = "DELETE"

/** Must match the first-grant consent flow so changing consent records one policy version. */
private const val PRIVACY_POLICY_VERSION = "2026-08-24"

sealed interface ExportState {
    data object Idle : ExportState
    data object Exporting : ExportState
    data class Success(val uri: Uri) : ExportState
    data class Failed(val message: String) : ExportState
}

sealed interface DeleteAccountState {
    data object Idle : DeleteAccountState
    /** Danger-zone card expanded, typed-confirmation field visible, submit disabled until it
     * matches [DELETE_CONFIRMATION_TOKEN] exactly. */
    data object Confirming : DeleteAccountState
    data object Deleting : DeleteAccountState
    data class Failed(val message: String) : DeleteAccountState
}

/** Backs [com.glowup.ai.feature.shell.GlowDestination.DataAndPrivacy]. */
data class DataAndPrivacyUiState(
    val loading: Boolean = true,
    val consentState: ConsentState = ConsentState.UNKNOWN,
    val consentUpdating: Boolean = false,
    val consentError: String? = null,
    val loadError: String? = null,
    val export: ExportState = ExportState.Idle,
    val delete: DeleteAccountState = DeleteAccountState.Idle,
    val deleteConfirmationText: String = "",
)

/**
 * Owns three flows that all sit behind `feature/account`'s two writable repositories:
 * consent (`POST /consent` via [SessionRepository], read-only reuse — this file does not edit
 * that repository), export (`GET /export` via [PrivacyRepository.exportData] +
 * [ExportFileWriter]), and account deletion (`DELETE /users/{id}` via
 * [PrivacyRepository.deleteAccount]).
 *
 * Deletion follows frontend-api-map.md trap #12 exactly: a typed `DELETE` confirmation with a
 * disabled submit until it matches, in-flight reads cancelled before the call, no success shown
 * until the `204` actually comes back, and on success clearing ONLY GlowUp's own
 * [com.glowup.ai.data.local.SessionStore] keys (never `localStorage.clear()`'s blanket wipe — see
 * [PrivacyRepository.deleteAccount] / [com.glowup.ai.data.local.SessionStore.clearSession]).
 */
@HiltViewModel
class DataAndPrivacyViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val sessionRepository: SessionRepository,
    private val privacyRepository: PrivacyRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DataAndPrivacyUiState())
    val uiState: StateFlow<DataAndPrivacyUiState> = _uiState.asStateFlow()

    private val _deleted = MutableStateFlow(false)
    /** One-shot: the screen navigates to Welcome and clears the ENTIRE back stack (not just this
     * screen) when this flips true — the server response, not the tap, is what makes this true. */
    val deleted: StateFlow<Boolean> = _deleted.asStateFlow()

    /** Tracks the in-flight profile/export reads so [confirmDelete] can cancel them before the
     * irreversible call — frontend-api-map.md trap #12: "cancel in-flight reads". */
    private var inFlightReads: Job? = null

    init {
        load()
    }

    fun load() {
        inFlightReads?.cancel()
        inFlightReads = viewModelScope.launch {
            _uiState.value = _uiState.value.copy(loading = true, loadError = null)
            val userId = sessionRepository.userIdFlow.first()
            if (userId == null) {
                _uiState.value = _uiState.value.copy(loading = false, loadError = "No active session.")
                return@launch
            }
            when (val result = sessionRepository.refreshProfile(userId)) {
                is GlowResult.Success -> _uiState.value = _uiState.value.copy(
                    loading = false,
                    consentState = result.data.user.consentState,
                    loadError = null,
                )
                is GlowResult.Failure -> _uiState.value = _uiState.value.copy(
                    loading = false,
                    loadError = result.error.toMessage(),
                )
            }
        }
    }

    fun retry() = load()

    // -- Consent --------------------------------------------------------------------------------

    /** Consent is changeable from here in both directions. Declining does not delete anything —
     * it only locks photo capture, mirroring the backend's own `require_consent` gate. */
    fun setConsent(active: Boolean) {
        if (_uiState.value.consentUpdating) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(consentUpdating = true, consentError = null)
            when (val result = sessionRepository.grantConsent(facialData = active, policyVersion = PRIVACY_POLICY_VERSION)) {
                is GlowResult.Success -> _uiState.value = _uiState.value.copy(
                    consentUpdating = false,
                    consentState = result.data.user.consentState,
                )
                is GlowResult.Failure -> _uiState.value = _uiState.value.copy(
                    consentUpdating = false,
                    consentError = result.error.toMessage(),
                )
            }
        }
    }

    // -- Export -----------------------------------------------------------------------------------

    /**
     * `GET /export`. Writes the JSON straight to a private file via [ExportFileWriter] — never
     * logged, never placed in a shared cache (frontend-api-map.md's export contract). Raw photos
     * are never in the payload; the UI is responsible for saying so.
     */
    fun exportData() {
        if (_uiState.value.export == ExportState.Exporting) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(export = ExportState.Exporting)
            val userId = sessionRepository.userIdFlow.first()
            if (userId == null) {
                _uiState.value = _uiState.value.copy(export = ExportState.Failed("No active session."))
                return@launch
            }
            when (val result = privacyRepository.exportData(userId)) {
                is GlowResult.Success -> {
                    try {
                        val uri = ExportFileWriter.write(appContext, userId, result.data)
                        _uiState.value = _uiState.value.copy(export = ExportState.Success(uri))
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (_: Exception) {
                        _uiState.value = _uiState.value.copy(
                            export = ExportState.Failed("Couldn't prepare the export. Please try again."),
                        )
                    }
                }
                is GlowResult.Failure -> _uiState.value = _uiState.value.copy(
                    export = ExportState.Failed(result.error.toMessage()),
                )
            }
        }
    }

    fun dismissExportState() {
        _uiState.value = _uiState.value.copy(export = ExportState.Idle)
    }

    fun onExportShareFailed() {
        _uiState.value = _uiState.value.copy(
            export = ExportState.Failed("Couldn't open a share or save app. Please try exporting again."),
        )
    }

    // -- Deletion ---------------------------------------------------------------------------------

    fun beginDeleteConfirmation() {
        _uiState.value = _uiState.value.copy(delete = DeleteAccountState.Confirming, deleteConfirmationText = "")
    }

    fun cancelDeleteConfirmation() {
        _uiState.value = _uiState.value.copy(delete = DeleteAccountState.Idle, deleteConfirmationText = "")
    }

    fun updateDeleteConfirmationText(text: String) {
        _uiState.value = _uiState.value.copy(deleteConfirmationText = text)
    }

    /**
     * `DELETE /api/users/{id}`. Only callable once [DataAndPrivacyUiState.deleteConfirmationText]
     * exactly matches [DELETE_CONFIRMATION_TOKEN] — the screen is expected to keep the submit
     * button disabled until then, this is the second guard. `204` means success with no body:
     * [PrivacyRepository.deleteAccount] never attempts to parse one. [deleted] flips only after
     * that response arrives — never optimistically.
     */
    fun confirmDelete() {
        val state = _uiState.value
        if (state.deleteConfirmationText != DELETE_CONFIRMATION_TOKEN) return
        if (state.delete == DeleteAccountState.Deleting) return

        // Cancel in-flight reads (profile refresh, export) before the irreversible call.
        inFlightReads?.cancel()

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(delete = DeleteAccountState.Deleting)
            val userId = sessionRepository.userIdFlow.first()
            if (userId == null) {
                _uiState.value = _uiState.value.copy(delete = DeleteAccountState.Failed("No active session."))
                return@launch
            }
            when (val result = privacyRepository.deleteAccount(userId)) {
                is GlowResult.Success -> {
                    // PrivacyRepository.deleteAccount already called SessionStore.clearSession()
                    // on success — GlowUp keys only, never a blanket wipe.
                    FirebaseAuthGateway.signOut()
                    _deleted.value = true
                }
                is GlowResult.Failure -> _uiState.value = _uiState.value.copy(
                    delete = DeleteAccountState.Failed(result.error.toMessage()),
                )
            }
        }
    }
}
