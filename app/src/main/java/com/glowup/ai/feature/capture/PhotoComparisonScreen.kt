package com.glowup.ai.feature.capture

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntSize
import com.glowup.ai.core.ui.GlowButton
import com.glowup.ai.core.ui.GlowTopBar
import kotlin.math.abs

/**
 * Photo Comparison Screen
 * Interactive side-by-side photo comparison with slider reveal
 * Inspired by Noom, MyFitnessPal before/after sliders
 * Key motivation feature - visual proof of progress
 */
@Composable
fun PhotoComparisonScreen(
    beforePhotoUrl: String,
    afterPhotoUrl: String,
    beforeDate: String,
    afterDate: String,
    daysBetween: Int,
    onBackClick: () -> Unit,
    onShareClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            GlowTopBar(
                title = "Compare Progress",
                onNavigationClick = onBackClick,
                actions = {
                    IconButton(onClick = onShareClick) {
                        Icon(
                            imageVector = Icons.Filled.Share,
                            contentDescription = "Share"
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Date info card
            ComparisonInfoCard(
                beforeDate = beforeDate,
                afterDate = afterDate,
                daysBetween = daysBetween
            )

            // Interactive slider comparison
            PhotoComparisonSlider(
                beforePhotoUrl = beforePhotoUrl,
                afterPhotoUrl = afterPhotoUrl,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )

            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = { /* Toggle zoom */ },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Filled.ZoomIn,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Zoom")
                }

                GlowButton(
                    onClick = onShareClick,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Share,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Share")
                }
            }

            // Tips
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Drag the slider to reveal before and after. Pinch to zoom.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * Comparison info card showing dates and timeline
 */
@Composable
private fun ComparisonInfoCard(
    beforeDate: String,
    afterDate: String,
    daysBetween: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Before
            Column(
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = "BEFORE",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f),
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = beforeDate,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            // Arrow with days
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Filled.ArrowForward,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = "$daysBetween days",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // After
            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = "AFTER",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f),
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = afterDate,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

/**
 * Interactive photo comparison slider
 * Drag to reveal before/after
 */
@Composable
fun PhotoComparisonSlider(
    beforePhotoUrl: String,
    afterPhotoUrl: String,
    modifier: Modifier = Modifier
) {
    var sliderPosition by remember { mutableStateOf(0.5f) } // 0.0 to 1.0
    var containerSize by remember { mutableStateOf(IntSize.Zero) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .onSizeChanged { containerSize = it }
    ) {
        // Background: AFTER photo (right side)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(16.dp))
        ) {
            // TODO: Load actual image with Coil
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Box(
                    contentAlignment = Alignment.Center
                ) {
                    Text("AFTER", style = MaterialTheme.typography.headlineMedium)
                }
            }

            // Label
            Surface(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp),
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.primary
            ) {
                Text(
                    text = "AFTER",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }

        // Foreground: BEFORE photo (left side) - clipped by slider position
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(16.dp))
        ) {
            Canvas(
                modifier = Modifier.fillMaxSize()
            ) {
                // Clip the before photo to slider position
                val clipWidth = size.width * sliderPosition

                clipRect(
                    left = 0f,
                    top = 0f,
                    right = clipWidth,
                    bottom = size.height
                ) {
                    // TODO: Draw actual before image with Coil
                    drawRect(
                        color = Color(0xFFE8E8E8),
                        size = Size(size.width, size.height)
                    )
                }
            }

            // BEFORE label
            if (sliderPosition > 0.3f) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(12.dp),
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF666666)
                ) {
                    Text(
                        text = "BEFORE",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }

        // Slider handle
        if (containerSize.width > 0) {
            Box(
                modifier = Modifier
                    .offset(x = (containerSize.width * sliderPosition / containerSize.density).dp)
                    .fillMaxHeight()
                    .width(60.dp)
                    .pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            val newPosition = sliderPosition + (dragAmount.x / containerSize.width)
                            sliderPosition = newPosition.coerceIn(0f, 1f)
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                // Vertical line
                Canvas(
                    modifier = Modifier.fillMaxHeight()
                ) {
                    drawLine(
                        color = Color.White,
                        start = Offset(size.width / 2, 0f),
                        end = Offset(size.width / 2, size.height),
                        strokeWidth = 3.dp.toPx()
                    )
                }

                // Handle circle
                Surface(
                    modifier = Modifier
                        .size(48.dp)
                        .align(Alignment.Center),
                    shape = CircleShape,
                    color = Color.White,
                    shadowElevation = 4.dp
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.ChevronLeft,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Icon(
                                imageVector = Icons.Filled.ChevronRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Clip rectangle helper
 */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.clipRect(
    left: Float,
    top: Float,
    right: Float,
    bottom: Float,
    block: androidx.compose.ui.graphics.drawscope.DrawScope.() -> Unit
) {
    val path = Path().apply {
        addRect(
            androidx.compose.ui.geometry.Rect(
                left = left,
                top = top,
                right = right,
                bottom = bottom
            )
        )
    }
    clipPath(path) {
        block()
    }
}
