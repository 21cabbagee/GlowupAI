# GlowupAI Testing Documentation

Comprehensive testing strategy and guidelines for the GlowupAI monorepo.

## Table of Contents

- [Overview](#overview)
- [Backend Tests](#backend-tests)
- [Android Tests](#android-tests)
- [CI/CD Pipeline](#cicd-pipeline)
- [Running Tests Locally](#running-tests-locally)
- [Coverage Requirements](#coverage-requirements)
- [Contributing](#contributing)

## Overview

GlowupAI employs a multi-layered testing strategy to ensure reliability, security, and performance:

- **Unit Tests**: Test individual components in isolation
- **Integration Tests**: Test interactions between components
- **UI Tests**: Test user interface and user flows
- **Load Tests**: Test performance under concurrent load
- **Security Scans**: Automated vulnerability detection

### Quality Gates

All changes must pass these gates before merging:

✅ All tests pass (100% pass rate)  
✅ Code coverage ≥ 70%  
✅ No high-severity security vulnerabilities  
✅ Lint checks pass  
✅ Performance benchmarks met (p95 < 2s)

## Backend Tests

### Location
```
backend/tests/
├── __init__.py
├── test_analysis.py          # Analysis pipeline tests
├── test_api.py                # API endpoint tests
├── test_auth.py               # Authentication tests
├── test_database.py           # Database CRUD tests
├── integration/
│   ├── __init__.py
│   └── test_flows.py          # End-to-end flow tests
└── load/
    ├── __init__.py
    └── locustfile.py          # Load testing scenarios
```

### Unit Tests

#### Analysis Pipeline (`test_analysis.py`)
Tests for face detection, metrics calculation, and quality gates.

```bash
# Run analysis tests
cd backend
pytest tests/test_analysis.py -v
```

**Coverage:**
- Face detection accuracy
- Metric calculation consistency
- Quality validation logic
- Error handling for invalid images

#### Database Operations (`test_database.py`)
Tests for CRUD operations on users, captures, products, and routine events.

```bash
# Run database tests
pytest tests/test_database.py -v
```

**Coverage:**
- User creation and retrieval
- Consent management
- Capture storage and queries
- Product and routine tracking
- Data deletion and privacy

#### API Endpoints (`test_api.py`)
Tests for FastAPI endpoints and request/response handling.

```bash
# Run API tests
pytest tests/test_api.py -v
```

**Coverage:**
- HTTP request validation
- Response formats
- Error codes (400, 403, 404, 422)
- Authentication middleware

### Integration Tests

#### Complete User Flows (`integration/test_flows.py`)
End-to-end tests simulating real user journeys.

```bash
# Run integration tests
pytest tests/integration/ -v
```

**Tested Flows:**
1. **Sign up → Consent → First Capture → Dashboard**
2. **Product Experiment Flow**
   - Create product
   - Start routine
   - Multiple captures
   - Compare results
3. **Quality Gates Enforcement**
4. **Error Handling**
5. **Rate Limiting**

### Load Tests

#### Locust Load Testing (`load/locustfile.py`)
Simulates concurrent users to test performance and scalability.

```bash
# Run load tests (local)
cd backend
locust -f tests/load/locustfile.py --host=http://localhost:8000

# Run headless with 100 users
locust -f tests/load/locustfile.py \
  --host=http://localhost:8000 \
  --users 100 \
  --spawn-rate 10 \
  --run-time 5m \
  --headless
```

**Test Scenarios:**
- **GlowupUser**: Standard user behavior (capture, dashboard, products)
- **MobileAppUser**: Mobile-specific patterns (70% returning users)
- **AdminUser**: Analytics and admin operations
- **StressTest**: Peak load (200 concurrent users)
- **SoakTest**: Sustained load over time (1 hour)

**Performance Targets:**
- p50: < 500ms
- p95: < 2s
- p99: < 5s
- Error rate: < 0.1%

### Running All Backend Tests

```bash
cd backend

# Install test dependencies
pip install -e ".[dev]"
pip install pytest-cov locust

# Run all unit tests with coverage
pytest tests/ \
  --cov=skinproof \
  --cov-report=html \
  --cov-report=term \
  -v

# Run only fast tests (exclude load tests)
pytest tests/ -m "not slow" -v

# Run specific test file
pytest tests/test_analysis.py -v

# Run with verbose output and stop on first failure
pytest tests/ -vx
```

## Android Tests

### Location
```
app/src/
├── test/java/com/glowup/ai/
│   ├── ExampleUnitTest.kt
│   ├── domain/
│   │   └── SessionStateMachineTest.kt
│   └── viewmodel/
│       ├── HomeViewModelTest.kt
│       └── CaptureViewModelTest.kt
└── androidTest/java/com/glowup/ai/
    ├── ExampleInstrumentedTest.kt
    └── ui/
        ├── OnboardingFlowTest.kt
        └── HomeScreenTest.kt
```

### Unit Tests (JVM)

#### ViewModel Tests
Test business logic and state management without Android dependencies.

```bash
# Run all unit tests
./gradlew testDebugUnitTest

# Run specific test class
./gradlew testDebugUnitTest --tests HomeViewModelTest

# Run with coverage
./gradlew testDebugUnitTest jacocoTestReport
```

**HomeViewModelTest:**
- Dashboard loading
- Streak calculation
- Error handling
- State updates
- Refresh logic

**CaptureViewModelTest:**
- Image capture flow
- Quality validation
- Upload handling
- Retry logic
- Baseline marking

### UI Tests (Instrumented)

#### Compose UI Tests
Test user interface and interactions on real devices/emulators.

```bash
# Run all instrumented tests
./gradlew connectedDebugAndroidTest

# Run on specific device
./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.glowup.ai.ui.OnboardingFlowTest
```

**OnboardingFlowTest:**
- Complete onboarding journey
- Skip to sign-in
- Consent requirements
- Accessibility checks

**HomeScreenTest:**
- Dashboard display
- Streak visualization
- Navigation flows
- Pull-to-refresh
- Theme toggle
- Accessibility

### Integration Tests

Test offline behavior, data persistence, and API mocking.

```bash
# Run integration tests
./gradlew connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.glowup.ai.integration.*
```

### Running All Android Tests

```bash
# All local unit tests
./gradlew test

# All instrumented tests
./gradlew connectedAndroidTest

# Lint checks
./gradlew lint

# Code quality (Detekt)
./gradlew detekt

# All checks
./gradlew check
```

## CI/CD Pipeline

### GitHub Actions Workflows

#### Backend CI (`.github/workflows/backend-ci.yml`)

**Triggers:**
- Push to `main`, `develop`
- Pull requests to `main`, `develop`
- Changes in `backend/**`

**Jobs:**
1. **Test & Quality** (Python 3.11, 3.12)
   - Black formatting check
   - Mypy type checking
   - Bandit security scan
   - Pytest with coverage
   - Upload to Codecov

2. **Docker Build Test**
   - Build Docker image
   - Test image import

3. **Integration Tests**
   - PostgreSQL service
   - Full flow tests

4. **Deploy to Render** (main branch only)
   - Trigger deployment hook
   - Health check
   - Notify on success

#### Android CI (`.github/workflows/android-ci.yml`)

**Triggers:**
- Push to `main`, `develop`
- Pull requests to `main`, `develop`
- Changes in `app/**`, gradle files

**Jobs:**
1. **Build & Test**
   - Lint checks
   - Unit tests
   - Build debug APK
   - Upload artifacts

2. **Instrumentation Tests**
   - Android emulator (API 30)
   - UI tests
   - Upload test reports

3. **Code Quality**
   - Detekt analysis
   - Ktlint formatting
   - Upload reports

#### Security Scanning (`.github/workflows/security.yml`)

**Triggers:**
- Push to `main`, `develop`
- Pull requests
- Daily at 2 AM UTC
- Manual workflow dispatch

**Jobs:**
1. **Dependency Scan** (Snyk)
2. **Secret Scan** (Gitleaks)
3. **Code Analysis** (Bandit, Safety)
4. **OWASP Dependency Check**
5. **Android Security**
6. **Docker Image Scan** (Trivy)
7. **Security Summary** (aggregated report)

### Setting Up CI/CD

#### Required Secrets

Add these secrets in GitHub Settings → Secrets and variables → Actions:

**Backend:**
- `RENDER_DEPLOY_HOOK`: Render deployment webhook URL
- `CODECOV_TOKEN`: Codecov upload token

**Security:**
- `SNYK_TOKEN`: Snyk API token
- `GITLEAKS_LICENSE`: Gitleaks license key (optional)

**Android (for release builds):**
- `KEYSTORE_BASE64`: Base64-encoded release keystore
- `KEYSTORE_PASSWORD`: Keystore password
- `KEY_ALIAS`: Signing key alias
- `KEY_PASSWORD`: Key password

#### Enabling Workflows

Workflows run automatically on push/PR. To enable:

1. Navigate to **Actions** tab in GitHub
2. Select workflow
3. Click **Enable workflow**

## Running Tests Locally

### Prerequisites

**Backend:**
```bash
cd backend
python -m venv venv
source venv/bin/activate  # or `venv\Scripts\activate` on Windows
pip install -e ".[dev]"
pip install pytest-cov locust
```

**Android:**
```bash
# Ensure Android SDK is installed
# Set ANDROID_HOME environment variable
./gradlew --version
```

### Backend Tests

```bash
cd backend

# All tests with coverage
pytest tests/ --cov=skinproof --cov-report=html -v

# Specific test suite
pytest tests/test_analysis.py -v
pytest tests/integration/ -v

# Load tests (requires running server)
# Terminal 1: Start server
uvicorn skinproof.api:app --reload

# Terminal 2: Run locust
locust -f tests/load/locustfile.py --host=http://localhost:8000
# Open http://localhost:8089 in browser
```

### Android Tests

```bash
# Unit tests (fast, no device needed)
./gradlew testDebugUnitTest

# UI tests (requires emulator or device)
# Start emulator first
emulator -avd Pixel_5_API_30

# Run tests
./gradlew connectedDebugAndroidTest

# Generate coverage report
./gradlew testDebugUnitTest jacocoTestReport
open app/build/reports/jacoco/testDebugUnitTest/html/index.html
```

### Pre-commit Checks

Run these before committing:

**Backend:**
```bash
cd backend
black skinproof tests  # Format code
mypy skinproof  # Type check
bandit -r skinproof  # Security scan
pytest tests/ -v  # Run tests
```

**Android:**
```bash
./gradlew ktlintFormat  # Format code
./gradlew lint  # Lint checks
./gradlew detekt  # Static analysis
./gradlew testDebugUnitTest  # Unit tests
```

## Coverage Requirements

### Current Coverage Targets

- **Backend**: ≥ 70% overall
  - Core analysis pipeline: ≥ 80%
  - API endpoints: ≥ 75%
  - Database layer: ≥ 85%

- **Android**: ≥ 60% overall
  - ViewModels: ≥ 80%
  - Repositories: ≥ 70%
  - UI components: ≥ 50%

### Viewing Coverage Reports

**Backend:**
```bash
cd backend
pytest tests/ --cov=skinproof --cov-report=html
open htmlcov/index.html
```

**Android:**
```bash
./gradlew testDebugUnitTest jacocoTestReport
open app/build/reports/jacoco/testDebugUnitTest/html/index.html
```

**CI/CD:**
- Backend coverage uploaded to [Codecov](https://codecov.io)
- Reports available in GitHub Actions artifacts

## Contributing

### Adding New Tests

#### Backend

1. **Unit Test**: Add to existing `test_*.py` or create new file
   ```python
   # tests/test_new_feature.py
   import unittest

   class TestNewFeature(unittest.TestCase):
       def test_something(self):
           # Test code
           self.assertEqual(result, expected)
   ```

2. **Integration Test**: Add to `tests/integration/`
   ```python
   # tests/integration/test_new_flow.py
   from fastapi.testclient import TestClient

   def test_new_user_flow(self):
       # End-to-end test
       pass
   ```

3. **Load Test**: Add scenario to `locustfile.py`
   ```python
   @task(5)
   def new_endpoint(self):
       self.client.get("/api/new-endpoint")
   ```

#### Android

1. **Unit Test**: Add to `app/src/test/`
   ```kotlin
   @Test
   fun `test new feature`() {
       // Given
       val input = "test"

       // When
       val result = feature.process(input)

       // Then
       assertEquals(expected, result)
   }
   ```

2. **UI Test**: Add to `app/src/androidTest/`
   ```kotlin
   @Test
   fun newFeature_displays() {
       composeTestRule
           .onNodeWithText("Expected Text")
           .assertIsDisplayed()
   }
   ```

### Best Practices

1. **Test Naming**
   - Descriptive names: `test_user_creation_with_valid_data`
   - Use backticks in Kotlin: `` `test description in plain English` ``

2. **Arrange-Act-Assert**
   ```python
   def test_feature(self):
       # Arrange (Given)
       input_data = create_test_data()

       # Act (When)
       result = feature.process(input_data)

       # Assert (Then)
       self.assertEqual(result, expected)
   ```

3. **Test Independence**
   - Each test should run independently
   - Use setUp/tearDown or fixtures
   - Don't rely on test execution order

4. **Mock External Services**
   - Mock API calls, Firebase, etc.
   - Use `unittest.mock` or `mockk`
   - Don't hit production services

5. **Meaningful Assertions**
   - Assert specific values, not just "not null"
   - Test edge cases and error paths
   - Verify error messages

### Debugging Failed Tests

**Backend:**
```bash
# Run with verbose output
pytest tests/test_failing.py -vv

# Run with print statements visible
pytest tests/test_failing.py -s

# Debug with pdb
pytest tests/test_failing.py --pdb
```

**Android:**
```bash
# Run with detailed logs
./gradlew testDebugUnitTest --info

# Run single test
./gradlew testDebugUnitTest --tests "HomeViewModelTest.test_specific_case"

# View test reports
open app/build/reports/tests/testDebugUnitTest/index.html
```

## Continuous Improvement

### Metrics to Track

- Test execution time
- Flaky test rate
- Coverage trends
- Bug escape rate (bugs found in production)

### Regular Reviews

- **Weekly**: Review failed CI runs
- **Monthly**: Coverage reports and quality trends
- **Quarterly**: Load test results and performance benchmarks

### Test Maintenance

- Remove obsolete tests
- Update tests when features change
- Refactor duplicated test code
- Keep test dependencies updated

---

## Quick Reference

### Backend Commands
```bash
# Run all tests
pytest tests/ -v

# Coverage report
pytest tests/ --cov=skinproof --cov-report=html

# Load tests
locust -f tests/load/locustfile.py --host=http://localhost:8000
```

### Android Commands
```bash
# Unit tests
./gradlew testDebugUnitTest

# UI tests
./gradlew connectedDebugAndroidTest

# All checks
./gradlew check
```

### CI Status
- Backend CI: ![Backend CI](https://github.com/piyushxpc7/GlowupAI/workflows/Backend%20CI/badge.svg)
- Android CI: ![Android CI](https://github.com/piyushxpc7/GlowupAI/workflows/Android%20CI/badge.svg)
- Security: ![Security](https://github.com/piyushxpc7/GlowupAI/workflows/Security%20Scanning/badge.svg)

---

**Last Updated**: September 1, 2026  
**Maintained By**: GlowupAI Team

For questions or issues, please open a GitHub issue or contact the team.
