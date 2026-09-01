package com.glowup.ai.feature.insights

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.glowup.ai.core.design.GlowSpacing
import com.glowup.ai.core.design.LocalGlowColors
import com.glowup.ai.core.ui.EmptyState
import com.glowup.ai.core.ui.ErrorState
import com.glowup.ai.core.ui.GlowButton
import com.glowup.ai.core.ui.GlowCard
import com.glowup.ai.core.ui.GlowTextField
import com.glowup.ai.core.ui.GlowTopBar
import com.glowup.ai.core.ui.LockedCard
import com.glowup.ai.core.ui.ShimmerSkeleton
import com.glowup.ai.domain.model.ContextEvent
import com.glowup.ai.domain.model.ContextEventType
import kotlinx.coroutines.launch

@Composable
fun ContextLogScreen(
    onBack: () -> Unit,
    onUpgrade: () -> Unit,
    viewModel: ContextLogViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val form by viewModel.form.collectAsState()
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    Scaffold(topBar = { GlowTopBar(title = "Context log", onBack = onBack) }) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (val current = state) {
                ScreenState.Loading -> {
                    Column(modifier = Modifier.padding(GlowSpacing.md)) {
                        ShimmerSkeleton(height = 120.dp)
                        ShimmerSkeleton(height = 64.dp, modifier = Modifier.padding(top = GlowSpacing.sm))
                    }
                }

                ScreenState.Locked -> {
                    Box(modifier = Modifier.padding(GlowSpacing.md)) {
                        LockedCard(
                            title = "Context log is Premium",
                            body = "Track sleep, travel, stress, and other context alongside your metrics.",
                            onUnlock = onUpgrade,
                        )
                    }
                }

                is ScreenState.Error -> {
                    Box(modifier = Modifier.padding(GlowSpacing.md)) {
                        ErrorState(message = current.message, onRetry = viewModel::load)
                    }
                }

                is ScreenState.Empty -> {
                    LazyColumn(state = listState, contentPadding = PaddingValues(GlowSpacing.md)) {
                        item { ContextEventForm(form, viewModel) }
                        item {
                            EmptyState(
                                modifier = Modifier.padding(top = GlowSpacing.md),
                                title = current.title,
                                body = current.body,
                                ctaLabel = "Log the first event",
                                onCtaClick = {
                                    coroutineScope.launch { listState.animateScrollToItem(index = 0) }
                                },
                            )
                        }
                    }
                }

                is ScreenState.Content -> {
                    LazyColumn(
                        state = listState,
                        contentPadding = PaddingValues(GlowSpacing.md),
                        verticalArrangement = Arrangement.spacedBy(GlowSpacing.sm),
                    ) {
                        item { ContextEventForm(form, viewModel) }
                        items(current.value, key = { it.id }) { event -> ContextEventRow(event) }
                    }
                }
            }
        }
    }
}

@Composable
private fun ContextEventForm(
    form: ContextLogFormState,
    viewModel: ContextLogViewModel,
) {
    val glow = LocalGlowColors.current
    GlowCard {
        Text("Log a context event", color = glow.ink900, style = androidx.compose.material3.MaterialTheme.typography.titleMedium)
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(top = GlowSpacing.sm)
                    .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(GlowSpacing.xs),
        ) {
            ContextEventType.entries.filter { it != ContextEventType.UNKNOWN }.forEach { type ->
                FilterChip(
                    modifier = Modifier.defaultMinSize(minHeight = 48.dp),
                    selected = form.eventType == type,
                    onClick = { viewModel.onTypeChange(type) },
                    label = { Text(type.name.lowercase().replaceFirstChar { it.uppercase() }) },
                )
            }
        }
        GlowTextField(
            modifier = Modifier.padding(top = GlowSpacing.sm),
            value = form.value,
            onValueChange = viewModel::onValueChange,
            label = "What happened",
            placeholder = "e.g. Only slept 4 hours",
        )
        GlowTextField(
            modifier = Modifier.padding(top = GlowSpacing.sm),
            value = form.occurredAt,
            onValueChange = viewModel::onDateChange,
            label = "Date (optional)",
            placeholder = "YYYY-MM-DD — defaults to now",
        )
        GlowTextField(
            modifier = Modifier.padding(top = GlowSpacing.sm),
            value = form.notes,
            onValueChange = viewModel::onNotesChange,
            label = "Notes (optional)",
        )
        if (form.error != null) {
            Text(form.error, color = glow.danger, modifier = Modifier.padding(top = GlowSpacing.xs))
        }
        GlowButton(
            modifier = Modifier.padding(top = GlowSpacing.sm),
            text = "Save event",
            onClick = viewModel::submit,
            loading = form.submitting,
            enabled = form.value.isNotBlank() && !form.submitting,
        )
    }
}

@Composable
private fun ContextEventRow(event: ContextEvent) {
    val glow = LocalGlowColors.current
    GlowCard {
        Text(
            text =
                event.eventType.name
                    .lowercase()
                    .replaceFirstChar { it.uppercase() } + " · ${event.occurredAt}",
            style = androidx.compose.material3.MaterialTheme.typography.labelMedium,
            color = glow.ink600,
        )
        if (!event.value.isNullOrBlank()) {
            Text(event.value, color = glow.ink900, modifier = Modifier.padding(top = 2.dp))
        }
        if (!event.notes.isNullOrBlank()) {
            Text(event.notes, color = glow.ink600, style = androidx.compose.material3.MaterialTheme.typography.bodySmall)
        }
    }
}
