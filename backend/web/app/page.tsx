import Link from "next/link";
import type { Metadata } from "next";
import { ButtonLink } from "@/components/ui";
import { LogoMark } from "@/components/icons";
import { LandingReveal } from "@/components/landing-reveal";

export const metadata: Metadata = {
  title: "SkinProof — Measured, not remembered",
};

const STEPS = [
  {
    title: "Capture on a cadence",
    body: "Guided windows, a quality gate on light and sharpness, and a rejection when a frame would break the comparison. Streaks exist to protect the cadence, not to nag.",
  },
  {
    title: "Log products as variables",
    body: "Every product is timestamped with a stabilization window. One-variable experiments hold that window before anything is called a result.",
  },
  {
    title: "Read a verdict, not a vibe",
    body: "Keep, Likely useful, Evidence unclear, or Investigate — each with the sample size, confidence, and noise floor that produced it.",
  },
];

const SURFACES = [
  {
    title: "Grounded Q&A",
    body: "Ask why redness moved. Answers cite your captures and routine events — never raw-photo guesswork, never a diagnosis.",
  },
  {
    title: "Cohort context",
    body: "Minimum-sample cohorts help with cold start. They never override what your own history says.",
  },
  {
    title: "Evidence you own",
    body: "Explicit facial-data consent, a full JSON export, and deletion that cascades through the photo store.",
  },
];

const HONESTY = [
  "It is cosmetic appearance tracking, not diagnosis. Medical-scope questions route to a dermatologist.",
  "Metrics are deterministic proxies with a stated model version, confidence, and noise floor.",
  "A verdict can say the evidence is unclear. That is a real answer, and often the correct one.",
  "Affiliate links are disclosed, and placement is never for sale.",
];

export default function LandingPage() {
  return (
    <div className="min-h-dvh bg-paper">
      {/* ------------------------------------------------------------ nav */}
      <header className="sticky top-0 z-40 border-b border-line bg-paper/85 backdrop-blur-xl">
        <div className="mx-auto flex h-[64px] max-w-[var(--content-max)] items-center gap-3 px-5 lg:px-10">
          <Link href="/" className="flex items-center gap-2.5">
            <LogoMark size={29} />
            <span className="display-sm text-[19px]">
              Skin<span className="text-honey-600">Proof</span>
            </span>
          </Link>
          <nav className="ml-auto hidden items-center gap-7 text-[13.5px] font-semibold text-muted md:flex">
            <a href="#how" className="hover:text-fg">
              How it works
            </a>
            <a href="#surfaces" className="hover:text-fg">
              Product
            </a>
            <a href="#honesty" className="hover:text-fg">
              Honesty
            </a>
          </nav>
          <div className="ml-auto flex items-center gap-2 md:ml-6">
            <ButtonLink href="/sign-in" variant="ghost" size="sm">
              Sign in
            </ButtonLink>
            <ButtonLink href="/start" size="sm">
              Start free
            </ButtonLink>
          </div>
        </div>
      </header>

      {/* ----------------------------------------------------------- hero */}
      <section className="mx-auto max-w-[var(--content-max)] px-5 pt-16 pb-8 lg:px-10 lg:pt-24">
        <div className="grid items-center gap-10 lg:grid-cols-12">
          <LandingReveal className="lg:col-span-7">
            <span className="inline-flex items-center gap-2 rounded-[var(--radius-full)] border border-honey-200 bg-honey-50 px-3 py-1.5 text-[12px] font-bold text-honey-800">
              <span aria-hidden className="size-1.5 rounded-full bg-honey-600" />
              Skin, measured over time
            </span>
            <h1 className="display mt-5 text-[clamp(40px,7.4vw,74px)]">
              Measured,
              <br />
              not remembered.
            </h1>
            <p className="mt-5 max-w-xl text-[16px] leading-relaxed text-muted">
              Skincare advice is mostly memory and marketing. SkinProof builds the
              other thing: a calibrated record of your own appearance, and honest
              verdicts about what actually moved it.
            </p>
            <div className="mt-8 flex flex-wrap gap-3">
              <ButtonLink href="/start" size="lg">
                Create a private profile
              </ButtonLink>
              <ButtonLink href="/sign-in" variant="secondary" size="lg">
                I already have a profile
              </ButtonLink>
            </div>
            <p className="mt-4 text-[12.5px] text-subtle">
              No email. No password. Capture stays locked until you consent.
            </p>
          </LandingReveal>

          {/* A real verdict card, not a stock screenshot */}
          <LandingReveal className="lg:col-span-5" delay={0.06}>
            <div className="rounded-[var(--radius-xl)] border border-line bg-surface p-6">
              <div className="flex items-center justify-between">
                <p className="eyebrow">Weekly verdict</p>
                <span className="rounded-[var(--radius-full)] bg-keep-bg px-2.5 py-1 text-[11px] font-bold text-keep">
                  Keep
                </span>
              </div>
              <p className="display-sm mt-3 text-[21px]">Barrier serum</p>
              <p className="mt-2.5 text-[13.5px] leading-relaxed text-muted">
                Redness fell 0.081 across 9 post-stabilization captures. The shift
                clears your noise floor and holds through the window.
              </p>

              <dl className="mt-5 grid grid-cols-3 gap-3 border-t border-line pt-4">
                {[
                  ["9", "captures"],
                  ["14d", "window"],
                  ["Strong", "evidence"],
                ].map(([v, k]) => (
                  <div key={k}>
                    <dt className="text-[11px] font-bold tracking-wide text-subtle uppercase">
                      {k}
                    </dt>
                    <dd className="tnum display-sm mt-0.5 text-[19px]">{v}</dd>
                  </div>
                ))}
              </dl>

              <p className="mt-4 text-[11.5px] leading-relaxed text-subtle">
                Model deterministic-3.1. Compared against your own baseline, not a
                population average.
              </p>
            </div>
          </LandingReveal>
        </div>
      </section>

      {/* ------------------------------------------------------- how it works */}
      <section id="how" className="mx-auto max-w-[var(--content-max)] px-5 py-16 lg:px-10 lg:py-24">
        <p className="eyebrow">How it works</p>
        <h2 className="display mt-2.5 max-w-2xl text-[clamp(28px,4.4vw,44px)]">
          Three habits. One causal record.
        </h2>
        <div className="mt-10 grid min-w-0 grid-cols-1 gap-3.5 sm:grid-cols-2 lg:grid-cols-3">
          {STEPS.map(({ title, body }, i) => (
            <LandingReveal key={title} delay={i * 0.05}>
              <article
              className="rounded-[var(--radius-lg)] border border-line bg-surface p-6 transition-colors duration-[var(--dur)] hover:border-line-strong hover:bg-surface-2"
            >
              <span className="tnum text-[12px] font-bold tracking-[0.14em] text-honey-700">
                0{i + 1}
              </span>
              <h3 className="display-sm mt-4 text-[18px]">{title}</h3>
              <p className="mt-2 text-[13.5px] leading-relaxed text-muted">{body}</p>
              </article>
            </LandingReveal>
          ))}
        </div>
      </section>

      {/* -------------------------------------------------------- honey band */}
      <section className="bg-primary text-on-primary">
        <div className="mx-auto max-w-[var(--content-max)] px-5 py-16 lg:px-10 lg:py-20">
          <div className="grid items-center gap-8 lg:grid-cols-12">
            <div className="lg:col-span-7">
              <h2 className="display text-[clamp(28px,4.6vw,46px)]">
                Four states, and one of them is
                <br />
                “we don’t know yet”.
              </h2>
              <p className="mt-4 max-w-xl text-[15px] leading-relaxed text-ink-900/80">
                Most products in most routines never earn a confident verdict.
                SkinProof says so instead of inventing a story, because a made-up
                result costs you months.
              </p>
            </div>
            <ul className="flex list-none flex-col gap-2.5 p-0 lg:col-span-5">
              {[
                ["Keep", "Held up through the window"],
                ["Likely useful", "Directional, not yet conclusive"],
                ["Evidence unclear", "Inside the noise floor"],
                ["Investigate", "Moved the wrong way"],
              ].map(([label, meaning]) => (
                <li
                  key={label}
                  className="flex items-center justify-between gap-4 rounded-[var(--radius)] bg-ink-900 px-4 py-3"
                >
                  <span className="font-bold text-honey-400">{label}</span>
                  <span className="text-right text-[12.5px] text-ink-300">
                    {meaning}
                  </span>
                </li>
              ))}
            </ul>
          </div>
        </div>
      </section>

      {/* ---------------------------------------------------------- surfaces */}
      <section
        id="surfaces"
        className="mx-auto max-w-[var(--content-max)] px-5 py-16 lg:px-10 lg:py-24"
      >
        <p className="eyebrow">The product</p>
        <h2 className="display mt-2.5 max-w-2xl text-[clamp(28px,4.4vw,44px)]">
          A workspace, not a feed.
        </h2>
        <div className="mt-10 grid min-w-0 grid-cols-1 gap-3.5 sm:grid-cols-2 lg:grid-cols-3">
          {SURFACES.map(({ title, body }, i) => (
            <LandingReveal key={title} delay={i * 0.05}>
              <article
              className="rounded-[var(--radius-lg)] border border-line bg-surface p-6 transition-colors duration-[var(--dur)] hover:border-line-strong hover:bg-surface-2"
            >
              <span className="tnum text-[12px] font-bold tracking-[0.14em] text-honey-700">
                0{i + 1}
              </span>
              <h3 className="display-sm mt-4 text-[18px]">{title}</h3>
              <p className="mt-2 text-[13.5px] leading-relaxed text-muted">{body}</p>
              </article>
            </LandingReveal>
          ))}
        </div>
      </section>

      {/* ----------------------------------------------------------- honesty */}
      <section id="honesty" className="border-y border-line bg-surface-2">
        <div className="mx-auto max-w-[var(--content-max)] px-5 py-16 lg:px-10 lg:py-20">
          <div className="grid gap-8 lg:grid-cols-12">
            <div className="lg:col-span-5">
              <p className="eyebrow">What this is not</p>
              <h2 className="display mt-2.5 text-[clamp(26px,3.8vw,38px)]">
                The limits, stated up front.
              </h2>
            </div>
            <ul className="flex list-none flex-col gap-4 p-0 lg:col-span-7">
              {HONESTY.map((line) => (
                <li key={line} className="flex gap-3">
                  <span aria-hidden className="mt-2 size-1.5 shrink-0 rounded-full bg-honey-600" />
                  <span className="text-[14px] leading-relaxed text-muted">{line}</span>
                </li>
              ))}
            </ul>
          </div>
        </div>
      </section>

      {/* --------------------------------------------------------------- CTA */}
      <section className="mx-auto max-w-[var(--content-max)] px-5 py-20 lg:px-10 lg:py-28">
        <div className="rounded-[var(--radius-xl)] bg-ink-900 px-6 py-14 text-center lg:px-16">
          <h2 className="display mx-auto max-w-2xl text-[clamp(28px,4.6vw,46px)] text-paper">
            Start the record today. Read it in twelve weeks.
          </h2>
          <p className="mx-auto mt-4 max-w-lg text-[14.5px] leading-relaxed text-ink-400">
            The first capture is a baseline. The second is the first comparison.
            Everything after that is evidence.
          </p>
          <div className="mt-8 flex flex-wrap justify-center gap-3">
            <ButtonLink href="/start" size="lg">
              Create a private profile
            </ButtonLink>
            <ButtonLink href="/sign-in" variant="dark" size="lg" className="border border-ink-700">
              I already have a profile
            </ButtonLink>
          </div>
        </div>
      </section>

      {/* ------------------------------------------------------------ footer */}
      <footer className="border-t border-line">
        <div className="mx-auto flex max-w-[var(--content-max)] flex-wrap items-center gap-4 px-5 py-8 lg:px-10">
          <div className="flex items-center gap-2.5">
            <LogoMark size={24} />
            <span className="text-[13px] font-bold">SkinProof</span>
          </div>
          <p className="text-[12px] text-subtle">
            Cosmetic appearance tracking, never diagnosis. Recommendations are not
            for sale.
          </p>
          <div className="ml-auto flex gap-5 text-[12.5px] font-semibold text-muted">
            <Link href="/start" className="hover:text-fg">
              Get started
            </Link>
            <Link href="/sign-in" className="hover:text-fg">
              Sign in
            </Link>
          </div>
        </div>
      </footer>
    </div>
  );
}
