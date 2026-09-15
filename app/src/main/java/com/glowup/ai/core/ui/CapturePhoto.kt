package com.glowup.ai.core.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil3.ImageLoader
import coil3.compose.SubcomposeAsyncImage
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import com.glowup.ai.BuildConfig
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface CaptureImageLoaderEntryPoint {
    fun imageLoader(): ImageLoader
}

/** Loads private photos only from the configured API, without persistent caches. */
@Composable
fun CapturePhoto(path: String?, description: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val loader = remember(context) {
        EntryPointAccessors.fromApplication(context.applicationContext, CaptureImageLoaderEntryPoint::class.java).imageLoader()
    }
    val safePath = path?.takeIf { it.matches(Regex("users/[A-Za-z0-9_-]+/captures/[A-Za-z0-9_-]+/photo")) }
    if (safePath == null) {
        Box(modifier, contentAlignment = Alignment.Center) { Text("Photo unavailable") }
        return
    }
    SubcomposeAsyncImage(
        model = ImageRequest.Builder(context).data(BuildConfig.API_BASE_URL.trimEnd('/') + "/" + safePath)
            .memoryCachePolicy(CachePolicy.DISABLED).diskCachePolicy(CachePolicy.DISABLED).build(),
        imageLoader = loader,
        contentDescription = description,
        contentScale = ContentScale.Crop,
        modifier = modifier,
        loading = { Box(contentAlignment = Alignment.Center) { Text("Loading photo…") } },
        error = { Box(contentAlignment = Alignment.Center) { Text("Photo unavailable") } },
    )
}
