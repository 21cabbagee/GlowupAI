"use client";

import Link from "next/link";
import { useCallback, useEffect, useState } from "react";
import {
  METRIC_LABELS,
  api,
  formatMetric,
  type Dashboard,
  type MetricKey,
} from "@/lib/api";
import { useSession } from "@/lib/session";
import { HistoryTable, Sparkline, StreakRing, TrendChart } from "@/components/charts";
import {
  Button,
  ButtonLink,
  Card,
  CardTitle,
  EmptyState,
  Notice,
  PageHeading,
  Reveal,
  Skeleton,
  StatTile,
  Tag,
  VerdictTag,
  cx,
} from "@/components/ui";
import { CameraIcon, ChevronRightIcon, TrendIcon } from "@/components/icons";

const METRICS: MetricKey[] = [
  "redness_score",
  "blemish_count",
  "darkspot_area",
  "texture_score",
];

const vertical = "skin";

function evidenceLabel(value: number): string {
  if (value >= 0.75) return "strong";
  if (value >= 0.55) return "directional";
  return "limited";
}

export default function HomePage() {
  const { userId, isPremium, profile } = useSession();
  const [data, setData] = useState<Dashboard | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [metric, setMetric] = useState<MetricKey>("redness_score");
  const [showTable, setShowTable] = useState(false);

  const load = useCallback(async () => {
    if (!userId) return;
    setError(null);
    try {
      setData(await api.dashboard(userId, vertical));
    } catch (e) {
      setError(e instanceof Error ? e.message : "Could not load your dashboard");
    }
  }, [userId, vertical]);

  useEffect(() => {
    setData(null);
    load();
  }, [load]);

  if (error) {
    return (
      <>
        <PageHeading eyebrow="Your evidence" title="Measured, not remembered." />
        <Notice tone="error">{error}</Notice>
      </>
    );
  }

  if (!data) return <HomeSkeleton />;

  const { engagement, analytics, verdicts, history, routine_events } = data;
  const latest = history[history.length - 1];

  return (
    <>
      <PageHeading
        eyebrow={`${vertical} · your evidence`}
        title="Measured, not remembered."
        action={
          // Mobile already has the capture FAB in the tab bar.
          <span className="hidden lg:block">
            <ButtonLink href="/capture">
              <CameraIcon size={18} />
              Guided capture
            </ButtonLink>
          </span>
        }
      >
        Every number is compared against your own history — never a population
        average. Verdicts wait for stabilization windows and sample size.
      </PageHeading>

      {/* Streak + cadence guidance is the daily hook, so it leads. */}
      <Reveal>
        <Card className="flex flex-wrap items-center gap-5 sm:gap-7">
          <StreakRing streak={engagement.capture_streak} />
          <div className="min-w-[150px] flex-1">
            <Tag tone={engagement.guide.state === "due" ? "honey" : "neutral"}>
              {engagement.guide.state.replace(/_/g, " ")}
            </Tag>
            <p className="mt-2.5 text-[15px] leading-relaxed font-semibold">
              {engagement.guide.message}
            </p>
            <p className="mt-1 text-[12px] text-subtle">
              Next comparable window opens{" "}
              {new Date(engagement.guide.next_window_start).toLocaleString(undefined, {
                dateStyle: "medium",
                timeStyle: "short",
              })}
            </p>
          </div>
          <Link
            href="/capture"
            className="group flex items-center gap-1.5 text-[13px] font-bold text-honey-700 lg:hidden"
          >
            Capture now
            <ChevronRightIcon size={16} />
          </Link>
        </Card>
      </Reveal>

      <WeeklyRecapCard recap={data.weekly_recap} />
      <QuickCheckIn onSaved={load} />
      <div className="mt-4 grid grid-cols-2 gap-4 lg:grid-cols-4">
        <Reveal delay={0.04}>
          <StatTile
            accent
            value={engagement.capture_count}
            label="Accepted captures"
            hint="Frames that passed the quality gate"
          />
        </Reveal>
        <Reveal delay={0.08}>
          <StatTile
            value={analytics.median_history_days}
            label="History days"
            hint="Median span of your record"
          />
        </Reveal>
        <Reveal delay={0.12}>
          <StatTile
            value={verdicts.length}
            label="Live verdicts"
            hint="Products with an evidence state"
          />
        </Reveal>
        <Reveal delay={0.16}>
          <StatTile
            value={isPremium ? "Premium" : "Free"}
            label="Plan"
            hint={profile?.user.consent_state === "active" ? "Consent active" : "Consent pending"}
          />
        </Reveal>
      </div>

      {/* Hero chart + verdicts */}
      <div className="mt-4 grid gap-4 lg:grid-cols-12 lg:items-start">
        <Reveal delay={0.1} className="lg:col-span-7">
          <Card>
            <CardTitle
              action={
                <button
                  onClick={() => setShowTable((v) => !v)}
                  className="text-[12px] font-bold text-honey-700 hover:underline"
                >
                  {showTable ? "Show chart" : "Show table"}
                </button>
              }
            >
              {METRIC_LABELS[metric]} over time
            </CardTitle>

            {/* One metric at a time: the four live on different scales, so a
                single shared axis would be a lie. */}
            <div
              role="tablist"
              aria-label="Metric"
              className="no-scrollbar mb-3 flex gap-1.5 overflow-x-auto"
            >
              {METRICS.map((m) => (
                <button
                  key={m}
                  role="tab"
                  aria-selected={m === metric}
                  onClick={() => setMetric(m)}
                  className={cx(
                    "shrink-0 rounded-[var(--radius-full)] px-3 py-1.5 text-[12px] font-bold transition-colors",
                    m === metric
                      ? "bg-ink-900 text-paper"
                      : "border border-line text-muted hover:text-fg",
                  )}
                >
                  {METRIC_LABELS[m]}
                </button>
              ))}
            </div>

            {history.length === 0 ? (
              <EmptyState
                title="No captures yet"
                body="One calibrated frame starts the baseline. The second frame is the first comparison."
                action={
                  <ButtonLink href="/capture">Take the first capture</ButtonLink>
                }
              />
            ) : showTable ? (
              <div className="overflow-x-auto">
                <HistoryTable history={history} />
              </div>
            ) : (
              <TrendChart history={history} metric={metric} />
            )}
          </Card>
        </Reveal>

        <Reveal delay={0.14} className="lg:col-span-5">
          <Card className="h-full">
            <CardTitle
              action={
                !isPremium ? <Tag tone="honey">Premium</Tag> : undefined
              }
            >
              Weekly verdicts
            </CardTitle>
            {verdicts.length === 0 ? (
              <EmptyState
                title={
                  isPremium
                    ? "No verdict has cleared its evidence window"
                    : "Product verdicts are a Premium feature"
                }
                body={
                  isPremium
                    ? "Hold a product steady through its stabilization window and keep capturing. A verdict appears once there is enough post-stabilization signal."
                    : "Your weekly trend stays free. Product-level attribution needs the experiment engine."
                }
                action={
                  !isPremium ? (
                    <ButtonLink href="/account" variant="secondary">
                      See Premium
                    </ButtonLink>
                  ) : undefined
                }
              />
            ) : (
              <ul className="flex list-none flex-col gap-3 p-0">
                {verdicts.map((v, i) =>
                  v.label === "locked" ? (
                    <Card
                      as="li"
                      key={i}
                      className="relative overflow-hidden bg-surface-2 p-4"
                    >
                      <div className="flex items-start justify-between gap-3 blur-[3px] select-none">
                        <p className="font-bold">{v.product_name}</p>
                        <Tag>keep</Tag>
                      </div>
                      <div className="absolute inset-0 flex flex-col items-center justify-center gap-2 bg-surface-2/80 px-4 text-center backdrop-blur-[1px]">
                        <Tag tone="honey">Premium</Tag>
                        <p className="text-[12px] leading-relaxed text-muted">
                          {v.generated_text}
                        </p>
                        <ButtonLink href="/account" variant="secondary" size="sm">
                          Upgrade to Premium
                        </ButtonLink>
                      </div>
                    </Card>
                  ) : (
                    <Card as="li" key={i} className="bg-surface-2 p-4" interactive>
                      <div className="flex items-start justify-between gap-3">
                        <p className="font-bold">{v.product_name}</p>
                        <VerdictTag label={v.label} />
                      </div>
                      <p className="mt-2 text-[13px] leading-relaxed text-muted">
                        {v.generated_text}
                      </p>
                      <p className="mt-2 flex flex-wrap gap-x-3 text-[11px] text-subtle">
                        <span>{v.evidence?.n_after ?? 0} post-stabilization captures</span>
                        <span>
                          evidence strength{" "}
                          {v.evidence?.confidence != null
                            ? evidenceLabel(v.evidence.confidence)
                            : "pending"}
                        </span>
                      </p>
                    </Card>
                  ),
                )}
              </ul>
            )}
          </Card>
        </Reveal>
      </div>

      {/* Small multiples: each metric on its own scale */}
      {history.length > 1 && (
        <Reveal delay={0.16}>
          <Card className="mt-4">
            <CardTitle
              action={
                <span className="flex items-center gap-1.5 text-[12px] text-subtle">
                  <TrendIcon size={15} /> own scale each
                </span>
              }
            >
              All four metrics
            </CardTitle>
            <div className="grid grid-cols-2 gap-4 lg:grid-cols-4">
              {METRICS.map((m) => (
                <button
                  key={m}
                  onClick={() => setMetric(m)}
                  className={cx(
                    "rounded-[var(--radius)] border p-3 text-left transition-colors",
                    m === metric
                      ? "border-honey-400 bg-honey-50"
                      : "border-line hover:border-line-strong",
                  )}
                >
                  <p className="text-[11px] font-bold tracking-wide text-subtle uppercase">
                    {METRIC_LABELS[m]}
                  </p>
                  <p className="tnum display-sm mt-0.5 text-[20px]">
                    {latest ? formatMetric(m, Number(latest[m])) : "—"}
                  </p>
                  <div className="mt-2">
                    <Sparkline history={history} metric={m} />
                  </div>
                </button>
              ))}
            </div>
          </Card>
        </Reveal>
      )}

      {/* Recent routine */}
      <Reveal delay={0.18}>
        <Card className="mt-4">
          <CardTitle
            action={
              <Link
                href="/routine"
                className="flex items-center gap-1 text-[12px] font-bold text-honey-700 hover:underline"
              >
                Open routine <ChevronRightIcon size={14} />
              </Link>
            }
          >
            Recent routine
          </CardTitle>
          {routine_events.length === 0 ? (
            <EmptyState
              title="No routine events logged"
              body="Products become variables only once they are timestamped. Log what you actually used."
              action={
                <ButtonLink href="/routine" variant="secondary">
                  Log a product
                </ButtonLink>
              }
            />
          ) : (
            <ol className="relative m-0 list-none p-0 pl-5">
              <span
                aria-hidden
                className="absolute top-2 bottom-2 left-1 w-px bg-line"
              />
              {routine_events.slice(0, 8).map((e, i) => (
                <li key={i} className="relative pb-4 last:pb-0">
                  <span
                    aria-hidden
                    className="absolute top-1.5 -left-[17px] size-2.5 rounded-full border-2 border-honey-500 bg-surface"
                  />
                  <p className="text-[14px] font-semibold">
                    {e.action} · {e.product_name}
                  </p>
                  <p className="text-[12px] text-subtle">
                    {new Date(e.timestamp).toLocaleString(undefined, {
                      dateStyle: "medium",
                      timeStyle: "short",
                    })}{" "}
                    · {e.slot}
                  </p>
                </li>
              ))}
            </ol>
          )}
        </Card>
      </Reveal>

      <p className="mt-6 text-[12px] leading-relaxed text-subtle">
        SkinProof tracks cosmetic appearance. It does not diagnose, treat, or rule
        out any medical condition.
      </p>
    </>
  );
}

function WeeklyRecapCard({ recap }: { recap: Dashboard["weekly_recap"] }) {
  return (
    <Card className="mt-4 border-honey-200 bg-honey-50">
      <CardTitle action={<Tag tone="honey">weekly read</Tag>}>What changed recently</CardTitle>
      <h2 className="display-sm text-[20px]">{recap.headline}</h2>
      <p className="mt-2 max-w-2xl text-[13.5px] leading-relaxed text-muted">{recap.body}</p>
      {recap.metric_summaries.length > 0 && (
        <div className="mt-4 grid gap-2 sm:grid-cols-2 lg:grid-cols-4">
          {recap.metric_summaries.map((item) => (
            <div key={item.metric} className="rounded-[var(--radius)] border border-honey-200 bg-surface/70 p-3">
              <div className="flex items-center justify-between gap-2">
                <span className="text-[11px] font-bold uppercase tracking-wide text-subtle">{item.label}</span>
                <Tag tone={item.direction === "improved" ? "likely_useful" : item.direction === "increased" ? "investigate" : "neutral"}>{item.direction}</Tag>
              </div>
              <p className="mt-2 text-[12px] leading-relaxed text-muted">{item.sentence}</p>
            </div>
          ))}
        </div>
      )}
      <div className="mt-4 flex flex-wrap items-center justify-between gap-3 border-t border-honey-200 pt-3">
        <p className="text-[12px] font-semibold text-honey-800">{recap.next_action}</p>
        <span className="text-[11px] text-subtle">{recap.confidence_label} · {recap.check_in_count} check-in{recap.check_in_count === 1 ? "" : "s"}</span>
      </div>
    </Card>
  );
}

function QuickCheckIn({ onSaved }: { onSaved: () => Promise<void> }) {
  const { userId } = useSession();
  const [skinFeel, setSkinFeel] = useState<"better" | "same" | "worse" | "not_sure">("same");
  const [routineState, setRoutineState] = useState<"steady" | "changed" | "missed" | "not_sure">("steady");
  const [saving, setSaving] = useState(false);
  const [saved, setSaved] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const submit = async () => {
    if (!userId || saving) return;
    setSaving(true);
    setError(null);
    try {
      await api.createCheckIn(userId, { routine_state: routineState, skin_feel: skinFeel });
      setSaved(true);
      await onSaved();
    } catch (e) {
      setError(e instanceof Error ? e.message : "Could not save your check-in");
    } finally {
      setSaving(false);
    }
  };

  return (
    <Card className="mt-4">
      <CardTitle action={<Tag>10 seconds</Tag>}>Quick check-in</CardTitle>
      <p className="text-[13px] leading-relaxed text-muted">A self-report adds context between standardized captures. It does not replace the measurement.</p>
      <div className="mt-4 grid gap-4 lg:grid-cols-2">
        <div>
          <p className="mb-2 text-[12px] font-bold tracking-wide text-muted">How does your skin feel?</p>
          <div className="flex flex-wrap gap-2">
            {(["better", "same", "worse", "not_sure"] as const).map((value) => (
              <button key={value} type="button" aria-pressed={skinFeel === value} onClick={() => setSkinFeel(value)} className={cx("rounded-[var(--radius-full)] border px-3 py-2 text-[12px] font-semibold capitalize", skinFeel === value ? "border-ink-900 bg-ink-900 text-paper" : "border-line text-muted hover:border-line-strong")}>{value.replace("_", " ")}</button>
            ))}
          </div>
        </div>
        <div>
          <p className="mb-2 text-[12px] font-bold tracking-wide text-muted">Was your routine steady?</p>
          <div className="flex flex-wrap gap-2">
            {(["steady", "changed", "missed", "not_sure"] as const).map((value) => (
              <button key={value} type="button" aria-pressed={routineState === value} onClick={() => setRoutineState(value)} className={cx("rounded-[var(--radius-full)] border px-3 py-2 text-[12px] font-semibold capitalize", routineState === value ? "border-ink-900 bg-ink-900 text-paper" : "border-line text-muted hover:border-line-strong")}>{value.replace("_", " ")}</button>
            ))}
          </div>
        </div>
      </div>
      {error && <div className="mt-3"><Notice tone="error">{error}</Notice></div>}
      <div className="mt-4 flex flex-wrap items-center gap-3">
        <Button onClick={submit} loading={saving}>{saved ? "Saved" : "Save check-in"}</Button>
        {saved && <span className="text-[12px] font-semibold text-useful">Added to this week’s context.</span>}
      </div>
    </Card>
  );
}

function HomeSkeleton() {
  return (
    <>
      <div className="mb-7">
        <Skeleton className="h-3 w-28" />
        <Skeleton className="mt-3 h-10 w-80 max-w-full" />
      </div>
      <Skeleton className="h-[148px] rounded-[var(--radius-lg)]" />
      <div className="mt-4 grid grid-cols-2 gap-4 lg:grid-cols-4">
        {[0, 1, 2, 3].map((i) => (
          <Skeleton key={i} className="h-[124px] rounded-[var(--radius-lg)]" />
        ))}
      </div>
      <div className="mt-4 grid gap-4 lg:grid-cols-12">
        <Skeleton className="h-[360px] rounded-[var(--radius-lg)] lg:col-span-7" />
        <Skeleton className="h-[360px] rounded-[var(--radius-lg)] lg:col-span-5" />
      </div>
    </>
  );
}
