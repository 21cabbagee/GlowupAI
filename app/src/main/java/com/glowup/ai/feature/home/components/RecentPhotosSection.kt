package com.glowup.ai.feature.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.glowup.ai.core.design.GlowShapes
import com.glowup.ai.core.design.GlowSpacing
import com.glowup.ai.core.design.LocalGlowColors
import com.glowup.ai.domain.model.HistoryItem
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Horizontal scrolling list of recent photos.
 * Shows up to 5 most recent captures with dates.
 * Empty state encourages first capture.
 */
@Composable
fun RecentPhotosSection(
    history: List<HistoryItem>,
    onPhotoClick: (String) -> Unit,
    onSeeAllClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val glowColors = LocalGlowColors.current
    val recentPhotos = history.takeLast(5).reversed()

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(GlowSpacing.sm),
    ) {
        // Header with "See All" button
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clickable(
                        onClick = onSeeAllClick,
                        role = Role.Button,
                    ),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Recent Photos",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = glowColors.ink900,
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = "See all",
                    style = MaterialTheme.typography.bodyMedium,
                    color = glowColors.honey700,
                    fontWeight = FontWeight.Medium,
                )
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = glowColors.honey700,
                    modifier = Modifier.size(16.dp),
                )
            }
        }

        if (recentPhotos.isEmpty()) {
            // Empty state
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = GlowShapes.md,
                colors =
                    CardDefaults.cardColors(
                        containerColor = glowColors.honey500.copy(alpha = 0.1f),
                    ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            ) {
                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(GlowSpacing.lg),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(GlowSpacing.sm),
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = null,
                        tint = glowColors.honey700,
                        modifier = Modifier.size(48.dp),
                    )
                    Text(
                        text = "No photos yet",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = glowColors.ink900,
                    )
                    Text(
                        text = "Take your first photo to start tracking",
                        style = MaterialTheme.typography.bodyMedium,
                        color = glowColors.ink600,
                    )
                }
            }
        } else {
            // Photo grid
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(GlowSpacing.sm),
                contentPadding = PaddingValues(horizontal = 2.dp),
            ) {
                items(recentPhotos) { photo ->
                    PhotoCard(
                        photo = photo,
                        onClick = { onPhotoClick(photo.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun PhotoCard(
    photo: HistoryItem,
    onClick: () -> Unit,
) {
    val glowColors = LocalGlowColors.current

    val dateText =
        try {
            photo.capturedAt.let { isoString ->
                val instant = Instant.parse(isoString)
                val localDate = instant.atZone(ZoneId.systemDefault()).toLocalDate()
                val today = java.time.LocalDate.now()
                val yesterday = today.minusDays(1)

                when (localDate) {
                    today -> "Today"
                    yesterday -> "Yesterday"
                    else -> localDate.format(DateTimeFormatter.ofPattern("MMM d"))
                }
            }
        } catch (e: Exception) {
            "Unknown"
        }

    Card(
        modifier =
            Modifier
                .width(120.dp)
                .clickable(onClick = onClick),
        shape = GlowShapes.md,
        colors =
            CardDefaults.cardColors(
                containerColor = glowColors.surfaceCard,
            ),
        elevation =
            CardDefaults.cardElevation(
                defaultElevation = 2.dp,
                pressedElevation = 4.dp,
            ),
    ) {
        Column(
            modifier = Modifier.padding(GlowSpacing.sm),
            verticalArrangement = Arrangement.spacedBy(GlowSpacing.xs),
        ) {
            // Placeholder for photo thumbnail
            // In a real implementation, you would load the actual image here
            Box(
                modifier =
                    Modifier
                        .size(104.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(glowColors.honey500.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = null,
                    tint = glowColors.honey700,
                    modifier = Modifier.size(32.dp),
                )
            }

            Text(
                text = dateText,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium,
                color = glowColors.ink900,
            )

            // Show baseline indicator if applicable
            if (photo.isBaseline) {
                Text(
                    text = "Baseline",
                    style = MaterialTheme.typography.labelSmall,
                    color = glowColors.honey700,
                    modifier =
                        Modifier
                            .background(
                                color = glowColors.honey500.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(4.dp),
                            ).padding(horizontal = 6.dp, vertical = 2.dp),
                )
            }
        }
    }
}
