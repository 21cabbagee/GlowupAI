package com.glowup.ai.feature.auth

import com.google.android.gms.tasks.Task
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Bridges a Google Play services [Task] (what every Firebase Auth SDK call returns) into a
 * suspend function. This app does not declare `org.jetbrains.kotlinx:kotlinx-coroutines-play-
 * services` as a dependency (it is not in `gradle/libs.versions.toml` and this task must not
 * touch gradle files), so the usual `Task<T>.await()` extension from that artifact is
 * unavailable — this is a minimal, dependency-free equivalent used only inside `feature/auth`.
 */
internal suspend fun <T> Task<T>.await(): T =
    suspendCancellableCoroutine { continuation ->
        addOnSuccessListener { result -> continuation.resume(result) }
        addOnFailureListener { exception -> continuation.resumeWithException(exception) }
        addOnCanceledListener {
            continuation.cancel()
        }
    }
