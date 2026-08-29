package com.glowup.ai.feature.routine

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.glowup.ai.core.util.GlowResult
import com.glowup.ai.data.local.SessionStore
import com.glowup.ai.data.repository.RoutineRepository
import com.glowup.ai.domain.model.JobStatus
import com.glowup.ai.domain.model.ProductCreateRequest
import com.glowup.ai.domain.model.ShelfScanSelection
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val POLL_INTERVAL_MS = 1500L

/**
 * Owns [com.glowup.ai.feature.shell.GlowDestination.ShelfScan]: submit a shelf photo, poll the
 * async job every ~1.5s, present editable candidate checkboxes, confirm only the checked ones —
 * and, since `candidates` comes back empty with an explanatory `message` whenever the vision
 * provider is unconfigured server-side (the likely state in production today per
 * ANDROID_PLAN.md), always offer a manual "add product" fallback in that case.
 */
@HiltViewModel
class ShelfScanViewModel @Inject constructor(
    private val routineRepository: RoutineRepository,
    private val sessionStore: SessionStore,
) : ViewModel() {

    private val _uiState = MutableStateFlow<ShelfScanUiState>(ShelfScanUiState.Idle)
    val uiState: StateFlow<ShelfScanUiState> = _uiState.asStateFlow()

    private var pollJob: Job? = null

    fun submitPhoto(imageBase64: String) {
        if (_uiState.value !is ShelfScanUiState.Idle && _uiState.value !is ShelfScanUiState.Error) return
        pollJob?.cancel()
        pollJob = viewModelScope.launch {
            _uiState.value = ShelfScanUiState.Uploading
            val userId = sessionStore.userId()
            if (userId == null) {
                _uiState.value = ShelfScanUiState.Error("Sign in to scan a shelf photo.")
                return@launch
            }
            when (val submitResult = routineRepository.submitShelfScan(userId, imageBase64)) {
                is GlowResult.Success -> pollUntilDone(userId, submitResult.data)
                is GlowResult.Failure -> _uiState.value = ShelfScanUiState.Error(submitResult.error.toDisplayMessage())
            }
        }
    }

    private suspend fun pollUntilDone(userId: String, jobId: String) {
        _uiState.value = ShelfScanUiState.Polling("Reading your shelf photo…")
        while (true) {
            when (val result = routineRepository.getShelfScanStatus(userId, jobId)) {
                is GlowResult.Success -> {
                    val job = result.data
                    when (job.status) {
                        JobStatus.COMPLETED -> {
                            val candidates = job.result?.candidates.orEmpty()
                            _uiState.value = ShelfScanUiState.Ready(
                                jobId = jobId,
                                candidates = candidates.map {
                                    ShelfScanCandidateUi(it.name, it.category ?: "other", it.ingredients, checked = true)
                                },
                                message = job.result?.message,
                                showManualAdd = candidates.isEmpty(),
                            )
                            return
                        }
                        JobStatus.FAILED -> {
                            _uiState.value = ShelfScanUiState.Error(job.error ?: "Shelf scan failed. Try another photo.")
                            return
                        }
                        JobStatus.QUEUED, JobStatus.RUNNING, JobStatus.UNKNOWN -> {
                            _uiState.value = ShelfScanUiState.Polling("Reading your shelf photo…")
                        }
                    }
                }
                is GlowResult.Failure -> {
                    _uiState.value = ShelfScanUiState.Error(result.error.toDisplayMessage())
                    return
                }
            }
            delay(POLL_INTERVAL_MS)
        }
    }

    fun toggleCandidate(index: Int, checked: Boolean) {
        val current = _uiState.value as? ShelfScanUiState.Ready ?: return
        if (index !in current.candidates.indices) return
        val updated = current.candidates.toMutableList().also {
            it[index] = it[index].copy(checked = checked)
        }
        _uiState.value = current.copy(candidates = updated)
    }

    fun editCandidateName(index: Int, name: String) {
        val current = _uiState.value as? ShelfScanUiState.Ready ?: return
        if (index !in current.candidates.indices) return
        val updated = current.candidates.toMutableList().also { it[index] = it[index].copy(name = name) }
        _uiState.value = current.copy(candidates = updated)
    }

    fun editCandidateCategory(index: Int, category: String) {
        val current = _uiState.value as? ShelfScanUiState.Ready ?: return
        if (index !in current.candidates.indices) return
        val updated = current.candidates.toMutableList().also { it[index] = it[index].copy(category = category) }
        _uiState.value = current.copy(candidates = updated)
    }

    fun confirmSelected() {
        val current = _uiState.value as? ShelfScanUiState.Ready ?: return
        if (current.confirming) return
        val selected = current.candidates.filter { it.checked && it.name.isNotBlank() }
        if (selected.isEmpty()) return
        viewModelScope.launch {
            val userId = sessionStore.userId() ?: return@launch
            _uiState.value = current.copy(confirming = true, confirmError = null)
            val selections = selected.map { ShelfScanSelection(name = it.name.trim(), category = it.category.ifBlank { "other" }, ingredients = it.ingredients) }
            when (val result = routineRepository.confirmShelfScan(userId, current.jobId, selections)) {
                is GlowResult.Success -> _uiState.value = ShelfScanUiState.Done(result.data)
                is GlowResult.Failure -> _uiState.value = current.copy(confirming = false, confirmError = result.error.toDisplayMessage())
            }
        }
    }

    fun openManualAdd() {
        val current = _uiState.value as? ShelfScanUiState.Ready ?: return
        _uiState.value = current.copy(showManualAdd = true, manualAddError = null)
    }

    fun dismissManualAdd() {
        val current = _uiState.value as? ShelfScanUiState.Ready ?: return
        _uiState.value = current.copy(showManualAdd = false)
    }

    /** Independent of the job — the manual fallback is a plain `POST /api/products`, guarded the
     * same way [RoutineRepository.createProduct] guards every other caller (global, non-idempotent
     * rows — frontend-api-map.md trap #7). */
    fun submitManualAdd(request: ProductCreateRequest) {
        val current = _uiState.value as? ShelfScanUiState.Ready ?: return
        if (current.manualAddPending) return
        viewModelScope.launch {
            _uiState.value = current.copy(manualAddPending = true, manualAddError = null)
            when (val result = routineRepository.createProduct(request)) {
                is GlowResult.Success -> _uiState.value = ShelfScanUiState.Done(listOf(result.data))
                is GlowResult.Failure -> _uiState.value = current.copy(manualAddPending = false, manualAddError = result.error.toDisplayMessage())
            }
        }
    }

    fun reset() {
        pollJob?.cancel()
        _uiState.value = ShelfScanUiState.Idle
    }
}
