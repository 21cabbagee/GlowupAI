# GitHub Actions Workflows

This directory contains CI/CD workflows for the GlowupAI project.

## Workflows

### 1. Backend CI (`backend-ci.yml`)

**Purpose**: Test, build, and deploy the Python/FastAPI backend.

**Triggers:**
- Push to `main` or `develop` branches
- Pull requests to `main` or `develop`
- Changes in `backend/**`

**Jobs:**
- **Test & Quality** (Python 3.11, 3.12)
  - Black formatting check
  - Mypy type checking
  - Bandit security scan
  - Pytest with coverage
  - Upload to Codecov
  
- **Docker Build Test**
  - Build Docker image
  - Test image import
  
- **Integration Tests**
  - PostgreSQL service container
  - Complete flow tests
  
- **Deploy to Render** (main branch only)
  - Trigger deployment hook
  - Health check
  - Notify on success

### 2. Android CI (`android-ci.yml`)

**Purpose**: Test and build the Android/Kotlin app.

**Triggers:**
- Push to `main` or `develop` branches
- Pull requests to `main` or `develop`
- Changes in `app/**`, gradle files

**Jobs:**
- **Build & Test**
  - Lint checks
  - Unit tests
  - Build debug APK
  - Upload artifacts
  
- **Instrumentation Tests**
  - Android Emulator (API 30)
  - Compose UI tests
  - Upload test reports
  
- **Code Quality**
  - Detekt static analysis
  - Ktlint formatting

### 3. Security Scanning (`security.yml`)

**Purpose**: Automated security vulnerability detection.

**Triggers:**
- Push to `main` or `develop`
- Pull requests
- Daily at 2 AM UTC
- Manual workflow dispatch

**Jobs:**
- **Dependency Scan** (Snyk)
  - Python dependencies
  - Gradle dependencies
  
- **Secret Scan** (Gitleaks)
  - Git history scan
  - AWS credentials check
  - Hardcoded secret patterns
  
- **Code Analysis**
  - Bandit (Python security)
  - Safety Check (Python deps)
  
- **OWASP Dependency Check**
  - Android CVE scan
  - CVSS 7+ fails build
  
- **Android Security**
  - Lint security checks
  - Configuration validation
  
- **Docker Image Scan** (Trivy)
  - Main branch only
  - SARIF format upload
  
- **Security Summary**
  - Aggregate all results
  - Comment on PR

## Required Secrets

Configure these in **Settings → Secrets and variables → Actions**:

### Backend
- `RENDER_DEPLOY_HOOK` - Render deployment webhook URL
- `CODECOV_TOKEN` - Codecov upload token

### Security
- `SNYK_TOKEN` - Snyk API token
- `GITLEAKS_LICENSE` - Gitleaks license (optional)

### Android Release
- `KEYSTORE_BASE64` - Base64-encoded release keystore
- `KEYSTORE_PASSWORD` - Keystore password
- `KEY_ALIAS` - Signing key alias
- `KEY_PASSWORD` - Key password

## Workflow Status

Check status at: https://github.com/piyushxpc7/GlowupAI/actions

[![Backend CI](https://github.com/piyushxpc7/GlowupAI/workflows/Backend%20CI/badge.svg)](https://github.com/piyushxpc7/GlowupAI/actions/workflows/backend-ci.yml)
[![Android CI](https://github.com/piyushxpc7/GlowupAI/workflows/Android%20CI/badge.svg)](https://github.com/piyushxpc7/GlowupAI/actions/workflows/android-ci.yml)
[![Security Scanning](https://github.com/piyushxpc7/GlowupAI/workflows/Security%20Scanning/badge.svg)](https://github.com/piyushxpc7/GlowupAI/actions/workflows/security.yml)

---

For detailed testing documentation, see [TESTING.md](../../TESTING.md).
