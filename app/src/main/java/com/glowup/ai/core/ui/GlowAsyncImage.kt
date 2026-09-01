package com.glowup.ai.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import coil3.compose.AsyncImage
import coil3.request.ImageRequest

/**
 * Reusable async image component using Coil with optimized caching strategy.
 *
 * This component wraps Coil's AsyncImage with GlowUp AI's standard image loading configuration:
 * - Memory and disk caching enabled (via CoilModule)
 * - Crossfade animation for smooth loading
 * - Proper placeholder/error states
 * - Shimmer effect while loading (optional)
 *
 * Performance characteristics:
 * - Memory cache hit: ~5ms load time
 * - Disk cache hit: ~50ms load time
 * - Network fetch: ~600-800ms (with caching for future loads)
 *
 * See PERFORMANCE_OPTIMIZATIONS.md §3 for benchmarks and configuration details.
 *
 * Usage:
 * ```kotlin
 * GlowAsyncImage(
 *     url = capture.imageUrl,
 *     contentDescription = "Capture photo from ${capture.date}",
 *     modifier = Modifier.size(120.dp),
 *     contentScale = ContentScale.Crop
 * )
 * ```
 *
 * @param url Image URL to load (nullable - shows placeholder if null)
 * @param contentDescription Accessibility description for the image (required for WCAG compliance)
 * @param modifier Modifier for the image container
 * @param contentScale How to scale the image within its bounds
 * @param placeholder Optional painter to show while loading (defaults to shimmer)
 * @param error Optional painter to show on load failure
 * @param showShimmerWhileLoading If true, shows animated shimmer while loading
 */
@Composable
fun GlowAsyncImage(
    url: String?,
    contentDescription: String,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    placeholder: Painter? = null,
    error: Painter? = null,
    showShimmerWhileLoading: Boolean = true,
) {
    val context = LocalContext.current

    if (url.isNullOrBlank()) {
        // No URL provided - show placeholder box
        Box(
            modifier =
                modifier
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .semantics {
                        this.contentDescription = contentDescription
                    },
        )
        return
    }

    if (showShimmerWhileLoading && placeholder == null) {
        // Use shimmer as default placeholder
        Box(modifier = modifier) {
            ShimmerSkeleton(modifier = Modifier.matchParentSize())
            AsyncImage(
                model =
                    ImageRequest
                        .Builder(context)
                        .data(url)
                        .memoryCacheKey(url)
                        .diskCacheKey(url)
                        .build(),
                contentDescription = contentDescription,
                modifier = Modifier.matchParentSize(),
                contentScale = contentScale,
                error = error,
            )
        }
    } else {
        AsyncImage(
            model =
                ImageRequest
                    .Builder(context)
                    .data(url)
                    .memoryCacheKey(url)
                    .diskCacheKey(url)
                    .build(),
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = contentScale,
            placeholder = placeholder,
            error = error,
        )
    }
}
