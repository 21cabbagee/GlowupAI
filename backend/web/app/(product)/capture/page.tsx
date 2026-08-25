"use client";

import { useCallback, useEffect, useRef, useState } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import {
  ApiError,
  api,
  type CaptureGuide,
  type CaptureRejection,
  type Experiment,
} from "@/lib/api";
import { useSession } from "@/lib/session";
import { ConsentPrompt } from "@/components/shell";
import {
  Button,
  Card,
  CardTitle,
  Field,
  Notice,
  PageHeading,
  Reveal,
  Row,
  Tag,
  cx,
  inputClass,
} from "@/components/ui";
import {
  CameraIcon,
  CheckIcon,
  CloseIcon,
  ShieldIcon,
  SparkIcon,
} from "@/components/icons";

type Stage = "idle" | "live" | "review" | "saved";

const vertical = "skin";

export default function CapturePage() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const onboarding = searchParams.get("onboarding") === "1";
  const { userId, isPremium, hasConsent } = useSession();

  const videoRef = useRef<HTMLVideoElement>(null);
  const streamRef = useRef<MediaStream | null>(null);
  const fileRef = useRef<HTMLInputElement>(null);

  const [stage, setStage] = useState<Stage>("idle");
  const [shot, setShot] = useState<string | null>(null); // data URL for preview
  const [guide, setGuide] = useState<CaptureGuide | null>(null);
  const [experiments, setExperiments] = useState<Experiment[]>([]);
  const [experimentId, setExperimentId] = useState("");
  const [isBaseline, setIsBaseline] = useState(false);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [coaching, setCoaching] = useState<string[]>([]);
  const [result, setResult] = useState<string | null>(null);
  const [savedCaptureId, setSavedCaptureId] = useState<string | null>(null);
  const [feedbackSent, setFeedbackSent] = useState(false);
  const [cameraError, setCameraError] = useState<string | null>(null);

  /* ------------------------------------------------------------- guidance */

  useEffect(() => {
    if (!userId) return;
    api
      .captureGuide(userId, vertical)
      .then((nextGuide) => {
        setGuide(nextGuide);
        if (nextGuide.state === "baseline_needed") setIsBaseline(true);
      })
      .catch(() => setGuide(null));
  }, [userId, vertical]);

  useEffect(() => {
    if (!userId || !isPremium) {
      setExperiments([]);
      return;
    }
    api
      .experiments(userId)
      .then((list) => setExperiments(list.filter((e) => e.status === "active")))
      .catch(() => setExperiments([]));
  }, [userId, isPremium]);

  /* --------------------------------------------------------------- camera */

  const stopCamera = useCallback(() => {
    streamRef.current?.getTracks().forEach((t) => t.stop());
    streamRef.current = null;
  }, []);

  // Always release the camera when leaving the screen.
  useEffect(() => stopCamera, [stopCamera]);

  const startCamera = useCallback(async () => {
    setCameraError(null);
    try {
      const stream = await navigator.mediaDevices.getUserMedia({
        video: {
          // Front camera on a phone; the constraint is ignored on desktop.
          facingMode: "user",
          width: { ideal: 1440 },
          height: { ideal: 1440 },
        },
        audio: false,
      });
      streamRef.current = stream;
      setStage("live");
      // The <video> only exists once stage is "live", so attach on the next tick.
      requestAnimationFrame(() => {
        if (videoRef.current) {
          videoRef.current.srcObject = stream;
          videoRef.current.play().catch(() => undefined);
        }
      });
    } catch {
      setCameraError(
        "No camera available, or permission was declined. Upload a photo instead.",
      );
      setStage("idle");
    }
  }, []);

  const takeShot = useCallback(() => {
    const video = videoRef.current;
    if (!video) return;
    // Square centre crop keeps framing consistent between captures, which is
    // what makes two frames comparable at all.
    const side = Math.min(video.videoWidth, video.videoHeight);
    const canvas = document.createElement("canvas");
    canvas.width = side;
    canvas.height = side;
    const ctx = canvas.getContext("2d");
    if (!ctx) return;
    ctx.drawImage(
      video,
      (video.videoWidth - side) / 2,
      (video.videoHeight - side) / 2,
      side,
      side,
      0,
      0,
      side,
      side,
    );
    setShot(canvas.toDataURL("image/jpeg", 0.92));
    stopCamera();
    setStage("review");
  }, [stopCamera]);

  const onFile = useCallback((file: File) => {
    const reader = new FileReader();
    reader.onload = () => {
      setShot(String(reader.result));
      setStage("review");
    };
    reader.readAsDataURL(file);
  }, []);

  /* ----------------------------------------------------------------- save */

  const save = useCallback(async () => {
    if (!userId || !shot) return;
    setSaving(true);
    setError(null);
    setCoaching([]);
    try {
      const base64 = shot.split(",")[1];
      const res = await api.createCapture({
        user_id: userId,
        image_base64: base64,
        vertical,
        is_baseline: isBaseline,
        experiment_id: experimentId || null,
      });
      setSavedCaptureId(res.capture?.id ?? null);
      setFeedbackSent(false);
      setResult(res.measurement?.confidence_message ?? "Your standardized frame was accepted and added to your history.");
      setStage("saved");
    } catch (e) {
      // A rejected frame carries structured coaching, not just a message —
      // surface each actionable tip instead of one generic failure line.
      if (e instanceof ApiError && e.detail && typeof e.detail === "object") {
        const rejection = e.detail as CaptureRejection;
        const tips = rejection.quality?.coaching?.map((c) => c.message) ?? [];
        if (tips.length > 0) {
          setError(rejection.message ?? e.message);
          setCoaching(tips);
        } else {
          setError(e.message);
        }
      } else {
        setError(e instanceof Error ? e.message : "The frame was not accepted");
      }
    } finally {
      setSaving(false);
    }
  }, [userId, shot, vertical, isBaseline, experimentId]);

  const sendFeedback = useCallback(async (agreement: "fair" | "uncertain" | "off") => {
    if (!userId || !savedCaptureId || feedbackSent) return;
    try {
      await api.submitMeasurementFeedback(userId, {
        capture_id: savedCaptureId,
        agreement,
      });
      setFeedbackSent(true);
    } catch {
      // Feedback is helpful but should never block the capture flow.
    }
  }, [userId, savedCaptureId, feedbackSent]);

  const reset = useCallback(() => {
    setShot(null);
    setResult(null);
    setSavedCaptureId(null);
    setFeedbackSent(false);
    setError(null);
    setCoaching([]);
    setStage("idle");
  }, []);

  /* ----------------------------------------------------------------- view */

  if (!hasConsent) {
    return (
      <>
        <PageHeading eyebrow="calibrated capture" title="Make the next frame comparable.">
          Capture is gated behind explicit facial-data consent.
        </PageHeading>
        <ConsentPrompt />
      </>
    );
  }

  return (
    <>
      <PageHeading eyebrow={`${vertical} · calibrated capture`} title={onboarding ? "Your first measurement" : "Make the next frame comparable."}>
        {onboarding ? "Start with one calm, standardized frame. You will get a plain-language quality read now and a trend after the next capture." : "Even light, neutral expression, frontal pose, 30–80 cm. The server measures brightness and sharpness and rejects frames that break the comparison assumptions."}
      </PageHeading>

      <div className="grid gap-4 lg:grid-cols-12">
        {/* ------------------------------------------------- capture stage */}
        <Reveal className="lg:col-span-7">
          <Card className="overflow-hidden p-0">
            <div className="relative aspect-square w-full bg-ink-900">
              {stage === "live" && (
                <>
                  <video
                    ref={videoRef}
                    playsInline
                    muted
                    autoPlay
                    className="size-full object-cover"
                    // Mirror the preview so it reads like a mirror, which is
                    // what people expect when framing their own face.
                    style={{ transform: "scaleX(-1)" }}
                  />
                  <FramingOverlay />
                </>
              )}

              {stage === "review" && shot && (
                // eslint-disable-next-line @next/next/no-img-element
                <img
                  src={shot}
                  alt="Capture pending review"
                  className="size-full object-cover"
                />
              )}

              {(stage === "idle" || stage === "saved") && (
                <div className="absolute inset-0 grid place-content-center gap-4 p-6 text-center">
                  {stage === "saved" ? (
                    <>
                      <span className="mx-auto grid size-14 place-items-center rounded-[var(--radius-full)] bg-primary text-on-primary">
                        <CheckIcon size={26} />
                      </span>
                      <p className="display-sm text-[19px] text-paper">
                        Frame accepted
                      </p>
                      <p className="mx-auto max-w-xs text-[13px] text-ink-300">
                        {result}
                      </p>
                    </>
                  ) : (
                    <>
                      <span className="mx-auto grid size-14 place-items-center rounded-[var(--radius-full)] bg-primary text-on-primary">
                        <CameraIcon size={26} />
                      </span>
                      <p className="display-sm text-[19px] text-paper">
                        Ready when you are
                      </p>
                      <p className="mx-auto max-w-xs text-[13px] text-ink-300">
                        Use the live camera for a calibrated frame, or upload an
                        existing photo.
                      </p>
                    </>
                  )}
                </div>
              )}
            </div>

            {/* Controls sit under the viewfinder, thumb-reachable on mobile */}
            <div className="flex flex-wrap items-center gap-2.5 p-4">
              {stage === "idle" && (
                <>
                  <Button onClick={startCamera} className="flex-1 sm:flex-none">
                    <CameraIcon size={18} />
                    Open camera
                  </Button>
                  <Button
                    variant="secondary"
                    onClick={() => fileRef.current?.click()}
                    className="flex-1 sm:flex-none"
                  >
                    Upload photo
                  </Button>
                </>
              )}

              {stage === "live" && (
                <>
                  <Button onClick={takeShot} size="lg" className="flex-1">
                    Capture frame
                  </Button>
                  <Button
                    variant="ghost"
                    onClick={() => {
                      stopCamera();
                      setStage("idle");
                    }}
                  >
                    <CloseIcon size={18} />
                    Cancel
                  </Button>
                </>
              )}

              {stage === "review" && (
                <>
                  <Button onClick={save} loading={saving} size="lg" className="flex-1">
                    Analyze and save
                  </Button>
                  <Button variant="secondary" onClick={reset} disabled={saving}>
                    Retake
                  </Button>
                </>
              )}

              {stage === "saved" && (
                <>
                  <Button onClick={() => router.push("/home")} className="flex-1 sm:flex-none">
                    See the update
                  </Button>
                  <Button variant="secondary" onClick={reset}>
                    Capture another
                  </Button>
                </>
              )}

              <input
                ref={fileRef}
                type="file"
                accept="image/*"
                className="hidden"
                onChange={(e) => {
                  const f = e.target.files?.[0];
                  if (f) onFile(f);
                  e.target.value = "";
                }}
              />
            </div>
          </Card>

          {cameraError && (
            <div className="mt-3">
              <Notice>{cameraError}</Notice>
            </div>
          )}
          {error && (
            <div className="mt-3">
              <Notice tone="error">
                <span className="block font-semibold">{error}</span>
                {coaching.length > 0 && (
                  <ul className="mt-2 list-disc space-y-1 pl-4">
                    {coaching.map((tip, i) => (
                      <li key={i}>{tip}</li>
                    ))}
                  </ul>
                )}
              </Notice>
            </div>
          )}
          {stage === "saved" && result && (
            <div className="mt-3">
              <Notice tone="success">{result}</Notice>
            </div>
          )}
          {stage === "saved" && savedCaptureId && (
            <Card className="mt-3 bg-surface-2">
              <CardTitle>Was this reading fair?</CardTitle>
              <p className="text-[12.5px] leading-relaxed text-muted">
                Your answer helps us improve the measurement model. It is not a medical assessment.
              </p>
              {!feedbackSent ? (
                <div className="mt-3 flex flex-wrap gap-2">
                  <Button size="sm" variant="secondary" onClick={() => sendFeedback("fair")}>Looks fair</Button>
                  <Button size="sm" variant="secondary" onClick={() => sendFeedback("uncertain")}>Not sure</Button>
                  <Button size="sm" variant="secondary" onClick={() => sendFeedback("off")}>Feels off</Button>
                </div>
              ) : (
                <p className="mt-3 text-[12px] font-semibold text-useful">Thanks — your feedback is recorded.</p>
              )}
            </Card>
          )}
        </Reveal>

        {/* ------------------------------------------------------ side rail */}
        <div className="flex flex-col gap-4 lg:col-span-5">
          <Reveal delay={0.06}>
            <Card>
              <CardTitle>This capture</CardTitle>

              <Field label="Experiment" hint={isPremium ? undefined : "Experiments are a Premium feature"}>
                <select
                  className={inputClass}
                  value={experimentId}
                  onChange={(e) => setExperimentId(e.target.value)}
                  disabled={!isPremium || experiments.length === 0}
                >
                  <option value="">Not part of an experiment</option>
                  {experiments.map((x) => (
                    <option key={x.id} value={x.id}>
                      {x.name}
                    </option>
                  ))}
                </select>
              </Field>

              <label className="mt-3 flex cursor-pointer items-start gap-3 rounded-[var(--radius)] border border-line bg-surface-2 p-3">
                <input
                  type="checkbox"
                  checked={isBaseline}
                  onChange={(e) => setIsBaseline(e.target.checked)}
                  className="mt-0.5 size-4 accent-[var(--honey-600)]"
                />
                <span>
                  <span className="block text-[13px] font-bold">
                    Set as baseline for {vertical}
                  </span>
                  <span className="block text-[12px] leading-snug text-muted">
                    The reference frame every later comparison is measured
                    against.
                  </span>
                </span>
              </label>
            </Card>
          </Reveal>

          {guide && (
            <Reveal delay={0.1}>
              <Card>
                <CardTitle action={<Tag tone="honey">{guide.state.replace(/_/g, " ")}</Tag>}>
                  Cadence
                </CardTitle>
                <p className="text-[14px] leading-relaxed">{guide.message}</p>
                <p className="mt-2 text-[12px] text-subtle">
                  Next window opens{" "}
                  {new Date(guide.next_window_start).toLocaleString(undefined, {
                    dateStyle: "medium",
                    timeStyle: "short",
                  })}
                </p>
              </Card>
            </Reveal>
          )}

          <Reveal delay={0.14}>
            <Card>
              <CardTitle
                action={
                  <span className="flex items-center gap-1.5 text-[12px] text-subtle">
                    <ShieldIcon size={15} /> enforced
                  </span>
                }
              >
                Acceptance contract
              </CardTitle>
              <Row>
                <span className="flex-1 text-[13px] font-semibold">Pose</span>
                <span className="text-[13px] text-muted">yaw/pitch ≤ 12°</span>
              </Row>
              <Row>
                <span className="flex-1 text-[13px] font-semibold">Light</span>
                <span className="text-[13px] text-muted">0.20 – 0.85</span>
              </Row>
              <Row>
                <span className="flex-1 text-[13px] font-semibold">Sharpness</span>
                <span className="text-[13px] text-muted">gradient ≥ 0.35</span>
              </Row>
              <Row>
                <span className="flex-1 text-[13px] font-semibold">Cadence</span>
                <span className="text-[13px] text-muted">every 3 – 7 days</span>
              </Row>
              <p className="mt-3 flex items-start gap-2 text-[12px] leading-relaxed text-subtle">
                <SparkIcon size={15} className="mt-0.5 shrink-0" />
                A rejected frame is not a failure. It means the frame would have
                polluted the comparison.
              </p>
            </Card>
          </Reveal>
        </div>
      </div>

      <p className="mt-6 text-[12px] leading-relaxed text-subtle">
        SkinProof tracks cosmetic appearance. It does not diagnose, treat, or rule
        out any medical condition.
      </p>
    </>
  );
}

/** Framing guides drawn over the live preview: oval, thirds, distance hint. */
function FramingOverlay() {
  return (
    <div aria-hidden className="pointer-events-none absolute inset-0">
      <svg viewBox="0 0 100 100" className="size-full" preserveAspectRatio="none">
        {/* Face oval */}
        <ellipse
          cx="50"
          cy="46"
          rx="26"
          ry="34"
          fill="none"
          stroke="var(--honey-500)"
          strokeWidth="0.6"
          strokeDasharray="3 2.4"
          opacity="0.9"
        />
        {/* Eye line — the single most useful alignment cue */}
        <line
          x1="20"
          y1="38"
          x2="80"
          y2="38"
          stroke="var(--honey-500)"
          strokeWidth="0.35"
          opacity="0.55"
        />
        {/* Corner brackets */}
        {[
          [6, 6, 1, 1],
          [94, 6, -1, 1],
          [6, 94, 1, -1],
          [94, 94, -1, -1],
        ].map(([x, y, dx, dy], i) => (
          <path
            key={i}
            d={`M${x} ${y + 9 * dy} L${x} ${y} L${x + 9 * dx} ${y}`}
            fill="none"
            stroke="var(--honey-500)"
            strokeWidth="0.9"
            strokeLinecap="round"
          />
        ))}
      </svg>
      <p
        className={cx(
          "absolute inset-x-0 bottom-3 mx-auto w-fit rounded-[var(--radius-full)]",
          "bg-ink-900/70 px-3 py-1.5 text-[11px] font-bold text-paper backdrop-blur-sm",
        )}
      >
        Fill the oval · neutral expression · even light
      </p>
    </div>
  );
}
