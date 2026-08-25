"use client";

import { useCallback, useState } from "react";
import { useRouter } from "next/navigation";
import { api } from "@/lib/api";
import { useSession } from "@/lib/session";
import { useTheme } from "@/lib/theme";
import { ConsentPrompt } from "@/components/shell";
import {
  Button,
  Card,
  CardTitle,

  Field,
  Notice,
  PageHeading,
  PaywallCard,
  Reveal,
  Row,
  Skeleton,
  Tag,
  cx,
  inputClass,
} from "@/components/ui";
import { DownloadIcon, MoonIcon, ShieldIcon, SunIcon, TrashIcon } from "@/components/icons";

const THEME_OPTIONS = [
  { value: "light" as const, label: "Light", Icon: SunIcon },
  { value: "dark" as const, label: "Dark", Icon: MoonIcon },
  { value: "system" as const, label: "System", Icon: ShieldIcon },
];

export default function AccountPage() {
  const router = useRouter();
  const { userId, profile, isPremium, hasConsent, refresh, signOut } = useSession();
  const { mode, setMode } = useTheme();

  // ------------------------------------------------------------ plan
  const [planPending, setPlanPending] = useState(false);
  const [planError, setPlanError] = useState<string | null>(null);

  const onUpgrade = useCallback(async () => {
    if (!userId) return;
    setPlanPending(true);
    setPlanError(null);
    try {
      await api.upgrade(userId);
      await refresh();
    } catch (e) {
      setPlanError(e instanceof Error ? e.message : "Could not upgrade your plan");
    } finally {
      setPlanPending(false);
    }
  }, [userId, refresh]);

  const onCancel = useCallback(async () => {
    if (!userId) return;
    setPlanPending(true);
    setPlanError(null);
    try {
      await api.cancel(userId);
      await refresh();
    } catch (e) {
      setPlanError(e instanceof Error ? e.message : "Could not cancel your plan");
    } finally {
      setPlanPending(false);
    }
  }, [userId, refresh]);

  // ------------------------------------------------------------ export
  const [exportPending, setExportPending] = useState(false);
  const [exportError, setExportError] = useState<string | null>(null);

  const onExport = useCallback(async () => {
    if (!userId) return;
    setExportPending(true);
    setExportError(null);
    try {
      const data = await api.exportData(userId);
      const blob = new Blob([JSON.stringify(data, null, 2)], {
        type: "application/json",
      });
      const url = URL.createObjectURL(blob);
      const a = document.createElement("a");
      a.href = url;
      a.download = "skinproof-export.json";
      document.body.appendChild(a);
      a.click();
      a.remove();
      URL.revokeObjectURL(url);
    } catch (e) {
      setExportError(e instanceof Error ? e.message : "Could not export your data");
    } finally {
      setExportPending(false);
    }
  }, [userId]);

  // ------------------------------------------------------ dermatologist export
  const [dermPending, setDermPending] = useState(false);
  const [dermError, setDermError] = useState<string | null>(null);

  const onDermExport = useCallback(async () => {
    if (!userId) return;
    setDermPending(true);
    setDermError(null);
    try {
      const report = await api.dermExport(userId);
      // Meant to be printed or shared, not downloaded — open it in a fresh
      // tab the same way any other printable document would be handed off.
      const win = window.open("", "_blank");
      if (!win) throw new Error("The report window was blocked. Allow pop-ups and try again.");
      win.document.write(report.printable_html);
      win.document.close();
    } catch (e) {
      setDermError(e instanceof Error ? e.message : "Could not generate the report");
    } finally {
      setDermPending(false);
    }
  }, [userId]);

  // ------------------------------------------------------------ reprocess
  const [modelVersion, setModelVersion] = useState("deterministic-3.1");
  const [reprocessPending, setReprocessPending] = useState(false);
  const [reprocessError, setReprocessError] = useState<string | null>(null);
  const [reprocessCount, setReprocessCount] = useState<number | null>(null);

  const onReprocess = useCallback(async () => {
    if (!userId) return;
    setReprocessPending(true);
    setReprocessError(null);
    setReprocessCount(null);
    try {
      const { processed_count } = await api.reprocess(userId, modelVersion);
      setReprocessCount(processed_count);
    } catch (e) {
      setReprocessError(e instanceof Error ? e.message : "Could not reprocess your history");
    } finally {
      setReprocessPending(false);
    }
  }, [userId, modelVersion]);

  // ------------------------------------------------------------ delete
  const [confirmText, setConfirmText] = useState("");
  const [deletePending, setDeletePending] = useState(false);
  const [deleteError, setDeleteError] = useState<string | null>(null);

  const onDelete = useCallback(async () => {
    if (!userId) return;
    setDeletePending(true);
    setDeleteError(null);
    try {
      await api.deleteUser(userId);
      signOut();
      router.replace("/");
    } catch (e) {
      setDeleteError(e instanceof Error ? e.message : "Could not delete your account");
      setDeletePending(false);
    }
  }, [userId, signOut, router]);

  // ------------------------------------------------------------ device access
  const [copyStatus, setCopyStatus] = useState<string | null>(null);

  const onCopyProfileId = useCallback(async () => {
    if (!userId) return;
    try {
      await navigator.clipboard.writeText(userId);
      setCopyStatus("Copied local profile ID.");
    } catch {
      setCopyStatus("Copy was unavailable. Select the ID and copy it manually.");
    }
  }, [userId]);

  const onSignOut = useCallback(() => {
    signOut();
    router.replace("/");
  }, [router, signOut]);

  const user = profile?.user;
  const entitlement = profile?.entitlement;

  return (
    <>
      <PageHeading eyebrow="account and privacy" title="Own the evidence.">
        Consent, plan, export, reprocessing and deletion are explicit controls —
        never hidden behind a settings maze.
      </PageHeading>

      <div className="grid gap-4 lg:grid-cols-2">
        {/* --------------------------------------------------- profile & consent */}
        <Reveal>
          <Card>
            <CardTitle>Profile &amp; consent</CardTitle>
            {!profile ? (
              <div className="flex flex-col gap-3">
                <Skeleton className="h-5 w-full" />
                <Skeleton className="h-5 w-full" />
                <Skeleton className="h-5 w-full" />
              </div>
            ) : (
              <>
                <Row>
                  <span className="text-[13px] text-muted">Profile</span>
                  <span className="ml-auto font-semibold">
                    {user?.skin_type ?? "Personal"}
                  </span>
                </Row>
                <Row>
                  <span className="text-[13px] text-muted">Consent</span>
                  <span className="ml-auto">
                    <Tag tone={hasConsent ? "likely_useful" : "neutral"}>
                      {user?.consent_state ?? "unknown"}
                    </Tag>
                  </span>
                </Row>
                <Row>
                  <span className="text-[13px] text-muted">Plan</span>
                  <span className="ml-auto font-semibold capitalize">
                    {entitlement?.plan ?? "free"}
                  </span>
                </Row>
                <Row>
                  <span className="text-[13px] text-muted">Status</span>
                  <span className="ml-auto font-semibold capitalize">
                    {entitlement?.status ?? "unknown"}
                  </span>
                </Row>
              </>
            )}
            {!hasConsent && (
              <div className="mt-4">
                <ConsentPrompt />
              </div>
            )}
          </Card>
        </Reveal>

        {/* --------------------------------------------------------------- plan */}
        <Reveal delay={0.04}>
          <Card>
            <CardTitle>Plan</CardTitle>
            <p className="text-[13px] leading-relaxed text-muted">
              Verdicts are never for sale. Premium unlocks product-level
              attribution and the experiment engine — your raw history stays
              yours either way.
            </p>
            <div className="mt-4 flex flex-wrap items-center gap-3">
              {!isPremium ? (
                <Button loading={planPending} onClick={onUpgrade}>
                  Upgrade to Premium
                </Button>
              ) : (
                <Button variant="secondary" loading={planPending} onClick={onCancel}>
                  Cancel Premium
                </Button>
              )}
            </div>
            {isPremium && (
              <p className="mt-3 text-[12px] text-subtle">
                Cancelling keeps your full free history. Nothing is deleted.
              </p>
            )}
            {planError && (
              <div className="mt-3">
                <Notice tone="error">{planError}</Notice>
              </div>
            )}
          </Card>
        </Reveal>

        {/* --------------------------------------------------------- appearance */}
        <Reveal delay={0.08}>
          <Card>
            <CardTitle>Appearance</CardTitle>
            <div
              role="tablist"
              aria-label="Theme"
              className="flex gap-1.5 rounded-[var(--radius)] bg-surface-2 p-1"
            >
              {THEME_OPTIONS.map(({ value, label, Icon }) => {
                const active = mode === value;
                return (
                  <button
                    key={value}
                    role="tab"
                    aria-selected={active}
                    onClick={() => setMode(value)}
                    className={cx(
                      "flex flex-1 items-center justify-center gap-1.5 rounded-[var(--radius-full)] px-3 py-2 text-[13px] font-bold transition-colors duration-[var(--dur-fast)]",
                      active
                        ? "bg-ink-900 text-paper"
                        : "border border-line text-muted hover:text-fg",
                    )}
                  >
                    <Icon size={16} />
                    {label}
                  </button>
                );
              })}
            </div>
          </Card>
        </Reveal>

        {/* --------------------------------------------------------- device access */}
        <Reveal delay={0.12}>
          <Card>
            <CardTitle>Device access</CardTitle>
            <p className="text-[13px] leading-relaxed text-muted">
              SkinProof currently restores profiles by local ID. This is for
              development only; it is not a password or production authentication.
            </p>
            <Field label="Local profile ID" className="mt-4">
              <input className={inputClass} value={userId ?? ""} readOnly />
            </Field>
            <div className="mt-3 flex flex-wrap gap-3">
              <Button variant="secondary" onClick={onCopyProfileId} disabled={!userId}>
                Copy profile ID
              </Button>
              <Button variant="ghost" onClick={onSignOut}>
                Sign out of this device
              </Button>
            </div>
            {copyStatus && <p className="mt-3 text-[12px] text-subtle">{copyStatus}</p>}
          </Card>
        </Reveal>

        {/* ---------------------------------------------------------- data controls */}
        <Reveal delay={0.16}>
          <Card>
            <CardTitle>Data controls</CardTitle>

            <div className="border-b border-line pb-4">
              <p className="text-[13px] font-semibold">Export your data</p>
              <p className="mt-1 text-[13px] leading-relaxed text-muted">
                Downloads a JSON file with your profile, captures, verdicts and
                routine events.
              </p>
              <Button
                className="mt-3"
                variant="secondary"
                loading={exportPending}
                onClick={onExport}
              >
                <DownloadIcon size={17} />
                Export data
              </Button>
              {exportError && (
                <div className="mt-3">
                  <Notice tone="error">{exportError}</Notice>
                </div>
              )}
            </div>

            <div className="border-b border-line py-4">
              <p className="flex items-center gap-2 text-[13px] font-semibold">
                Dermatologist export
                {!isPremium && <Tag tone="honey">Premium</Tag>}
              </p>
              <p className="mt-1 text-[13px] leading-relaxed text-muted">
                A printable measurement summary for a dermatologist visit —
                not a diagnostic document.
              </p>
              {isPremium ? (
                <>
                  <Button
                    className="mt-3"
                    variant="secondary"
                    loading={dermPending}
                    onClick={onDermExport}
                  >
                    Open printable report
                  </Button>
                  {dermError && (
                    <div className="mt-3">
                      <Notice tone="error">{dermError}</Notice>
                    </div>
                  )}
                </>
              ) : (
                <div className="mt-3">
                  <PaywallCard
                    title="Dermatologist export"
                    body="Premium generates a printable summary of your captures and verdicts, ready to bring to or share with a dermatologist."
                    onUnlock={onUpgrade}
                    pending={planPending}
                  />
                </div>
              )}
            </div>

            <div className="pt-4">
              <p className="text-[13px] font-semibold">Historical model reprocessing</p>
              <p className="mt-1 text-[13px] leading-relaxed text-muted">
                Re-runs your stored captures through a named model version so
                your history stays comparable even as the model improves.
              </p>
              <div className="mt-3 flex flex-wrap items-end gap-3">
                <Field label="Model version" className="max-w-[220px]">
                  <input
                    className={inputClass}
                    value={modelVersion}
                    onChange={(e) => setModelVersion(e.target.value)}
                  />
                </Field>
                <Button
                  variant="secondary"
                  loading={reprocessPending}
                  onClick={onReprocess}
                  disabled={!modelVersion.trim()}
                >
                  Reprocess history
                </Button>
              </div>
              {reprocessError && (
                <div className="mt-3">
                  <Notice tone="error">{reprocessError}</Notice>
                </div>
              )}
              {reprocessCount != null && (
                <div className="mt-3">
                  <Notice tone="success">
                    Reprocessed {reprocessCount} capture{reprocessCount === 1 ? "" : "s"} on{" "}
                    {modelVersion}.
                  </Notice>
                </div>
              )}
            </div>
          </Card>
        </Reveal>
      </div>

      {/* --------------------------------------------------------------- danger */}
      <Reveal delay={0.2}>
        <Card className="mt-4 border-investigate bg-investigate-bg/30">
          <CardTitle>Danger zone</CardTitle>
          <p className="text-[14px] font-semibold text-investigate">Delete account</p>
          <p className="mt-1.5 max-w-xl text-[13px] leading-relaxed text-muted">
            This cascades through the database and photo store. All captures,
            verdicts and routine events are removed. It cannot be undone.
          </p>
          <div className="mt-4 flex flex-wrap items-end gap-3">
            <Field label='Type "DELETE" to confirm' className="max-w-[220px]">
              <input
                className={inputClass}
                value={confirmText}
                onChange={(e) => setConfirmText(e.target.value)}
              />
            </Field>
            <Button
              variant="danger"
              loading={deletePending}
              disabled={confirmText !== "DELETE"}
              onClick={onDelete}
            >
              <TrashIcon size={17} />
              Delete account
            </Button>
          </div>
          {deleteError && (
            <div className="mt-3">
              <Notice tone="error">{deleteError}</Notice>
            </div>
          )}
        </Card>
      </Reveal>


    </>
  );
}
