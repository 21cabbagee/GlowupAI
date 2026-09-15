# Release decisions

These decisions keep implementation aligned with the production plan and prevent local feature work from inventing provider, billing, legal, or measurement behavior.

## Recorded decisions

1. The active onboarding destination is the server-backed `OnboardingRoute`. It submits the profile through `PATCH /api/users/{userId}/profile`; local completion is a cache of the authoritative response.
2. Gallery selection uses Android Photo Picker and does not require `READ_MEDIA_IMAGES` or legacy external-storage permissions. Camera permission remains contextual to capture.
3. Production backend startup fails closed unless Supabase auth, PostgreSQL, explicit HTTPS CORS origins, quality validation, durable encrypted photo storage, and a strong admin secret are configured.
4. Production Vercel configuration does not allow `GLOWUPAI_SKIP_QUALITY_CHECKS`, localhost origins, memory-only photos, or an ephemeral database/storage configuration.
5. Capture result navigation carries only the capture ID. The owner-checked capture-detail endpoint is the recovery source after process death; the in-memory cache is an optimization only.
6. Public safety triage remains `/api/triage`. The admin triage route is `/api/admin/triage` so route registration order cannot change its authorization policy.
7. Subscription upgrades remain blocked from being presented as real payment until Google Play product configuration, purchase-token verification, RTDN, reconciliation, and Play-installed sandbox evidence exist. A local entitlement mutation is not production billing evidence.

## Inputs still owned outside the repository

- Play developer/merchant identity, launch country, product IDs, monthly/annual prices, tax setup and testing eligibility.
- Production Supabase application identities and staging Supabase application identity.
- Supabase project URLs, private bucket names, retention period, and service-role key ownership.
- Public support/privacy/legal contacts and qualified review of cosmetic measurement and health-related copy.
- Consented measurement evaluation data and reviewer-approved thresholds.
