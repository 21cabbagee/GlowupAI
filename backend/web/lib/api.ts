/**
 * Typed client for the SkinProof FastAPI surface.
 *
 * Requests go to relative `/api/*` paths; `next.config.ts` rewrites them to the
 * uvicorn origin, so there is no base URL or CORS handling here.
 */

export type Vertical = "skin";

export const VERTICALS: Vertical[] = ["skin"];

export type MetricKey =
  | "redness_score"
  | "blemish_count"
  | "darkspot_area"
  | "texture_score";

export type VerdictLabel =
  | "keep"
  | "likely_useful"
  | "evidence_unclear"
  | "investigate"
  | "locked";

export interface User {
  id: string;
  skin_type: string | null;
  consent_state: string;
  created_at?: string;
}

export interface Entitlement {
  plan: "free" | "premium";
  status: string;
  source?: string | null;
  updated_at?: string;
}

export interface ExperienceProfile {
  user_id: string;
  display_name: string | null;
  focus_vertical: Vertical;
  goals: string[];
  experience_level: string | null;
  onboarding_completed_at: string | null;
  updated_at?: string;
}

export interface Profile {
  user: User;
  entitlement: Entitlement;
  experience_profile?: ExperienceProfile;
}

export interface CaptureGuide {
  message: string;
  state: string;
  next_window_start: string;
}

export interface Engagement {
  capture_count: number;
  capture_streak: number;
  guide: CaptureGuide;
}

export interface Analytics {
  median_history_days: number;
  [key: string]: unknown;
}

export interface MeasurementExplanation {
  confidence_label: string;
  confidence_message: string;
  comparison_ready: boolean;
  quality_score: number;
  quality_issues: string[];
  capture_protocol: string;
  noise_floor_message: string;
}

export interface HistoryPoint {
  id?: string;
  captured_at: string;
  is_baseline?: boolean;
  redness_score: number;
  blemish_count: number;
  darkspot_area: number;
  texture_score: number;
  confidence: number;
  model_version: string;
  confidence_label?: string;
  confidence_message?: string;
  comparison_ready?: boolean;
  quality_score?: number;
  quality_issues?: string[];
  capture_protocol?: string;
  noise_floor?: Partial<Record<MetricKey, number>>;
}

export interface VerdictEvidence {
  n_after?: number;
  n_before?: number;
  confidence?: number | null;
}

export interface Verdict {
  label: VerdictLabel;
  generated_text: string;
  product_name: string;
  product_id?: string;
  metric?: MetricKey;
  evidence?: VerdictEvidence;
}

/** A product currently mid-stabilization-window; starting/changing another
 *  product now would make its evidence come back evidence_unclear. */
export interface ConfoundWindow {
  product_id: string;
  product_name: string;
  started_at: string;
  stable_at: string;
}

export interface ConfoundCheck {
  confounded: boolean;
  active_windows: ConfoundWindow[];
  message?: string;
}

export interface RoutineEvent {
  id?: string;
  action: string;
  product_name: string;
  timestamp: string;
  slot: string;
  notes?: string | null;
  /** Present (non-null) only when a "start"/"change" event overlapped an
   *  in-progress stabilization window for another product. */
  confound_warning?: ConfoundCheck | null;
}

export interface DashboardFeatures {
  experiments: boolean;
  ingredient_analysis: boolean;
  long_history: boolean;
  qna: boolean;
  discover: boolean;
  root_cause: boolean;
  budget_optimizer: boolean;
  derm_export: boolean;
  product_prediction: boolean;
  /** True once the user has spent their one free lifetime verdict unlock,
   *  or is on Premium — distinct from the per-feature booleans above. */
  product_verdicts_unlocked: boolean;
}

export interface WeeklyMetricSummary {
  metric: MetricKey;
  label: string;
  direction: "improved" | "steady" | "increased";
  delta: number;
  noise_floor: number;
  sentence: string;
}

export interface WeeklyRecap {
  status: "baseline_needed" | "building_signal" | "directional" | "steady";
  headline: string;
  body: string;
  next_action: string;
  capture_count: number;
  total_capture_count?: number;
  check_in_count: number;
  comparison_mode?: string;
  confidence_label: string;
  metric_summaries: WeeklyMetricSummary[];
  period: { start: string | null; end: string | null };
  disclaimer: string;
}

export interface CheckIn {
  id: string;
  routine_state: "steady" | "changed" | "missed" | "not_sure";
  skin_feel: "better" | "same" | "worse" | "not_sure";
  note?: string | null;
  occurred_at: string;
}

export interface Dashboard {
  engagement: Engagement;
  analytics: Analytics;
  weekly_recap: WeeklyRecap;
  check_ins: CheckIn[];
  verdicts: Verdict[];
  history: HistoryPoint[];
  routine_events: RoutineEvent[];
  features?: DashboardFeatures;
}
export interface Product {
  id: string;
  name: string;
  category: string;
  barcode?: string | null;
  ingredients?: string[];
  stabilization_days: number;
}

/** Told early, before target_days, once the evidence is already conclusive. */
export interface EarlyStop {
  conclusive: boolean;
  recommended_status?: string;
  message: string;
}

export interface Experiment {
  id: string;
  name: string;
  status: string;
  hypothesis?: string | null;
  primary_metric: MetricKey;
  target_days: number;
  started_at?: string;
  products?: { id?: string; name: string; product_id?: string }[];
  early_stop?: EarlyStop;
}

export interface CaptureResult {
  capture?: { id: string; captured_at: string };
  metric: {
    confidence: number;
    redness_score?: number;
    blemish_count?: number;
    model_version?: string;
  };
  measurement?: MeasurementExplanation;
  capture_protocol?: string;
}
/** One actionable tip for why a submitted frame was rejected. */
export interface CaptureCoachingTip {
  check: string;
  message: string;
}

export interface CaptureRejection {
  message: string;
  quality: {
    accepted?: boolean;
    score?: number;
    coaching?: CaptureCoachingTip[];
    [key: string]: unknown;
  };
}

export interface QnaTurn {
  role: "user" | "assistant";
  content: string;
  created_at?: string;
  citations?: { date: string; kind?: string; id?: string }[];
}

export interface IngredientExplainer {
  product_name: string;
  reviewed: { ingredient: string; purpose: string; caution: string }[];
  unknown: string[];
}

export interface CohortRecommendation {
  name: string;
  category: string;
  sample_size: number;
  average_effect: number;
}

export interface Offer {
  id: string;
  product_name: string;
  merchant: string;
  price_cents?: number | null;
  currency?: string;
}

export interface TriageResult {
  scope: "cosmetic_tracking" | "dermatology_review";
  message: string;
}

// ---------------------------------------------------------- shelf scan
export type ShelfScanJobStatus = "queued" | "running" | "completed" | "failed";

export interface ShelfScanCandidate {
  name: string;
  brand?: string | null;
  category: string;
  ingredients: string[];
}

export interface ShelfScanResult {
  candidates: ShelfScanCandidate[];
  message: string;
}

export interface ShelfScanJob {
  id: string;
  status: ShelfScanJobStatus;
  result: ShelfScanResult | null;
  error?: string | null;
}

export interface ShelfScanSelection {
  name: string;
  category: string;
  ingredients: string[];
  stabilization_days: number;
}

// ------------------------------------------------------ product prediction
export interface ProductOverlap {
  product_name: string;
  shared_ingredients: string[];
}

export interface ProductPrediction {
  product_id: string;
  product_name: string;
  ingredients: string[];
  overlap_with_investigate: ProductOverlap[];
  overlap_with_likely_useful: ProductOverlap[];
  cohort_overlap: ProductOverlap[];
  headline: string;
  disclaimer: string;
}

export interface PurchaseGuidance {
  product_id: string | null;
  product_name: string;
  barcode: string | null;
  ingredients: string[];
  signal: "caution" | "promising_similarity" | "new_profile" | "missing_ingredients";
  headline: string;
  next_action: string;
  overlap_with_investigate: ProductOverlap[];
  overlap_with_likely_useful: ProductOverlap[];
  cohort_overlap: ProductOverlap[];
  estimated_annual_cost_cents: number | null;
  currency: string;
  disclaimer: string;
}
// ---------------------------------------------------- context / root-cause
export type ContextEventType =
  | "sleep"
  | "travel"
  | "weather"
  | "cycle"
  | "stress"
  | "diet"
  | "custom";

export const CONTEXT_EVENT_TYPES: ContextEventType[] = [
  "sleep",
  "travel",
  "weather",
  "cycle",
  "stress",
  "diet",
  "custom",
];

export interface ContextEvent {
  id: string;
  event_type: ContextEventType;
  value: string | null;
  notes?: string | null;
  occurred_at: string;
}

export interface RootCauseCorrelation {
  event_type: ContextEventType;
  occurrences: number;
  normalized_effect: number;
  metric: MetricKey;
  message: string;
}

// -------------------------------------------------------- budget optimizer
export interface FlaggedProduct {
  product_id: string;
  product_name: string;
  days_stable: number;
  estimated_annual_cost_cents: number | null;
  currency: string;
  reason: string;
}

export interface BudgetOptimizer {
  flagged: FlaggedProduct[];
  estimated_annual_waste_cents: number;
  currency: string;
  disclaimer: string;
}

// --------------------------------------------------------- derm export
export interface DermExport {
  generated_at: string;
  capture_count: number;
  model_versions: string[];
  verdicts: Verdict[];
  printable_html: string;
  disclaimer: string;
}

/** Carries the HTTP status so callers can branch on 403 (consent/premium).
 *  Also carries the raw `detail` payload so callers that need more than the
 *  flattened message — e.g. capture-quality coaching tips — can read it. */
export class ApiError extends Error {
  status: number;
  detail?: unknown;

  constructor(message: string, status: number, detail?: unknown) {
    super(message);
    this.name = "ApiError";
    this.status = status;
    this.detail = detail;
  }
}

/** FastAPI `detail` is a string, an object, or a validation array. */
function readDetail(payload: unknown, fallback: string): string {
  if (!payload || typeof payload !== "object") return fallback;
  const detail = (payload as { detail?: unknown }).detail;
  if (typeof detail === "string") return detail;
  if (Array.isArray(detail)) {
    const first = detail[0] as { msg?: string } | undefined;
    return first?.msg ?? fallback;
  }
  if (detail && typeof detail === "object") {
    const obj = detail as { message?: string; reason?: string };
    return obj.message ?? obj.reason ?? fallback;
  }
  return fallback;
}

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const res = await fetch(path, {
    ...init,
    headers: {
      "Content-Type": "application/json",
      ...(init?.headers ?? {}),
    },
  });

  if (res.status === 204) return undefined as T;

  const payload = await res.json().catch(() => null);
  if (!res.ok) {
    const detail = payload && typeof payload === "object" ? (payload as { detail?: unknown }).detail : undefined;
    throw new ApiError(readDetail(payload, `Request failed (${res.status})`), res.status, detail);
  }
  return payload as T;
}

const post = <T>(path: string, body?: unknown) =>
  request<T>(path, {
    method: "POST",
    body: JSON.stringify(body ?? {}),
  });

export const api = {
  health: () => request<{ status: string }>("/api/health"),

  // ---------------------------------------------------------------- account
  createUser: (skin_type: string | null) =>
    post<{ user: User }>("/api/users", { skin_type }),

  profile: (userId: string) => request<Profile>(`/api/users/${userId}/profile`),

  updateProfile: (
    userId: string,
    input: {
      display_name?: string;
      skin_type?: string | null;
      focus_vertical?: Vertical;
      goals?: string[];
      experience_level?: string;
      onboarding_complete?: boolean;
    },
  ) =>
    request<Profile>(`/api/users/${userId}/profile`, {
      method: "PATCH",
      body: JSON.stringify(input),
    }),

  grantConsent: (userId: string) =>
    post<unknown>(`/api/users/${userId}/consent`, { facial_data: true }),

  upgrade: (userId: string) =>
    post<Entitlement>(`/api/users/${userId}/subscription/upgrade`, {
      source: "local_checkout",
    }),

  cancel: (userId: string) =>
    post<Entitlement>(`/api/users/${userId}/subscription/cancel`),

  exportData: (userId: string) =>
    request<Record<string, unknown>>(`/api/users/${userId}/export`),

  deleteUser: (userId: string) =>
    request<void>(`/api/users/${userId}`, { method: "DELETE" }),

  reprocess: (userId: string, model_version: string) =>
    post<{ processed_count: number }>(`/api/users/${userId}/reprocess`, {
      model_version,
    }),

  // ------------------------------------------------------------- dashboard
  dashboard: (userId: string, vertical: Vertical) =>
    request<Dashboard>(`/api/users/${userId}/dashboard?vertical=${vertical}`),

  history: (userId: string, vertical: Vertical) =>
    request<HistoryPoint[]>(`/api/users/${userId}/history?vertical=${vertical}`),

  analytics: (userId: string) =>
    request<Analytics>(`/api/users/${userId}/analytics`),

  captureGuide: (userId: string, vertical: Vertical) =>
    request<CaptureGuide>(
      `/api/users/${userId}/capture-guide?vertical=${vertical}`,
    ),

  weeklyRecap: (userId: string, vertical: Vertical) =>
    request<WeeklyRecap>(
      `/api/users/${userId}/weekly-recap?vertical=${vertical}`,
    ),

  checkIns: (userId: string) =>
    request<CheckIn[]>(`/api/users/${userId}/check-ins`),

  createCheckIn: (
    userId: string,
    input: { routine_state: CheckIn["routine_state"]; skin_feel: CheckIn["skin_feel"]; note?: string },
  ) => post<CheckIn>(`/api/users/${userId}/check-ins`, input),

  submitMeasurementFeedback: (
    userId: string,
    input: { capture_id: string; agreement: "fair" | "uncertain" | "off"; note?: string },
  ) => post<unknown>(`/api/users/${userId}/measurement-feedback`, input),
  logEngagement: (userId: string, event_type: string, reference_id?: string) =>
    post<unknown>(`/api/users/${userId}/engagement`, {
      event_type,
      reference_id: reference_id ?? null,
    }),

  // --------------------------------------------------------------- capture
  createCapture: (input: {
    user_id: string;
    image_base64: string;
    vertical: Vertical;
    is_baseline: boolean;
    experiment_id: string | null;
    quality?: Record<string, unknown>;
  }) =>
    post<CaptureResult>("/api/captures", {
      quality: {
        face_present: true,
        yaw_degrees: 0,
        pitch_degrees: 0,
        distance_cm: 45,
        expression_neutral: true,
      },
      ...input,
    }),

  // --------------------------------------------------------------- routine
  searchProducts: (q = "") =>
    request<Product[]>(`/api/products/search?q=${encodeURIComponent(q)}`),

  lookupProduct: (barcode: string) =>
    request<Product | null>(`/api/products/lookup?barcode=${encodeURIComponent(barcode)}`),
  createProduct: (input: {
    name: string;
    category: string;
    stabilization_days: number;
    ingredients: string;
  }) => post<Product>("/api/products", input),

  ingredientExplainer: (productId: string, userId: string) =>
    request<IngredientExplainer>(
      `/api/products/${productId}/ingredient-explainer?user_id=${userId}`,
    ),

  /** Premium — ingredient-overlap similarity signal ahead of a purchase. */
  predictProduct: (productId: string, userId: string) =>
    request<ProductPrediction>(
      `/api/products/${productId}/predict?user_id=${userId}`,
    ),

  purchaseGuidance: (
    userId: string,
    input: {
      name?: string;
      barcode?: string;
      category?: string;
      ingredients?: string;
      price_cents?: number;
      currency?: string;
    },
  ) => post<PurchaseGuidance>(`/api/users/${userId}/purchase-guidance`, input),
  logRoutineEvent: (input: {
    user_id: string;
    product_id: string;
    action: string;
    slot: string;
    notes?: string | null;
    experiment_id?: string | null;
  }) => post<RoutineEvent>("/api/routine-events", input),

  /** Free — warns before a "start"/"change" would confound an in-progress
   *  stabilization window for another product. */
  confoundCheck: (userId: string, excludeProductId?: string) =>
    request<ConfoundCheck>(
      `/api/users/${userId}/confound-check${
        excludeProductId ? `?exclude_product_id=${excludeProductId}` : ""
      }`,
    ),

  experiments: (userId: string) =>
    request<Experiment[]>(`/api/users/${userId}/experiments`),

  createExperiment: (input: {
    user_id: string;
    name: string;
    hypothesis: string | null;
    product_id: string;
    primary_metric: MetricKey;
    target_days: number;
  }) => post<Experiment>("/api/experiments", input),

  setExperimentStatus: (userId: string, experimentId: string, status: string) =>
    post<Experiment>(
      `/api/users/${userId}/experiments/${experimentId}/status`,
      { user_id: userId, status },
    ),

  // -------------------------------------------------------------- insights
  qnaHistory: (userId: string) =>
    request<QnaTurn[]>(`/api/users/${userId}/qna`),

  ask: (userId: string, question: string) =>
    post<QnaTurn>(`/api/users/${userId}/qna`, { question }),

  triage: (text: string) => post<TriageResult>("/api/triage", { text }),

  // -------------------------------------------------------------- discover
  discover: (userId: string) =>
    request<{ recommendations: CohortRecommendation[] }>(
      `/api/users/${userId}/discover`,
    ),

  offers: (userId: string) =>
    request<Offer[]>(`/api/users/${userId}/commerce/offers`),

  clickOffer: (userId: string, offerId: string) =>
    post<{ url: string }>(
      `/api/users/${userId}/commerce/offers/${offerId}/click`,
    ),

  // ------------------------------------------------------------ shelf scan
  /** Free — photograph a shelf; returns a job id to poll. */
  scanShelf: (userId: string, image_base64: string) =>
    post<{ job_id: string; status: "queued" }>(
      `/api/users/${userId}/shelf-scan`,
      { image_base64 },
    ),

  shelfScanStatus: (userId: string, jobId: string) =>
    request<ShelfScanJob>(`/api/users/${userId}/shelf-scan/${jobId}`),

  confirmShelfScan: (
    userId: string,
    jobId: string,
    selections: ShelfScanSelection[],
  ) =>
    post<Product[]>(`/api/users/${userId}/shelf-scan/${jobId}/confirm`, {
      selections,
    }),

  // ------------------------------------------------------- context events
  contextEvents: (userId: string) =>
    request<ContextEvent[]>(`/api/users/${userId}/context-events`),

  addContextEvent: (
    userId: string,
    input: {
      event_type: ContextEventType;
      value?: string;
      occurred_at?: string;
      notes?: string;
    },
  ) => post<ContextEvent>(`/api/users/${userId}/context-events`, input),

  /** Premium — correlates logged context events with a chosen metric. */
  rootCause: (userId: string, metric: MetricKey) =>
    request<RootCauseCorrelation[]>(
      `/api/users/${userId}/root-cause?metric=${metric}`,
    ),

  // ---------------------------------------------------- budget optimizer
  /** Premium — products with no measurable effect, and what they cost. */
  budgetOptimizer: (userId: string) =>
    request<BudgetOptimizer>(`/api/users/${userId}/budget-optimizer`),

  // -------------------------------------------------------- derm export
  /** Premium — a printable measurement summary, not a diagnostic document. */
  dermExport: (userId: string) =>
    request<DermExport>(`/api/users/${userId}/derm-export`),
};

// ------------------------------------------------------------------ helpers

export const METRIC_LABELS: Record<MetricKey, string> = {
  redness_score: "Redness",
  blemish_count: "Blemish proxy",
  darkspot_area: "Dark spots",
  texture_score: "Texture",
};

export const VERDICT_COPY: Record<VerdictLabel, string> = {
  keep: "Keep",
  likely_useful: "Likely useful",
  evidence_unclear: "Evidence unclear",
  investigate: "Investigate",
  locked: "Locked",
};

/** Lower is better for every metric the deterministic model reports. */
export function formatMetric(key: MetricKey, value: number): string {
  return key === "blemish_count" ? value.toFixed(0) : value.toFixed(3);
}
