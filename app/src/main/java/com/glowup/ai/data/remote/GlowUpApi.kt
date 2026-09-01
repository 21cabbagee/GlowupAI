package com.glowup.ai.data.remote

import com.glowup.ai.data.remote.dto.AdminAuditEntryDto
import com.glowup.ai.data.remote.dto.AdminMeasurementFeedbackSummaryDto
import com.glowup.ai.data.remote.dto.AdminOfferCreateRequestDto
import com.glowup.ai.data.remote.dto.AnalyticsDto
import com.glowup.ai.data.remote.dto.BudgetOptimizerDto
import com.glowup.ai.data.remote.dto.CaptureCreateRequestDto
import com.glowup.ai.data.remote.dto.CaptureGuideDto
import com.glowup.ai.data.remote.dto.CaptureResponseDto
import com.glowup.ai.data.remote.dto.CheckInCreateRequestDto
import com.glowup.ai.data.remote.dto.CheckInDto
import com.glowup.ai.data.remote.dto.ConfoundCheckDto
import com.glowup.ai.data.remote.dto.ConsentRequestDto
import com.glowup.ai.data.remote.dto.ContextEventCreateRequestDto
import com.glowup.ai.data.remote.dto.ContextEventDto
import com.glowup.ai.data.remote.dto.DashboardDto
import com.glowup.ai.data.remote.dto.DermExportDto
import com.glowup.ai.data.remote.dto.DiscoverDto
import com.glowup.ai.data.remote.dto.EngagementDto
import com.glowup.ai.data.remote.dto.EngagementEventRequestDto
import com.glowup.ai.data.remote.dto.ExperienceProfileUpdateRequestDto
import com.glowup.ai.data.remote.dto.ExperimentCreateRequestDto
import com.glowup.ai.data.remote.dto.ExperimentDto
import com.glowup.ai.data.remote.dto.ExperimentStatusRequestDto
import com.glowup.ai.data.remote.dto.ExportBundleDto
import com.glowup.ai.data.remote.dto.HealthDto
import com.glowup.ai.data.remote.dto.HistoryItemDto
import com.glowup.ai.data.remote.dto.IngredientExplainerDto
import com.glowup.ai.data.remote.dto.JobQueuedResponseDto
import com.glowup.ai.data.remote.dto.LabelCreateRequestDto
import com.glowup.ai.data.remote.dto.LabelDto
import com.glowup.ai.data.remote.dto.MeasurementFeedbackCreateRequestDto
import com.glowup.ai.data.remote.dto.MeasurementFeedbackDto
import com.glowup.ai.data.remote.dto.OfferDto
import com.glowup.ai.data.remote.dto.ProductCreateRequestDto
import com.glowup.ai.data.remote.dto.ProductDetailDto
import com.glowup.ai.data.remote.dto.ProductDto
import com.glowup.ai.data.remote.dto.ProductPredictionDto
import com.glowup.ai.data.remote.dto.ProfileResponseDto
import com.glowup.ai.data.remote.dto.PurchaseGuidanceDto
import com.glowup.ai.data.remote.dto.PurchaseGuidanceRequestDto
import com.glowup.ai.data.remote.dto.QnaCreateRequestDto
import com.glowup.ai.data.remote.dto.QnaMessageDto
import com.glowup.ai.data.remote.dto.QnaResponseDto
import com.glowup.ai.data.remote.dto.ReprocessCreateRequestDto
import com.glowup.ai.data.remote.dto.ReprocessJobDto
import com.glowup.ai.data.remote.dto.RootCauseInsightDto
import com.glowup.ai.data.remote.dto.RoutineEventCreateRequestDto
import com.glowup.ai.data.remote.dto.RoutineEventDto
import com.glowup.ai.data.remote.dto.ShelfScanConfirmRequestDto
import com.glowup.ai.data.remote.dto.ShelfScanCreateRequestDto
import com.glowup.ai.data.remote.dto.ShelfScanJobDto
import com.glowup.ai.data.remote.dto.SubscriptionDto
import com.glowup.ai.data.remote.dto.TriageCreateRequestDto
import com.glowup.ai.data.remote.dto.TriageResultDto
import com.glowup.ai.data.remote.dto.UpgradeRequestDto
import com.glowup.ai.data.remote.dto.UserCreateRequestDto
import com.glowup.ai.data.remote.dto.WeeklyRecapDto
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Retrofit surface for every route in `backend/glowupai/complete_api.py`
 * (ANDROID_PLAN.md Task 2.3 — ~56 routes, no exceptions). Suspend functions
 * throw `retrofit2.HttpException` on any non-2xx response; callers should
 * always go through the `apiCall`/`apiCallNoContent` helpers in
 * [ApiCall.kt] rather than catching `HttpException` themselves, so every
 * failure is normalised by [ApiErrorMapper] the same way.
 *
 * `Authorization: Bearer <idToken>` is attached by [AuthInterceptor] for
 * every request; routes that are documented as open (health, triage,
 * product search/lookup, `POST /api/users`, `POST /api/auth/session`)
 * simply ignore it server-side unless `GLOWUPAI_AUTH_REQUIRED=1`.
 */
interface GlowUpApi {
    // -- Startup ------------------------------------------------------------

    @GET("health")
    suspend fun health(): HealthDto

    // -- Authentication -------------------------------------------------------

    /** Exchange the current Firebase ID token (attached by [AuthInterceptor])
     * for a GlowUpAI profile. Idempotent per Firebase uid. */
    @POST("auth/session")
    suspend fun authSession(): ProfileResponseDto

    // -- Onboarding, profile, consent ----------------------------------------

    @POST("users")
    suspend fun createUser(
        @Body body: UserCreateRequestDto,
    ): ProfileResponseDto

    @GET("users/{userId}/profile")
    suspend fun getProfile(
        @Path("userId") userId: String,
    ): ProfileResponseDto

    @PATCH("users/{userId}/profile")
    suspend fun updateProfile(
        @Path("userId") userId: String,
        @Body body: ExperienceProfileUpdateRequestDto,
    ): ProfileResponseDto

    @POST("users/{userId}/consent")
    suspend fun grantConsent(
        @Path("userId") userId: String,
        @Body body: ConsentRequestDto,
    ): ProfileResponseDto

    // -- Subscription / entitlement -------------------------------------------

    @GET("users/{userId}/subscription")
    suspend fun getSubscription(
        @Path("userId") userId: String,
    ): SubscriptionDto

    @POST("users/{userId}/subscription/upgrade")
    suspend fun upgradeSubscription(
        @Path("userId") userId: String,
        @Body body: UpgradeRequestDto,
    ): SubscriptionDto

    @POST("users/{userId}/subscription/cancel")
    suspend fun cancelSubscription(
        @Path("userId") userId: String,
    ): SubscriptionDto

    // -- Products / routine / experiments -------------------------------------

    @POST("products")
    suspend fun createProduct(
        @Body body: ProductCreateRequestDto,
    ): ProductDto

    @GET("products/search")
    suspend fun searchProducts(
        @Query("q") query: String = "",
    ): List<ProductDto>

    @GET("products/lookup")
    suspend fun lookupProduct(
        @Query("barcode") barcode: String,
    ): ProductDto?

    @GET("products/{productId}")
    suspend fun getProduct(
        @Path("productId") productId: String,
        @Query("user_id") userId: String,
    ): ProductDetailDto

    @GET("products/{productId}/ingredient-explainer")
    suspend fun getIngredientExplainer(
        @Path("productId") productId: String,
        @Query("user_id") userId: String,
    ): IngredientExplainerDto

    @GET("products/{productId}/predict")
    suspend fun predictProduct(
        @Path("productId") productId: String,
        @Query("user_id") userId: String,
    ): ProductPredictionDto

    @POST("users/{userId}/purchase-guidance")
    suspend fun purchaseGuidance(
        @Path("userId") userId: String,
        @Body body: PurchaseGuidanceRequestDto,
    ): PurchaseGuidanceDto

    @POST("routine-events")
    suspend fun logRoutineEvent(
        @Body body: RoutineEventCreateRequestDto,
    ): RoutineEventDto

    @GET("users/{userId}/confound-check")
    suspend fun confoundCheck(
        @Path("userId") userId: String,
        @Query("exclude_product_id") excludeProductId: String? = null,
    ): ConfoundCheckDto

    @POST("experiments")
    suspend fun createExperiment(
        @Body body: ExperimentCreateRequestDto,
    ): ExperimentDto

    @GET("users/{userId}/experiments")
    suspend fun listExperiments(
        @Path("userId") userId: String,
    ): List<ExperimentDto>

    @GET("users/{userId}/experiments/{experimentId}")
    suspend fun getExperiment(
        @Path("userId") userId: String,
        @Path("experimentId") experimentId: String,
    ): ExperimentDto

    @POST("users/{userId}/experiments/{experimentId}/status")
    suspend fun setExperimentStatus(
        @Path("userId") userId: String,
        @Path("experimentId") experimentId: String,
        @Body body: ExperimentStatusRequestDto,
    ): ExperimentDto

    // -- Capture, dashboard, history ------------------------------------------

    @POST("captures")
    suspend fun createCapture(
        @Body body: CaptureCreateRequestDto,
    ): CaptureResponseDto

    @GET("users/{userId}/capture-guide")
    suspend fun getCaptureGuide(
        @Path("userId") userId: String,
        @Query("vertical") vertical: String = "skin",
    ): CaptureGuideDto

    @GET("users/{userId}/dashboard")
    suspend fun getDashboard(
        @Path("userId") userId: String,
        @Query("vertical") vertical: String = "skin",
    ): DashboardDto

    @GET("users/{userId}/history")
    suspend fun getHistory(
        @Path("userId") userId: String,
        @Query("vertical") vertical: String = "skin",
    ): List<HistoryItemDto>

    /** Side-effecting (writes a reminder row) — never poll. */
    @GET("users/{userId}/engagement")
    suspend fun getEngagement(
        @Path("userId") userId: String,
    ): EngagementDto

    @POST("users/{userId}/engagement")
    suspend fun logEngagementEvent(
        @Path("userId") userId: String,
        @Body body: EngagementEventRequestDto,
    ): EngagementEventResponseDto

    @GET("users/{userId}/check-ins")
    suspend fun getCheckIns(
        @Path("userId") userId: String,
        @Query("limit") limit: Int = 30,
    ): List<CheckInDto>

    @POST("users/{userId}/check-ins")
    suspend fun createCheckIn(
        @Path("userId") userId: String,
        @Body body: CheckInCreateRequestDto,
    ): CheckInDto

    @GET("users/{userId}/weekly-recap")
    suspend fun getWeeklyRecap(
        @Path("userId") userId: String,
        @Query("vertical") vertical: String = "skin",
        @Query("as_of") asOf: String? = null,
    ): WeeklyRecapDto

    @POST("users/{userId}/measurement-feedback")
    suspend fun addMeasurementFeedback(
        @Path("userId") userId: String,
        @Body body: MeasurementFeedbackCreateRequestDto,
    ): MeasurementFeedbackDto

    @GET("users/{userId}/analytics")
    suspend fun getAnalytics(
        @Path("userId") userId: String,
    ): AnalyticsDto

    // -- Q&A, triage, discover, commerce ---------------------------------------

    @POST("users/{userId}/qna")
    suspend fun askQna(
        @Path("userId") userId: String,
        @Body body: QnaCreateRequestDto,
    ): QnaResponseDto

    @GET("users/{userId}/qna")
    suspend fun getQnaHistory(
        @Path("userId") userId: String,
    ): List<QnaMessageDto>

    /** Open route — no `user_id`, no Premium requirement. */
    @POST("triage")
    suspend fun triage(
        @Body body: TriageCreateRequestDto,
    ): TriageResultDto

    @GET("users/{userId}/discover")
    suspend fun getDiscover(
        @Path("userId") userId: String,
    ): DiscoverDto

    /** Commerce is free for every plan — never gate this call on Premium. */
    @GET("users/{userId}/commerce/offers")
    suspend fun getOffers(
        @Path("userId") userId: String,
        @Query("product_id") productId: String? = null,
    ): List<OfferDto>

    @POST("users/{userId}/commerce/offers/{offerId}/click")
    suspend fun clickOffer(
        @Path("userId") userId: String,
        @Path("offerId") offerId: String,
    ): OfferDto

    // -- Labels -----------------------------------------------------------------

    @GET("users/{userId}/labels")
    suspend fun getLabels(
        @Path("userId") userId: String,
    ): List<LabelDto>

    @POST("users/{userId}/labels")
    suspend fun addLabel(
        @Path("userId") userId: String,
        @Body body: LabelCreateRequestDto,
    ): LabelDto

    // -- Reprocessing (async job) -------------------------------------------------

    @POST("users/{userId}/reprocess")
    suspend fun reprocess(
        @Path("userId") userId: String,
        @Body body: ReprocessCreateRequestDto,
    ): JobQueuedResponseDto

    @GET("users/{userId}/reprocess/{jobId}")
    suspend fun getReprocessStatus(
        @Path("userId") userId: String,
        @Path("jobId") jobId: String,
    ): ReprocessJobDto

    // -- Shelf scan (async job) ---------------------------------------------------

    @POST("users/{userId}/shelf-scan")
    suspend fun submitShelfScan(
        @Path("userId") userId: String,
        @Body body: ShelfScanCreateRequestDto,
    ): JobQueuedResponseDto

    @GET("users/{userId}/shelf-scan/{jobId}")
    suspend fun getShelfScanStatus(
        @Path("userId") userId: String,
        @Path("jobId") jobId: String,
    ): ShelfScanJobDto

    @POST("users/{userId}/shelf-scan/{jobId}/confirm")
    suspend fun confirmShelfScan(
        @Path("userId") userId: String,
        @Path("jobId") jobId: String,
        @Body body: ShelfScanConfirmRequestDto,
    ): List<ProductDto>

    // -- Context events + root-cause search (Premium) -----------------------------

    @GET("users/{userId}/context-events")
    suspend fun getContextEvents(
        @Path("userId") userId: String,
    ): List<ContextEventDto>

    @POST("users/{userId}/context-events")
    suspend fun addContextEvent(
        @Path("userId") userId: String,
        @Body body: ContextEventCreateRequestDto,
    ): ContextEventDto

    @GET("users/{userId}/root-cause")
    suspend fun getRootCause(
        @Path("userId") userId: String,
        @Query("metric") metric: String = "texture_score",
    ): List<RootCauseInsightDto>

    @GET("users/{userId}/budget-optimizer")
    suspend fun getBudgetOptimizer(
        @Path("userId") userId: String,
    ): BudgetOptimizerDto

    @GET("users/{userId}/derm-export")
    suspend fun getDermExport(
        @Path("userId") userId: String,
    ): DermExportDto

    // -- Export, deletion ---------------------------------------------------------

    @GET("users/{userId}/export")
    suspend fun exportUser(
        @Path("userId") userId: String,
    ): ExportBundleDto

    /** `204 No Content` on success — do NOT attempt to parse a body. */
    @DELETE("users/{userId}")
    suspend fun deleteUser(
        @Path("userId") userId: String,
    ): Response<Unit>

    // -- Admin (NOT FOR APP USE) ----------------------------------------------------
    //
    // Requires `Authorization: Bearer <GLOWUPAI_ADMIN_TOKEN>`, a secret this
    // app must never hold. Declared here only for complete route coverage —
    // no repository may call these. See domain.model.AdminAuditEntry doc.

    @GET("admin/audit")
    suspend fun adminAudit(
        @Query("limit") limit: Int = 100,
        @Header("Authorization") adminBearerToken: String,
    ): List<AdminAuditEntryDto>

    @POST("admin/offers")
    suspend fun adminAddOffer(
        @Body body: AdminOfferCreateRequestDto,
        @Header("Authorization") adminBearerToken: String,
    ): OfferDto

    @GET("admin/measurement-feedback")
    suspend fun adminMeasurementFeedbackSummary(
        @Header("Authorization") adminBearerToken: String,
    ): AdminMeasurementFeedbackSummaryDto
}

/** `POST /engagement` response — the static UI does not consume it, but a
 * typed client still needs a shape to deserialize into. */
@Serializable
data class EngagementEventResponseDto(
    val id: String = "",
    @SerialName("event_type") val eventType: String = "",
    @SerialName("reference_id") val referenceId: String? = null,
    @SerialName("occurred_at") val occurredAt: String? = null,
)
