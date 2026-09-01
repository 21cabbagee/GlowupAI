<div align="center">

# GlowUp AI

### Your Personal Skincare Science Lab

Track what actually works for YOUR skin. Scientific experiments. Privacy-first. No guessing.

[![License](https://img.shields.io/badge/License-Proprietary-red.svg)](LICENSE)
[![Android](https://img.shields.io/badge/Platform-Android-green.svg)](https://android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.3.10-purple.svg)](https://kotlinlang.org)
[![Compose](https://img.shields.io/badge/Jetpack%20Compose-Latest-blue.svg)](https://developer.android.com/jetpack/compose)
[![API](https://img.shields.io/badge/Min%20SDK-26-orange.svg)](https://developer.android.com/studio/releases/platforms)

[![Backend CI](https://github.com/piyushxpc7/GlowupAI/workflows/Backend%20CI/badge.svg)](https://github.com/piyushxpc7/GlowupAI/actions/workflows/backend-ci.yml)
[![Android CI](https://github.com/piyushxpc7/GlowupAI/workflows/Android%20CI/badge.svg)](https://github.com/piyushxpc7/GlowupAI/actions/workflows/android-ci.yml)
[![Security Scanning](https://github.com/piyushxpc7/GlowupAI/workflows/Security%20Scanning/badge.svg)](https://github.com/piyushxpc7/GlowupAI/actions/workflows/security.yml)
[![codecov](https://codecov.io/gh/piyushxpc7/GlowupAI/branch/main/graph/badge.svg)](https://codecov.io/gh/piyushxpc7/GlowupAI)

[Features](#-features) • [Architecture](#-architecture) • [Tech Stack](#-tech-stack) • [Getting Started](#-getting-started) • [Contributing](#-contributing)

</div>

---

## Why GlowUp AI?

Stop relying on influencer hype and marketing claims. GlowUp AI brings scientific rigor to your skincare routine by tracking objective metrics over time and letting you run real experiments on your own skin.

### The Problem We're Solving

- **You've tried "everything"** but don't know what actually helped
- **You're wasting money** on products that don't work for you
- **You can't see progress** because changes happen gradually
- **Your data isn't private** - other apps upload your face to random cloud servers
- **You're inconsistent** because you don't have accountability

### Our Solution

A privacy-first Android app that:
- Captures guided selfies with consistent lighting and positioning
- Tracks 6+ skin metrics automatically (redness, texture, tone, hydration, pores, breakouts)
- Lets you run scientific A/B tests on products
- Stores your photos locally on your device by default
- Shows you objective data about what's working

**This is cosmetic tracking, not medical diagnosis.** We're building a tool for people who want to optimize their skincare routine with data, not replace dermatologists.

---

## Features

### Core Features

**Guided Photo Capture**
- CameraX + ML Kit face detection for consistent positioning
- Lighting quality indicators (prevents bad data from inconsistent captures)
- Local storage by default (your face, your device)
- Offline capture with background sync

**Scientific Tracking**
- 6+ automatic skin metrics: redness, texture, tone evenness, pore visibility, hydration, breakouts
- Trend charts showing how your metrics change over time
- Before/after comparisons with side-by-side views
- Product correlation analysis (which products helped/hurt)

**Experimentation Framework**
- Run real A/B tests on skincare products
- Define hypothesis, test duration, and success criteria
- Get data-driven verdicts on what works for YOUR skin
- Track experiments in a structured timeline

**Habit Building**
- Streak tracking with visual calendar heatmaps
- Achievement badges (7, 30, 90+ day milestones)
- Smart reminders at optimal capture times
- Weekly progress summaries

**Product Management**
- Barcode scanning for instant product lookup
- Track all products in your morning/evening routine
- Budget tracking and ROI calculation per product
- Product verdict recommendations based on your metrics

**AI Insights**
- Gemini-powered Q&A assistant for skincare questions
- Evidence-based answers with citations (not medical advice)
- Routine optimization suggestions
- Ingredient compatibility checking

**Privacy-First Architecture**
- Photos stored locally on device by default
- Optional encrypted cloud backup (user controlled)
- Full data export anytime (JSON + photos)
- Delete account and all data instantly
- No sharing without explicit user consent

---

## Architecture

GlowUp AI follows modern Android development best practices with a clean, layered architecture:

```
app/src/main/java/com/glowup/ai/
├── core/
│   ├── design/          # Design system ("Honey" theme - warm amber tones)
│   ├── ui/              # Reusable components (buttons, cards, charts)
│   └── util/            # Result types, extensions, helpers
├── data/
│   ├── remote/          # Retrofit API client, DTOs, interceptors
│   ├── local/           # Room database, DataStore, offline cache
│   ├── repository/      # Single source of truth, cache + network coordination
│   └── work/            # WorkManager (background upload, reminders)
├── domain/
│   ├── model/           # UI-facing domain models
│   └── SessionStateMachine.kt  # Auth state management
├── di/                  # Hilt dependency injection modules
└── feature/             # Feature modules (one per screen/flow)
    ├── auth/            # Firebase authentication
    ├── onboarding/      # User consent and initial setup
    ├── capture/         # Camera capture with ML Kit face detection
    ├── home/            # Dashboard, stats, verdicts
    ├── routine/         # Products, routine events, experiments
    ├── insights/        # Q&A, root-cause analysis, budget optimizer
    ├── discover/        # Recommendations, offers
    └── account/         # Profile, subscription, settings, data export
```

### Backend

- **Python + FastAPI** service exposing ~56 REST endpoints
- **PostgreSQL** (production) or **SQLite** (dev) for persistence
- **Gemini API** for shelf-scan OCR and cited Q&A responses
- Documented endpoint-by-endpoint in `backend/docs/frontend-api-map.md`

The Android app and backend share the same API contract. No separate mobile BFF.

---

## Tech Stack

### Android App

| Category | Technologies |
|----------|-------------|
| **Language** | Kotlin 2.3.10 |
| **UI** | Jetpack Compose + Material 3 |
| **Architecture** | MVVM + MVI, Clean Architecture |
| **Dependency Injection** | Hilt (Dagger) |
| **Navigation** | Navigation Compose + kotlinx.serialization |
| **Networking** | Retrofit + OkHttp + kotlinx.serialization |
| **Local Storage** | Room (SQLite) + DataStore (Preferences) |
| **Background Work** | WorkManager (upload queue, reminders) |
| **Camera** | CameraX + ML Kit Face Detection |
| **Images** | Coil 3.x (async image loading) |
| **Firebase** | Auth, Analytics, Crashlytics |
| **Testing** | JUnit, Espresso, Turbine, MockWebServer, Robolectric |

### Backend

| Category | Technologies |
|----------|-------------|
| **Language** | Python 3.10+ |
| **Framework** | FastAPI |
| **Database** | PostgreSQL (production), SQLite (dev) |
| **AI** | Google Gemini API (optional, degrades gracefully) |
| **Deployment** | Docker, Railway/Render (documented) |

### Design System

The **"Honey"** design system (specified in `backend/docs/ui-revamp-plan.md`):
- Warm amber tones (#FFB84D to #FF8C42 gradients)
- Material 3 components with custom theming
- Accessibility-first (WCAG AA+ contrast ratios)
- Dark mode support throughout
- No glassmorphism or gradient-mesh backgrounds (anti-slop rule)

---

## Getting Started

### Prerequisites

- **Android Studio** Ladybug | 2024.2.1 or later
- **JDK 25** (with bytecode target 17)
- **Android SDK 34+** (compileSdk 35, minSdk 26)
- **Git** for version control
- **Python 3.10+** (if running the backend locally)

### Quick Start (Android App)

1. **Clone the repository**
   ```bash
   git clone https://github.com/yourusername/glowup-ai.git
   cd glowup-ai
   ```

2. **Open in Android Studio**
   - Open the project root directory in Android Studio
   - Wait for Gradle sync to complete

3. **Set up Firebase (optional for first build)**
   ```bash
   # The build works without this, but Firebase features won't initialize
   # See PRODUCTION_READINESS.md for creating a Firebase project
   cp app/google-services.json.example app/google-services.json
   # (Or add your real google-services.json from Firebase Console)
   ```

4. **Configure API endpoint**
   ```bash
   # For local backend testing (emulator)
   # Debug builds default to http://10.0.2.2:8000/
   # (This is how Android emulator accesses host machine's localhost)
   
   # For staging/release, set in gradle.properties:
   # STAGING_API_BASE_URL=https://staging.glowup.example/
   # RELEASE_API_BASE_URL=https://api.glowup.example/
   ```

5. **Run the app**
   - Select `app` run configuration
   - Choose an emulator or physical device (SDK 26+)
   - Click Run (or `./gradlew installDebug`)

### Running the Backend Locally

```bash
cd backend

# Install dependencies
pip install -e .

# Set up environment
cp .env.example .env
# Edit .env and add your GEMINI_API_KEY (optional but recommended)

# Start the server
uvicorn skinproof.complete_api:app --reload --port 8000
```

API docs available at: http://localhost:8000/docs

See `DEPLOY.md` for production deployment to Railway/Render.

---

## Building from Source

### Debug Build

```bash
./gradlew assembleDebug
```

Output: `app/build/outputs/apk/debug/app-debug.apk`

### Release Build

1. **Generate upload keystore** (one-time setup)
   ```bash
   # See KEYSTORE_GENERATION_GUIDE.md for full instructions
   keytool -genkeypair -v \
     -keystore upload-keystore.jks \
     -alias upload \
     -keyalg RSA -keysize 2048 -validity 10000 \
     -storetype JKS
   ```

2. **Configure signing** (git-ignored)
   ```bash
   cp app/keystore.properties.example app/keystore.properties
   # Edit app/keystore.properties with your keystore path and passwords
   ```

3. **Build release APK**
   ```bash
   ./gradlew assembleRelease
   ```

Output: `app/build/outputs/apk/release/app-release.apk`

See `RELEASE_BUILD_GUIDE.md` for Android App Bundle (AAB) instructions for Google Play.

---

## Testing

### Run Unit Tests

```bash
./gradlew test
```

### Run Instrumented Tests

```bash
./gradlew connectedAndroidTest
```

### Test Coverage

Tests cover:
- ViewModels and business logic
- Repository layer (cache + network coordination)
- API error mapping
- Session state machine
- Outbox processor (offline capture sync)
- Request deduplication

See `TESTING_MASTER_PLAN.md` for the full testing strategy.

---

## Contributing

We welcome contributions! Whether you're fixing bugs, adding features, or improving documentation, your help makes GlowUp AI better.

### How to Contribute

1. **Read** [CONTRIBUTING.md](CONTRIBUTING.md) for detailed guidelines
2. **Check** existing issues or create a new one
3. **Fork** the repository
4. **Create** a feature branch (`feature/amazing-feature`)
5. **Make** your changes with clear commit messages
6. **Test** your changes thoroughly
7. **Submit** a pull request

### Good First Issues

Look for issues tagged with `good-first-issue` or `help-wanted`:
- UI polish improvements
- Additional unit tests
- Documentation improvements
- Bug fixes in non-critical paths
- Accessibility enhancements

### Development Guidelines

- Follow Kotlin coding conventions
- Use Jetpack Compose best practices
- Write tests for new features
- Update documentation for API changes
- Follow the "Honey" design system
- Keep PRs focused and atomic

---

## Roadmap

### Current Status

The app is under active development. Core features are implemented but not yet deployed to production.

**Completed:**
- Full Android app with clean architecture
- Backend API (~56 endpoints)
- Camera capture with ML Kit face detection
- Local photo storage + offline sync
- Experiment framework
- Product tracking
- Streak system
- Firebase auth integration

**In Progress:**
- Firebase project setup (blocked on human with Firebase Console access)
- Backend deployment to Railway/Render
- Google Play listing and screenshots
- Release signing with upload keystore

**Planned (Post-Launch):**
- iOS app (Swift + SwiftUI, same backend)
- Social features (anonymous sharing, cohort insights)
- Advanced analytics (root-cause analysis, budget optimizer)
- Wearable integration (sleep/stress data correlation)
- Dermatologist export (PDF reports)

See `PRODUCTION_READINESS.md` for current production blockers and `GLOWUP_AI_GROWTH_BLUEPRINT.md` for long-term vision.

---

## Documentation

| Document | Purpose |
|----------|---------|
| [README.md](README.md) | This file - project overview |
| [CONTRIBUTING.md](CONTRIBUTING.md) | How to contribute to the project |
| [PRODUCTION_READINESS.md](PRODUCTION_READINESS.md) | Production blockers and launch gates |
| [DEPLOY.md](DEPLOY.md) | Backend deployment runbook |
| [app/README.md](app/README.md) | Android-specific build/run setup |
| [REPO_STRUCTURE.md](REPO_STRUCTURE.md) | File tree and organization |
| [backend/docs/frontend-api-map.md](backend/docs/frontend-api-map.md) | API contract (all 56 endpoints) |
| [backend/docs/ui-revamp-plan.md](backend/docs/ui-revamp-plan.md) | Design system ("Honey") specification |

---

## License

**Proprietary** - All rights reserved.

This is currently a closed-source project. Unauthorized copying, distribution, or use of this code is prohibited.

*Note: We're considering an open-source license for future releases. Stay tuned.*

---

## Acknowledgments

- **Jetpack Compose** for making Android UI development actually enjoyable
- **FastAPI** for the cleanest Python web framework
- **ML Kit** for on-device face detection
- **Gemini API** for powering our Q&A assistant
- **Cal.com** for open-source inspiration and best practices

---

## Contact & Support

- **Issues**: [GitHub Issues](https://github.com/yourusername/glowup-ai/issues)
- **Email**: support@glowup.ai
- **Documentation**: See `/docs` in this repo

---

<div align="center">

**Built with passion for people who care about data-driven skincare.**

[Star this repo](https://github.com/yourusername/glowup-ai) if you believe in privacy-first, scientific skincare tracking.

</div>
