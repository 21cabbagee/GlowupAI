# 🌟 GlowUp AI

AI-powered skincare tracking and analysis platform. Track your skin health with computer vision and machine learning.

[![Android CI](https://github.com/piyushxpc7/GlowupAI/actions/workflows/android.yml/badge.svg)](https://github.com/piyushxpc7/GlowupAI/actions/workflows/android.yml)
[![Backend CI](https://github.com/piyushxpc7/GlowupAI/actions/workflows/backend-ci.yml/badge.svg)](https://github.com/piyushxpc7/GlowupAI/actions/workflows/backend-ci.yml)

## 🚀 Features

- **AI Skin Analysis**: Computer vision-powered skin quality metrics
- **Progress Tracking**: Track skin changes over time with photo comparison
- **Smart Insights**: ML-generated personalized skincare recommendations
- **Product Tracking**: Monitor effectiveness of your skincare products
- **Streak System**: Gamified consistency tracking
- **Analytics Dashboard**: Comprehensive skin health visualization

## 📱 Tech Stack

### Android App
- **Language**: Kotlin
- **UI**: Jetpack Compose (Material You)
- **Architecture**: MVVM + Clean Architecture
- **DI**: Hilt
- **Networking**: Retrofit + OkHttp
- **ML**: ML Kit Face Detection
- **Image Processing**: Custom CV pipeline

### Backend
- **Language**: Python 3.11+
- **Framework**: FastAPI
- **Database**: PostgreSQL / SQLite
- **ML**: PyTorch, scikit-learn
- **Authentication**: Firebase Auth
- **Deployment**: Docker, Render

## 🛠️ Development Setup

### Prerequisites
- Android Studio Ladybug+
- JDK 17
- Python 3.11+
- Node.js 18+ (for landing page)

### Backend Setup
```bash
cd backend
python -m venv venv
source venv/bin/activate  # or `venv\Scripts\activate` on Windows
pip install -r requirements.txt

# Set environment variables
export SKINPROOF_FIREBASE_PROJECT_ID=your-project-id
export SKINPROOF_DATABASE_URL=sqlite:///./dev.db

# Run server
python -m uvicorn glowupai.complete_api:app --reload
```

### Android Setup
```bash
# Open project in Android Studio
# Sync Gradle
# Add google-services.json to app/

# Build debug APK
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease
```

## 📊 Project Status

- ✅ Core features complete
- ✅ CI/CD pipelines configured
- ✅ Production backend deployed
- ⏳ Public launch Q1 2026

## 🔒 Privacy & Security

- End-to-end encryption for photo data
- GDPR compliant data handling
- Local-first processing where possible
- No photo storage without explicit consent

## 📄 License

All rights reserved. Private repository.

## 👥 Team

Built by [21cabbage](https://github.com/piyushxpc7)

---

**⚠️ Disclaimer**: GlowUp AI is for cosmetic tracking only. Not intended for medical diagnosis or treatment.
