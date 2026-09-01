package com.glowup.ai.feature.routine

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.glowup.ai.core.design.LocalGlowColors
import com.glowup.ai.core.ui.DisclaimerNote
import com.glowup.ai.core.ui.EmptyState
import com.glowup.ai.core.ui.ErrorState
import com.glowup.ai.core.ui.GlowBottomSheet
import com.glowup.ai.core.ui.GlowButton
import com.glowup.ai.core.ui.GlowButtonVariant
import com.glowup.ai.core.ui.GlowCard
import com.glowup.ai.core.ui.GlowTextField
import com.glowup.ai.core.ui.GlowTopBar
import com.glowup.ai.core.ui.SectionHeader
import com.glowup.ai.core.ui.ShimmerSkeleton
import com.glowup.ai.domain.model.DashboardRoutineEvent
import com.glowup.ai.domain.model.Product
import com.glowup.ai.feature.routine.components.AddProductSheet
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoutineRoute(
    onOpenProduct: (String) -> Unit,
    onOpenShelfScan: () -> Unit,
    onOpenExperiments: () -> Unit,
    viewModel: RoutineViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(state.navigateToProductId) {
        state.navigateToProductId?.let {
            onOpenProduct(it)
            viewModel.consumeNavigation()
        }
    }

    RoutineScreen(
        state = state,
        onQueryChange = viewModel::onQueryChange,
        onBarcodeChange = viewModel::onBarcodeChange,
        onLookupBarcode = viewModel::lookupBarcode,
        onProductClick = { onOpenProduct(it.id) },
        onOpenAddProduct = { viewModel.openAddProductSheet(prefillBarcode = state.barcode.trim().ifBlank { null }) },
        onDismissAddProduct = viewModel::dismissAddProductSheet,
        onSubmitAddProduct = viewModel::submitAddProduct,
        onOpenShelfScan = onOpenShelfScan,
        onOpenExperiments = onOpenExperiments,
        onRetryTimeline = viewModel::loadTimeline,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RoutineScreen(
    state: RoutineUiState,
    onQueryChange: (String) -> Unit,
    onBarcodeChange: (String) -> Unit,
    onLookupBarcode: () -> Unit,
    onProductClick: (Product) -> Unit,
    onOpenAddProduct: () -> Unit,
    onDismissAddProduct: () -> Unit,
    onSubmitAddProduct: (com.glowup.ai.domain.model.ProductCreateRequest) -> Unit,
    onOpenShelfScan: () -> Unit,
    onOpenExperiments: () -> Unit,
    onRetryTimeline: () -> Unit,
) {
    val glow = LocalGlowColors.current
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    Scaffold(topBar = { GlowTopBar(title = "Routine") }) { padding ->
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            contentPadding =
                androidx.compose.foundation.layout
                    .PaddingValues(vertical = 16.dp),
        ) {
            item {
                DisclaimerNote(
                    modifier = Modifier.padding(bottom = 16.dp),
                    text = "GlowUp AI tracks cosmetic skin appearance over time. It is not a diagnosis and does not replace a dermatologist.",
                )
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    GlowButton(
                        modifier = Modifier.weight(1f),
                        text = "Scan shelf",
                        variant = GlowButtonVariant.Secondary,
                        onClick = onOpenShelfScan,
                    )
                    GlowButton(
                        modifier = Modifier.weight(1f),
                        text = "Experiments",
                        variant = GlowButtonVariant.Secondary,
                        onClick = onOpenExperiments,
                    )
                }
            }
            item {
                GlowTextField(
                    value = state.query,
                    onValueChange = onQueryChange,
                    label = "Search products",
                    placeholder = "e.g. barrier serum",
                    modifier = Modifier.padding(bottom = 12.dp),
                )
            }
            if (state.query.isNotBlank()) {
                if (state.searching) {
                    item { ShimmerSkeleton(modifier = Modifier.padding(bottom = 12.dp), height = 64.dp) }
                } else if (state.searchError != null) {
                    item {
                        ErrorState(
                            modifier = Modifier.padding(bottom = 12.dp),
                            message = state.searchError,
                            onRetry = { onQueryChange(state.query) },
                        )
                    }
                } else if (state.hasSearched && state.searchResults.isEmpty()) {
                    item {
                        EmptyState(
                            modifier = Modifier.padding(bottom = 12.dp),
                            title = "No matching products",
                            body = "Add it — product rows are shared with every GlowUp AI user, so check spelling first.",
                            ctaLabel = "Add \"${state.query.trim()}\"",
                            onCtaClick = onOpenAddProduct,
                        )
                    }
                } else {
                    items(state.searchResults, key = { it.id }) { product ->
                        ProductRow(product = product, onClick = { onProductClick(product) }, modifier = Modifier.padding(bottom = 8.dp))
                    }
                }
            }
            item {
                GlowCard(modifier = Modifier.padding(top = 8.dp, bottom = 16.dp)) {
                    Text(
                        text = "Have a barcode?",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = glow.ink900,
                    )
                    GlowTextField(
                        value = state.barcode,
                        onValueChange = onBarcodeChange,
                        label = "Barcode",
                        errorText = state.barcodeLookupError,
                        modifier = Modifier.padding(top = 8.dp, bottom = 8.dp),
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        GlowButton(
                            text = "Look up",
                            loading = state.barcodeLookupPending,
                            enabled = state.barcode.isNotBlank() && !state.barcodeLookupPending,
                            variant = GlowButtonVariant.Secondary,
                            onClick = onLookupBarcode,
                        )
                        GlowButton(
                            text = "Add product",
                            variant = GlowButtonVariant.Ghost,
                            onClick = onOpenAddProduct,
                        )
                    }
                }
            }
            item {
                SectionHeader(title = "Recent routine changes", modifier = Modifier.padding(bottom = 12.dp))
            }
            when {
                state.timelineLoading -> {
                    item {
                        ShimmerSkeleton(modifier = Modifier.padding(bottom = 8.dp), height = 56.dp)
                    }
                }

                state.timelineError != null -> {
                    item {
                        ErrorState(message = state.timelineError, onRetry = onRetryTimeline)
                    }
                }

                state.timeline.isEmpty() -> {
                    item {
                        EmptyState(
                            title = "No routine changes logged yet",
                            body = "Log when you start, stop or change a product to build a causal timeline.",
                            ctaLabel = "Search for a product",
                            onCtaClick = {
                                coroutineScope.launch { listState.animateScrollToItem(index = 2) }
                            },
                        )
                    }
                }

                else -> {
                    items(state.timeline) { event -> TimelineRow(event = event, modifier = Modifier.padding(bottom = 8.dp)) }
                }
            }
        }
    }

    if (state.showAddProductSheet) {
        GlowBottomSheet(onDismissRequest = onDismissAddProduct) {
            AddProductSheet(
                prefillBarcode = state.addProductPrefillBarcode,
                pending = state.addProductPending,
                errorText = state.addProductError,
                onSubmit = onSubmitAddProduct,
            )
        }
    }
}

@Composable
private fun ProductRow(
    product: Product,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val glow = LocalGlowColors.current
    GlowCard(modifier = modifier.fillMaxWidth(), onClick = onClick, contentDescription = "Open ${product.name}") {
        Text(product.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = glow.ink900)
        Text(
            product.category.replaceFirstChar { it.uppercase() },
            style = MaterialTheme.typography.bodySmall,
            color = glow.ink600,
            modifier = Modifier.padding(top = 2.dp),
        )
    }
}

@Composable
private fun TimelineRow(
    event: DashboardRoutineEvent,
    modifier: Modifier = Modifier,
) {
    val glow = LocalGlowColors.current
    GlowCard(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "${event.action.name.lowercase().replaceFirstChar(Char::uppercase)} • ${event.productName ?: "Unknown product"}",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = glow.ink900,
        )
        val details = listOfNotNull(event.timestamp, event.slot?.takeIf { it != "unspecified" }, event.notes)
        if (details.isNotEmpty()) {
            Text(
                text = details.joinToString(" • "),
                style = MaterialTheme.typography.bodySmall,
                color = glow.ink600,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}
