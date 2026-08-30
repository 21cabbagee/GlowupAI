package com.glowup.ai.data.telemetry

import android.content.Context
import android.os.Bundle
import com.glowup.ai.core.util.GlowResult
import com.glowup.ai.data.local.SessionStore
import com.glowup.ai.data.repository.HomeRepository
import com.glowup.ai.domain.model.EngagementEventRequest
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton
import com.glowup.ai.di.ApplicationScope

/** Stable, non-clinical product events. Values are deliberately not user-entered text. */
enum class TelemetryEvent(val wireName: String) {
    APP_OPEN("app_open"),
    HOME_VIEWED("home_viewed"),
    DASHBOARD_REFRESHED("dashboard_refreshed"),
    CAPTURE_STARTED("capture_started"),
    CAPTURE_ACCEPTED("capture_accepted"),
    CAPTURE_REJECTED("capture_rejected"),
    ROUTINE_EVENT_LOGGED("routine_event_logged"),
    QNA_ASKED("qna_asked"),
    QNA_HANDOFF("qna_handoff"),
    OFFER_OPENED("offer_opened"),
    ACCOUNT_EXPORT_REQUESTED("account_export_requested"),
    ACCOUNT_DELETED("account_deleted"),
    STREAK_FREEZE_DAY_USED("streak_freeze_day_used"),
}

/**
 * Fire-and-forget engagement telemetry.
 *
 * The caller only enqueues a small, bounded event and never waits for network or Firebase. The
 * queue contains stable event names and identifiers only; photo bytes, question text, notes, and
 * other free-form user content are intentionally not accepted by this API. Failed network posts
 * are retried off the UI thread and then dropped, so telemetry can never block product flows.
 */
@Singleton
class Telemetry @Inject constructor(
    @ApplicationContext private val context: Context,
    private val sessionStore: SessionStore,
    private val homeRepository: HomeRepository,
    @ApplicationScope private val appScope: CoroutineScope,
) {
    private data class QueuedEvent(
        val event: TelemetryEvent,
        val referenceId: String?,
        val metadata: Map<String, String>,
    )

    private val queue = Channel<QueuedEvent>(capacity = 64)
    private val analytics: FirebaseAnalytics? by lazy {
        runCatching { FirebaseAnalytics.getInstance(context) }.getOrNull()
    }
    private val crashlytics: FirebaseCrashlytics? by lazy {
        runCatching { FirebaseCrashlytics.getInstance() }.getOrNull()
    }

    init {
        appScope.launch {
            for (event in queue) {
                deliver(event)
            }
        }
    }

    fun track(
        event: TelemetryEvent,
        referenceId: String? = null,
        metadata: Map<String, String> = emptyMap(),
    ) {
        // Only low-cardinality, machine-readable values can cross this boundary.
        val safeMetadata = metadata
            .filterKeys { it.matches(SAFE_KEY) }
            .filterValues { it.matches(SAFE_VALUE) }
            .mapValues { it.value.take(48) }
        analytics?.let { firebase ->
            runCatching {
                firebase.logEvent(event.wireName, Bundle().apply {
                    safeMetadata.forEach { (key, value) -> putString(key, value) }
                })
            }
        }
        queue.trySend(QueuedEvent(event, referenceId?.takeIf { it.matches(SAFE_VALUE) }, safeMetadata))
    }

    /** Records unexpected app failures without logging user content. */
    fun recordNonFatal(throwable: Throwable) {
        runCatching { crashlytics?.recordException(throwable) }
    }

    private suspend fun deliver(event: QueuedEvent) {
        val userId = sessionStore.userId() ?: return
        val request = EngagementEventRequest(
            eventType = event.event.wireName,
            referenceId = event.referenceId,
            metadata = event.metadata.ifEmpty { null },
        )
        repeat(3) { attempt ->
            when (val result = homeRepository.logEngagementEvent(userId, request)) {
                is GlowResult.Success -> return
                is GlowResult.Failure -> {
                    if (result.error !is com.glowup.ai.data.remote.ApiError.Network || attempt == 2) return
                    delay(750L * (attempt + 1))
                }
            }
        }
    }

    private companion object {
        val SAFE_KEY = Regex("^[a-zA-Z][a-zA-Z0-9_]{0,31}$")
        val SAFE_VALUE = Regex("^[a-zA-Z0-9_.:-]{1,64}$")
    }
}