package com.glowup.ai.feature.capture

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.glowup.ai.core.design.LocalGlowColors
import com.glowup.ai.core.ui.GlowTopBar
import com.glowup.ai.domain.model.Capture
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.*

/**
 * Photo Grid Screen
 * Browse all captures in grid layout with filters
 * Inspired by Instagram grid, Google Photos
 * Key feature for visual progress tracking
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PhotoGridScreen(
    captures: List<Capture>,
    onCaptureClick: (Capture) -> Unit,
    onBackClick: () -> Unit,
    onCompareClick: (Capture, Capture) -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedFilter by remember { mutableStateOf(PhotoGridFilter.ALL) }
    var selectionMode by remember { mutableStateOf(false) }
    var selectedCaptures by remember { mutableStateOf(setOf<String>()) }

    Scaffold(
        topBar = {
            GlowTopBar(
                title = if (selectionMode) "${selectedCaptures.size} selected" else "Your Photos",
                onBack = {
                    if (selectionMode) {
                        selectionMode = false
                        selectedCaptures = emptySet()
                    } else {
                        onBackClick()
                    }
                },
                actions = {
                    if (selectionMode) {
                        if (selectedCaptures.size == 2) {
                            IconButton(
                                onClick = {
                                    val list = captures.filter { it.id in selectedCaptures }
                                    if (list.size == 2) {
                                        onCompareClick(list[0], list[1])
                                        selectionMode = false
                                        selectedCaptures = emptySet()
                                    }
                                },
                            ) {
                                Icon(Icons.Filled.Compare, "Compare")
                            }
                        }
                        IconButton(
                            onClick = {
                                selectionMode = false
                                selectedCaptures = emptySet()
                            },
                        ) {
                            Icon(Icons.Filled.Close, "Cancel")
                        }
                    } else {
                        IconButton(onClick = { selectionMode = true }) {
                            Icon(Icons.Filled.CheckCircle, "Select")
                        }
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier =
                modifier
                    .fillMaxSize()
                    .padding(padding),
        ) {
            // Filter tabs
            PhotoGridFilterTabs(
                selectedFilter = selectedFilter,
                onFilterSelected = { selectedFilter = it },
                captures = captures,
            )

            // Grid
            val filteredCaptures = filterCaptures(captures, selectedFilter)
            val groupedByMonth = groupCapturesByMonth(filteredCaptures)

            if (groupedByMonth.isEmpty()) {
                // Empty state
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.PhotoLibrary,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        )
                        Text(
                            text = "No photos yet",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = "Take your first capture to start tracking!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    contentPadding = PaddingValues(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    groupedByMonth.forEach { (month, monthCaptures) ->
                        // Month header
                        item(span = { GridItemSpan(3) }) {
                            MonthHeader(month)
                        }

                        // Photos
                        items(monthCaptures, key = { it.id }) { capture ->
                            PhotoGridItem(
                                capture = capture,
                                isSelected = capture.id in selectedCaptures,
                                selectionMode = selectionMode,
                                onClick = {
                                    if (selectionMode) {
                                        selectedCaptures =
                                            if (capture.id in selectedCaptures) {
                                                selectedCaptures - capture.id
                                            } else {
                                                selectedCaptures + capture.id
                                            }
                                    } else {
                                        onCaptureClick(capture)
                                    }
                                },
                                onLongClick = {
                                    if (!selectionMode) {
                                        selectionMode = true
                                        selectedCaptures = setOf(capture.id)
                                    }
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Filter tabs
 */
@Composable
private fun PhotoGridFilterTabs(
    selectedFilter: PhotoGridFilter,
    onFilterSelected: (PhotoGridFilter) -> Unit,
    captures: List<Capture>,
) {
    ScrollableTabRow(
        selectedTabIndex = selectedFilter.ordinal,
        edgePadding = 16.dp,
        divider = {},
    ) {
        PhotoGridFilter.values().forEach { filter ->
            Tab(
                selected = selectedFilter == filter,
                onClick = { onFilterSelected(filter) },
                text = {
                    Text(
                        text = filter.displayName,
                        style = MaterialTheme.typography.labelLarge,
                    )
                },
            )
        }
    }
}

/**
 * Month section header
 */
@Composable
private fun MonthHeader(month: String) {
    Text(
        text = month,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp),
    )
}

/**
 * Individual photo grid item
 */
@Composable
private fun PhotoGridItem(
    capture: Capture,
    isSelected: Boolean,
    selectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val glowColors = LocalGlowColors.current
    val honeyColor = glowColors.honey500

    Box(
        modifier =
            modifier
                .aspectRatio(1f)
                .clip(RoundedCornerShape(8.dp))
                .clickable(onClick = onClick),
    ) {
        // Photo (TODO: Load with Coil)
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = capture.capturedAt.take(10), // Date only
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        // Selection indicator
        if (selectionMode) {
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .background(
                            if (isSelected) {
                                honeyColor.copy(alpha = 0.5f)
                            } else {
                                Color.Black.copy(alpha = 0.3f)
                            },
                        ),
            )

            Icon(
                imageVector = if (isSelected) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
                contentDescription = null,
                tint = if (isSelected) honeyColor else Color.White,
                modifier =
                    Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .size(24.dp),
            )
        }

        // Baseline indicator
        if (capture.isBaseline) {
            Surface(
                modifier =
                    Modifier
                        .align(Alignment.BottomStart)
                        .padding(4.dp),
                shape = RoundedCornerShape(4.dp),
                color = MaterialTheme.colorScheme.primary,
            ) {
                Text(
                    text = "BASELINE",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                )
            }
        }
    }
}

/**
 * Filter enum
 */
enum class PhotoGridFilter(
    val displayName: String,
) {
    ALL("All"),
    THIS_MONTH("This Month"),
    LAST_MONTH("Last Month"),
    BASELINE("Baseline"),
    EXPERIMENTS("Experiments"),
}

/**
 * Helper: Filter captures by selected filter
 */
private fun filterCaptures(
    captures: List<Capture>,
    filter: PhotoGridFilter,
): List<Capture> {
    val now = LocalDate.now()
    return when (filter) {
        PhotoGridFilter.ALL -> {
            captures
        }

        PhotoGridFilter.THIS_MONTH -> {
            captures.filter {
                val date = LocalDate.parse(it.capturedAt.take(10))
                date.month == now.month && date.year == now.year
            }
        }

        PhotoGridFilter.LAST_MONTH -> {
            captures.filter {
                val date = LocalDate.parse(it.capturedAt.take(10))
                val lastMonth = now.minusMonths(1)
                date.month == lastMonth.month && date.year == lastMonth.year
            }
        }

        PhotoGridFilter.BASELINE -> {
            captures.filter { it.isBaseline }
        }

        PhotoGridFilter.EXPERIMENTS -> {
            emptyList()
        } // TODO: Add experiment tracking to HistoryItem
    }
}

/**
 * Helper: Group captures by month
 */
private fun groupCapturesByMonth(captures: List<Capture>): Map<String, List<Capture>> =
    captures
        .sortedByDescending { it.capturedAt }
        .groupBy {
            val date = LocalDate.parse(it.capturedAt.take(10))
            "${date.month.getDisplayName(TextStyle.FULL, Locale.getDefault())} ${date.year}"
        }
