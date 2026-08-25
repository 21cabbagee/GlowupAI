package com.glowup.ai.feature.capture

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.glowup.ai.feature.shell.GlowDestination

/**
 * Wires: [GlowDestination.Capture], [GlowDestination.CaptureResult].
 *
 * Real implementation replaces the legacy `com.glowup.ai.CameraScreen` entirely: CameraX front
 * camera with an oval framing guide, ML Kit face-detection preflight driving a live quality HUD,
 * gallery import, downscale-before-upload, `GET /capture-guide`-driven entry state,
 * server-authoritative quality gating with per-tip coaching on rejection, an outbox for offline
 * capture (never an automatic resend), and a result screen built entirely from the real
 * `POST /api/captures` response plus `POST /measurement-feedback`.
 *
 * Do not rename this function or change its signature — [com.glowup.ai.feature.shell.GlowNavGraph]
 * calls it by this exact name. This destination is full-screen — no bottom bar (see
 * [GlowDestination.fullScreenRoutes]).
 */
fun NavGraphBuilder.captureGraph(navController: NavController) {
    composable<GlowDestination.Capture> {
        CaptureRoute(
            onNavigateToResult = { captureId ->
                navController.navigate(GlowDestination.CaptureResult(captureId)) {
                    // Replace Capture on the back stack: "clear/rearm the capture input" per
                    // `frontend-api-map.md` `POST /api/captures` — back from the result screen
                    // should not return to a live camera holding a just-submitted frame.
                    popUpTo<GlowDestination.Capture> { inclusive = true }
                }
            },
            onClose = { navController.popBackStack() },
        )
    }
    composable<GlowDestination.CaptureResult> {
        CaptureResultRoute(
            onDone = {
                navController.popBackStack()
            },
        )
    }
}
