package com.glowup.ai.feature.capture

import com.glowup.ai.domain.model.CaptureResult
import javax.inject.Inject
import javax.inject.Singleton

/**
 * In-process handoff from the capture flow to [GlowDestination.CaptureResult][com.glowup.ai.feature.shell.GlowDestination.CaptureResult].
 *
 * The result screen receives a `captureId: String` (fixed by the shell navigation contract), so
 * this cache provides an immediate render while the screen also has a server-backed recovery path
 * through `GET /users/{userId}/captures/{captureId}`.
 *
 * It is deliberately NOT persistence: a process death between accept and viewing the result screen
 * can recover the owner-scoped result from the server; missing or deleted captures remain visibly
 * unavailable rather than being reconstructed locally.
 */
@Singleton
class CaptureResultCache
    @Inject
    constructor() {
        private val results = mutableMapOf<String, CaptureResult>()

        fun put(result: CaptureResult) {
            results[result.id] = result
        }

        fun get(captureId: String): CaptureResult? = results[captureId]

        /** Clears in-process results at session end; capture ids are not user-qualified. */
        fun clear() {
            results.clear()
        }
    }
