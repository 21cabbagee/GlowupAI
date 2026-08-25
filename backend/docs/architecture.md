# SkinProof complete architecture

SkinProof is implemented as a complete modular product with a stable domain
boundary around the evidence engine. The browser workspace and HTTP API expose
the full product lifecycle; cloud storage, mobile camera SDKs, app-store
billing, KMS, and queue credentials are deployment adapters rather than
missing product workflows.

## Product flow

```text
Capture guide -> calibrated photo -> metric snapshot + provenance
      |                                      |
Routine log -> experiment design ------------+
      |                                      v
      +------------------------------ Attribution engine
                                      |
                    Keep / Likely useful / Evidence unclear / Investigate
                                      |
                 grounded copy, Q&A, Discover, commerce disclosure
```

The complete PostgreSQL schema includes users, consent events, products, routine events,
captures, jobs, versioned metrics, verdicts, appearance profiles,
experiments, reminders, engagement, entitlements, billing events, Q&A,
affiliate offers/clicks, cohort insights, labels, reprocessing jobs, and audit
records. `FullDatabase` extends the transactional schema without changing the
domain contracts used by the focused compatibility API.

## Domain boundaries

- `capture.py`: hard comparability gates. Server brightness and sharpness are
  authoritative; client pose/face fields are accepted only as guidance data.
- `metrics.py`: explainable image measurements with a model version and noise
  floor. It cannot issue a product verdict.
- `attribution.py`: stabilization, confounder detection, before/after paired
  comparisons, confidence, and the four-state verdict contract.
- `complete_service.py`: experiments, engagement, Premium,
  grounded Q&A, cohort evidence, affiliate-only commerce, labels, and
  reprocessing.
- `complete_api.py`: complete HTTP product surface. `api.py` keeps the old
  constructor available for compatibility tests while the default app is the
  complete product.

## Appearance profile

Skin capture shares cadence, consent, raw-photo handling, provenance, and
history under a single appearance profile and metric projection, kept
separate from other users' personal verdicts.

## Privacy and trust

Facial-data consent is recorded separately from account creation. Exports cover
database-held personal data; deletion cascades through the complete database
and photo store. The default development store is memory-only. An optional
AES-GCM file store derives a per-user key from a deployment root key. Paid
placement cannot write or alter verdict labels, and cohort discovery requires
a minimum sample size.

## Database and production adapters

PostgreSQL migrations and pooled connections are implemented in the backend;
the same DATABASE_URL contract works with local PostgreSQL and Neon. The
remaining deployment work is infrastructure integration: replace local
object storage with managed services, provide authenticated identity and
admin roles, connect App Store/Play billing webhooks, put MediaPipe/ARKit in a
mobile shell, and run analysis/reprocessing through durable queue workers.
The user-facing product behavior and local end-to-end flows are implemented.
