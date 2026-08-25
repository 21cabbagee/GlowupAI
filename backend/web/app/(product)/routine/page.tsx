"use client";

import { useCallback, useEffect, useRef, useState } from "react";
import {
  METRIC_LABELS,
  api,
  type ConfoundCheck,
  type Dashboard,
  type Experiment,
  type MetricKey,
  type Product,
  type ProductPrediction,
  type PurchaseGuidance,
  type ShelfScanCandidate,
  type ShelfScanJob,
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
  PaywallCard,
  Reveal,
  Skeleton,
  Tag,
  inputClass,
} from "@/components/ui";
import { PlusIcon, RoutineIcon } from "@/components/icons";

const CATEGORIES = [
  "cleanser",
  "moisturizer",
  "serum",
  "sunscreen",
  "retinoid",
  "other",
] as const;

/* The service accepts exactly these three actions: a routine event marks a
   change to the variable, not a daily tick. */
const ACTIONS = [
  { value: "start", label: "Started using" },
  { value: "change", label: "Changed how I use it" },
  { value: "stop", label: "Stopped using" },
] as const;

type Action = (typeof ACTIONS)[number]["value"];
const SLOTS = ["morning", "evening", "unspecified"] as const;
const METRICS = Object.keys(METRIC_LABELS) as MetricKey[];
const vertical = "skin";

export default function RoutinePage() {
  const { userId, isPremium, refresh } = useSession();

  const [dashboard, setDashboard] = useState<Dashboard | null>(null);
  const [products, setProducts] = useState<Product[] | null>(null);
  const [experiments, setExperiments] = useState<Experiment[] | null>(null);
  const [loadError, setLoadError] = useState<string | null>(null);

  // ------------------------------------------------------------ add product
  const [productName, setProductName] = useState("");
  const [productCategory, setProductCategory] =
    useState<(typeof CATEGORIES)[number]>("cleanser");
  const [productStabilization, setProductStabilization] = useState(14);
  const [productIngredients, setProductIngredients] = useState("");
  const [productSubmitting, setProductSubmitting] = useState(false);
  const [productError, setProductError] = useState<string | null>(null);

  // -------------------------------------------------------------- log event
  const [eventProductId, setEventProductId] = useState("");
  const [eventAction, setEventAction] = useState<Action>("start");
  const [eventSlot, setEventSlot] = useState<(typeof SLOTS)[number]>("morning");
  const [eventNotes, setEventNotes] = useState("");
  const [eventExperimentId, setEventExperimentId] = useState("");
  const [eventSubmitting, setEventSubmitting] = useState(false);
  const [eventError, setEventError] = useState<string | null>(null);
  const [proactiveConfound, setProactiveConfound] = useState<ConfoundCheck | null>(null);
  const [postSubmitConfound, setPostSubmitConfound] = useState<ConfoundCheck | null>(null);

  // ------------------------------------------------------------- experiment
  const [expName, setExpName] = useState("");
  const [expHypothesis, setExpHypothesis] = useState("");
  const [expProductId, setExpProductId] = useState("");
  const [expMetric, setExpMetric] = useState<MetricKey>(METRICS[0]);
  const [expTargetDays, setExpTargetDays] = useState(14);
  const [expSubmitting, setExpSubmitting] = useState(false);
  const [expError, setExpError] = useState<string | null>(null);
  const [unlockPending, setUnlockPending] = useState(false);
  const [unlockError, setUnlockError] = useState<string | null>(null);

  const [completingId, setCompletingId] = useState<string | null>(null);
  const [completeError, setCompleteError] = useState<string | null>(null);

  // ---------------------------------------------------------- shelf scan
  const shelfFileRef = useRef<HTMLInputElement>(null);
  const [shelfOpen, setShelfOpen] = useState(false);
  const [shelfJob, setShelfJob] = useState<ShelfScanJob | null>(null);
  const [shelfError, setShelfError] = useState<string | null>(null);
  const [shelfSelections, setShelfSelections] = useState<
    (ShelfScanCandidate & { checked: boolean; stabilization_days: number })[]
  >([]);
  const [shelfConfirming, setShelfConfirming] = useState(false);

  // ------------------------------------------------------ predict-before-buy
  const [predictProductId, setPredictProductId] = useState<string | null>(null);
  const [predictResult, setPredictResult] = useState<ProductPrediction | null>(null);
  const [predictLoading, setPredictLoading] = useState(false);
  const [predictError, setPredictError] = useState<string | null>(null);

  const load = useCallback(async () => {
    if (!userId) return;
    setLoadError(null);
    try {
      const [dash, prods] = await Promise.all([
        api.dashboard(userId, vertical),
        api.searchProducts(""),
      ]);
      setDashboard(dash);
      setProducts(prods);
      if (isPremium) {
        setExperiments(await api.experiments(userId));
      } else {
        setExperiments([]);
      }
    } catch (e) {
      setLoadError(
        e instanceof Error ? e.message : "Could not load your routine",
      );
    }
  }, [userId, vertical, isPremium]);

  useEffect(() => {
    setDashboard(null);
    setProducts(null);
    setExperiments(null);
    load();
  }, [load]);

  // Proactively warn when a start/change would overlap another product's
  // in-progress stabilization window, before the user submits.
  useEffect(() => {
    if (!userId || !eventProductId || eventAction === "stop") {
      setProactiveConfound(null);
      return;
    }
    let cancelled = false;
    api
      .confoundCheck(userId, eventProductId)
      .then((res) => {
        if (!cancelled) setProactiveConfound(res.confounded ? res : null);
      })
      .catch(() => {
        if (!cancelled) setProactiveConfound(null);
      });
    return () => {
      cancelled = true;
    };
  }, [userId, eventProductId, eventAction]);

  const handleAddProduct = async () => {
    if (!productName.trim()) {
      setProductError("Name your product first.");
      return;
    }
    setProductSubmitting(true);
    setProductError(null);
    try {
      await api.createProduct({
        name: productName.trim(),
        category: productCategory,
        stabilization_days: productStabilization,
        ingredients: productIngredients,
      });
      setProductName("");
      setProductIngredients("");
      setProductStabilization(14);
      setProductCategory("cleanser");
      await load();
    } catch (e) {
      setProductError(
        e instanceof Error ? e.message : "Could not add the product",
      );
    } finally {
      setProductSubmitting(false);
    }
  };

  const handleLogEvent = async () => {
    if (!userId) return;
    if (!eventProductId) {
      setEventError("Pick a product to log against.");
      return;
    }
    setEventSubmitting(true);
    setEventError(null);
    setPostSubmitConfound(null);
    try {
      const created = await api.logRoutineEvent({
        user_id: userId,
        product_id: eventProductId,
        action: eventAction,
        slot: eventSlot,
        notes: eventNotes.trim() ? eventNotes.trim() : null,
        experiment_id: eventExperimentId ? eventExperimentId : null,
      });
      setEventNotes("");
      setPostSubmitConfound(created.confound_warning ?? null);
      await load();
    } catch (e) {
      setEventError(
        e instanceof Error ? e.message : "Could not log that routine event",
      );
    } finally {
      setEventSubmitting(false);
    }
  };

  const handleCreateExperiment = async () => {
    if (!userId) return;
    if (!expName.trim() || !expProductId) {
      setExpError("Name the experiment and choose the product being tested.");
      return;
    }
    setExpSubmitting(true);
    setExpError(null);
    try {
      await api.createExperiment({
        user_id: userId,
        name: expName.trim(),
        hypothesis: expHypothesis.trim() ? expHypothesis.trim() : null,
        product_id: expProductId,
        primary_metric: expMetric,
        target_days: expTargetDays,
      });
      setExpName("");
      setExpHypothesis("");
      setExpTargetDays(14);
      await load();
    } catch (e) {
      setExpError(
        e instanceof Error ? e.message : "Could not create the experiment",
      );
    } finally {
      setExpSubmitting(false);
    }
  };

  const handleUnlock = async () => {
    if (!userId) return;
    setUnlockPending(true);
    setUnlockError(null);
    try {
      await api.upgrade(userId);
      await refresh();
    } catch (e) {
      setUnlockError(
        e instanceof Error ? e.message : "Could not complete the upgrade",
      );
    } finally {
      setUnlockPending(false);
    }
  };

  // ---------------------------------------------------------- shelf scan
  const pollShelfJob = useCallback(async (jobId: string) => {
    if (!userId) return;
    try {
      const job = await api.shelfScanStatus(userId, jobId);
      setShelfJob(job);
      if (job.status === "completed") {
        setShelfSelections(
          (job.result?.candidates ?? []).map((c) => ({
            ...c,
            checked: true,
            stabilization_days: 14,
          })),
        );
      } else if (job.status === "failed") {
        setShelfError(job.error ?? "The shelf scan failed.");
      } else {
        setTimeout(() => pollShelfJob(jobId), 1500);
      }
    } catch (e) {
      setShelfError(e instanceof Error ? e.message : "Could not check the scan status");
    }
  }, [userId]);

  const onShelfFile = useCallback(
    (file: File) => {
      if (!userId) return;
      setShelfError(null);
      setShelfJob(null);
      setShelfSelections([]);
      const reader = new FileReader();
      reader.onload = async () => {
        const dataUrl = String(reader.result);
        const base64 = dataUrl.split(",")[1];
        try {
          const { job_id } = await api.scanShelf(userId, base64);
          setShelfJob({ id: job_id, status: "queued", result: null });
          pollShelfJob(job_id);
        } catch (e) {
          setShelfError(e instanceof Error ? e.message : "Could not start the shelf scan");
        }
      };
      reader.readAsDataURL(file);
    },
    [userId, pollShelfJob],
  );

  const toggleShelfSelection = (index: number, patch: Partial<(typeof shelfSelections)[number]>) => {
    setShelfSelections((prev) =>
      prev.map((item, i) => (i === index ? { ...item, ...patch } : item)),
    );
  };

  const handleConfirmShelfScan = async () => {
    if (!userId || !shelfJob) return;
    const chosen = shelfSelections.filter((s) => s.checked);
    if (chosen.length === 0) {
      setShelfError("Check at least one product to add it.");
      return;
    }
    setShelfConfirming(true);
    setShelfError(null);
    try {
      await api.confirmShelfScan(
        userId,
        shelfJob.id,
        chosen.map((s) => ({
          name: s.name,
          category: s.category,
          ingredients: s.ingredients,
          stabilization_days: s.stabilization_days,
        })),
      );
      setShelfOpen(false);
      setShelfJob(null);
      setShelfSelections([]);
      await load();
    } catch (e) {
      setShelfError(e instanceof Error ? e.message : "Could not add the selected products");
    } finally {
      setShelfConfirming(false);
    }
  };

  // ------------------------------------------------------ predict-before-buy
  const handlePredict = async (productId: string) => {
    if (!userId) return;
    setPredictProductId(productId);
    setPredictResult(null);
    setPredictError(null);
    setPredictLoading(true);
    try {
      setPredictResult(await api.predictProduct(productId, userId));
    } catch (e) {
      setPredictError(
        e instanceof Error ? e.message : "Could not predict this product",
      );
    } finally {
      setPredictLoading(false);
    }
  };

  const handleComplete = async (experimentId: string) => {
    if (!userId) return;
    setCompletingId(experimentId);
    setCompleteError(null);
    try {
      await api.setExperimentStatus(userId, experimentId, "completed");
      await load();
    } catch (e) {
      setCompleteError(
        e instanceof Error ? e.message : "Could not update the experiment",
      );
    } finally {
      setCompletingId(null);
    }
  };

  if (loadError) {
    return (
      <>
        <PageHeading eyebrow="causal routine" title="Change one thing." />
        <Notice tone="error">{loadError}</Notice>
      </>
    );
  }

  if (!dashboard || !products || experiments === null) {
    return <RoutineSkeleton />;
  }

  const routineEvents = dashboard.routine_events;

  return (
    <>
      <PageHeading eyebrow="causal routine" title="Change one thing.">
        Every product you log becomes a timestamped variable. An experiment
        holds everything else constant and waits out a stabilization window
        before it will offer a conclusion.
      </PageHeading>

      <PurchaseGuidanceCard />
      <div className="grid gap-4 lg:grid-cols-2">
        <Reveal>
          <Card>
            <CardTitle>Add product</CardTitle>
            <div className="flex flex-col gap-3.5">
              <Field label="Name">
                <input
                  className={inputClass}
                  value={productName}
                  onChange={(e) => setProductName(e.target.value)}
                />
              </Field>
              <div className="grid grid-cols-2 gap-3.5">
                <Field label="Category">
                  <select
                    className={inputClass}
                    value={productCategory}
                    onChange={(e) =>
                      setProductCategory(
                        e.target.value as (typeof CATEGORIES)[number],
                      )
                    }
                  >
                    {CATEGORIES.map((c) => (
                      <option key={c} value={c}>
                        {c}
                      </option>
                    ))}
                  </select>
                </Field>
                <Field
                  label="Stabilization days"
                  hint="0-180, how long before change is trusted"
                >
                  <input
                    type="number"
                    min={0}
                    max={180}
                    className={inputClass}
                    value={productStabilization}
                    onChange={(e) =>
                      setProductStabilization(Number(e.target.value))
                    }
                  />
                </Field>
              </div>
              <Field label="Ingredients (INCI)" hint="Comma separated">
                <textarea
                  className={inputClass}
                  rows={2}
                  value={productIngredients}
                  onChange={(e) => setProductIngredients(e.target.value)}
                />
              </Field>
              {productError && <Notice tone="error">{productError}</Notice>}
              <Button
                onClick={handleAddProduct}
                loading={productSubmitting}
                className="self-start"
              >
                <PlusIcon size={18} />
                Add product
              </Button>
            </div>
          </Card>
        </Reveal>

        <Reveal delay={0.04}>
          <Card>
            <CardTitle>Log routine event</CardTitle>
            <div className="flex flex-col gap-3.5">
              <Field label="Product">
                <select
                  className={inputClass}
                  value={eventProductId}
                  onChange={(e) => setEventProductId(e.target.value)}
                >
                  <option value="">Choose a product</option>
                  {products.map((p) => (
                    <option key={p.id} value={p.id}>
                      {p.name}
                    </option>
                  ))}
                </select>
              </Field>
              <div className="grid grid-cols-2 gap-3.5">
                <Field label="Action">
                  <select
                    className={inputClass}
                    value={eventAction}
                    onChange={(e) =>
                      setEventAction(e.target.value as Action)
                    }
                  >
                    {ACTIONS.map((a) => (
                      <option key={a.value} value={a.value}>
                        {a.label}
                      </option>
                    ))}
                  </select>
                </Field>
                <Field label="Slot">
                  <select
                    className={inputClass}
                    value={eventSlot}
                    onChange={(e) =>
                      setEventSlot(e.target.value as (typeof SLOTS)[number])
                    }
                  >
                    {SLOTS.map((s) => (
                      <option key={s} value={s}>
                        {s}
                      </option>
                    ))}
                  </select>
                </Field>
              </div>
              {isPremium && experiments.length > 0 && (
                <Field label="Experiment" hint="Optional — attach to a running test">
                  <select
                    className={inputClass}
                    value={eventExperimentId}
                    onChange={(e) => setEventExperimentId(e.target.value)}
                  >
                    <option value="">None</option>
                    {experiments.map((exp) => (
                      <option key={exp.id} value={exp.id}>
                        {exp.name}
                      </option>
                    ))}
                  </select>
                </Field>
              )}
              <Field label="Notes" hint="Optional">
                <textarea
                  className={inputClass}
                  rows={2}
                  value={eventNotes}
                  onChange={(e) => setEventNotes(e.target.value)}
                />
              </Field>
              {proactiveConfound && (
                <Notice>{proactiveConfound.message}</Notice>
              )}
              {eventError && <Notice tone="error">{eventError}</Notice>}
              {postSubmitConfound && (
                <Notice>{postSubmitConfound.message}</Notice>
              )}
              <Button
                onClick={handleLogEvent}
                loading={eventSubmitting}
                className="self-start"
              >
                <RoutineIcon size={18} />
                Log event
              </Button>
            </div>
          </Card>
        </Reveal>
      </div>

      <Reveal delay={0.06} className="mt-4">
        <Card>
          <CardTitle
            action={
              !shelfOpen ? (
                <Button size="sm" variant="secondary" onClick={() => setShelfOpen(true)}>
                  Scan your shelf
                </Button>
              ) : (
                <Button size="sm" variant="ghost" onClick={() => setShelfOpen(false)}>
                  Close
                </Button>
              )
            }
          >
            Shelf scan
          </CardTitle>
          {!shelfOpen ? (
            <p className="text-[13px] leading-relaxed text-muted">
              Photograph the products on your shelf and we will propose a
              candidate list — review, edit, and confirm the ones you actually
              use to add them in one pass.
            </p>
          ) : (
            <div className="flex flex-col gap-3.5">
              {!shelfJob && (
                <Button
                  variant="secondary"
                  onClick={() => shelfFileRef.current?.click()}
                  className="self-start"
                >
                  Upload shelf photo
                </Button>
              )}
              <input
                ref={shelfFileRef}
                type="file"
                accept="image/*"
                capture="environment"
                className="hidden"
                onChange={(e) => {
                  const f = e.target.files?.[0];
                  if (f) onShelfFile(f);
                  e.target.value = "";
                }}
              />

              {shelfJob && (shelfJob.status === "queued" || shelfJob.status === "running") && (
                <Notice>Scanning your photo for products&hellip; this updates automatically.</Notice>
              )}

              {shelfError && <Notice tone="error">{shelfError}</Notice>}

              {shelfJob?.status === "completed" && (
                <>
                  {shelfSelections.length === 0 ? (
                    <EmptyState
                      title="No products recognized"
                      body={shelfJob.result?.message ?? "Try better lighting or a closer shot of the labels."}
                    />
                  ) : (
                    <>
                      <p className="text-[13px] text-muted">{shelfJob.result?.message}</p>
                      <ul className="flex list-none flex-col gap-3 p-0">
                        {shelfSelections.map((s, i) => (
                          <li
                            key={i}
                            className="rounded-[var(--radius)] border border-line bg-surface-2 p-3"
                          >
                            <label className="flex items-start gap-3">
                              <input
                                type="checkbox"
                                className="mt-1 size-4 accent-[var(--honey-600)]"
                                checked={s.checked}
                                onChange={(e) =>
                                  toggleShelfSelection(i, { checked: e.target.checked })
                                }
                              />
                              <div className="flex-1">
                                <div className="grid gap-2.5 sm:grid-cols-2">
                                  <Field label="Name">
                                    <input
                                      className={inputClass}
                                      value={s.name}
                                      onChange={(e) =>
                                        toggleShelfSelection(i, { name: e.target.value })
                                      }
                                    />
                                  </Field>
                                  <Field label="Category">
                                    <input
                                      className={inputClass}
                                      value={s.category}
                                      onChange={(e) =>
                                        toggleShelfSelection(i, { category: e.target.value })
                                      }
                                    />
                                  </Field>
                                </div>
                                <Field label="Ingredients (INCI)" className="mt-2.5" hint="Comma separated">
                                  <input
                                    className={inputClass}
                                    value={s.ingredients.join(", ")}
                                    onChange={(e) =>
                                      toggleShelfSelection(i, {
                                        ingredients: e.target.value
                                          .split(",")
                                          .map((v) => v.trim())
                                          .filter(Boolean),
                                      })
                                    }
                                  />
                                </Field>
                              </div>
                            </label>
                          </li>
                        ))}
                      </ul>
                      <Button
                        onClick={handleConfirmShelfScan}
                        loading={shelfConfirming}
                        className="self-start"
                      >
                        Add selected products
                      </Button>
                    </>
                  )}
                </>
              )}
            </div>
          )}
        </Card>
      </Reveal>

      <Reveal delay={0.08} className="mt-4">
        {isPremium ? (
          <Card>
            <CardTitle action={<Tag tone="honey">Premium</Tag>}>
              One-variable experiment
            </CardTitle>
            <div className="grid gap-3.5 lg:grid-cols-2">
              <Field label="Name">
                <input
                  className={inputClass}
                  value={expName}
                  onChange={(e) => setExpName(e.target.value)}
                />
              </Field>
              <Field label="Product under test">
                <select
                  className={inputClass}
                  value={expProductId}
                  onChange={(e) => setExpProductId(e.target.value)}
                >
                  <option value="">Choose a product</option>
                  {products.map((p) => (
                    <option key={p.id} value={p.id}>
                      {p.name}
                    </option>
                  ))}
                </select>
              </Field>
              <Field label="Hypothesis" className="lg:col-span-2" hint="Optional">
                <textarea
                  className={inputClass}
                  rows={2}
                  value={expHypothesis}
                  onChange={(e) => setExpHypothesis(e.target.value)}
                />
              </Field>
              <Field label="Primary metric">
                <select
                  className={inputClass}
                  value={expMetric}
                  onChange={(e) => setExpMetric(e.target.value as MetricKey)}
                >
                  {METRICS.map((m) => (
                    <option key={m} value={m}>
                      {METRIC_LABELS[m]}
                    </option>
                  ))}
                </select>
              </Field>
              <Field label="Target days" hint="0-180">
                <input
                  type="number"
                  min={0}
                  max={180}
                  className={inputClass}
                  value={expTargetDays}
                  onChange={(e) => setExpTargetDays(Number(e.target.value))}
                />
              </Field>
            </div>
            {expError && <Notice tone="error">{expError}</Notice>}
            <Button
              onClick={handleCreateExperiment}
              loading={expSubmitting}
              className="mt-3.5"
            >
              <PlusIcon size={18} />
              Start experiment
            </Button>
          </Card>
        ) : (
          <>
            <PaywallCard
              title="One-variable experiments"
              body="The experiment engine isolates a single product, holds everything else steady, and waits out a stabilization window before it will speak. Premium unlocks it."
              onUnlock={handleUnlock}
              pending={unlockPending}
            />
            {unlockError && (
              <div className="mt-3">
                <Notice tone="error">{unlockError}</Notice>
              </div>
            )}
          </>
        )}
      </Reveal>

      <div className="mt-4 grid gap-4 lg:grid-cols-2">
        <Reveal delay={0.12}>
          <Card>
            <CardTitle>Experiments</CardTitle>
            {completeError && (
              <div className="mb-3">
                <Notice tone="error">{completeError}</Notice>
              </div>
            )}
            {experiments.length === 0 ? (
              <EmptyState
                title={
                  isPremium
                    ? "No experiments running yet"
                    : "Experiments are a Premium feature"
                }
                body={
                  isPremium
                    ? "Start one above — pick a single product and a metric to watch."
                    : "Unlock Premium to isolate one product at a time."
                }
              />
            ) : (
              <ul className="flex list-none flex-col gap-3 p-0">
                {experiments.map((exp) => (
                  <Card as="li" key={exp.id} className="bg-surface-2 p-4">
                    <div className="flex items-start justify-between gap-3">
                      <div>
                        <p className="font-bold">{exp.name}</p>
                        <p className="mt-0.5 text-[12px] text-subtle">
                          {exp.products?.[0]?.name ?? "No product attached"} ·{" "}
                          {METRIC_LABELS[exp.primary_metric]}
                        </p>
                      </div>
                      <Tag tone={exp.status === "completed" ? "honey" : "neutral"}>
                        {exp.status}
                      </Tag>
                    </div>
                    {exp.early_stop?.conclusive && (
                      <div className="mt-3">
                        <Notice tone="info">{exp.early_stop.message}</Notice>
                      </div>
                    )}
                    {exp.status !== "completed" && (
                      <Button
                        size="sm"
                        variant="secondary"
                        className="mt-3"
                        loading={completingId === exp.id}
                        onClick={() => handleComplete(exp.id)}
                      >
                        Mark complete
                      </Button>
                    )}
                  </Card>
                ))}
              </ul>
            )}
          </Card>
        </Reveal>

        <Reveal delay={0.16}>
          <Card>
            <CardTitle>Products catalog</CardTitle>
            {products.length === 0 ? (
              <EmptyState
                title="No products yet"
                body="Add the products you actually use — each one becomes a variable the moment it is timestamped."
              />
            ) : (
              <ul className="flex list-none flex-col gap-3 p-0">
                {products.map((p) => (
                  <Card as="li" key={p.id} className="bg-surface-2 p-4">
                    <div className="flex items-start justify-between gap-3">
                      <p className="font-bold">{p.name}</p>
                      <Tag>{p.category}</Tag>
                    </div>
                    <p className="mt-1.5 text-[12px] text-subtle">
                      {p.stabilization_days} day stabilization window
                    </p>
                    <Button
                      size="sm"
                      variant="secondary"
                      className="mt-3"
                      loading={predictLoading && predictProductId === p.id}
                      onClick={() =>
                        isPremium
                          ? handlePredict(p.id)
                          : setPredictProductId(
                              predictProductId === p.id ? null : p.id,
                            )
                      }
                    >
                      Predict before you buy
                    </Button>

                    {!isPremium && predictProductId === p.id && (
                      <div className="mt-3">
                        <PaywallCard
                          title="Predict before you buy"
                          body="Premium checks a candidate product's ingredients against your own verdict history and cohort signals before you spend anything."
                          onUnlock={handleUnlock}
                          pending={unlockPending}
                        />
                      </div>
                    )}

                    {isPremium && predictProductId === p.id && (
                      <div className="mt-3">
                        {predictError && <Notice tone="error">{predictError}</Notice>}
                        {predictResult && predictResult.product_id === p.id && (
                          <div className="rounded-[var(--radius)] border border-line bg-surface p-3">
                            <p className="text-[13px] font-semibold">
                              {predictResult.headline}
                            </p>
                            {predictResult.overlap_with_investigate.length > 0 && (
                              <p className="mt-2 text-[12px] leading-relaxed text-investigate">
                                Shares ingredients with:{" "}
                                {predictResult.overlap_with_investigate
                                  .map((o) => o.product_name)
                                  .join(", ")}{" "}
                                (investigate)
                              </p>
                            )}
                            {predictResult.overlap_with_likely_useful.length > 0 && (
                              <p className="mt-2 text-[12px] leading-relaxed text-useful">
                                Shares ingredients with:{" "}
                                {predictResult.overlap_with_likely_useful
                                  .map((o) => o.product_name)
                                  .join(", ")}{" "}
                                (likely useful)
                              </p>
                            )}
                            <p className="mt-2 text-[11px] text-subtle">
                              {predictResult.disclaimer}
                            </p>
                          </div>
                        )}
                      </div>
                    )}
                  </Card>
                ))}
              </ul>
            )}
          </Card>
        </Reveal>
      </div>

      <Reveal delay={0.2} className="mt-4">
        <Card>
          <CardTitle>Routine timeline</CardTitle>
          {routineEvents.length === 0 ? (
            <EmptyState
              title="No routine events logged"
              body="Products become variables only once they are timestamped. Log what you actually used."
            />
          ) : (
            <ol className="relative m-0 list-none p-0 pl-5">
              <span
                aria-hidden
                className="absolute top-2 bottom-2 left-1 w-px bg-line"
              />
              {routineEvents.map((e, i) => (
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
    </>
  );
}

function PurchaseGuidanceCard() {
  const { userId, isPremium, refresh } = useSession();
  const [barcode, setBarcode] = useState("");
  const [name, setName] = useState("");
  const [ingredients, setIngredients] = useState("");
  const [price, setPrice] = useState("");
  const [guidance, setGuidance] = useState<PurchaseGuidance | null>(null);
  const [loading, setLoading] = useState(false);
  const [upgrading, setUpgrading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const [scanning, setScanning] = useState(false);
  const scanVideoRef = useRef<HTMLVideoElement>(null);
  const scanStreamRef = useRef<MediaStream | null>(null);

  const stopBarcodeScan = useCallback(() => {
    scanStreamRef.current?.getTracks().forEach((track) => track.stop());
    scanStreamRef.current = null;
    setScanning(false);
  }, []);

  useEffect(() => stopBarcodeScan, [stopBarcodeScan]);

  const scanBarcode = useCallback(async () => {
    const detectorConstructor = (window as typeof window & {
      BarcodeDetector?: new (options?: { formats: string[] }) => {
        detect(video: HTMLVideoElement): Promise<Array<{ rawValue?: string }>>;
      };
    }).BarcodeDetector;
    if (!detectorConstructor) {
      setError("Camera barcode scanning is not supported here. Enter the barcode or use a hardware scanner instead.");
      return;
    }
    try {
      setError(null);
      const detector = new detectorConstructor({ formats: ["ean_13", "ean_8", "upc_a", "upc_e", "code_128"] });
      const stream = await navigator.mediaDevices.getUserMedia({ video: { facingMode: { ideal: "environment" } }, audio: false });
      scanStreamRef.current = stream;
      setScanning(true);
      const detect = async () => {
        const video = scanVideoRef.current;
        if (!video || !scanStreamRef.current) return;
        try {
          const matches = await detector.detect(video);
          const rawValue = matches[0]?.rawValue;
          if (rawValue) {
            setBarcode(rawValue);
            stopBarcodeScan();
            return;
          }
        } catch {
          setError("The barcode could not be read. Try better light or enter it manually.");
          stopBarcodeScan();
          return;
        }
        requestAnimationFrame(() => void detect());
      };
      requestAnimationFrame(() => {
        if (scanVideoRef.current && scanStreamRef.current) {
          scanVideoRef.current.srcObject = scanStreamRef.current;
          scanVideoRef.current.play().catch(() => undefined);
          void detect();
        }
      });
    } catch {
      setError("Camera access was unavailable. Enter the barcode manually instead.");
      stopBarcodeScan();
    }
  }, [stopBarcodeScan]);
  const unlock = useCallback(async () => {
    if (!userId) return;
    setUpgrading(true);
    try {
      await api.upgrade(userId);
      await refresh();
    } catch (e) {
      setError(e instanceof Error ? e.message : "Could not upgrade your plan");
    } finally {
      setUpgrading(false);
    }
  }, [userId, refresh]);

  const runGuidance = useCallback(async () => {
    if (!userId || loading) return;
    setLoading(true);
    setError(null);
    setGuidance(null);
    try {
      let product = null;
      if (barcode.trim()) product = await api.lookupProduct(barcode.trim());
      if (product) {
        setName(product.name);
        setIngredients((product.ingredients ?? []).join(", "));
      }
      setGuidance(await api.purchaseGuidance(userId, {
        name: (product?.name ?? name.trim()) || undefined,
        barcode: barcode.trim() || undefined,
        category: product?.category,
        ingredients: product?.ingredients?.join(", ") ?? ingredients,
        price_cents: price ? Math.round(Number(price) * 100) : undefined,
        currency: "INR",
      }));
    } catch (e) {
      setError(e instanceof Error ? e.message : "Could not check this product");
    } finally {
      setLoading(false);
    }
  }, [userId, loading, barcode, name, ingredients, price]);

  if (!isPremium) {
    return (
      <Reveal>
        <PaywallCard
          title="Check before you buy"
          body="Premium compares a product’s ingredients with your own past outcomes, then gives you a next action before you spend money."
          onUnlock={unlock}
          pending={upgrading}
        />
      </Reveal>
    );
  }

  return (
    <Reveal>
      <Card>
        <CardTitle action={<Tag tone="honey">Premium</Tag>}>Check before you buy</CardTitle>
        <p className="text-[13px] leading-relaxed text-muted">Enter a barcode from a scanner or paste the product details. A known barcode is matched to your catalog; unknown products can still be checked when you add their INCI list.</p>
        <div className="mt-4 grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
          <Field label="Barcode" hint="Optional">
            <input className={inputClass} inputMode="numeric" placeholder="Scan or enter" value={barcode} onChange={(e) => setBarcode(e.target.value)} />
          </Field>
          <Field label="Product name">
            <input className={inputClass} placeholder="e.g. Barrier serum" value={name} onChange={(e) => setName(e.target.value)} />
          </Field>
          <Field label="Price (INR)" hint="Optional">
            <input className={inputClass} inputMode="decimal" placeholder="0" value={price} onChange={(e) => setPrice(e.target.value)} />
          </Field>
          <div className="flex items-end">
            <Button onClick={runGuidance} loading={loading} block>Check product</Button>
          </div>
        </div>
        <div className="mt-3 flex flex-wrap items-center gap-2">
          <Button variant="secondary" size="sm" onClick={scanBarcode} loading={scanning}>Scan barcode with camera</Button>
          {scanning && <Button variant="ghost" size="sm" onClick={stopBarcodeScan}>Stop scan</Button>}
        </div>
        {scanning && <video ref={scanVideoRef} muted playsInline autoPlay className="mt-3 aspect-[3/1] w-full rounded-[var(--radius)] bg-ink-900 object-cover" aria-label="Barcode scanner preview" />}
        <Field label="Ingredients (INCI)" className="mt-3" hint="Required for an unknown barcode; comma separated">
          <textarea className={inputClass} rows={2} value={ingredients} onChange={(e) => setIngredients(e.target.value)} />
        </Field>
        {error && <div className="mt-3"><Notice tone="error">{error}</Notice></div>}
        {guidance && (
          <div className="mt-4 rounded-[var(--radius)] border border-line bg-surface-2 p-4">
            <div className="flex flex-wrap items-start justify-between gap-3">
              <div>
                <p className="font-bold">{guidance.product_name}</p>
                <p className="mt-1 text-[13px] leading-relaxed text-muted">{guidance.headline}</p>
              </div>
              <Tag tone={guidance.signal === "caution" ? "investigate" : guidance.signal === "promising_similarity" ? "likely_useful" : "neutral"}>{guidance.signal.replace("_", " ")}</Tag>
            </div>
            <p className="mt-3 text-[13px] leading-relaxed text-fg">{guidance.next_action}</p>
            {guidance.estimated_annual_cost_cents != null && <p className="mt-2 text-[12px] text-subtle">At this price, an estimated annual cost is {new Intl.NumberFormat("en-IN", { style: "currency", currency: guidance.currency }).format(guidance.estimated_annual_cost_cents / 100)} based on the stabilization window.</p>}
            <p className="mt-3 text-[11px] leading-relaxed text-subtle">{guidance.disclaimer}</p>
          </div>
        )}
      </Card>
    </Reveal>
  );
}

function RoutineSkeleton() {
  return (
    <>
      <div className="mb-7">
        <Skeleton className="h-3 w-28" />
        <Skeleton className="mt-3 h-10 w-80 max-w-full" />
      </div>
      <div className="grid gap-4 lg:grid-cols-2">
        <Skeleton className="h-[320px] rounded-[var(--radius-lg)]" />
        <Skeleton className="h-[320px] rounded-[var(--radius-lg)]" />
      </div>
      <Skeleton className="mt-4 h-[220px] rounded-[var(--radius-lg)]" />
      <div className="mt-4 grid gap-4 lg:grid-cols-2">
        <Skeleton className="h-[260px] rounded-[var(--radius-lg)]" />
        <Skeleton className="h-[260px] rounded-[var(--radius-lg)]" />
      </div>
      <Skeleton className="mt-4 h-[220px] rounded-[var(--radius-lg)]" />
    </>
  );
}
