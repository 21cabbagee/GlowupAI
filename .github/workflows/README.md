# GlowUp AI CI/CD Workflows

This directory contains GitHub Actions workflows for automated building, testing, and deployment of GlowUp AI.

## Workflows Overview

### 1. Android CI (`android-ci.yml`)
**Triggers:** Push/PR to `main` or `develop` branches (Android files only)

**Jobs:**
- **Build & Test**: Runs lint, unit tests, and builds debug APK
- **Instrumentation Tests**: Runs Android instrumented tests on emulator
- **Code Quality**: Runs Detekt and Ktlint for code quality checks

**Features:**
- Fast feedback with parallel jobs
- Gradle dependency caching
- Automatic PR comments with results
- Artifact uploads (APK, test reports, lint results)
- Fail-fast on critical errors

**Status Badge:**
```markdown
![Android CI](https://github.com/YOUR_USERNAME/GlowupAI/workflows/Android%20CI/badge.svg)
```

---

### 2. Backend CI (`backend-ci.yml`)
**Triggers:** Push/PR to `main` or `develop` branches (backend files only)

**Jobs:**
- **Test & Quality**: Multi-Python version testing (3.11, 3.12)
  - Black formatting checks
  - Mypy type checking
  - Bandit security scanning
  - Pytest with coverage
- **Docker Build Test**: Validates Docker image builds
- **Integration Tests**: Full integration tests with PostgreSQL

**Features:**
- Matrix testing across Python versions
- Code coverage reporting (Codecov)
- Security vulnerability scanning
- PostgreSQL integration tests
- Docker build validation

**Status Badge:**
```markdown
![Backend CI](https://github.com/YOUR_USERNAME/GlowupAI/workflows/Backend%20CI/badge.svg)
```

---

### 3. Release Build (`release.yml`)
**Trigger:** Manual workflow dispatch

**Input Parameters:**
- `version_name`: Version string (e.g., "1.0.0")
- `version_code`: Integer build number
- `release_notes`: Release description (optional)
- `build_type`: Choose between "release" or "staging"

**Jobs:**
- **Pre-flight Checks**: Validates inputs and verifies secrets
- **Build**: Compiles signed release APK with full testing
- **Create Release**: Creates GitHub release with APK (release builds only)
- **Notify**: Success notifications and next steps

**Features:**
- Signed APK generation
- Full test suite execution
- APK verification and checksums
- Automated GitHub releases
- ProGuard mapping file preservation
- Build size reporting

**Status Badge:**
```markdown
![Release Build](https://github.com/YOUR_USERNAME/GlowupAI/workflows/Release%20Build/badge.svg)
```

---

## Required Secrets

Configure these secrets in your GitHub repository settings (`Settings` > `Secrets and variables` > `Actions`):

### Android Release Signing
| Secret Name | Description | How to Generate |
|-------------|-------------|-----------------|
| `RELEASE_KEYSTORE_BASE64` | Base64-encoded release keystore | `base64 -i release.keystore \| pbcopy` |
| `KEYSTORE_PASSWORD` | Keystore password | Set during keystore generation |
| `KEY_ALIAS` | Key alias name | Set during keystore generation |
| `KEY_PASSWORD` | Key password | Set during keystore generation |

### Optional Secrets
| Secret Name | Description | Required For |
|-------------|-------------|--------------|
| `SLACK_WEBHOOK_URL` | Slack webhook for notifications | Slack notifications |
| `CODECOV_TOKEN` | Codecov upload token | Private repos coverage |

---

## Setting Up Release Keystore

### 1. Generate a Release Keystore
```bash
keytool -genkey -v \
  -keystore release.keystore \
  -alias glowup-release-key \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000
```

**Important:** Save the passwords securely! You'll need them for GitHub secrets.

### 2. Convert Keystore to Base64
```bash
# macOS/Linux
base64 -i release.keystore | pbcopy

# Or output to file
base64 -i release.keystore > keystore.base64.txt
```

### 3. Add Secrets to GitHub
1. Go to your GitHub repository
2. Navigate to `Settings` > `Secrets and variables` > `Actions`
3. Click `New repository secret`
4. Add each secret:
   - `RELEASE_KEYSTORE_BASE64`: Paste the base64 output
   - `KEYSTORE_PASSWORD`: Enter your keystore password
   - `KEY_ALIAS`: Enter your key alias (e.g., `glowup-release-key`)
   - `KEY_PASSWORD`: Enter your key password

---

## Usage Examples

### Running a Release Build

1. Go to `Actions` tab in your GitHub repository
2. Select `Release Build` workflow
3. Click `Run workflow`
4. Fill in the parameters:
   ```
   Version name: 1.0.0
   Version code: 1
   Release notes: Initial public release with core features
   Build type: release
   ```
5. Click `Run workflow` button

The workflow will:
- Validate inputs and secrets
- Run full test suite
- Build signed release APK
- Create GitHub release
- Upload APK as release asset

### Manual Testing Locally

#### Test Android Build
```bash
./gradlew clean assembleDebug testDebugUnitTest lintDebug
```

#### Test Backend
```bash
cd backend
pip install -e ".[dev]"
pytest tests/ --cov=skinproof
black --check skinproof tests
mypy skinproof
bandit -r skinproof
```

---

## Troubleshooting

### Android CI Fails

**Problem:** Lint checks fail
```
Solution: Run ./gradlew lintDebug locally and fix issues
```

**Problem:** Unit tests fail
```
Solution: Run ./gradlew testDebugUnitTest locally to debug
```

**Problem:** Gradle build times out
```
Solution: Check if dependencies are cached properly, increase timeout if needed
```

### Backend CI Fails

**Problem:** Black formatting fails
```
Solution: Run 'black skinproof tests' locally to auto-format
```

**Problem:** Type checking errors
```
Solution: Add type hints or use '# type: ignore' comments sparingly
```

**Problem:** Pytest failures
```
Solution: Run 'pytest tests/ -v' locally to debug failing tests
```

### Release Build Fails

**Problem:** "Keystore secret not set" error
```
Solution: Verify all four keystore secrets are configured in GitHub
```

**Problem:** "APK signing failed"
```
Solution: Verify keystore password and alias are correct
```

**Problem:** "Version format invalid"
```
Solution: Use format X.Y.Z (e.g., 1.0.0) or X.Y.Z-suffix (e.g., 1.0.0-beta)
```

---

## Performance Optimizations

### Gradle Build Cache
- Caches are saved per branch
- Main branch caches are read-only for PRs
- Cache keys based on Gradle files hash

### Python Dependency Cache
- pip cache enabled via setup-python action
- Cached based on pyproject.toml hash

### Parallel Execution
- Android CI runs lint, test, and build in parallel
- Backend tests run across multiple Python versions simultaneously

---

## Adding Status Badges to README

Add these badges to your main README.md:

```markdown
# GlowUp AI

![Android CI](https://github.com/YOUR_USERNAME/GlowupAI/workflows/Android%20CI/badge.svg)
![Backend CI](https://github.com/YOUR_USERNAME/GlowupAI/workflows/Backend%20CI/badge.svg)
![Release](https://github.com/YOUR_USERNAME/GlowupAI/workflows/Release%20Build/badge.svg)
[![codecov](https://codecov.io/gh/YOUR_USERNAME/GlowupAI/branch/main/graph/badge.svg)](https://codecov.io/gh/YOUR_USERNAME/GlowupAI)
```

Replace `YOUR_USERNAME` with your actual GitHub username or organization name.

---

## Best Practices

1. **Always test locally first** before pushing
2. **Keep secrets secure** - never commit keystore files
3. **Use meaningful commit messages** for better CI/CD tracking
4. **Review PR comments** from automated CI checks
5. **Monitor workflow run times** and optimize if needed
6. **Update dependencies regularly** to get security fixes
7. **Test release builds** on physical devices before publishing
8. **Keep mapping files** for ProGuard crash analysis

---

## Maintenance

### Updating Workflow Dependencies

Check for updates quarterly:
- actions/checkout
- actions/setup-java
- actions/setup-python
- gradle/actions/setup-gradle
- actions/upload-artifact
- actions/download-artifact

### Updating Build Tools

When updating Android Gradle Plugin or Gradle version:
1. Update locally first
2. Test builds pass
3. Update CI workflows if needed
4. Commit and push changes

---

## Support

For issues or questions:
1. Check workflow logs in Actions tab
2. Review this README for common solutions
3. Search GitHub Issues for similar problems
4. Create a new issue with workflow logs attached

---

## License

These workflows are part of the GlowUp AI project.
