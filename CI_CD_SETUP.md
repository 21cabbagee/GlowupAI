# CI/CD Quick Setup Guide

Welcome to GlowUp AI's automated CI/CD pipeline powered by GitHub Actions!

## Quick Links

- **Workflows Documentation**: [.github/workflows/README.md](.github/workflows/README.md)
- **View CI/CD Runs**: [GitHub Actions](../../actions)
- **Configure Secrets**: [Settings > Secrets](../../settings/secrets/actions)

## What's Automated?

### ✅ On Every Push/PR

#### Android
- Lint checking (code quality)
- Unit tests
- Instrumentation tests (on emulator)
- Debug APK build
- Code quality analysis (Detekt, Ktlint)

#### Backend
- Code formatting (Black)
- Type checking (Mypy)
- Security scanning (Bandit)
- Unit & integration tests
- Coverage reporting
- Docker build validation

### 🚀 Manual Release Builds

Trigger manually to:
- Build signed release APK
- Run full test suite
- Create GitHub release
- Upload APK to releases

## First-Time Setup

### Step 1: Generate Release Keystore

```bash
cd app
keytool -genkey -v \
  -keystore release.keystore \
  -alias glowup-release-key \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000
```

**⚠️ Important:** Save the passwords securely! You'll need them for GitHub secrets.

### Step 2: Encode Keystore for GitHub

```bash
cd ..
.github/encode-keystore.sh app/release.keystore
```

This will create `keystore.base64.txt` with instructions.

### Step 3: Add GitHub Secrets

Go to: `Settings` > `Secrets and variables` > `Actions` > `New repository secret`

Add these 4 secrets:

| Secret Name | Value |
|-------------|-------|
| `RELEASE_KEYSTORE_BASE64` | Contents of `keystore.base64.txt` |
| `KEYSTORE_PASSWORD` | Your keystore password |
| `KEY_ALIAS` | Your key alias (e.g., `glowup-release-key`) |
| `KEY_PASSWORD` | Your key password |

### Step 4: Delete Temporary Files

```bash
rm keystore.base64.txt
```

**Never commit:**
- `release.keystore`
- `keystore.base64.txt`
- `keystore.properties`

These are already in `.gitignore`.

## Running Your First Release

1. Go to `Actions` tab on GitHub
2. Select `Release Build` workflow
3. Click `Run workflow`
4. Fill in:
   - Version name: `1.0.0`
   - Version code: `1`
   - Release notes: Your release description
   - Build type: `release`
5. Click `Run workflow`

The build takes ~10-15 minutes. You'll get:
- Signed release APK
- ProGuard mapping files
- GitHub release with APK attached

## Local Testing (Before Push)

### Android
```bash
# Clean build
./gradlew clean

# Run tests
./gradlew testDebugUnitTest

# Run lint
./gradlew lintDebug

# Build debug APK
./gradlew assembleDebug
```

### Backend
```bash
cd backend

# Format code
black skinproof tests

# Type check
mypy skinproof

# Security scan
bandit -r skinproof

# Run tests
pytest tests/ -v
```

## Status Badges

Add these to your README:

```markdown
![Android CI](https://github.com/YOUR_USERNAME/GlowupAI/workflows/Android%20CI/badge.svg)
![Backend CI](https://github.com/YOUR_USERNAME/GlowupAI/workflows/Backend%20CI/badge.svg)
![Release](https://github.com/YOUR_USERNAME/GlowupAI/workflows/Release%20Build/badge.svg)
```

Replace `YOUR_USERNAME` with your GitHub username.

## Common Issues

### "Keystore secrets not found"
- **Solution**: Make sure all 4 keystore secrets are added to GitHub

### Lint failures
- **Solution**: Run `./gradlew lintDebug` locally and fix issues

### Backend formatting fails
- **Solution**: Run `black skinproof tests` to auto-format

### Tests fail on CI but pass locally
- **Check**: Environment differences (API keys, database)
- **Solution**: Add necessary env vars to workflow files

## Monitoring Builds

- **All Workflows**: Go to `Actions` tab
- **Build Status**: Check the badge on main README
- **Logs**: Click any workflow run to see detailed logs
- **Artifacts**: Download APKs, test reports from workflow runs

## Workflow Files

| File | Purpose | Triggers |
|------|---------|----------|
| `android-ci.yml` | Android build & tests | Push/PR (Android files) |
| `backend-ci.yml` | Backend tests & quality | Push/PR (Backend files) |
| `release.yml` | Release builds | Manual only |

## Best Practices

1. ✅ Always test locally before pushing
2. ✅ Write meaningful commit messages
3. ✅ Keep PRs focused and small
4. ✅ Respond to CI failures promptly
5. ✅ Test release builds on real devices
6. ✅ Keep secrets secure
7. ✅ Review automated PR comments

## Getting Help

1. Check workflow logs in Actions tab
2. Review [.github/workflows/README.md](.github/workflows/README.md)
3. Search existing GitHub Issues
4. Create new issue with logs attached

## Next Steps

After setup:
1. Push code to trigger first CI run
2. Monitor the Actions tab
3. Fix any issues that arise
4. Create your first release build
5. Test APK on devices
6. Upload to Google Play Console

---

**Need more details?** See the full documentation: [.github/workflows/README.md](.github/workflows/README.md)

**Ready to release?** See: [RELEASE_BUILD_GUIDE.md](RELEASE_BUILD_GUIDE.md)
