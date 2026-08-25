package com.glowup.ai.data.repository

import com.glowup.ai.core.util.GlowResult
import com.glowup.ai.core.util.map
import com.glowup.ai.data.local.DashboardCacheDao
import com.glowup.ai.data.local.DashboardCacheEntity
import com.glowup.ai.data.local.EngagementCacheDao
import com.glowup.ai.data.local.EngagementCacheEntity
import com.glowup.ai.data.local.HistoryCacheDao
import com.glowup.ai.data.local.HistoryCacheEntity
import com.glowup.ai.data.local.SessionStore
import com.glowup.ai.data.remote.GlowUpApi
import com.glowup.ai.data.remote.NetworkJson
import com.glowup.ai.data.remote.apiCall
import com.glowup.ai.data.remote.dto.DashboardDto
import com.glowup.ai.data.remote.dto.toDomain
import com.glowup.ai.data.remote.dto.toDto
import com.glowup.ai.data.repository.support.Cached
import com.glowup.ai.data.repository.support.CacheInvalidationBus
import com.glowup.ai.data.repository.support.InvalidationSignal
import com.glowup.ai.data.repository.support.KeyedMemoryCache
import com.glowup.ai.data.repository.support.RequestDeduplicator
import com.glowup.ai.data.work.WorkScheduler
import com.glowup.ai.di.ApplicationScope
import com.glowup.ai.domain.model.CaptureGuide
import com.glowup.ai.domain.model.CheckIn
import com.glowup.ai.domain.model.CheckInCreateRequest
import com.glowup.ai.domain.model.Dashboard
import com.glowup.ai.domain.model.Engagement
import com.glowup.ai.domain.model.EngagementEventRequest
import com.glowup.ai.domain.model.HistoryItem
import com.glowup.ai.domain.model.WeeklyRecap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Owns: `GET /dashboard`, `GET /history`, `GET /capture-guide`, `GET /engagement`,
 * `POST /engagement`, `GET|POST /check-ins`, `GET /weekly-recap`.
 *
 * This is the concrete implementation of frontend-api-map.md trap #7: `GET /dashboard` recomputes
 * verdicts and `GET /engagement` writes a reminder row server-side, so BOTH:
 * 1. are only ever called on an explicit, user-visible trigger (pull-to-refresh, screen open) —
 *    never on a timer/poll, and
 * 2. are deduplicated ([RequestDeduplicator]) and cached ([KeyedMemoryCache], keyed by
 *    `{userId, plan, vertical}`) so a burst of concurrent callers (e.g. two composables observing
 *    the same screen) shares one network call.
 *
 * Invalidation is driven entirely by [CacheInvalidationBus] — see that file's table — so
 * mutation-owning repositories never need a direct reference to this one.
 */
@Singleton
class HomeRepository @Inject constructor(
    private val api: GlowUpApi,
    private val sessionStore: SessionStore,
    private val invalidationBus: CacheInvalidationBus,
    private val workScheduler: WorkScheduler,
    private val dashboardCacheDao: DashboardCacheDao,
    private val engagementCacheDao: EngagementCacheDao,
    private val historyCacheDao: HistoryCacheDao,
    @ApplicationScope private val appScope: CoroutineScope,
) {

    private val dashboardCache = KeyedMemoryCache<Dashboard>()
    private val dashboardDedup = RequestDeduplicator<Dashboard>(appScope)

    private val engagementCache = KeyedMemoryCache<Engagement>()
    private val engagementDedup = RequestDeduplicator<Engagement>(appScope)

    private val historyCache = KeyedMemoryCache<List<HistoryItem>>()
    private val historyDedup = RequestDeduplicator<List<HistoryItem>>(appScope)

    init {
        appScope.launch {
            invalidationBus.events.collect { signal ->
                val userId = when (signal) {
                    is InvalidationSignal.CaptureAccepted -> signal.userId
                    is InvalidationSignal.RoutineEventLogged -> signal.userId
                    is InvalidationSignal.ExperimentChanged -> signal.userId
                    is InvalidationSignal.ConsentChanged -> signal.userId
                    is InvalidationSignal.SubscriptionChanged -> signal.userId
                    is InvalidationSignal.ShelfScanConfirmed -> signal.userId
                    is InvalidationSignal.SessionCleared -> signal.userId
                }
                dashboardCache.invalidate { it.startsWith("$userId:") }
                engagementCache.invalidate { it == userId }
                dashboardCacheDao.invalidateForUser(userId)
                engagementCacheDao.invalidateForUser(userId)
                if (signal is InvalidationSignal.CaptureAccepted) {
                    historyCache.invalidate { it == "$userId:${signal.vertical}" }
                    historyCacheDao.invalidateForUser(userId, signal.vertical)
                }
            }
        }
    }

    /**
     * Never polled. Returns a [Cached] `Dashboard` immediately from cache when one is fresh
     * (matching the CURRENT `{userId, plan}`); otherwise dedupes a single network fetch across
     * concurrent callers and serves a stale copy (if any existed — checking the in-memory cache
     * first, then [DashboardCacheDao] so a stale dashboard survives process death) alongside the
     * failure when that fetch fails, so offline/flaky-network users still see their last known
     * dashboard.
     */
    suspend fun getDashboard(userId: String, vertical: String = "skin"): GlowResult<Cached<Dashboard>> {
        val plan = sessionStore.plan()
        val key = "$userId:$vertical"
        dashboardCache.getFresh(key, plan)?.let { return GlowResult.Success(it) }

        val stale = dashboardCache.peek(key, plan) ?: readDashboardFromRoom(userId, plan, vertical)
        return when (
            val result = dashboardDedup.run(key) {
                apiCall {
                    val dto = api.getDashboard(userId, vertical)
                    val dashboard = dto.toDomain()
                    persistDashboardToRoom(userId, dashboard.profile.entitlement.plan, vertical, dto)
                    dashboard
                }
            }
        ) {
            is GlowResult.Success -> {
                dashboardCache.put(key, result.data, result.data.profile.entitlement.plan)
                GlowResult.Success(Cached(result.data, stale = false, fetchedAtMillis = System.currentTimeMillis()))
            }
            is GlowResult.Failure -> if (stale != null) {
                dashboardCache.put(key, stale.data, plan, stale.fetchedAtMillis)
                dashboardCache.invalidate { it == key } // keep the copy but mark it stale again
                GlowResult.Success(stale.copy(refreshError = result.error))
            } else {
                result
            }
        }
    }

    private fun dashboardRoomKey(userId: String, plan: com.glowup.ai.domain.model.Plan, vertical: String) =
        "$userId:${plan.name.lowercase()}:$vertical"

    private suspend fun persistDashboardToRoom(userId: String, plan: com.glowup.ai.domain.model.Plan, vertical: String, dto: DashboardDto) {
        dashboardCacheDao.upsert(
            DashboardCacheEntity(
                cacheKey = dashboardRoomKey(userId, plan, vertical),
                userId = userId,
                plan = plan.name.lowercase(),
                vertical = vertical,
                json = NetworkJson.encodeToString(DashboardDto.serializer(), dto),
                fetchedAtMillis = System.currentTimeMillis(),
                valid = true,
            ),
        )
        dashboardCacheDao.dropOtherPlans(userId, plan.name.lowercase())
    }

    private suspend fun readDashboardFromRoom(userId: String, plan: com.glowup.ai.domain.model.Plan, vertical: String): Cached<Dashboard>? {
        val row = dashboardCacheDao.get(dashboardRoomKey(userId, plan, vertical)) ?: return null
        val dto = runCatching { NetworkJson.decodeFromString(DashboardDto.serializer(), row.json) }.getOrNull() ?: return null
        return Cached(dto.toDomain(), stale = true, fetchedAtMillis = row.fetchedAtMillis)
    }

    /** Explicit invalidation entry point for callers that know a mutation just happened outside
     * the [CacheInvalidationBus] wiring (e.g. a cold-start reconciliation). Prefer publishing to
     * the bus from the owning repository instead of calling this directly. */
    fun invalidateDashboard(userId: String) = dashboardCache.invalidate { it.startsWith("$userId:") }

    /**
     * `GET /engagement` writes a reminder row server-side — never poll this. On success, persists
     * the server's own cadence/window into [SessionStore] so [com.glowup.ai.data.work.ReminderWorker]
     * schedules from real data instead of inventing a client-side interval.
     */
    suspend fun getEngagement(userId: String): GlowResult<Cached<Engagement>> {
        val plan = sessionStore.plan()
        val key = userId
        engagementCache.getFresh(key, plan)?.let { return GlowResult.Success(it) }

        val stale = engagementCache.peek(key, plan) ?: readEngagementFromRoom(userId, plan)
        return when (val result = engagementDedup.run(key) {
            apiCall {
                val dto = api.getEngagement(userId)
                persistEngagementToRoom(userId, plan, dto)
                dto.toDomain()
            }
        }) {
            is GlowResult.Success -> {
                engagementCache.put(key, result.data, plan)
                val reminder = result.data.reminders.firstOrNull()
                sessionStore.setReminderSchedule(
                    cadenceDays = reminder?.cadenceDays,
                    nextAt = reminder?.nextAt,
                    windowStart = result.data.guide?.nextWindowStart,
                    windowEnd = result.data.guide?.nextWindowEnd,
                )
                scheduleReminderFrom(reminder?.nextAt, reminder?.cadenceDays)
                GlowResult.Success(Cached(result.data, stale = false, fetchedAtMillis = System.currentTimeMillis()))
            }
            is GlowResult.Failure -> if (stale != null) GlowResult.Success(stale.copy(refreshError = result.error)) else result
        }
    }

    /** Fire-and-forget engagement telemetry (`POST /engagement`) — not idempotent, but harmless
     * to fire more than once, so no [com.glowup.ai.data.repository.support.MutationLock] here;
     * callers should not block navigation on this. */
    suspend fun logEngagementEvent(userId: String, request: EngagementEventRequest): GlowResult<Unit> =
        apiCall { api.logEngagementEvent(userId, request.toDto()) }.map { }

    suspend fun getCaptureGuide(userId: String, vertical: String = "skin"): GlowResult<CaptureGuide> =
        apiCall { api.getCaptureGuide(userId, vertical).toDomain() }.also { result ->
            if (result is GlowResult.Success) {
                sessionStore.setReminderSchedule(
                    cadenceDays = null,
                    nextAt = null,
                    windowStart = result.data.nextWindowStart,
                    windowEnd = result.data.nextWindowEnd,
                )
            }
        }

    suspend fun getHistory(userId: String, vertical: String = "skin"): GlowResult<Cached<List<HistoryItem>>> {
        val plan = sessionStore.plan()
        val key = "$userId:$vertical"
        historyCache.getFresh(key, plan)?.let { return GlowResult.Success(it) }
        val stale = historyCache.peek(key, plan) ?: readHistoryFromRoom(userId, vertical)
        return when (val result = historyDedup.run(key) {
            apiCall {
                val dtos = api.getHistory(userId, vertical)
                val items = dtos.map { dto -> dto.toDomain() }
                historyCacheDao.upsertAll(dtos.map { dto ->
                    HistoryCacheEntity(
                        id = dto.id,
                        userId = userId,
                        vertical = vertical,
                        capturedAt = dto.capturedAt,
                        json = NetworkJson.encodeToString(com.glowup.ai.data.remote.dto.HistoryItemDto.serializer(), dto),
                        fetchedAtMillis = System.currentTimeMillis(),
                    )
                })
                items
            }
        }) {
            is GlowResult.Success -> {
                historyCache.put(key, result.data, plan)
                GlowResult.Success(Cached(result.data, stale = false, fetchedAtMillis = System.currentTimeMillis()))
            }
            is GlowResult.Failure -> if (stale != null) GlowResult.Success(stale.copy(refreshError = result.error)) else result
        }
    }

    suspend fun getCheckIns(userId: String, limit: Int = 30): GlowResult<List<CheckIn>> =
        apiCall { api.getCheckIns(userId, limit).map { it.toDomain() } }

    /**
     * `POST /check-ins` is not one of the trap #7 side-effecting GETs itself, but `GET /dashboard`
     * embeds `check_ins[]`/`weekly_recap` (task 3.3 deliverable #1), which a fresh check-in
     * changes — so a successful submit invalidates the dashboard cache for this user the same way
     * [com.glowup.ai.data.repository.support.CacheInvalidationBus]'s other mutation triggers do,
     * ensuring the very next `getDashboard` call (never a poll — an explicit screen reload after
     * this submit) re-fetches rather than serving a now-stale cached copy.
     */
    suspend fun createCheckIn(userId: String, request: CheckInCreateRequest): GlowResult<CheckIn> =
        apiCall { api.createCheckIn(userId, request.toDto()).toDomain() }.also { result ->
            if (result is GlowResult.Success) invalidateDashboard(userId)
        }

    suspend fun getWeeklyRecap(userId: String, vertical: String = "skin", asOf: String? = null): GlowResult<WeeklyRecap> =
        apiCall { api.getWeeklyRecap(userId, vertical, asOf).toDomain() }

    /** Reschedules [com.glowup.ai.data.work.ReminderWorker] from whatever the server just said —
     * prefers an explicit `nextAt` timestamp, falls back to `cadenceDays`, and does nothing (never
     * invents an interval) if neither is present. */
    private fun scheduleReminderFrom(nextAt: String?, cadenceDays: Int?) {
        val delayMillis = nextAt?.let { parseIsoToEpochMillis(it) }?.let { it - System.currentTimeMillis() }
            ?: cadenceDays?.let { TimeUnit.DAYS.toMillis(it.toLong()) }
        if (delayMillis != null && delayMillis > 0) {
            workScheduler.scheduleReminder(delayMillis)
        }
    }

    private fun parseIsoToEpochMillis(iso: String): Long? = runCatching {
        val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
        format.timeZone = TimeZone.getTimeZone("UTC")
        format.parse(iso.take(19))?.time
    }.getOrNull()

    private suspend fun persistEngagementToRoom(userId: String, plan: com.glowup.ai.domain.model.Plan, dto: com.glowup.ai.data.remote.dto.EngagementDto) {
        engagementCacheDao.upsert(
            EngagementCacheEntity(
                cacheKey = userId,
                userId = userId,
                plan = plan.name.lowercase(),
                json = NetworkJson.encodeToString(com.glowup.ai.data.remote.dto.EngagementDto.serializer(), dto),
                fetchedAtMillis = System.currentTimeMillis(),
                valid = true,
            ),
        )
    }

    private suspend fun readEngagementFromRoom(userId: String, plan: com.glowup.ai.domain.model.Plan): Cached<Engagement>? {
        val row = engagementCacheDao.get(userId) ?: return null
        if (row.plan != plan.name.lowercase()) return null
        val dto = runCatching {
            NetworkJson.decodeFromString(com.glowup.ai.data.remote.dto.EngagementDto.serializer(), row.json)
        }.getOrNull() ?: return null
        return Cached(dto.toDomain(), stale = true, fetchedAtMillis = row.fetchedAtMillis)
    }

    private suspend fun readHistoryFromRoom(userId: String, vertical: String): Cached<List<HistoryItem>>? {
        val rows = historyCacheDao.forUser(userId, vertical)
        if (rows.isEmpty()) return null
        val items = rows.mapNotNull { row ->
            runCatching {
                NetworkJson.decodeFromString(com.glowup.ai.data.remote.dto.HistoryItemDto.serializer(), row.json).toDomain()
            }.getOrNull()
        }.sortedBy { it.capturedAt }
        return items.takeIf { it.isNotEmpty() }?.let {
            Cached(it, stale = true, fetchedAtMillis = rows.maxOf { row -> row.fetchedAtMillis })
        }
    }
}
