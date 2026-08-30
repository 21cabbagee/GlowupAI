# GlowUp AI - Keystore Generation Guide

**Simple step-by-step guide to generate your Android release keystore**

⚠️ **CRITICAL:** Your keystore is like your app's birth certificate. If you lose it, you can never update your app on Google Play. Follow this guide carefully!

---

## What You'll Need

- Terminal/Command line access
- 10 minutes of your time
- A password manager to store the passwords

---

## Step 1: Navigate to App Directory

```bash
cd /Users/21cabbage/Skinproof/app
```

---

## Step 2: Generate the Keystore

Copy and paste this command:

```bash
keytool -genkey -v \
  -keystore release.keystore \
  -alias glowup-release \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000
```

**What this does:**
- Creates a file called `release.keystore`
- Valid for 10,000 days (~27 years)
- Uses RSA 2048-bit encryption (Play Store requirement)
- Alias name: `glowup-release`

---

## Step 3: Answer the Prompts

You'll be asked several questions. Here's what to enter:

### Keystore Password
```
Enter keystore password:
Re-enter new password:
```

**Choose a strong password** (e.g., 20+ characters, mix of letters, numbers, symbols)  
**SAVE THIS PASSWORD!** You'll need it forever.

### Key Password
```
Enter key password for <glowup-release>
  (RETURN if same as keystore password):
```

**Recommendation:** Press RETURN to use the same password (simpler to manage)

### Certificate Information
```
What is your first and last name?
  [Unknown]:
```
Enter: Your name or "GlowUp AI"

```
What is the name of your organizational unit?
  [Unknown]:
```
Enter: "Engineering" or "Development" or just press RETURN

```
What is the name of your organization?
  [Unknown]:
```
Enter: "GlowUp AI" or your company name

```
What is the name of your City or Locality?
  [Unknown]:
```
Enter: Your city (e.g., "San Francisco")

```
What is the name of your State or Province?
  [Unknown]:
```
Enter: Your state/province (e.g., "California") or press RETURN

```
What is the two-letter country code for this unit?
  [Unknown]:
```
Enter: Your country code (e.g., "US" for United States, "UK", "CA", "AU", etc.)

### Confirmation
```
Is CN=..., OU=..., O=..., L=..., ST=..., C=... correct?
  [no]:
```
Type: `yes` and press RETURN

---

## Step 4: Verify Keystore Was Created

```bash
ls -la release.keystore
```

You should see a file around 2-3 KB. If you see "No such file", something went wrong - try Step 2 again.

---

## Step 5: View Certificate Details

```bash
keytool -list -v -keystore release.keystore -alias glowup-release
```

Enter your keystore password when prompted.

You'll see output like:
```
Alias name: glowup-release
Creation date: Aug 30, 2026
Entry type: PrivateKeyEntry
Certificate chain length: 1
Certificate[1]:
Owner: CN=..., OU=..., O=..., L=..., ST=..., C=...
Issuer: CN=..., OU=..., O=..., L=..., ST=..., C=...
Serial number: ...
Valid from: ... until: ...
Certificate fingerprints:
         SHA1: AA:BB:CC:DD:... (40 hex digits)
         SHA256: 11:22:33:44:... (64 hex digits)
```

**SAVE THE SHA1 AND SHA256 FINGERPRINTS!** You'll need them for Firebase.

---

## Step 6: Get SHA Fingerprints (Easy Way)

```bash
keytool -list -v -keystore release.keystore -alias glowup-release | grep -E "SHA1:|SHA256:"
```

Copy both lines. Should look like:
```
         SHA1: AA:BB:CC:DD:EE:FF:...
         SHA256: 11:22:33:44:55:66:...
```

---

## Step 7: Create keystore.properties

```bash
cp keystore.properties.example keystore.properties
```

Now edit `keystore.properties`:

```bash
# Use your favorite editor
nano keystore.properties
# or
open -e keystore.properties
```

Update with your actual values:

```properties
storeFile=release.keystore
storePassword=YOUR_ACTUAL_KEYSTORE_PASSWORD
keyAlias=glowup-release
keyPassword=YOUR_ACTUAL_KEY_PASSWORD
```

**Save and close the file.**

---

## Step 8: Add SHA Fingerprints to Firebase

1. Open Firebase Console:
   ```
   https://console.firebase.google.com/project/glowup-ai-38ae7
   ```

2. Click the **gear icon** (⚙️) → **Project settings**

3. Scroll to **Your apps** section

4. Find **GlowUp AI** (Android app)

5. Scroll down to **SHA certificate fingerprints**

6. Click **Add fingerprint**

7. Paste your **SHA-1** fingerprint (the one that starts with AA:BB:CC:...)

8. Click **Add fingerprint** again

9. Paste your **SHA-256** fingerprint (the one that starts with 11:22:33:...)

10. Click **Download google-services.json** (just to be safe)

11. Replace `app/google-services.json` with the newly downloaded file

---

## Step 9: Secure Your Keystore

### Save Passwords Immediately

Add to your password manager:
- Service: "GlowUp AI - Android Keystore"
- Keystore Password: [your password]
- Key Password: [your password]
- Keystore Alias: glowup-release
- Location: /Users/21cabbage/Skinproof/app/release.keystore

### Backup the Keystore File

Make at least 3 copies:
1. Upload to secure cloud storage (Google Drive, Dropbox, etc.)
2. Copy to external USB drive
3. Email to yourself (encrypted attachment)

**DO THIS NOW!** Don't wait!

### Verify Git Ignores It

```bash
cd /Users/21cabbage/Skinproof
git check-ignore app/release.keystore
git check-ignore app/keystore.properties
```

Both commands should output the file paths. If not, **DO NOT COMMIT THEM!**

---

## Step 10: Test the Setup

```bash
cd /Users/21cabbage/Skinproof

# Build a release APK
./gradlew :app:assembleRelease \
  -PRELEASE_API_BASE_URL="https://api.glowup.example.com/api/"
```

If successful, you should see:
```
BUILD SUCCESSFUL
```

Verify signing:
```bash
jarsigner -verify -verbose -certs app/build/outputs/apk/release/app-release.apk
```

Should say **"jar verified"** with your certificate details.

---

## Troubleshooting

### "keytool: command not found"

Install Java JDK:
```bash
# Check if Java is installed
java -version

# If not, install JDK 17 or higher
```

### "Keystore was tampered with, or password was incorrect"

You entered the wrong password. Try again.

### "git check-ignore" doesn't output anything

The file is NOT git-ignored! Check `.gitignore`:
```bash
cat /Users/21cabbage/Skinproof/app/.gitignore
```

Should contain:
```
keystore.properties
*.keystore
```

### Firebase Auth still fails in release build

1. Make sure you added **both** SHA-1 and SHA-256 to Firebase
2. Download the updated `google-services.json` from Firebase
3. Rebuild the app completely:
   ```bash
   ./gradlew clean
   ./gradlew :app:assembleRelease -PRELEASE_API_BASE_URL="..."
   ```

---

## Quick Reference

### View keystore details:
```bash
keytool -list -v -keystore app/release.keystore -alias glowup-release
```

### Get SHA fingerprints:
```bash
keytool -list -v -keystore app/release.keystore -alias glowup-release | grep -E "SHA1:|SHA256:"
```

### Verify build is signed:
```bash
jarsigner -verify -verbose -certs app/build/outputs/apk/release/app-release.apk
```

---

## What's Next?

After completing this guide:

1. **Test a release build:**
   ```bash
   ./scripts/release-build.sh production
   ```

2. **Read the full release guide:**
   - `RELEASE_BUILD_GUIDE.md` - Complete release process
   - `RELEASE_QUICK_REFERENCE.md` - Copy-paste commands

3. **Upload to Play Store:**
   - Upload `app/build/outputs/bundle/release/app-release.aab`

---

## Security Checklist

- [ ] Keystore password saved in password manager
- [ ] Key password saved in password manager
- [ ] Keystore file backed up (3+ locations)
- [ ] SHA-1 fingerprint added to Firebase
- [ ] SHA-256 fingerprint added to Firebase
- [ ] `google-services.json` downloaded and updated
- [ ] `release.keystore` is git-ignored
- [ ] `keystore.properties` is git-ignored
- [ ] Test build completed successfully
- [ ] Verified build is signed (not debug)

---

**Done! Your keystore is ready for production releases.** 🎉

For the next steps, see `RELEASE_BUILD_GUIDE.md` or run:
```bash
./scripts/release-build.sh production
```
