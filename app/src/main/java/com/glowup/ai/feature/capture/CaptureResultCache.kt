package com.glowup.ai.feature.capture

import com.glowup.ai.domain.model.CaptureResult
import javax.inject.Inject
import javax.inject.Singleton

/**
 * In-process handoff from the capture flow to [GlowDestination.CaptureResult][com.glowup.ai.feature.shell.GlowDestination.CaptureResult].
 *
 * There is no `GET /captures/{id}` route in the backend (`backend/docs/frontend-api-map.md`
 * documents `POST /api/captures`'s response fields but no single-capture read) — the only place
 * the full [CaptureResult] (with its real `redness_score`/`blemish_count`/`darkspot_area`/
 * `texture_score`/`confidence`/`model_version`, per ANDROID_PLAN.md 3.2 item 7) ever exists is the
 * `POST /api/captures` response itself. [GlowDestination.CaptureResult] only carries a
 * `captureId: String` (fixed by the shell's navigation contract), so this small in-memory cache is
 * what lets the result screen render the real response without re-deriving or re-fetching it.
 *
 * It is deliberately NOT persistence: a process death between accept and viewing the result screen
 * is expected to fall back to "open Home instead" (see `CaptureResultViewModel`), not to a stale or
 * fabricated re-hydration of numbers that must always come from the server.
 */
@Singleton
class CaptureResultCache @Inject constructor() {
    private val results = mutableMapOf<String, CaptureResult>()

    fun put(result: CaptureResult) {
        results[result.id] = result
    }

    fun get(captureId: String): CaptureResult? = results[captureId]
}
