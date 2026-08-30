package com.glowup.ai.domain.calculator

import com.glowup.ai.domain.model.AchievementRequirement
import com.glowup.ai.domain.model.AchievementType
import com.glowup.ai.domain.model.ConsentState
import com.glowup.ai.domain.model.Dashboard
import com.glowup.ai.domain.model.ExperimentStatus
import com.glowup.ai.domain.model.Plan
import com.glowup.ai.domain.model.UserAchievement
import java.time.Instant

/**
 * Achievement Calculator
 * Checks unlock conditions and calculates progress for all achievements
 *
 * Business Logic:
 * - Capture milestones based on engagement.captureCount
 * - Streak achievements based on engagement.captureStreak
 * - Routine achievements based on unique products in routineEvents
 * - Experiment achievements based on experiments list
 * - Engagement achievements based on profile flags
 */
object AchievementCalculator {

    /**
     * Calculate achievement states from current dashboard data
     * Returns list of all achievements with their progress and unlock status
     */
    fun calculateAchievements(
        dashboard: Dashboard,
        previouslyUnlocked: Set<String>
    ): List<UserAchievement> {
        val engagement = dashboard.engagement
        val profile = dashboard.profile

        return AchievementType.values().map { type ->
            val (progress, isUnlocked) = checkRequirement(
                requirement = type.requirement,
                captureCount = engagement?.captureCount ?: 0,
                streakDays = engagement?.captureStreak ?: 0,
                productCount = countUniqueProducts(dashboard),
                experimentCount = dashboard.experiments.size,
                completedExperimentCount = dashboard.experiments.count { it.status == ExperimentStatus.COMPLETED },
                hasBaseline = dashboard.analytics?.baselineCapture == true,
                hasConsent = profile.user.consentState != ConsentState.PENDING,
                sharedProgress = false, // TODO: track this when sharing feature is added
                isPremium = profile.entitlement.plan == Plan.PREMIUM
            )

            val wasUnlocked = previouslyUnlocked.contains(type.id)
            val justUnlocked = isUnlocked && !wasUnlocked

            UserAchievement(
                type = type,
                unlockedAt = if (isUnlocked) Instant.now().toString() else null,
                progress = progress,
                isUnlocked = isUnlocked,
                isNew = justUnlocked
            )
        }
    }

    /**
     * Check if a specific requirement is met and calculate progress
     * Returns (progress: Float, isUnlocked: Boolean)
     */
    private fun checkRequirement(
        requirement: AchievementRequirement,
        captureCount: Int,
        streakDays: Int,
        productCount: Int,
        experimentCount: Int,
        completedExperimentCount: Int,
        hasBaseline: Boolean,
        hasConsent: Boolean,
        sharedProgress: Boolean,
        isPremium: Boolean
    ): Pair<Float, Boolean> {
        return when (requirement) {
            is AchievementRequirement.CaptureCount -> {
                val progress = (captureCount.toFloat() / requirement.count.toFloat()).coerceIn(0f, 1f)
                val unlocked = captureCount >= requirement.count
                progress to unlocked
            }
            is AchievementRequirement.StreakDays -> {
                val progress = (streakDays.toFloat() / requirement.days.toFloat()).coerceIn(0f, 1f)
                val unlocked = streakDays >= requirement.days
                progress to unlocked
            }
            is AchievementRequirement.ProductCount -> {
                val progress = (productCount.toFloat() / requirement.count.toFloat()).coerceIn(0f, 1f)
                val unlocked = productCount >= requirement.count
                progress to unlocked
            }
            is AchievementRequirement.ExperimentCount -> {
                val progress = (experimentCount.toFloat() / requirement.count.toFloat()).coerceIn(0f, 1f)
                val unlocked = experimentCount >= requirement.count
                progress to unlocked
            }
            is AchievementRequirement.CompletedExperimentCount -> {
                val progress = (completedExperimentCount.toFloat() / requirement.count.toFloat()).coerceIn(0f, 1f)
                val unlocked = completedExperimentCount >= requirement.count
                progress to unlocked
            }
            AchievementRequirement.HasBaseline -> {
                val progress = if (hasBaseline) 1f else 0f
                progress to hasBaseline
            }
            AchievementRequirement.HasConsent -> {
                val progress = if (hasConsent) 1f else 0f
                progress to hasConsent
            }
            AchievementRequirement.SharedProgress -> {
                val progress = if (sharedProgress) 1f else 0f
                progress to sharedProgress
            }
            AchievementRequirement.IsPremium -> {
                val progress = if (isPremium) 1f else 0f
                progress to isPremium
            }
        }
    }

    /**
     * Count unique products from routine events
     */
    private fun countUniqueProducts(dashboard: Dashboard): Int {
        val uniqueProducts = dashboard.routineEvents
            .mapNotNull { it.productName }
            .toSet()
        return uniqueProducts.size
    }

    /**
     * Get newly unlocked achievements by comparing previous and current state
     */
    fun getNewlyUnlocked(
        current: List<UserAchievement>,
        previouslyUnlocked: Set<String>
    ): List<UserAchievement> {
        return current.filter { achievement ->
            achievement.isUnlocked && !previouslyUnlocked.contains(achievement.type.id)
        }
    }
}
