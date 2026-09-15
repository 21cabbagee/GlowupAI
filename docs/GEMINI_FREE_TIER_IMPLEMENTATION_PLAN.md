# GlowUpAI: GPT-5.6 Luna vision + Gemini intelligence

Status: implemented. The former unpaid-tier routing switch has been removed;
personal Gemini processing now uses an explicit reviewed deployment opt-in.
Prepared 2026-09-07. Audience: Luna implementation agents. Revised for a paid OpenAI vision stage and a separate Gemini reasoning stage.

## 1. Decision and scope

Replace the custom MobileNet skin inference and deterministic skin-scoring paths with a two-stage provider pipeline. Stage 1 sends an eligible uploaded image to the paid OpenAI API using the exact configured `gpt-5.6-luna` model and asks it for bounded, structured cosmetic observations. Stage 2 sends only the validated Luna observation object plus approved evidence to Gemini for summaries, comparisons, routine explanations and question answering. Gemini is not used for raw-face vision in this plan. Do not implement Google Cloud Vision, Vertex AI, model training, image generation, or an unapproved provider.

“GPT Luna” in this document means the callable OpenAI API model ID `gpt-5.6-luna`, not a ChatGPT subscription or a Luna coding agent. A ChatGPT/Luna subscription does not by itself give the backend permission or credentials to call a model for app users. L01 must verify API account access, model availability, image-input support and applicable data controls. OpenAI's official model documentation describes GPT-5.6 Luna as a cost-sensitive, high-volume model; the API documentation shows image inputs are sent through the Responses API. Sources: https://developers.openai.com/api/docs/models/all and https://platform.openai.com/docs/quickstart/make-your-first-api-request

Keep ordinary application logic: authentication, databases, routines, capture quality checks, arithmetic, authorization, scheduling, and caching. Keep Android's Google ML Kit face detection for immediate camera guidance; sending video frames to Gemini would waste quota and slow capture. “Replace everything” refers to the AI analysis and language providers, not this infrastructure.

The provider responsibilities are deliberately split:

- Luna sees the image and returns observations, quality/assessability, product label transcription, or a two-image comparability result.
- The backend validates and stores the Luna result; it performs ownership, arithmetic, time windows, quota accounting and policy checks locally.
- Gemini receives no image. It turns a validated, minimal evidence packet into clear user-facing language and bounded suggestions. It must not add facts, measurements, diagnoses or evidence IDs.
- Product verdict copy, weekly recaps, routine guidance and Q&A all use this same text-only stage; factual aggregation remains local and cached so screen refreshes do not create repeated provider calls.
- If Gemini fails, Luna's text-reasoning mode receives the same validated evidence packet and produces the language response. It does not receive the image again. If both reasoning attempts fail, the app shows the validated Luna observations and factual timeline locally.

### Features to implement

The implementation must deliver the six features discussed in the previous planning conversation:

1. Image-based skin/cosmetic observation from uploaded captures.
2. Region-specific results with uncertainty and retake guidance.
3. Comparable-photo progress tracking and dated improvement timelines.
4. Product-label and ingredient scanning with user confirmation.
5. Intelligent routine, ingredient and product guidance grounded in the user's approved evidence.
6. Routine-change attribution notes, weekly recaps and conversational Q&A.

### Provider split by task

| Task | OpenAI Luna | Gemini | Local backend |
| --- | --- | --- | --- |
| Selfie/capture observation | Receives the uploaded image and emits structured visible observations; can text-reason from that evidence if Gemini fails | Receives no image; optionally phrases validated observations when its data tier permits | Quality gates, ownership, versioning and persistence |
| Two-photo comparison | Receives both owned images and judges comparability/directional changes | Optionally explains the already validated comparison | Date window, pair selection and trend aggregation |
| Product-label scan | Reads packaging/ingredient text from a product-only image | Normalizes/explains confirmed text | Product confirmation, catalog matching and routine mutation |
| Routine/ingredient question | Text-reasoning fallback only if Gemini fails | Uses approved evidence packet for language and suggestions | Evidence retrieval, safety rules, action allowlist and cache |
| Weekly recap | Text-reasoning fallback only if Gemini fails | Optional one-time wording request | Computes facts and timeline locally |

The normal successful path is one Luna vision call plus one Gemini call. If Gemini fails, make one bounded Luna text-reasoning call using the validated evidence packet; do not resend the image. Cached Luna evidence can support many local screens without another vision call; cached language can support repeated views without another provider call. If both reasoning calls fail, use the local factual renderer.

Deliver six capabilities:

1. Guided capture and structured cosmetic observations.
2. Region-specific explanations with uncertainty.
3. Comparable-photo progress reports.
4. Product-label scanning with user verification.
5. Evidence-backed routine assistance and ingredient explanations.
6. Routine-change timelines and weekly summaries.

Do not present generated scores as clinically validated measurements. Do not infer diagnoses, medical treatment, skin moisture, exact lesion counts, percentage improvements, attractiveness scores, or guaranteed product outcomes from a selfie. The first version describes visible cosmetic appearance qualitatively.

## 2. Feasibility gates: distinguish implementation from launch

Google's unpaid-service terms state: “Do not submit sensitive, confidential, or personal information to the Unpaid Services.” They also restrict client age, regions, and medical use. Read the current terms before deployment. Consent does not override provider restrictions. An identifiable face or a personal skin-history payload must not be treated as anonymous just because names or EXIF were removed. Regional exceptions need explicit verification for the actual deployment; do not infer eligibility from the developer's location.

The proposed production path therefore pays for the OpenAI Luna image call. It does not make the Gemini hop automatically permitted: a Luna observation derived from a user's face and linked to that user's history may still be personal information. Sending that packet to Gemini's unpaid quota can violate the same restriction even though the image has been removed. The exact Luna → Gemini personalized chain requires either a Gemini paid/data-processing arrangement that permits the use, or a local postprocessor. Paying for Luna alone does not authorize sending personal evidence to free Gemini.

Source: https://ai.google.dev/gemini-api/terms

Consequently, under the presently verified constraints:

- Build and test the entire dual-provider flow with fake clients and permitted fixtures first.
- Luna real-user image submission is gated on an OpenAI API account, model access, applicable data controls, user disclosure/consent and the deployment's privacy review. Never put an OpenAI key in Android or browser code.
- Gemini free tier can be used for generic, nonpersonal explanations and product-only flows only after a data-classification check. It must not receive user-linked observations, routine history or a face-derived packet unless an eligible Gemini paid arrangement is configured.
- In the strict zero-cost-Gemini mode, the local renderer handles personal Luna results. Gemini can still improve generic ingredient definitions, prompt templates and nonpersonal product-label text, but it is not the personalized reasoning stage.
- Existing private history remains local for timelines and comparisons. Only the minimum permitted evidence is sent to a second provider.

The full personal-photo product can be designed around paid Luna, but its second-stage Gemini use remains a separate terms and billing gate. This is an external launch dependency, not an engineering task that a Luna agent should bypass. Complete all independent implementation tasks anyway.

### Deployment modes

Implement one code path with explicit configuration rather than maintaining separate forks:

1. `dual_paid_personal`: paid Luna receives eligible images; Gemini paid/data-processing access receives validated user-derived evidence. This is the mode that fulfills the requested Luna → Gemini personalized chain, subject to both providers' account, regional, privacy and safety reviews.
2. `luna_paid_local_language`: paid Luna receives eligible images; all user-derived reasoning stays in the backend's local renderer; Gemini is disabled for personal evidence. This is the safe fallback when Gemini remains unpaid.
3. `product_free_gemini`: Gemini free tier handles only approved product-only or public-catalog content. Luna is not called for a personal face in this mode.
4. `disabled`: no external AI calls; existing capture, routine and factual-history behavior remains available.

Luna agents must implement and test all four modes, but the release configuration selects exactly one. The requested production target is mode 1 if you approve Gemini billing/terms as well as Luna billing; otherwise use mode 2 for personal captures and mode 3 for nonpersonal product intelligence.

## 3. Model, provider policy and cost control

Primary model: `gpt-5.6-luna`, selected by exact model ID and called through the OpenAI Responses API. It has two explicitly bounded task modes: `vision_observation` receives an image and returns structured evidence; `text_reasoning_fallback` receives validated evidence only and writes the user-facing explanation when Gemini fails. Set `reasoning.effort` to `medium` on **every** Luna request, including vision observations, comparisons, label extraction and text fallback. Do not use `none`, `low`, `high`, `xhigh` or `max` in this plan. Do not use an alias, ChatGPT web session, browser automation or a Luna coding-agent session as the runtime provider. Luna text fallback must follow the same safety, evidence-only and action-allowlist rules as Gemini.

Model-selection rule: Luna is the cost-sensitive, high-volume model (roughly the nano-tier role in earlier GPT-5 families), but this plan chooses `medium` reasoning effort everywhere for a consistent quality/latency tradeoff. Do not lower reasoning effort per feature to save tokens. Control cost through the fixed provider derivative, `detail: "low"` for ordinary face images, bounded output schemas, caching and quotas. Reasoning effort and image detail are separate controls: medium reasoning does not require sending a high-resolution image.

For each Luna Responses request, send the provider setting as `reasoning: {"effort": "medium"}` (or the exact equivalent required by the installed SDK) and assert it in the adapter tests. This applies to `vision_observation` and `text_reasoning_fallback`; Gemini configuration is independent and must not be given an OpenAI-only reasoning field.

Secondary intelligence model: `gemini-3.5-flash-lite`, already referenced in the repository, called through `google-genai` with text-only structured input. Keep it behind a separate policy switch because its free tier may not receive personal evidence. A future Gemini paid arrangement may use the same model or a separately verified model; never infer eligibility from model listing alone.

Sources:

- https://developers.openai.com/api/docs/models/all
- https://platform.openai.com/docs/quickstart/make-your-first-api-request
- https://developers.openai.com/api/docs/guides/images-vision
- https://developers.openai.com/api/docs/guides/your-data
- https://ai.google.dev/gemini-api/docs/models/gemini-3.5-flash-lite
- https://ai.google.dev/gemini-api/docs/pricing
- https://ai.google.dev/gemini-api/docs/rate-limits
- https://ai.google.dev/gemini-api/docs/structured-output

Before enabling either provider, record exact model ID, verification date, endpoint, image/text/structured-output support, account/project tier, region, retention/data controls, and effective RPM/TPM/RPD in `docs/release/ai-provider-eligibility.md`. Recheck at deployment and model changes. Listing a model through an SDK is not proof of account access, image support, free eligibility or permitted data use. If Gemini is unavailable after Luna vision succeeds, use Luna text reasoning; use the local fallback only if both language stages are unavailable.

Use a dedicated OpenAI project with a hard monthly spend budget and a dedicated Gemini project. App limits are additional guards, not proof of provider billing state. Do not use browser automation, key/project rotation, paid grounding, provider context caching, batch/flex/priority modes, file-search services, or quota evasion. Use standard Responses image/text generation only for Luna and standard text generation only for Gemini. Local database result caching remains allowed.

Proposed configuration, to add to `config.py` and a sanitized example environment file:

```text
GLOWUPAI_LUNA_ENABLED=0                  # 1 only after OpenAI review
OPENAI_API_KEY=                           # backend secret only
GLOWUPAI_LUNA_MODEL=gpt-5.6-luna
GLOWUPAI_LUNA_ENDPOINT=https://api.openai.com/v1/responses
GLOWUPAI_LUNA_REASONING_EFFORT=medium   # required for every Luna request
GLOWUPAI_LUNA_MONTHLY_SPEND_CAP_USD=0    # explicit cap; 0 disables
GLOWUPAI_LUNA_REASONING_FALLBACK=1       # only invoked after Gemini failure
GLOWUPAI_LUNA_IMAGE_MAX_EDGE=768         # provider derivative, not stored original
GLOWUPAI_LUNA_IMAGE_JPEG_QUALITY=82
GLOWUPAI_LUNA_IMAGE_DETAIL=low           # low | auto; verify model support first
GLOWUPAI_LUNA_LABEL_DETAIL=auto          # small label text may need more detail
GLOWUPAI_LUNA_ESCALATE_DETAIL=0          # no expensive retry unless explicitly enabled
GLOWUPAI_LUNA_IMAGE_ESTIMATED_COST_USD=0.001   # conservative reservation estimate
GLOWUPAI_LUNA_FALLBACK_ESTIMATED_COST_USD=0.0005
# Optional hard request/token/user limits are enforced before each call.
GLOWUPAI_GEMINI_ENABLED=1
GLOWUPAI_GEMINI_API_KEY=                  # backend secret only
GLOWUPAI_GEMINI_MODEL=gemini-3.5-flash-lite
GLOWUPAI_GEMINI_PERSONAL_DATA_ENABLED=1
GLOWUPAI_GEMINI_ELIGIBILITY_REVIEW_ID=replace_with_internal_approval_id
GLOWUPAI_AI_MAX_ATTEMPTS=2
GLOWUPAI_AI_TIMEOUT_SECONDS=30
GLOWUPAI_AI_DAILY_USER_LIMIT=0            # set only after quota review
```

Zero/missing quota or spend cap means that provider stage is disabled, not unlimited. Track Luna monetary usage and Gemini request/token quotas independently, with a proposed 20% operational reserve. Google applies quotas per project and daily quotas reset at midnight Pacific; use `America/Los_Angeles`, including daylight saving, for that quota window. App daily allowances may use UTC but must be separate counters. Never assume that paying for Luna creates free or unlimited Gemini capacity.

Start with proposed per-user allowances of one Luna image analysis, one shelf scan, one comparison and three Gemini questions per day; these are app limits, not provider allowances. Global provider budgets override user allowances. Estimate cost before enabling Luna: image detail, resolution, output ceiling and retries affect billed tokens. A paid Luna API does not make hosting, storage, or existing app subscriptions free; changing subscription entitlements is outside this migration.

## 4. Repository map and integration boundaries

Read these files before editing; current worktree contains substantial existing changes. Do not reset, overwrite, or clean unrelated work.

| Area | Existing locations | Required direction |
| --- | --- | --- |
| Providers | `backend/glowupai/google_ai.py`, `config.py`, new `openai_ai.py`, new `ai_orchestrator.py` | Keep Luna image calls and Gemini text calls behind separate adapters and one orchestration/policy boundary. |
| Main application | `api.py`, `complete_api.py`, `complete_service.py` | Preserve current app construction and inject the new service. |
| Capture processing | `service.py:process_analysis_job`, `capture_service.py`, `routers/captures.py` | Replace both initial analysis and reprocessing; preserve owner checks and upload idempotency. |
| Old inference | `metrics.py`, `ml_model.py`, `pipeline.py`, `preprocessing.py` | Remove runtime skin-score use after migration; audit indirect callers. |
| Jobs | `durable_jobs.py`, `jobs.py`, `analysis_jobs` and `jobs` tables | Bridge existing analysis jobs to durable execution; do not create an independent third queue. |
| Storage | `full_db.py`, `postgres_db.py`, `migrations/`, `photos.py`, `deletion.py`, `supabase_storage.py` | Add versioned observations and deletion coverage in SQLite and Postgres. |
| Guidance | `guidance_service.py`, `insights.py`, `attribution.py`, `catalog.py`, `analytics_service.py` | Consume validated Luna evidence; use Gemini only for bounded language reasoning under its data-policy mode. |
| Android contracts | `data/remote/GlowUpApi.kt`, `data/remote/dto/`, `domain/model/` | Add nullable observation fields and explicit availability states. |
| Android capture | `feature/capture/`, `data/work/CaptureOutboxProcessor.kt` | Preserve offline capture, retry identity, process-death recovery and live ML Kit guidance. |
| Android features | `feature/home/`, `feature/comparison/`, `feature/analytics/`, `feature/routine/`, `feature/insights/` | Replace misleading scores/charts and connect new results. |
| Other clients | `backend/web/`, `landing/` | Audit score/API consumers and product claims; do not redesign these apps. |

Existing `docs/contracts/capture-detail.md` specifies an owner-scoped capture read that survives Android process death. Extend it rather than bypassing that read with an in-memory result.

## 5. Target request flow

```text
Android capture/product scan
  -> existing authenticated backend upload
  -> local validation + owner + provider eligibility + consent checks
  -> persistent job/reference, no image bytes in job JSON
  -> cached Luna result lookup
  -> atomic OpenAI spend/quota reservation
  -> Luna adapter: one bounded image + structured-output request
  -> Luna schema + semantic validation
  -> persist minimal Luna evidence
  -> local data-classification gate
  -> Gemini adapter only when the evidence class and reviewed opt-in permit it
  -> Gemini schema + evidence-ID validation
  -> on Gemini failure: Luna text-reasoning fallback with evidence only
  -> if both fail: local factual renderer
  -> transactionally persist versioned result
  -> capture/job detail read -> Android result screen
```

Only the orchestrator can invoke the adapters. Callers cannot specify arbitrary models, endpoints, tools, system prompts or provider order. API keys never enter APKs, responses, logs or client configuration. Never send the raw image to Gemini in this architecture. Send personal Luna results only when the reviewed personal-data opt-in is enabled.

## 6. Contracts to freeze before feature work

Create `backend/glowupai/ai_contracts.py` with Pydantic models and matching Kotlin DTOs. Reject extra provider fields; validate ranges, enum values, string lengths and list sizes. Structured output constrains syntax but does not establish factual correctness.

### Observation envelope

```json
{
  "schema_version": "cosmetic-observation-v1",
  "analysis_id": "server-generated",
  "capture_id": "server-owned-reference",
  "status": "completed",
  "vision_provider": "openai",
  "vision_model_id": "gpt-5.6-luna",
  "vision_reasoning_effort": "medium",
  "language_provider": "gemini",
  "language_model_id": "gemini-3.5-flash-lite",
  "language_fallback_provider": "openai",
  "language_fallback_model_id": "gpt-5.6-luna",
  "language_fallback_reasoning_effort": "medium",
  "language_mode": "gemini_primary",
  "prompt_version": "skin-vision-v1",
  "image_input": {
    "preprocessing_version": "face-derivative-v1",
    "width": 768,
    "height": 768,
    "bytes": 0,
    "detail": "low"
  },
  "quality": {"usable": true, "issues": []},
  "observations": [
    {
      "region": "left_cheek",
      "concern": "visible_redness",
      "visibility": "visible",
      "extent": "localized",
      "description": "A small area looks redder than the surrounding visible skin."
    }
  ],
  "limitations": ["Lighting can affect apparent redness."],
  "summary": "A short description of visible cosmetic appearance."
}
```

Luna vision returns only the quality/observations/limitations/summary body. Gemini may return wording and allowed actions but cannot change observations. On Gemini failure, Luna text reasoning may return wording and allowed actions from the same evidence packet; it cannot change observations. The server creates identifiers, timestamps, ownership and provenance; never trust provider-supplied IDs. Persist each stage's model, prompt, endpoint, data class, policy decision and provider request ID (if safe) separately. Record `language_mode` as `gemini_primary`, `luna_fallback`, `local_fallback` or `unavailable`.

Enums:

- Regions: `forehead`, `left_cheek`, `right_cheek`, `nose`, `chin`, `whole_face`; left/right refer to the subject's anatomy, not screen position. Start with region cards, not precise overlays.
- Concerns: `visible_redness`, `visible_spots`, `uneven_tone`, `visible_texture`. No medical labels.
- Visibility: `visible`, `not_visible`, `uncertain`, `not_assessable`.
- Extent: `localized`, `widespread`, `uncertain`, `not_assessable`. No implicit 0–100 mapping.
- Quality issues: `blur`, `lighting`, `pose`, `occlusion`, `multiple_faces`, `no_face`, `resolution`, `possible_filter`, `other`.
- Limit observations to 24, each description to 240 characters, summary to 600 characters, limitations to 8 items of 180 characters. These are proposed contract limits.

Do not display self-reported model confidence as calibrated probability. Derive a simple evidence-availability label from quality and assessability; label it accordingly.

### Status and errors

API `analysis_status`: `not_requested`, `queued`, `running`, `completed`, `needs_retake`, `unavailable`, `failed`. `unavailable_reason`: `provider_disabled`, `policy_restricted`, `consent_required`, `quota_exhausted`, `model_unavailable`. Include nullable `retry_after_seconds` only when meaningful. Do not report policy restriction as a transient error.

Jobs retain their existing generic statuses; feature availability is returned in a typed job result. Add scheduled retry metadata to the existing runner instead of making clients understand two competing retry state machines. Capture storage success and analysis success are separate facts.

### Comparison

Input: two owned capture IDs, no arbitrary image URL. Luna receives the two images only when OpenAI policy/consent permits and returns `comparable`, `reasons`, and per-region/concern `change` in `less_visible`, `similar`, `more_visible`, `uncertain`. Gemini may turn that validated result into a short explanation only when its tier permits the data class. If incomparable, no directional changes. Photo order and timestamps are supplied by the server. Do not compute percentages from qualitative categories.

### Products and assistant

Product extraction returns `name`, nullable `brand`, category, verbatim label text, ingredient list, unreadable fields, and `needs_confirmation`. Unknown is null/empty, never a guessed fact. User confirms before database product/routine mutation.

Assistant response returns `answer`, `evidence_ids`, `limitations`, and optional `suggested_actions` from an allowlist such as `open_product`, `log_routine`, `retake_photo`. Luna is never asked to perform routine changes. Gemini may write the answer from the validated Luna evidence packet only when allowed. Validate evidence IDs against the supplied set; links come from server catalog entries. No arbitrary URLs, purchases, routine edits or autonomous tool calls.

## 7. Persistence, compatibility and job reliability

Add migrations using the next available number at implementation time; current migrations already reach `0010`. Implement equivalent test schema changes in `full_db.py`; do not assume adding a SQL file updates SQLite.

Proposed tables:

- `ai_analyses`: id, user_id, capture_id, task_type, schema_version, vision_provider, vision_model_id, vision_reasoning_effort, language_provider, language_model_id, language_reasoning_effort, prompt_version, preprocessing_version, input_digest, data_class, policy_decision, status, validated_vision_json, validated_language_json, safe_error_code, created_at/completed_at. Unique work identity includes capture, task, provider/model pair, reasoning effort, prompt and preprocessing version.
- `ai_comparisons`: id, user_id, earlier_analysis_id, later_analysis_id, versions, validated_result_json, timestamps, unique ordered pair/version identity.
- `ai_usage_reservations`: id, provider, project_alias, model_id, user_id, task, request identity, quota window, reserved tokens, estimated/actual cost, attempt number, state and timestamps. Store no key, image bytes or provider payload. Luna reservations enforce money; Gemini reservations enforce request/token quota and tier.
- `ai_policy_consents`: user_id, policy version, scope, granted/revoked timestamps; this records consent separately from deployment eligibility.
- Extend existing jobs with `next_attempt_at` if absent; index due jobs. Prefer existing conversation/product tables over creating duplicate feature stores.

Persist capture and intended work atomically, or use an existing durable outbox. Prevent a stored photo from being stranded if the process dies before job submission. Workers must recheck consent, eligibility and deletion immediately before provider invocation and before writing results.

Use DB transactions for quota and spend reservations across workers. Never hold a DB transaction during network calls. Reserve conservatively for Luna image input and maximum output, reconcile actual usage when returned, and retain conservative spend for ambiguous timeouts. Reserve Gemini text tokens separately. Include provider attempts, including errors, in both budgets. Unknown token/cost must fail closed or use a documented conservative bound. Do not let a free Gemini quota failure cause a second Luna image call unless a new reservation is made.

One service owns retry counting across SDKs and the worker: disable hidden SDK retries. A normal task has one Luna vision call and one Gemini language call. If Gemini fails, make at most one Luna text-reasoning fallback call using evidence only; the logical task has at most three external calls total. A transient retry may replace an attempt within that ceiling, not add an unbounded call. Retry transient 5xx/transport failures with jitter; respect each provider's 429 information; stop Gemini until its reset on daily quota exhaustion; never retry policy blocks or invalid schema automatically. A timeout can mean a provider processed the request; exactly-once external execution is not guaranteed. Unique persistence prevents duplicate results, not all duplicate calls. If both language attempts fail, retain Luna evidence and render factual content locally.

Cache validated Luna and Gemini results separately, scoped to owner and complete input/version identity. Include data-class policy, routine/profile revision and both provider versions in language cache keys. A Gemini language cache may never be reused across users, even when the text looks identical, unless it contains only a versioned public catalog fact. Revoke/delete associated caches and queued work with user deletion. Product reference facts may be shared only after personal content is excluded.

Keep historical `metric_snapshots` readable as legacy results. New analyses do not insert invented numeric values or zero placeholders into that table. Update joins so captures without numeric snapshots still appear. Add an `analysis` envelope to capture detail/history; retain nullable old metric fields for compatibility. Separate old metric charts from new qualitative observations, with no trend connecting the two.

## 8. Feature implementation details

### A. Capture and observations

Preserve on-device ML Kit quality coaching. Validate MIME by decoded content, byte and pixel limits, EXIF rotation, decompression limits, single-face framing, server brightness and sharpness. Do not trust client quality metadata alone. Avoid CLAHE, smoothing, whitening, or aggressive color correction before cosmetic analysis: they change what is being observed.

Keep the user's original upload in the existing private storage path, subject to the existing retention/deletion policy. Create a separate provider derivative and send only that derivative to Luna: orient it, crop the largest detected face with a fixed 10–15% margin, strip EXIF/metadata, resize the longest edge to a fixed 768px (or the value in configuration), and encode as JPEG quality 82. Record `preprocessing_version`, derivative dimensions and a digest; never upscale a low-resolution source. Use the same derivative recipe for every capture and both images in a comparison so changes reflect the user rather than resizing differences.

Set the Luna image input `detail` explicitly to `low` for ordinary face observations and comparisons, after verifying that the exact model supports it. OpenAI documents `low` as the context-efficient image mode and says it uses fewer input tokens; exact token usage must be measured from the response for this model. A fixed face crop plus low detail is the cost-saving default, not a promise that fine blemishes remain visible. If a quality gate or evaluation shows that a concern is not assessable, return `needs_retake` rather than silently escalating. An optional, separately budgeted escalation may retry once with `auto`/higher detail only when `GLOWUPAI_LUNA_ESCALATE_DETAIL=1`; it must be recorded as a new attempt and never happen automatically on every capture.

Product-label scanning is a separate task: use a user-guided tight label crop and a fixed maximum edge (start at 1024px) with `auto` detail because tiny text needs resolution. Do not send the full camera photo when a label crop is available. Keep label scans infrequent, cache by derivative digest, and use `low` only for packaging identification where text is not required. The label-detail exception is deliberate because aggressively reducing it can increase transcription errors and cause costly recaptures.

Record the requested detail, actual dimensions, image bytes and provider-reported token usage for every Luna call. Run a paired consistency evaluation before changing the defaults: repeated same-condition captures should agree on categorical observations, while changed lighting/pose should trigger `uncertain` or `not_assessable` where appropriate. Cost reduction is accepted only if the consistency and abstention gates pass.

Analyze once after a user submits a capture: Luna receives the provider derivative; Gemini receives the resulting validated evidence only when permitted. Never call either provider on every camera frame or screen refresh. A rejected capture explains retake reasons and consumes no provider quota when rejected locally. Unavailable analysis still allows an appropriate stored-photo/history experience, without fabricated observations. If Gemini is unavailable, do not repeat the Luna image call; invoke Luna text reasoning with the existing evidence and retry language generation independently.

### B. Region-specific results

Show named region cards and the observation text. Support `not_assessable` visibly. A polished anatomical illustration may navigate regions, but do not imply pixel-accurate lesion localization. Exact bounding boxes and heatmaps are deferred until independently evaluated. Preserve subject-left/right semantics after selfie mirroring.

### C. Progress

Require matching task/schema/preprocessing/model/prompt versions for automatic comparison. Choose the nearest suitable earlier capture in a proposed 7–35 day window; allow explicit user selection. Evaluate pose, lighting, filter/makeup reports and visibility before direction. If fewer than two suitable captures exist, explain what is missing. Model changes start a new series; do not bulk reprocess historical photos without explicit intent, policy eligibility and a quota budget.

Display dated comparison cards and a categorical timeline. Weekly summaries aggregate already stored Luna results locally; at most one cached Gemini wording request per evidence revision when permitted. No continuous numerical curves or promises of product effectiveness.

### D. Product scanning

Extend existing shelf-scan job and confirm flow. Offer front-label and ingredient-label capture separately; retain the relationship to the same product. Encourage a close-up on a plain background. Route unreadable text to recapture/manual entry. Never invent ingredients from a familiar brand name. Distinguish exact packaging transcription from catalog enrichment and user correction.

### E. Routine assistant and ingredient reference

Expand the existing `guidance_service.py` evidence builder. Store user goals, budget, products, preferences and reported reactions locally. Build a data-classification step before Gemini: `public_catalog`, `product_label_unlinked`, `user_derived_observation`, `user_history`, or `sensitive`. Public/catalog evidence is allowed; personal evidence requires the explicit reviewed opt-in plus its retention and consent checks. In restricted mode, the local renderer handles personal Luna results; do not pretend generic answers are personalized.

For a permitted personal workflow, supply Gemini a bounded, owner-scoped evidence bundle containing confirmed products, approved catalog facts, validated Luna observations, routine changes and user-reported context. Mark each item with its source and evidence ID. Keep system instructions separate from untrusted questions, OCR and product text. Treat text inside images and Luna output as data, never instructions. Never send the original image in the second stage.

Create a small versioned ingredient reference with source URL, reviewed date, cosmetic purpose and limitations. Gemini explains supplied facts; it does not author authoritative ingredient rules. Unsupported properties yield insufficient evidence. Use existing prices only if timestamped and explicitly provided; no invented live prices or stock. Advice requiring medical judgment is outside scope.

### F. Routine timeline and recaps

Join existing routine events, check-ins and Luna comparison cards by time locally. Say a change occurred after a routine event, not because of it. Multiple simultaneous changes produce an ambiguity note. Do not rename temporal association to root cause. Gemini can phrase the evidence only; it cannot establish causation. Audit existing `RootCauseScreen`, product predictions, effectiveness cards and exports for unsupported claims after numeric analysis is removed. Retain factual routines and user notes even when either provider is unavailable.

## 9. Luna task packets

Execute in dependency order. Each packet is one focused implementation handoff; if too large for one turn, split backend/UI while retaining the same frozen contract. Parallel work is optional only when explicitly authorized; shared contract, migration and dependency edits require one owner. Do not spawn agents merely because this plan mentions agents.

### L01 — Freeze contracts and deployment eligibility

Dependencies: none. Files: this plan, new `docs/contracts/ai-v1.md`, new `docs/release/ai-provider-eligibility.md`, existing capture contract.
Work: verify callable OpenAI `gpt-5.6-luna` access, image/structured-output support, account data controls, endpoint and spend controls; verify Gemini model/tier/data policy separately; document the Luna-paid/Gemini-free restricted mode and the Gemini-paid personalized mode; freeze schemas/statuses/API additions; inventory numeric consumers with `rg`.
Acceptance: no claim that a ChatGPT subscription grants API access; no claim that paying for Luna makes free Gemini personal-data use permitted; every current analysis entry point and client is listed; missing credentials do not block writing contracts.

### L02 — Typed schemas and migrations

Depends: L01. Files: new `ai_contracts.py`, next migration(s), `full_db.py`, `postgres_db.py`, new contract/migration tests.
Work: implement schemas/tables/indexes and owner-scoped persistence helpers; use existing DB transaction conventions.
Acceptance: upgrade from populated legacy database preserves data; SQLite/Postgres parity; invalid enums/extra fields rejected; duplicate result identity prevented.

### L03 — Provider adapter and policy gate

Depends: L02. Files: new `openai_ai.py`, `google_ai.py`, `config.py`, new `ai_policy.py`, `test_google_ai.py`, new `test_openai_ai.py`.
Work: implement injectable OpenAI Responses client for Luna image input and injectable Gemini client for text-only postprocessing; enforce exact model allowlists, separate system instructions, structured requests, `reasoning.effort="medium"` on every Luna request, bounded image detail/output, safe errors and data-class policy. Build the deterministic provider derivative before the client call, set `detail` explicitly, and record dimensions, bytes, reasoning effort and usage. Move existing explanation and shelf calls through the orchestration boundary. Add a request spy proving the raw stored upload is never sent and only the derivative is sent to Luna.
Acceptance: policy-disabled, absent-key, disallowed-model, wrong-tier and personal-data-blocked cases make zero network calls; no API secrets in results/logs; fake clients cover blocked, empty, malformed, refusal, truncated and instruction-injection responses; no raw provider response persisted; repeated identical uploads produce the same derivative digest, detail and `medium` reasoning settings; any Luna request using another reasoning effort is rejected by tests.

### L04 — Shared quotas and durable execution

Depends: L02–L03. Files: new `ai_quota.py`, new `ai_orchestrator.py`, `durable_jobs.py`, `complete_service.py`, new quota/orchestration tests, existing durable job tests.
Work: run Luna vision first and Gemini second only when policy permits; invoke Luna text reasoning only after Gemini failure; implement transactional Luna spend for both task modes and Gemini quota reservations, due-time scheduling, unified attempts, stage-specific cache keys, ownership-aware workers and provider telemetry.
Acceptance: two concurrent workers cannot spend the last Luna budget or Gemini allowance twice; restart preserves limits; daily reset handles DST; Gemini 429 triggers at most one evidence-only Luna fallback subject to a new reservation; replay does not duplicate stored results; ambiguous timeout remains budgeted; Luna success plus Gemini failure returns Luna-generated language when available, then local factual fallback.

### L05 — Capture pipeline replacement

Depends: L03–L04. Files: `service.py`, `capture_service.py`, `routers/captures.py`, `complete_service.py`, preprocessing call sites, capture tests.
Work: route initial and reprocess jobs to Luna observations followed by optional Gemini wording and Luna text fallback; preserve upload idempotency; bridge existing analysis jobs; separate stored capture from analysis status; remove skin-score fallback from these paths. Store the original separately and send only the fixed derivative to Luna.
Acceptance: capture survives process restart; owned detail reflects completion; local invalid image makes no provider call; blocked analysis has null legacy metrics; no `metrics.analyze` or MobileNet invocation on migrated paths; Gemini failure invokes at most one Luna evidence-only fallback and never resubmits the image; provider dimensions/detail are stable across reprocessing.

### L06 — Android observation experience

Depends: L01 contracts; integrate after L05. Files: Capture DTO/domain/repository, capture result ViewModel/screens, outbox, home metric grid, relevant unit tests.
Work: typed new fields, unknown-enum handling, polling with backoff and lifecycle cancellation, region cards, retry/retake/unavailable UI; maintain live camera checks.
Acceptance: process death reloads by capture ID; offline retry reuses idempotency key; unknown server fields do not crash; quota and policy states are distinct; null metrics never display zero scores.

### L07 — Comparison and progress

Depends: L05–L06. Files: new `comparison_service.py`, capture/analytics routes as appropriate, comparison DTO/repository/screens, home history and analytics components.
Work: owner-scoped pair endpoint and job; Luna performs one bounded two-image request; Gemini optionally phrases the validated comparison; implement comparability rules and qualitative timeline/version boundaries.
Acceptance: cross-owner pair rejected before image read; incomparable pair has no directional verdict; model change breaks the series; repeated request uses cached result; one-photo account has a useful empty state; Gemini failure leaves the Luna comparison visible.

### L08 — Product scan verification

Depends: L03–L04. Files: `guidance_service.py`, `openai_ai.py`, `google_ai.py`, shelf endpoints, ShelfScan DTO/ViewModel/screen, catalog mapping.
Work: Luna performs structured label transcription from product-only images; Gemini optionally normalizes/explains the validated text; implement product-only eligibility flow, two-image support, draft corrections and confirmation.
Acceptance: unreadable label creates no invented ingredient; scan itself never modifies a routine; confirmation is idempotent; timeout/quota state recoverable; every label scan reserves the Luna spend cap; personal-content fixture makes no external call; Gemini never sees the label image.

### L09 — Grounded assistant and catalog

Depends: L03–L04 and L08; personal observation integration after L07 and eligibility clearance. Files: `guidance_service.py`, `catalog.py`, `insights.py`, `safety.py`, Qna DTO/repository/ViewModel/screen, new versioned catalog resource.
Work: reference retrieval, bounded evidence bundle, evidence-ID validation, Gemini unpaid generic mode, explicitly gated Gemini-paid personalized mode, Luna text-reasoning fallback, local factual fallback, source cards and action suggestions.
Acceptance: unknown ingredient property returns insufficient evidence; injected OCR cannot alter instructions; invalid evidence ID invalidates response; user A's question cannot retrieve B's history; personal payload remains blocked in unpaid restricted mode; Gemini failure invokes Luna text reasoning before any local fallback; Luna evidence is not discarded when Gemini is unavailable.

### L10 — Routine timelines and weekly recaps

Depends: L07–L09. Files: `attribution.py`, `analytics_service.py`, `guidance_service.py`, routine timeline, weekly/monthly recap, root-cause and effectiveness UI.
Work: factual local event aggregation, cautious temporal wording, revision-keyed optional Gemini explanation; remove numerical formulas that presume provider outputs are measurements.
Acceptance: multiple routine changes produce uncertainty; cached recap costs no additional call; no data produces no invented trend; provider disabled still shows factual timeline.

### L11 — Privacy, deletion and client compatibility

Depends: L05–L10. Files: `deletion.py`, user export path, consent/settings/legal UI, `backend/web/` API consumers, landing claim text.
Work: delete Luna/Gemini analyses, comparisons, caches and queued work; block worker writes after deletion/revocation; export both provider provenance and policy decisions; render legacy/new data separately; align product claims.
Acceptance: deletion during either provider call cannot resurrect data; revoked consent prevents the next call; OpenAI/Gemini provider retention is not misrepresented as locally deletable; old-client behavior is tested and either safely compatible or explicitly version-gated.

### L12 — Remove obsolete runtime dependencies

Depends: L11 and passing migration tests. Files: `metrics.py`, `ml_model.py`, pipeline callers, dependency manifests/container configuration and tests as discovered.
Work: remove unreachable runtime ML initialization and numeric fallback; add OpenAI SDK dependency and remove unused Torch runtime dependency only after callers are gone; preserve legacy data serializers as needed. Archive training scripts/weights only if separately authorized; do not delete user artifacts as cleanup. Audit `api_legacy.py` and reprocessing for alternate entry points.
Acceptance: production app starts without torch; no live analysis path produces legacy scores; historical reads still work; error fallback is unavailable/local factual display, not old model inference; provider keys are backend-only.

### L13 — Evaluation and rollout report

Depends: all integration tasks. Files: new `backend/tests/evaluation/`, `docs/release/ai-evaluation.md`, release configuration/runbook.
Work: run offline dual-provider contract tests, permitted live product evaluation, reliability/cost tests and a clearly separated face evaluation protocol. Compare Luna vision + Luna text fallback, Luna + Gemini, and both-provider-failure local output. Record results, failures, per-stage latency/cost, provider retention and feature flags.
Acceptance: no launch approval from mocked tests alone; OpenAI access/data controls and Gemini tier/terms are explicitly reported; disabled personal Gemini features remain disabled; rollout/rollback documented; no user-image test fixture is sent to Gemini unpaid. Do not deploy merely because implementation is complete.

## 10. Evaluation and release criteria

All ordinary tests use an injected fake provider; CI must not consume quota or send personal content. Add opt-in live tests requiring a separate flag, bounded request budget, eligible project and permitted fixtures.

Proposed initial engineering gates, to be measured rather than advertised:

- 100% of offline ownership, consent, disabled-provider, quota, deletion and idempotency scenarios pass.
- Zero calls to an unapproved provider/model/endpoint in request-spy tests; Luna spend is always bounded by the configured cap.
- Product evaluation: at least 30 permitted packaging cases with readable and deliberately unreadable labels; target 95% precision for extracted readable ingredient names and zero fabricated ingredients on the curated unreadable set. Report sample size and failures, not universal accuracy.
- Face validation, only under a permitted arrangement: at least 50 adults with representative skin tones/devices, repeated captures and changed-lighting pairs, independently reviewed cosmetic labels, and subject-disjoint development/holdout sets. Compare the fixed 768px derivative with the original/auto-detail baseline and record per-concern agreement, abstention, bytes and billed input tokens. These are pilot numbers, not clinical validation.
- Proposed pilot targets: at least 90% same-condition categorical agreement and at least 90% abstention on deliberately incomparable pairs. Report subgroup denominators, uncertainty and reviewer disagreement. If evidence is insufficient or thresholds fail, keep the affected feature off; do not solve failure by changing display wording alone.
- Measure p50/p95 latency for each stage and end-to-end, attempts/stage, invalid-output rate, derivative bytes/dimensions, requested detail, `medium` reasoning effort, Luna image/output tokens and estimated cost/task, Gemini tokens/task, quota denials and fallback-mode rates. Accept the reduced image only if it preserves the agreed consistency/abstention gates while reducing measured Luna input usage. A proposed 30-second per-provider timeout is an operational limit, not a guaranteed response time.

Capacity planning has two independent budgets. For Gemini, let usable RPD be `Dg`, mean Gemini calls per active user/day be `cg`, and reserve fraction be `r`; request-bound capacity is `floor(Dg * (1-r) / cg)`, with token limits possibly lower. For Luna, calculate a monthly dollar ceiling from the exact model's current token prices, image detail and output ceiling; reserve 20% for retries and ambiguity. Example values are planning arithmetic only, not published allowances. Show wait/unavailable states instead of promising unlimited AI. Never spend Luna budget to recover a Gemini quota failure without a new explicit reservation.

Verification commands, run from the indicated directories using the installed toolchain and repository CI conventions:

```sh
# backend/: targeted tests first, full suite at integration
python -m pytest tests/test_openai_ai.py tests/test_google_ai.py tests/test_durable_jobs.py tests/test_api.py
python -m pytest tests/

# repository root, using the configured Android Studio JDK if needed
./gradlew testDebugUnitTest lintDebug assembleDebug
```

Add new test filenames to targeted commands as packets introduce them. Run real PostgreSQL integration checks for locking/migrations; SQLite passing alone is insufficient. Browser/client builds are required if those files change. Preserve existing CI checks; document environmental failures separately from regressions.

Rollout: both providers disabled + mocks -> paid-Luna internal pilot with Luna text fallback -> product-only Gemini free pilot -> Gemini-paid personalized pilot only after its terms/data review -> controlled user beta. Kill switches independently disable Luna images, Gemini language or the whole chain while existing photos/routines remain accessible. Reverting the AI feature does not restore misleading scoring. Roll back app routing/config, not destructive data migrations.

## 11. Copyable Luna handoff

```text
Implement packet LXX from docs/GEMINI_FREE_TIER_IMPLEMENTATION_PLAN.md.
Read that packet, its dependencies, frozen contracts and relevant existing files first.
Inspect git status and preserve all existing unrelated changes.
Implement only this packet. Do not deploy, send personal data, exceed configured
provider budgets, or broaden scope. Do not activate billing or change model IDs
unless the packet explicitly requires that provider configuration and the plan's
eligibility gate has been completed. Use fake provider fixtures by default.
Preserve owner checks, null semantics, idempotency and legacy history.
Run relevant tests and existing required checks for changed components.
If a contract is missing, document the smallest proposed addition before dependent edits.
Report changed files, behavior, tests actually run, remaining failures and blockers.
Do not report a mocked result as real model accuracy or production readiness.
```

Completion means the replacement code and all six feature paths are integrated and tested, old inference has no active callers, Luna spend and Gemini tier usage are enforced, each provider's policy gate is testable, and release gates are honestly reported. Code completion, provider account access and permission to launch personal-photo processing are separate outcomes.
