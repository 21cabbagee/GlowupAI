import Link from "next/link";
import type { Metadata } from "next";
import { LogoMark } from "@/components/icons";

export const metadata: Metadata = {
  title: "Page not found",
  description: "The requested SkinProof page could not be found.",
};

/** Root-level fallback for unknown URLs and route-level notFound() calls. */
export default function NotFound() {
  return (
    <main className="grid min-h-dvh place-items-center bg-paper px-5 py-10">
      <section
        aria-labelledby="not-found-title"
        className="w-full max-w-md rounded-[var(--radius-xl)] border border-line bg-surface p-7 text-center sm:p-10"
      >
        <div className="mx-auto grid size-14 place-items-center rounded-[18px] bg-honey-100">
          <LogoMark size={34} />
        </div>
        <p className="mt-7 text-[12px] font-bold tracking-[0.18em] text-honey-700 uppercase">
          Error 404
        </p>
        <h1 id="not-found-title" className="display mt-2 text-[clamp(32px,8vw,44px)]">
          This page isn&apos;t in your record.
        </h1>
        <p className="mt-4 text-[15px] leading-relaxed text-muted">
          The link may be out of date, or the page may have moved. Return to a
          familiar part of SkinProof to continue.
        </p>
        <div className="mt-7 flex flex-col justify-center gap-3 sm:flex-row">
          <Link
            href="/"
            className="inline-flex h-11 items-center justify-center rounded-[var(--radius)] bg-primary px-5 text-[14px] font-semibold text-on-primary transition-colors duration-[var(--dur-fast)] hover:bg-[var(--primary-press)] focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[var(--focus)]"
          >
            Go to the homepage
          </Link>
          <Link
            href="/home"
            className="inline-flex h-11 items-center justify-center rounded-[var(--radius)] border border-line bg-surface px-5 text-[14px] font-semibold text-fg transition-colors duration-[var(--dur-fast)] hover:border-line-strong hover:bg-surface-2 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[var(--focus)]"
          >
            Open workspace
          </Link>
        </div>
      </section>
    </main>
  );
}
