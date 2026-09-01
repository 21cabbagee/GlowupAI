package com.glowup.ai.data.repository

import com.glowup.ai.domain.model.UserAchievement
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Achievement Repository
 * Manages achievement state and persistence
 *
 * Currently uses in-memory storage. Can be migrated to Room database
 * for persistence across app restarts in the future.
 */
@Singleton
class AchievementRepository
    @Inject
    constructor() {
        // In-memory storage keyed by userId
        private val achievementCache = mutableMapOf<String, List<UserAchievement>>()

        // Flow for observing achievement changes
        private val _achievementFlow = MutableStateFlow<Map<String, List<UserAchievement>>>(emptyMap())
        val achievementFlow: StateFlow<Map<String, List<UserAchievement>>> = _achievementFlow.asStateFlow()

        /**
         * Get all achievements for a user
         */
        fun getAchievements(userId: String): List<UserAchievement> = achievementCache[userId] ?: emptyList()

        /**
         * Get set of unlocked achievement IDs for a user
         */
        fun getUnlockedIds(userId: String): Set<String> =
            achievementCache[userId]
                ?.filter { it.isUnlocked }
                ?.map { it.type.id }
                ?.toSet()
                ?: emptySet()

        /**
         * Save achievements for a user
         */
        fun saveAchievements(
            userId: String,
            achievements: List<UserAchievement>,
        ) {
            achievementCache[userId] = achievements
            _achievementFlow.update { achievementCache.toMap() }
        }

        /**
         * Mark new achievements as seen (clear the isNew flag)
         */
        fun markAchievementsAsSeen(userId: String) {
            val current = achievementCache[userId] ?: return
            val updated = current.map { it.copy(isNew = false) }
            saveAchievements(userId, updated)
        }

        /**
         * Get count of new (unseen) achievements for a user
         */
        fun getNewAchievementCount(userId: String): Int = achievementCache[userId]?.count { it.isNew } ?: 0

        /**
         * Clear all achievements for a user (e.g., on sign out)
         */
        fun clearAchievements(userId: String) {
            achievementCache.remove(userId)
            _achievementFlow.update { achievementCache.toMap() }
        }

        /**
         * Get achievement statistics for display
         */
        fun getAchievementStats(userId: String): AchievementStats {
            val achievements = getAchievements(userId)
            val unlockedCount = achievements.count { it.isUnlocked }
            val totalCount = achievements.size
            val newCount = achievements.count { it.isNew }

            return AchievementStats(
                unlockedCount = unlockedCount,
                totalCount = totalCount,
                newCount = newCount,
                progressPercent =
                    if (totalCount > 0) {
                        (unlockedCount.toFloat() / totalCount.toFloat()) * 100f
                    } else {
                        0f
                    },
            )
        }
    }

/**
 * Achievement statistics data class
 */
data class AchievementStats(
    val unlockedCount: Int,
    val totalCount: Int,
    val newCount: Int,
    val progressPercent: Float,
)
