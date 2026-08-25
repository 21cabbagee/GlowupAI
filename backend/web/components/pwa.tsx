"use client";

import { useEffect } from "react";

/**
 * Registers the service worker that makes SkinProof installable on Android.
 * Development is skipped so an aggressive cache never shadows a hot reload.
 */
export function ServiceWorker() {
  useEffect(() => {
    if (process.env.NODE_ENV !== "production") return;
    if (!("serviceWorker" in navigator)) return;
    navigator.serviceWorker.register("/sw.js").catch(() => undefined);
  }, []);

  return null;
}
