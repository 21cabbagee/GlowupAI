# SkinProof

SkinProof is a privacy-first personal appearance evidence workspace. It turns
standardized skin captures, routine events, products, and context into a
longitudinal record with conservative trends and product verdicts.

It is a cosmetic tracking product, not a diagnosis or treatment product.
Medical-scope questions are routed to dermatologist guidance.

## End-to-end flow

```text
Landing -> Create/restore profile -> Choose goals -> Grant photo consent
        -> Guided baseline capture -> Server quality checks -> Metric snapshot
        -> Add products -> Log routine changes -> Wait through stabilization
        -> Repeat comparable captures -> Trends/recap -> Product verdict
        -> Premium experiments, grounded insights, and deeper analysis
```

1. **Onboarding:** create a local profile, select goals and experience level,
   then grant explicit facial-photo consent. The current vertical is `skin`.
2. **Capture:** use camera or upload with frontal pose, neutral expression, even
   light, and consistent distance. Server-side checks reject unsuitable frames
   and return specific coaching. Client quality values cannot override them.
3. **Measurement:** accepted captures produce blemish proxy, redness,
   dark-spot, and texture metrics with confidence, quality score, noise floors,
   comparison readiness, model version, and provenance.
4. **Routine:** search, scan, barcode-lookup, or manually create products; log
   `start`, `change`, and `stop` events. Confound warnings identify overlapping
   stabilization windows.
5. **Evidence:** repeat comparable captures while holding the routine steady.
   Charts and weekly recaps explain direction, confidence, and next action.
6. **Verdict:** stabilization-aware attribution returns `Keep`,
   `Likely useful`, `Evidence unclear`, or `Investigate`. Personal verdicts use
   only the user's own history; cohort signals never alter them.
7. **Premium:** run one-variable experiments, ask grounded questions, inspect
   ingredients and product overlap, log context, search correlations, estimate
   routine waste, see eligible cohort signals, and generate a dermatologist
   export.
8. **Privacy:** export the record, review audit events, reprocess retained
   captures, or delete the profile and its related records.

## Functionality

### Account, consent, and privacy

- Local profile creation and restore by profile ID.
- Display name, skin type, focus, goals, and experience-level profile.
- Explicit versioned facial-data consent before capture.
- Structured JSON data export and cascading account deletion.
- Audit trail, measurement feedback, user labels, and model reprocessing jobs.
- Raw image bytes stay outside SQLite and API JSON.
- Optional AES-GCM file storage uses a per-user derived key and fresh nonce.

### Capture and progress

- Camera/upload review flow with framing guidance.
- Server-side brightness, sharpness, pose, distance, and quality gates.
- Actionable retake coaching rather than generic rejection.
- Baseline selection and comparable longitudinal history.
- Deterministic blemish, redness, dark-spot, and texture measurements.
- Confidence labels, capture protocol, noise-floor copy, and model provenance.
- Capture streak, cadence guide, reminders, analytics, and next capture window.
- Weekly recap and quick check-ins for routine state and perceived skin feel.

### Products, routines, and evidence

- Product catalog, search, barcode lookup, manual entry, INCI parsing, category,
  stabilization days, price, dose, frequency, slot, notes, and timestamps.
- AI shelf scan with review-before-commit and manual fallback if Gemini vision
  is not configured.
- Start/change/stop event history and proactive confound warnings.
- One-variable Premium experiments with lifecycle status and early-stop advice.
- Conservative four-state verdicts with evidence windows and generated copy.
- Free lifetime unlock for one definitive verdict; `Evidence unclear` remains
  visible because uncertainty is useful evidence.
- Premium ingredient explanation, personal/cohort overlap prediction, and
  pre-purchase guidance.

### Insights, discovery, and commerce

- Premium grounded Q&A with capture and routine-event citations.
- Safety triage before any insight generation.
- Premium context events for sleep, weather, travel, cycle, stress, illness,
  and other possible confounders.
- Premium root-cause correlations, budget optimizer, and printable
  dermatologist report.
- Premium cohort Discover with minimum sample-size protection.
- Affiliate offers and click disclosure for all plans; offers cannot influence
  a personal verdict and there is no paid cohort placement.
- Optional Gemini text and vision adapters with deterministic/local fallbacks.

## Free and Premium matrix

| Capability | Free | Premium |
| --- | --- | --- |
| Profile, consent, export, deletion, audit | Yes | Yes |
| Guided captures, quality coaching, metrics, streaks | Yes | Yes |
| Products, routine events, stabilization/confound warnings | Yes | Yes |
| Shelf scan with manual fallback | Yes | Yes |
| Affiliate offers with disclosure | Yes | Yes |
| Capture history | Recent 90-day window | Full history |
| Definitive product verdicts | One lifetime unlock | Unlimited |
| Experiments and early-stop guidance | No | Yes |
| Ingredient analysis and purchase prediction | No | Yes |
| Grounded Q&A | No | Yes |
| Context and root-cause search | No | Yes |
| Budget optimizer and dermatologist export | No | Yes |
| Cohort Discover | No | Yes |

## Application routes

| Route | Function |
| --- | --- |
| `/` | Landing page |
| `/start` | Profile creation and onboarding |
| `/sign-in` | Local restore and developer demo access |
| `/home` | Dashboard, history, recap, verdicts, and check-ins |
| `/capture` | Guided camera/upload and capture review |
| `/routine` | Products, events, scan, purchase guidance, experiments |
| `/insights` | Q&A, ingredients, triage, context, correlations, budget |
| `/discover` | Cohort context and disclosed offers |
| `/account` | Profile, consent, plan, export, reprocessing, deletion |
| `/docs` | Interactive FastAPI documentation |

The Next.js client is an installable PWA with responsive desktop/mobile shells,
camera framing, a mobile capture action, theme support, and accessible charts.
Its service worker caches the UI shell but never `/api/*` responses.

## API groups

- **Account:** health, create user, profile read/update, consent, subscription,
  export, deletion.
- **Capture:** submit capture, capture guide, dashboard, history, weekly recap,
  check-ins, feedback, engagement, analytics.
- **Products/routine:** create/search/lookup/detail, ingredient explainer,
  product prediction, purchase guidance, routine events, confound check.
- **Experiments/insights:** experiment create/list/detail/status, Q&A, triage,
  context events, root-cause search, budget optimizer, dermatologist export.
- **Jobs/discovery:** shelf scan/status/confirmation, labels, reprocessing,
  cohort Discover, offers/clicks, admin audit and feedback summary.

Premium or consent-protected operations return HTTP 403 when prerequisites are
not met. Domain validation failures return HTTP 400. See `/docs` and
`docs/frontend-api-map.md` for request and response fields.

## Architecture

```text
Next.js web/PWA (web/)
        | relative /api/* through Next rewrites
        v
FastAPI (skinproof/complete_api.py)
        v
CompleteSkinProofService
  |-- capture quality + deterministic metrics
  |-- attribution + stabilization windows
  |-- entitlements + feature usage
  |-- grounded insight + optional Gemini
  |-- in-process jobs
        |
        +--> SQLite for local/test or PostgreSQL in deployment
        +--> memory photo store or AES-GCM encrypted files
```

| Path | Responsibility |
| --- | --- |
| `skinproof/complete_api.py` | FastAPI routes and validation |
| `skinproof/complete_service.py` | Complete domain workflows |
| `skinproof/metrics.py` | Deterministic appearance measurements |
| `skinproof/attribution.py` | Stabilization-aware verdict logic |
| `skinproof/photos.py` | Memory and encrypted photo stores |
| `skinproof/postgres_db.py` | PostgreSQL pool and migration runner |
| `skinproof/migrations/` | Production SQL migrations |
| `skinproof/google_ai.py` | Optional grounded Gemini text/vision adapters |
| `web/app/` | Next.js pages |
| `web/lib/api.ts` | Typed frontend API client |
| `tests/` | Core, API, growth, experience, and AI coverage |

## Run locally

Backend:

```powershell
python -m pip install -e .
python -m skinproof.cli serve
```

Open <http://127.0.0.1:8000>. SQLite defaults to
`.data/skinproof.sqlite3`, which is git-ignored.

Web client in a second terminal:

```powershell
python -m skinproof.cli serve --port 8010

cd web
npm ci
$env:SKINPROOF_API_ORIGIN = "http://127.0.0.1:8010"
npm run dev
```

Open <http://localhost:3000>. Set `SKINPROOF_API_ORIGIN` before `npm run build`
too because Next.js evaluates rewrites at build time.

PostgreSQL and API with Docker:

```powershell
docker compose up --build
```

This starts PostgreSQL 16 and the API on port 8000. A database URL selects
PostgreSQL and applies `skinproof/migrations/`; SQLite remains the local/test
fallback.

## Configuration

Use `.env.example` for the PostgreSQL URL and pool, SQLite path, Gemini key and
model, environment, consent policy, and model version. Additional storage
settings include `SKINPROOF_PHOTO_DIR`, base64 32-byte
`SKINPROOF_PHOTO_KEY`, and `SKINPROOF_RAW_RETENTION_DAYS`.

Keep real keys in environment variables or a secret manager. The ignored local
`first.py` key bridge and `.data/` database are not pushed.

## Premium test user and login credentials

This build does **not** have server-side email/password authentication. A local
profile ID is the current development restore token and is not a production
security boundary.

The Premium test profile already created in the current local database is:

| Field | Value |
| --- | --- |
| Display name | `Alex Demo` |
| Local profile ID | `b6a10e48-689a-411e-85b3-c3845a2f2a4e` |
| Plan/status | `premium` / `active` |
| Entitlement source | `local_seed` |
| Email/password | Not applicable |

To open it, run the API against the existing `.data/skinproof.sqlite3`, visit
`/sign-in`, enter the profile ID in **Local profile ID**, and continue. The
database is git-ignored, so this exact profile exists only in the current local
workspace. On a fresh clone, create a profile and use Account -> Upgrade, or
call `POST /api/users/{user_id}/subscription/upgrade`.

The separate temporary developer fixture on the sign-in page is:

| Field | Value |
| --- | --- |
| Username | `skinproof-demo` |
| Access code | `temporary-access-2026` |
| Result | Creates/restores a local **Free** profile |

It is not the Premium profile. Upgrade it locally from Account when Premium UI
testing is needed.

## Tests

```powershell
python -m unittest discover -s tests -v
```

The suite covers the core loop, consent, metrics, quality gates, attribution,
entitlements, experiments, history limits, check-ins, measurement feedback,
AI fallbacks, shelf scan, commerce, export, and deletion.

## Production limitations

- Authentication and authorization must be added before exposing the API to
  untrusted users; the current API accepts arbitrary `user_id` values.
- Billing is a local entitlement toggle, not a payment processor.
- Managed object storage, KMS, billing webhooks, and a durable external job
  queue remain deployment adapters.
- SkinProof is cosmetic tracking, not diagnosis.

## Supporting documentation

- [Architecture](docs/architecture.md)
- [Frontend/API contract](docs/frontend-api-map.md)
- [Metrics and verdict rules](docs/metrics-and-verdicts.md)
- [Operations checklist](docs/operations.md)
- [Experience redesign](docs/experience-redesign.md)
- [Growth features](docs/superiority-plan.md)
- [Gemini integration](docs/google-gemini.md)
- [Web workspace](web/README.md)

## License

No license file is currently included. Add the intended license before public
distribution.
