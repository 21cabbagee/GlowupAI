# GlowupAI frontend API map

This is the integration contract for the complete GlowupAI surface as it exists
in `glowupai/complete_api.py` and `glowupai/complete_service.py` (the former
`complete_api_runtime`/`complete_service_runtime` subclass layer was folded
into these two modules; there is no longer a separate runtime layer). The
default application exposed by `glowupai.api:app` is `complete_api.app`
directly. The fallback page is `glowupai/static/index.html`; it calls these
endpoints directly from the browser. The typed Next client in `web/lib/api.ts`
is an alternate consumer of the same surface.

The field notes below prioritize fields read by the current static UI and the
typed web client. The backend often returns additional database columns; a
client should ignore unknown fields.

## Contract conventions

- Base URL: same-origin `/api`. The static page assumes it is served by the
  FastAPI app. The Next app proxies `/api/*` to FastAPI.
- Request and response format: JSON, except the image is a base64 string inside
  JSON. Send `Content-Type: application/json` for JSON requests.
- Timestamps are ISO-8601 strings, normally UTC with a `Z` suffix. Render in
  the device locale but store and compare as UTC.
- By default the API still has no enforced authentication: the client carries
  a `user_id`, and every user-scoped route trusts the path/query/body value as
  given. An optional auth boundary exists (see "Authentication" below) but is
  off unless the deployment sets `GLOWUPAI_AUTH_REQUIRED=1`. Even when it is
  on, treat `user_id` as sensitive and do not log it or put it in analytics
  URLs unnecessarily.
- The only valid appearance vertical is `skin`.
- `PermissionError` becomes HTTP `403`. Service `ValueError` becomes HTTP
  `400`. Pydantic body/query validation becomes HTTP `422`. A successful delete
  returns `204` with no body.
- Error payloads normally look like `{"detail": "..."}`. Capture quality
  failures use an object in `detail`, for example
  `{"detail":{"message":"capture quality is below the acceptance threshold","quality":{...}}}`.
  Validation errors use FastAPI's array of `{loc,msg,type}` objects.
- Missing users, products, experiments, and threads are currently `400
  "... not found"`, not `404`.
- Premium means both `entitlement.plan == "premium"` and
  `entitlement.status == "active"`. Premium routes return `403` otherwise.
- The complete app's `POST /api/users` returns a profile-shaped object. The
  older legacy app (`glowupai/api_legacy.py`, used only by its own test) returns
  only a user object. Frontend integration must point at `glowupai.api:app`
  and must not mix the two contracts.
- **Commerce is no longer Premium-gated.** `GET .../commerce/offers` and the
  offer-click route are available to every consenting user, free or premium;
  do not show an upgrade gate around them.
- **Product verdicts use a one-free-lifetime-unlock model, not a boolean.**
  A free user's dashboard verdicts array can contain entries shaped
  `{"product_id","product_name","label":"locked","generated_text":"Upgrade to
  Premium to see this verdict and get unlimited product verdicts.","evidence":{}}`
  once a definitive verdict has been reached for more than one product.
  `evidence_unclear` verdicts are never locked — there is nothing to withhold
  there. `dashboard().features.product_verdicts_unlocked` is `true` once the
  free unlock has happened (or the user is Premium). Render `label === "locked"`
  as a distinct upsell card, not as a normal verdict.
- **Reprocessing and shelf-scan are asynchronous.** They return
  `{"job_id","status":"queued"}` immediately; poll the matching `GET .../{job_id}`
  status route (documented under each feature below) rather than blocking on
  the POST response.

## Sequential product workflow

The API does not expose an onboarding-progress field. The client must derive
the next step from the following state and must not show the full product shell
before the state is ready:

```text
welcome
  -> profile_created       POST /api/users
  -> consent_required      GET profile, then POST /consent
  -> baseline_needed       GET /capture-guide, then POST /captures
  -> measured              GET /dashboard /history
  -> routine_started       POST /products, then POST /routine-events
  -> evidence_window       repeat guided captures every 3–7 days
  -> premium_unlocked      subscription upgrade, then experiments/Q&A/insights
```

After every mutation, refetch the smallest authoritative resource needed for
the next screen. Do not infer consent, baseline, premium, or experiment state
from a button click alone.

## Startup and session recovery

### `GET /api/health`

- Minimal payload: none.
- Response fields used: `status`, `version`, `scope`, `features`.
- Prerequisites: the API server is reachable.
- Expected errors: network failure; normally no application error.
- Ideal UI state: use as a short startup gate. Show a non-blocking offline or
  unavailable state if it fails; do not create a profile until the API is
  reachable. `scope` should be shown as cosmetic tracking, never diagnosis.

On app boot, load a locally persisted `user_id` only as a candidate. Call
`GET /api/users/{user_id}/profile`; if it returns `400 user not found`, clear
only the GlowupAI session key and restart at `welcome`. The static page
currently leaves a stale ID in local storage after this error.

## Authentication

The backend added a Firebase Auth boundary on top of the existing `user_id`
model (Android Phase 1). It is intentionally opt-in via config so the
pre-existing static/typed web clients and the full pre-auth test suite keep
working unchanged; a client built against this contract should send a bearer
token on every request regardless, so it keeps working once a deployment
turns the flag on.

### Config flags (server-side, not client-visible)

- `GLOWUPAI_FIREBASE_PROJECT_ID` — the Firebase project whose ID tokens are
  accepted. Required for `POST /api/auth/session` and for ownership
  enforcement to do anything other than fail closed.
- `GLOWUPAI_AUTH_REQUIRED` — default **off**. When unset/false, every route
  behaves exactly as documented elsewhere in this file: no header is checked,
  `user_id` is trusted as given. When set to a truthy value, every
  user-scoped route requires a valid bearer token whose Firebase uid owns the
  `user_id` in play.
- `GLOWUPAI_ADMIN_TOKEN` — gates the three `/api/admin/*` routes
  independently of the flag above (see "Admin routes" below).

### `POST /api/auth/session`

- Header: `Authorization: Bearer <firebase-id-token>`. No body.
- Response: the same profile shape as `GET /api/users/{user_id}/profile`
  (`{user, appearance_profiles, entitlement, verticals, experience_profile}`).
- Behavior: verifies the Firebase ID token (signature, `exp`, `iss`, `aud`
  against `GLOWUPAI_FIREBASE_PROJECT_ID`), then looks up a user by the
  token's `sub` (Firebase uid). On first sight it creates the user, the skin
  appearance profile, and the free entitlement — the same side effects as
  `POST /api/users` — and binds the uid. Every later call with a token for the
  same uid returns the same user; it never creates a duplicate. This is the
  route an Android client should call right after Firebase sign-in, replacing
  the current `POST /api/users` call from a fresh account state.
- Expected errors: `401` for a missing, malformed, expired, wrong-audience,
  wrong-issuer, or otherwise unverifiable token; `401` if
  `GLOWUPAI_FIREBASE_PROJECT_ID` is not configured on the server (fails
  closed rather than accepting an unverifiable token).
- Ideal UI state: call this immediately after Firebase Google/email-password
  sign-in succeeds, persist the returned `user.id` the same way the client
  already persists it after `POST /api/users`, then continue the existing
  sequential workflow (consent, baseline, etc.) unchanged.

### Ownership enforcement on user-scoped routes

When `GLOWUPAI_AUTH_REQUIRED=1`, every route that is scoped to a `user_id` —
whether it appears in the path (`/api/users/{user_id}/...`), a query
parameter (`GET /api/products/{id}?user_id=...`), or the JSON body
(`POST /api/routine-events`, `POST /api/experiments`) — requires
`Authorization: Bearer <firebase-id-token>` and checks that the token's uid
is bound (via `POST /api/auth/session`) to that exact `user_id`.

- Missing or unverifiable token on a protected route: `401`.
- Valid token, but it belongs to a different `user_id` than the one in the
  request: `403`.
- Left open regardless of the flag (no user to scope to, or needed before a
  session exists): `GET /api/health`, `POST /api/triage`,
  `GET /api/products/search`, `GET /api/products/lookup`, `POST /api/users`,
  `POST /api/auth/session`, the static asset/root routes. `POST /api/products`
  is also unauthenticated because product rows are global and carry no
  `user_id` at all (see trap #7 below).
- When the flag is off (default), none of the above checks run and every
  route behaves exactly as it always has — no header required, `user_id`
  trusted as given.

### Admin routes

`GET /api/admin/audit`, `POST /api/admin/offers`, and
`GET /api/admin/measurement-feedback` previously had **no protection at all**
regardless of `GLOWUPAI_AUTH_REQUIRED` — `GET /api/admin/audit` in
particular leaked the global audit log to any caller (see the trap noted
below under that route). All three now always require
`Authorization: Bearer <GLOWUPAI_ADMIN_TOKEN>`, checked with a
constant-time comparison, independent of the per-user auth flag:

- `GLOWUPAI_ADMIN_TOKEN` unset on the server: all three routes refuse every
  request with `403`, token or not. There is no "open" fallback.
- Token configured but missing/wrong in the request: `403`.
- Token configured and matching: the route behaves as documented elsewhere in
  this file.

No user-facing client should ever hold `GLOWUPAI_ADMIN_TOKEN` or call these
routes; they exist for operator tooling only. The Android app must not call
`GET /api/admin/audit` for a personal timeline, exactly as this document
already warned before the boundary existed.

## Onboarding, profile, and consent

### `POST /api/users`

- Minimal payload: `{}`. The current clients send
  `{"skin_type":"combination"}` or `{"skin_type":null}`.
- Response fields used: `user.id`, `user.skin_type`,
  `user.consent_state`; `entitlement.plan/status`; `verticals`; and, for a
  complete profile render, `appearance_profiles[].vertical`.
- Actual complete response: `{user, appearance_profiles, entitlement,
  verticals}`. `user.consent_state` starts as `"pending"`; entitlement starts
  as free/active; the skin appearance profile is created.
- Prerequisites: none. The backend currently does not require an auth
  session, consent, or a unique client token.
- Expected errors: `422` for a non-string `skin_type`; `500` for an
  infrastructure/database failure. There is no idempotency protection, so a
  double submit creates two profiles.
- Ideal UI state: a single “Create my private profile” step. Disable the CTA
  while pending, persist `user.id` only after the response, then transition to
  `consent_required`. Do not send the user to a tabbed dashboard yet.

### `GET /api/users/{user_id}/profile`

- Minimal payload: no body; `user_id` is the path parameter.
- Response fields used: `user.id`, `user.skin_type`, `user.consent_state`,
  `user.created_at`; `appearance_profiles[].vertical` and
  `appearance_profiles[].baseline_capture_id`; `entitlement.plan`,
  `entitlement.status`, `entitlement.renews_at`, `entitlement.source`; and
  `verticals`.
- Prerequisites: a live, non-deleted user ID.
- Expected errors: `400 user not found`. A deleted or stale local ID reaches
  the same error.
- Ideal UI state: the authoritative session snapshot. Render the next step
  from it: `pending/declined` consent means consent screen; active consent with
  no history means baseline capture; active consent plus history can
  open the workspace. Refresh it after consent, upgrade, cancel, and delete.

### `POST /api/users/{user_id}/consent`

- Minimal payload: `{"facial_data":true}`. Optional
  `policy_version` records the policy version accepted by the user.
- Response fields used: the same profile fields as the profile endpoint,
  especially `user.consent_state` and `entitlement`.
- Prerequisites: profile exists. The consent decision must be explicit and
  should be preceded by the product's consent copy/version acknowledgement.
- Expected errors: `400 user not found`; `422` when `facial_data` is absent or
  not boolean. Sending `false` is valid and returns a profile with
  `user.consent_state == "declined"`; photo capture remains blocked.
- Ideal UI state: a dedicated consent screen with a clear accept/decline
  choice. On success with `active`, show the capture
  guide. On decline, keep the profile usable for non-photo features but keep
  capture visibly locked. Never silently grant consent during profile creation.

## Products, routine, and experiments

### `POST /api/products`

- Minimal payload: `{"name":"Barrier serum"}`. Supported optional fields are
  `barcode`, `category`, `ingredients`, and `stabilization_days`.
- Response fields used: `id`, `name`, `category`, `stabilization_days`; use
  `barcode` for duplicate identification. The raw backend row also contains
  `ingredients_json` and `created_at`.
- Prerequisites: none enforced by this route. A robust client should require a
  profile and use the current user's product workflow before submitting.
- Expected errors: `400 product name is required`; `400` when
  `stabilization_days` is outside `0..180`; `400 barcode already exists`; `422`
  for body validation.
- Ideal UI state: a product-created confirmation that immediately offers the
  next action: “Log when you started using it.” Refresh product search after
  success. Prevent duplicate submits; this route is not idempotent.

### `GET /api/products/search?q={query}`

- Minimal payload: no body; `q` is optional and defaults to an empty string.
- Response fields used: list items `id`, `name`, `category`,
  `stabilization_days`; optionally `barcode` and `ingredients_json` for a
  product card. The service returns at most 30 rows.
- Prerequisites: none enforced. Product rows are global in the current schema,
  not scoped to `user_id`.
- Expected errors: `422` for an invalid query type; otherwise an empty list is
  a successful no-results state.
- Ideal UI state: searchable product picker with loading, empty, and retry
  states. Debounce search and do not treat an empty list as an error. Avoid
  displaying `ingredients_json` directly; parse or use the explainer endpoint.

### `GET /api/products/{product_id}?user_id={user_id}`

- Minimal payload: no body; `user_id` is a required query parameter even though
  the user is not part of the path.
- Response fields used: product `id/name/category/stabilization_days`; for a
  premium detail screen, `offers` and `ingredient_analysis`.
- Actual response: product row plus `offers` (empty for free users) and
  `ingredient_analysis` (null for free users, explainer object for premium).
- Prerequisites: product and user exist. Premium is not required for the base
  detail response, but controls the nested fields.
- Expected errors: `400 user not found`; `400 product not found`; `422` if
  `user_id` is omitted.
- Ideal UI state: product detail sheet. Load it only after a picker selection;
  keep the product ID and user ID separate and construct the query with
  `URLSearchParams`.

### `GET /api/products/{product_id}/ingredient-explainer?user_id={user_id}`

- Minimal payload: no body; required `user_id` query parameter.
- Response fields used: `product_name`; `reviewed[]` with `ingredient`,
  `purpose`, `caution`; `unknown[]`.
- Prerequisites: live user, product exists, and active Premium entitlement.
- Expected errors: `400 user not found` or `product not found`; `403 Ingredient
  analysis requires Premium`; `422` for missing `user_id`.
- Ideal UI state: an explicit loading panel followed by reviewed/unknown
  sections. Never present `unknown` as safe or unsafe; it means the catalog has
  no reviewed explanation.

### `POST /api/routine-events`

- Minimal payload:

  ```json
  {"user_id":"<id>","product_id":"<id>","action":"start"}
  ```

  Optional fields: `timestamp`, `slot`, `dose`, `frequency`, `notes`, and
  `experiment_id`.
- Response fields used: direct event `id`, `product_id`, `action`,
  `timestamp`, `slot`, `dose`, `frequency`, `notes`; dashboard history adds
  `product_name` through its join.
- Prerequisites: user and product exist. Consent and Premium are not required
  for routine logging. `action` must be exactly `start`, `stop`, or `change`.
- Expected errors: `400 user not found`; `400 product not found`; `400 action
  must be start, stop, or change`; invalid timestamp is `400`; malformed body
  is `422`. If `experiment_id` is invalid, the runtime service can report
  `400 experiment not found` after the base routine insert has already been
  written; prevalidate the experiment client-side and reconcile after failure.
- Ideal UI state: the required bridge between “product added” and “evidence
  window started.” Confirm the timestamp and slot, then show the next capture
  window. Do not model this as a daily “applied” tick: the backend only accepts
  routine state changes.

### `POST /api/experiments`

- Minimal payload:

  ```json
  {"user_id":"<id>","name":"Barrier test","product_id":"<id>"}
  ```

  Optional fields: `hypothesis`, `primary_metric` (`blemish_count`,
  `redness_score`, `darkspot_area`, or `texture_score`), and `target_days`
  (`1..180`).
- Response fields used: `id`, `name`, `hypothesis`, `primary_metric`,
  `status`, `target_days`; `products[]` (`name`, `category`, `role`);
  `events[]`; and `captures[]` when showing experiment detail.
- Prerequisites: active consent, active Premium, and an existing product.
- Expected errors: `403 Experiments requires Premium`; `400 explicit
  facial-data consent is required...`; `400 unsupported primary metric`;
  `400 target_days must be between 1 and 180`; `400 product not found`; `422`
  for missing/invalid fields.
- Ideal UI state: after a product start event, present this as an optional
  “measure one change” step. Show the stabilization target and hypothesis
  before confirming. Do not claim the experiment has an outcome immediately;
  it starts with `status == "running"` and needs captures over time.

### `GET /api/users/{user_id}/experiments`

- Minimal payload: none.
- Response fields used: array of experiment objects; current UI uses `id`,
  `name`, `status`, and `products[0].name`. Detail screens can use
  `hypothesis`, `primary_metric`, `target_days`, `events`, and `captures`.
- Prerequisites: live user and active Premium.
- Expected errors: `400 user not found`; `403 Experiments requires Premium`.
- Ideal UI state: a list with a Premium gate. If the plan is free, use the
  `403` to show an upgrade explanation rather than an empty experiment list.
  The static page hides the call on free plans, which is fine, but must still
  handle a server-side `403` after a plan change.

### `GET /api/users/{user_id}/experiments/{experiment_id}`

- Minimal payload: none.
- Response fields used: `id`, `name`, `status`, `hypothesis`,
  `primary_metric`, `target_days`, `products`, `events`, and `captures`.
- Prerequisites: experiment belongs to the user and Premium is active only
  when reached through the list/detail feature; the service checks ownership.
- Expected errors: `400 experiment not found` (including wrong user); the
  route itself does not call `require_premium`, so the client should keep the
  detail behind the same entitlement gate as the list.
- Ideal UI state: experiment detail timeline. Treat `captures` as a read-only
  history snapshot and show when the next capture is due from the capture guide.

### `POST /api/users/{user_id}/experiments/{experiment_id}/status`

- Minimal payload: `{"user_id":"<same path id>","status":"completed"}`.
  Allowed statuses are `planned`, `running`, `paused`, `completed`, and
  `cancelled`.
- Response fields used: the updated experiment fields, especially `status`,
  `end_at`, `products`, `events`, and `captures`.
- Prerequisites: path/body user IDs must match; active Premium; experiment
  belongs to the user.
- Expected errors: `400 user_id mismatch`; `400 invalid experiment status`;
  `400 experiment not found`; `403 Experiments requires Premium`; `422` body
  validation.
- Ideal UI state: status transition confirmation with rollback-like retry
  affordance. Refetch the experiment list and dashboard after success. Do not
  mark it complete optimistically before the response.

## Capture, dashboard, and history

### `GET /api/users/{user_id}/capture-guide?vertical={vertical}`

- Minimal payload: no body; `vertical` defaults to `skin`.
- Response fields used: `vertical`, `state`, `message`,
  `next_window_start`, `next_window_end`, and, after the first capture,
  `last_capture`. States are `baseline_needed`, `scheduled`, `due`, or
  `overdue`.
- Prerequisites: user exists. Consent is not required to read guidance.
- Expected errors: `400 user not found`; `400 invalid vertical`; `422` for a
  malformed query.
- Ideal UI state: the capture entry screen. Make `baseline_needed` a primary
  CTA, `due/overdue` an active CTA, and `scheduled` a calm reminder state. Use
  the server window rather than a client-only interval.

### `POST /api/captures`

- Minimal payload:

  ```json
  {"user_id":"<id>","image_base64":"<base64>"}
  ```

  The practical payload should include `vertical`, `is_baseline`, optional
  `experiment_id`, optional `captured_at`, optional `device_meta`, and a
  `quality` object. Pose fields expected in `quality` are `face_present`,
  `yaw_degrees`, `pitch_degrees`, `distance_cm`, and `expression_neutral`.
  The server overwrites brightness and sharpness with measurements from the
  uploaded image.
- Response fields used: `id`, `captured_at`, `is_baseline`, `status`,
  `capture_quality.accepted/score/failed_checks`, `analysis_job_id`,
  `metric.confidence`, `metric.redness_score`, `metric.blemish_count`,
  `metric.darkspot_area`, `metric.texture_score`, `metric.model_version`,
  `vertical`, and `appearance_metrics`.
- Prerequisites: active facial-data consent; valid base64; image at least
  160x160; an accepted server quality result; if supplied, experiment belongs
  to the user. The first accepted capture for the user becomes a baseline even
  if `is_baseline` is false.
- Expected errors: `400 image_base64 must be valid base64`; `403 explicit
  facial-data consent is required before using photo capture`; `400 image must
  be at least 160x160 pixels`; `400` quality rejection with a structured
  quality detail; `400 vertical must be skin`;
  `400 experiment not found`; `422` for missing required fields. Analysis or
  photo-store failures can surface as `500`.
- Ideal UI state: camera/file capture with a server-authoritative quality gate.
  Show preview, upload progress, analyzing state, accepted/rejected result, and
  the exact `failed_checks`. On success, clear/rearm the capture input and
  refetch guide, dashboard, and history. Never silently retry an image upload:
  duplicate accepted captures change the history.

The current static page sends fixed pose values (`face_present: true`, zero
yaw/pitch, 45 cm, neutral expression). Brightness and sharpness are still
server-derived, but a production camera client should obtain pose/face values
from a real device quality check rather than asserting them.

The backend stores one baseline at the user level for analysis while appearance
profiles are vertical-specific. The current first-capture rule also counts
captures across verticals. The UI must explain this or avoid offering a
“baseline for this vertical” promise until the backend semantics are separated.

### `GET /api/users/{user_id}/dashboard?vertical={vertical}`

- Minimal payload: no body; `vertical` defaults to `skin`.
- Response fields used:
  - `profile.user`, `profile.appearance_profiles`, and
    `profile.entitlement` for shell/session state;
  - `vertical`;
  - `history[]` fields listed under the history endpoint;
  - `verdicts[].label`, `generated_text`, `product_name`, and
    `evidence.n_after/confidence`;
  - `experiments[]` when Premium;
  - `engagement.capture_count`, `capture_streak`, `capture_days`, `guide`,
    `reminders`;
  - `analytics.median_history_days`, `baseline_capture`,
    `first_three_captures`, `activation`;
  - `routine_events[].action`, `product_name`, `timestamp`, `slot`, `notes`;
  - `features` and `disclaimer`.
- Prerequisites: user exists. Premium controls whether verdicts and
  experiments are populated; free users receive empty arrays for those
  sections.
- Expected errors: `400 user not found`; `400 invalid vertical`; Premium
  failures can occur while refreshing verdicts/experiments if entitlement
  changes during the request; infrastructure failures are `500`.
- Ideal UI state: the authenticated home screen after onboarding. Use one
  request as the initial data snapshot, but treat it as refreshable and do not
  assume it is cheap: it recalculates verdict copy and updates reminders.
  Render empty states as the next action (baseline, log routine, or capture
  again), not as an error.

### `GET /api/users/{user_id}/history?vertical={vertical}`

- Minimal payload: no body; optional `vertical`, default `skin`.
- Response fields used per item: `id`, `captured_at`, `is_baseline`,
  `redness_score`, `blemish_count`, `redness_delta`, `darkspot_area`,
  `texture_score`, `confidence`, `model_version`, `capture_quality`,
  `noise_floor`, and `appearance_metrics`.
- Prerequisites: user exists; vertical is valid. Consent is not required to
  read already stored history.
- Expected errors: `400 user not found`; `400 invalid vertical`.
- Ideal UI state: history chart/table and metric detail. Keep the raw capture
  out of this endpoint's rendering; it returns derived measurements and quality
  metadata, not an image URL. Show model version and noise floor wherever a
  verdict or trend could be mistaken for medical certainty.

### `GET /api/users/{user_id}/engagement`

- Minimal payload: none.
- Response fields used: `capture_streak`, `capture_count`, `capture_days`,
  `guide`, and `reminders[]` (`id`, `kind`, `next_at`, `enabled`,
  `cadence_days`, `last_sent_at`).
- Prerequisites: user exists.
- Expected errors: `400 user not found`; possible `500` on reminder/database
  failure.
- Ideal UI state: streak ring, cadence summary, and next reminder. This GET
  currently writes a reminder row as a side effect; do not call it on every
  keystroke or poll aggressively.

### `POST /api/users/{user_id}/engagement`

- Minimal payload: `{"event_type":"verdict_open"}`. Optional
  `reference_id` and `metadata`.
- Response fields used: `id`, `event_type`, `reference_id`, `metadata_json`,
  and `occurred_at` only if the client needs confirmation; the static UI does
  not currently consume the response.
- Prerequisites: user exists. There is no allowlist of event names.
- Expected errors: `400 user not found`; `422` if `event_type` is missing.
- Ideal UI state: fire-and-forget telemetry after the UI has completed the
  associated action. Do not block core navigation on this call; use a queue or
  best-effort retry and avoid sending photo contents or sensitive free text in
  metadata.

### `GET /api/users/{user_id}/analytics`

- Minimal payload: none.
- Response fields used: `activation`, `baseline_capture`,
  `first_three_captures`, `median_history_days`,
  `weekly_verdict_open_rate`, `verdict_action_rate`,
  `evidence_unclear_engagement_rate`, and `raw_events`.
- Prerequisites: user exists.
- Expected errors: `400 user not found`.
- Ideal UI state: an account/product-health or internal progress panel. Do not
  display the rates as clinical confidence; they are engagement-derived
  product analytics.

## Q&A, insights, discover, and commerce

### `POST /api/users/{user_id}/qna`

- Minimal payload: `{"question":"Why did redness change?"}`. Optional
  `thread_id` continues an existing thread.
- Response fields used: `thread_id`, `answer`, `scope`, and `citations[]` with
  `type`, `date`, and `id`.
- Prerequisites: active Premium. The server triages the question first. For
  cosmetic tracking, the answer is grounded in structured history, routine
  events, and verdicts; raw photos are not sent to the Gemini provider.
- Expected errors: `403 Data Q&A requires Premium`; `400 thread not found`;
  `422` for an empty question or a question longer than 2,000 characters;
  network/provider failures normally fall back to deterministic copy in the
  runtime service, but unexpected provider/database failures may be `500`.
- Ideal UI state: chat composer with a pending assistant bubble, then render
  `scope` and citations. If `scope == "dermatology_review"`, present the safety
  message without making it conversationally diagnostic. Preserve the returned
  `thread_id` and send it on the next turn; the current static page discards it
  and creates a new thread for every question.

### `GET /api/users/{user_id}/qna`

- Minimal payload: none.
- Response fields used: each message's `role`, `content`, `created_at`,
  `scope`, and parsed `citations[]`; `thread_id`/`title` are available through
  the row join and are useful for grouping messages.
- Prerequisites: active Premium.
- Expected errors: `400 user not found`; `403 Data Q&A requires Premium`.
- Ideal UI state: hydrate one conversation timeline. Group by thread ID/title,
  preserve assistant citations, and do not show the raw `citations_json`
  storage field to users.

### `POST /api/triage`

- Minimal payload: `{"text":"I have a painful changing mole"}`.
- Response fields used: `scope`, `message`, and `matched_terms`.
- Prerequisites: none; this endpoint does not require a user or Premium.
- Expected errors: `422` for missing/empty text or text over 2,000 characters.
- Ideal UI state: run before submitting a free-form insight question if the
  product wants an immediate safety interstitial. If the route is
  `dermatology_review`, stop cosmetic interpretation and show a qualified
  dermatologist recommendation.

The typed web client currently expects a `route` field from this endpoint, but
the backend returns `scope`. Use `scope` (or support both during a migration),
not `route` alone.

### `GET /api/users/{user_id}/discover`

- Minimal payload: none.
- Response fields used: `recommendations[]` with `product_id`, `name`,
  `category`, `sample_size`, `average_effect`, and `reason`; top-level
  `minimum_cohort_size` and `disclaimer`.
- Prerequisites: active Premium. Recommendations are emitted only when at
  least three consenting users contribute a likely-useful verdict.
- Expected errors: `400 user not found`; `403 Discover requires Premium`.
- Ideal UI state: a clearly labeled cohort-context screen with a cold-start
  empty state. Never render these recommendations as personal verdicts or
  medical recommendations, and always show the disclaimer.

### `GET /api/users/{user_id}/commerce/offers?product_id={product_id}`

- Minimal payload: no body; optional `product_id` filter.
- Response fields used: `id`, `product_id`, `product_name`, `merchant`,
  `url`, `price_cents`, `currency`, `disclosed`, and `active`.
- Prerequisites: live user only — **commerce is free for every plan**, not
  Premium-gated. The offer must be active; optional product filter must
  identify a known product to be useful.
- Expected errors: `400 user not found`; `422` for an invalid query type.
- Ideal UI state: affiliate-disclosed offer cards, shown on every plan. Display
  currency and disclosure, and never imply that placement changes a verdict.

### `POST /api/users/{user_id}/commerce/offers/{offer_id}/click`

- Minimal payload: none.
- Response fields used: `url` for opening the merchant; `id`, `product_id`,
  `merchant`, `price_cents`, `currency`, and `disclosed` for confirmation.
- Prerequisites: live user and an active offer — no Premium requirement.
- Expected errors: `400 user not found`; `400 offer not found`.
- Ideal UI state: record the click, then open the returned URL in a new tab or
  external browser. If opening fails, retain the offer and show a retry rather
  than recording repeated clicks automatically.

### `GET /api/users/{user_id}/labels`

- Minimal payload: none.
- Response fields used: label `id`, `photo_id`, `label_type`, `value`,
  `confidence`, `notes`, and `created_at`.
- Prerequisites: user exists; no Premium requirement.
- Expected errors: `400 user not found`.
- Ideal UI state: optional review/annotation screen. Keep labels distinct from
  model-generated metric names and show their provenance.

### `POST /api/users/{user_id}/labels`

- Minimal payload:

  ```json
  {"photo_id":"<capture id>","label_type":"user_note","value":"..."}
  ```

  Optional `confidence` (`0..1`) and `notes`.
- Response fields used: the created label row, especially `id`, `photo_id`,
  `value`, `confidence`, and `created_at`.
- Prerequisites: user and capture belong together.
- Expected errors: `400 user not found`; `400 capture not found`; `422` for
  missing fields or invalid confidence.
- Ideal UI state: save annotation, then invalidate the labels for that capture.
  Do not use a user label as an automated medical classification.

### `POST /api/users/{user_id}/reprocess`

- Minimal payload: `{"model_version":"deterministic-3.1"}`.
- Response: `{"job_id","status":"queued"}` — the reprocess itself runs on a
  background thread pool (`glowupai/jobs.py`), not on the request.
- Prerequisites: active Premium; the photo store must still contain accepted
  captures.
- Expected errors: `400 user not found`; `403 Historical reprocessing requires
  Premium`; `422` for an empty/too-long model version.
- Ideal UI state: a deliberate account action with a warning that metric values
  may change. Show "queued" immediately, then poll the status route below
  until `completed`/`failed`, then refresh history/dashboard.

### `GET /api/users/{user_id}/reprocess/{job_id}`

- Minimal payload: no body.
- Response fields: `status` (`queued`/`running`/`completed`/`failed`),
  `result` (`{"processed_count","model_version"}` once completed, else `null`),
  `error` (populated only on failure).
- Prerequisites: job belongs to the user.
- Expected errors: `400 reprocess job not found` for a wrong/foreign job id.
- Ideal UI state: poll every 1-2s while `queued`/`running`; stop on
  `completed`/`failed` and surface `error` if present.

## Subscriptions and entitlements

### `GET /api/users/{user_id}/subscription`

- Minimal payload: none.
- Response fields used: `user_id`, `plan`, `status`, `started_at`, `renews_at`,
  and `source`.
- Prerequisites: user exists.
- Expected errors: `400 user not found`.
- Ideal UI state: the authoritative plan gate. Use it rather than assuming a
  successful upgrade from local UI state.

### `POST /api/users/{user_id}/subscription/upgrade`

- Minimal payload: `{}`. Optional `{"source":"local_checkout"}`.
- Response fields used: the updated entitlement, especially `plan`, `status`,
  `started_at`, `renews_at`, and `source`.
- Prerequisites: user exists. This is a local checkout simulation; it is not a
  payment-provider confirmation.
- Expected errors: `400 user not found`; `422` if `source` is not a string or
  exceeds its declared limits.
- Ideal UI state: show checkout/confirmation only if a real billing boundary
  is added. After success, refetch profile and all Premium-gated screens.
  Disable repeated clicks because each call records a billing event.

### `POST /api/users/{user_id}/subscription/cancel`

- Minimal payload: none.
- Response fields used: entitlement `plan == "free"` and `status ==
  "cancelled"`, plus the remaining dates/source.
- Prerequisites: user exists.
- Expected errors: `400 user not found`; infrastructure failures may be `500`.
- Ideal UI state: require a clear confirmation, then show that history is
  retained while Premium-only features lock. Refetch profile, dashboard, and
  Q&A state; do not delete data.

## Export, deletion, and account controls

### `GET /api/users/{user_id}/export`

- Minimal payload: none.
- Response fields used: `export_version`, `exported_at`, `profile`,
  `consent_events`, `appearance_profiles`, `routine_events`, `experiments`,
  `captures_and_metrics`, `appearance_captures`, `verdicts`, `qna`,
  `engagement`, and `note`.
- Prerequisites: user exists. Q&A is included only when the current plan is
  Premium. Raw photo bytes are not embedded in this JSON response.
- Expected errors: `400 user not found`; `500` if a related read fails.
- Ideal UI state: privacy/data-control screen. Show an export-in-progress
  state, download the JSON as a file, and explain that raw photos require the
  configured authenticated object-store workflow. Do not put the export in a
  shared cache or log its contents.

### `DELETE /api/users/{user_id}`

- Minimal payload: none.
- Response fields used: none; successful response is `204 No Content`.
- Prerequisites: user exists. The service deletes the photo store contents and
  cascades database records.
- Expected errors: `400 user not found`; `500` if photo deletion/database
  deletion fails. The route has no confirmation token or idempotency key.
- Ideal UI state: irreversible action behind a typed `DELETE` confirmation,
  final warning, and disabled submit. On `204`, clear only GlowupAI session
  storage, in-memory caches, and any pending requests, then return to the
  welcome screen. The static page currently calls `localStorage.clear()`, which
  can erase unrelated application data.

### `GET /api/admin/audit?limit={limit}` — currently called by the UI, not a user endpoint

- Minimal payload: no body; optional `limit`, default `100`, clamped by the
  service to `1..1000`.
- Response fields used by the static account page: `action`, `subject_type`,
  and `created_at`; the actual rows also contain `id`, `actor_type`, `actor_id`,
  `subject_id`, `metadata_json`.
- Prerequisites: none in the current implementation. There is no admin
  authentication and no `user_id` filter.
- Expected errors: `422` for a non-integer limit; infrastructure errors may be
  `500`.
- Ideal UI state: **do not call this endpoint from a browser account page**.
  It returns the global audit log and is currently exposed without an admin
  boundary. Replace it with a user-scoped audit endpoint before showing a
  personal timeline. Until then, hide the audit section rather than leaking
  other users' events.

## Operational-only endpoint not required by the user workflow

`POST /api/admin/offers` creates global affiliate offers and returns the offer
row (`id`, `product_id`, `merchant`, `url`, `price_cents`, `currency`,
`disclosed`, `active`, `created_at`). It is not called by the current static or
typed user UI and must remain behind an authenticated admin surface. A user
client should never ship a button that calls it.

## Frontend workflow traps and robust client behavior

### 1. The current static shell is not sequential

`glowupai/static/index.html` renders Overview, Capture, Routine, Insights,
Discover, and Account tabs at boot. Its `view()` function permits navigation
to every view before a profile or consent exists. It does route a newly created
profile to Account for consent, but the navigation remains available and
`localStorage` can bypass the intended order.

Use an explicit finite state machine and hide or disable the workspace shell
until each gate is authoritative:

```text
no_user -> profile_pending -> consent_pending
-> baseline_pending -> home_ready
```

The only escape from `consent_pending` should be Account/privacy or a safe
non-photo explanation. The capture CTA must be disabled until
`profile.user.consent_state == "active"`.

### 2. Profile creation and profile response types differ between apps

The complete route returns a full profile; the legacy app returns a user. The
Next type currently types `createUser` as `{user: User}`, which is compatible
with the fields it reads, but the client must still call the profile endpoint
before deciding state. Never assume a newly created response proves consent or
Premium.

### 3. No authentication means local storage is not an identity system

The browser's `user_id` is enough for this local product but is not a secure
production identity boundary. Use an authenticated session/token at the edge,
bind every user-scoped route to the authenticated subject, and remove global
admin routes from the browser. At minimum, handle stale IDs and never put them
in analytics URLs or logs unnecessarily.

### 4. Product creation is global and routine logging is missing from the static flow

`POST /api/products` has no `user_id`; every user searches the same product
table. The static Routine view can create a product and an experiment but has no
form that calls `POST /api/routine-events`. That leaves the causal timeline
empty and prevents useful attribution. The robust sequence is:

1. Create or select a product.
2. Confirm `POST /api/routine-events` with `action: "start"`.
3. Optionally create an experiment using the same product.
4. Capture baseline/regular measurements through the guide.
5. Use `stop` or `change` only when the routine variable changes.

### 5. Do not use free-plan empty arrays to mean “no data”

The dashboard intentionally returns empty `verdicts` and `experiments` for
free users. A free gate and a true no-evidence state look different. Branch on
`profile.entitlement`/`features` first, then render the appropriate upgrade or
empty state.

### 6. Capture quality is server-authoritative

The UI can preflight dimensions, face pose, and camera framing, but the server
recomputes brightness and sharpness. Do not mark a frame accepted from local
checks. On a `400` quality object, map each `failed_checks` value to one clear
instruction and preserve the image for local retry only; do not resend it
automatically.

### 7. GET dashboard/engagement calls have side effects and expensive work

Dashboard refreshes verdicts and engagement refreshes reminders. Add request
deduplication and avoid polling either endpoint. Cache by `{user_id,plan}` and
invalidate on capture, routine, experiment, consent, and subscription
mutations.

### 8. Error handling must preserve structured detail

The static error parser assumes `detail` is a string or `{message}`. It will
render validation arrays poorly and loses capture quality instructions. A typed
client should normalize:

```text
403 -> show consent or Premium gate based on message/operation
400 -> show actionable domain validation, including detail.quality
422 -> map loc/msg to the field
5xx/network -> retry safe GETs, never blindly retry mutations
204 -> finish deletion without JSON parsing
```

### 9. Mutations need pending locks and reconciliation

Profile creation, capture, routine events, subscription changes, and offer
clicks are not idempotent. Disable their controls while pending, use a client
request ID where a future server version supports it, and refetch the
authoritative resource after success. If a timeout occurs after a capture or
subscription mutation, show “status unknown” and reconcile with dashboard or
subscription before retrying.

### 10. Q&A threads and safety scope must be preserved

Persist `thread_id` from Q&A POST responses. Send it on follow-up questions and
render `scope`/`citations` with the answer. A dermatology-review scope is a
handoff, not an invitation to ask the model for a diagnosis. The current static
UI reloads history after each question and drops the thread ID; the ideal UI
keeps one thread and displays citations inline.

### 11. Response naming mismatches are real integration bugs

- Backend triage returns `scope`; the typed web client currently declares
  `route`.
- Experiments are stored with `start_at`; the typed web client interface uses
  `started_at`.
- Product rows return `ingredients_json`; the typed web interface suggests an
  `ingredients` array.
- Q&A history includes parsed `citations` as well as the storage column
  `citations_json`.

Use the backend names in the adapter layer, normalize once, and keep UI
components unaware of storage-column names. Add contract tests around these
fields before changing either client.

### 12. Account deletion must be scoped and recoverable at the UI boundary

Require a typed confirmation, cancel in-flight reads, remove only the GlowupAI
session key, invalidate cached profile/data, and route to onboarding after the
`204`. Do not show a success state until the server response is received.

## Growth features (added on top of the original surface)

### `GET /api/users/{user_id}/confound-check?exclude_product_id={product_id}`

- Free feature. Call before submitting a `start`/`change` routine event to
  warn the user proactively; the same shape also comes back inline as
  `confound_warning` on the `POST /api/routine-events` response for
  `start`/`change` actions (`null` when nothing is at risk).
- Response: `{"confounded": bool, "active_windows": [{"product_id",
  "product_name","started_at","stable_at"}], "message"?: string}`.
- Prerequisites: live user.
- Ideal UI state: a dismissible warning banner naming the at-risk product(s)
  and explaining the evidence will come back `evidence_unclear` if the user
  proceeds anyway.

### Capture coaching (extends `POST /api/captures`)

- On a `400` quality rejection, `detail.quality.coaching` is now a list of
  `{"check","message"}` objects with a specific, actionable instruction per
  failed check (e.g. "Turn 15° back toward center (right) — yaw must stay
  within 12°."). Render each `coaching[].message` as a distinct tip instead of
  the raw `failed_checks` code list.

### Shelf scan → auto-logging (free)

1. `POST /api/users/{user_id}/shelf-scan` `{"image_base64"}` →
   `{"job_id","status":"queued"}`.
2. `GET /api/users/{user_id}/shelf-scan/{job_id}` → job row with `status`
   and, once `completed`, `result: {"candidates":[{"name","brand","category",
   "ingredients":[...]}],"message"}`. `candidates` is empty with an
   explanatory `message` when the AI vision provider is not configured
   (`GEMINI_API_KEY`/`GLOWUPAI_GEMINI_ENABLED` unset) — always handle that
   case with a manual "add product" fallback.
3. `POST /api/users/{user_id}/shelf-scan/{job_id}/confirm`
   `{"selections":[{"name","category","ingredients","stabilization_days"}]}`
   → list of created `Product` rows. The job must be `completed` first.
- Ideal UI state: capture/upload a shelf photo, poll every ~1.5s, show
  editable candidate cards with checkboxes (user can correct the name/category
  before confirming), confirm only the checked ones.

### `GET /api/products/{product_id}/predict?user_id={user_id}` (Premium)

- Pre-purchase prediction. Response: `{"product_id","product_name",
  "ingredients":[...],"overlap_with_investigate":[{"product_name",
  "shared_ingredients"}],"overlap_with_likely_useful":[...],"cohort_overlap":
  [...],"headline","disclaimer"}`.
- Expected errors: `403 Pre-purchase prediction requires Premium`;
  `400 product not found`.
- Ideal UI state: a "Predict before you buy" panel from the product
  picker/detail screen. This is a similarity signal, not an efficacy
  prediction — always show `disclaimer` and never phrase `headline` as a
  guarantee.

### Context events + root-cause search (Premium)

- `GET`/`POST /api/users/{user_id}/context-events` — CRUD for
  `{"event_type": "sleep"|"travel"|"weather"|"cycle"|"stress"|"diet"|"custom",
  "value"?,"occurred_at"?,"notes"?}`. `occurred_at` defaults to now.
- `GET /api/users/{user_id}/root-cause?metric=texture_score` (metric one of
  `blemish_count`/`redness_score`/`darkspot_area`/`texture_score`) →
  `[{"event_type","occurrences","normalized_effect","metric","message"}]`,
  ranked by effect size, possibly empty.
- Expected errors: `403 Root-cause search requires Premium`.
- Ideal UI state: a simple context log (date + type + free-text value) next to
  a correlations panel in Insights. Always show each `message` verbatim — it
  already states the correlation-not-causation caveat.

### `GET /api/users/{user_id}/budget-optimizer` (Premium)

- Response: `{"flagged":[{"product_id","product_name","days_stable",
  "estimated_annual_cost_cents","currency","reason"}],
  "estimated_annual_waste_cents","currency","disclaimer"}`.
  `estimated_annual_cost_cents` is `null` when no offer price is on file for
  that product — show the product as flagged without a cost figure rather
  than hiding it.
- Expected errors: `403 Routine budget optimizer requires Premium`.

### `GET /api/users/{user_id}/derm-export` (Premium)

- Response: `{"generated_at","capture_count","model_versions":[...],
  "verdicts":[...],"printable_html","disclaimer"}`.
- Expected errors: `403 Dermatologist export requires Premium`.
- Ideal UI state: open `printable_html` in a new window/iframe for the user to
  print or save as PDF via the browser — it is a plain HTML string, not a
  downloadable file the backend generates.

### Experiment early-stop (extends experiment detail)

- `GET /api/users/{user_id}/experiments/{experiment_id}` (and the list route)
  now includes `early_stop: {"conclusive": bool, "recommended_status"?: string,
  "message": string}`.
- Ideal UI state: show a callout on the experiment detail screen when
  `conclusive` is `true`, offering to call
  `POST .../experiments/{id}/status` with `recommended_status` instead of
  waiting out `target_days`.

## Recommended client adapter surface

The UI should depend on a small typed adapter rather than building URLs inside
components:

```text
session: createUser, getProfile, grantConsent
home: getDashboard, getHistory, getCaptureGuide, getEngagement, logEngagement
capture: createCapture
routine: searchProducts, getProduct, createProduct, logRoutineEvent
experiments: list, create, get, setStatus (get/list include early_stop)
insights: ask, history, triage, ingredientExplainer, labels, addLabel,
  reprocess, reprocessStatus, contextEvents, addContextEvent, rootCause
discover: recommendations, offers, clickOffer, predictProduct
shelfScan: submit, status (poll), confirm
growth: confoundCheck, budgetOptimizer, dermExport
billing: entitlement, upgrade, cancel
privacy: exportData, deleteUser
```

Each adapter method should return normalized data plus preserve the HTTP
status/error detail. Components should own only visual state (`idle`,
`loading`, `success`, `empty`, `error`, `locked`), while session gates and
cache invalidation remain centralized.
