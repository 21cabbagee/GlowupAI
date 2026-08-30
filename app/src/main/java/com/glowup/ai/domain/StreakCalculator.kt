package com.glowup.ai.domain

import com.glowup.ai.domain.model.Capture
import com.glowup.ai.domain.model.Streak
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Calculates streak from capture history
 * Implements behavioral psychology: loss aversion + streak freeze mechanics
 */
object StreakCalculator {

    /**
     * Calculate current streak from list of captures
     * @param captures List of all captures, sorted by date (newest first)
     * @param freezeDayUsedThisWeek Whether user already used freeze this week
     * @return Calculated streak data
     */
    fun calculateStreak(
        captures: List<Capture>,
        freezeDayUsedThisWeek: Boolean = false
    ): Streak {
        if (captures.isEmpty()) {
            return Streak.EMPTY
        }

        // Convert captures to dates only (ignore time)
        val captureDates = captures
            .map { capture ->
                // Parse captured_at timestamp to LocalDate
                LocalDateTime.parse(capture.capturedAt.replace(" ", "T"))
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate()
            }
            .distinct()
            .sortedDescending() // Newest first

        val today = LocalDate.now()
        val lastCaptureDate = captureDates.firstOrNull() ?: return Streak.EMPTY

        // Calculate current streak
        var currentStreak = 0
        var checkDate = today
        var streakStartDate: LocalDate? = null

        for (date in captureDates) {
            val daysDiff = java.time.temporal.ChronoUnit.DAYS.between(date, checkDate).toInt()

            when {
                daysDiff == 0 -> {
                    // Same day
                    currentStreak++
                    streakStartDate = date
                    checkDate = date.minusDays(1)
                }
                daysDiff == 1 -> {
                    // Previous day
                    currentStreak++
                    streakStartDate = date
                    checkDate = date.minusDays(1)
                }
                else -> {
                    // Gap in streak
                    break
                }
            }
        }

        // Calculate longest streak ever
        val longestStreak = calculateLongestStreak(captureDates)

        // Calculate freeze days (1 per week)
        val freezeDaysRemaining = if (freezeDayUsedThisWeek) 0 else 1

        return Streak(
            currentStreak = currentStreak,
            longestStreak = maxOf(longestStreak, currentStreak),
            lastCaptureDate = lastCaptureDate,
            freezeDaysRemaining = freezeDaysRemaining,
            freezeDayUsedThisWeek = freezeDayUsedThisWeek,
            totalCaptures = captures.size,
            streakStartDate = streakStartDate
        )
    }

    /**
     * Calculate longest consecutive streak from all dates
     */
    private fun calculateLongestStreak(dates: List<LocalDate>): Int {
        if (dates.isEmpty()) return 0

        val sortedDates = dates.sorted() // Oldest first
        var maxStreak = 1
        var currentStreak = 1

        for (i in 1 until sortedDates.size) {
            val daysDiff = java.time.temporal.ChronoUnit.DAYS.between(
                sortedDates[i - 1],
                sortedDates[i]
            ).toInt()

            currentStreak = if (daysDiff == 1) {
                currentStreak + 1
            } else {
                1
            }

            maxStreak = maxOf(maxStreak, currentStreak)
        }

        return maxStreak
    }

    /**
     * Check if streak would break if no capture today
     * Considers freeze day availability
     */
    fun wouldStreakBreak(streak: Streak): Boolean {
        if (streak.currentStreak == 0) return false
        if (!streak.needsCaptureToday) return false
        return !streak.canUseFreeze
    }

    /**
     * Get motivational message based on streak state
     */
    fun getStreakMessage(streak: Streak): String {
        return when {
            streak.currentStreak == 0 -> "Start your journey today!"
            streak.currentStreak == 1 -> "Great start! Come back tomorrow to build your streak."
            streak.currentStreak < 7 -> "${streak.currentStreak} day streak! Keep it going."
            streak.currentStreak < 30 -> "${streak.currentStreak} days strong! You're building a habit."
            streak.currentStreak < 90 -> "${streak.currentStreak} day streak! You're in the top 10%."
            else -> "${streak.currentStreak} days! You're a skincare scientist now."
        }
    }

    /**
     * Get warning message if streak at risk
     */
    fun getStreakWarning(streak: Streak): String? {
        return when {
            !streak.isActive && streak.currentStreak > 0 -> {
                if (streak.canUseFreeze) {
                    "Your ${streak.currentStreak}-day streak is at risk! Capture today or use a freeze day."
                } else {
                    "Your ${streak.currentStreak}-day streak is at risk! Capture today to save it."
                }
            }
            streak.needsCaptureToday && streak.currentStreak > 3 -> {
                "Don't lose your ${streak.currentStreak}-day streak! Capture before midnight."
            }
            else -> null
        }
    }
}
