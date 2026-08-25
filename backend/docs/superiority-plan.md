# SkinProof — features and migrations to do now

Grounded in the code as of 2026-08-19. Ordered by what blocks what, not by
appeal. Sections 0–2 are prerequisites; sections 3–5 are the product work that
makes the app worth paying for.

**Status as of 2026-08-19 (same-day follow-up build):** §0.1 (auth/session/CORS)
is explicitly deferred — out of scope for this pass, tracked separately, and
still the top blocker before real users. §0.2 (module collapse) and §0.3
(async job queue) are done. §1 (tier repackaging), §2 (quota/context/job
migrations), §3 (capture coach, shelf-scan, pre-purchase prediction), and §4
(confound warnings, root-cause search, budget optimizer, derm export,
experiment early-stop) are implemented end-to-end (backend + frontend + test
coverage — see `tests/test_growth_features.py`). §5's photo storage/KMS and
model-pinning items remain open.

---

## 0. Blockers — do these before anything else

### 0.1 There is no authentication

`user_id` is a path or body parameter on every endpoint
(`skinproof/complete_api.py:145-283`). There is no `Depends`, no bearer token,
no session, no cookie anywhere in `skinproof/`. The web client stores a raw
user id in `localStorage` (`web/app/sign-in/page.tsx:65-78`) and sends it.

Consequences as written:

- any caller who knows or guesses a user id can read that user's facial
  captures, `GET /api/users/{user_id}/export`, or
  `DELETE /api/users/{user_id}`;
- `GET /api/admin/audit` and `POST /api/admin/offers` are unauthenticated;
- `allow_origins=["*"]` with `allow_methods=["*"]`
  (`skinproof/complete_api.py:118`) makes every one of those callable from any
  origin in a browser.

This is facial biometric data behind a consent flow the product advertises. It
is a legal blocker under DPDP/GDPR, not only an engineering one. Nothing else
in this document should ship first.

**Migration**

1. Add a `sessions` table and an `auth_credentials` table (or adopt an
   OIDC provider and store only `subject`). Keep it boring — email link or
   password + argon2.
2. Add `dependencies.py` with `current_user(request) -> User` resolving a
   bearer token or `HttpOnly; Secure; SameSite=Lax` cookie.
3. Change every `/api/users/{user_id}/...` route to `/api/me/...` and derive
   the id from the session. Retain the old paths for one release returning
   `410 Gone` so the web client fails loudly rather than silently reading a
   stranger's data.
4. Split admin routes onto a separate router with a distinct role check.
5. Replace the CORS wildcard with an explicit allow-list from
   `Settings` (`SKINPROOF_ALLOWED_ORIGINS`), and add `allow_credentials=True`
   only alongside that list.
6. Rate-limit `POST /api/captures`, `/api/me/qna`, and auth endpoints.

Add a test that asserts user A cannot read user B — the current suite would
pass with the hole wide open.

### 0.2 Collapse the duplicated module tree

There are three generations of the same application living side by side:

| Layer | Files |
|---|---|
| legacy | `api_legacy.py`, `service.py`, `db.py` |
| complete | `complete_api.py`, `complete_service.py`, `complete_db.py`, `full_db.py` |
| runtime | `complete_api_runtime.py`, `complete_service_runtime.py` |
| dead | `full_db.ai-previous.py`, `google_ai.ai-previous.py`, `insights.ai-previous.py` |

`api.py` re-exports the runtime app and keeps an `atexit` hook that reaches
into `complete_api` and `api_legacy` to close whichever databases happened to
be constructed (`skinproof/api.py:31-49`). That hook exists only because three
apps can each build their own pool.

Roughly 340 lines are `.ai-previous` files that nothing imports, and the
`*_runtime` pair is a subclass layer adding profile endpoints and overriding
four service methods. Every feature below has to be written three times or
guessed at.

**Migration**

1. Delete the three `.ai-previous.py` files (they are recoverable from
   history; if this directory is not yet a git repo, `git init` first — that
   is itself overdue).
2. Fold `complete_service_runtime.py` into `complete_service.py` and
   `complete_api_runtime.py` into `complete_api.py`. There is no second
   consumer justifying the split.
3. Decide whether `api_legacy.py` + `service.py` still have a caller. If the
   only one is `create_app(service=...)` in `api.py`, delete the legacy path
   and simplify `create_app` to a single constructor. `tests/test_api.py` and
   `tests/test_core.py` will tell you.
4. Rename the survivors: `complete_api.py` → `api.py`, `complete_service.py` →
   `service.py`, `complete_db.py`/`full_db.py` → `db.py`. The word "complete"
   is a historical artifact.
5. Once one app object exists, drop the `atexit` scanner for a FastAPI
   `lifespan` handler.

### 0.3 Make analysis asynchronous

`reprocess_jobs` is written, run, and completed inside the request
(`skinproof/complete_service.py:361-377`). A user with 400 captures blocks a
worker for the whole reprocess and gets a timeout. The `analysis_jobs` table
exists but is not used as a queue.

**Migration**: put capture analysis and reprocessing behind a job table
consumed by a worker process (`skinproof.worker`), with the API returning
`202` plus a job id. This is a hard prerequisite for the vision features in
§3 — those add seconds of model latency per capture and cannot run inline.

---

## 1. Repackage the tiers

Current split (`skinproof/complete_service.py:19`):

```python
PREMIUM_FEATURES = {"experiments", "product_verdicts", "ingredient_analysis",
                    "long_history", "qna", "discover", "commerce"}
```

Free is daily photographic labour with every payoff removed. The fix is to
give away the first proof and sell the second.

**Target split**

| Free | Premium |
|---|---|
| capture, quality gates, streaks | unlimited verdicts |
| **one lifetime product verdict, in full** | experiment designer + early-stop |
| AI capture coach (§3.1) | pre-purchase prediction (§3.3) |
| shelf-scan auto-logging (§3.2) | root-cause search (§4.1) |
| confound warnings (§4.2) | routine budget optimiser (§4.3) |
| 90-day history | full history + reprocessing |
| weekly recap (2 sentences) | dermatologist export (§4.4) |
| affiliate offers | — |

**Migration**

1. Replace the flat set with a `Feature` enum carrying a limit, not a boolean:
   `verdicts: 1 lifetime / unlimited`, `history_days: 90 / None`.
   `require_premium` becomes `check_quota(user_id, feature)` returning
   remaining allowance so the UI can show "1 free verdict left" instead of a
   403.
2. Add `entitlement_usage(user_id, feature, used_count)` to the schema
   (`0002_entitlement_quota.sql`).
3. Move `commerce`/`offers` **out** of premium. Charging a subscription and
   taking affiliate revenue on the same recommendation contradicts the
   no-paid-placement claim in `README.md:26`. Free-tier affiliate, paid-tier
   honest broker is the defensible arrangement.
4. Return `402`-style upgrade metadata (feature, what it unlocks, quota state)
   rather than the current bare `PermissionError` string.

---

## 2. Schema migrations to write now

The Postgres runner is already versioned and ordered
(`skinproof/postgres_db.py:100-125`), so these are additive files in
`skinproof/migrations/`. Keep the SQLite DDL in step.

| File | Contents |
|---|---|
| `0002_auth.sql` | `auth_credentials`, `sessions`, `roles` |
| `0003_entitlement_quota.sql` | per-feature usage counters |
| `0004_jobs.sql` | generalised job queue, status index, retry count |
| `0005_context.sql` | `context_events` (sleep, travel, weather, cycle) for §4.1 |
| `0006_predictions.sql` | `product_predictions` cache for §3.3 |

Two indexes worth adding while you are there: `metric_snapshots(user_id,
metric_name, created_at)` — the attribution median scans are per-metric — and
a partial index on `jobs(status)` for the worker poll.

---

## 3. Vision AI — the features that change the product

Today the only model call is text explanation of an already-computed verdict
(`skinproof/google_ai.py`). The system prompt correctly forbids the model from
changing the label. **Keep that line absolute for everything below.** The
moment a model invents a verdict, the evidence contract in
`docs/metrics-and-verdicts.md:46` is worthless and so is the differentiation.

### 3.1 Live capture coach — highest ROI item in the plan

The quality gates (`docs/metrics-and-verdicts.md:5-15`) only reject. Rejection
without instruction is why capture-based apps lose users in week two.

Return, alongside every gate failure, an actionable correction: *"tilt down
about 5° — pitch is 17°"*, *"you are in warmer light than your baseline, move
to the window you used on day 1."* Most of this is deterministic from the
existing pose/brightness numbers; the model layer only phrases it. Ship the
deterministic version first — it needs no model at all.

### 3.2 Shelf scan → routine auto-logging

Photo of the bathroom shelf → multimodal extraction of product names →
existing catalog matching, INCI parsing, stabilization defaults
(`skinproof/catalog.py`). Manual routine entry is the largest abandonment
cause in every tracker of this shape; this removes the onboarding cliff in one
screen.

Requires: image input path to the provider, a review-and-confirm step (never
auto-write products the user has not seen), and §0.3's queue.

### 3.3 Pre-purchase prediction — the actual premium hook

Scan a product in a shop. Against the user's own verdict history plus cohort
data: *"three of the four actives here overlap with the two products that came
back `investigate` for you."*

This prevents the ₹3,000 mistake instead of confirming it 60 days later. The
ingredient parsing, cohort minimums, and verdict history all already exist —
this is mostly composition over `catalog.py` + `attribution.py`, with the
model restricted to explaining the overlap it is handed.

Constraint: it is a similarity statement, never a prediction of efficacy, and
the copy must say so.

---

## 4. Premium depth

### 4.1 Root-cause search
Upgrade Q&A from lookup to hypothesis generation over the full record:
*"texture dips 6–9 days after every travel event."* Needs `context_events`
(migration `0005`). The correlation must be computed in `attribution.py`
against the noise floor; the model only narrates the ranked output.

### 4.2 Confound warnings (free)
"You started two products four days apart — this will resolve as
`evidence_unclear`." Rule 3 in the attribution contract already knows this;
surfacing it *before* the user wastes 30 days is nearly free, feels like the
app is on their side, and protects your own data quality.

### 4.3 Routine budget optimiser
"Four products show no measurable effect — that is ₹4,800/year." A tier that
pays for itself in month one does not need a discount to convert.

### 4.4 Dermatologist export
Measurement-grounded PDF: capture series, verdicts, model versions, noise
floors. High perceived value, near-zero marginal cost, and it stays inside the
non-diagnostic boundary because it reports measurements rather than
conclusions.

### 4.5 Experiment early-stop
Tell the user on day 34 that the result is already conclusive instead of
making them wait 90. Time-to-answer is the thing premium is actually selling.

---

## 5. Operational gaps to close alongside

- **Photo storage**: AES-GCM local only (`skinproof/photos.py`). Needs the S3/
  GCS adapter and KMS-held keys before real users, not after.
- **Model pinning**: `SKINPROOF_GEMINI_MODEL` in `.env.example` names
  `gemini-3.5-flash-lite`. Verify that id against current provider docs before
  deploy, and record the resolved model id in verdict provenance so
  reprocessing stays reproducible.
- **Test suite**: five files, ~400 lines, for 3,600 lines of application. The
  gaps that matter most are cross-user access (§0.1), quota enforcement (§1),
  and attribution edge cases around the stabilization window.
- **Version control**: this directory is not a git repository. Before any of
  the deletions in §0.2, initialise one.

---

## Suggested order

1. §0.1 auth + CORS — nothing ships before this
2. §0.2 module collapse — makes every later change one edit instead of three
3. §2 migrations + §0.3 job queue
4. §1 tier repackaging + §4.2 confound warnings — cheapest retention wins
5. §3.1 capture coach, then §3.2 shelf scan — the onboarding cliff
6. §3.3 pre-purchase prediction — the conversion feature
7. §4 remaining premium depth
