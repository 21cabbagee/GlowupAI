(() => {
  "use strict";

  const app = document.getElementById("app");
  const sheet = document.getElementById("sheet");
  const sheetBackdrop = document.getElementById("sheetBackdrop");
  const toasts = document.getElementById("toasts");
  const capturePicker = document.getElementById("capturePicker");

  const USER_KEY = "skinproof_user_id";
  const DRAFT_KEY = "skinproof_onboarding";
  const ROUTE_KEY = "skinproof_route";
  const DEMO_PROFILE_KEY = "skinproof_demo_profile_id";
  const DEMO_USERNAME = "skinproof-demo";
  const DEMO_ACCESS_CODE = "temporary-access-2026";
  const VERTICALS = ["skin", "hair", "scalp", "beard", "body"];
  const GOALS = {
    skin: ["Breakouts", "Redness", "Uneven tone", "Dark spots", "Texture", "Dryness", "Oil balance", "Not sure yet"],
    hair: ["Density", "Shedding", "Texture", "Shine", "Breakage", "Consistency", "Not sure yet"],
    scalp: ["Comfort", "Visible flakes", "Oil balance", "Redness", "Consistency", "Not sure yet"],
    beard: ["Coverage", "Texture", "Breakage", "Skin comfort", "Consistency", "Not sure yet"],
    body: ["Texture", "Tone", "Dryness", "Visible marks", "Consistency", "Not sure yet"],
  };

  const readDraft = () => {
    try { return JSON.parse(localStorage.getItem(DRAFT_KEY) || "{}"); }
    catch (_) { return {}; }
  };

  const state = {
    id: localStorage.getItem(USER_KEY),
    profile: null,
    dashboard: null,
    products: [],
    experiments: [],
    qna: [],
    discover: null,
    offers: [],
    route: localStorage.getItem(ROUTE_KEY) || "today",
    sheet: null,
    loading: false,
    captureFile: null,
    capturePreview: null,
    captureResult: null,
    draft: {
      step: 0,
      name: "",
      focus: "skin",
      goals: [],
      skinType: "",
      experience: "",
      productName: "",
      productCategory: "moisturizer",
      productSlot: "both",
      addProduct: false,
      consentChecked: false,
      processing: false,
      ...readDraft(),
    },
  };

  const esc = (value) => String(value ?? "").replace(/[&<>"']/g, (char) => ({
    "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;",
  }[char]));
  const title = (value) => String(value || "").replace(/_/g, " ").replace(/\b\w/g, (c) => c.toUpperCase());
  const initials = (name) => String(name || "SP").trim().split(/\s+/).slice(0, 2).map((part) => part[0]).join("").toUpperCase();
  const when = (value) => value ? new Date(value).toLocaleDateString(undefined, { month: "short", day: "numeric", year: "numeric" }) : "Not yet";
  const number = (value, digits = 2) => Number.isFinite(Number(value)) ? Number(value).toFixed(digits) : ":";
  const premium = () => state.profile?.entitlement?.plan === "premium" && state.profile?.entitlement?.status === "active";
  const experience = () => state.profile?.experience_profile || {};
  const focus = () => experience().focus_vertical || state.draft.focus || "skin";
  const saveDraft = () => localStorage.setItem(DRAFT_KEY, JSON.stringify(state.draft));

  const icon = (name) => {
    const paths = {
      today: '<path d="M3 11.5 12 4l9 7.5"/><path d="M5.5 10.5V21h13V10.5M9 21v-6h6v6"/>',
      journey: '<path d="M4 19V9m5 10V5m5 14v-7m5 7V3"/>',
      capture: '<rect x="3" y="5" width="18" height="15" rx="3"/><path d="m8 5 1.5-2h5L16 5"/><circle cx="12" cy="12.5" r="4"/>',
      you: '<circle cx="12" cy="8" r="4"/><path d="M4.5 21a7.5 7.5 0 0 1 15 0"/>',
      coach: '<path d="M4 5h16v12H8l-4 4V5Z"/>',
      routine: '<path d="M7 3h10v4H7zM6 7h12v14H6z"/><path d="M9 11h6m-6 4h6"/>',
      arrow: '<path d="m9 18 6-6-6-6"/>',
      back: '<path d="m15 18-6-6 6-6"/>',
      close: '<path d="m6 6 12 12M18 6 6 18"/>',
      status: '<path d="M5 12h14"/>',
      lock: '<rect x="5" y="10" width="14" height="11" rx="2"/><path d="M8 10V7a4 4 0 0 1 8 0v3"/>',
    };
    return `<svg aria-hidden="true" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">${paths[name] || paths.coach}</svg>`;
  };

  async function api(path, options = {}) {
    const request = { ...options, headers: { ...(options.headers || {}) } };
    if (request.body && !(request.body instanceof FormData)) request.headers["Content-Type"] = "application/json";
    const response = await fetch(path, request);
    if (response.status === 204) return null;
    const payload = await response.json().catch(() => ({}));
    if (!response.ok) {
      const detail = payload.detail?.message || payload.detail;
      const error = new Error(typeof detail === "string" ? detail : "We could not save that yet.");
      error.status = response.status;
      error.detail = detail;
      throw error;
    }
    return payload;
  }

  function toast(message, type = "") {
    const node = document.createElement("div");
    node.className = `toast ${type ? `is-${type}` : ""}`;
    node.textContent = message;
    toasts.appendChild(node);
    setTimeout(() => node.remove(), 4200);
  }

  function setStep(step) {
    state.draft.step = Math.max(0, Math.min(13, step));
    saveDraft();
    renderOnboarding();
    window.scrollTo({ top: 0, behavior: "smooth" });
  }

  function progress(step) {
    if (step < 2 || step > 10) return "";
    const current = step - 1;
    return `<div class="story-progress" style="--story-count:9" aria-label="Onboarding progress">${Array.from({ length: 9 }, (_, i) => `<span class="${i < current ? "is-complete" : i === current ? "is-current" : ""}"></span>`).join("")}</div>`;
  }

  function onboardingFrame(content, options = {}) {
    const step = state.draft.step;
    const canBack = step > 0 && !state.draft.processing;
    app.innerHTML = `<main class="onboarding-shell"><div class="onboarding-inner">
      <div class="onboarding-brand">
        <span class="wordmark">Skin<em>Proof</em></span>
        ${canBack ? `<button class="text-button" data-action="onboarding-back">${icon("back")} Back</button>` : `<span class="small">Private by design</span>`}
      </div>
      ${progress(step)}
      <section class="onboarding-card ${options.cardClass || ""}">${content}</section>
      <p class="caption text-center" style="margin:18px auto 0">Cosmetic appearance tracking. Never medical diagnosis.</p>
    </div></main>`;
    const autofocus = app.querySelector("[autofocus]");
    if (autofocus) setTimeout(() => autofocus.focus(), 80);
  }

  const choice = (value, label, copy, selected, action, multi = false) => `<button class="choice-card ${selected ? "is-selected" : ""}" aria-pressed="${selected}" data-action="${action}" data-value="${esc(value)}">
    <span class="choice-title">${esc(label)}</span>${copy ? `<span class="choice-copy">${esc(copy)}</span>` : ""}<span class="choice-mark">${selected ? icon("status") : ""}</span>
  </button>`;

  function renderOnboarding() {
    const d = state.draft;
    const name = d.name || experience().display_name || "you";
    switch (d.step) {
      case 0:
        onboardingFrame(`<div class="journey-intro"><div><span class="eyebrow">SKINPROOF</span><h1>Know yourself over time.</h1><p class="lead">A private record of what changes, what stays steady, and what your routine is actually doing.</p><div class="onboarding-actions"><button class="primary" data-action="next-step">Start my profile ${icon("arrow")}</button><button class="text-button" data-action="restore-profile">I already have a profile</button></div></div><div class="profile-mark" aria-hidden="true">SP</div></div>`);
        break;
      case 1:
        onboardingFrame(`<span class="eyebrow">THE PROMISE</span><h1>This is your starting line.</h1><p class="lead">No scores against strangers. No judgment from one photo.</p><div style="margin-top:28px">
          <div class="journey-card"><span class="journey-index">1</span><div class="journey-copy"><strong>Capture the same way.</strong><p>Comparable frames make patterns visible.</p></div></div>
          <div class="journey-card"><span class="journey-index">2</span><div class="journey-copy"><strong>See your own history.</strong><p>Your baseline is the only comparison that matters.</p></div></div>
          <div class="journey-card"><span class="journey-index">3</span><div class="journey-copy"><strong>Change one thing.</strong><p>Give products enough time before judging them.</p></div></div>
        </div><div class="onboarding-actions"><button class="secondary" data-action="privacy-sheet">How privacy works</button><button class="primary" data-action="next-step">Make it mine ${icon("arrow")}</button></div>`);
        break;
      case 2:
        onboardingFrame(`<span class="story-step">Your profile</span><h1>What should we call you?</h1><p class="lead">This space is about one person: you.</p><div class="field" style="margin-top:30px"><label for="profileName">First name or nickname</label><input id="profileName" maxlength="80" autocomplete="given-name" placeholder="Your name" value="${esc(d.name)}" autofocus><p class="caption">Only used to personalize your private space.</p></div><div class="onboarding-actions"><span></span><button class="primary" data-action="create-profile">Create my profile ${icon("arrow")}</button></div>`);
        break;
      case 3:
        onboardingFrame(`<div class="profile-mark">${esc(initials(name))}</div><span class="eyebrow">PROFILE CREATED</span><h1>There you are, ${esc(name)}.</h1><p class="lead">Your private space is ready. Now we shape it around what matters to you: one choice at a time.</p><div class="onboarding-actions"><span></span><button class="primary" data-action="next-step">Shape my space ${icon("arrow")}</button></div>`, { cardClass: "has-signal" });
        break;
      case 4:
        onboardingFrame(`<span class="story-step">Choose a focus</span><h1>Where should we start?</h1><p class="lead">Pick one focus. The rest will still be here when you want them.</p><div class="choice-grid">${VERTICALS.map((v) => choice(v, title(v), v === "skin" ? "Tone, texture, redness and blemish patterns" : `Build a comparable ${v} history`, d.focus === v, "select-focus")).join("")}</div><div class="onboarding-actions"><span class="caption">You can change this later.</span><button class="primary" data-action="save-focus">Continue with ${esc(title(d.focus))} ${icon("arrow")}</button></div>`);
        break;
      case 5: {
        const goals = GOALS[d.focus] || GOALS.skin;
        onboardingFrame(`<span class="story-step">Make it personal</span><h1>What would feel like progress?</h1><p class="lead">Choose up to three. We will shape your check-ins around them.</p><div class="choice-grid">${goals.map((goal) => choice(goal, goal, "", d.goals.includes(goal), "toggle-goal", true)).join("")}</div><div class="onboarding-actions"><span class="caption">${d.goals.length} of 3 selected</span><button class="primary" data-action="save-goals" ${d.goals.length ? "" : "disabled"}>Save my goals ${icon("arrow")}</button></div>`);
        break;
      }
      case 6:
        onboardingFrame(`<span class="story-step">Your starting point</span><h1>How does your ${esc(d.focus)} usually feel?</h1><p class="lead">There is no perfect label. Pick the closest fit:or stay unsure.</p><div class="choice-grid">${["dry", "balanced", "oily", "combination", "sensitive", "not sure"].map((v) => choice(v, title(v), "", d.skinType === v, "select-skin-type")).join("")}</div><div class="onboarding-actions"><button class="text-button" data-action="skip-skin-type">Skip this</button><button class="primary" data-action="save-skin-type" ${d.skinType ? "" : "disabled"}>Continue ${icon("arrow")}</button></div>`);
        break;
      case 7:
        onboardingFrame(`<span class="story-step">Baseline context</span><h1>What is your routine doing today?</h1><p class="lead">This helps us read your first week honestly.</p><div class="choice-grid">${[
          ["steady", "It is steady", "The same products for a while"], ["changing", "I am changing things", "One or more products are new"], ["fresh", "I am starting fresh", "There is little or no routine yet"], ["unsure", "I am not sure", "We will learn as we go"],
        ].map(([v, l, c]) => choice(v, l, c, d.experience === v, "select-experience")).join("")}</div><div class="onboarding-actions"><span></span><button class="primary" data-action="save-experience" ${d.experience ? "" : "disabled"}>Continue ${icon("arrow")}</button></div>`);
        break;
      case 8:
        onboardingFrame(`<span class="story-step">Your routine</span><h1>${d.addProduct ? "Add one anchor product." : "Bring your routine with you?"}</h1><p class="lead">${d.addProduct ? "Start with the product you use most consistently." : "Starting fresh is completely valid. You can add products whenever you are ready."}</p>${d.addProduct ? `<div style="margin-top:26px"><div class="field"><label for="firstProduct">Product name</label><input id="firstProduct" maxlength="160" placeholder="Daily moisturizer" value="${esc(d.productName)}" autofocus></div><div class="field"><label for="firstCategory">Category</label><select id="firstCategory"><option value="cleanser">Cleanser</option><option value="moisturizer" ${d.productCategory === "moisturizer" ? "selected" : ""}>Moisturizer</option><option value="serum">Serum</option><option value="sunscreen">Sunscreen</option><option value="retinoid">Retinoid</option><option value="other">Other</option></select></div><div class="field"><label for="firstSlot">When do you use it?</label><select id="firstSlot"><option value="morning">Morning</option><option value="evening">Evening</option><option value="both" ${d.productSlot === "both" ? "selected" : ""}>Morning and evening</option><option value="occasionally">Occasionally</option></select></div></div><div class="onboarding-actions"><button class="text-button" data-action="skip-routine">Start fresh instead</button><button class="primary" data-action="save-first-product">Save my routine ${icon("arrow")}</button></div>` : `<div class="choice-grid"><button class="choice-card" data-action="add-first-product"><span class="choice-title">Add my anchor product</span><span class="choice-copy">One product is enough to begin.</span><span class="choice-mark">+</span></button><button class="choice-card" data-action="skip-routine"><span class="choice-title">I am starting fresh</span><span class="choice-copy">No products, no pressure.</span><span class="choice-mark">${icon("arrow")}</span></button></div>`}`);
        break;
      case 9:
        onboardingFrame(`<span class="story-step">A clear privacy choice</span><div class="profile-mark" aria-hidden="true">${icon("lock")}</div><h1>Your face is yours.</h1><p class="lead">To compare your appearance over time, SkinProof stores only the captures you choose and creates measurements from them.</p><div style="margin-top:24px"><div class="journey-card"><span class="journey-index">1</span><div class="journey-copy"><strong>You choose every capture.</strong><p>Camera access never runs in the background.</p></div></div><div class="journey-card"><span class="journey-index">2</span><div class="journey-copy"><strong>Cosmetic tracking only.</strong><p>SkinProof does not diagnose or rule out conditions.</p></div></div><div class="journey-card"><span class="journey-index">3</span><div class="journey-copy"><strong>You stay in control.</strong><p>Export or delete your profile and history.</p></div></div></div><label class="check-row" style="margin-top:24px"><input id="consentCheck" type="checkbox" ${d.consentChecked ? "checked" : ""}><span>I understand and consent to facial-data capture for my SkinProof profile.</span></label><div class="onboarding-actions"><button class="text-button" data-action="decline-consent">Not now</button><button class="primary" data-action="grant-consent" ${d.consentChecked ? "" : "disabled"}>I consent and continue ${icon("arrow")}</button></div>`);
        break;
      case 10:
        onboardingFrame(`<span class="story-step">Capture readiness</span><h1>Make your first frame boring.</h1><p class="lead">Boring is useful here: same light, same distance, same expression.</p><div style="margin-top:26px"><div class="journey-card"><span class="journey-index">1</span><div class="journey-copy"><strong>Face soft, even light.</strong><p>A window in front of you works well.</p></div></div><div class="journey-card"><span class="journey-index">2</span><div class="journey-copy"><strong>Keep a neutral expression.</strong><p>Center your face and remove filters.</p></div></div><div class="journey-card"><span class="journey-index">3</span><div class="journey-copy"><strong>Hold 30 to 80 cm away.</strong><p>We will reject a frame that cannot be compared.</p></div></div></div><div class="onboarding-actions"><button class="secondary" data-action="choose-photo">Use a photo</button><button class="primary" data-action="open-camera">Open camera ${icon("capture")}</button></div>`);
        break;
      case 11:
        renderOnboardingCapture();
        break;
      case 12:
        renderBaselineResult();
        break;
      case 13:
        onboardingFrame(`<div class="profile-mark">${esc(initials(name))}</div><span class="eyebrow">YOUR SPACE IS READY</span><h1>Welcome home, ${esc(name)}.</h1><p class="lead">Your profile now has a focus, a reason, and a starting line. From here, SkinProof gives you one useful next step at a time.</p><div class="onboarding-actions"><span></span><button class="primary" data-action="enter-app">See my space ${icon("arrow")}</button></div>`, { cardClass: "has-signal" });
        break;
      default:
        setStep(0);
    }
  }

  function renderOnboardingCapture() {
    const d = state.draft;
    if (d.processing) {
      onboardingFrame(`<span class="eyebrow">CREATING YOUR STARTING LINE</span><h1>Reading the frame...</h1><p class="lead">Checking comparability, measuring the signal, and saving your private history.</p><div class="progress-line" style="margin-top:32px"><span style="--progress:74%"></span></div><div style="margin-top:24px"><div class="journey-card is-current"><span class="journey-index">${icon("status")}</span><div class="journey-copy"><strong>Frame received</strong><p>Server quality checks are running.</p></div><span class="journey-status">Working</span></div><div class="journey-card"><span class="journey-index">2</span><div class="journey-copy"><strong>Measurement created</strong><p>Your first comparable signal.</p></div></div><div class="journey-card"><span class="journey-index">3</span><div class="journey-copy"><strong>History ready</strong><p>Your next window will be scheduled.</p></div></div></div>`);
      return;
    }
    const preview = state.capturePreview;
    onboardingFrame(`<span class="story-step">Your baseline</span><h1>${preview ? "Use this frame?" : "Find your light."}</h1><p class="lead">${preview ? "Check the light and expression. The server makes the final quality decision." : "Choose a fresh, unfiltered photo with a centered, neutral face."}</p><div class="capture-frame ${preview ? "is-ready" : ""}" style="margin-top:26px">${preview ? `<img class="capture-preview" src="${esc(preview)}" alt="Selected baseline preview">` : `<div class="capture-placeholder">Center your face here</div>`}<div class="capture-status"><strong>${preview ? "Ready to check" : "Waiting for a frame"}</strong><span>${esc(title(state.draft.focus))}</span></div></div><div class="onboarding-actions">${preview ? `<button class="secondary" data-action="retake-photo">Retake</button><button class="primary" data-action="submit-baseline">Use this frame ${icon("arrow")}</button>` : `<span></span><button class="primary" data-action="choose-photo">Choose photo ${icon("capture")}</button>`}</div>`);
  }

  function renderBaselineResult() {
    const result = state.captureResult || {};
    const metric = result.metric || {};
    const confidence = Math.round(Number(metric.confidence || 0) * 100);
    onboardingFrame(`<span class="eyebrow">YOUR STARTING LINE</span><h1>Your baseline is saved, ${esc(state.draft.name || "you")}.</h1><p class="lead">This is a reference point, not a verdict. The useful part comes from comparable captures over time.</p><div class="metrics-grid" style="margin-top:28px"><div class="metric-card"><span class="metric-label">Capture confidence</span><strong class="metric-value">${confidence}%</strong><span class="metric-delta">Frame accepted</span></div><div class="metric-card"><span class="metric-label">Redness signal</span><strong class="metric-value">${number(metric.redness_score, 3)}</strong><span class="metric-delta">Baseline</span></div><div class="metric-card"><span class="metric-label">Texture signal</span><strong class="metric-value">${number(metric.texture_score, 1)}</strong><span class="metric-delta">Baseline</span></div><div class="metric-card"><span class="metric-label">Next useful window</span><strong class="metric-value" style="font-size:32px">3 to 7d</strong><span class="metric-delta">Keep things steady</span></div></div><div class="onboarding-actions"><button class="secondary" data-action="measurement-details">View measurement details</button><button class="primary" data-action="finish-onboarding">Continue ${icon("arrow")}</button></div>`);
  }

  async function loadWorkspace() {
    if (!state.id) return;
    const profile = await api(`/api/users/${state.id}/profile`);
    state.profile = profile;
    const selected = profile.experience_profile?.focus_vertical || "skin";
    const [dashboard, products] = await Promise.all([
      api(`/api/users/${state.id}/dashboard?vertical=${selected}`),
      api("/api/products/search?q="),
    ]);
    state.dashboard = dashboard;
    state.products = products;
    state.experiments = premium() ? await api(`/api/users/${state.id}/experiments`).catch(() => []) : [];
  }

  function navButton(route, label, iconName) {
    const active = state.route === route;
    return `<button class="nav-item ${active ? "active" : ""}" aria-current="${active ? "page" : "false"}" data-action="navigate" data-route="${route}"><span class="nav-icon">${icon(iconName)}</span>${label}</button>`;
  }

  function shell(content) {
    const name = experience().display_name || "My profile";
    app.innerHTML = `<div class="app-shell">
      <aside class="desktop-rail">
        <span class="wordmark">Skin<em>Proof</em></span>
        <span class="rail-label">Your space</span>
        <nav class="rail-nav" aria-label="Primary navigation">${navButton("today", "Today", "today")}${navButton("journey", "Journey", "journey")}${navButton("capture", "Capture", "capture")}${navButton("you", "You", "you")}</nav>
        <button class="rail-profile" data-action="navigate" data-route="you"><span class="avatar">${esc(initials(name))}</span><span><strong>${esc(name)}</strong><br><span class="caption">${esc(title(focus()))} profile</span></span></button>
      </aside>
      <div>
        <header class="topbar"><span class="wordmark">Skin<em>Proof</em></span><button class="icon-button" data-action="navigate" data-route="you" aria-label="Open profile">${esc(initials(name))}</button></header>
        <main class="app-main">${content}</main>
        <nav class="bottom-dock" aria-label="Primary navigation">${["today", "journey", "capture", "you"].map((route) => `<button class="dock-item ${state.route === route ? "active" : ""}" aria-current="${state.route === route ? "page" : "false"}" data-action="navigate" data-route="${route}"><span class="dock-icon">${icon(route === "today" ? "today" : route === "journey" ? "journey" : route === "capture" ? "capture" : "you")}</span>${title(route)}</button>`).join("")}</nav>
      </div>
    </div>`;
  }

  function renderWorkspace() {
    if (!state.profile || !state.dashboard) return renderLoadingWorkspace();
    if (state.route === "journey") return shell(renderJourney());
    if (state.route === "capture") return shell(renderCapture());
    if (state.route === "you") return shell(renderYou());
    if (state.route === "discover") return shell(renderDiscover());
    return shell(renderToday());
  }

  function renderLoadingWorkspace() {
    shell(`<section class="page-header"><div><span class="eyebrow">OPENING YOUR SPACE</span><h1>Bringing your story together...</h1></div></section><div class="metrics-grid">${Array.from({ length: 4 }, () => '<div class="metric-card skeleton"></div>').join("")}</div><div class="card skeleton" style="min-height:260px"></div>`);
  }

  function greeting() {
    const hour = new Date().getHours();
    return hour < 12 ? "Good morning" : hour < 18 ? "Good afternoon" : "Good evening";
  }

  function renderToday() {
    const d = state.dashboard;
    const name = experience().display_name || "there";
    const guide = d.engagement?.guide || {};
    const history = d.history || [];
    const latest = history[history.length - 1];
    const verdict = (d.verdicts || [])[0];
    const routine = d.routine_events || [];
    const due = ["due", "overdue", "baseline_needed"].includes(guide.state);
    return `<section class="page-header"><div><span class="eyebrow">${esc(title(focus()))} · YOUR PRIVATE SPACE</span><h1>${greeting()}, ${esc(name)}.</h1><p class="lead">${guide.message || "Your next useful step is ready when you are."}</p></div><button class="icon-button" data-action="open-sheet" data-sheet="coach" aria-label="Ask SkinProof">${icon("coach")}</button></section>
      <section class="card ${due ? "has-signal" : "has-signal"}"><span class="eyebrow">YOUR NEXT BEST STEP</span><h2>${due ? (history.length ? "Your check-in window is open." : "Start with one honest baseline.") : "Consistency is doing the work."}</h2><p>${due ? "Use the same light, distance, and expression so this frame can join your story." : `Your next useful window opens ${when(guide.next_window_start)}. Keep your routine steady until then.`}</p><div class="actions"><button class="primary" data-action="navigate" data-route="capture">${history.length ? "Take check-in" : "Take baseline"} ${icon("capture")}</button><button class="secondary" data-action="open-sheet" data-sheet="routine">Manage routine</button></div></section>
      <section><div class="section-heading"><div><span class="section-kicker">YOUR RHYTHM</span><h2>Small signals, over time.</h2></div></div><div class="metrics-grid">
        <div class="metric-card"><span class="metric-label">Comparable captures</span><strong class="metric-value">${d.engagement?.capture_count || 0}</strong><span class="metric-delta">Your history</span></div>
        <div class="metric-card"><span class="metric-label">Capture rhythm</span><strong class="metric-value">${d.engagement?.capture_streak || 0}</strong><span class="metric-delta">Streak</span></div>
        <div class="metric-card"><span class="metric-label">History span</span><strong class="metric-value">${d.analytics?.median_history_days || 0}d</strong><span class="metric-delta">Measured days</span></div>
        <div class="metric-card"><span class="metric-label">Latest confidence</span><strong class="metric-value">${latest ? Math.round(Number(latest.confidence) * 100) : 0}%</strong><span class="metric-delta">Frame quality</span></div>
      </div></section>
      <section class="insight-card"><header><div><span class="insight-label">WHAT THE EVIDENCE SAYS</span><h2>${verdict ? esc(verdict.product_name) : "Your first pattern needs time."}</h2></div>${verdict ? `<span class="tag ${verdict.label === "investigate" ? "alert" : verdict.label === "evidence_unclear" ? "gold" : "positive"}">${esc(title(verdict.label))}</span>` : `<span class="tag gold">Building</span>`}</header><div class="insight-body">${verdict ? esc(verdict.generated_text) : "One frame creates a starting line. Comparable captures and a steady routine create a story worth trusting."}</div><footer class="insight-footer"><span>${history.length} capture${history.length === 1 ? "" : "s"}</span><span>${routine.length} routine event${routine.length === 1 ? "" : "s"}</span><button class="text-button" data-action="open-sheet" data-sheet="coach">Ask your history ${icon("arrow")}</button></footer></section>
      <section class="grid"><div class="card c7"><div class="section-heading"><div><span class="section-kicker">YOUR RITUAL</span><h2>${routine.length ? "What you are holding steady." : "Starting fresh is valid."}</h2></div><button class="secondary" data-action="open-sheet" data-sheet="routine">Edit</button></div>${routine.length ? `<div class="timeline">${routine.slice(0, 5).map((event) => `<div class="event"><strong>${esc(event.product_name)}</strong><p class="small">${esc(title(event.action))} · ${esc(title(event.slot))} · ${when(event.timestamp)}</p></div>`).join("")}</div>` : `<div class="empty">No products yet. Your baseline can still begin today.</div>`}</div>
      <div class="card c5 has-signal"><span class="section-kicker">CONTEXT, LATER</span><h2>See what works for people like you.</h2><p>Discover opens cohort context without replacing your own evidence.</p><button class="secondary" data-action="navigate" data-route="discover">Open Discover ${icon("arrow")}</button></div></section>`;
  }

  function renderJourney() {
    const history = state.dashboard.history || [];
    const latest = history[history.length - 1];
    return `<section class="page-header"><div><span class="eyebrow">YOUR JOURNEY</span><h1>Proof, not memory.</h1><p class="lead">Every point below belongs to your own history.</p></div><button class="primary" data-action="navigate" data-route="capture">New capture ${icon("capture")}</button></section>
      <section class="card"><div class="section-heading"><div><span class="section-kicker">FOCUS</span><h2>${esc(title(focus()))}</h2></div></div><div class="actions">${VERTICALS.map((v) => `<button class="${focus() === v ? "primary" : "secondary"}" data-action="change-vertical" data-value="${v}">${title(v)}</button>`).join("")}</div></section>
      ${latest ? `<section><div class="section-heading"><div><span class="section-kicker">LATEST SIGNAL</span><h2>${when(latest.captured_at)}</h2></div><span class="tag positive">${Math.round(Number(latest.confidence) * 100)}% confidence</span></div><div class="metrics-grid"><div class="metric-card"><span class="metric-label">Redness</span><strong class="metric-value">${number(latest.redness_score, 3)}</strong></div><div class="metric-card"><span class="metric-label">Blemish proxy</span><strong class="metric-value">${number(latest.blemish_count, 0)}</strong></div><div class="metric-card"><span class="metric-label">Dark spots</span><strong class="metric-value">${number(latest.darkspot_area, 3)}</strong></div><div class="metric-card"><span class="metric-label">Texture</span><strong class="metric-value">${number(latest.texture_score, 1)}</strong></div></div></section>` : ""}
      <section><div class="section-heading"><div><span class="section-kicker">TIMELINE</span><h2>${history.length ? `${history.length} comparable moments` : "Your history starts here"}</h2></div></div>${history.length ? history.slice().reverse().map((item, index) => `<article class="journey-card ${index === 0 ? "is-current" : ""}"><span class="journey-index">${history.length - index}</span><div class="journey-copy"><strong>${when(item.captured_at)} ${item.is_baseline ? "· Baseline" : ""}</strong><p>Redness ${number(item.redness_score, 3)} · Texture ${number(item.texture_score, 1)} · ${Math.round(Number(item.confidence) * 100)}% confidence</p></div><span class="journey-status">${esc(item.model_version)}</span></article>`).join("") : `<div class="empty"><div><strong>No baseline yet.</strong><p>One comparable frame starts the whole story.</p><button class="primary" data-action="navigate" data-route="capture">Start baseline</button></div></div>`}</section>
      ${state.experiments.length ? `<section><div class="section-heading"><div><span class="section-kicker">EXPERIMENTS</span><h2>One change at a time.</h2></div><button class="secondary" data-action="open-sheet" data-sheet="routine">Manage</button></div>${state.experiments.map((item, index) => `<article class="journey-card"><span class="journey-index">${index + 1}</span><div class="journey-copy"><strong>${esc(item.name)}</strong><p>${esc(item.hypothesis || "Watching the evidence window")}</p></div><span class="tag ${item.status === "running" ? "positive" : "gold"}">${esc(item.status)}</span></article>`).join("")}</section>` : ""}`;
  }

  function renderCapture() {
    const consented = state.profile.user.consent_state === "active";
    const preview = state.capturePreview;
    if (!consented) return `<section class="page-header"><div><span class="eyebrow">CAPTURE</span><h1>One privacy choice first.</h1><p class="lead">Camera access stays locked until facial-data consent is active.</p></div></section><section class="card has-signal"><h2>Your face is yours.</h2><p>Review the same clear consent used during onboarding.</p><button class="primary" data-action="workspace-consent">Review and consent</button></section>`;
    return `<section class="page-header"><div><span class="eyebrow">GUIDED CAPTURE · ${esc(title(focus()))}</span><h1>${preview ? "Use this frame?" : "Make the next frame comparable."}</h1><p class="lead">${state.dashboard.engagement?.guide?.message || "Same light. Same distance. Neutral expression."}</p></div></section><section class="capture-stage"><div class="capture-frame ${preview ? "is-ready" : ""}">${preview ? `<img class="capture-preview" src="${esc(preview)}" alt="Capture preview">` : `<div class="capture-placeholder">Same light.<br>Same you.</div>`}<div class="capture-status"><strong>${preview ? "Ready for server checks" : "Center your face"}</strong><span>${esc(title(focus()))}</span></div></div><aside class="capture-checklist"><span class="section-kicker">COMPARABILITY CHECK</span><div class="capture-check is-ready"><span class="check-icon">${icon("status")}</span><div><strong>Soft, even light</strong><p class="small">Face a window, avoid backlight.</p></div></div><div class="capture-check is-ready"><span class="check-icon">${icon("status")}</span><div><strong>Neutral and centered</strong><p class="small">No filters or exaggerated expression.</p></div></div><div class="capture-check is-ready"><span class="check-icon">${icon("status")}</span><div><strong>30 to 80 cm away</strong><p class="small">The server checks the final frame.</p></div></div><div class="capture-tip">A rejected frame is useful feedback, not failure.</div><div class="actions">${preview ? `<button class="secondary" data-action="retake-photo">Retake</button><button class="primary" data-action="submit-workspace-capture">Analyze frame</button>` : `<button class="primary" data-action="open-camera">Open camera ${icon("capture")}</button><button class="secondary" data-action="choose-photo">Choose photo</button>`}</div></aside></section>`;
  }

  function renderYou() {
    const e = experience();
    const goals = e.goals || [];
    return `<section class="page-header"><div><span class="eyebrow">YOUR PROFILE</span><h1>This is your space.</h1><p class="lead">Identity, goals, privacy, and data:kept in one calm place.</p></div><button class="secondary" data-action="open-sheet" data-sheet="edit-profile">Edit profile</button></section>
      <section class="card has-signal"><div class="journey-intro"><div><div class="profile-mark">${esc(initials(e.display_name))}</div><span class="eyebrow">${esc(title(e.focus_vertical || "skin"))} PROFILE</span><h2>${esc(e.display_name || "Your profile")}</h2><p>${goals.length ? `Tracking ${goals.map((goal) => esc(goal.toLowerCase())).join(", ")}.` : "Ready for goals whenever you are."}</p></div><div class="aside-note"><strong>${state.dashboard.engagement?.capture_count || 0} accepted captures</strong><br>${state.dashboard.analytics?.median_history_days || 0} days of personal history</div></div></section>
      <section class="grid"><div class="card c6"><span class="section-kicker">MEMBERSHIP</span><h2>${premium() ? "Premium is active." : "Your history stays free."}</h2><p>${premium() ? "Experiments, product verdicts, Q&A, Discover, and long history are open." : "Unlock experiments, grounded Q&A, ingredient intelligence, and Discover when useful."}</p><div class="actions">${premium() ? `<button class="secondary" data-action="cancel-premium">Cancel Premium</button>` : `<button class="primary" data-action="upgrade">Explore Premium</button>`}</div></div>
      <div class="card c6"><span class="section-kicker">PRIVACY</span><h2>${state.profile.user.consent_state === "active" ? "Facial-data consent is active." : "Capture consent is pending."}</h2><p>You choose every capture. SkinProof is cosmetic tracking, never diagnosis.</p>${state.profile.user.consent_state !== "active" ? `<button class="primary" data-action="workspace-consent">Review consent</button>` : ""}</div></section>
      <section class="card"><div class="section-heading"><div><span class="section-kicker">YOUR DATA</span><h2>Owned by you.</h2></div></div><div class="grid"><div class="c6"><p>Download your profile, events, metrics, experiments, verdicts, and Q&A as JSON.</p><button class="secondary" data-action="export-data">Download my data</button></div><div class="c6"><p>Reprocess stored captures with a new measurement model when one is available.</p><button class="secondary" data-action="open-sheet" data-sheet="advanced">Advanced controls</button></div></div></section>
      <section class="card"><span class="section-kicker">DANGER ZONE</span><h2>Leave without loose ends.</h2><p>Deleting your profile removes database-held history and stored photos through the configured photo store.</p><button class="danger" data-action="delete-profile">Delete my profile</button></section>`;
  }

  function renderDiscover() {
    if (!premium()) return `<section class="page-header"><div><span class="eyebrow">DISCOVER</span><h1>Context, never a verdict.</h1><p class="lead">Cohort patterns can help with cold start. Your history remains the source of personal truth.</p></div><button class="text-button" data-action="navigate" data-route="today">${icon("back")} Today</button></section><section class="lock"><h2>Discover is a Premium layer.</h2><p>Recommendations appear only after a minimum consenting cohort exists. Paid placement never changes evidence.</p><button class="primary" data-action="upgrade">Unlock Discover</button></section>`;
    const recommendations = state.discover?.recommendations || [];
    return `<section class="page-header"><div><span class="eyebrow">DISCOVER</span><h1>Context, never substitution.</h1><p class="lead">Signals from sufficiently sized cohorts, always separate from your own verdicts.</p></div><button class="text-button" data-action="navigate" data-route="today">${icon("back")} Today</button></section><section><div class="section-heading"><div><span class="section-kicker">COHORT SIGNALS</span><h2>Patterns worth exploring.</h2></div><span class="tag positive">Minimum n=${state.discover?.minimum_cohort_size || 3}</span></div>${recommendations.length ? recommendations.map((item, index) => `<article class="journey-card"><span class="journey-index">${index + 1}</span><div class="journey-copy"><strong>${esc(item.name)}</strong><p>${esc(item.reason)}</p></div><span class="journey-status">${item.sample_size} people</span></article>`).join("") : `<div class="empty">No cohort has reached the privacy-preserving minimum yet.</div>`}</section><section><div class="section-heading"><div><span class="section-kicker">DISCLOSED OFFERS</span><h2>Commerce cannot buy a verdict.</h2></div></div>${state.offers.length ? state.offers.map((offer) => `<div class="card"><h3>${esc(offer.product_name)}</h3><p>${esc(offer.merchant)} · Affiliate relationship disclosed</p><button class="secondary" data-action="offer-click" data-id="${offer.id}">View offer</button></div>`).join("") : `<div class="empty">No offers are active.</div>`}</section>`;
  }

  function openSheet(kind) {
    state.sheet = kind;
    sheetBackdrop.classList.add("is-open");
    sheetBackdrop.setAttribute("aria-hidden", "false");
    sheet.classList.add("is-open");
    sheet.setAttribute("aria-hidden", "false");
    renderSheet();
    if (kind === "coach" && premium()) loadQna();
  }

  function closeSheet() {
    state.sheet = null;
    sheetBackdrop.classList.remove("is-open");
    sheetBackdrop.setAttribute("aria-hidden", "true");
    sheet.classList.remove("is-open");
    sheet.setAttribute("aria-hidden", "true");
    sheet.innerHTML = "";
  }

  function sheetHead(kicker, heading) {
    return `<div class="sheet-handle"></div><header class="sheet-header"><div><span class="section-kicker">${kicker}</span><h2>${heading}</h2></div><button class="icon-button" data-action="close-sheet" aria-label="Close">${icon("close")}</button></header>`;
  }

  function renderSheet() {
    if (state.sheet === "privacy") {
      sheet.innerHTML = `${sheetHead("PLAIN LANGUAGE", "How privacy works")}<p>SkinProof creates cosmetic measurements from captures you explicitly choose. Raw images never go to the language model. You can export or delete your history from your profile.</p><div class="notice info">Consent is a separate step immediately before capture. Creating a profile does not grant camera or facial-data permission.</div><button class="primary" data-action="close-sheet">Got it</button>`;
      return;
    }
    if (state.sheet === "restore") {
      sheet.innerHTML = `${sheetHead("RETURNING", "Sign in to SkinProof")}<p>Use the temporary account credentials supplied with this build.</p><div class="field"><label for="loginUsername">Username</label><input id="loginUsername" autocomplete="username" value="${DEMO_USERNAME}"></div><div class="field"><label for="loginAccessCode">Access code</label><input id="loginAccessCode" type="password" autocomplete="current-password"></div><button class="primary" data-action="restore-login">Sign in</button><hr><details><summary>Restore an older local profile</summary><p class="small">Use the profile ID copied from that profile's data settings.</p><div class="field"><label for="restoreId">Profile ID</label><input id="restoreId" autocomplete="off"></div><button class="secondary" data-action="restore-id">Open profile</button></details>`;
      return;
    }
    if (state.sheet === "details") {
      const metric = state.captureResult?.metric || {};
      sheet.innerHTML = `${sheetHead("MEASUREMENT DETAILS", "Your baseline signal")}<div class="row"><strong>Model</strong><span>${esc(metric.model_version || ":")}</span></div><div class="row"><strong>Blemish proxy</strong><span>${number(metric.blemish_count, 0)}</span></div><div class="row"><strong>Redness</strong><span>${number(metric.redness_score, 4)}</span></div><div class="row"><strong>Dark-spot area</strong><span>${number(metric.darkspot_area, 4)}</span></div><div class="row"><strong>Texture</strong><span>${number(metric.texture_score, 2)}</span></div><p class="caption">These are cosmetic measurements from one frame, not a diagnosis or product verdict.</p>`;
      return;
    }
    if (state.sheet === "routine") return renderRoutineSheet();
    if (state.sheet === "coach") return renderCoachSheet();
    if (state.sheet === "edit-profile") return renderEditProfileSheet();
    if (state.sheet === "advanced") {
      sheet.innerHTML = `${sheetHead("ADVANCED", "Historical controls")}<p>Re-run stored captures with a named measurement model. Original snapshots remain in history for provenance.</p><div class="field"><label for="reprocessModel">Model version</label><input id="reprocessModel" value="deterministic-3.1"></div><button class="primary" data-action="reprocess">Reprocess my history</button>`;
      return;
    }
    if (state.sheet === "consent") {
      sheet.innerHTML = `${sheetHead("A CLEAR CHOICE", "Your face is yours")}<p>SkinProof stores only captures you choose and creates cosmetic measurements from them. You can export or delete your history.</p><label class="check-row"><input id="workspaceConsentCheck" type="checkbox"><span>I understand and consent to facial-data capture for my SkinProof profile.</span></label><div class="actions"><button class="primary" data-action="grant-workspace-consent" disabled>I consent</button></div>`;
    }
  }

  function renderRoutineSheet() {
    const events = state.dashboard?.routine_events || [];
    const productOptions = state.products.map((p) => `<option value="${p.id}">${esc(p.name)}</option>`).join("");
    sheet.innerHTML = `${sheetHead("YOUR RITUAL", "Keep the variables honest")}<p>Add what is in your routine today. A start or stop timestamp is part of the evidence.</p><div class="card is-quiet"><div class="field"><label for="routineProductName">Add a product by name</label><input id="routineProductName" maxlength="160" placeholder="Barrier serum"></div><div class="field"><label for="routineCategory">Category</label><select id="routineCategory"><option>cleanser</option><option>moisturizer</option><option>serum</option><option>sunscreen</option><option>retinoid</option><option>other</option></select></div><div class="field"><label for="routineSlot">Routine slot</label><select id="routineSlot"><option value="morning">Morning</option><option value="evening">Evening</option><option value="both">Morning and evening</option><option value="occasionally">Occasionally</option></select></div><button class="primary" data-action="add-routine-product">Add to routine</button></div><hr><h3>Current timeline</h3>${events.length ? `<div class="timeline">${events.slice(0, 8).map((event) => `<div class="event"><strong>${esc(event.product_name)}</strong><p class="small">${esc(title(event.action))} · ${esc(title(event.slot))} · ${when(event.timestamp)}</p></div>`).join("")}</div>` : `<div class="empty">No products yet. Starting fresh is a valid routine.</div>`}${premium() ? `<hr><span class="section-kicker">ONE-VARIABLE EXPERIMENT</span><div class="field"><label for="experimentName">Experiment name</label><input id="experimentName" placeholder="Introduce one serum"></div><div class="field"><label for="experimentProduct">Test product</label><select id="experimentProduct">${productOptions}</select></div><div class="field"><label for="experimentMetric">Primary signal</label><select id="experimentMetric"><option value="redness_score">Redness</option><option value="blemish_count">Blemish proxy</option><option value="darkspot_area">Dark spots</option><option value="texture_score">Texture</option></select></div><button class="secondary" data-action="start-experiment" ${state.products.length ? "" : "disabled"}>Start experiment</button>` : `<hr><div class="lock"><h3>Run one-variable experiments</h3><p class="small">Premium adds stabilization windows and product-level verdicts.</p><button class="secondary" data-action="upgrade">Explore Premium</button></div>`}`;
  }

  function renderCoachSheet() {
    if (!premium()) {
      sheet.innerHTML = `${sheetHead("ASK YOUR HISTORY", "Answers grounded in your data")}<div class="lock"><h3>Coach opens with Premium.</h3><p>Ask about redness, routine changes, products, ingredients, cadence, or a specific date. Medical-scope questions always route outside the model.</p><button class="primary" data-action="upgrade">Unlock grounded Q&A</button></div>`;
      return;
    }
    sheet.innerHTML = `${sheetHead("YOUR EVIDENCE COACH", "Ask your own history")}<div class="chat" style="max-height:46vh;overflow:auto">${state.qna.length ? state.qna.map((item) => `<div class="notice ${item.role === "user" ? "info" : ""}" style="margin-bottom:9px"><div><strong>${item.role === "user" ? "You" : "SkinProof"}</strong><br>${esc(item.content)}${item.citations?.length ? `<p class="caption">Cites ${item.citations.map((c) => when(c.date)).join(", ")}</p>` : ""}</div></div>`).join("") : `<div class="empty">Try “What changed after my last routine update?”</div>`}</div><div class="field" style="margin-top:18px"><label for="coachQuestion">Your question</label><textarea id="coachQuestion" rows="3" placeholder="Why did redness change?"></textarea></div><button class="primary" data-action="ask-coach">Ask my history ${icon("coach")}</button><hr><button class="secondary" data-action="ingredient-sheet">Explain a product's ingredients</button>`;
  }

  function renderEditProfileSheet() {
    const e = experience();
    sheet.innerHTML = `${sheetHead("EDIT PROFILE", "Keep it personal")}<div class="field"><label for="editName">Name</label><input id="editName" maxlength="80" value="${esc(e.display_name || "")}"></div><div class="field"><label for="editFocus">Primary focus</label><select id="editFocus">${VERTICALS.map((v) => `<option value="${v}" ${e.focus_vertical === v ? "selected" : ""}>${title(v)}</option>`).join("")}</select></div><div class="field"><label for="editGoals">Goals <small>comma separated</small></label><textarea id="editGoals">${esc((e.goals || []).join(", "))}</textarea></div><button class="primary" data-action="save-profile-edit">Save changes</button>`;
  }

  async function loadQna() {
    try { state.qna = await api(`/api/users/${state.id}/qna`); if (state.sheet === "coach") renderCoachSheet(); }
    catch (error) { toast(error.message, "error"); }
  }

  async function createProfile() {
    const input = document.getElementById("profileName");
    const name = input?.value.trim();
    if (!name) return toast("Add the name you want this space to use.", "error");
    try {
      state.loading = true;
      const created = await api("/api/users", { method: "POST", body: JSON.stringify({ skin_type: null }) });
      state.id = created.user.id;
      localStorage.setItem(USER_KEY, state.id);
      state.draft.name = name;
      state.profile = await api(`/api/users/${state.id}/profile`, { method: "PATCH", body: JSON.stringify({ display_name: name }) });
      setStep(3);
    } catch (error) { toast(error.message, "error"); }
    finally { state.loading = false; }
  }

  async function patchProfile(fields) {
    state.profile = await api(`/api/users/${state.id}/profile`, { method: "PATCH", body: JSON.stringify(fields) });
    return state.profile;
  }

  async function submitCapture(onboarding) {
    if (!state.captureFile) return toast("Choose a photo first.", "error");
    state.draft.processing = true;
    if (onboarding) renderOnboarding(); else renderWorkspace();
    try {
      const imageBase64 = await fileToBase64(state.captureFile);
      const result = await api("/api/captures", { method: "POST", body: JSON.stringify({
        user_id: state.id, image_base64: imageBase64, vertical: focus(), is_baseline: onboarding || !(state.dashboard?.history || []).length,
        quality: { face_present: true, yaw_degrees: 0, pitch_degrees: 0, distance_cm: 45, expression_neutral: true },
        device_meta: { source: "responsive_web", user_agent: navigator.userAgent.slice(0, 180) },
      }) });
      state.captureResult = result;
      state.captureFile = null;
      if (state.capturePreview) URL.revokeObjectURL(state.capturePreview);
      state.capturePreview = null;
      if (onboarding) setStep(12);
      else { await loadWorkspace(); state.route = "journey"; renderWorkspace(); toast("Frame accepted. Your journey is updated.", "success"); }
    } catch (error) {
      toast(formatCaptureError(error), "error");
      if (onboarding) renderOnboarding(); else renderWorkspace();
    } finally {
      state.draft.processing = false;
      saveDraft();
    }
  }

  const fileToBase64 = (file) => new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.onload = () => resolve(String(reader.result).split(",")[1]);
    reader.onerror = reject;
    reader.readAsDataURL(file);
  });

  function formatCaptureError(error) {
    const message = String(error.message || "").toLowerCase();
    if (message.includes("dark") || message.includes("brightness")) return "This frame is too dark. Face a window and try again.";
    if (message.includes("sharp") || message.includes("blur")) return "Hold the phone steadier and try again.";
    if (message.includes("distance")) return "Move into the guide and hold the phone 30 to 80 cm away.";
    if (message.includes("face")) return "We could not find a usable face frame. Center your face and try again.";
    return error.message || "We could not save that frame yet. Try again.";
  }

  async function navigate(route) {
    state.route = route;
    localStorage.setItem(ROUTE_KEY, route);
    if (route === "discover" && premium()) {
      renderLoadingWorkspace();
      try {
        [state.discover, state.offers] = await Promise.all([
          api(`/api/users/${state.id}/discover`), api(`/api/users/${state.id}/commerce/offers`),
        ]);
      } catch (error) { toast(error.message, "error"); }
    }
    renderWorkspace();
    window.scrollTo({ top: 0, behavior: "smooth" });
  }

  async function refreshAndRender(message) {
    await loadWorkspace();
    renderWorkspace();
    if (state.sheet) renderSheet();
    if (message) toast(message, "success");
  }

  document.addEventListener("change", (event) => {
    if (event.target.id === "consentCheck") { state.draft.consentChecked = event.target.checked; saveDraft(); renderOnboarding(); }
    if (event.target.id === "workspaceConsentCheck") {
      const button = sheet.querySelector('[data-action="grant-workspace-consent"]');
      if (button) button.disabled = !event.target.checked;
    }
  });

  capturePicker.addEventListener("change", () => {
    const file = capturePicker.files?.[0];
    if (!file) return;
    if (!file.type.startsWith("image/")) return toast("Choose an image file.", "error");
    if (file.size > 15 * 1024 * 1024) return toast("Choose an image smaller than 15 MB.", "error");
    state.captureFile = file;
    if (state.capturePreview) URL.revokeObjectURL(state.capturePreview);
    state.capturePreview = URL.createObjectURL(file);
    if (state.draft.step >= 10 && state.draft.step <= 12 && !experience().onboarding_completed_at) setStep(11);
    else { state.route = "capture"; renderWorkspace(); }
    capturePicker.value = "";
  });

  document.addEventListener("keydown", (event) => {
    if (event.key === "Escape" && state.sheet) closeSheet();
    if (event.key === "Enter" && event.target.id === "profileName") createProfile();
  });

  sheetBackdrop.addEventListener("click", closeSheet);

  document.addEventListener("click", async (event) => {
    const target = event.target.closest("[data-action]");
    if (!target || state.loading) return;
    const action = target.dataset.action;
    try {
      if (action === "next-step") return setStep(state.draft.step + 1);
      if (action === "onboarding-back") return setStep(state.draft.step - 1);
      if (action === "create-profile") return createProfile();
      if (action === "privacy-sheet") return openSheet("privacy");
      if (action === "restore-profile") return openSheet("restore");
      if (action === "close-sheet") return closeSheet();
      if (action === "select-focus") { state.draft.focus = target.dataset.value; saveDraft(); return renderOnboarding(); }
      if (action === "save-focus") { await patchProfile({ focus_vertical: state.draft.focus }); return setStep(5); }
      if (action === "toggle-goal") {
        const value = target.dataset.value;
        if (value === "Not sure yet") state.draft.goals = state.draft.goals.includes(value) ? [] : [value];
        else {
          state.draft.goals = state.draft.goals.filter((goal) => goal !== "Not sure yet");
          state.draft.goals = state.draft.goals.includes(value) ? state.draft.goals.filter((goal) => goal !== value) : state.draft.goals.length < 3 ? [...state.draft.goals, value] : state.draft.goals;
        }
        saveDraft(); return renderOnboarding();
      }
      if (action === "save-goals") { await patchProfile({ goals: state.draft.goals }); return setStep(6); }
      if (action === "select-skin-type") { state.draft.skinType = target.dataset.value; saveDraft(); return renderOnboarding(); }
      if (action === "save-skin-type") { await patchProfile({ skin_type: state.draft.skinType }); return setStep(7); }
      if (action === "skip-skin-type") return setStep(7);
      if (action === "select-experience") { state.draft.experience = target.dataset.value; saveDraft(); return renderOnboarding(); }
      if (action === "save-experience") { await patchProfile({ experience_level: state.draft.experience }); return setStep(8); }
      if (action === "add-first-product") { state.draft.addProduct = true; saveDraft(); return renderOnboarding(); }
      if (action === "skip-routine") { state.draft.addProduct = false; return setStep(9); }
      if (action === "save-first-product") {
        const name = document.getElementById("firstProduct")?.value.trim();
        if (!name) return toast("Add the product name, or start fresh.", "error");
        const category = document.getElementById("firstCategory").value;
        const slot = document.getElementById("firstSlot").value;
        const product = await api("/api/products", { method: "POST", body: JSON.stringify({ name, category, stabilization_days: 14 }) });
        await api("/api/routine-events", { method: "POST", body: JSON.stringify({ user_id: state.id, product_id: product.id, action: "start", slot }) });
        state.draft.productName = name; state.draft.productCategory = category; state.draft.productSlot = slot; saveDraft();
        return setStep(9);
      }
      if (action === "decline-consent") { await api(`/api/users/${state.id}/consent`, { method: "POST", body: JSON.stringify({ facial_data: false }) }); toast("Capture will stay locked. You can return when ready."); return; }
      if (action === "grant-consent") { state.profile = await api(`/api/users/${state.id}/consent`, { method: "POST", body: JSON.stringify({ facial_data: true }) }); return setStep(10); }
      if (action === "open-camera" || action === "choose-photo") { capturePicker.setAttribute("capture", action === "open-camera" ? "user" : ""); return capturePicker.click(); }
      if (action === "retake-photo") { state.captureFile = null; if (state.capturePreview) URL.revokeObjectURL(state.capturePreview); state.capturePreview = null; return capturePicker.click(); }
      if (action === "submit-baseline") return submitCapture(true);
      if (action === "measurement-details") return openSheet("details");
      if (action === "finish-onboarding") { await patchProfile({ onboarding_complete: true }); return setStep(13); }
      if (action === "enter-app") { localStorage.removeItem(DRAFT_KEY); state.draft.step = 13; state.route = "today"; await loadWorkspace(); return renderWorkspace(); }
      if (action === "navigate") return navigate(target.dataset.route);
      if (action === "open-sheet") return openSheet(target.dataset.sheet);
      if (action === "change-vertical") { await patchProfile({ focus_vertical: target.dataset.value }); await refreshAndRender(); return; }
      if (action === "submit-workspace-capture") return submitCapture(false);
      if (action === "workspace-consent") return openSheet("consent");
      if (action === "grant-workspace-consent") { await api(`/api/users/${state.id}/consent`, { method: "POST", body: JSON.stringify({ facial_data: true }) }); closeSheet(); await refreshAndRender("Consent saved. Capture is ready."); return; }
      if (action === "upgrade") { await api(`/api/users/${state.id}/subscription/upgrade`, { method: "POST", body: JSON.stringify({ source: "local_experience" }) }); closeSheet(); await refreshAndRender("Premium is active."); return; }
      if (action === "cancel-premium") { await api(`/api/users/${state.id}/subscription/cancel`, { method: "POST" }); await refreshAndRender("Premium cancelled. Your history remains."); return; }
      if (action === "add-routine-product") {
        const name = document.getElementById("routineProductName")?.value.trim(); if (!name) return toast("Add a product name.", "error");
        const product = await api("/api/products", { method: "POST", body: JSON.stringify({ name, category: document.getElementById("routineCategory").value, stabilization_days: 14 }) });
        await api("/api/routine-events", { method: "POST", body: JSON.stringify({ user_id: state.id, product_id: product.id, action: "start", slot: document.getElementById("routineSlot").value }) });
        await refreshAndRender(`${name} joined your routine.`); return;
      }
      if (action === "start-experiment") {
        const name = document.getElementById("experimentName")?.value.trim(); if (!name) return toast("Name the experiment.", "error");
        await api("/api/experiments", { method: "POST", body: JSON.stringify({ user_id: state.id, name, product_id: document.getElementById("experimentProduct").value, primary_metric: document.getElementById("experimentMetric").value, target_days: 14 }) });
        await refreshAndRender("Experiment started. Hold everything else steady."); return;
      }
      if (action === "ask-coach") {
        const question = document.getElementById("coachQuestion")?.value.trim(); if (!question) return toast("Ask a question about your history.", "error");
        await api(`/api/users/${state.id}/qna`, { method: "POST", body: JSON.stringify({ question }) }); await loadQna(); return;
      }
      if (action === "ingredient-sheet") {
        sheet.innerHTML = `${sheetHead("INGREDIENT INTELLIGENCE", "Explain a product")}<div class="field"><label for="ingredientProduct">Product</label><select id="ingredientProduct">${state.products.map((p) => `<option value="${p.id}">${esc(p.name)}</option>`).join("")}</select></div><button class="primary" data-action="analyze-ingredients" ${state.products.length ? "" : "disabled"}>Explain ingredients</button><div id="ingredientResult" style="margin-top:20px"></div>`; return;
      }
      if (action === "analyze-ingredients") {
        const result = await api(`/api/products/${document.getElementById("ingredientProduct").value}/ingredient-explainer?user_id=${state.id}`);
        document.getElementById("ingredientResult").innerHTML = result.reviewed.length ? result.reviewed.map((item) => `<div class="journey-card"><span class="journey-index">${icon("status")}</span><div class="journey-copy"><strong>${esc(item.ingredient)}</strong><p>${esc(item.purpose)} · ${esc(item.caution)}</p></div></div>`).join("") + (result.unknown.length ? `<p class="caption">Unknown entries: ${result.unknown.map(esc).join(", ")}</p>` : "") : `<div class="empty">No reviewed ingredients found.</div>`; return;
      }
      if (action === "save-profile-edit") {
        const goals = document.getElementById("editGoals").value.split(",").map((g) => g.trim()).filter(Boolean).slice(0, 8);
        await patchProfile({ display_name: document.getElementById("editName").value, focus_vertical: document.getElementById("editFocus").value, goals }); closeSheet(); await refreshAndRender("Profile updated."); return;
      }
      if (action === "export-data") {
        const data = await api(`/api/users/${state.id}/export`); const url = URL.createObjectURL(new Blob([JSON.stringify(data, null, 2)], { type: "application/json" })); const link = document.createElement("a"); link.href = url; link.download = "skinproof-export.json"; link.click(); setTimeout(() => URL.revokeObjectURL(url), 1000); return;
      }
      if (action === "reprocess") { const result = await api(`/api/users/${state.id}/reprocess`, { method: "POST", body: JSON.stringify({ model_version: document.getElementById("reprocessModel").value }) }); closeSheet(); toast(`${result.processed_count} captures reprocessed.`, "success"); return; }
      if (action === "delete-profile") {
        if (!window.confirm("Delete this profile, its database history, and stored photos? This cannot be undone.")) return;
        await api(`/api/users/${state.id}`, { method: "DELETE" }); localStorage.removeItem(USER_KEY); localStorage.removeItem(DRAFT_KEY); state.id = null; state.profile = null; state.dashboard = null; state.draft = { step: 0, name: "", focus: "skin", goals: [] }; renderOnboarding(); return;
      }
      if (action === "offer-click") { const offer = await api(`/api/users/${state.id}/commerce/offers/${target.dataset.id}/click`, { method: "POST" }); window.open(offer.url, "_blank", "noopener,noreferrer"); return; }
      if (action === "restore-login") {
        const username = document.getElementById("loginUsername")?.value.trim();
        const accessCode = document.getElementById("loginAccessCode")?.value;
        if (username !== DEMO_USERNAME || accessCode !== DEMO_ACCESS_CODE) return toast("The username or access code is incorrect.", "error");

        let id = localStorage.getItem(DEMO_PROFILE_KEY);
        let profile = null;
        if (id) profile = await api(`/api/users/${id}/profile`).catch(() => null);
        if (!profile) {
          const created = await api("/api/users", { method: "POST", body: JSON.stringify({ skin_type: "temporary demo" }) });
          id = created.user.id;
          profile = await api(`/api/users/${id}/profile`, { method: "PATCH", body: JSON.stringify({ display_name: "Demo", focus_vertical: "skin", goals: ["Consistency"], onboarding_complete: true }) });
          localStorage.setItem(DEMO_PROFILE_KEY, id);
        }

        state.id = id;
        state.profile = profile;
        localStorage.setItem(USER_KEY, id);
        closeSheet();
        await loadWorkspace();
        renderWorkspace();
        toast("Signed in to the temporary account.", "success");
        return;
      }
      if (action === "restore-id") {
        const id = document.getElementById("restoreId")?.value.trim(); if (!id) return toast("Enter a profile ID.", "error");
        const profile = await api(`/api/users/${id}/profile`); state.id = id; state.profile = profile; localStorage.setItem(USER_KEY, id); closeSheet();
        if (profile.experience_profile?.onboarding_completed_at) { await loadWorkspace(); renderWorkspace(); } else { state.draft.name = profile.experience_profile?.display_name || ""; state.draft.step = state.draft.name ? 4 : 2; renderOnboarding(); } return;
      }
    } catch (error) {
      toast(error.message, "error");
    }
  });

  async function boot() {
    try {
      if (state.id) {
        state.profile = await api(`/api/users/${state.id}/profile`);
        const e = experience();
        state.draft.name = state.draft.name || e.display_name || "";
        state.draft.focus = e.focus_vertical || state.draft.focus;
        state.draft.goals = e.goals?.length ? e.goals : state.draft.goals;
        if (e.onboarding_completed_at) {
          await loadWorkspace();
          renderWorkspace();
          return;
        }
        state.draft.step = state.draft.step || (e.display_name ? 4 : 2);
        saveDraft();
      }
      renderOnboarding();
    } catch (error) {
      if (error.status === 400 || error.status === 404) {
        localStorage.removeItem(USER_KEY);
        state.id = null;
        state.draft.step = 0;
        saveDraft();
        renderOnboarding();
      } else {
        app.innerHTML = `<main class="onboarding-shell"><section class="onboarding-card"><span class="eyebrow">CONNECTION PAUSED</span><h1>Your progress is still here.</h1><p class="lead">We could not open the local API yet.</p><button class="primary" onclick="location.reload()">Try again</button></section></main>`;
      }
    }
  }

  boot();
})();
