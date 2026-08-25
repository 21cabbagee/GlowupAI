package com.glowup.ai.data.repository

import com.glowup.ai.core.util.GlowResult
import com.glowup.ai.core.util.onSuccess
import com.glowup.ai.data.remote.GlowUpApi
import com.glowup.ai.data.remote.apiCall
import com.glowup.ai.data.remote.dto.toDomain
import com.glowup.ai.data.remote.dto.toDto
import com.glowup.ai.data.repository.support.CacheInvalidationBus
import com.glowup.ai.data.repository.support.InvalidationSignal
import com.glowup.ai.data.repository.support.MutationLock
import com.glowup.ai.domain.model.Experiment
import com.glowup.ai.domain.model.ExperimentCreateRequest
import com.glowup.ai.domain.model.ExperimentStatusRequest
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Owns: `POST /experiments`, `GET /experiments`, `GET /experiments/{id}` (includes `early_stop`),
 * `POST /experiments/{id}/status`.
 *
 * `status` vocabulary is `planned|running|paused|completed|cancelled` — `"active"` is NEVER
 * emitted (ANDROID_PLAN.md §3 bug #1 / trap #11); [com.glowup.ai.domain.model.ExperimentStatus]
 * enforces this at the type boundary. Create and status-change are both non-idempotent mutations.
 */
@Singleton
class ExperimentRepository @Inject constructor(
    private val api: GlowUpApi,
    private val invalidationBus: CacheInvalidationBus,
) {

    private val mutations = MutationLock<String>()
    val pendingKeys: StateFlow<Set<String>> = mutations.pendingKeys

    suspend fun createExperiment(request: ExperimentCreateRequest): GlowResult<Experiment> =
        mutations.run("create_experiment:${request.userId}:${request.productId}:${request.name}") {
            apiCall { api.createExperiment(request.toDto()).toDomain() }
        }.onSuccess { invalidationBus.publish(InvalidationSignal.ExperimentChanged(request.userId)) }

    suspend fun listExperiments(userId: String): GlowResult<List<Experiment>> =
        apiCall { api.listExperiments(userId).map { it.toDomain() } }

    suspend fun getExperiment(userId: String, experimentId: String): GlowResult<Experiment> =
        apiCall { api.getExperiment(userId, experimentId).toDomain() }

    suspend fun setExperimentStatus(experimentId: String, request: ExperimentStatusRequest): GlowResult<Experiment> =
        mutations.run("set_status:$experimentId") {
            apiCall { api.setExperimentStatus(request.userId, experimentId, request.toDto()).toDomain() }
        }.onSuccess { invalidationBus.publish(InvalidationSignal.ExperimentChanged(request.userId)) }
}
