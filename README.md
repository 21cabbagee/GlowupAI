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
              │PostgreSQL│    │  Redis   │   │ Firebase │
              │          │    │  Cache   │   │   Auth   │
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
| **Authentication** | Firebase Auth |

### Backend API
| Component | Technology |
|-----------|------------|
| **Language** | Python 3.11+ |
| **Framework** | FastAPI |
| **Database** | PostgreSQL (production), SQLite (dev) |
| **Cache** | Redis |
| **ML Framework** | PyTorch, scikit-learn |
| **Computer Vision** | OpenCV, NumPy, Pillow |
| **Authentication** | Firebase Admin SDK |
| **Error Tracking** | Sentry |
| **Observability** | OpenTelemetry (optional) |
| **Deployment** | Docker + Render.com |

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
# Required
GLOWUPAI_GEMINI_API_KEY=your_gemini_api_key

# Optional - Production
DATABASE_URL=postgresql://user:pass@host:5432/dbname
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
```

**Firebase Setup** (both platforms):
1. Create Firebase project at [console.firebase.google.com](https://console.firebase.google.com)
2. Download `google-services.json` → `app/google-services.json`
3. Enable Authentication > Email/Password
4. Enable Crashlytics

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

### Backend - Render.com

1. **Fork/Clone** this repository
2. **Create Render Web Service**:
   - Build Command: `pip install -e .`
   - Start Command: `uvicorn glowupai.complete_api:app --host 0.0.0.0 --port $PORT`
3. **Set Environment Variables** in Render dashboard
4. **Add PostgreSQL Database** (optional, SQLite works for small scale)

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
- **Render.com** for free tier hosting
- All our contributors and users!

---

## 📞 Contact & Support

- **Issues**: [GitHub Issues](https://github.com/piyushxpc7/GlowupAI/issues)
- **Discussions**: [GitHub Discussions](https://github.com/piyushxpc7/GlowupAI/discussions)
- **Email**: See [SECURITY.md](SECURITY.md) for contact information

---

**Made with ❤️ by the GlowupAI Team**

⭐ Star us on GitHub if you find this project useful!
