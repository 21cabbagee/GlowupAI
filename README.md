# 🌟 GlowupAI

AI-powered skincare tracking and analysis platform. Track your skin health journey with computer vision, machine learning, and evidence-based tracking.

[![Android CI](https://github.com/piyushxpc7/GlowupAI/actions/workflows/android-ci.yml/badge.svg)](https://github.com/piyushxpc7/GlowupAI/actions/workflows/android-ci.yml)
[![Backend CI](https://github.com/piyushxpc7/GlowupAI/actions/workflows/backend-ci.yml/badge.svg)](https://github.com/piyushxpc7/GlowupAI/actions/workflows/backend-ci.yml)
[![Security Scanning](https://github.com/piyushxpc7/GlowupAI/actions/workflows/security.yml/badge.svg)](https://github.com/piyushxpc7/GlowupAI/actions/workflows/security.yml)
[![CodeQL](https://github.com/piyushxpc7/GlowupAI/actions/workflows/codeql.yml/badge.svg)](https://github.com/piyushxpc7/GlowupAI/actions/workflows/codeql.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![PRs Welcome](https://img.shields.io/badge/PRs-welcome-brightgreen.svg)](CONTRIBUTING.md)

---

## 📖 Table of Contents

- [Features](#-features)
- [Architecture](#-architecture)
- [Tech Stack](#-tech-stack)
- [Getting Started](#-getting-started)
- [Development Setup](#-development-setup)
- [Deployment](#-deployment)
- [Contributing](#-contributing)
- [Security](#-security)
- [License](#-license)

---

## ✨ Features

### 📸 Smart Capture System
- **Standardized Photo Protocol**: Consistent capture conditions for accurate comparison
- **Quality Validation**: Real-time feedback on lighting, distance, and pose
- **Face Detection**: ML Kit integration for optimal alignment
- **Reference Card Support**: Optional color/size calibration

### 📊 Evidence-Based Tracking
- **Quantitative Metrics**: Redness, blemishes, texture, dark spots
- **Noise Floor Estimation**: Statistical confidence in changes
- **Baseline Comparison**: Track improvements against your starting point
- **Time-Series Visualization**: See your progress over weeks and months

### 💊 Product Experiments
- **Controlled Testing**: Start one product at a time
- **Stabilization Windows**: Account for product adjustment period
- **Verdict System**: Rate effectiveness based on your data
- **Confound Detection**: Identify variables that might affect results

### 🔬 Cohort Insights (Premium)
- **Discover Similar Products**: What works for people like you
- **Privacy-Preserved**: Anonymized aggregation, no identifiable data
- **Minimum Cohort Size**: Statistical validity requirements
- **No Paid Placement**: Only evidence-based recommendations

### 📈 Analytics Dashboard
- **Historical Trends**: Visualize metrics over time
- **Root Cause Analysis**: Correlate changes with routine events
- **Weekly Recaps**: Automated progress summaries
- **Export for Dermatologist**: Professional-ready reports

---

## 🏗️ Architecture

```
┌─────────────────┐         ┌──────────────────┐
│   Android App   │ <-----> │  FastAPI Backend │
│  (Kotlin/Compose)│         │   (Python 3.11)  │
└─────────────────┘         └──────────────────┘
                                     │
                    ┌────────────────┼────────────────┐
                    │                │                │
              ┌──────────┐    ┌──────────┐   ┌──────────┐
              │ Supabase │    │  Redis   │   │ Supabase │
              │ Postgres │    │  Cache   │   │ Auth +   │
              │ +Storage │    │          │   │ Storage  │
              └──────────┘    └──────────┘   └──────────┘
```

### Key Design Principles
1. **Privacy-First**: User data stays local, explicit consent for features
2. **Offline-Capable**: Core functionality works without internet
3. **Evidence-Based**: ML augments, doesn't replace, user judgment
4. **Transparent**: Open algorithms, explainable metrics
5. **Extensible**: Plugin architecture for new verticals (skin, hair, nails)

---

## 🛠️ Tech Stack

### Android App
| Component | Technology |
|-----------|------------|
| **Language** | Kotlin 1.9+ |
| **UI Framework** | Jetpack Compose (Material 3) |
| **Architecture** | MVVM + Clean Architecture |
| **Dependency Injection** | Hilt/Dagger |
| **Networking** | Retrofit + OkHttp |
| **Database** | Room + DataStore |
| **ML/CV** | ML Kit Face Detection |
| **Image Processing** | OpenCV Android |
| **Charts** | Vico Compose |
| **Authentication** | Supabase Auth (Google + Email/Password) |

### Backend API
| Component | Technology |
|-----------|------------|
| **Language** | Python 3.11+ |
| **Framework** | FastAPI |
| **Database** | Supabase PostgreSQL (production), SQLite (dev) |
| **Cache** | Redis |
| **ML Framework** | PyTorch, scikit-learn |
| **Computer Vision** | OpenCV, NumPy, Pillow |
| **Authentication** | Backend-verified Supabase JWT |
| **Image Storage** | Private Supabase Storage bucket |
| **Error Tracking** | Sentry |
| **Observability** | OpenTelemetry (optional) |
| **Deployment** | Vercel Functions |

### CI/CD & DevOps
- **GitHub Actions**: Automated testing, building, security scanning
- **CodeQL**: Static analysis security testing
- **Dependabot**: Automated dependency updates
- **Trivy**: Docker vulnerability scanning
- **Bandit**: Python security linter
- **Black**: Python code formatting
- **ktlint**: Kotlin code formatting

---

## 🚀 Getting Started

### Prerequisites
- **Android Development**: Android Studio Ladybug+, JDK 17
- **Backend Development**: Python 3.11+, PostgreSQL (optional)
- **Git LFS**: For model files (if contributing ML changes)

### Quick Start - Android App

```bash
# Clone the repository
git clone https://github.com/piyushxpc7/GlowupAI.git
cd GlowupAI

# Open in Android Studio
# File > Open > Select GlowupAI directory

# Build and run
./gradlew assembleDebug
./gradlew installDebug
```

### Quick Start - Backend API

```bash
cd backend

# Create virtual environment
python -m venv venv
source venv/bin/activate  # On Windows: venv\Scripts\activate

# Install dependencies
pip install -e ".[dev]"

# Set up environment variables
cp .env.example .env
# Edit .env with your configuration

# Run migrations (if using PostgreSQL)
python -m glowupai.migrations

# Start development server
uvicorn glowupai.complete_api:app --reload
```

Visit `http://localhost:8000/api/docs` for interactive API documentation.

---

## 💻 Development Setup

### Environment Variables

**Backend** (`.env`):
```env
# AI providers (backend-only; never add these to Android or browser code)
OPENAI_API_KEY=your_openai_api_key
GLOWUPAI_LUNA_ENABLED=1
GLOWUPAI_LUNA_MONTHLY_SPEND_CAP_USD=5
GEMINI_API_KEY=your_gemini_api_key
GLOWUPAI_GEMINI_ENABLED=1

# Optional - Production/staging
SUPABASE_URL=https://your-project.supabase.co
SUPABASE_DB_URL=postgresql://postgres:[PASSWORD]@db.[PROJECT-REF].supabase.co:5432/postgres
SUPABASE_JWT_SECRET=server_only_jwt_secret
SUPABASE_SERVICE_ROLE_KEY=server_only_service_role_key
SUPABASE_STORAGE_BUCKET=user-images
REDIS_URL=redis://localhost:6379
GLOWUPAI_ADMIN_TOKEN=secure_random_token
SENTRY_DSN=your_sentry_dsn

# Optional - Development
GLOWUPAI_ENV=development
GLOWUPAI_LOG_LEVEL=DEBUG
```

**Android** (`local.properties`):
```properties
# Local backend URL for development
DEBUG_API_BASE_URL=http://10.0.2.2:8000/api/
# Public Supabase values only; never put service-role/JWT/database secrets here.
DEBUG_SUPABASE_URL=https://your-development-project.supabase.co
DEBUG_SUPABASE_ANON_KEY=your_public_anon_or_publishable_key
# OAuth 2.0 Web client ID from the Google Cloud project connected to Supabase Auth.
# This is public (not a client secret) and enables the native Google account chooser.
DEBUG_GOOGLE_WEB_CLIENT_ID=1234567890-abc123.apps.googleusercontent.com
```

**Supabase Setup** (development, staging, and production):
1. Create a separate Supabase project for each environment.
2. Enable Google and Email/Password providers in Supabase Auth.
3. Create a private Storage bucket named `user-images` (and `user-images-staging` for staging).
4. Set `SUPABASE_URL`, `SUPABASE_DB_URL`, `SUPABASE_JWT_SECRET`, `SUPABASE_SERVICE_ROLE_KEY`, and `SUPABASE_STORAGE_BUCKET` only on the backend.
5. Set `DEBUG_*`, `STAGING_*`, or `RELEASE_*` URL/anon-key values on the Android build; the service-role key and JWT secret never ship in the APK. Also set the matching `*_GOOGLE_WEB_CLIENT_ID` to the Google OAuth **Web application** client ID configured for the Supabase Google provider. It is a public identifier, never a client secret.
6. For CI/release builds, add `RELEASE_GOOGLE_WEB_CLIENT_ID` to GitHub Actions secrets (alongside the existing `RELEASE_SUPABASE_*` secrets). The Android workflow passes it into the signed APK at build time; it is not stored in the backend database.

### Running Tests

**Backend:**
```bash
cd backend

# Run all tests with coverage
pytest tests/ --cov=glowupai --cov-report=html

# Run specific test file
pytest tests/test_router_refactoring.py -v

# Run with specific markers
pytest -m unit          # Only unit tests
pytest -m integration   # Only integration tests
```

**Android:**
```bash
# Unit tests
./gradlew testDebugUnitTest

# Instrumentation tests (requires emulator)
./gradlew connectedDebugAndroidTest

# Lint checks
./gradlew lintDebug

# Code formatting
./gradlew ktlintFormat
```

### Pre-commit Hooks

```bash
# Install pre-commit
pip install pre-commit

# Install hooks
pre-commit install

# Run manually
pre-commit run --all-files
```

---

## 🌐 Deployment

### Backend - Vercel

1. **Fork/Clone** this repository
2. **Link the `backend/` directory to the Vercel `backend` project** and deploy it with `vercel --prod`.
3. **Set the Supabase environment variables** from `backend/.env.production.template` in the Vercel project settings.
4. **Use the Supabase PostgreSQL connection string** and private Storage bucket.
5. **Configure production operations** from [`docs/PRODUCTION_OPERATIONS.md`](docs/PRODUCTION_OPERATIONS.md): shared Redis, alerts, daily verified backups, protected metrics, and Vercel rollback access.

### Android - Release Build

```bash
# Generate release keystore (first time only)
keytool -genkey -v -keystore release.keystore -alias glowupai -keyalg RSA -keysize 2048 -validity 10000

# Create keystore.properties
echo "storeFile=../release.keystore
storePassword=YOUR_STORE_PASSWORD
keyAlias=glowupai
keyPassword=YOUR_KEY_PASSWORD" > app/keystore.properties

# Build release APK
RELEASE_API_BASE_URL=https://backend-piyushcapitals-4171.vercel.app/api/ \
RELEASE_SUPABASE_URL=https://your-production-project.supabase.co \
RELEASE_SUPABASE_ANON_KEY=your_public_anon_or_publishable_key \
RELEASE_GOOGLE_WEB_CLIENT_ID=1234567890-abc123.apps.googleusercontent.com \
./gradlew assembleRelease

# Output: app/build/outputs/apk/release/app-release.apk
```

---

## 🤝 Contributing

We love contributions! Please see our [Contributing Guide](CONTRIBUTING.md) for details.

### Quick Contribution Steps

1. **Fork** the repository
2. **Create** a feature branch (`git checkout -b feature/amazing-feature`)
3. **Commit** your changes (`git commit -m 'feat: add amazing feature'`)
4. **Push** to the branch (`git push origin feature/amazing-feature`)
5. **Open** a Pull Request

### Development Resources

- 📖 [Contributing Guide](CONTRIBUTING.md)
- 🐛 [Report a Bug](https://github.com/piyushxpc7/GlowupAI/issues/new?template=bug_report.yml)
- ✨ [Request a Feature](https://github.com/piyushxpc7/GlowupAI/issues/new?template=feature_request.yml)
- 💬 [Discussions](https://github.com/piyushxpc7/GlowupAI/discussions)

---

## 🔒 Security

Security is a top priority. Please see [SECURITY.md](SECURITY.md) for:
- Vulnerability reporting process
- Supported versions
- Security features
- Best practices

**Found a vulnerability?** Please email the maintainers privately rather than opening a public issue.

---

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

```
MIT License

Copyright (c) 2026 GlowupAI

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction...
```

---

## 🙏 Acknowledgments

- **ML Kit** by Google for face detection
- **OpenCV** for image processing
- **FastAPI** for the backend framework
- **Jetpack Compose** for modern Android UI
- **Vercel** for backend hosting
- All our contributors and users!

---

## 📞 Contact & Support

- **Issues**: [GitHub Issues](https://github.com/piyushxpc7/GlowupAI/issues)
- **Discussions**: [GitHub Discussions](https://github.com/piyushxpc7/GlowupAI/discussions)
- **Email**: See [SECURITY.md](SECURITY.md) for contact information

---

**Made with ❤️ by the GlowupAI Team**

⭐ Star us on GitHub if you find this project useful!
