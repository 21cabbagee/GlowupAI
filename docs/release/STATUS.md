# GlowUpAI release status

Updated: 2026-09-07  
Revision inspected: `b340e3f` on `feature-branch`  
Scope: Android + FastAPI backend, Google Play target

This ledger records implementation evidence. A task is not `DONE` because a screen or endpoint exists; it needs the acceptance evidence described in `PRODUCTION_PLAN.md` and reviewed against `LUNA_IMPLEMENTATION_GUIDE.md`.

## Baseline evidence

| Check | Result | Evidence |
|---|---|---|
| Android unit tests | PASS | `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew testDebugUnitTest --stacktrace` |
| Android debug lint | PASS | `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew lintDebug --stacktrace` |
| Backend test suite | PASS | Python 3.11 isolated environment: `GLOWUPAI_DISABLE_LOCAL_ENV=1 /private/tmp/glowupai-ops-venv/bin/python -m pytest backend/tests -q --tb=short` — 368 passed, 1 skipped. |
| Device/instrumentation tests | NOT RUN | `adb devices` returned no connected devices |
| SQLite upgrade regression | PASS | `backend/tests/test_sqlite_upgrade.py`; legacy identity columns and capture idempotency upgrade safely |
| Q&A unsafe-answer reporting | IMPLEMENTED | Owner-checked report route, durable `qna_reports` table, Android report action, and admin review route |
| Production Supabase/Play/PostgreSQL/storage | BLOCKED | Credentials, merchant inputs, and deployed staging infrastructure are external inputs |

## Task ledger

| ID | Status | Evidence / next action |
|---|---|---|
| A01 | IN PROGRESS | Clean Python 3.11 verification environment created and used; lockfile/toolchain ownership still needs repository normalization. |
| A02 | IN PROGRESS | This ledger and `DECISIONS.md` created; add release manifest after clean environment. |
| A03 | IN PROGRESS | Release/staging URLs and signing are fail-closed; provision distinct staging Supabase app. |
| A04 | IN PROGRESS | Production settings validate auth, PostgreSQL, HTTPS origins, quality, durable photo key, and admin secret; deploy smoke test pending. |
| A05 | IN PROGRESS | Owner checks cover active private routers; analytics and admin triage routes were corrected; finish route matrix and identity tests. |
| A06 | IN PROGRESS | Android, backend, CodeQL, security, and release workflows no longer suppress failed quality/security gates; hosted CI evidence remains. |
| B01 | IN PROGRESS | Capture detail contract added to Android/FastAPI; publish complete OpenAPI/error/capability contract. |
| B02 | BLOCKED | Requires approved private Supabase Storage provider, region, credentials, migration and restart evidence. |
| B03 | BLOCKED | Current job runner is process-local; durable worker and PostgreSQL lease migration remain. |
| B04 | IN PROGRESS | Capture request idempotency key now survives the Android outbox and has a database uniqueness constraint; quota reservation remains. |
| B05 | NOT STARTED | Produce active-route screenshot inventory and component map. |
| B06 | IN PROGRESS | Existing design system is in use; duplicate token/theme sources and accessibility evidence remain. |
| B07 | IN PROGRESS | Core typed navigation exists; process/deep-link tests remain. |
| C01 | IN PROGRESS | Supabase session bootstrap and account recovery foundations exist; real-provider matrix remains. |
| C02 | IN PROGRESS | Active onboarding now uses the server-backed profile form; persistence regression and skip/default contract remain. |
| C03 | IN PROGRESS | Broad media permissions removed and gallery selection uses Android Photo Picker; purpose-specific consent and device evidence remain. |
| C04 | IN PROGRESS | Server quality gate exists; bounded upload/pixel/EXIF/provenance acceptance remains. |
| C05 | IN PROGRESS | Camera/gallery states exist; OEM, lifecycle, memory, and real-device evidence remain. |
| C06 | IN PROGRESS | Owner-checked capture detail recovery and server idempotency added; cancellation and durable worker remain. |
| C07 | IN PROGRESS | Dashboard/history foundations exist; delete/reset/compatibility and screenshot evidence remain. |
| C08 | IN PROGRESS | Check-ins/reminders exist; timezone, reboot, opt-out and duplicate-event evidence remain. |
| C09 | BLOCKED | Deletion is synchronous and storage/job cleanup is not durable; external alpha remains prohibited. |
| D01 | IN PROGRESS | Routine event foundations exist; schedule/event separation and reconciliation remain. |
| D02 | IN PROGRESS | Catalog/search foundations exist; licensed launch catalog and private custom products remain. |
| D03 | IN PROGRESS | Ingredient/product detail foundations exist; provenance and honest missing-data states remain. |
| D04 | BLOCKED | Durable shelf-scan jobs and provider/storage inputs remain. |
| D05 | IN PROGRESS | Experiment lifecycle exists; limits, transitions and evidence rules remain. |
| D06 | IN PROGRESS | Context and feedback routes exist; identity and consent contract remains. |
| D07 | BLOCKED | Measurement validation and approved association policy are absent. |
| D08 | BLOCKED | Qualified safety/provider review and reporting flow are absent. |
| D09 | IN PROGRESS | Budget/purchase guidance exists; currency, stale price and assumption handling remain. |
| D10 | BLOCKED | Cohort privacy floor and reviewer-approved aggregation are absent. |
| D11 | IN PROGRESS | Offer foundations exist; URL allowlist, expiry and moderation remain. |
| D12 | BLOCKED | Durable export/PDF job and complete evidence remain. |
| D13 | BLOCKED | Versioned reprocessing lineage and rollback evidence remain. |
| E01 | BLOCKED | Google Play product/base-plan and merchant inputs are missing; local checkout remains. |
| E02 | BLOCKED | Play Developer API verification and secure account binding are not implemented. |
| E03 | BLOCKED | Android BillingClient and Play-installed sandbox evidence are not implemented. |
| E04 | BLOCKED | RTDN/reconciliation worker is not implemented. |
| E05 | IN PROGRESS | Premium gates exist but the single rolling-limit capability matrix is not implemented. |
| E06 | IN PROGRESS | Account/privacy screens exist; consent history and backup/privacy evidence remain. |
| E07 | BLOCKED | Durable deletion worker and public deletion request flow remain. |
| F01 | BLOCKED | Consented evaluation set and qualified measurement report are missing. |
| F02 | BLOCKED | Legal/privacy/health review and approved retention decision are missing. |
| F03 | NOT STARTED | Complete real-device accessibility and visual QA. |
| F04 | NOT STARTED | Measure release performance and distribution compatibility. |
| F05 | BLOCKED | Requires production-like PostgreSQL/storage/worker capacity environment. |
| F06 | IN PROGRESS | Telemetry/monitoring foundations exist; alerts and safe operational views remain. |
| F07 | BLOCKED | Full staging Supabase/PostgreSQL/storage/worker/Play regression is unavailable. |
| G01 | BLOCKED | Play owner, merchant, pricing, country and testing inputs are external. |
| G02 | IN PROGRESS | Landing surface exists; public support/privacy/terms/deletion routes and licensed assets remain. |
| G03 | IN PROGRESS | Release configuration refuses debug signing and missing HTTPS inputs; CI now builds and verifies a signed AAB when required secrets are present. |
| G04 | BLOCKED | Console declarations and pre-launch report require the actual release artifact and owner review. |
| G05 | BLOCKED | Closed beta and observed-user evidence require the Play account and staging environment. |
| G06 | BLOCKED | Production rollout is an external deployment milestone. |

## Current release decision

**NO-GO for external testing or Google Play submission.** The code and local test gates are substantially stronger, but the production plan is not fulfilled. Real billing, durable jobs/storage/deletion, measurement/privacy review, deployed staging verification, and Play-installed end-to-end evidence remain required.
