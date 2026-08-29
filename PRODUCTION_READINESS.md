# SkinProof production readiness

This is the launch checklist for moving the current repository from a verified
development build to a real production service and Android release.

Last reviewed: 2026-08-29

## Current status

The source tree is buildable and the core application flows are implemented.
The following verification is complete:

- Backend test suite: 58 tests passing.
- Android debug unit-test task: passing.
- Android release build with R8/resource shrinking: passing.
- Release APK can be generated at `app/build/outputs/apk/release/app-release.apk`.
- No emulator/device run or live cloud deployment has been performed in this
  environment.

The app is not ready to accept real users until the external prerequisites and
the production gates below are complete. A build succeeding is not the same
as a production deployment being safe.

## P0 — blockers before any real users

### 1. Create and configure Firebase

In the Firebase project used by the app:

1. Create or select the production Firebase project.
2. Enable Email/Password and Google sign-in providers.
3. Register the Android application with the production package name.
4. Add the release keystore SHA-1 and SHA-256 fingerprints.
5. Download the production `google-services.json` into `app/`.
6. Test sign-in on a release-like build and confirm that Firebase returns an
   ID token.

`app/google-services.json` is intentionally not committed. Without it,
Firebase Auth, Analytics, and Crashlytics do not initialize at runtime even
though the Gradle build can still compile.

### 2. Deploy the backend with durable infrastructure

Use Railway, Render, or another HTTPS container host. `DEPLOY.md` contains the
existing Railway-oriented runbook; the requirements are provider-independent.

Production must have:

- Managed PostgreSQL, not the SQLite fallback.
- Automated backups and a tested restore procedure.
- A public HTTPS domain.
- A health check against `/api/health`.
- Restart policy and service monitoring.
- A secret manager/platform secret store; never commit filled `.env` files.

The backend container runs migrations during startup. Verify this against a
real empty staging Postgres database before pointing production traffic at it.

### 3. Configure production backend variables

Set these in the hosting provider's secret/configuration UI. Values below are
requirements, not values to commit:

| Variable | Production requirement |
| --- | --- |
| `DATABASE_URL` | Managed Postgres connection string. Do not allow the SQLite fallback. |
| `SKINPROOF_ENV` | `production` |
| `SKINPROOF_DISABLE_LEGACY_KEY_FILE` | `1` |
| `SKINPROOF_FIREBASE_PROJECT_ID` | Production Firebase project id. |
| `SKINPROOF_AUTH_REQUIRED` | `1` before real user traffic. See the client-token gate below. |
| `SKINPROOF_ADMIN_TOKEN` | Long random secret, stored only in the secret manager. |
| `GEMINI_API_KEY` | Required if shelf-scan OCR and cited Q&A are part of launch. |
| `SKINPROOF_GEMINI_ENABLED` | `1` when Gemini-backed features are enabled and tested. |
| `SKINPROOF_MODEL_VERSION` | Pinned version approved for this release. |
| `SKINPROOF_POLICY_VERSION` | Version matching the consent/privacy copy shipped in the app. |
| `SKINPROOF_RAW_RETENTION_DAYS` | Approved retention period, with enforcement completed below. |
| `SKINPROOF_DB_POOL_MAX_SIZE` | Sized to the database provider's connection limit. |

Generate a photo encryption key outside the repository, for example:

```bash
python3 -c 'import base64,secrets; print(base64.b64encode(secrets.token_bytes(32)).decode())'
```

### 4. Make photo storage durable before launch

The backend must not use its default in-memory photo store in production.
Choose one of these before launch:

- Preferred: implement an encrypted object-storage `PhotoStore` using S3,
  GCS, R2, or an equivalent provider with lifecycle rules and server-side/KMS
  encryption.
- Temporary closed-alpha option: attach a durable single-instance volume and
  set both `SKINPROOF_PHOTO_DIR` and `SKINPROOF_PHOTO_KEY`.

The current encrypted local-disk store is a stopgap. A normal container disk
is lost on restart/redeploy and does not support safe horizontal scaling.
Confirm that account deletion removes the remote photo objects as well as the
database rows.

### 5. Enforce authenticated ownership

The Android network layer must be confirmed to attach Firebase bearer tokens
to every user-scoped request. Then:

1. Deploy staging with `SKINPROOF_AUTH_REQUIRED=1`.
2. Test a valid user's requests.
3. Test a second user's token against the first user's id and confirm `403`.
4. Test missing/expired tokens and confirm `401`.
5. Only then enable `SKINPROOF_AUTH_REQUIRED=1` in production.

Never ship production with the default `SKINPROOF_AUTH_REQUIRED=0`; that mode
allows a caller to supply another user's path id without an ownership check.

### 6. Replace wildcard CORS

`backend/skinproof/complete_api.py` currently configures
`allow_origins=["*"]`. Before production, replace this with an explicit
allow-list containing only the approved web origins (if the web client is
served) and the required application origins. Do not combine wildcard origins
with credentialed requests.

## P1 — release configuration

### Android production build

1. Generate an upload/release keystore and store it outside the repository.
2. Create local/CI-only `app/keystore.properties` from
   `app/keystore.properties.example`.
3. Verify that `keystore.properties`, keystore files, Firebase config, and
   local properties remain ignored by Git.
4. Set the release API URL to the real HTTPS backend:

   ```bash
   ./gradlew :app:assembleStaging \
     -PSTAGING_API_BASE_URL=https://staging.example.com/api/

   ./gradlew :app:assembleRelease \
     -PRELEASE_API_BASE_URL=https://api.example.com/api/
   ```

5. Confirm the release build does not use the invalid placeholder URL.
6. Confirm the output is signed by the real release key, not the debug key.
7. Record and review the final version code/name and APK/AAB size.

The current repository can fall back to debug signing so development builds
stay reproducible. That fallback is not acceptable for Play distribution.

### Production-only Android checks

- Release network security must remain HTTPS-only.
- Firebase Auth must work with the release package/signature.
- Notification permission and reminder behavior must work on supported Android
  versions.
- Offline capture outbox upload must not upload after sign-out or account
  deletion.
- Consent-declined users must never reach the camera or upload a photo.
- Export and account deletion must work on a device with no share handler and
  with intermittent network connectivity.

## P1 — data protection and operations

### Privacy and retention

- Publish a privacy policy that explicitly covers facial images, derived
  appearance measurements, analytics, retention, deletion, vendors, and user
  export rights.
- Complete the Google Play Data Safety declaration for camera and facial/
  biometric-related data.
- Decide whether raw images are retained, for how long, and why.
- Implement and schedule the raw-photo cleanup job; the current
  `SKINPROOF_RAW_RETENTION_DAYS` setting is configuration input, not proof that
  a cleanup worker is running.
- Test account deletion end to end: database cascade, object storage, local
  cache, pending outbox files, analytics identifiers, and Firebase sign-out.
- Confirm export files contain no raw photo bytes and are not logged.

### Security and reliability

- Rotate all staging/prototype secrets before production.
- Use separate Firebase, database, Gemini, and admin credentials for staging
  and production.
- Configure rate limiting/WAF protections for authentication, capture upload,
  Q&A, and admin routes.
- Keep `/api/admin/*` disabled unless an authenticated operator workflow needs
  it; never expose the admin token to the Android app.
- Set alerts for health-check failures, 5xx rates, queue failures, database
  saturation, storage errors, and Gemini quota/errors.
- Add structured server logs with request ids, but never log bearer tokens,
  images, free-text Q&A, or exported data.
- Run a dependency/security scan and review the release R8 mapping output.
- Perform a database backup restore drill before launch.

## P1 — feature/business decisions still represented by development code

### Billing

The current Premium upgrade/cancel flow is a simulated/local backend flow.
If paid subscriptions are required for production, implement and verify:

- Google Play Billing purchase and restore flows.
- Backend purchase-token validation.
- Subscription lifecycle/webhook reconciliation.
- Cancellation, grace period, refund, and account-switch behavior.
- Authoritative entitlement updates after every billing event.

If the first release is free or invite-only, explicitly disable/label the
paywall and document that billing is not part of that release.

### Gemini-backed features

Shelf scan and cited Q&A degrade without Gemini. Decide whether launch will:

- Require and monitor Gemini in production, including quota and failure
  behavior; or
- Ship the manual shelf-add and non-Gemini fallback as the intentional launch
  experience.

Test both paths; do not let an upstream provider outage strand the rest of the
app.

## Staging smoke test

Run this against a deployed staging backend and a release-like Android build
using real Firebase test accounts and a disposable database:

1. Sign up/sign in with email and Google.
2. Create or restore the profile and complete onboarding.
3. Decline consent; verify Home/Routine/Insights/You remain usable and camera
   capture is locked.
4. Re-enable consent from Data & Privacy.
5. Capture a deliberately poor frame; verify server quality coaching appears.
6. Capture a valid baseline; verify analysis status/result and history.
7. Turn off the network, queue a capture, restore network, and verify one
   eventual upload without duplication.
8. Add/search/look up a product, log start/stop/change, and verify the timeline.
9. Run shelf scan with Gemini enabled and disabled; verify editable candidates
   or manual-add fallback.
10. Verify Premium locked states and entitlement changes.
11. Verify Q&A triage, thread continuity, citations, and clinician handoff.
12. Verify offers are available to free users and a click is recorded once.
13. Verify export, share failure handling, reminder settings, and notification
   permission.
14. Verify account deletion requires typing `DELETE`, returns from the `204`
   response, clears local data, and prevents the deleted account from being
   reused.
15. Restart/redeploy the backend and confirm Postgres data and photos persist.

## Final go/no-go checklist

Do not open production traffic until every applicable item is checked:

- [ ] Firebase production project and release fingerprints configured.
- [ ] `google-services.json` installed in the build environment.
- [ ] Real release keystore configured and verified.
- [ ] Release API URL points to the production HTTPS backend.
- [ ] Managed Postgres is configured, backed up, restored once, and monitored.
- [ ] Durable encrypted photo storage is configured and deletion-tested.
- [ ] `SKINPROOF_AUTH_REQUIRED=1` has passed staging ownership tests.
- [ ] Wildcard CORS has been replaced with an explicit allow-list.
- [ ] Production secrets are unique, stored securely, and not in Git.
- [ ] Raw-photo retention is enforced by an actual scheduled process.
- [ ] Privacy policy, Data Safety, camera, and facial-data disclosures are ready.
- [ ] Billing is either production-ready or intentionally out of scope.
- [ ] Gemini dependency/fallback behavior is an explicit launch decision.
- [ ] Staging smoke test is complete on a physical device or equivalent.
- [ ] Monitoring, alerting, backup restore, incident owner, and rollback plan
      are documented.
- [ ] Internal Play testing is complete before public rollout.

## Recommended rollout order

1. Create separate staging Firebase/Postgres/provider resources.
2. Deploy backend to staging and validate migrations, health, auth, storage,
   CORS, and smoke tests.
3. Build and sign the staging Android artifact with the staging API URL.
4. Fix every staging failure and repeat the smoke test from a clean account.
5. Create production resources and rotate production-only secrets.
6. Deploy production backend with traffic restricted or private.
7. Build the signed release artifact with the production API URL.
8. Publish to Google Play internal testing.
9. Monitor crashes, auth failures, capture quality failures, queue health,
   database/storage, and deletion requests.
10. Roll out gradually, retaining the previous backend image and Android
    release for rollback.

For provider-specific deployment commands, use `DEPLOY.md`. For Android SDK,
signing, build types, and Firebase file placement, use `app/README.md`.
