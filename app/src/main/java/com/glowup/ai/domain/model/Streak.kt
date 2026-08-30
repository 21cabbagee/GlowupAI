package com.glowup.ai.domain.model

import java.time.LocalDate

/**
 * Represents user's capture streak data
 * Inspired by Strava/Duolingo streak mechanics with loss aversion psychology
 */
data class Streak(
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val lastCaptureDate: LocalDate? = null,
    val freezeDaysRemaining: Int = 1, // 1 freeze day per week
    val freezeDayUsedThisWeek: Boolean = false,
    val totalCaptures: Int = 0,
    val streakStartDate: LocalDate? = null,
) {
    /**
     * Check if streak is active (captured today or yesterday)
     */
    val isActive: Boolean
        get() {
            if (lastCaptureDate == null) return false
            val today = LocalDate.now()
            val yesterday = today.minusDays(1)
            return lastCaptureDate == today || lastCaptureDate == yesterday
        }

    /**
     * Check if user needs to capture today to maintain streak
     */
    val needsCaptureToday: Boolean
        get() {
            if (lastCaptureDate == null) return false
            val yesterday = LocalDate.now().minusDays(1)
            return lastCaptureDate == yesterday
        }

    /**
     * Check if freeze day can be used to save streak
     */
    val canUseFreeze: Boolean
        get() = freezeDaysRemaining > 0 && !freezeDayUsedThisWeek

    /**
     * Calculate days since last capture
     */
    val daysSinceLastCapture: Int?
        get() {
            if (lastCaptureDate == null) return null
            val today = LocalDate.now()
            return java.time.temporal.ChronoUnit.DAYS.between(lastCaptureDate, today).toInt()
        }

    companion object {
        val EMPTY = Streak()
    }
}
