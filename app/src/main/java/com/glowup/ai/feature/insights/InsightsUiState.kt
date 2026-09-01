package com.glowup.ai.feature.insights

/**
 * The shared `Loading | Content | Empty | Error | Locked` shape (ANDROID_PLAN.md §1) for every
 * simple fetch-and-render screen in this package: context log, root-cause, budget optimizer,
 * derm export, and labels. Q&A has its own [QnaUiState] because a chat thread is not a single
 * fetched value, and the reprocess job has its own [ReprocessUiState] because it is a polled
 * async job, not a one-shot fetch.
 */
sealed interface ScreenState<out T> {
    data object Loading : ScreenState<Nothing>

    data class Content<T>(
        val value: T,
    ) : ScreenState<T>

    data class Empty(
        val title: String,
        val body: String? = null,
    ) : ScreenState<Nothing>

    data class Error(
        val message: String,
    ) : ScreenState<Nothing>

    /** The backend 403'd this call as Premium-only. Rendered as [com.glowup.ai.core.ui.LockedCard],
     * never folded into [Error]. */
    data object Locked : ScreenState<Nothing>
}

fun <T> ScreenState<T>.valueOrNull(): T? =
    when (this) {
        is ScreenState.Content -> value
        else -> null
    }
