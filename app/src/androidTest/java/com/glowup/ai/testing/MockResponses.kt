package com.glowup.ai.testing

import okhttp3.mockwebserver.MockResponse

/**
 * Common mock API responses for tests.
 *
 * Provides pre-built JSON responses for various API endpoints.
 */
object MockResponses {
    fun successfulDashboard(userId: String = "test_user") = MockResponse()
        .setResponseCode(200)
        .setBody(
            """
            {
                "profile": {
                    "user": {"id": "$userId", "consent_state": "granted"},
                    "appearance_profiles": [{"id": "appearance_1", "vertical": "skin", "baseline_capture_id": "baseline_1"}],
                    "entitlement": {"plan": "free", "status": "active"},
                    "verticals": ["skin"]
                },
                "vertical": "skin",
                "history": [],
                "engagement": {"capture_streak": 5, "capture_count": 15, "capture_days": [], "reminders": []},
                "features": {}
            }
            """.trimIndent()
        )

    fun successfulCapture(captureId: String = "capture_123") = MockResponse()
        .setResponseCode(201)
        .setBody(
            """
            {
                "id": "$captureId",
                "user_id": "test_user",
                "timestamp": ${System.currentTimeMillis()},
                "metrics": {
                    "smoothness_score": 75.5,
                    "clarity_score": 80.0
                },
                "is_baseline": false
            }
            """.trimIndent()
        )

    fun errorResponse(message: String = "Internal server error") = MockResponse()
        .setResponseCode(500)
        .setBody(
            """
            {
                "error": "$message"
            }
            """.trimIndent()
        )

    fun unauthorizedResponse() = MockResponse()
        .setResponseCode(401)
        .setBody(
            """
            {
                "error": "Unauthorized"
            }
            """.trimIndent()
        )

    fun notFoundResponse() = MockResponse()
        .setResponseCode(404)
        .setBody(
            """
            {
                "error": "Not found"
            }
            """.trimIndent()
        )

    fun emptyDashboard(userId: String = "new_user") = MockResponse()
        .setResponseCode(200)
        .setBody(
            """
            {
                "profile": {
                    "user": {"id": "$userId", "consent_state": "pending"},
                    "appearance_profiles": [{"id": "appearance_1", "vertical": "skin", "baseline_capture_id": null}],
                    "entitlement": {"plan": "free", "status": "active"},
                    "verticals": ["skin"]
                },
                "vertical": "skin",
                "history": [],
                "engagement": {"capture_streak": 0, "capture_count": 0, "capture_days": [], "reminders": []},
                "features": {}
            }
            """.trimIndent()
        )

    fun networkTimeout() = MockResponse()
        .setSocketPolicy(okhttp3.mockwebserver.SocketPolicy.NO_RESPONSE)
}
