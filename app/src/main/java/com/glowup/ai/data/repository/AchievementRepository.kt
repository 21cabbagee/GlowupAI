package com.glowup.ai.data.repository

import android.content.Context
import androidx.core.content.edit
import com.glowup.ai.domain.model.UserAchievement
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Achievement Repository
 * Manages achievement state and persistence
 *
 * The calculated snapshot is kept in memory for fast UI updates, while the set of
 * unlocked IDs is persisted in app-private preferences. That durable set is what
 * prevents an app restart from replaying "new achievement" celebrations.
 */
@Singleton
class AchievementRepository
    @Inject
    constructor(
        @ApplicationContext context: Context,
    ) {
        private val persisted =
            context.applicationContext.getSharedPreferences(
                PREFERENCES_NAME,
                Context.MODE_PRIVATE,
            )

        // Calculated snapshots keyed by userId; the authoritative unlocked IDs also live in
        // [persisted] so this cache may safely be discarded on process death.
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
                ?: persisted.getStringSet(keyFor(userId), emptySet()).orEmpty().toSet()

        /**
         * Save achievements for a user
         */
        fun saveAchievements(
            userId: String,
            achievements: List<UserAchievement>,
        ) {
            achievementCache[userId] = achievements
            persisted.edit {
                putStringSet(
                    keyFor(userId),
                    achievements.filter { it.isUnlocked }.map { it.type.id }.toSet(),
                )
            }
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
            persisted.edit { remove(keyFor(userId)) }
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

        private fun keyFor(userId: String): String {
            val digest = MessageDigest.getInstance("SHA-256").digest(userId.toByteArray())
            return "unlocked_" + digest.joinToString("") { "%02x".format(it) }
        }

        private companion object {
            const val PREFERENCES_NAME = "glowup_achievements"
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
