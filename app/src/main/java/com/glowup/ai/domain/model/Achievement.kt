package com.glowup.ai.domain.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Achievement system for gamification and habit formation
 * Inspired by Strava/Duolingo achievement mechanics
 * Implements variable rewards and identity reinforcement psychology
 */
enum class AchievementType(
    val id: String,
    val title: String,
    val description: String,
    val icon: ImageVector,
    val requirement: AchievementRequirement
) {
    // Capture milestones
    FIRST_CAPTURE(
        id = "first_capture",
        title = "First Step",
        description = "Took your first capture",
        icon = Icons.Filled.CameraAlt,
        requirement = AchievementRequirement.CaptureCount(1)
    ),
    TENTH_CAPTURE(
        id = "tenth_capture",
        title = "Committed",
        description = "Completed 10 captures",
        icon = Icons.Filled.Stars,
        requirement = AchievementRequirement.CaptureCount(10)
    ),
    FIFTIETH_CAPTURE(
        id = "fiftieth_capture",
        title = "Dedicated Tracker",
        description = "Completed 50 captures",
        icon = Icons.Filled.EmojiEvents,
        requirement = AchievementRequirement.CaptureCount(50)
    ),
    HUNDREDTH_CAPTURE(
        id = "hundredth_capture",
        title = "Century Club",
        description = "Completed 100 captures",
        icon = Icons.Filled.MilitaryTech,
        requirement = AchievementRequirement.CaptureCount(100)
    ),

    // Streak achievements
    WEEK_STREAK(
        id = "week_streak",
        title = "Week Warrior",
        description = "Maintained a 7-day streak",
        icon = Icons.Filled.LocalFireDepartment,
        requirement = AchievementRequirement.StreakDays(7)
    ),
    MONTH_STREAK(
        id = "month_streak",
        title = "Monthly Master",
        description = "Maintained a 30-day streak",
        icon = Icons.Filled.Whatshot,
        requirement = AchievementRequirement.StreakDays(30)
    ),
    QUARTER_STREAK(
        id = "quarter_streak",
        title = "Quarterly Champion",
        description = "Maintained a 90-day streak",
        icon = Icons.Filled.EmojiEvents,
        requirement = AchievementRequirement.StreakDays(90)
    ),

    // Routine achievements
    FIRST_PRODUCT(
        id = "first_product",
        title = "Product Pioneer",
        description = "Added your first product",
        icon = Icons.Filled.ShoppingBag,
        requirement = AchievementRequirement.ProductCount(1)
    ),
    FULL_ROUTINE(
        id = "full_routine",
        title = "Routine Master",
        description = "Built a routine with 5+ products",
        icon = Icons.Filled.AutoAwesome,
        requirement = AchievementRequirement.ProductCount(5)
    ),

    // Experiment achievements
    FIRST_EXPERIMENT(
        id = "first_experiment",
        title = "Skin Scientist",
        description = "Started your first experiment",
        icon = Icons.Filled.Science,
        requirement = AchievementRequirement.ExperimentCount(1)
    ),
    COMPLETED_EXPERIMENT(
        id = "completed_experiment",
        title = "Evidence-Based",
        description = "Completed an experiment",
        icon = Icons.Filled.VerifiedUser,
        requirement = AchievementRequirement.CompletedExperimentCount(1)
    ),

    // Engagement achievements
    BASELINE_SET(
        id = "baseline_set",
        title = "Baseline Established",
        description = "Completed your baseline capture",
        icon = Icons.Filled.Flag,
        requirement = AchievementRequirement.HasBaseline
    ),
    CONSENT_GIVEN(
        id = "consent_given",
        title = "Privacy Conscious",
        description = "Reviewed and accepted consent",
        icon = Icons.Filled.Security,
        requirement = AchievementRequirement.HasConsent
    ),

    // Comparison achievements
    BEFORE_AFTER(
        id = "before_after",
        title = "Before & After",
        description = "Used comparison mode to track progress",
        icon = Icons.Filled.CompareArrows,
        requirement = AchievementRequirement.UsedComparison
    ),

    // Social achievements
    SHARED_PROGRESS(
        id = "shared_progress",
        title = "Inspiration",
        description = "Shared your progress (opt-in)",
        icon = Icons.Filled.Share,
        requirement = AchievementRequirement.SharedProgress
    ),

    // Premium achievements
    PREMIUM_UPGRADE(
        id = "premium_upgrade",
        title = "Power User",
        description = "Upgraded to Premium",
        icon = Icons.Filled.WorkspacePremium,
        requirement = AchievementRequirement.IsPremium
    );

    /**
     * Get tier based on achievement difficulty
     */
    val tier: AchievementTier
        get() = when (this) {
            FIRST_CAPTURE, FIRST_PRODUCT, BASELINE_SET, CONSENT_GIVEN -> AchievementTier.BRONZE
            WEEK_STREAK, TENTH_CAPTURE, FIRST_EXPERIMENT, FULL_ROUTINE, BEFORE_AFTER -> AchievementTier.SILVER
            MONTH_STREAK, FIFTIETH_CAPTURE, COMPLETED_EXPERIMENT, SHARED_PROGRESS -> AchievementTier.GOLD
            QUARTER_STREAK, HUNDREDTH_CAPTURE, PREMIUM_UPGRADE -> AchievementTier.PLATINUM
        }
}

/**
 * Achievement requirement types
 */
sealed class AchievementRequirement {
    data class CaptureCount(val count: Int) : AchievementRequirement()
    data class StreakDays(val days: Int) : AchievementRequirement()
    data class ProductCount(val count: Int) : AchievementRequirement()
    data class ExperimentCount(val count: Int) : AchievementRequirement()
    data class CompletedExperimentCount(val count: Int) : AchievementRequirement()
    object HasBaseline : AchievementRequirement()
    object HasConsent : AchievementRequirement()
    object UsedComparison : AchievementRequirement()
    object SharedProgress : AchievementRequirement()
    object IsPremium : AchievementRequirement()
}

/**
 * Achievement tiers for visual distinction
 */
enum class AchievementTier(
    val displayName: String,
    val color: Long // ARGB color
) {
    BRONZE("Bronze", 0xFFCD7F32),
    SILVER("Silver", 0xFFC0C0C0),
    GOLD("Gold", 0xFFFFD700),
    PLATINUM("Platinum", 0xFFE5E4E2)
}

/**
 * User's achievement progress
 */
data class UserAchievement(
    val type: AchievementType,
    val unlockedAt: String? = null, // ISO timestamp
    val progress: Float = 0f, // 0.0 to 1.0
    val isUnlocked: Boolean = false,
    val isNew: Boolean = false // Just unlocked, show celebration
) {
    /**
     * Get display progress text
     */
    fun getProgressText(): String {
        return when (val req = type.requirement) {
            is AchievementRequirement.CaptureCount -> {
                val current = (progress * req.count).toInt()
                "$current / ${req.count}"
            }
            is AchievementRequirement.StreakDays -> {
                val current = (progress * req.days).toInt()
                "$current / ${req.days} days"
            }
            is AchievementRequirement.ProductCount -> {
                val current = (progress * req.count).toInt()
                "$current / ${req.count} products"
            }
            is AchievementRequirement.ExperimentCount -> {
                val current = (progress * req.count).toInt()
                "$current / ${req.count}"
            }
            is AchievementRequirement.CompletedExperimentCount -> {
                val current = (progress * req.count).toInt()
                "$current / ${req.count}"
            }
            else -> if (isUnlocked) "Unlocked" else "Locked"
        }
    }
}
