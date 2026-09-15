package com.glowup.ai.feature.capture

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.glowup.ai.core.ui.ErrorState
import com.glowup.ai.core.ui.GlowTopBar
import com.glowup.ai.domain.model.Capture

/** Full history surface backed by the same cached/server history used by Home and Comparison. */
@Composable
fun PhotoHistoryRoute(
    onBack: () -> Unit,
    onCaptureClick: (Capture) -> Unit,
    onCompareClick: (Capture, Capture) -> Unit,
    viewModel: PhotoHistoryViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    when (val current = state) {
        PhotoHistoryUiState.Loading -> {
            Column(modifier = Modifier.fillMaxSize()) {
                GlowTopBar(title = "Your Photos", onBack = onBack)
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
        }

        is PhotoHistoryUiState.Error -> {
            Column(modifier = Modifier.fillMaxSize()) {
                GlowTopBar(title = "Your Photos", onBack = onBack)
                Box(
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    ErrorState(message = current.message, onRetry = viewModel::reload)
                }
            }
        }

        is PhotoHistoryUiState.Content -> {
            PhotoGridScreen(
                captures = current.captures,
                onCaptureClick = onCaptureClick,
                onBackClick = onBack,
                onCompareClick = onCompareClick,
            )
        }
    }
}

sealed interface PhotoHistoryUiState {
    data object Loading : PhotoHistoryUiState
    data class Content(val captures: List<Capture>) : PhotoHistoryUiState
    data class Error(val message: String) : PhotoHistoryUiState
}
