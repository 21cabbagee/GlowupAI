# GlowUpAI Play Store external validation checklist

Updated 8 September 2026. This file contains work that cannot be completed from this repository alone. Check each item with an owner and attach the evidence before uploading a production bundle.

## Current local evidence

- Android `testDebugUnitTest` and `lintDebug`: passed locally.
- Backend: `368 passed, 1 skipped` with the isolated test database and photo store.
- Android instrumentation/device testing: not run; no device was connected in this workspace.
- A production signed AAB: not built here; the release keystore and production endpoints are intentionally absent.
- CI quality checks still need an owner to resolve the existing Black, Mypy, and Bandit findings before the release workflow can be treated as green.

## Must be completed before closed testing

### Production services and secrets

- [ ] Create separate staging and production Supabase projects. Apply every SQL migration in `backend/glowupai/migrations/` and record the migration versions.
- [ ] Enable private Supabase Storage for the configured image bucket. Verify object ownership, retention, versioning, and restore behavior with a non-production account.
- [ ] Provision managed PostgreSQL, Redis (recommended), and the API and worker services. Confirm the worker claims and retries jobs after an API restart.
- [ ] Configure production HTTPS origins, Supabase JWT/JWKS verification, service-role key, billing encryption key, Play package name, product IDs, rate limits, alert webhook, and Sentry if used. Do not put any secret in the Android project.
- [ ] Run `/api/live`, `/api/ready`, authenticated two-user isolation, capture upload/retry, storage access, deletion, backup/restore, and worker restart smoke tests against staging.
- [ ] Configure daily database plus object-storage backups, verify one restore, and record measured RPO/RTO.

### Google Play billing

- [ ] Create the subscription product and base plans in Play Console, set countries, prices, tax/merchant profile, grace period, account hold, pause, and cancellation behavior.
- [ ] Create the Play service account, restrict its permissions, and configure the backend purchase verification credentials.
- [ ] Configure Real-time Developer Notifications through Cloud Pub/Sub and verify duplicate delivery, renewal, expiry, refund, revoke, grace-period, and account-hold reconciliation.
- [ ] Install the signed staging/release build from Google Play internal testing and exercise purchase, restore purchases, upgrade/downgrade, cancellation, offline launch, and pending purchase flows on physical devices.

### Privacy, safety, and policy review

- [ ] Publish a working privacy policy, terms, support, and account-deletion webpage using the same developer/app name shown in Play Console. Test the deletion link while logged out and logged in.
- [ ] Complete Play Console Data safety, Account deletion, Health apps, App content, Ads, and AI-generated content declarations as applicable. Google requires a Health apps declaration for apps offering health-related features or information: <https://support.google.com/googleplay/android-developer/answer/16679511>.
- [ ] Have a qualified reviewer approve cosmetic-only claims, dermatologist hand-off/safety copy, AI prompt/provider policy, retention periods, and model measurement limitations. Preserve the signed review record.
- [ ] Verify that optional facial-photo consent, analytics consent, export, deletion, and model-training consent are separate and accurately represented in the published policy.
- [ ] Review all third-party assets, fonts, icons, product data, ingredient data, affiliate links, and provider terms for commercial licenses and attribution requirements.

### Device and release validation

- [ ] Build `bundleRelease` with the real production URLs, Supabase publishable key, and release keystore. Verify the signer, version code, R8 mapping, native libraries, and AAB upload in Play Console.
- [ ] Test the Play-installed build on representative Android versions and OEMs: sign-in/recovery, onboarding, photo picker, camera permission denial, rotation/background/process death, offline/outbox retry, capture quality rejection, result recovery, deletion, export/share, reminders, dark theme, large text, TalkBack, and reduced motion.
- [ ] Test the release artifact on a 16 KB page-size Android 15/16 device or emulator and record the result. Google Play requires compatible apps targeting Android 15+ to support 16 KB pages: <https://developer.android.com/guide/practices/page-sizes>.
- [ ] Run Play pre-launch report and resolve crashes, ANRs, privacy warnings, accessibility failures, deep-link issues, and incompatible-device findings.
- [ ] Complete the required closed-test track with the required opted-in testers, collect observed-user evidence, and verify staged rollout/rollback procedures.

## Launch gate

The app is ready for Play submission only when all unchecked items above have an owner, evidence, and a recorded date. A local green unit-test run does not substitute for provider, Play-installed, policy, billing, device, or production recovery evidence.

### Useful source policies

- Account deletion: <https://support.google.com/googleplay/android-developer/answer/13327111>
- AI-generated content: <https://support.google.com/googleplay/android-developer/answer/14094294>
- Play technical quality and 16 KB compatibility: <https://support.google.com/googleplay/android-developer/answer/17492799>
