package com.glowup.ai.data.local

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Removes user-scoped local data during sign-out, stale-session recovery, and
 * confirmed account deletion. It deliberately leaves the global product
 * catalog intact, but removes every profile/history/entitlement-derived cache
 * and every pending capture image belonging to the user.
 */
@Singleton
class LocalDataCleaner @Inject constructor(
    private val dashboardCacheDao: DashboardCacheDao,
    private val historyCacheDao: HistoryCacheDao,
    private val productDetailCacheDao: ProductDetailCacheDao,
    private val routineEventCacheDao: RoutineEventCacheDao,
    private val experimentCacheDao: ExperimentCacheDao,
    private val verdictCacheDao: VerdictCacheDao,
    private val offerCacheDao: OfferCacheDao,
    private val engagementCacheDao: EngagementCacheDao,
    private val discoverCacheDao: DiscoverCacheDao,
    private val labelCacheDao: LabelCacheDao,
    private val contextEventCacheDao: ContextEventCacheDao,
    private val outboxDao: CaptureOutboxDao,
    private val imageStore: CaptureImageStore,
) {

    suspend fun clearUser(userId: String) {
        val pendingImages = outboxDao.forUser(userId).map { it.imagePath }
        outboxDao.deleteForUser(userId)
        imageStore.deleteAll(pendingImages)

        dashboardCacheDao.clearForUser(userId)
        historyCacheDao.clearAllForUser(userId)
        productDetailCacheDao.clearForUser(userId)
        routineEventCacheDao.clearForUser(userId)
        experimentCacheDao.clearForUser(userId)
        verdictCacheDao.clearForUser(userId)
        offerCacheDao.clearForUser(userId)
        engagementCacheDao.clearForUser(userId)
        discoverCacheDao.clearForUser(userId)
        labelCacheDao.clearForUser(userId)
        contextEventCacheDao.clearForUser(userId)
    }
}
