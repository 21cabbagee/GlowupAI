# Google Play Billing setup

The Android client uses Google Play Billing for the purchase UI. Premium is granted only after
the backend verifies the purchase token with the Google Play Developer API.

## Play Console

1. Create the application with package name `com.glowup.ai`.
2. Create subscription products whose product IDs exactly match
   `GLOWUPAI_PLAY_PRODUCT_IDS` (comma-separated). Configure and activate their base plans.
3. Add license testers and publish the app to an internal or closed testing track. A locally
   installed APK is not enough to exercise real Play checkout.

## Backend credentials

Create a Google Cloud service account, enable the Android Publisher API, and grant the service
account the required Play Console API access for this application. Encode its JSON key as one
line and set `GLOWUPAI_PLAY_SERVICE_ACCOUNT_JSON_B64` on the backend. It must never be shipped in
the Android APK, frontend bundle, or repository. `GOOGLE_APPLICATION_CREDENTIALS` may be used
instead when the deployment provides a protected credentials file.

Set these backend values:

- `GLOWUPAI_PLAY_PACKAGE_NAME=com.glowup.ai`
- `GLOWUPAI_PLAY_PRODUCT_IDS=premium_monthly,premium_yearly`
- `GLOWUPAI_BILLING_KEY` to a random base64-encoded 32-byte AES key
- `GLOWUPAI_PLAY_RTDN_AUDIENCE` to the exact HTTPS Pub/Sub push endpoint
- `GLOWUPAI_PLAY_RTDN_EMAIL` to the verified service-account email used by Pub/Sub push auth

## RTDN

Create a Cloud Pub/Sub topic, grant Google Play permission to publish to it, and create an HTTPS
push subscription targeting `/api/billing/play/notifications`. Configure the push subscription
to include an OIDC token from the service account named by `GLOWUPAI_PLAY_RTDN_EMAIL`. The
backend validates the token audience and email, deduplicates Pub/Sub message IDs, then fetches
the complete subscription state from Google before changing entitlements.

Run the worker continuously so the five-minute purchase reconciliation loop can repair missed
notifications. A successful client purchase still posts its token immediately; RTDN and worker
reconciliation cover renewals, cancellation, grace period, hold, refund, and expiry.

## Verification checklist

- Build and install a signed AAB from an internal/closed Play track.
- Buy with a license tester and confirm the token verification endpoint returns `premium/active`.
- Force a pending purchase, cancellation, renewal, account hold, and expiry; confirm only the
  states documented by Google grant access.
- Reinstall or use a second device, tap **Restore purchases**, and confirm the same Play account
  cannot bind its token to a different GlowUp account.
