package com.glowup.ai.data.repository

import com.glowup.ai.core.util.GlowResult
import com.glowup.ai.core.util.onSuccess
import com.glowup.ai.data.remote.GlowUpApi
import com.glowup.ai.data.remote.apiCall
import com.glowup.ai.data.remote.dto.ShelfScanCreateRequestDto
import com.glowup.ai.data.remote.dto.toDomain
import com.glowup.ai.data.remote.dto.toDto
import com.glowup.ai.data.repository.support.CacheInvalidationBus
import com.glowup.ai.data.repository.support.InvalidationSignal
import com.glowup.ai.data.repository.support.MutationLock
import com.glowup.ai.domain.model.ConfoundCheck
import com.glowup.ai.domain.model.IngredientExplainer
import com.glowup.ai.domain.model.Product
import com.glowup.ai.domain.model.ProductCreateRequest
import com.glowup.ai.domain.model.ProductDetail
import com.glowup.ai.domain.model.ProductPrediction
import com.glowup.ai.domain.model.PurchaseGuidance
import com.glowup.ai.domain.model.PurchaseGuidanceRequest
import com.glowup.ai.domain.model.RoutineEvent
import com.glowup.ai.domain.model.RoutineEventRequest
import com.glowup.ai.domain.model.ShelfScanJob
import com.glowup.ai.domain.model.ShelfScanSelection
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Owns: `POST /api/products`, `GET /products/search`, `GET /products/lookup`,
 * `GET /products/{id}`, `GET /products/{id}/ingredient-explainer`, `GET /products/{id}/predict`,
 * `POST /purchase-guidance`, `POST /routine-events`, `GET /confound-check`,
 * `POST /shelf-scan`, `GET /shelf-scan/{jobId}`, `POST /shelf-scan/{jobId}/confirm`.
 *
 * Two non-idempotent traps this repository exists to guard:
 * - `POST /api/products` is global (no `user_id`) and not idempotent — [createProduct] is
 *   [MutationLock]-guarded per product name so a double-tap "add product" can't create two rows.
 * - `POST /routine-events` accepts ONLY `start`/`stop`/`change`
 *   ([com.glowup.ai.domain.model.RoutineAction] enforces this at the type level via `toWire()`
 *   throwing on `UNKNOWN`) — never a daily "applied" tick.
 */
@Singleton
class RoutineRepository @Inject constructor(
    private val api: GlowUpApi,
    private val invalidationBus: CacheInvalidationBus,
) {

    private val mutations = MutationLock<String>()
    val pendingKeys: StateFlow<Set<String>> = mutations.pendingKeys

    suspend fun createProduct(request: ProductCreateRequest): GlowResult<Product> =
        mutations.run("create_product:${request.name}:${request.barcode}") {
            apiCall { api.createProduct(request.toDto()).toDomain() }
        }

    suspend fun searchProducts(query: String = ""): GlowResult<List<Product>> =
        apiCall { api.searchProducts(query).map { it.toDomain() } }

    suspend fun lookupProduct(barcode: String): GlowResult<Product> =
        apiCall { api.lookupProduct(barcode)?.toDomain() ?: throw MissingProductException }

    suspend fun getProduct(productId: String, userId: String): GlowResult<ProductDetail> =
        apiCall { api.getProduct(productId, userId).toDomain() }

    suspend fun getIngredientExplainer(productId: String, userId: String): GlowResult<IngredientExplainer> =
        apiCall { api.getIngredientExplainer(productId, userId).toDomain() }

    suspend fun predictProduct(productId: String, userId: String): GlowResult<ProductPrediction> =
        apiCall { api.predictProduct(productId, userId).toDomain() }

    suspend fun purchaseGuidance(userId: String, request: PurchaseGuidanceRequest): GlowResult<PurchaseGuidance> =
        apiCall { api.purchaseGuidance(userId, request.toDto()).toDomain() }

    /** Free feature — call before `start`/`change` to warn proactively; the same shape also
     * arrives inline as `RoutineEvent.confoundWarning`. */
    suspend fun confoundCheck(userId: String, excludeProductId: String? = null): GlowResult<ConfoundCheck> =
        apiCall { api.confoundCheck(userId, excludeProductId).toDomain() }

    suspend fun logRoutineEvent(request: RoutineEventRequest): GlowResult<RoutineEvent> =
        mutations.run("routine_event:${request.userId}:${request.productId}:${request.action}") {
            apiCall { api.logRoutineEvent(request.toDto()).toDomain() }
        }.onSuccess {
            invalidationBus.publish(InvalidationSignal.RoutineEventLogged(request.userId))
        }

    // -- Shelf scan (queued job) -----------------------------------------------------------------

    suspend fun submitShelfScan(userId: String, imageBase64: String): GlowResult<String> =
        apiCall { api.submitShelfScan(userId, ShelfScanCreateRequestDto(imageBase64)).jobId }

    /** Poll at ~1.5s per frontend-api-map.md — the CALLER (ViewModel) owns the poll loop/cadence,
     * this just wraps the single status call. */
    suspend fun getShelfScanStatus(userId: String, jobId: String): GlowResult<ShelfScanJob> =
        apiCall { api.getShelfScanStatus(userId, jobId).toDomain(jobId) }

    suspend fun confirmShelfScan(userId: String, jobId: String, selections: List<ShelfScanSelection>): GlowResult<List<Product>> =
        mutations.run("shelf_scan_confirm:$jobId") {
            apiCall {
                api.confirmShelfScan(
                    userId,
                    jobId,
                    com.glowup.ai.data.remote.dto.ShelfScanConfirmRequestDto(selections.map { it.toDto() }),
                ).map { it.toDomain() }
            }
        }.onSuccess {
            invalidationBus.publish(InvalidationSignal.ShelfScanConfirmed(userId))
        }
}

private object MissingProductException : IllegalStateException("product not found")
