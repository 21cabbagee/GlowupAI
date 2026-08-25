# GlowUp AI — Android Delivery Plan (v1 → Play Store)

**Owner of this document:** the orchestrating agent. Every implementation agent MUST read this
file plus `backend/docs/frontend-api-map.md` before writing code.

**Status of the codebase as audited 2026-08-24 (ground truth, not the marketing docs):**

- `QUICK_SUMMARY.md`, `JOURNEY.md` and `REPO_STRUCTURE.md` are **inaccurate**. They claim MVVM,
  Firebase auth, ML Kit, Retrofit, 12 screens, 7 endpoints, a different API shape
  (`/api/auth/register`), and backend files (`main.py`, `models.py`, `api/`) that do not exist.
  Do not treat them as a spec. They are corrected in Phase 6.6.
- Real Android app: 3 Kotlin files. `MainActivity.kt` is 1426 lines containing every screen.
  No ViewModel, no DI, no Navigation library, no repository, no persistence.
- **3 of ~56 backend endpoints** are wired: `POST /api/users`, `POST /api/users/{id}/consent`,
  `POST /api/captures`.
- A **new backend user is created on every single analysis** (`ApiService.kt:150`). There is no
  stored identity. History is in-memory `remember` state.
- Base URL hardcoded `http://10.0.2.2:8000/api`. Raw `HttpURLConnection`. `GlobalScope.launch`.
  Retrofit, Coil and Accompanist are declared dependencies but unused.
- Theme is the unmodified purple Compose template; screens bypass it with hardcoded
  `Color(0xFF...)` literals. 5 of 6 metrics on the result screen are hardcoded `8/7/9/6/8`.
  Streak is `// Mock streak data`.
- No signing config; R8 disabled for release; only the two template tests.

**The backend is complete and is the contract.** `backend/skinproof/complete_api.py` exposes
~56 routes, fully documented in `backend/docs/frontend-api-map.md`. Frontend work must not
change backend behaviour except in Phase 1, which is an explicitly authorised auth addition.

---

## 0. Decisions already made (do not relitigate)

| Decision | Choice |
| --- | --- |
| Deliverable | **Android app only.** `backend/web` is reference material to port from, not a deliverable. |
| Build verification | Install Android SDK command-line tools locally. Every agent must leave the build green. |
| Identity | **Firebase Auth** (Google Sign-In + email/password). This requires the Phase 1 backend auth work. |
| Billing | Simulated upgrade via the existing `POST /subscription/upgrade`. **Alpha / closed testing only** — real paid distribution requires Google Play Billing, which is out of v1 scope. |
| Backend hosting | Deploy the existing Dockerfile to Railway or Render, HTTPS, Postgres. |
| Design language | The "Honey" system already specified in `backend/docs/ui-revamp-plan.md` §2 — Bumble-adjacent warm amber, ported to Compose. |

### Non-negotiable product constraints

- The product is **cosmetic tracking, never diagnosis**. Every metric, verdict and Q&A surface
  must carry the backend's `disclaimer` text. `scope == "dermatology_review"` from
  `POST /api/triage` is a hand-off to a clinician, not a conversation to continue.
- Capture quality is **server-authoritative**. Client preflight improves UX; it never marks a
  frame accepted.
- Free-plan empty arrays are **not** "no data" — branch on `entitlement`/`features` first and
  render an upgrade state, per `frontend-api-map.md` trap #5.
- `label == "locked"` verdicts render as a distinct upsell card, not a normal verdict.
- Commerce offers are **free for every plan** — never gate them.

---

## 1. Target architecture

Single Gradle module `app`. Package root `com.glowup.ai`.

```
com.glowup.ai
├── GlowUpApplication.kt          @HiltAndroidApp
├── MainActivity.kt               thin: setContent { GlowUpTheme { GlowUpApp() } }
├── core/
│   ├── design/                   theme, tokens, typography, spacing, shapes, motion
│   ├── ui/                       reusable components (see §3.2)
│   └── util/                     Result wrappers, formatters, date/locale
├── data/
│   ├── remote/
│   │   ├── GlowUpApi.kt          Retrofit interface — ALL endpoints
│   │   ├── dto/                  one file per domain family
│   │   ├── ApiErrorMapper.kt     403/400/422/204 + detail.quality + coaching
│   │   └── AuthInterceptor.kt    Firebase ID token → Authorization: Bearer
│   ├── local/
│   │   ├── GlowUpDatabase.kt     Room: cached dashboard/history/products + capture outbox
│   │   └── SessionStore.kt       DataStore: user_id, plan, onboarding state
│   ├── repository/               one repository per domain family
│   └── work/                     WorkManager: capture outbox, reminders
├── domain/
│   ├── model/                    UI-facing models, storage column names normalised away
│   └── SessionStateMachine.kt    no_user → authed → profile → consent → baseline → ready
├── di/                           Hilt modules
└── feature/
    ├── auth/  onboarding/  home/  capture/  routine/  insights/  discover/  account/
        each: <Name>Screen.kt, <Name>ViewModel.kt, <Name>UiState.kt, components/
```

**Stack:** Kotlin 2.2, Compose BOM 2026.02.01, Material3, Hilt, Navigation Compose (type-safe
routes), Retrofit + OkHttp + kotlinx.serialization, Room, DataStore, WorkManager, CameraX,
ML Kit face detection, Coil, Firebase (Auth, Analytics, Crashlytics), Turbine + MockWebServer
for tests. Charts are hand-drawn on Compose `Canvas` — no chart library, so the palette stays
under our control (port the maths from `backend/web/components/charts.tsx`).

**Every screen** exposes `UiState = Loading | Content | Empty | Error | Locked`. Locked is a
first-class state because the backend returns real `403`s.

---

## 2. Phases and task handoffs

Legend: **[H]** = requires a human (account creation, credentials, console UI).
**[A]** = agent task. Agent tasks name the files they own; two concurrent agents never own the
same file.

### Phase 0 — Environment and build spine  *(blocking; nothing verifiable without it)*

- **0.1 [A]** Install Android SDK command-line tools to `%LOCALAPPDATA%\Android\Sdk`: `platform-tools`,
  `platforms;android-37`, `build-tools;37.0.0`, `cmdline-tools;latest`. Accept licences. Write
  `local.properties` (git-ignored). Verify `./gradlew :app:assembleDebug` on the **current,
  unmodified** code and record the baseline result. Do not fix app code in this task.
- **0.2 [A]** Optional emulator: `system-images;android-36;google_apis;x86_64` + an AVD named
  `glowup_pixel` at 412×915. Used for screenshots and smoke runs.
- **0.3 [A]** Build config rework in `app/build.gradle.kts` + `gradle/libs.versions.toml`:
  version catalog for every dependency (remove all hardcoded versions), drop unused deps
  (`accompanist-permissions`, gson-converter once kotlinx lands), `buildTypes` debug/staging/release,
  `signingConfigs` reading `keystore.properties` (git-ignored, absent-tolerant), **enable R8 +
  resource shrinking for release** with real `proguard-rules.pro`, `buildConfigField` for
  `API_BASE_URL` per build type, `JavaVersion.VERSION_17`, `applicationIdSuffix` for debug.
  Ship a `.gitignore` update and a `keystore.properties.example`.

**Gate:** `./gradlew :app:assembleDebug :app:assembleRelease` both succeed.

### Phase 1 — Backend auth + deployment  *(runs parallel to Phase 2)*

This is the one authorised backend change. It exists because Firebase Auth was chosen and the
backend currently has **no auth boundary at all** — every user-scoped route trusts a path
parameter.

- **1.1 [H]** Create the Firebase project `glowup-ai`. Enable Google + Email/Password providers.
  Download `google-services.json` into `app/`. Create a service-account JSON for the backend.
  Add the debug and release SHA-1/SHA-256 fingerprints for Google Sign-In.
- **1.2 [A]** Backend auth layer, in `backend/skinproof/`:
  - `auth.py` — verify Firebase ID tokens (`firebase-admin`, or JWKS verification against
    `https://www.googleapis.com/service_accounts/v1/jwk/securetoken@system.gserviceaccount.com`
    to avoid the heavy SDK). Config via `Settings`: `SKINPROOF_FIREBASE_PROJECT_ID`,
    `SKINPROOF_AUTH_REQUIRED` (default off so existing tests and the web client keep passing).
  - migration `0004_firebase_identity.sql` — `users.firebase_uid` unique nullable + index.
    Mirror in `full_db.py` and `postgres_db.py`.
  - `POST /api/auth/session` — exchange a verified ID token for the profile; create the user and
    appearance profile on first sight; idempotent per `uid`.
  - A FastAPI dependency that, when `SKINPROOF_AUTH_REQUIRED=1`, asserts the authenticated
    subject owns the `{user_id}` in the path and returns `403` otherwise. Apply to every
    user-scoped route. Leave `/api/health`, `/api/triage`, `/api/products/search` open.
  - Put `/api/admin/*` behind a separate static admin token — `frontend-api-map.md` flags
    `GET /api/admin/audit` as an unauthenticated global leak.
  - Tests in `backend/tests/test_auth.py`; existing suites must stay green.
- **1.3 [A+H]** Deploy: review `Dockerfile`/`docker-compose.yml`, add a `/api/health`-based
  healthcheck, Postgres via `DATABASE_URL`, env matrix (`GEMINI_API_KEY`,
  `SKINPROOF_FIREBASE_PROJECT_ID`, service-account secret, `SKINPROOF_ENV=production`,
  `SKINPROOF_DISABLE_LEGACY_KEY_FILE=1`), migration-on-boot, `railway.json` or `render.yaml`,
  and a `DEPLOY.md`. **[H]** runs the provider login and holds the secrets. Output: an HTTPS
  base URL wired into `API_BASE_URL` for staging/release.

**Gate:** `python -m unittest` green; deployed `/api/health` returns 200 over HTTPS;
`POST /api/auth/session` returns a profile for a real Firebase token.

### Phase 2 — App foundation  *(everything in Phase 3 depends on this; do it before fanning out)*

- **2.1 [A] Design system.** Port `ui-revamp-plan.md` §2 to Compose. Owns
  `core/design/*`, `res/values/themes.xml`, `res/values/colors.xml`.
  - `Tokens.kt`: honey-300 `#FFE29A`, honey-400 `#FFD166`, **honey-500 `#FFBE2E` primary**,
    honey-600 `#F0A400`, honey-700 `#B87300`, ink-900 `#14110B`, ink-600 `#57503F`,
    paper `#FFFDF8`, surface `#FFFFFF`; dark surfaces on warm charcoal `#0F0D0A`.
  - Verdict semantics: `keep` → honey, `likely_useful` → sage `#3F7D5C`,
    `evidence_unclear` → clay `#B87300`, `investigate` → rust `#C2453F`.
  - **Contrast rule, non-negotiable: yellow is never text. Yellow is a surface with `ink-900`
    on top.** Verify AA on every pair.
  - Full `lightColorScheme`/`darkColorScheme` — every role assigned, not just primary/secondary.
    **`dynamicColor` off** (Material You currently overrides the brand on Android 12+).
  - Complete `Typography` scale, display at `-0.04em` tracking / 1.02 leading, body 1.55.
    Two weights. `Spacing`, `Shapes`, `Motion` (spring 140–220ms, honours reduced-motion).
  - Anti-slop rules from the plan are binding: no gradient-mesh blobs, no glassmorphism, no
    emoji as iconography, no purple-blue AI gradients, no centred marketing copy in-product.
  - Deliver a `DesignSystemPreview.kt` with every token and component rendered light + dark.
- **2.2 [A] Component library.** Owns `core/ui/*`. `GlowButton` (primary/secondary/ghost/danger,
  loading + disabled), `GlowCard`, `StatTile`, `VerdictChip`, `LockedCard` (the premium upsell),
  `GlowTextField`, `GlowBottomSheet`, `EmptyState` (must name the next action),
  `ErrorState` (retry), `ShimmerSkeleton`, `StreakRing`, `MetricBar`, `SectionHeader`,
  `DisclaimerNote`, `GlowTopBar`, `GlowBottomBar` + centre `CaptureFab`, `PollingIndicator`.
  Every one gets a `@Preview` in both themes and a TalkBack content description.
- **2.3 [A] Network layer.** Owns `data/remote/*`. `GlowUpApi.kt` covering **all ~56 routes**
  from `complete_api.py` — no exceptions, including ones no screen uses yet. DTOs use the
  **backend's** field names (`ingredients_json`, `start_at`, `scope`, `citations_json`) and
  normalise to clean domain models in one mapper layer; `frontend-api-map.md` §11 lists the
  exact mismatches the web client got wrong — do not repeat them. `ApiErrorMapper` produces a
  sealed `ApiError`: `Unauthorized`, `PremiumRequired(feature)`, `ConsentRequired`,
  `Validation(field→msg)`, `CaptureQualityRejected(quality, coaching)`, `NotFound`,
  `Network`, `Server`. `204` must not be JSON-parsed. Base URL from `BuildConfig`.
  `AuthInterceptor` attaches the Firebase ID token and refreshes on `401`. Timeouts, retry on
  safe GETs only, **never** auto-retry a mutation. `MockWebServer` tests per family.
- **2.4 [A] Persistence + repositories.** Owns `data/local/*`, `data/repository/*`, `data/work/*`.
  `SessionStore` (DataStore) holds `user_id`, plan, consent state, onboarding step — this is
  what fixes the create-a-user-every-time bug. Room caches dashboard/history/products and holds
  a **capture outbox** so a capture taken offline uploads later via WorkManager. Cache
  invalidation matrix per `frontend-api-map.md` trap #7: `GET /dashboard` and `GET /engagement`
  have server side effects — dedupe requests, never poll them, invalidate on capture, routine
  event, experiment, consent and subscription mutations.
- **2.5 [A] DI, navigation, app shell.** Owns `di/*`, `GlowUpApplication.kt`, `MainActivity.kt`,
  `feature/shell/*`. Hilt modules. Type-safe Navigation Compose graph declaring **every**
  Phase 3 destination as a stub composable, so Phase 3 agents only ever edit their own feature
  package — this is the concurrency contract. Shell: 4 tabs + centre Capture FAB
  (Home · Routine · **[FAB]** · Insights · You), Discover reachable from Home and from the
  product picker. Safe-area insets, 48dp minimum targets, predictive back, deep links.
- **2.6 [A] Session gate.** Owns `domain/SessionStateMachine.kt` + `feature/shell/SessionGate.kt`.
  Implements the sequential workflow from `frontend-api-map.md`:
  `no_user → authed → profile_created → consent_required → baseline_needed → home_ready`.
  The workspace shell must not render before the state is authoritative, and the capture CTA
  stays disabled until `profile.user.consent_state == "active"`. Derive state from
  `GET /profile`, never from a button press. On `400 user not found`, clear **only** the GlowUp
  session keys and restart at welcome.

**Gate:** build green, `DesignSystemPreview` renders, network tests pass, shell navigates
between all stub destinations, session gate unit-tested.

### Phase 3 — Feature screens  *(7 agents, parallel, each owns one feature package)*

Each agent owns `feature/<name>/**` only, consumes repositories from Phase 2, and must handle
Loading / Content / Empty / Error / Locked for every surface.

- **3.1 Auth + Onboarding** — `feature/auth`, `feature/onboarding`.
  Firebase Google + email/password, splash (`SplashScreen` API), 3-card value carousel with
  working skip, `POST /api/auth/session`, `PATCH /profile` (display name, skin type, goals,
  experience level, `onboarding_complete`), and a **real consent screen** with explicit
  accept/decline calling `POST /consent` with `policy_version`. Declining must keep the profile
  usable with capture visibly locked — never silently grant consent.
- **3.2 Capture** — `feature/capture`. Replaces `CameraScreen.kt`. CameraX front camera,
  **ML Kit face detection preflight** feeding real pose values into the `quality` object
  (`face_present`, `yaw_degrees`, `pitch_degrees`, `distance_cm`, `expression_neutral`) instead
  of today's hardcoded lies. Oval framing guide, live quality HUD, gallery import, downscale
  before JPEG+base64 (full-res bitmaps are currently sent), upload progress, analysing state.
  On `400`, render every `detail.quality.coaching[].message` as its own tip. `GET /capture-guide`
  drives the entry state (`baseline_needed`/`due`/`overdue`/`scheduled`). Never auto-retry an
  upload. Also owns `POST /measurement-feedback` ("does this reading look fair?").
- **3.3 Home** — `feature/home`. `GET /dashboard` as the single snapshot, plus `/capture-guide`,
  `/engagement`, `/check-ins`, `/weekly-recap`. Streak ring, stat tiles from **real** metrics
  (no hardcoded 8/7/9/6/8), verdict cards including the `locked` upsell variant, history trend
  charts from `GET /history` with model version + noise floor shown, daily check-in sheet,
  weekly recap card, next-capture window. Empty states name the next action.
- **3.4 Routine** — `feature/routine`. Product search/lookup/create/detail, ingredient explainer
  (Premium), `POST /routine-events` restricted to **`start` / `stop` / `change` only** — it is
  not a daily "applied" tick, and a 4-value action list is the exact bug the web revamp caught.
  `GET /confound-check` warning banner before start/change plus the inline `confound_warning`.
  Shelf-scan: submit → poll `~1.5s` → editable candidate cards with checkboxes → confirm, with
  a manual-add fallback for when the vision provider is unconfigured and `candidates` is empty.
  Experiments: list/create/detail/status, and the `early_stop` callout offering
  `recommended_status`.
- **3.5 Insights** — `feature/insights`. Q&A chat that **persists `thread_id`** across turns
  (the web static page loses it), renders `scope` + `citations`, runs `POST /triage` first and
  shows the clinician hand-off for `dermatology_review` without continuing diagnostically.
  Context-event log, root-cause correlations rendering each `message` verbatim, budget
  optimizer (handle `null` cost — show the product flagged, without a figure), derm export
  (render `printable_html`, offer print/share), labels, and reprocess with job polling and a
  "values may change" warning.
- **3.6 Discover** — `feature/discover`. Cohort recommendations with `minimum_cohort_size` and
  the disclaimer always visible and never phrased as a personal verdict; affiliate offers
  **ungated for free users** with disclosure and currency; click-then-open with retry that does
  not double-record; product prediction and purchase guidance framed as similarity, never
  efficacy.
- **3.7 Account** — `feature/account`. Profile, subscription state from
  `GET /subscription` (authoritative — never infer from a tap), paywall listing the real
  Premium features with the simulated upgrade + cancel, data export to a file, **typed
  `DELETE` confirmation** for account deletion that on `204` clears only GlowUp keys and
  returns to welcome, analytics panel labelled as engagement metrics not clinical confidence,
  and settings that actually work (theme, reminder cadence, notifications) — today all four
  settings rows are `onClick = {}`. Do **not** call `GET /api/admin/audit` from the app.

**Gate:** every screen builds, previews render in both themes, no hardcoded `Color(0xFF…)`
outside `core/design`, no endpoint family left without a surface.

### Phase 4 — Cross-cutting polish

- **4.1 [A]** Offline: outbox flush, cached-read banners, request dedupe, invalidation matrix
  end-to-end.
- **4.2 [A]** Notifications: WorkManager reminders driven by the server's cadence/window from
  `/engagement`, POST-13 notification permission flow, daily capture nudge, streak-at-risk.
- **4.3 [A]** Motion + haptics: shared-element capture thumbnail transition, sheet springs,
  optimistic streak fill, honour reduced-motion.
- **4.4 [A]** Accessibility: TalkBack pass on every screen, 48dp targets, AA contrast audit,
  dynamic type up to 200%, focus order, RTL.
- **4.5 [A]** State audit: every screen has real Loading / Empty / Error / Locked. No spinner
  without a timeout, no error without a retry.
- **4.6 [A]** Telemetry: `POST /engagement` fire-and-forget queue (never blocking navigation,
  never carrying photo bytes or free text), Firebase Analytics funnels, Crashlytics.

### Phase 5 — Quality gates

- **5.1 [A]** Unit tests: error mapper, session state machine, every repository, every ViewModel
  (Turbine).
- **5.2 [A]** Compose UI tests for the five critical journeys: onboarding→consent→baseline,
  capture reject→coach→accept, product→routine event→experiment, Q&A thread continuity,
  delete account.
- **5.3 [A]** Contract tests against the running backend for the `frontend-api-map.md` §11
  mismatches, the 403 gates, and the `204` path.
- **5.4 [A]** Performance: baseline profile, cold-start budget, bitmap memory, StrictMode clean,
  no main-thread I/O.
- **5.5 [A]** Security review: no cleartext traffic in release (`network_security_config`),
  token storage, no `user_id` or PII in logs/analytics URLs, R8 keep rules correct,
  dependency audit.

### Phase 6 — Release

- **6.1 [A]** Branding: adaptive launcher icon in honey, monochrome layer, `SplashScreen` API,
  Play feature graphic, 8 phone screenshots from the emulator, store listing copy, and a
  **privacy policy** that covers facial-image processing explicitly.
- **6.2 [H]** Generate the upload keystore; store it and its passwords outside the repo.
- **6.3 [A]** Signed release AAB with R8, `versionCode`/`versionName` scheme, size report.
- **6.4 [H+A]** Play Console: app record, **Data Safety form declaring facial/biometric data**,
  sensitive-permission declarations for camera, content rating, health-claims review — the
  "cosmetic tracking, never diagnosis" framing must be consistent in the listing, upload to the
  **internal testing** track first.
- **6.5 [A]** CI: GitHub Actions — assemble, unit tests, lint, bundle on tag.
- **6.6 [A]** Documentation correction: rewrite `REPO_STRUCTURE.md`, `QUICK_SUMMARY.md` and
  `JOURNEY.md` to describe what actually exists; add `app/README.md` with real setup steps and
  `DEPLOY.md`.
- **6.7 [H]** Staged rollout: internal → closed testing, monitor Crashlytics, then promote.

---

## 3. Porting reference: what to reuse from `backend/web`, and what to fix

The web app is **not** a deliverable, but it is a working consumer of the same API and saves
real time. Port from it; do not trust it blindly.

**Worth porting more or less directly:**

- `web/lib/api.ts` — 45 typed client methods with request/response shapes. Fastest route to a
  complete `GlowUpApi.kt`. Note the 10 routes it is *missing* (below) — those have no reference.
- `web/components/charts.tsx` — the trend-chart maths: padded domain, gridlines, baseline
  ring markers, nearest-point hit testing, the "needs 2+ captures" fallback, and the accessible
  table alternative. Port the geometry to Compose `Canvas`.
- `web/lib/theme.tsx` + `globals.css` — the honey token values in their final, contrast-checked
  form.
- `web/app/(product)/routine/page.tsx` — the most complete screen in either client. Its
  shelf-scan poll → editable candidate checklist → confirm flow, and its confound-warning
  placement, are the reference for Task 3.4.
- `web/components/ui.tsx` — component inventory to mirror: `Button`, `Card`, `StatTile`, `Tag`,
  `VerdictTag`, `Field`, `EmptyState`, `Skeleton`, `Notice`, `PaywallCard`.

**Routes with no web reference at all — Android builds these from the API docs:**
`GET /subscription`, `GET /products/{id}`, `GET /engagement`,
`GET /experiments/{experiment_id}`, `GET|POST /labels`, `GET /reprocess/{job_id}`.
Plus six methods the web client declares but never calls, so they are unproven:
`health`, `history`, `analytics`, `weekly-recap`, `GET /check-ins`, `POST /engagement`.

**Bugs in the web client — do not reproduce these:**

1. **Experiment status vocabulary.** `capture/page.tsx:83` filters on `status === "active"`,
   which the backend **never emits**. Real values: `planned`, `running`, `paused`, `completed`,
   `cancelled`; a new experiment starts `running`. That filter means the web experiment picker
   is permanently empty.
2. **Premium check is wrong.** `session.tsx:75` checks only `plan === "premium"`. Premium
   requires **both** `plan == "premium"` **and** `status == "active"`, or the client shows
   Premium UI while every gated call `403`s.
3. **Reprocess treated as synchronous.** `account/page.tsx:133` reads `processed_count` off the
   POST response and renders "Reprocessed undefined captures". It is a queued job; poll
   `GET /reprocess/{job_id}`. The web client has no polling method at all.
4. **`Product.ingredients` does not exist.** The backend returns `ingredients_json`, a JSON
   string. The web client types an array and `.join(", ")`s it, silently rendering nothing.
   Parse `ingredients_json` once in the mapper. Note the same field is a comma-joined *string*
   on write — read and write shapes differ.
5. **Q&A `thread_id` is discarded** (`insights/page.tsx:115-132`), starting a new thread on
   every question. Task 3.5 must persist it.
6. `Experiment.started_at` is typed but the column is `start_at` — dormant in web, would be
   `undefined` in Kotlin too.
7. A **hardcoded demo credential is baked into web client source** (`sign-in/page.tsx:11-12`).
   Never carry that pattern into the app.
8. Triage `scope` vs `route`: the web client is actually **correct** here; the API doc is stale
   on that one point. Use `scope`.

## 4. Known traps, carried forward from the audit

1. `POST /api/routine-events` accepts **only** `start`/`stop`/`change`.
2. `rewrites`/base URLs bake at build time — verify the release APK points at the deployed host.
3. The capture quality gate is stricter than it looks; sharpness is measured on a 64×64
   downsample, so aggressive client-side compression can fail it.
4. `GET /dashboard` and `GET /engagement` mutate server state. Do not poll them.
5. Missing entities return `400`, not `404`.
6. Premium checks require **both** `plan == "premium"` and `status == "active"`.
7. Product rows are global, not per-user; `POST /api/products` takes no `user_id` and is not
   idempotent — guard double submits.
8. Backend triage returns `scope`; the web client wrongly declares `route`. Use `scope`.
9. Experiments are stored as `start_at`, not `started_at`.
10. The first accepted capture becomes the baseline even when `is_baseline` is false, and the
    rule counts captures across verticals.

11. Experiment `status` is `running`, never `active`.
12. Premium requires `plan == "premium"` **and** `status == "active"`.
13. `POST /reprocess` and `POST /shelf-scan` are queued jobs — poll their status routes.
14. `ingredients_json` is a JSON string on read, a comma-joined string on write.

## 5. Human-only prerequisites, collected

| # | Item | Blocks |
| --- | --- | --- |
| H1 | Firebase project + `google-services.json` + SHA fingerprints + service account | 1.2, 3.1 |
| H2 | Railway/Render account and provider login | 1.3 |
| H3 | `GEMINI_API_KEY` for production (shelf-scan and Q&A degrade without it) | 1.3 |
| H4 | Upload keystore + passwords | 6.2, 6.3 |
| H5 | Google Play Console developer account ($25) | 6.4 |
| H6 | Privacy policy hosted at a public URL | 6.4 |
