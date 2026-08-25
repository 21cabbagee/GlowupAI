"use client";

/**
 * SkinProof primitives. Every screen composes from these so spacing, radii,
 * motion and colour stay consistent. Colours come only from globals.css tokens.
 */

import Link from "next/link";
import { motion } from "motion/react";
import type { ButtonHTMLAttributes, ReactNode } from "react";
import { VERDICT_COPY, type VerdictLabel } from "@/lib/api";

export function cx(...parts: (string | false | null | undefined)[]) {
  return parts.filter(Boolean).join(" ");
}

/* ------------------------------------------------------------------ Button */

type Variant = "primary" | "secondary" | "ghost" | "danger" | "dark";
type Size = "sm" | "md" | "lg";

const VARIANTS: Record<Variant, string> = {
  // ink-900 label on honey: 16:1 contrast. This is the signature button.
  primary:
    "bg-primary text-on-primary hover:bg-[var(--primary-press)]",
  secondary:
    "bg-surface text-fg border border-line hover:border-line-strong hover:bg-surface-2",
  ghost: "bg-transparent text-muted hover:bg-surface-2 hover:text-fg",
  danger:
    "bg-investigate-bg text-investigate border border-transparent hover:border-investigate",
  dark: "bg-ink-900 text-paper hover:bg-ink-800",
};

const SIZES: Record<Size, string> = {
  sm: "h-9 px-3.5 text-[13px] rounded-[var(--radius-sm)]",
  // 44px+ so mobile taps clear the Android accessibility minimum
  md: "h-11 px-5 text-[14px] rounded-[var(--radius)]",
  lg: "h-[52px] px-7 text-[15px] rounded-[var(--radius)]",
};

/** Shared so buttons and link-buttons cannot drift apart. */
export function buttonClass(
  variant: Variant = "primary",
  size: Size = "md",
  block?: boolean,
  className?: string,
) {
  return cx(
    "inline-flex items-center justify-center gap-2 font-semibold whitespace-nowrap",
    "transition-colors duration-[var(--dur-fast)] ease-[var(--ease)]",
    "active:scale-[0.97] disabled:pointer-events-none disabled:opacity-45",
    VARIANTS[variant],
    SIZES[size],
    block && "w-full",
    className,
  );
}

interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: Variant;
  size?: Size;
  block?: boolean;
  loading?: boolean;
}

export function Button({
  variant = "primary",
  size = "md",
  block,
  loading,
  className,
  children,
  disabled,
  ...rest
}: ButtonProps) {
  return (
    <button
      {...rest}
      disabled={disabled || loading}
      className={buttonClass(variant, size, block, className)}
    >
      {loading && <Spinner />}
      {children}
    </button>
  );
}

/**
 * A navigation control that looks like a Button. Kept separate because an
 * <a> inside a <button> is invalid HTML.
 */
export function ButtonLink({
  href,
  variant = "primary",
  size = "md",
  block,
  className,
  children,
}: {
  href: string;
  variant?: Variant;
  size?: Size;
  block?: boolean;
  className?: string;
  children: ReactNode;
}) {
  return (
    <Link href={href} className={buttonClass(variant, size, block, className)}>
      {children}
    </Link>
  );
}

function Spinner() {
  return (
    <span
      aria-hidden
      className="size-3.5 shrink-0 animate-spin rounded-full border-2 border-current border-t-transparent opacity-70"
    />
  );
}

/* -------------------------------------------------------------------- Card */

export function Card({
  className,
  children,
  as: Tag = "section",
  interactive,
}: {
  className?: string;
  children: ReactNode;
  as?: "section" | "div" | "article" | "li";
  interactive?: boolean;
}) {
  return (
    <Tag
      className={cx(
        "rounded-[var(--radius-lg)] border border-line bg-surface p-5",
        interactive &&
          "transition-colors duration-[var(--dur)] ease-[var(--ease)] hover:border-line-strong hover:bg-surface-2",
        className,
      )}
    >
      {children}
    </Tag>
  );
}

export function CardTitle({
  children,
  action,
}: {
  children: ReactNode;
  action?: ReactNode;
}) {
  return (
    <header className="mb-4 flex items-start justify-between gap-3">
      <h2 className="display-sm text-[17px]">{children}</h2>
      {action}
    </header>
  );
}

/* --------------------------------------------------------------- StatTile */

export function StatTile({
  value,
  label,
  hint,
  accent,
}: {
  value: ReactNode;
  label: string;
  hint?: string;
  /** Fills the tile with honey — use for exactly one tile per row. */
  accent?: boolean;
}) {
  return (
    <div
      className={cx(
        "rounded-[var(--radius-lg)] border p-5",
        accent ? "border-primary bg-primary text-on-primary" : "border-line bg-surface",
      )}
    >
      <div className="tnum display text-[34px]">{value}</div>
      <div
        className={cx(
          "mt-1 text-[12px] font-semibold tracking-wide uppercase",
          accent ? "text-ink-900/70" : "text-subtle",
        )}
      >
        {label}
      </div>
      {hint && (
        <div
          className={cx(
            "mt-2 text-[12px] leading-snug",
            accent ? "text-ink-900/75" : "text-muted",
          )}
        >
          {hint}
        </div>
      )}
    </div>
  );
}

/* --------------------------------------------------------------------- Tag */

type TagTone = "neutral" | "honey" | VerdictLabel;

const TONES: Record<TagTone, string> = {
  neutral: "bg-surface-2 text-muted border-line",
  honey: "bg-honey-100 text-honey-700 border-transparent",
  keep: "bg-keep-bg text-keep border-transparent",
  likely_useful: "bg-useful-bg text-useful border-transparent",
  evidence_unclear: "bg-unclear-bg text-unclear border-transparent",
  investigate: "bg-investigate-bg text-investigate border-transparent",
  locked: "bg-surface-2 text-subtle border-line",
};

export function Tag({
  tone = "neutral",
  children,
}: {
  tone?: TagTone;
  children: ReactNode;
}) {
  return (
    <span
      className={cx(
        "inline-flex items-center gap-1.5 rounded-[var(--radius-full)] border px-2.5 py-1",
        "text-[11px] font-bold tracking-wide whitespace-nowrap",
        TONES[tone],
      )}
    >
      {children}
    </span>
  );
}

export function VerdictTag({ label }: { label: VerdictLabel }) {
  return <Tag tone={label}>{VERDICT_COPY[label] ?? label}</Tag>;
}

/* ------------------------------------------------------------------- Field */

export function Field({
  label,
  hint,
  children,
  className,
}: {
  label: string;
  hint?: string;
  children: ReactNode;
  className?: string;
}) {
  return (
    <label className={cx("block", className)}>
      <span className="mb-1.5 block text-[12px] font-bold tracking-wide text-muted">
        {label}
      </span>
      {children}
      {hint && <span className="mt-1.5 block text-[12px] text-subtle">{hint}</span>}
    </label>
  );
}

export const inputClass = cx(
  "w-full rounded-[var(--radius-sm)] border border-line bg-surface px-3.5 py-2.5",
  "text-[14px] text-fg placeholder:text-ink-400",
  "transition-colors duration-[var(--dur-fast)]",
  "hover:border-line-strong focus:border-[var(--focus)]",
);

/* -------------------------------------------------------------- EmptyState */

export function EmptyState({
  title,
  body,
  action,
}: {
  title: string;
  body?: string;
  action?: ReactNode;
}) {
  return (
    <div className="rounded-[var(--radius)] border border-dashed border-line-strong bg-surface-2 px-5 py-8 text-center">
      <p className="font-semibold text-fg">{title}</p>
      {body && (
        <p className="mx-auto mt-1.5 max-w-sm text-[13px] leading-relaxed text-muted">
          {body}
        </p>
      )}
      {action && <div className="mt-4 flex justify-center">{action}</div>}
    </div>
  );
}

/* ---------------------------------------------------------------- Skeleton */

export function Skeleton({ className }: { className?: string }) {
  return <div className={cx("skeleton", className ?? "h-4 w-full")} />;
}

/* ------------------------------------------------------------------ Notice */

export function Notice({
  tone = "info",
  children,
}: {
  tone?: "info" | "error" | "success";
  children: ReactNode;
}) {
  const tones = {
    info: "bg-honey-50 text-honey-800 border-honey-200",
    error: "bg-investigate-bg text-investigate border-transparent",
    success: "bg-useful-bg text-useful border-transparent",
  } as const;
  return (
    <div
      role={tone === "error" ? "alert" : "status"}
      className={cx(
        "rounded-[var(--radius)] border px-4 py-3 text-[13px] leading-relaxed",
        tones[tone],
      )}
    >
      {children}
    </div>
  );
}

/* ------------------------------------------------------------- PageHeading */

export function PageHeading({
  eyebrow,
  title,
  children,
  action,
}: {
  eyebrow: string;
  title: string;
  children?: ReactNode;
  action?: ReactNode;
}) {
  return (
    <div className="mb-7 flex flex-wrap items-end justify-between gap-4">
      <div className="max-w-2xl">
        <p className="eyebrow">{eyebrow}</p>
        <h1 className="display mt-2 text-[clamp(28px,4.2vw,42px)]">{title}</h1>
        {children && (
          <p className="mt-2.5 text-[14px] leading-relaxed text-muted">{children}</p>
        )}
      </div>
      {action}
    </div>
  );
}

/* ------------------------------------------------------------------- Rows */

export function Row({
  children,
  className,
}: {
  children: ReactNode;
  className?: string;
}) {
  return (
    <div
      className={cx(
        "flex items-center gap-3 border-b border-line py-3 last:border-0",
        className,
      )}
    >
      {children}
    </div>
  );
}

/* --------------------------------------------------------------- PaywallCard */

export function PaywallCard({
  title,
  body,
  onUnlock,
  pending,
}: {
  title: string;
  body: string;
  onUnlock: () => void;
  pending?: boolean;
}) {
  return (
    <Card className="relative overflow-hidden">
      {/* A single flat honey band, not a gradient wash */}
      <div aria-hidden className="absolute inset-x-0 top-0 h-1 bg-primary" />
      <Tag tone="honey">Premium</Tag>
      <h2 className="display-sm mt-3 text-[20px]">{title}</h2>
      <p className="mt-2 max-w-md text-[14px] leading-relaxed text-muted">{body}</p>
      <Button className="mt-5" onClick={onUnlock} loading={pending}>
        Unlock Premium
      </Button>
      <p className="mt-3 text-[12px] text-subtle">
        Local checkout. Verdicts are never for sale.
      </p>
    </Card>
  );
}

/* ---------------------------------------------------------------- Reveal */

/** Standard entrance: a small fade-in that honours reduced motion via CSS. */
export function Reveal({
  children,
  delay = 0,
  className,
}: {
  children: ReactNode;
  delay?: number;
  className?: string;
}) {
  return (
    <motion.div
      initial={{ opacity: 0, y: 6 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.24, delay, ease: [0.2, 0.8, 0.2, 1] }}
      className={className}
    >
      {children}
    </motion.div>
  );
}
