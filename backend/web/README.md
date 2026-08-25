# SkinProof UI

The web workspace and the installable Android app are one Next.js codebase.
The FastAPI service in `../skinproof` owns all data; this app only renders it.

## Run

Two processes. Start the API first.

```powershell
# terminal 1 - the API
python -m skinproof.cli serve --port 8010

# terminal 2 - the UI
cd web
$env:SKINPROOF_API_ORIGIN = "http://127.0.0.1:8010"
npm run dev
```

Open <http://localhost:3000>.

`SKINPROOF_API_ORIGIN` defaults to `http://127.0.0.1:8000`, which matches
`skinproof.cli serve` with no `--port`. Set it whenever the API is elsewhere.

> **`rewrites()` is evaluated at build time.** `npm run build` bakes the API
> origin into `.next/routes-manifest.json`, so `SKINPROOF_API_ORIGIN` must be
> set for the *build*, not just for `next start`. Building without it and then
> starting with it silently proxies to the default port.

Requests go to relative `/api/*` paths and Next rewrites them to the API, so the
browser stays on one origin: no CORS, no API base URL in the client.

## Layout

| Path | Contents |
| --- | --- |
| `app/globals.css` | **All** design tokens — the honey palette, both themes, type, motion, chart colours. No colour is hardcoded anywhere else. |
| `app/page.tsx` | Public landing page |
| `app/start/` | Onboarding (profile creation) |
| `app/(product)/` | The authenticated app behind `AppShell` |
| `components/ui.tsx` | Primitives: Button, Card, StatTile, Tag, Field, Notice, EmptyState, PaywallCard, Reveal |
| `components/shell.tsx` | Desktop rail, mobile header, bottom tab bar, consent gate |
| `components/charts.tsx` | Trend chart, sparklines, streak ring, table fallback |
| `components/icons.tsx` | Inline SVG icon set (no icon package, no emoji) |
| `lib/api.ts` | Typed client for all 35 endpoints |
| `lib/session.tsx` | Profile, vertical, plan and consent state |
| `lib/theme.tsx` | Light / dark / system, with a pre-paint script |

## Design rules

- **Yellow is a surface, never text on light.** `honey-500` measures 1.66:1
  against white, so `ink-900` always rides on top of it. Chart strokes use
  `honey-700` on light and `honey-400` on dark, both verified above 3:1.
- Charts show **one metric per plot**. The four metrics have different scales,
  so they are small multiples, never a shared or dual axis.
- Dark mode is warm charcoal, defined token-by-token — not an inverted grey.
- Every interactive target is at least 44px on mobile; the tab bar and FAB
  respect `env(safe-area-inset-*)`.
- All motion is 140–320ms on one easing curve and yields to
  `prefers-reduced-motion`.

Google Fonts is unreachable from this environment, so typography uses a tuned
system stack (`Segoe UI Variable` leads on Windows 11) rather than a webfont.

## Android

`app/manifest.webmanifest` plus `public/sw.js` make the app installable from
Chrome on Android ("Add to home screen") with a standalone window, maskable
icon, and capture/routine shortcuts. The service worker caches the shell but
**never** `/api/*` — a stale metric is worse than no metric. It is registered
only in production builds.

The mobile layout is a real app shell, not a narrowed website: bottom tab bar
with a raised capture FAB, live `getUserMedia` camera with framing guides, and
a review-then-commit capture flow.

A native Jetpack Compose port is deferred: this machine has no Android SDK,
Gradle, or Studio, so Kotlin sources could not be compiled or run. The tokens in
`globals.css` map 1:1 onto a Material 3 `ColorScheme` when that happens.
