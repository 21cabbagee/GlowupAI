"use client";

import { createContext, useCallback, useContext, useEffect, useState } from "react";

type Mode = "light" | "dark" | "system";

const KEY = "skinproof_theme";

const ThemeContext = createContext<{
  mode: Mode;
  setMode: (m: Mode) => void;
} | null>(null);

/**
 * Writes `data-theme` on <html>. "system" removes the attribute so the
 * `prefers-color-scheme` rules in globals.css take over.
 */
export function ThemeProvider({ children }: { children: React.ReactNode }) {
  const [mode, setModeState] = useState<Mode>("system");

  useEffect(() => {
    const stored = localStorage.getItem(KEY) as Mode | null;
    if (stored) {
      setModeState(stored);
      apply(stored);
    }
  }, []);

  const setMode = useCallback((m: Mode) => {
    setModeState(m);
    localStorage.setItem(KEY, m);
    apply(m);
  }, []);

  return (
    <ThemeContext.Provider value={{ mode, setMode }}>
      {children}
    </ThemeContext.Provider>
  );
}

function apply(m: Mode) {
  const root = document.documentElement;
  if (m === "system") root.removeAttribute("data-theme");
  else root.setAttribute("data-theme", m);
}

export function useTheme() {
  const ctx = useContext(ThemeContext);
  if (!ctx) throw new Error("useTheme must be used inside ThemeProvider");
  return ctx;
}

/**
 * Runs before paint to stamp the stored theme, preventing a light flash on
 * load for dark-mode users. Injected as an inline script in the root layout.
 */
export const THEME_SCRIPT = `try{var m=localStorage.getItem("${KEY}");if(m&&m!=="system")document.documentElement.setAttribute("data-theme",m)}catch(e){}`;
