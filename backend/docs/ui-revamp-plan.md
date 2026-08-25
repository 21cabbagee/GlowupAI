# SkinProof UI Revamp — End-to-End Plan

Status: **implemented and verified.** See `web/README.md` to run it.
Scope: presentation layer only. The FastAPI domain (35 `/api` endpoints) is unchanged.

## 1. Decisions

| Question | Decision | Why |
| --- | --- | --- |
| Web stack | Next.js 16 App Router + React 19 + Tailwind v4 + Motion | Highest polish ceiling; npm is reachable so it is buildable and verifiable here. |
| Android app | Same codebase, dedicated mobile shell + installable PWA | No Android SDK/Gradle/Studio on this machine, so Compose source could never be compiled or run. A PWA with native-grade interaction ships a real, testable Android UI today. |
| Backend coupling | Next `rewrites` proxy `/api/*` to uvicorn | No CORS, no client config, one origin. |
| Fonts | Tuned system stack, no webfonts | fonts.googleapis.com and fonts.gstatic.com are TLS-blocked here; a webfont build would fail. |
| Native Compose | Deferred, not cancelled | Port once `ANDROID_HOME` exists. The design tokens are authored to translate 1:1 to a Compose `ColorScheme`. |

The old `skinproof/static/index.html` is retained as a fallback until the new UI reaches parity.

## 2. Design language

Reference points: Bumble's confident yellow-on-black and generous type; Instagram's
quiet chrome, edge-to-edge media, and gesture-first navigation. Premium daily-use
SaaS, not a marketing template.

### Colour — "Honey", distinct from Bumble

Bumble's primary is `#FFC629`, a lemon yellow. Ours is deliberately warmer and
deeper — a honey/amber gold. Adjacent enough to read as the same family, clearly
not a clone.

| Token | Value | Role |
| --- | --- | --- |
| `honey-300` | `#FFE29A` | tints, chart fills |
| `honey-400` | `#FFD166` | hover on dark, secondary accent |
| `honey-500` | `#FFBE2E` | **primary** — CTAs, active nav, focus |
| `honey-600` | `#F0A400` | press state |
| `honey-700` | `#B87300` | yellow-as-text on light (AA) |
| `ink-900` | `#14110B` | text, and the label on every honey surface |
| `ink-600` | `#57503F` | secondary text |
| `paper` | `#FFFDF8` | warm white app background |
| `surface` | `#FFFFFF` | cards |

Contrast rule, non-negotiable: yellow is never text on white. Yellow is a
**surface**; `ink-900` sits on top of it (16.1:1). This is exactly why Bumble
reads as premium rather than cheap.

Semantic states map to the four verdict labels the backend already returns:
`keep` → honey, `likely_useful` → sage `#3F7D5C`, `evidence_unclear` → clay
`#B87300`, `investigate` → rust `#C2453F`.

Dark mode is first-class: warm charcoal `#0F0D0A` surfaces, honey accents,
same tokens redefined — never a grey inversion.

### Type

System stack tuned per platform (`Segoe UI Variable` leads on Windows 11).
Display sizes get tight tracking (`-0.04em`) and `1.02` leading; body stays at
`1.55`. Two weights only — 800 for display, 400/600 for text.

### Motion

Motion communicates state, never decorates. Spring `[0.2, 0.8, 0.2, 1]`,
140–220ms. Shared-element transition on capture thumbnails, sheet springs on
mobile, optimistic streak-ring fill. Everything respects
`prefers-reduced-motion`.

### Anti-slop rules

No gradient-mesh hero blobs, no glassmorphism, no emoji as iconography, no
purple-blue AI gradients, no centred marketing copy inside the product, no
stock-photo placeholders. Density and real data instead: every screen shows the
user's actual numbers, and empty states name the next action.

## 3. Information architecture

Both surfaces consume the same seven domains. Layout differs; content does not.

| Screen | Web layout | Mobile layout | Endpoints |
| --- | --- | --- | --- |
| Landing | public marketing page | — | none |
| Onboarding | centred single column | full-bleed steps | `POST /api/users`, `POST /consent` |
| Home | 12-col: streak ring, stat tiles, verdicts, history chart | scroll feed, sticky header | `GET /dashboard`, `/capture-guide` |
| Capture | drop zone + live quality gate | camera-first, FAB entry | `POST /api/captures` |
| Routine | two-pane products / experiments + timeline | tabbed, bottom sheets | `/api/products`, `/api/experiments`, `/api/routine-events` |
| Insights | chat + ingredient panel | full-height chat | `/api/users/{id}/qna`, `/ingredient-explainer` |
| Discover | cohort grid + offers rail | cards | `/discover`, `/commerce/offers` |
| Account | settings sections + audit | list rows | `/profile`, `/subscription`, `/export`, `DELETE /users` |

Web chrome: persistent left rail (collapsible), 1240px content max.
Mobile chrome: 5-tab bottom bar (Home · Routine · **Capture FAB** · Insights ·
You) with safe-area insets, 48dp minimum targets, swipe-back.

## 4. Build order

1. **Scaffold** — Next.js 16 + TS + Tailwind v4 in `web/`, `/api` rewrite to `:8000`.
2. **Design system** — `tokens.css` (both themes), primitives: Button, Card,
   StatTile, Tag, Field, Sheet, EmptyState, Skeleton. Typed API client mirroring
   every endpoint.
3. **Shells** — desktop rail + mobile tab bar, theme provider, PWA manifest +
   maskable icons + service worker.
4. **Screens** — Home and Capture first (they set the bar), then Routine,
   Insights, Discover, Account, Onboarding, Landing.
5. **Charts** — history sparklines and metric trends on the shared palette.
6. **Verify** — `npm run build`, uvicorn + Next running together, real profile
   created through the UI, capture posted, DevTools mobile viewport at 412×915
   (Pixel), Lighthouse PWA + a11y pass, existing `python -m unittest` still green.

## 5. What verification caught

Worth recording, because all three were invisible until the UI ran against the
real service:

- **`/api/routine-events` accepts only `start`, `stop`, `change`** — not a daily
  "applied" tick. The Routine screen had a four-value action list that would
  have 400'd on every submit. A routine event marks a *change to the variable*.
- **`rewrites()` is baked at build time.** Building without
  `SKINPROOF_API_ORIGIN` and then setting it for `next start` silently proxied
  the whole app to whatever else was on port 8000.
- **The capture quality gate is stricter than it looks.** `inspect_image`
  measures sharpness on a 64×64 downsample, so seed frames need structure at
  ~8px to clear `sharpness >= 0.35`; fine noise is destroyed by the resize.

## 6. Definition of done

- Both themes ship; no hardcoded colour outside `tokens.css`.
- Every backend endpoint the old `index.html` used is reachable in the new UI.
- Keyboard navigable, visible focus rings, AA contrast throughout.
- Production build passes; app runs against the real FastAPI server.
- Native Compose port documented as the follow-on once the Android SDK exists.
