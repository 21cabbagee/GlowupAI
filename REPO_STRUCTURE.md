# Repository Structure

This is the actual file tree, verified by listing the directories directly
(not copied from an earlier, inaccurate version of this document). It will
drift as the Android rebuild in `ANDROID_PLAN.md` continues; re-run `find`
if you need it exact for a specific commit.

```
-GlowUpAI/
├── ANDROID_PLAN.md          # Working plan + ground-truth status audit — read this first
├── DEPLOY.md                # Backend deployment runbook (Railway/Render)
├── README.md                # Project overview
├── REPO_STRUCTURE.md        # This file
├── QUICK_SUMMARY.md         # Status summary, what's blocked on a human
├── JOURNEY.md               # Project history
├── build.gradle.kts         # Root Gradle build (plugin aliases only)
├── settings.gradle.kts      # Gradle repositories incl. the Maven Central mirror (see app/README.md)
├── gradle.properties
├── gradlew / gradlew.bat
├── gradle/
│   ├── wrapper/
│   └── libs.versions.toml   # Version catalog — every dependency version lives here
│
├── app/                     # Android app, Gradle module ":app", package com.glowup.ai
│   ├── README.md            # Android-specific setup (SDK, JDK, mirror, signing, Firebase)
│   ├── build.gradle.kts
│   ├── proguard-rules.pro
│   ├── keystore.properties.example   # Copy to keystore.properties (git-ignored) for release signing
│   ├── .gitignore
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml
│       │   ├── java/com/glowup/ai/
│       │   │   ├── GlowUpApplication.kt      # @HiltAndroidApp
│       │   │   ├── MainActivity.kt
│       │   │   ├── core/
│       │   │   │   ├── design/               # Tokens, ColorScheme, Type, Shapes, Spacing, Motion, Theme,
│       │   │   │   │                          # DesignSystemPreview (the "Honey" design system)
│       │   │   │   ├── ui/                   # Reusable components: GlowButton, GlowCard, StatTile,
│       │   │   │   │                          # VerdictChip, LockedCard, EmptyState, ErrorState,
│       │   │   │   │                          # ShimmerSkeleton, StreakRing, MetricBar, etc.
│       │   │   │   └── util/                 # GlowResult and similar helpers
│       │   │   ├── data/
│       │   │   │   ├── remote/               # GlowUpApi (Retrofit), DTOs (one file per domain family),
│       │   │   │   │   └── dto/              # ApiError/ApiErrorMapper, AuthInterceptor, NetworkFactory,
│       │   │   │   │                          # RetryPolicyInterceptor, RedactingLoggingInterceptor
│       │   │   │   ├── local/                # SessionStore (DataStore), GlowUpDatabase +
│       │   │   │   │                          # GlowUpOutboxDatabase (Room), CacheEntities, Converters
│       │   │   │   ├── repository/           # One repository per domain family (Home, Capture, Routine,
│       │   │   │   │   └── support/          # Insights, Discover, Experiment, Billing, Privacy, Session)
│       │   │   │   │                          # + CacheInvalidationBus, KeyedMemoryCache, RequestDeduplicator
│       │   │   │   └── work/                 # WorkManager: CaptureUploadWorker, CaptureOutboxProcessor,
│       │   │   │                              # ReminderWorker/Notifier, WorkScheduler
│       │   │   ├── di/                       # Hilt modules: AppModule, NetworkModule, DispatcherModule
│       │   │   ├── domain/
│       │   │   │   ├── model/                # UI-facing models (Profile, Dashboard, Capture, Product,
│       │   │   │   │                          # Experiment, Routine, Insights, Account, Admin, Jobs, Enums)
│       │   │   │   ├── SessionState.kt
│       │   │   │   └── SessionStateMachine.kt
│       │   │   └── feature/                  # One package per feature, each with Screen/ViewModel/UiState:
│       │   │       ├── shell/                # GlowUpApp, GlowNavGraph, GlowDestination — app shell + nav
│       │   │       ├── auth/                 # Sign-in, splash, Firebase auth gateway
│       │   │       ├── onboarding/           # Onboarding + consent
│       │   │       ├── capture/              # Camera capture, ML Kit face-quality analyzer, result screen
│       │   │       ├── home/                 # Dashboard, stats, verdicts, history trend chart, check-ins
│       │   │       ├── routine/              # Products, routine events, shelf-scan, experiments
│       │   │       ├── insights/             # Q&A, root-cause, context log, derm export, budget optimizer
│       │   │       ├── discover/             # Cohort recommendations, offers, product prediction
│       │   │       └── account/              # Profile, subscription/paywall, settings, data export
│       │   └── res/                          # Standard Android resources (themes, colors, mipmaps, xml)
│       ├── debug/res/xml/network_security_config.xml
│       ├── test/java/com/glowup/ai/          # JVM unit tests (ApiErrorMapper, domain mapping, session
│       │                                      # state machine, cache/dedup, outbox processor, ...)
│       └── androidTest/java/com/glowup/ai/   # Instrumented tests
│
└── backend/                 # Python/FastAPI backend — the API contract
    ├── README.md
    ├── pyproject.toml
    ├── Dockerfile
    ├── docker-compose.yml
    ├── railway.json
    ├── .env.example
    ├── skinproof/                     # The application package
    │   ├── complete_api.py            # FastAPI app: skinproof.complete_api:app — ~56 routes
    │   ├── complete_service.py        # Business logic behind complete_api.py
    │   ├── complete_db.py             # Picks Postgres or SQLite backend
    │   ├── db.py / full_db.py / postgres_db.py   # Storage implementations
    │   ├── auth.py                    # Firebase ID token verification (JWKS-based)
    │   ├── config.py                  # Settings / environment variables
    │   ├── capture.py, metrics.py, insights.py, jobs.py, catalog.py,
    │   │   attribution.py, safety.py, photos.py, google_ai.py, cli.py
    │   ├── api.py / api_legacy.py     # Earlier/legacy API surface, not the active app
    │   ├── migrations/                # 0001–0004 SQL migrations, applied on boot in order
    │   └── static/
    ├── tests/                         # Backend test suite (unittest-based)
    ├── docs/
    │   ├── frontend-api-map.md        # The API contract: every route, request/response shape, traps
    │   ├── architecture.md
    │   ├── metrics-and-verdicts.md
    │   ├── ui-revamp-plan.md          # The "Honey" design system + anti-slop rules
    │   ├── experience-redesign.md
    │   ├── superiority-plan.md
    │   ├── google-gemini.md
    │   └── operations.md
    └── web/                           # Reference Next.js client (not a shipped product)
        ├── app/ (product)/…           # Pages: routine, capture, insights, account, sign-in, ...
        ├── components/                # ui.tsx, charts.tsx — porting reference for Compose
        ├── lib/                       # api.ts (typed client), theme.tsx
        └── package.json
```

## What is not in this tree

There is no `skinproof/main.py`, `models.py`, `schemas.py`, `api/` package,
or `services/` package — an earlier version of this document described
those files and they never existed. The real FastAPI entry point is
`skinproof.complete_api:app`, and its business logic lives in
`complete_service.py`, not a `services/` package.
