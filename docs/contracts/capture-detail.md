# Capture detail contract

`GET /api/users/{userId}/captures/{captureId}?vertical=skin` is an authenticated owner-scoped read.

The server returns the accepted capture result shape used by `POST /api/captures`: capture ID, capture timestamp, baseline flag, processing status, quality, analysis job ID, nullable metric values, measurement explanation, vertical and baseline comparison. The route may return an owned capture outside the free history presentation window.

An unknown, deleted, or differently owned capture is not disclosed and returns the normal not-found/ownership response. The Android result screen carries only the ID, reads its in-memory handoff first, and refetches this route after process death. A network failure renders a retryable error; a confirmed not-found renders an unavailable state.
