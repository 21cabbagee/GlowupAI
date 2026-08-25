"use client";

import { useEffect, useMemo, useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { api, type Vertical } from "@/lib/api";
import { useSession } from "@/lib/session";
import { Button, Field, Notice, cx, inputClass } from "@/components/ui";
import { CheckIcon, ChevronLeftIcon, LogoMark, ShieldIcon } from "@/components/icons";

const PROMISES = [
  { title: "Your history is the control group", body: "Every number is compared against your own baseline, never a population average." },
  { title: "One variable at a time", body: "Products are timestamped. Experiments hold a stabilization window before any conclusion." },
  { title: "Verdicts are never for sale", body: "Affiliate links are disclosed and cannot influence what the evidence says." },
];

const GOALS: Record<Vertical, string[]> = {
  skin: ["Breakouts", "Redness", "Uneven tone", "Dark spots", "Texture", "Dryness", "Oil balance", "I’m not sure yet"],
};

const EXPERIENCE_LEVELS = [
  { value: "starting_fresh", label: "Starting fresh", body: "I’m building a routine now." },
  { value: "changing_things", label: "Changing things", body: "I’m testing or replacing products." },
  { value: "steady", label: "Steady routine", body: "I want to measure what is already working." },
  { value: "not_sure", label: "Not sure yet", body: "I’ll use the first few captures to learn." },
] as const;

type Step = 1 | 2;

export default function StartPage() {
  const router = useRouter();
  const { userId, ready, signIn } = useSession();
  const [step, setStep] = useState<Step>(1);
  const [createdId, setCreatedId] = useState<string | null>(null);
  const [displayName, setDisplayName] = useState("");
  const [skinType, setSkinType] = useState("");
  const [focus] = useState<Vertical>("skin");
  const [goals, setGoals] = useState<string[]>([]);
  const [experience, setExperience] = useState("");
  const [pending, setPending] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (ready && userId) router.replace("/capture?onboarding=1");
  }, [ready, userId, router]);

  const goalOptions = useMemo(() => GOALS[focus], [focus]);

  async function create() {
    if (pending) return;
    setPending(true);
    setError(null);
    try {
      const { user } = await api.createUser(skinType.trim() || null);
      setCreatedId(user.id);
      setStep(2);
    } catch (e) {
      setError(e instanceof Error ? e.message : "Could not create the profile");
    } finally {
      setPending(false);
    }
  }

  async function personalize() {
    if (pending || !createdId) return;
    if (!displayName.trim()) {
      setError("Add a name or nickname so your space can feel personal.");
      return;
    }
    if (!experience) {
      setError("Choose the routine state that fits best today.");
      return;
    }
    if (goals.length === 0) {
      setError("Choose at least one goal. You can change it later.");
      return;
    }

    setPending(true);
    setError(null);
    try {
      await api.updateProfile(createdId, {
        display_name: displayName.trim(),
        focus_vertical: focus,
        goals,
        experience_level: experience,
        onboarding_complete: true,
      });
      signIn(createdId);
      router.replace("/capture?onboarding=1");
    } catch (e) {
      setError(e instanceof Error ? e.message : "Could not save your preferences");
    } finally {
      setPending(false);
    }
  }

  function toggleGoal(goal: string) {
    if (goal === "I’m not sure yet") {
      setGoals((current) => (current.includes(goal) ? [] : [goal]));
      return;
    }
    setGoals((current) => {
      const withoutUncertain = current.filter((item) => item !== "I’m not sure yet");
      if (withoutUncertain.includes(goal)) return withoutUncertain.filter((item) => item !== goal);
      if (withoutUncertain.length >= 3) return withoutUncertain;
      return [...withoutUncertain, goal];
    });
  }

  return (
    <main className="grid min-h-dvh lg:grid-cols-2">
      <section className="relative hidden flex-col justify-between bg-primary p-12 text-on-primary lg:flex">
        <Link href="/" className="flex items-center gap-2.5">
          <span className="grid size-8 place-items-center rounded-[9px] bg-ink-900"><CheckIcon size={19} className="text-honey-500" /></span>
          <span className="display-sm text-[19px]">SkinProof</span>
        </Link>
        <div>
          <h2 className="display text-[clamp(30px,3.4vw,44px)]">A model of one person.<br />You.</h2>
          <ul className="mt-8 flex list-none flex-col gap-5 p-0">
            {PROMISES.map((p) => (
              <li key={p.title} className="flex gap-3">
                <span className="mt-0.5 grid size-6 shrink-0 place-items-center rounded-[var(--radius-full)] bg-ink-900"><CheckIcon size={14} className="text-honey-500" /></span>
                <span><span className="block font-bold">{p.title}</span><span className="block text-[13px] leading-relaxed text-ink-900/75">{p.body}</span></span>
              </li>
            ))}
          </ul>
        </div>
        <p className="text-[12px] text-ink-900/70">Cosmetic appearance tracking. Never diagnosis.</p>
      </section>

      <section className="flex flex-col justify-center px-6 py-12 sm:px-12">
        <div className="mx-auto w-full max-w-[520px]">
          <div className="flex items-center gap-2.5 lg:hidden"><LogoMark size={30} /><span className="display-sm text-[18px]">Skin<span className="text-honey-600">Proof</span></span></div>
          <div className="mt-8 flex items-center justify-between gap-4 lg:mt-0"><p className="eyebrow">Step {step} of 2</p><div className="flex gap-1.5" aria-label={`Step ${step} of 2`}>{[1, 2].map((item) => <span key={item} className={cx("h-1.5 w-10 rounded-full", item <= step ? "bg-primary" : "bg-line")} />)}</div></div>

          {step === 1 ? (
            <>
              <h1 className="display mt-2.5 text-[clamp(28px,4vw,38px)]">What should we call you?</h1>
              <p className="mt-3 text-[14px] leading-relaxed text-muted">This space is about one person: you. A name or nickname keeps the record human without needing an email or social account.</p>
              <div className="mt-8 grid gap-5">
                <Field label="Your name" hint="Only used to personalize your private space."><input className={inputClass} placeholder="First name or nickname" value={displayName} autoFocus onChange={(e) => setDisplayName(e.target.value)} onKeyDown={(e) => { if (e.key === "Enter") create(); }} /></Field>
                <Field label="Skin type (optional)" hint="Free text. It only labels your own history."><input className={inputClass} value={skinType} onChange={(e) => setSkinType(e.target.value)} /></Field>
              </div>
              {error && <div className="mt-4"><Notice tone="error">{error}</Notice></div>}
              <Button className="mt-6" size="lg" block loading={pending} disabled={!displayName.trim()} onClick={create}>Continue</Button>
              <div className="mt-6 flex gap-3 rounded-[var(--radius)] border border-line bg-surface-2 p-4"><ShieldIcon size={18} className="mt-0.5 shrink-0 text-honey-700" /><p className="text-[12.5px] leading-relaxed text-muted"><strong className="text-fg">Your name is not a credential.</strong> No email, password, or social graph is collected in this local build.</p></div>
            </>
          ) : (
            <>
              <button type="button" onClick={() => { setStep(1); setError(null); }} className="mt-5 inline-flex items-center gap-1 text-[13px] font-bold text-honey-700 hover:underline"><ChevronLeftIcon size={16} /> Back</button>
              <h1 className="display mt-4 text-[clamp(28px,4vw,38px)]">Make it yours, {displayName.trim()}.</h1>
              <p className="mt-3 text-[14px] leading-relaxed text-muted">Choose a few goals. This gives your home feed a reason to exist from day one.</p>
              <div className="mt-7"><div className="flex items-center justify-between gap-3"><p className="text-[12px] font-bold tracking-wide text-muted">What would feel like progress?</p><span className="text-[12px] font-semibold text-subtle">{goals.length} of 3</span></div><div className="mt-2 flex flex-wrap gap-2">{goalOptions.map((goal) => { const active = goals.includes(goal); return <button key={goal} type="button" onClick={() => toggleGoal(goal)} aria-pressed={active} className={cx("rounded-[var(--radius-full)] border px-3 py-2 text-[12.5px] font-semibold transition-colors", active ? "border-ink-900 bg-ink-900 text-paper" : "border-line bg-surface text-muted hover:border-line-strong hover:text-fg")}>{goal}</button>; })}</div></div>
              <div className="mt-6"><p className="mb-2 text-[12px] font-bold tracking-wide text-muted">What is your routine doing today?</p><div className="grid gap-2 sm:grid-cols-2">{EXPERIENCE_LEVELS.map((item) => { const active = experience === item.value; return <button key={item.value} type="button" onClick={() => setExperience(item.value)} aria-pressed={active} className={cx("rounded-[var(--radius)] border p-3 text-left transition-colors", active ? "border-primary bg-honey-50" : "border-line bg-surface hover:border-line-strong")}><span className="block text-[13px] font-bold">{item.label}</span><span className="mt-1 block text-[12px] leading-relaxed text-muted">{item.body}</span></button>; })}</div></div>
              {error && <div className="mt-4"><Notice tone="error">{error}</Notice></div>}
              <Button className="mt-6" size="lg" block loading={pending} onClick={personalize}>Save my starting point</Button>
              <p className="mt-3 text-center text-[12px] text-subtle">You can edit your goals later. Facial-photo consent comes before any capture.</p>
            </>
          )}
          <p className="mt-6 text-[12px] text-subtle">Already have a profile? <Link href="/sign-in" className="font-semibold text-honey-700 hover:underline">Sign in on this device</Link></p>
        </div>
      </section>
    </main>
  );
}
