package com.glowup.ai.feature.insights

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.glowup.ai.core.design.GlowSpacing
import com.glowup.ai.core.design.LocalGlowColors
import com.glowup.ai.core.ui.DisclaimerNote
import com.glowup.ai.core.ui.ErrorState
import com.glowup.ai.core.ui.GlowButton
import com.glowup.ai.core.ui.GlowButtonVariant
import com.glowup.ai.core.ui.GlowTextField
import com.glowup.ai.core.ui.GlowTopBar
import com.glowup.ai.core.ui.LockedCard
import com.glowup.ai.core.ui.ShimmerSkeleton
import com.glowup.ai.feature.insights.components.ChatBubble

private const val QNA_DISCLAIMER =
    "Answers are grounded in your cosmetic tracking history, not a diagnosis. " +
        "GlowUp AI never replaces a dermatologist."

@Composable
fun QnaScreen(
    onBack: () -> Unit,
    onUpgrade: () -> Unit,
    viewModel: QnaViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = { GlowTopBar(title = "Ask about your skin", onBack = onBack) },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (val current = state) {
                QnaUiState.Loading -> QnaLoading()
                QnaUiState.Locked -> Box(modifier = Modifier.padding(GlowSpacing.md)) {
                    LockedCard(
                        title = "Data Q&A is Premium",
                        body = "Ask questions about your own tracked history and get cited, grounded answers.",
                        onUnlock = onUpgrade,
                    )
                }
                is QnaUiState.Error -> Box(modifier = Modifier.padding(GlowSpacing.md)) {
                    ErrorState(message = current.message, onRetry = viewModel::load)
                }
                is QnaUiState.Content -> QnaContent(current, viewModel)
            }
        }
    }
}

@Composable
private fun QnaLoading() {
    Column(modifier = Modifier.fillMaxSize().padding(GlowSpacing.md)) {
        ShimmerSkeleton(height = 48.dp, modifier = Modifier.padding(bottom = GlowSpacing.sm))
        ShimmerSkeleton(height = 48.dp, modifier = Modifier.padding(bottom = GlowSpacing.sm))
        ShimmerSkeleton(height = 80.dp)
    }
}

@Composable
private fun QnaContent(state: QnaUiState.Content, viewModel: QnaViewModel) {
    val glow = LocalGlowColors.current
    val listState = rememberLazyListState()

    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) listState.animateScrollToItem(state.messages.size - 1)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        if (state.messages.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(GlowSpacing.lg),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("Ask anything about your tracked history — e.g. \"Why did redness change?\"", color = glow.ink600)
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(GlowSpacing.md),
                verticalArrangement = Arrangement.spacedBy(GlowSpacing.sm),
            ) {
                items(state.messages, key = { it.id }) { message ->
                    ChatBubble(
                        isUser = message.role == "user",
                        text = message.content,
                        pending = message.pending,
                        isSafetyHandoff = message.isSafetyHandoff,
                        isError = message.isError,
                        citations = message.citations,
                    )
                }
            }
        }

        DisclaimerNote(
            modifier = Modifier.padding(horizontal = GlowSpacing.md),
            text = QNA_DISCLAIMER,
        )

        if (state.threadBlocked) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(GlowSpacing.md),
                horizontalArrangement = Arrangement.Center,
            ) {
                GlowButton(
                    text = "Start a new conversation",
                    onClick = viewModel::startNewConversation,
                    variant = GlowButtonVariant.Secondary,
                )
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding()
                    .padding(GlowSpacing.md),
                verticalAlignment = Alignment.Bottom,
            ) {
                GlowTextField(
                    modifier = Modifier.weight(1f),
                    value = state.input,
                    onValueChange = viewModel::onInputChange,
                    label = "Question",
                    placeholder = "Ask about your history…",
                    enabled = !state.sending,
                    supportingText = if (state.sending) "Sending…" else null,
                )
                GlowButton(
                    modifier = Modifier.padding(start = GlowSpacing.sm),
                    text = "Send",
                    onClick = viewModel::send,
                    enabled = state.input.isNotBlank() && !state.sending,
                    loading = state.sending,
                )
            }
        }
    }
}
