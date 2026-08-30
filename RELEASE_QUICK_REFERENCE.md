# GlowUp AI - Release Build Quick Reference

**Copy-paste ready commands for the founder! 🚀**

---

## First-Time Setup (One-Time Only)

### 1. Generate Release Keystore

```bash
cd /Users/21cabbage/Skinproof/app

# Generate keystore (you'll be prompted for passwords and details)
keytool -genkey -v \
  -keystore release.keystore \
  -alias glowup-release \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000
```

**Important:** Choose strong passwords and **save them securely**! You'll need them forever.

### 2. Get SHA Fingerprints

```bash
cd /Users/21cabbage/Skinproof/app
keytool -list -v -keystore release.keystore -alias glowup-release | grep -E "SHA1:|SHA256:"
```

Copy both SHA-1 and SHA-256 fingerprints.

### 3. Add Fingerprints to Firebase

1. Go to: https://console.firebase.google.com/project/glowup-ai-38ae7
2. **Project Settings** → **Your apps** → **GlowUp AI**
3. Scroll to **SHA certificate fingerprints**
4. Click **Add fingerprint** and paste both SHA-1 and SHA-256

### 4. Create keystore.properties

```bash
cd /Users/21cabbage/Skinproof/app
cp keystore.properties.example keystore.properties
```

Then edit `keystore.properties` with your actual passwords:

```properties
storeFile=release.keystore
storePassword=YOUR_KEYSTORE_PASSWORD
keyAlias=glowup-release
keyPassword=YOUR_KEY_PASSWORD
```

**Done!** You only need to do this once.

---

## Building Releases (Every Time)

### Quick Build (Automated Script)

```bash
cd /Users/21cabbage/Skinproof

# For staging
./scripts/release-build.sh staging

# For production
./scripts/release-build.sh production
```

The script will:
- Check all prerequisites
- Prompt for API URL
- Build signed AAB
- Verify signing
- Show output location

### Manual Build (If You Prefer)

```bash
cd /Users/21cabbage/Skinproof

# Production App Bundle (for Play Store)
./gradlew :app:bundleRelease \
  -PRELEASE_API_BASE_URL="https://api.glowup.your-domain.com/api/"

# Output: app/build/outputs/bundle/release/app-release.aab
```

### Verify Signing

```bash
cd /Users/21cabbage/Skinproof
jarsigner -verify -verbose -certs app/build/outputs/bundle/release/app-release.aab
```

Should say **"jar verified"** and show your certificate (not "Android Debug").

---

## Testing Release Build

### Install APK on Physical Device

```bash
cd /Users/21cabbage/Skinproof

# Build APK first (if not already built)
./gradlew :app:assembleRelease \
  -PRELEASE_API_BASE_URL="https://api.glowup.your-domain.com/api/"

# Install on connected device
adb install app/build/outputs/apk/release/app-release.apk
```

### Critical Tests

- [ ] App launches without crashes
- [ ] Email sign-in works
- [ ] Google sign-in works
- [ ] Network requests reach backend
- [ ] Camera capture works
- [ ] Photo upload works

**Test on a real device, not emulator!**

---

## Version Bumping

### Before Next Release

```bash
cd /Users/21cabbage/Skinproof

# Example: Bump to version 1.1 (code 2)
./scripts/version-bump.sh 1.1 2

# Example: Bump to version 2.0 (code 10)
./scripts/version-bump.sh 2.0 10
```

### Commit Version Bump

```bash
git add app/build.gradle.kts
git commit -m "Bump version to 1.1 (code: 2)"
```

---

## Upload to Google Play

### 1. Build Production AAB

```bash
cd /Users/21cabbage/Skinproof
./scripts/release-build.sh production
```

### 2. Upload to Play Console

1. Go to: https://play.google.com/console
2. Select **GlowUp AI**
3. **Release** → **Production** (or **Internal testing**)
4. **Create new release**
5. Upload: `app/build/outputs/bundle/release/app-release.aab`
6. Add release notes
7. Review and roll out

### 3. Tag Release in Git

```bash
git tag -a v1.0 -m "Release version 1.0"
git push origin v1.0
```

---

## Common Commands

### Clean Build

```bash
./gradlew clean
```

### Debug Build (Development)

```bash
./gradlew :app:assembleDebug
adb install app/build/outputs/apk/debug/app-debug.apk
```

### Run Tests

```bash
./gradlew :app:testDebugUnitTest
```

### Check Output Size

```bash
ls -lh app/build/outputs/bundle/release/app-release.aab
ls -lh app/build/outputs/apk/release/app-release.apk
```

---

## Troubleshooting

### "keystore.properties not found"

```bash
cd /Users/21cabbage/Skinproof/app
cp keystore.properties.example keystore.properties
# Then edit with your passwords
```

### "google-services.json not found"

Download from Firebase Console:
1. https://console.firebase.google.com/project/glowup-ai-38ae7
2. **Project Settings** → **Your apps** → **Download google-services.json**
3. Save to: `app/google-services.json`

### Google Sign-In Fails in Release

Add your keystore SHA fingerprints to Firebase (see "First-Time Setup" above).

### App Can't Reach Backend

Make sure you set the correct API URL when building:
```bash
export RELEASE_API_BASE_URL="https://api.glowup.your-domain.com/api/"
./scripts/release-build.sh production
```

### Build Fails with "JDK not found"

Ensure JDK 17+ is installed:
```bash
java -version
```

---

## File Locations

| File | Location | Purpose |
|------|----------|---------|
| Release AAB | `app/build/outputs/bundle/release/app-release.aab` | Upload to Play Store |
| Release APK | `app/build/outputs/apk/release/app-release.apk` | Testing only |
| Keystore | `app/release.keystore` | Release signing (keep safe!) |
| Keystore config | `app/keystore.properties` | Keystore passwords (git-ignored) |
| Firebase config | `app/google-services.json` | Firebase setup |
| ProGuard rules | `app/proguard-rules.pro` | Code shrinking rules |

---

## Security Checklist

- [ ] `release.keystore` backed up securely (multiple locations)
- [ ] Keystore passwords stored in password manager
- [ ] `keystore.properties` is git-ignored
- [ ] `release.keystore` is git-ignored
- [ ] Never committed secrets to git
- [ ] SHA fingerprints added to Firebase

---

## Quick Checklist for Every Release

### Pre-Build
- [ ] Version bumped (`./scripts/version-bump.sh`)
- [ ] Release notes written
- [ ] All tests pass
- [ ] Backend deployed and tested

### Build
- [ ] Run `./scripts/release-build.sh production`
- [ ] Verify signing (not debug signed)
- [ ] Check API URL (not placeholder)

### Test
- [ ] Install on physical device
- [ ] Test critical flows (auth, camera, upload)
- [ ] No crashes or errors

### Upload
- [ ] Upload AAB to Play Console
- [ ] Add release notes
- [ ] Roll out (start with internal testing)

### Post-Release
- [ ] Monitor Crashlytics
- [ ] Check user feedback
- [ ] Tag release in git

---

## Environment Variables (Optional)

Set these to avoid prompts:

```bash
# Add to ~/.zshrc or ~/.bashrc
export RELEASE_API_BASE_URL="https://api.glowup.your-domain.com/api/"
export STAGING_API_BASE_URL="https://staging.glowup.your-domain.com/api/"
```

---

## Need Help?

- **Full guide:** See `RELEASE_BUILD_GUIDE.md`
- **Production checklist:** See `PRODUCTION_READINESS.md`
- **App setup:** See `app/README.md`
- **Firebase Console:** https://console.firebase.google.com/project/glowup-ai-38ae7
- **Play Console:** https://play.google.com/console

---

**Ready to ship? Let's go! 🚀**

```bash
./scripts/release-build.sh production
```
