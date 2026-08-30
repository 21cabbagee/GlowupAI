package com.glowup.ai.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.glowup.ai.core.design.LocalGlowColors
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.*

/**
 * Calendar Heatmap Component
 * Visual representation of capture consistency over time
 * Inspired by GitHub contribution graph and Duolingo calendar
 */
@Composable
fun CalendarHeatmap(
    captureDates: Set<LocalDate>,
    modifier: Modifier = Modifier,
    currentMonth: YearMonth = YearMonth.now(),
    onDateClick: (LocalDate) -> Unit = {}
) {
    val glowColors = LocalGlowColors.current
    val honeyColor = glowColors.honey500
    val paperColor = glowColors.paper
    val inkColor = MaterialTheme.colorScheme.onBackground

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Month selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = currentMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault()) +
                            " ${currentMonth.year}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = inkColor
                )

                // PERFORMANCE: Memoize month count (PERFORMANCE_OPTIMIZATIONS.md §2.3)
                val capturesThisMonth = remember(captureDates, currentMonth) {
                    captureDates.count {
                        it.month == currentMonth.month && it.year == currentMonth.year
                    }
                }
                Text(
                    text = "$capturesThisMonth captures",
                    style = MaterialTheme.typography.bodyMedium,
                    color = inkColor.copy(alpha = 0.6f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Day of week headers
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                listOf("S", "M", "T", "W", "T", "F", "S").forEach { day ->
                    Text(
                        text = day,
                        style = MaterialTheme.typography.labelSmall,
                        color = inkColor.copy(alpha = 0.5f),
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Calendar grid
            // PERFORMANCE: Memoize calendar calculations (PERFORMANCE_OPTIMIZATIONS.md §2.3)
            val firstDayOfMonth = remember(currentMonth) { currentMonth.atDay(1) }
            val daysInMonth = remember(currentMonth) { currentMonth.lengthOfMonth() }
            val firstDayOfWeek = remember(firstDayOfMonth) { firstDayOfMonth.dayOfWeek.value % 7 } // 0 = Sunday

            // Calculate weeks needed
            val weeksNeeded = remember(firstDayOfWeek, daysInMonth) {
                ((firstDayOfWeek + daysInMonth) / 7.0).toInt() + 1
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                repeat(weeksNeeded) { week ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        repeat(7) { dayOfWeek ->
                            val dayNumber = week * 7 + dayOfWeek - firstDayOfWeek + 1

                            if (dayNumber in 1..daysInMonth) {
                                val date = currentMonth.atDay(dayNumber)
                                val hasCapture = date in captureDates
                                val isToday = date == LocalDate.now()
                                val isFuture = date.isAfter(LocalDate.now())

                                CalendarDayCell(
                                    day = dayNumber,
                                    hasCapture = hasCapture,
                                    isToday = isToday,
                                    isFuture = isFuture,
                                    honeyColor = honeyColor,
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f)
                                        .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                                        .clickable(enabled = !isFuture) {
                                            onDateClick(date)
                                        }
                                )
                            } else {
                                // Empty cell
                                Spacer(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Legend
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LegendItem(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    label = "No capture"
                )
                LegendItem(
                    color = honeyColor,
                    label = "Captured"
                )
                LegendItem(
                    color = MaterialTheme.colorScheme.primary,
                    label = "Today",
                    hasBorder = true
                )
            }
        }
    }
}

@Composable
private fun CalendarDayCell(
    day: Int,
    hasCapture: Boolean,
    isToday: Boolean,
    isFuture: Boolean,
    honeyColor: Color,
    modifier: Modifier = Modifier
) {
    // Fade-in animation with staggered delay
    val fadeAlpha = rememberFadeInAnimation(delay = day * 15)

    // Highlight pulse animation for today
    val highlightAlpha = rememberHighlightAnimation(enabled = isToday)

    val backgroundColor = when {
        isFuture -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        hasCapture -> honeyColor
        else -> MaterialTheme.colorScheme.surfaceVariant
    }

    val textColor = when {
        hasCapture -> MaterialTheme.colorScheme.onPrimary
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    val cellDescription = buildString {
        append("Day $day")
        if (isToday) append(", today")
        if (hasCapture) append(", captured")
        else if (!isFuture) append(", no capture")
        if (isFuture) append(", future date")
    }

    Box(
        modifier = modifier
            .alpha(fadeAlpha)
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .then(
                if (isToday) {
                    Modifier
                        .border(
                            width = 2.dp,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = highlightAlpha),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .graphicsLayer {
                            scaleX = 0.95f + (highlightAlpha * 0.05f)
                            scaleY = 0.95f + (highlightAlpha * 0.05f)
                        }
                } else Modifier
            )
            .semantics {
                contentDescription = cellDescription
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = day.toString(),
            style = MaterialTheme.typography.bodySmall,
            color = textColor,
            fontWeight = if (hasCapture) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
private fun LegendItem(
    color: Color,
    label: String,
    hasBorder: Boolean = false,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(16.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(color)
                .then(
                    if (hasBorder) {
                        Modifier.border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.primary,
                            shape = RoundedCornerShape(4.dp)
                        )
                    } else Modifier
                )
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Compact month-view heatmap (7x5 grid showing current month only)
 */
@Composable
fun CompactCalendarHeatmap(
    captureDates: Set<LocalDate>,
    modifier: Modifier = Modifier,
    onDateClick: (LocalDate) -> Unit = {}
) {
    val glowColors = LocalGlowColors.current
    val honeyColor = glowColors.honey500
    val currentMonth = remember { YearMonth.now() }

    // PERFORMANCE: Memoize calendar calculations (PERFORMANCE_OPTIMIZATIONS.md §2.4)
    val firstDayOfMonth = remember(currentMonth) { currentMonth.atDay(1) }
    val daysInMonth = remember(currentMonth) { currentMonth.lengthOfMonth() }
    val firstDayOfWeek = remember(firstDayOfMonth) { firstDayOfMonth.dayOfWeek.value % 7 }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Calculate weeks
        val weeksNeeded = remember(firstDayOfWeek, daysInMonth) {
            minOf(5, ((firstDayOfWeek + daysInMonth) / 7.0).toInt() + 1)
        }

        repeat(weeksNeeded) { week ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                repeat(7) { dayOfWeek ->
                    val dayNumber = week * 7 + dayOfWeek - firstDayOfWeek + 1

                    if (dayNumber in 1..daysInMonth) {
                        val date = currentMonth.atDay(dayNumber)
                        val hasCapture = date in captureDates
                        val isFuture = date.isAfter(LocalDate.now())

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .defaultMinSize(minWidth = 40.dp, minHeight = 40.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(
                                    when {
                                        isFuture -> Color.Transparent
                                        hasCapture -> honeyColor
                                        else -> MaterialTheme.colorScheme.surfaceVariant
                                    }
                                )
                                .clickable(enabled = hasCapture && !isFuture) {
                                    onDateClick(date)
                                }
                                .semantics {
                                    contentDescription = when {
                                        isFuture -> "Future date"
                                        hasCapture -> "Capture on day $dayNumber"
                                        else -> "No capture on day $dayNumber"
                                    }
                                }
                        )
                    } else {
                        Spacer(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                        )
                    }
                }
            }
        }
    }
}
