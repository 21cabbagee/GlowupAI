"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { api } from "@/lib/api";
import { useSession } from "@/lib/session";
import { Button, Field, Notice, inputClass } from "@/components/ui";
import { LogoMark, ShieldIcon } from "@/components/icons";

const DEMO_USERNAME = "skinproof-demo";
const DEMO_ACCESS_CODE = "temporary-access-2026";
const DEMO_PROFILE_KEY = "skinproof_demo_profile_id";

export default function SignInPage() {
  const router = useRouter();
  const { userId, ready, signIn } = useSession();
  const [profileId, setProfileId] = useState("");
  const [demoUsername, setDemoUsername] = useState(DEMO_USERNAME);
  const [demoAccessCode, setDemoAccessCode] = useState("");
  const [pending, setPending] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (ready && userId) router.replace("/home");
  }, [ready, userId, router]);

  async function restoreProfile() {
    const id = profileId.trim();
    if (!id || pending) return;
    if (id === DEMO_USERNAME) {
      setDemoUsername(DEMO_USERNAME);
      setError("Enter the temporary access code in the demo account section below.");
      return;
    }
    setPending(true);
    setError(null);
    try {
      await api.profile(id);
      signIn(id);
      router.replace("/home");
    } catch (e) {
      setError(
        e instanceof Error
          ? e.message
          : "That local profile ID could not be found.",
      );
      setPending(false);
    }
  }

  async function continueDemo() {
    if (pending) return;
    if (
      demoUsername.trim() !== DEMO_USERNAME ||
      demoAccessCode !== DEMO_ACCESS_CODE
    ) {
      setError("Use the temporary demo username and access code provided for this build.");
      return;
    }

    setPending(true);
    setError(null);
    try {
      const storedId = localStorage.getItem(DEMO_PROFILE_KEY);
      if (storedId) {
        try {
          await api.profile(storedId);
          signIn(storedId);
          router.replace("/home");
          return;
        } catch {
          localStorage.removeItem(DEMO_PROFILE_KEY);
        }
      }

      const { user } = await api.createUser("temporary demo");
      localStorage.setItem(DEMO_PROFILE_KEY, user.id);
      signIn(user.id);
      router.replace("/home");
    } catch (e) {
      setError(
        e instanceof Error
          ? e.message
          : "Could not create a temporary demo profile.",
      );
      setPending(false);
    }
  }

  return (
    <main className="grid min-h-dvh place-items-center px-6 py-12">
      <section className="w-full max-w-[440px] rounded-[var(--radius-xl)] border border-line bg-surface p-6 sm:p-8">
        <Link href="/" className="flex items-center gap-2.5">
          <LogoMark size={30} />
          <span className="display-sm text-[19px]">
            Skin<span className="text-honey-600">Proof</span>
          </span>
        </Link>

        <p className="eyebrow mt-8">Local profile access</p>
        <h1 className="display mt-2.5 text-[clamp(28px,4vw,38px)]">
          Continue your record.
        </h1>
        <p className="mt-3 text-[14px] leading-relaxed text-muted">
          Enter the local profile ID saved from your account page to use the
          same profile on this device.
        </p>

        <div className="mt-6">
          <Field
            label="Local profile ID"
            hint="This is a development restore flow, not password-based authentication."
          >
            <input
              className={inputClass}
              value={profileId}
              onChange={(e) => setProfileId(e.target.value)}
              onKeyDown={(e) => {
                if (e.key === "Enter") restoreProfile();
              }}
              autoComplete="off"
            />
          </Field>
        </div>

        <Button
          className="mt-5"
          size="lg"
          block
          loading={pending}
          disabled={!profileId.trim()}
          onClick={restoreProfile}
        >
          Continue
        </Button>

        <div className="mt-6 rounded-[var(--radius)] border border-honey-200 bg-honey-50 p-4">
            <p className="text-[13px] font-semibold text-honey-800">Temporary developer account</p>
            <p className="mt-1.5 text-[12.5px] leading-relaxed text-honey-800/85">
              This demo reuses its local profile ID on this browser. The supplied
              username and access code are development fixtures, not secrets.
            </p>
            <div className="mt-4 grid gap-3">
              <Field label="Temporary username">
                <input
                  className={inputClass}
                  value={demoUsername}
                  onChange={(e) => setDemoUsername(e.target.value)}
                  autoComplete="username"
                />
              </Field>
              <Field label="Temporary access code">
                <input
                  className={inputClass}
                  type="password"
                  value={demoAccessCode}
                  onChange={(e) => setDemoAccessCode(e.target.value)}
                  onKeyDown={(e) => {
                    if (e.key === "Enter") continueDemo();
                  }}
                  autoComplete="current-password"
                />
              </Field>
            </div>
            <Button
              className="mt-3"
              variant="secondary"
              loading={pending}
              disabled={!demoUsername.trim() || !demoAccessCode}
              onClick={continueDemo}
            >
              Continue with demo
            </Button>
        </div>

        {error && (
          <div className="mt-4">
            <Notice tone="error">{error}</Notice>
          </div>
        )}

        <div className="mt-6 flex gap-3 rounded-[var(--radius)] border border-line bg-surface-2 p-4">
          <ShieldIcon size={18} className="mt-0.5 shrink-0 text-honey-700" />
          <p className="text-[12.5px] leading-relaxed text-muted">
            SkinProof does not yet have server-side authentication. Do not use a
            profile ID as a production credential or share it outside a trusted
            development environment.
          </p>
        </div>

        <p className="mt-6 text-[12px] text-subtle">
          Need a new record?{" "}
          <Link href="/start" className="font-semibold text-honey-700 hover:underline">
            Create a private profile
          </Link>
        </p>
      </section>
    </main>
  );
}
