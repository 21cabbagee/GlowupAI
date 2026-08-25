# GlowUp AI — Status Summary

This replaces an earlier version of this file that described a 10-step
narrative, a "billion dollar vision" with valuation projections, and
Mac-specific paths (`/Users/21cabbage/...`) that do not apply to this
machine or this repo. None of that belonged in a status document. This
version states what is verifiably true as of this writing and nothing else.

The authoritative, continuously-updated source for status is
`ANDROID_PLAN.md` — this file is a plain-language summary of it and of the
current repo contents, written at one point in time while a multi-agent
rebuild is in progress. If this file and `ANDROID_PLAN.md` disagree, trust
`ANDROID_PLAN.md`.

## Where this started

An earlier Android app existed as three Kotlin files, with almost the
entire app (1,426 lines) in one `MainActivity.kt`: no ViewModel, no
dependency injection, no navigation library, no persistence, only 3 of the
backend's roughly 56 endpoints wired up, a new backend user created on
every single analysis, five of six result-screen metrics hardcoded, a
hardcoded mock streak, the unmodified purple Compose template theme, no
release signing, R8 disabled, and only the two Android Studio template
tests. `ANDROID_PLAN.md` records this audit in full and is the reason a
structured rebuild plan exists at all.

## What exists now

The app package (`app/src/main/java/com/glowup/ai/`) now has a layered
structure — `core/design`, `core/ui`, `data/remote`, `data/local`,
`data/repository`, `data/work`, `di`, `domain`, and one `feature/<name>`
package per product area (auth, onboarding, capture, home, routine,
insights, discover, account, shell) — matching the target architecture in
`ANDROID_PLAN.md` §1. That is a structural observation from reading the
file tree, not a claim that every screen is feature-complete or bug-free;
several feature packages are still being actively written by other agents
as this document is being written, and this file was not re-verified after
those edits. `ANDROID_PLAN.md`'s phase gates are the place to check for the
current, exact state of each phase.

Backend: `backend/skinproof/complete_api.py` is the live FastAPI app
(`skinproof.complete_api:app`), backed by `complete_service.py`, exposing
on the order of 56 routes documented endpoint-by-endpoint in
`backend/docs/frontend-api-map.md`. It runs against SQLite locally or
Postgres in production (`DATABASE_URL`).

## Known to be missing or incomplete

- **Not deployed anywhere.** There is no live backend URL. `DEPLOY.md`
  documents a Railway runbook that has been verified only by reading code —
  no live Railway/Render project, no live Postgres instance, and no real
  HTTPS round trip have been exercised, because the environment this was
  written in has no Docker and no provider account.
- **No Firebase project yet.** `app/google-services.json` does not exist in
  this checkout. The Gradle build tolerates this by design (it skips the
  Firebase Gradle plugins and prints a warning instead of failing), but
  Firebase Auth will not initialize at runtime until the file is added.
- **No release signing configured.** `app/keystore.properties` does not
  exist; release builds fall back to debug signing with a build-time
  warning. Do not distribute a release build signed that way.
- **Google Play listing, screenshots, and privacy policy do not exist yet.**

## Blocked on a human

These cannot be done by an agent in this environment — they require an
account, a browser login, or a secret that must not be held by an agent:

| Item | Needed for |
| --- | --- |
| Firebase project (`glowup-ai`), `google-services.json`, SHA-1/SHA-256 fingerprints, service account | Firebase Auth / Google Sign-In, backend token verification |
| Railway or Render account + login | Backend deployment |
| `GEMINI_API_KEY` (production) | Shelf-scan OCR and cited Q&A — the app degrades gracefully without it, but those two features don't work |
| Upload keystore + passwords, generated and stored outside the repo | Signed release builds |
| Google Play Console developer account ($25 one-time fee) | Any Play Store listing, including internal testing |
| A hosted privacy policy at a public URL, covering facial-image processing explicitly | Play Console Data Safety form |

## Where to look for more detail

- `ANDROID_PLAN.md` — phase-by-phase plan, task ownership, and gates.
- `REPO_STRUCTURE.md` — the actual file tree.
- `backend/docs/frontend-api-map.md` — the full API contract.
- `DEPLOY.md` — the deployment runbook and what has/hasn't been verified.
- `app/README.md` — Android build/run setup on this machine.
