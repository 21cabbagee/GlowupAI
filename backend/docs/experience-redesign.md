# SkinProof consumer experience redesign

Status: implementation specification  
Audience: product, design, frontend, backend, QA  
Primary surface: mobile web/PWA at `/`  
Responsive surface: desktop web using the same state machine and copy  
Scope: onboarding through daily use; this document does not change source code.

## 1. Product decision

SkinProof should feel like a private, calm ritual that happens one intentional
screen at a time. The interaction grammar may borrow from Bumble's pacing—large
visual focus, one decision per screen, direct primary action, directional
transitions, and a strong personal profile—but it must not copy Bumble's brand,
colors, language, or swipe-to-match metaphor.

The current `skinproof/static/index.html` is the opposite of that experience:

- it opens with a persistent desktop-style tab bar (`Overview`, `Capture`,
  `Routine`, `Insights`, `Discover`, `Account`);
- the first screen asks for an optional skin type before establishing identity
  or value;
- profile creation immediately sends the user to `Account`;
- consent is presented as an account control instead of a meaningful step in
  the journey;
- the first screen exposes stats, Premium, experiments, commerce, audit, and
  three-year roadmap content before the user has a baseline;
- empty states are scattered across panels instead of becoming the next action;
- `Discover` is a top-level destination even though its API requires Premium and
  a minimum cohort sample;
- the current page is a single large HTML document with all screens in the DOM,
  so it behaves like a dashboard with hidden sections rather than a guided
  product.

The redesign has two modes:

1. **Journey mode**: one screen at a time, no application tabs, no dashboard
   density, and no unrelated choices.
2. **Daily mode**: a personalized home feed with five destinations revealed
   only after the first accepted baseline capture.

The user should know exactly where they are, why the step exists, and what one
action moves them forward.

## 2. Experience principles

1. **One decision per screen.** A screen can contain supporting detail, but it
   gets one dominant question and one primary CTA.
2. **Identity before instrumentation.** The user creates a person-shaped
   profile before being asked for measurements, consent, products, or metrics.
3. **Consent before camera.** Facial-data consent is a dedicated, plain-language
   decision immediately before capture readiness. It is never hidden in Account.
4. **Progressive disclosure.** Advanced metrics, Premium, experiments, cohort
   context, commerce, exports, and audit appear only after the user reaches the
   moment where they are useful.
5. **The next action is always visible.** Every empty, loading, and error state
   has a recovery action or a clearly stated reason to wait.
6. **Evidence over performance.** The interface never implies that a single
   image diagnoses a condition or proves a product works.
7. **Personal profile over generic dashboard.** The home surface uses the user's
   name, chosen focus, goal, routine state, capture cadence, and evidence state.
8. **Cinematic, not decorative.** Motion creates a sense of continuity between
   steps; it does not add bouncing cards, confetti, gradients, or AI spectacle.

## 3. State machine and navigation rules

Persist the following client state in the authenticated/session store. Local
storage may hold only an anonymous draft and the non-sensitive user id; it must
not hold raw images, consent evidence, or API credentials.

```text
new
  -> profile_created
  -> goals_selected
  -> baseline_context_saved
  -> routine_setup_complete | routine_setup_skipped
  -> consent_pending
  -> consent_active
  -> capture_ready
  -> baseline_processing
  -> baseline_saved
  -> daily_home
```

Guard rules:

- `new`: show Welcome only.
- `profile_created` through `consent_pending`: resume at the first incomplete
  onboarding screen. Do not show daily navigation.
- `consent_declined`: show a respectful privacy state with a route back to
  Account/consent explanation; do not show camera controls.
- `consent_active` without a capture: show Capture Readiness as the next step;
  the Home route may show a limited setup summary but not a fake dashboard.
- `baseline_processing`: show a blocking progress state for the active capture;
  prevent duplicate submits.
- `baseline_saved`: show the result once, then reveal Daily Home.
- `daily_home`: allow bottom navigation on mobile and a left rail on desktop.

Browser back, Android back, and the in-app back button must move to the previous
journey screen without discarding saved data. A step already committed to the
API is revisited in edit mode, not recreated. Refresh resumes from the last
server-confirmed checkpoint.

## 4. Exact first-run screen order

The numbered sequence below is normative. A user may skip only the steps marked
optional. The `Continue` button is disabled until the required choice for the
screen is valid. Do not expose the application nav until screen 12 is complete.

### Screen 0 — Welcome

**Purpose:** establish tone and invite the first commitment.

**Visual:** warm off-white background, full-height portrait composition, one
abstract measurement frame or live-light illustration, SkinProof wordmark, no
charts and no product grid.

**Copy:**

- Eyebrow: `SKINPROOF`
- Heading: `Know your skin over time.`
- Body: `A private record of what changes, what stays steady, and what your routine is actually doing.`
- Primary CTA: `Start my profile`
- Secondary text action: `I already have a profile`
- Footer: `Cosmetic appearance tracking. Not medical diagnosis.`

**Behavior:** primary CTA begins the new-user journey. Existing-profile action
opens the restore/sign-in adapter; until real authentication exists, it can ask
for a local profile id and must not pretend recovery succeeded.

### Screen 1 — The promise

**Purpose:** explain the product in three beats without overwhelming the user.

**Visual:** three sequential cards that appear within the same screen as the
user taps, not three dashboard panels.

**Copy:**

- Heading: `This is your starting line.`
- Beat 1: `Capture the same way each time.`
- Beat 2: `See patterns in your own history.`
- Beat 3: `Change one thing before judging it.`
- Primary CTA: `Make it mine`
- Secondary: `How privacy works`

The privacy action opens a bottom sheet with the short explanation and a
`Continue` action. It does not open Account or interrupt the journey.

### Screen 2 — Profile identity

**Purpose:** create a person, not just a database row.

**Copy:**

- Heading: `What should we call you?`
- Body: `This space is about one person: you.`
- Field label: `Your name`
- Placeholder: `First name or nickname`
- Supporting text: `Only used to personalize your private space.`
- Primary CTA: `Continue`

**Interaction:** one text field, autofocus on entry, submit on keyboard. Show a
small avatar monogram or selected color after entry. No skin type question here.

**Required persistence:** the current `POST /api/users` accepts only
`skin_type`. The implementation must add a profile completion contract that
persists `display_name` (and, if desired, an avatar color) before this screen is
considered complete. Do not silently keep the user's name only in local storage.

### Screen 3 — Concerns and goals

**Purpose:** give the home feed a personal reason to exist.

**Copy:**

- Heading: `What would feel like progress?`
- Body: `Choose up to three. We will use them to shape your check-ins.`
- Skin choices: `Breakouts`, `Redness`, `Uneven tone`, `Dark spots`, `Texture`, `Dryness`, `Oil balance`, `I’m not sure yet`
- Optional field label: `Anything else?`
- Placeholder: `A sentence is enough.`
- Primary CTA: `Save my goals`

**Interaction:** multi-select chips with a visible `2 of 3` counter. Selecting
`I’m not sure yet` clears other choices and makes the copy reassuring rather
than diagnostic. The free-text field is optional and collapsible behind
`Add a note`.

**Required persistence:** `appearance_profiles.goal` currently stores one
string per vertical. The implementation should persist a structured goal list
plus the optional note, while retaining a human-readable primary goal for
existing dashboard consumers.

### Screen 4 — Baseline context

**Purpose:** establish a useful comparison frame before the user takes a photo.

**Copy:**

- Heading: `What is your routine doing today?`
- Body: `There is no right answer. This helps us read your first week honestly.`
- Question 1: `Your current skin state`
- Choices: `I have a steady routine`, `I’m changing things`, `I’m starting fresh`, `I’m not sure`
- Question 2, optional: `How long has this been your routine?`
- Choices: `Less than 2 weeks`, `2–8 weeks`, `More than 8 weeks`, `Not sure`
- Primary CTA: `Continue`

Do not ask for ingredient lists or product details on this screen. This is
context, not a setup form. Store it as onboarding context attached to the
selected appearance profile.

### Screen 5 — Routine setup

**Purpose:** make the first comparison meaningful while keeping setup light.

**Copy:**

- Heading: `What do you use right now?`
- Body: `Add only what is part of your routine today. You can build the rest later.`
- Search field: `Search a product`
- Empty prompt: `No products yet? Add one by name.`
- Product action: `Add this product`
- Routine slot choices: `Morning`, `Evening`, `Both`, `Occasionally`
- Primary CTA with one item: `Save my routine`
- Secondary CTA: `I’m starting fresh`

**Interaction:** product search and add happen in a bottom sheet. Each saved
item becomes a compact row with name, slot, and `Edit`/`Remove`; no ingredient
analysis or Premium upsell appears here. The user may save zero items through
`I’m starting fresh`.

**Current API mapping:**

- Search uses `GET /api/products/search?q=...`.
- Product creation uses `POST /api/products`.
- Routine changes use `POST /api/routine-events` with `action: "start"` and
  the selected `slot`, `frequency`, `dose`, and optional `experiment_id`.

The current product route is global and the routine route requires a product
id. Before shipping this flow, add a user-scoped routine aggregate or a
transactional onboarding endpoint so a partially saved routine cannot leave
orphaned products/events. The UI must show the save result only after the API
confirms it.

### Screen 6 — Explicit facial-data consent

**Purpose:** make the sensitive-data decision clear and reversible.

**Visual:** no camera preview yet. Use a quiet illustration of a face outline,
not a real face photo.

**Copy:**

- Heading: `Your face is yours.`
- Body: `To compare your appearance over time, SkinProof stores the captures you choose to take and creates measurements from them.`
- Point 1: `You choose every capture.`
- Point 2: `We use it for cosmetic appearance tracking, not diagnosis.`
- Point 3: `You can export or delete your data from your profile.`
- Checkbox label: `I understand and consent to facial-data capture for my SkinProof profile.`
- Primary CTA: `I consent and continue`
- Secondary: `Not now`
- Link: `Read the full privacy policy`

**Interaction:** the primary CTA remains disabled until the checkbox is checked.
`Not now` moves to a consent-declined state with `Return to consent` and
`Leave SkinProof`; it must not unlock camera access. Submit
`POST /api/users/{user_id}/consent` with `facial_data: true` and the active
policy version. Audit success only after the response is confirmed.

### Screen 7 — Capture readiness

**Purpose:** teach the capture contract immediately before the camera.

**Copy:**

- Heading: `Make your first frame boring.`
- Body: `Boring is useful here: same light, same distance, same expression.`
- Instruction 1: `Face a window or soft light.`
- Instruction 2: `Keep your face neutral and centered.`
- Instruction 3: `Hold the phone 30–80 cm away.`
- Instruction 4: `Remove filters and avoid backlight.`
- Primary CTA: `Open camera`
- Secondary: `Use a photo from this device`
- Footer: `We will show you if the frame is not comparable.`

**Interaction:** request browser camera permission only after `Open camera`.
Permission denial becomes a recoverable state with `Try camera again` and
`Choose a photo`. Do not ask for notification permission here.

### Screen 8 — First baseline capture

**Purpose:** complete the first valuable action with immediate quality feedback.

**Visual:** full-screen camera/preview, face-position guide, large shutter, no
navigation. The shutter is disabled until a local preflight detects a usable
frame. The preview never leaves the device before the user submits it.

**Copy:**

- Heading above shutter: `Find your light.`
- Quality states: `Move into the frame`, `More light`, `Hold still`, `Looks good`
- Shutter action: `Capture baseline`
- Retake action: `Retake`
- Submit action after preview: `Use this frame`

**API:** submit `POST /api/captures` with the user id, base64 image,
`is_baseline: true`, device metadata, and quality data. The server
remains authoritative for pose, brightness, sharpness, and acceptance.

**Interaction:** once submitted, transition to Processing and disable all
capture actions. Never submit a second baseline while a request is in flight.

### Screen 9 — Processing

**Purpose:** make server work feel deliberate and prevent duplicate actions.

**Copy:**

- Heading: `Reading your starting line…`
- Body: `Checking the frame, measuring the signal, and saving your private history.`
- Progress labels: `Frame accepted` → `Measurement created` → `History ready`

Use an indeterminate progress treatment; do not show fake percentage values.
If the backend returns a structured quality failure, return to the captured frame
with the exact correction (`More light`, `Hold still`, or `Center your face`).

### Screen 10 — Baseline result

**Purpose:** reward completion without over-interpreting one image.

**Copy:**

- Eyebrow: `YOUR STARTING LINE`
- Heading: `Your baseline is saved, [name].`
- Body: `This is a reference point, not a verdict. The useful part comes from comparable captures over time.`
- Metric label: `Current signal`
- Confidence label: `Capture confidence`
- Cadence line: `Your next useful window is in 3–7 days.`
- Primary CTA: `See my space`
- Secondary: `Take another look`

Show only the skin metric summary and confidence. Put raw model
version, noise floors, and full metric fields behind `View measurement details`.
The existing capture response, `GET /capture-guide`, and `GET /dashboard` can
populate this screen.

### Screen 11 — First Home reveal

**Purpose:** transition from setup into a daily relationship with the product.

**Copy:**

- Greeting: `Good morning, [name].`
- Subheading: `Skin is ready for its first check-in.`
- Primary card title: `Your next best step`
- With no routine: `Keep your routine simple and return when your next window opens.`
- With routine: `Keep your routine steady. Consistency makes the comparison useful.`
- CTA: `View today’s plan`

Reveal the home feed with a single upward fade/slide. Do not show a tour modal.
Use inline callouts on the first visit to introduce the Capture and You
destinations; each callout can be dismissed and must never block the feed.

## 5. Transition and motion specification

- Journey screens occupy the same viewport and use a horizontal slide. Forward
  navigation enters from the right; back navigation enters from the left.
- Duration: 180–240 ms, ease-out; the next screen is mounted before the
  outgoing screen leaves so the layout never flashes white.
- When a choice is selected, use a 100 ms scale/outline confirmation and move
  automatically only when the choice is a binary confirmation. Multi-select
  screens always wait for `Continue`.
- Preserve the selected card's title as a shared element into the next screen's
  eyebrow where practical; do not morph large images into unrelated content.
- Product add is a bottom sheet with a spring entrance and a scrim. The sheet
  closes on successful save, explicit close, or Escape; tapping the scrim must
  not discard unsaved text without confirmation.
- Capture preview to processing uses a crossfade with the captured thumbnail;
  processing to result uses a vertical reveal.
- The in-app back action is always top-left during onboarding. A small progress
  indicator is top-right: `Step 3 of 9` for the main journey, with no progress
  count on the camera or processing screen.
- Support touch swipe-back only when the current screen has no unsaved input.
- Respect `prefers-reduced-motion`: replace slides with a 100 ms opacity change,
  remove spring effects, and keep all state changes explicit.
- Keep primary CTAs above the mobile safe-area inset and at least 48 px high.

## 6. Loading, empty, error, and recovery states

### Global boot

Show a quiet branded loading screen with `Opening your private space…`. After
1.5 seconds, show `Still loading` and `Try again`; never leave a blank page.
Resolve the profile before rendering daily navigation.

### No profile

Copy: `Nothing is set up yet.`  
CTA: `Start my profile`

This is the only valid empty state for a new install. Do not render dashboard
stats with zeroes before onboarding is complete.

### Profile created, consent pending

Copy: `Your profile is ready. One privacy decision unlocks your first capture.`  
CTA: `Review consent`

### No baseline

Copy: `Your history starts with one comparable frame.`  
CTA: `Take baseline`

### No routine

Copy: `Starting fresh is a valid routine.`  
CTA: `Keep it simple`
Secondary: `Add a product`

### Capture upload

Show a disabled shutter and `Saving your frame…`. If the connection drops,
retain the image in an encrypted in-memory queue only; offer `Retry upload` or
`Discard frame`. Do not silently retry indefinitely.

### Capture rejected

Use the server's structured quality payload. Show one correction at a time:

- `This frame is too dark. Face a window and try again.`
- `Hold the phone steadier.`
- `Move into the guide.`
- `We could not find a usable face frame. Try again or choose another photo.`

Buttons: `Retake`, `Choose another photo`, `Why this matters`.

### Network/API failure

Copy: `We could not save that yet. Your progress before this step is safe.`  
Actions: `Try again`, `Go back`.

Preserve form input, selected goals, and routine draft. Never claim a capture,
consent, or routine event succeeded before the API response.

### Premium locked state

Do not show a wall or a pricing card during onboarding. In daily mode, use a
compact inline unlock state:

- `See what your routine changed`
- `Premium adds product experiments, ingredient context, and history Q&A.`
- CTA: `Explore Premium`

The free home feed must remain useful: capture cadence, baseline, history, and
basic measurement are not locked.

### Q&A safety state

Call `POST /api/triage` before displaying the answer. For dermatology-review
scope, show the safety message prominently and do not send the question to the
cosmetic insight model. For cosmetic questions, show citations as expandable
dates/measurements below the answer.

### Delete/export states

- Export: `Preparing your private copy…` → download success or retry error.
- Delete: require a typed confirmation phrase, then show a final irreversible
  warning. After success, clear the local user id and return to Welcome.

## 7. Post-onboarding information architecture

### Mobile navigation

Reveal a fixed bottom navigation only after `daily_home`:

1. `Home` — today's next action and personal evidence.
2. `Routine` — products, active changes, experiments, and timeline.
3. `Capture` — central raised action, opens the guided capture flow.
4. `Insights` — trends, verdicts, Q&A, ingredient explanations, and Discover.
5. `You` — profile, focus areas, consent, privacy, Premium, export, delete, and audit.

Use labels and icons; never icon-only. The central Capture action is visually
dominant but remains a normal keyboard/focus target.

### Desktop navigation

Use a compact left rail after onboarding with the same five destinations. The
rail may expand to show labels, but must not reintroduce the old six-tab header.
On desktop, content max width is 1120–1240 px; onboarding remains a focused
single column of 440–560 px so it still feels like a guided product.

### Home feed order

The Home screen is a vertically ordered feed, not a grid of equal cards:

1. **Personal header** — avatar/monogram, `Good morning, [name]`, and a
   small `You` entry.
2. **Next best action** — one card determined by state:
   baseline, capture due, routine consistency, review verdict, or wait.
3. **Goal pulse** — the selected goal in plain language with current evidence
   status (`Building history`, `Evidence forming`, `Ready to review`).
4. **Latest capture** — date, quality, confidence, and selected metrics; never
   imply improvement from a single data point.
5. **Trend** — a small history visualization only after two accepted captures;
   use `Need one more comparable capture` before then.
6. **Routine pulse** — today's active products and the last recorded change.
7. **Evidence timeline** — collapsible chronological events and captures.
8. **Privacy footer** — concise link to `You` controls, not a large compliance
   panel.

### Routine

Primary route: `/routine` in the client shell.

- Header: `Keep the variables clear.`
- First content: active routine and `Log a change`.
- Product detail: product name, category, start/change/stop event, slot,
  frequency, stabilization days, ingredient analysis if Premium.
- Experiment detail: hypothesis, target metric, stabilization window, event
  count, captures, status, and eventual verdict.
- Empty copy: `Your routine is a blank page. Add the one thing you want to observe.`
- Do not present a four-state "daily tick" control: the backend accepts only
  `start`, `stop`, and `change` for routine events.

### Capture

Always opens directly into the guided capture flow. The screen remembers
the last guide state but requires a fresh quality check.

### Insights

Use a segmented internal switcher only here, because these are related views:

- `Trends` — metrics and measurement history.
- `Verdicts` — product evidence labels and evidence windows.
- `Ask` — Premium grounded Q&A.
- `Discover` — Premium cohort context and affiliate-disclosed offers.

The switcher must be a local context control, not global application tabs. The
current `/dashboard`, `/history`, `/qna`, `/discover`, and commerce routes map
directly to these views.

### You

Top of screen is a real profile card:

- avatar/monogram and display name;
- selected focus and `Edit focus`;
- goals in plain language;
- baseline date and capture streak;
- current plan.

Below it, use list rows in this order: `Focus areas`, `Routine preferences`,
`Consent & privacy`, `Premium`, `Export my data`, `Delete my account`, `Audit
history`. Keep raw model versions and reprocessing under an advanced disclosure
inside `Privacy & data`, not beside the user's identity.

## 8. API integration map

### Existing routes to reuse

| Experience need | Existing route(s) | UI use |
|---|---|---|
| Create profile row | `POST /api/users` | Create the server identity at Screen 2; current payload only has `skin_type`. |
| Load profile | `GET /api/users/{user_id}/profile` | Resume journey, populate You, plan, consent. |
| Consent | `POST /api/users/{user_id}/consent` | Screen 7; `facial_data: true` only after checkbox. |
| Product search/add | `GET /api/products/search`, `POST /api/products` | Routine setup sheet and product detail. |
| Routine changes | `POST /api/routine-events` | Save start/stop/change events. |
| Capture guidance | `GET /api/users/{user_id}/capture-guide` | Readiness, home next action, cadence. |
| Capture processing | `POST /api/captures` | Baseline and future captures; include experiment id. |
| Home/history | `GET /api/users/{user_id}/dashboard`, `/history`, `/engagement`, `/analytics` | Feed, trends, streak, activation. |
| Experiments | `POST /api/experiments`, `GET /api/users/{user_id}/experiments`, detail/status routes | Premium routine experiments. |
| Q&A | `POST/GET /api/users/{user_id}/qna` | Ask and history; run triage first. |
| Discover/commerce | `/discover`, `/commerce/offers`, offer click | Secondary Insights views, Premium only. |
| Account controls | subscription, export, delete, labels, reprocess, audit | You and advanced privacy controls. |
| Safety | `POST /api/triage` | Preflight cosmetic vs dermatology-review handling. |

### Required API additions before the redesign is considered complete

The frontend must not fake these values in local storage:

1. `PATCH /api/users/{user_id}/profile`
   - `display_name`
   - `avatar_color` or avatar seed
   - `skin_type` (optional)
   - `onboarding_state`
   - `onboarding_completed_at`
2. `PUT /api/users/{user_id}/appearance-profile`
   - `enabled`
   - `primary_goal`
   - `goals[]`
   - `goal_note`
   - `baseline_context`
3. `GET /api/users/{user_id}/onboarding`
   - server-confirmed current step;
   - completed steps;
   - draft-safe fields needed to resume.
4. `POST /api/users/{user_id}/routine/setup`
   - atomic list of product references and routine slots;
   - supports an explicit `starting_fresh: true` state;
   - returns saved routine items and created event ids.
5. `POST /api/users/{user_id}/engagement` should be used for journey events
   (`onboarding_started`, `goal_saved`, `consent_reviewed`, `baseline_started`,
   `baseline_completed`, `home_revealed`) so activation can be measured without
   scraping the UI.

The current `POST /api/users` may remain backward compatible. The redesign
should call it first, then call the profile/onboarding endpoints in order and
advance the UI only after each response succeeds.

## 9. Content and visual rules

- Use short, human sentences. One heading is usually 4–8 words.
- Never say `AI analysis`, `AI-powered`, or `diagnose` in onboarding.
- Prefer `measurement`, `signal`, `history`, `starting line`, and `evidence`.
- Never show a verdict label without its evidence state and time window.
- Never show zero-value stats as if they were meaningful progress.
- Use warm paper, deep ink, a restrained honey accent, and sage/rust semantic
  colors. Do not use purple-blue AI gradients, glassmorphism, or stock faces.
- Cards should have one job. Avoid nested card grids during onboarding.
- The primary action is always visually stronger than secondary actions, but
  destructive actions are separated and require confirmation.
- Use real user content as soon as available: name, focus, goals, last capture,
  routine state, and next window.

## 10. Instrumentation and success criteria

Record these events through the engagement API with a `flow_version`:

`onboarding_started`, `profile_created`, `goals_saved`,
`baseline_context_saved`, `routine_saved`, `routine_skipped`, `consent_reviewed`,
`consent_granted`, `camera_permission_granted`, `camera_permission_denied`,
`baseline_started`, `baseline_rejected`, `baseline_completed`, `home_revealed`,
`next_action_opened`, `capture_due_opened`, `profile_opened`, `premium_opened`.

Definition of done for the experience rebuild:

- A new user sees no global tabs during onboarding.
- A new user can create a named profile, choose a focus, choose goals, review
  routine context, make an explicit consent decision, and reach camera
  readiness in one linear journey.
- A baseline cannot be submitted without active consent.
- A failed API call never advances the journey or creates a false success state.
- Refresh/back/resume returns the user to the last server-confirmed step.
- A successful baseline ends with a personal Home feed, not Account or a blank
  dashboard.
- The Home feed has a single clear next action and useful empty states.
- Desktop and mobile use the same copy, data contract, and sequence; only shell
  layout changes.
- The old global tab bar and the pre-baseline Premium/Discover/audit panels are
  absent from the first-run path.
- Existing API behavior remains covered by the backend test suite, and the new
  journey is covered with mobile viewport, keyboard, reduced-motion, offline,
  permission-denied, rejected-capture, and delete-account scenarios.
