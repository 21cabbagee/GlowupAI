"use client";

import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { motion } from "motion/react";
import { useEffect, useState } from "react";
import { useSession } from "@/lib/session";
import { useTheme } from "@/lib/theme";
import { Button, Tag, cx } from "./ui";
import {
  AccountIcon,
  CameraIcon,

  HomeIcon,
  InsightIcon,
  LogoMark,
  MoonIcon,
  RoutineIcon,
  SunIcon,
  type IconProps,
} from "./icons";

interface NavItem {
  href: string;
  label: string;
  Icon: (p: IconProps) => React.ReactElement;
}

const NAV: NavItem[] = [
  { href: "/home", label: "Home", Icon: HomeIcon },
  { href: "/routine", label: "Routine", Icon: RoutineIcon },
  { href: "/capture", label: "Capture", Icon: CameraIcon },
  { href: "/insights", label: "Insights", Icon: InsightIcon },

  { href: "/account", label: "You", Icon: AccountIcon },
];

/** Mobile keeps five tabs; Capture is promoted to the centre FAB. */
const TABS = NAV.filter((n) => n.href !== "/capture");

export function AppShell({ children }: { children: React.ReactNode }) {
  const pathname = usePathname();
  const router = useRouter();
  const { userId, ready } = useSession();

  // Everything under the shell needs a profile; bounce to onboarding if absent.
  useEffect(() => {
    if (ready && !userId) router.replace("/start");
  }, [ready, userId, router]);

  if (!ready || !userId) {
    return (
      <div className="grid min-h-dvh place-items-center bg-paper">
        <LogoMark size={40} className="animate-pulse" />
      </div>
    );
  }

  return (
    <div className="min-h-dvh bg-paper">
      <DesktopRail pathname={pathname} />
      <MobileHeader />

      <main className="min-w-0 overflow-x-clip lg:pl-[var(--rail-w)]">
        <div
          className={cx(
            "mx-auto w-full max-w-[var(--content-max)]",
            "px-4 pt-5 pb-safe-tab sm:px-6 lg:px-10 lg:pt-10 lg:pb-16",
          )}
        >
          {children}
        </div>
      </main>

      <MobileTabBar pathname={pathname} />
    </div>
  );
}

/* --------------------------------------------------------------- desktop */

function DesktopRail({ pathname }: { pathname: string }) {
  const { profile, isPremium } = useSession();

  return (
    <aside className="fixed inset-y-0 left-0 z-30 hidden w-[var(--rail-w)] flex-col border-r border-line bg-surface lg:flex">
      <Link href="/home" className="flex items-center gap-2.5 px-5 py-6">
        <LogoMark />
        <span className="display-sm text-[19px]">
          Skin<span className="text-honey-600">Proof</span>
        </span>
      </Link>

      <nav className="flex flex-1 flex-col gap-1 px-3">
        {NAV.map(({ href, label, Icon }) => {
          const active = pathname === href;
          return (
            <Link
              key={href}
              href={href}
              aria-current={active ? "page" : undefined}
              className={cx(
                "relative flex items-center gap-3 rounded-[var(--radius)] px-3 py-2.5",
                "text-[14px] font-semibold transition-colors duration-[var(--dur-fast)]",
                active
                  ? "bg-honey-100 text-honey-800"
                  : "text-muted hover:bg-surface-2 hover:text-fg",
              )}
            >
              {active && (
                <motion.span
                  layoutId="rail-active"
                  className="absolute top-1/2 left-0 h-6 w-[3px] -translate-y-1/2 rounded-r-full bg-primary"
                  transition={{ duration: 0.22, ease: [0.2, 0.8, 0.2, 1] }}
                />
              )}
              <Icon size={20} />
              {label}
            </Link>
          );
        })}
      </nav>

      <div className="border-t border-line p-3">
        <div className="flex items-center justify-between gap-2 px-1">
          <div className="min-w-0">
            <p className="truncate text-[13px] font-semibold">
              {profile?.user.skin_type ?? "Personal"} profile
            </p>
            <p className="text-[11px] text-subtle">
              {isPremium ? "Premium" : "Free plan"}
            </p>
          </div>
          <ThemeToggle />
        </div>
      </div>
    </aside>
  );
}

/* ---------------------------------------------------------------- mobile */

function MobileHeader() {
  const { isPremium } = useSession();

  return (
    <header className="sticky top-0 z-30 border-b border-line bg-paper/85 backdrop-blur-xl lg:hidden pt-safe">
      <div className="flex h-[var(--header-h)] items-center gap-3 px-4">
        <Link href="/home" className="flex items-center gap-2">
          <LogoMark size={26} />
          <span className="display-sm text-[16px]">
            Skin<span className="text-honey-600">Proof</span>
          </span>
        </Link>
        <div className="ml-auto flex items-center gap-2">
          {isPremium && <Tag tone="honey">Premium</Tag>}
          <ThemeToggle />
        </div>
      </div>
    </header>
  );
}

function MobileTabBar({ pathname }: { pathname: string }) {
  return (
    <nav
      aria-label="Primary"
      className="fixed inset-x-0 bottom-0 z-40 border-t border-line bg-paper/92 backdrop-blur-xl lg:hidden pb-safe"
    >
      <div className="relative flex h-[var(--tabbar-h)] items-stretch">
        {TABS.slice(0, 2).map((t) => (
          <Tab key={t.href} item={t} active={pathname === t.href} />
        ))}

        {/* Capture FAB sits in the tab strip, raised above the bar */}
        <div className="relative w-[68px] shrink-0">
          <Link
            href="/capture"
            aria-label="Guided capture"
            className={cx(
              "absolute -top-5 left-1/2 grid size-14 -translate-x-1/2 place-items-center",
              "rounded-[var(--radius-full)] bg-primary text-on-primary",
              "transition-transform duration-[var(--dur-fast)]",
              "active:scale-90",
              pathname === "/capture" && "ring-4 ring-honey-200",
            )}
          >
            <CameraIcon size={25} />
          </Link>
        </div>

        {TABS.slice(2).map((t) => (
          <Tab key={t.href} item={t} active={pathname === t.href} />
        ))}
      </div>
    </nav>
  );
}

function Tab({ item, active }: { item: NavItem; active: boolean }) {
  const { Icon, href, label } = item;
  return (
    <Link
      href={href}
      aria-current={active ? "page" : undefined}
      className={cx(
        "flex flex-1 flex-col items-center justify-center gap-1 pt-1.5 pb-1",
        "text-[10.5px] font-bold tracking-wide transition-colors duration-[var(--dur-fast)]",
        active ? "text-honey-700" : "text-subtle",
      )}
    >
      <span className="relative grid place-items-center">
        {active && (
          <motion.span
            layoutId="tab-active"
            className="absolute -inset-x-3.5 -inset-y-1.5 rounded-[var(--radius-full)] bg-honey-100"
            transition={{ duration: 0.24, ease: [0.2, 0.8, 0.2, 1] }}
          />
        )}
        <Icon size={21} className="relative" />
      </span>
      {label}
    </Link>
  );
}

/* ---------------------------------------------------------------- shared */

function ThemeToggle() {
  const { mode, setMode } = useTheme();
  const [dark, setDark] = useState(false);

  // The rendered glyph depends on the resolved theme, which is only knowable
  // in the browser, so it is computed after mount.
  useEffect(() => {
    const resolve = () =>
      mode === "system"
        ? window.matchMedia("(prefers-color-scheme: dark)").matches
        : mode === "dark";
    setDark(resolve());
  }, [mode]);

  return (
    <button
      onClick={() => setMode(dark ? "light" : "dark")}
      aria-label={dark ? "Switch to light theme" : "Switch to dark theme"}
      className="grid size-9 shrink-0 place-items-center rounded-[var(--radius-full)] text-muted transition-colors hover:bg-surface-2 hover:text-fg"
    >
      {dark ? <SunIcon size={19} /> : <MoonIcon size={19} />}
    </button>
  );
}

/* ------------------------------------------------------- gates for screens */

/** Consistent "consent required" / "premium required" interception. */
export function Gate({
  children,
  requirePremium,
  requireConsent,
  fallback,
}: {
  children: React.ReactNode;
  requirePremium?: boolean;
  requireConsent?: boolean;
  fallback: React.ReactNode;
}) {
  const { isPremium, hasConsent } = useSession();
  if (requirePremium && !isPremium) return <>{fallback}</>;
  if (requireConsent && !hasConsent) return <>{fallback}</>;
  return <>{children}</>;
}

export function ConsentPrompt() {
  const { userId, refresh } = useSession();
  const [pending, setPending] = useState(false);

  return (
    <div className="rounded-[var(--radius-lg)] border border-honey-200 bg-honey-50 p-5">
      <p className="font-semibold text-honey-800">Facial-data consent required</p>
      <p className="mt-1.5 max-w-md text-[13px] leading-relaxed text-honey-800/85">
        Capture stores facial images. Consent is explicit, separate from your
        account, and revocable by deleting your data.
      </p>
      <Button
        className="mt-4"
        loading={pending}
        onClick={async () => {
          if (!userId) return;
          setPending(true);
          try {
            const { api } = await import("@/lib/api");
            await api.grantConsent(userId);
            await refresh();
          } finally {
            setPending(false);
          }
        }}
      >
        Grant consent
      </Button>
    </div>
  );
}
