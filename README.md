# GlowUp AI

GlowUp AI is a personal appearance-tracking app: a user takes a periodic
selfie under guided capture conditions, the backend derives quality-gated
metrics and verdicts from it, and the app shows how those metrics move over
time alongside a skincare routine and product/experiment tracking.

**This is cosmetic tracking, not a diagnostic or medical product.** Every
metric, verdict, and Q&A surface in the app is required to carry the
backend's `disclaimer` text, and a triage response with
`scope == "dermatology_review"` is a hand-off to a clinician, not something
the app continues discussing. This framing is a compliance requirement, not
a marketing choice — see `backend/docs/ui-revamp-plan.md` and
`backend/docs/metrics-and-verdicts.md`.

For the production launch checklist and remaining external prerequisites, read
**`PRODUCTION_READINESS.md`** at the repo root. This README is a stable
overview; deployment details live in `DEPLOY.md` and Android setup details
live in `app/README.md`.

## Architecture

Two independent pieces in one repository:

- **`backend/`** — a Python/FastAPI service, `skinproof.complete_api:app`
  (module `backend/skinproof/complete_api.py`, business logic in
  `backend/skinproof/complete_service.py`). It owns everything: users,
  consent, photo captures, metric snapshots, verdicts, routine events,
  experiments, Q&A threads, products, subscriptions, engagement. It exposes
  roughly 56 REST routes, documented endpoint-by-endpoint in
  `backend/docs/frontend-api-map.md`, which also lists known trap fields and
  bugs found in the existing web client. There is also a reference Next.js
  web client under `backend/web/` — it is not a shipped product, just a
  second consumer of the same API kept around as a porting reference.
- **`app/`** — the native Android application, Kotlin + Jetpack Compose,
  package root `com.glowup.ai`. It is a single Gradle module being rebuilt
  against the full backend API. See `app/README.md` for Android-specific
  setup (SDK, JDK, network mirror, signing, Firebase).

The Android app talks to the backend over HTTPS (or `http://10.0.2.2:8000/`
to a local backend from the emulator) using Retrofit. There is no separate
mobile backend-for-frontend — the Android client and the web client consume
the same `complete_api.py` routes.

## Tech stack

**Backend:** Python, FastAPI, SQLite (dev) or PostgreSQL (production, via
`DATABASE_URL`), Gemini API for shelf-scan OCR and cited Q&A (optional —
those two features degrade gracefully without a key), Docker for deployment.

**Android:** Kotlin, Jetpack Compose (Material 3), Hilt for DI, Navigation
Compose, Retrofit + OkHttp + kotlinx.serialization, Room + DataStore for
local persistence and an offline capture outbox, WorkManager for background
upload/reminders, CameraX + ML Kit face detection for guided capture,
Firebase (Auth, Analytics, Crashlytics) for identity and telemetry, Coil for
image loading. Design language is the "Honey" system specified in
`backend/docs/ui-revamp-plan.md` §2 — warm amber tones; glassmorphism and
gradient-mesh backgrounds are explicitly disallowed by that spec's
anti-slop rules.

## Setup

### Backend

```bash
cd backend
pip install -e .
cp .env.example .env   # fill in values as needed; see backend/docs/operations.md
uvicorn skinproof.complete_api:app --reload --port 8000
```

API docs are served at `http://localhost:8000/docs` once running. Deployment
to Railway/Render, the environment variable matrix, and what is and isn't
verified about the production path are documented in `DEPLOY.md`.

### Android app

See `app/README.md` for the full, verified setup: Android SDK requirement,
the local Maven mirror this machine needs, the JDK 25 / bytecode-17
toolchain setup, `local.properties` and `keystore.properties`, the
`google-services.json` requirement for Firebase, per-build-type API base
URLs, and how to run the `glowup_pixel` emulator.

## Current status

The codebase is under active development. Current implementation status and
production blockers are summarized in `QUICK_SUMMARY.md` and
`PRODUCTION_READINESS.md`; this README is the stable product overview.

## Documentation index

- `PRODUCTION_READINESS.md` — production blockers, launch gates, and rollout order.
- `QUICK_SUMMARY.md` — plain-language status summary and what's blocked on
  a human.
- `JOURNEY.md` — project history.
- `REPO_STRUCTURE.md` — the actual file tree.
- `app/README.md` — Android build/run setup.
- `DEPLOY.md` — backend deployment runbook.
- `backend/docs/frontend-api-map.md` — the API contract, endpoint by
  endpoint, with known trap fields.
- `backend/docs/ui-revamp-plan.md` — the design system ("Honey") and its
  anti-slop rules.

## License

Proprietary — all rights reserved. This is a closed-source project;
unauthorized copying, distribution, or use of this code is prohibited.
