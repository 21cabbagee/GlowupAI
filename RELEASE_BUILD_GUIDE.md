# GlowUp AI - Release Build Guide

**Last updated:** 2026-08-30  
**Firebase Project:** glowup-ai-38ae7  
**Package:** com.glowup.ai

This guide walks you through creating a production-ready release build of GlowUp AI for Google Play Store distribution.

---

## Prerequisites Checklist

Before building a release, ensure you have:

- [ ] Android SDK with API 37 installed
- [ ] JDK 25 (or JDK 17+)
- [ ] Firebase project configured (glowup-ai-38ae7)
- [ ] `app/google-services.json` in place
- [ ] Production backend URL ready

---

## Step 1: Generate Release Keystore

**⚠️ CRITICAL:** Keep your keystore file and passwords extremely safe. If you lose them, you cannot update your app on Google Play.

### Generate the keystore:

```bash
cd /Users/21cabbage/Skinproof/app

# Generate release keystore (will prompt for passwords and certificate info)
keytool -genkey -v \
  -keystore release.keystore \
  -alias glowup-release \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000
```

### Certificate Information Prompts:

When prompted, provide:
- **Keystore password:** Create a strong password (remember this!)
- **Key password:** Can be same as keystore password
- **First and Last Name:** Your name or company name
- **Organizational Unit:** e.g., "Engineering" or "Development"
- **Organization:** e.g., "GlowUp AI" or your company
- **City/Locality:** Your city
- **State/Province:** Your state
- **Country Code:** Two-letter code (e.g., US, UK, CA)

### Verify keystore creation:

```bash
keytool -list -v -keystore release.keystore -alias glowup-release
```

This will show your certificate details including **SHA-1** and **SHA-256** fingerprints (needed for Firebase).

---

## Step 2: Configure Keystore Properties

Create `app/keystore.properties` from the example:

```bash
cd /Users/21cabbage/Skinproof/app
cp keystore.properties.example keystore.properties
```

Edit `app/keystore.properties` with your actual values:

```properties
# Path to keystore (relative to app/ directory)
storeFile=release.keystore

# Your keystore password
storePassword=YOUR_KEYSTORE_PASSWORD

# Key alias (use glowup-release if you followed Step 1)
keyAlias=glowup-release

# Your key password
keyPassword=YOUR_KEY_PASSWORD
```

**⚠️ IMPORTANT:** This file is git-ignored and contains secrets. Never commit it!

---

## Step 3: Add Release Keystore to Firebase

Get your keystore fingerprints:

```bash
cd /Users/21cabbage/Skinproof/app
keytool -list -v -keystore release.keystore -alias glowup-release | grep -E "SHA1:|SHA256:"
```

Then in Firebase Console:

1. Go to https://console.firebase.google.com/project/glowup-ai-38ae7
2. Navigate to **Project Settings** → **Your apps** → **GlowUp AI (Android)**
3. Scroll to **SHA certificate fingerprints**
4. Click **Add fingerprint**
5. Add both **SHA-1** and **SHA-256** fingerprints

**Why this matters:** Firebase Auth (Google Sign-In) requires these fingerprints to work in release builds.

---

## Step 4: Update google-services.json (if needed)

After adding fingerprints to Firebase, download the updated `google-services.json`:

1. In Firebase Console → **Project Settings**
2. Scroll to **Your apps** → **GlowUp AI**
3. Click **Download google-services.json**
4. Replace `app/google-services.json` with the new file

```bash
# Verify the file exists
ls -la /Users/21cabbage/Skinproof/app/google-services.json
```

---

## Step 5: Verify ProGuard Rules

The app already has comprehensive ProGuard rules in `app/proguard-rules.pro` covering:

- ✅ kotlinx.serialization
- ✅ Retrofit / OkHttp
- ✅ Room database
- ✅ Hilt / Dagger
- ✅ ML Kit face detection
- ✅ Firebase
- ✅ App DTOs and domain models

No changes needed unless you've added new libraries.

---

## Step 6: Set Production API URL

Set your production backend URL when building:

```bash
# Option 1: Environment variable (recommended)
export RELEASE_API_BASE_URL="https://api.glowup.your-domain.com/api/"

# Option 2: Pass directly to Gradle
./gradlew assembleRelease -PRELEASE_API_BASE_URL="https://api.glowup.your-domain.com/api/"
```

**⚠️ IMPORTANT:** Without this, the build uses a placeholder invalid URL and won't work!

---

## Step 7: Build Release APK/AAB

### Option A: Build Release APK (for testing)

```bash
cd /Users/21cabbage/Skinproof

# Build release APK
./gradlew :app:assembleRelease \
  -PRELEASE_API_BASE_URL="https://api.glowup.your-domain.com/api/"
```

Output: `app/build/outputs/apk/release/app-release.apk`

### Option B: Build App Bundle (for Play Store)

```bash
cd /Users/21cabbage/Skinproof

# Build release AAB (recommended for Play Store)
./gradlew :app:bundleRelease \
  -PRELEASE_API_BASE_URL="https://api.glowup.your-domain.com/api/"
```

Output: `app/build/outputs/bundle/release/app-release.aab`

**Google Play requires AAB (App Bundle)** for new apps since August 2021.

---

## Step 8: Verify Release Build

### Check the signing:

```bash
cd /Users/21cabbage/Skinproof

# For APK:
jarsigner -verify -verbose -certs app/build/outputs/apk/release/app-release.apk

# For AAB:
jarsigner -verify -verbose -certs app/build/outputs/bundle/release/app-release.aab
```

Should show: **"jar verified"** with your certificate details (not "Android Debug").

### Check the API URL:

```bash
# Extract and check BuildConfig
unzip -p app/build/outputs/apk/release/app-release.apk classes.dex > /tmp/classes.dex
strings /tmp/classes.dex | grep -i "api.glowup"
```

Should show your production URL, NOT `example.invalid`.

### Check the file size:

```bash
ls -lh app/build/outputs/apk/release/app-release.apk
ls -lh app/build/outputs/bundle/release/app-release.aab
```

APK should be ~15-30 MB, AAB should be smaller.

---

## Step 9: Test Release Build

### Install on a physical device:

```bash
# Enable USB debugging on your device, then:
adb install app/build/outputs/apk/release/app-release.apk
```

### Test checklist:

- [ ] App launches without crashes
- [ ] Firebase Auth (Email/Password) works
- [ ] Firebase Auth (Google Sign-In) works
- [ ] Network requests reach production backend
- [ ] Camera capture works
- [ ] Photo upload works
- [ ] All navigation flows work
- [ ] No debug logs visible

**⚠️ Test on a physical device, not emulator**, to catch real-world issues.

---

## Step 10: Prepare for Play Store Upload

### Generate Release Notes

Create release notes for this version. Example:

```
Version 1.0 (Build 1)
- Initial release
- AI-powered skin analysis
- Personalized skincare recommendations
- Product tracking and timeline
- Smart Q&A with dermatology citations
```

### Build Artifacts Checklist

For Play Store upload, you need:

- [ ] `app-release.aab` (App Bundle)
- [ ] Release notes for this version
- [ ] Screenshots (phone, tablet, 7-inch tablet)
- [ ] Feature graphic (1024x500)
- [ ] App icon (512x512)
- [ ] Privacy Policy URL
- [ ] Data Safety form completed

### Upload to Play Console

1. Go to https://play.google.com/console
2. Select **GlowUp AI** app
3. Navigate to **Release** → **Production** (or **Internal testing** first)
4. Click **Create new release**
5. Upload `app-release.aab`
6. Add release notes
7. Review and roll out

---

## Version Bumping Guide

### Before next release, bump version:

Edit `app/build.gradle.kts`:

```kotlin
defaultConfig {
    applicationId = "com.glowup.ai"
    minSdk = 24
    targetSdk = 37
    versionCode = 2        // Increment by 1
    versionName = "1.1"    // Update semantic version
    ...
}
```

**Version Code:** Must increment for every Play Store release (1, 2, 3, ...)  
**Version Name:** Semantic version shown to users (1.0, 1.1, 2.0, ...)

Use the provided script:

```bash
./scripts/version-bump.sh 1.1 2
```

---

## Quick Reference: Build Commands

### Clean build:

```bash
./gradlew clean
```

### Debug build (for development):

```bash
./gradlew :app:assembleDebug
```

### Staging build (with staging backend):

```bash
./gradlew :app:assembleStaging \
  -PSTAGING_API_BASE_URL="https://staging.glowup.your-domain.com/api/"
```

### Release build (production):

```bash
./gradlew :app:assembleRelease \
  -PRELEASE_API_BASE_URL="https://api.glowup.your-domain.com/api/"
```

### Release App Bundle:

```bash
./gradlew :app:bundleRelease \
  -PRELEASE_API_BASE_URL="https://api.glowup.your-domain.com/api/"
```

---

## Automated Release Script

Use the provided script for one-command releases:

```bash
./scripts/release-build.sh production
```

This script:
1. Validates keystore.properties exists
2. Checks google-services.json exists
3. Prompts for production API URL
4. Builds signed release AAB
5. Verifies signing
6. Shows output location and next steps

See `scripts/release-build.sh` for details.

---

## Security Best Practices

### Keystore Security:

- ✅ Store keystore file in a password manager (encrypted)
- ✅ Back up keystore to multiple secure locations
- ✅ Never commit keystore.properties or release.keystore to git
- ✅ Use strong, unique passwords (20+ characters)
- ✅ Consider using Google Play App Signing (managed signing)

### Verify .gitignore:

```bash
# These should be git-ignored:
git check-ignore app/keystore.properties       # Should output the path
git check-ignore app/release.keystore          # Should output the path
git check-ignore app/google-services.json      # Should output the path
```

If any don't show output, they're not ignored - **do not commit them!**

---

## Troubleshooting

### "keystore.properties not found"

**Solution:** Create `app/keystore.properties` following Step 2.

### "google-services.json not found"

**Solution:** Download from Firebase Console and place at `app/google-services.json`.

### "jar verified" fails or shows "Android Debug"

**Problem:** Using debug signing instead of release signing.  
**Solution:** Ensure `app/keystore.properties` exists with correct values.

### Google Sign-In fails in release build

**Problem:** SHA fingerprints not added to Firebase.  
**Solution:** Follow Step 3 to add keystore fingerprints to Firebase.

### App can't reach backend (network error)

**Problem:** Using placeholder API URL (`example.invalid`).  
**Solution:** Set `RELEASE_API_BASE_URL` when building (Step 6).

### ProGuard crashes or missing classes

**Problem:** ProGuard rules incomplete.  
**Solution:** Check `app/proguard-rules.pro` and add rules for new libraries.

### Build fails with "JDK not found"

**Solution:** Ensure JDK 17+ or JDK 25 is installed and JAVA_HOME is set.

---

## Additional Resources

- **Production Readiness:** See `PRODUCTION_READINESS.md` for full pre-launch checklist
- **App Documentation:** See `app/README.md` for build types and configuration
- **Backend Deployment:** See `DEPLOY.md` for backend setup
- **Firebase Console:** https://console.firebase.google.com/project/glowup-ai-38ae7
- **Play Console:** https://play.google.com/console

---

## Release Checklist

Use this checklist for every release:

### Pre-Build:
- [ ] Version code incremented in build.gradle.kts
- [ ] Version name updated in build.gradle.kts
- [ ] Release notes written
- [ ] All tests passing (`./gradlew test`)
- [ ] ProGuard rules reviewed
- [ ] API URLs configured (production backend deployed)

### Build:
- [ ] Keystore and keystore.properties in place
- [ ] google-services.json updated with release fingerprints
- [ ] Clean build completed
- [ ] Release AAB generated
- [ ] Signing verified (not debug signed)
- [ ] API URL verified (not placeholder)

### Testing:
- [ ] Installed on physical device
- [ ] Firebase Auth works (Email + Google)
- [ ] Network requests reach production backend
- [ ] Camera and photo upload work
- [ ] All critical flows tested
- [ ] No crashes or errors in logs

### Upload:
- [ ] Screenshots updated
- [ ] Store listing reviewed
- [ ] Privacy Policy updated
- [ ] Data Safety form completed
- [ ] AAB uploaded to Play Console
- [ ] Release notes added
- [ ] Internal testing complete (recommended)
- [ ] Production release rolled out

### Post-Release:
- [ ] Monitor Crashlytics for crashes
- [ ] Monitor backend logs for errors
- [ ] Check user reviews and ratings
- [ ] Track analytics for adoption
- [ ] Plan next version features

---

**Questions?** See `PRODUCTION_READINESS.md` or consult Android and Firebase documentation.

**Ready to build?** Run `./scripts/release-build.sh production`
