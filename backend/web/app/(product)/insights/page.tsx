"use client";

import { useCallback, useEffect, useRef, useState, type FormEvent } from "react";
import {
  CONTEXT_EVENT_TYPES,
  METRIC_LABELS,
  api,
  type BudgetOptimizer,
  type ContextEvent,
  type ContextEventType,
  type IngredientExplainer,
  type MetricKey,
  type Product,
  type QnaTurn,
  type RootCauseCorrelation,
} from "@/lib/api";
import { useSession } from "@/lib/session";
import {
  Button,
  Card,
  CardTitle,
  EmptyState,
  Field,
  Notice,
  PageHeading,
  Reveal,
  Skeleton,
  Tag,
  cx,
  inputClass,
  PaywallCard,
} from "@/components/ui";
import { InsightIcon, ShieldIcon, SparkIcon } from "@/components/icons";

function formatCiteDate(date: string): string {
  const d = new Date(date);
  if (Number.isNaN(d.getTime())) return date;
  return d.toLocaleDateString(undefined, { dateStyle: "medium" });
}

/** Cosmetic-tracking results are informational; medical handoffs are alerts. */
function isCosmeticScope(scope: string): boolean {
  return scope === "cosmetic_tracking";
}

export default function InsightsPage() {
  return (
    <>
      <PageHeading
        eyebrow="grounded assistant"
        title="Ask your own history."
      >
        Answers cite your own structured captures and routine events — never
        raw photos, and never an invented label. Questions outside cosmetic
        scope are routed to a dermatologist.
      </PageHeading>

      <div className="grid gap-4 lg:grid-cols-12">
        <Reveal delay={0} className="lg:col-span-7">
          <ChatPanel />
        </Reveal>

        <div className="flex flex-col gap-4 lg:col-span-5">
          <Reveal delay={0.06}>
            <IngredientCard />
          </Reveal>
          <Reveal delay={0.12}>
            <TriageCard />
          </Reveal>
        </div>
      </div>

      <div className="mt-4 grid gap-4 lg:grid-cols-2">
        <Reveal delay={0.16}>
          <ContextCard />
        </Reveal>
        <Reveal delay={0.2}>
          <BudgetOptimizerCard />
        </Reveal>
      </div>
    </>
  );
}

/* ------------------------------------------------------------------ Chat */

function ChatPanel() {
  const { userId, isPremium, refresh } = useSession();
  const [history, setHistory] = useState<QnaTurn[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [question, setQuestion] = useState("");
  const [sending, setSending] = useState(false);
  const [upgrading, setUpgrading] = useState(false);
  const listRef = useRef<HTMLDivElement | null>(null);

  const load = useCallback(async () => {
    if (!userId) return;
    setError(null);
    try {
      setHistory(await api.qnaHistory(userId));
    } catch (e) {
      setError(e instanceof Error ? e.message : "Could not load your history");
    }
  }, [userId]);

  useEffect(() => {
    if (isPremium) load();
  }, [load, isPremium]);

  useEffect(() => {
    const el = listRef.current;
    if (el) el.scrollTop = el.scrollHeight;
  }, [history]);

  const onSubmit = useCallback(
    async (e: FormEvent) => {
      e.preventDefault();
      if (!userId || !question.trim() || sending) return;
      setSending(true);
      setError(null);
      try {
        await api.ask(userId, question.trim());
        setQuestion("");
        await load();
      } catch (e) {
        setError(e instanceof Error ? e.message : "Could not send that question");
      } finally {
        setSending(false);
      }
    },
    [userId, question, sending, load],
  );

  const onUnlock = useCallback(async () => {
    if (!userId) return;
    setUpgrading(true);
    try {
      await api.upgrade(userId);
      await refresh();
    } catch {
      // The paywall card has no error slot; a stale plan state is enough
      // signal for the user to retry.
    } finally {
      setUpgrading(false);
    }
  }, [userId, refresh]);

  if (!isPremium) {
    return (
      <PaywallCard
        title="Ask your own history"
        body="Premium unlocks a grounded assistant that answers only from your captures and routine events — with citations, never guesses."
        onUnlock={onUnlock}
        pending={upgrading}
      />
    );
  }

  return (
    <Card className="flex h-[560px] flex-col">
      <CardTitle action={<Tag tone="honey">Grounded</Tag>}>
        <span className="inline-flex items-center gap-2">
          <InsightIcon size={18} />
          Your Q&amp;A
        </span>
      </CardTitle>

      {error && (
        <div className="mb-3">
          <Notice tone="error">{error}</Notice>
        </div>
      )}

      <div
        ref={listRef}
        className="min-h-0 flex-1 overflow-y-auto rounded-[var(--radius)] bg-surface-2/40 p-3"
      >
        {history === null ? (
          <div className="flex flex-col gap-3">
            <Skeleton className="h-14 w-2/3 rounded-[var(--radius-lg)]" />
            <Skeleton className="ml-auto h-10 w-1/2 rounded-[var(--radius-lg)]" />
          </div>
        ) : history.length === 0 ? (
          <EmptyState
            title="No questions yet"
            body="Ask something like “has redness changed since I started the new serum?” Answers cite your captures and routine log."
          />
        ) : (
          <ul className="m-0 flex list-none flex-col gap-3 p-0">
            {history.map((turn, i) => (
              <li
                key={i}
                className={cx(
                  "flex",
                  turn.role === "user" ? "justify-end" : "justify-start",
                )}
              >
                <div className="max-w-[85%]">
                  <div
                    className={cx(
                      "rounded-[var(--radius-lg)] px-4 py-2.5 text-[14px] leading-relaxed",
                      turn.role === "user"
                        ? "bg-primary text-on-primary"
                        : "bg-surface-2 text-fg",
                    )}
                  >
                    {turn.content}
                  </div>
                  {turn.role === "assistant" &&
                    turn.citations &&
                    turn.citations.length > 0 && (
                      <div className="mt-1.5 flex flex-wrap gap-1.5">
                        {turn.citations.map((c, ci) => (
                          <Tag key={ci} tone="neutral">
                            {formatCiteDate(c.date)}
                          </Tag>
                        ))}
                      </div>
                    )}
                </div>
              </li>
            ))}
          </ul>
        )}
      </div>

      <form onSubmit={onSubmit} className="mt-3 flex gap-2">
        <input
          className={cx(inputClass, "flex-1")}
          value={question}
          onChange={(e) => setQuestion(e.target.value)}
          disabled={sending}
        />
        <Button type="submit" loading={sending} disabled={!question.trim()}>
          Send
        </Button>
      </form>
    </Card>
  );
}

/* ------------------------------------------------------------ Ingredients */

function IngredientCard() {
  const { userId } = useSession();
  const [products, setProducts] = useState<Product[] | null>(null);
  const [productsError, setProductsError] = useState<string | null>(null);
  const [selectedId, setSelectedId] = useState("");
  const [explainer, setExplainer] = useState<IngredientExplainer | null>(null);
  const [analyzing, setAnalyzing] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    api
      .searchProducts("")
      .then(setProducts)
      .catch((e) =>
        setProductsError(
          e instanceof Error ? e.message : "Could not load products",
        ),
      );
  }, []);

  const onAnalyze = useCallback(async () => {
    if (!userId || !selectedId) return;
    setAnalyzing(true);
    setError(null);
    try {
      setExplainer(await api.ingredientExplainer(selectedId, userId));
    } catch (e) {
      setError(
        e instanceof Error ? e.message : "Could not analyze this product",
      );
    } finally {
      setAnalyzing(false);
    }
  }, [userId, selectedId]);

  return (
    <Card>
      <CardTitle>
        <span className="inline-flex items-center gap-2">
          <SparkIcon size={18} />
          Ingredient intelligence
        </span>
      </CardTitle>

      {productsError && (
        <div className="mb-3">
          <Notice tone="error">{productsError}</Notice>
        </div>
      )}

      <Field label="Product" className="mb-3">
        <select
          className={inputClass}
          value={selectedId}
          onChange={(e) => {
            setSelectedId(e.target.value);
            setExplainer(null);
          }}
        >
          <option value="">Choose a product…</option>
          {(products ?? []).map((p) => (
            <option key={p.id} value={p.id}>
              {p.name}
            </option>
          ))}
        </select>
      </Field>

      <Button
        variant="secondary"
        onClick={onAnalyze}
        loading={analyzing}
        disabled={!selectedId}
        block
      >
        Analyze ingredients
      </Button>

      {error && (
        <div className="mt-3">
          <Notice tone="error">{error}</Notice>
        </div>
      )}

      <div className="mt-4">
        {!explainer ? (
          <EmptyState
            title="No analysis yet"
            body="Pick a product and analyze it to see what its ingredients do, and what we cannot yet explain."
          />
        ) : (
          <div className="flex flex-col gap-4">
            <p className="font-bold">{explainer.product_name}</p>

            {explainer.reviewed.length > 0 && (
              <ul className="m-0 flex list-none flex-col gap-3 p-0">
                {explainer.reviewed.map((r, i) => (
                  <li
                    key={i}
                    className="rounded-[var(--radius)] border border-line bg-surface-2 p-3"
                  >
                    <p className="font-bold">{r.ingredient}</p>
                    <p className="mt-1 text-[13px] leading-relaxed text-fg">
                      {r.purpose}
                    </p>
                    {r.caution && (
                      <p className="mt-1 text-[12px] leading-relaxed text-muted">
                        {r.caution}
                      </p>
                    )}
                  </li>
                ))}
              </ul>
            )}

            {explainer.unknown.length > 0 && (
              <div className="rounded-[var(--radius)] border border-dashed border-line-strong p-3">
                <p className="text-[12px] font-bold tracking-wide text-subtle uppercase">
                  Not yet explainable
                </p>
                <div className="mt-2 flex flex-wrap gap-1.5">
                  {explainer.unknown.map((u, i) => (
                    <Tag key={i} tone="neutral">
                      {u}
                    </Tag>
                  ))}
                </div>
              </div>
            )}
          </div>
        )}
      </div>
    </Card>
  );
}

/* ----------------------------------------------------------------- Triage */

function TriageCard() {
  const [text, setText] = useState("");
  const [result, setResult] = useState<{ scope: string; message: string } | null>(
    null,
  );
  const [triaging, setTriaging] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const onCheck = useCallback(async () => {
    if (!text.trim() || triaging) return;
    setTriaging(true);
    setError(null);
    try {
      setResult(await api.triage(text.trim()));
    } catch (e) {
      setError(e instanceof Error ? e.message : "Could not run triage");
    } finally {
      setTriaging(false);
    }
  }, [text, triaging]);

  return (
    <Card>
      <CardTitle>
        <span className="inline-flex items-center gap-2">
          <ShieldIcon size={18} />
          Safety triage
        </span>
      </CardTitle>

      <Field label="Describe the concern">
        <textarea
          className={cx(inputClass, "min-h-[96px] resize-y")}
          value={text}
          onChange={(e) => setText(e.target.value)}
        />
      </Field>

      <Button
        className="mt-3"
        variant="secondary"
        onClick={onCheck}
        loading={triaging}
        disabled={!text.trim()}
        block
      >
        Check
      </Button>

      {error && (
        <div className="mt-3">
          <Notice tone="error">{error}</Notice>
        </div>
      )}

      {result && (
        <div className="mt-3">
          <Notice tone={isCosmeticScope(result.scope) ? "info" : "error"}>
            {result.message}
          </Notice>
        </div>
      )}

      <p className="mt-3 text-[12px] leading-relaxed text-subtle">
        Pain, bleeding, changing moles, infection, or wounds belong with a
        qualified dermatologist.
      </p>
    </Card>
  );
}

/* --------------------------------------------------- Context / root-cause */

const METRICS: MetricKey[] = Object.keys(METRIC_LABELS) as MetricKey[];

function ContextCard() {
  const { userId, isPremium, refresh } = useSession();

  const [events, setEvents] = useState<ContextEvent[] | null>(null);
  const [eventType, setEventType] = useState<ContextEventType>("sleep");
  const [value, setValue] = useState("");
  const [occurredAt, setOccurredAt] = useState("");
  const [notes, setNotes] = useState("");
  const [logging, setLogging] = useState(false);
  const [logError, setLogError] = useState<string | null>(null);

  const [metric, setMetric] = useState<MetricKey>("texture_score");
  const [correlations, setCorrelations] = useState<RootCauseCorrelation[] | null>(null);
  const [searching, setSearching] = useState(false);
  const [searchError, setSearchError] = useState<string | null>(null);
  const [upgrading, setUpgrading] = useState(false);

  const loadEvents = useCallback(async () => {
    if (!userId) return;
    try {
      setEvents(await api.contextEvents(userId));
    } catch {
      setEvents([]);
    }
  }, [userId]);

  useEffect(() => {
    loadEvents();
  }, [loadEvents]);

  const onLog = useCallback(async () => {
    if (!userId) return;
    setLogging(true);
    setLogError(null);
    try {
      await api.addContextEvent(userId, {
        event_type: eventType,
        value: value.trim() ? value.trim() : undefined,
        occurred_at: occurredAt ? new Date(occurredAt).toISOString() : undefined,
        notes: notes.trim() ? notes.trim() : undefined,
      });
      setValue("");
      setNotes("");
      await loadEvents();
    } catch (e) {
      setLogError(e instanceof Error ? e.message : "Could not log that event");
    } finally {
      setLogging(false);
    }
  }, [userId, eventType, value, occurredAt, notes, loadEvents]);

  const onSearch = useCallback(async () => {
    if (!userId) return;
    setSearching(true);
    setSearchError(null);
    try {
      setCorrelations(await api.rootCause(userId, metric));
    } catch (e) {
      setSearchError(e instanceof Error ? e.message : "Could not run root-cause search");
    } finally {
      setSearching(false);
    }
  }, [userId, metric]);

  const onUnlock = useCallback(async () => {
    if (!userId) return;
    setUpgrading(true);
    try {
      await api.upgrade(userId);
      await refresh();
    } finally {
      setUpgrading(false);
    }
  }, [userId, refresh]);

  return (
    <Card>
      <CardTitle>Context &amp; root-cause</CardTitle>

      <div className="grid gap-2.5 sm:grid-cols-2">
        <Field label="Event">
          <select
            className={inputClass}
            value={eventType}
            onChange={(e) => setEventType(e.target.value as ContextEventType)}
          >
            {CONTEXT_EVENT_TYPES.map((t) => (
              <option key={t} value={t}>
                {t}
              </option>
            ))}
          </select>
        </Field>
        <Field label="Date" hint="Optional — defaults to now">
          <input
            type="date"
            className={inputClass}
            value={occurredAt}
            onChange={(e) => setOccurredAt(e.target.value)}
          />
        </Field>
      </div>
      <Field label="Value" className="mt-2.5" hint="Free text — e.g. '6 hours', 'flight to Denver'">
        <input className={inputClass} value={value} onChange={(e) => setValue(e.target.value)} />
      </Field>
      <Field label="Notes" className="mt-2.5" hint="Optional">
        <input className={inputClass} value={notes} onChange={(e) => setNotes(e.target.value)} />
      </Field>
      {logError && (
        <div className="mt-3">
          <Notice tone="error">{logError}</Notice>
        </div>
      )}
      <Button className="mt-3" variant="secondary" onClick={onLog} loading={logging}>
        Log event
      </Button>

      {events && events.length > 0 && (
        <div className="mt-4 flex flex-wrap gap-1.5">
          {events.slice(0, 8).map((e) => (
            <Tag key={e.id}>
              {e.event_type}
              {e.value ? `: ${e.value}` : ""}
            </Tag>
          ))}
        </div>
      )}

      <div className="mt-5 border-t border-line pt-4">
        <p className="mb-3 text-[13px] font-bold">Root-cause search</p>
        {!isPremium ? (
          <PaywallCard
            title="Root-cause search"
            body="Premium correlates your logged context events (sleep, travel, weather, stress, diet, cycle) with your measured metrics — a starting hypothesis for a real experiment, never a proven cause."
            onUnlock={onUnlock}
            pending={upgrading}
          />
        ) : (
          <>
            <div className="flex flex-wrap items-end gap-2.5">
              <Field label="Metric" className="min-w-[160px]">
                <select
                  className={inputClass}
                  value={metric}
                  onChange={(e) => setMetric(e.target.value as MetricKey)}
                >
                  {METRICS.map((m) => (
                    <option key={m} value={m}>
                      {METRIC_LABELS[m]}
                    </option>
                  ))}
                </select>
              </Field>
              <Button variant="secondary" onClick={onSearch} loading={searching}>
                Find patterns
              </Button>
            </div>
            {searchError && (
              <div className="mt-3">
                <Notice tone="error">{searchError}</Notice>
              </div>
            )}
            <div className="mt-3">
              {correlations === null ? null : correlations.length === 0 ? (
                <EmptyState
                  title="No repeatable pattern yet"
                  body="Log more context events and keep capturing consistently — root-cause search needs enough overlapping history to find a signal."
                />
              ) : (
                <ul className="flex list-none flex-col gap-2.5 p-0">
                  {correlations.map((c, i) => (
                    <li
                      key={i}
                      className="rounded-[var(--radius)] border border-line bg-surface-2 p-3"
                    >
                      <div className="flex items-center justify-between gap-2">
                        <Tag>{c.event_type}</Tag>
                        <span className="text-[11px] text-subtle">
                          {c.occurrences} occurrences
                        </span>
                      </div>
                      <p className="mt-2 text-[13px] leading-relaxed text-muted">
                        {c.message}
                      </p>
                    </li>
                  ))}
                </ul>
              )}
            </div>
          </>
        )}
      </div>
    </Card>
  );
}

/* --------------------------------------------------------- Budget optimizer */

function formatCents(cents: number, currency: string): string {
  try {
    return new Intl.NumberFormat(undefined, { style: "currency", currency }).format(
      cents / 100,
    );
  } catch {
    return `${(cents / 100).toFixed(2)} ${currency}`;
  }
}

function BudgetOptimizerCard() {
  const { userId, isPremium, refresh } = useSession();
  const [data, setData] = useState<BudgetOptimizer | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [upgrading, setUpgrading] = useState(false);

  useEffect(() => {
    if (!userId || !isPremium) return;
    api
      .budgetOptimizer(userId)
      .then(setData)
      .catch((e) => setError(e instanceof Error ? e.message : "Could not load the optimizer"));
  }, [userId, isPremium]);

  const onUnlock = useCallback(async () => {
    if (!userId) return;
    setUpgrading(true);
    try {
      await api.upgrade(userId);
      await refresh();
    } finally {
      setUpgrading(false);
    }
  }, [userId, refresh]);

  if (!isPremium) {
    return (
      <PaywallCard
        title="Routine budget optimizer"
        body="Premium flags products with no measurable effect beyond your capture noise floor, and estimates what they are costing you a year."
        onUnlock={onUnlock}
        pending={upgrading}
      />
    );
  }

  return (
    <Card>
      <CardTitle action={<Tag tone="honey">Premium</Tag>}>Budget optimizer</CardTitle>
      {error && <Notice tone="error">{error}</Notice>}
      {!data ? (
        <p className="text-[13px] text-muted">Loading…</p>
      ) : data.flagged.length === 0 ? (
        <EmptyState
          title="Nothing flagged"
          body="Every product held long enough to judge is showing a measurable effect, or none has been stable for 30+ days yet."
        />
      ) : (
        <>
          <p className="text-[13px] leading-relaxed text-muted">
            Estimated annual waste:{" "}
            <span className="font-bold text-fg">
              {formatCents(data.estimated_annual_waste_cents, data.currency)}
            </span>
          </p>
          <ul className="mt-3 flex list-none flex-col gap-2.5 p-0">
            {data.flagged.map((f) => (
              <li
                key={f.product_id}
                className="rounded-[var(--radius)] border border-line bg-surface-2 p-3"
              >
                <div className="flex items-start justify-between gap-3">
                  <p className="font-bold">{f.product_name}</p>
                  {f.estimated_annual_cost_cents != null && (
                    <span className="tnum text-[13px] font-semibold">
                      {formatCents(f.estimated_annual_cost_cents, f.currency)}/yr
                    </span>
                  )}
                </div>
                <p className="mt-1 text-[12px] leading-relaxed text-muted">{f.reason}</p>
                <p className="mt-1 text-[11px] text-subtle">
                  {f.days_stable} days stable
                </p>
              </li>
            ))}
          </ul>
          <p className="mt-3 text-[12px] text-subtle">{data.disclaimer}</p>
        </>
      )}
    </Card>
  );
}
