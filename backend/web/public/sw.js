/**
 * SkinProof service worker.
 *
 * Deliberately conservative: it makes the app installable and keeps the shell
 * available offline, but it never caches `/api/*`. Appearance data is
 * personal and time-sensitive, and a stale metric is worse than no metric.
 */

const CACHE = "skinproof-shell-v1";
const SHELL = ["/", "/home", "/manifest.webmanifest", "/icon-192.png"];

self.addEventListener("install", (event) => {
  event.waitUntil(
    caches
      .open(CACHE)
      // Individually, so one missing entry cannot fail the whole install.
      .then((cache) => Promise.allSettled(SHELL.map((url) => cache.add(url))))
      .then(() => self.skipWaiting()),
  );
});

self.addEventListener("activate", (event) => {
  event.waitUntil(
    caches
      .keys()
      .then((keys) =>
        Promise.all(keys.filter((k) => k !== CACHE).map((k) => caches.delete(k))),
      )
      .then(() => self.clients.claim()),
  );
});

self.addEventListener("fetch", (event) => {
  const { request } = event;
  if (request.method !== "GET") return;

  const url = new URL(request.url);
  if (url.origin !== self.location.origin) return;
  // Never serve personal data from a cache.
  if (url.pathname.startsWith("/api/")) return;

  // Navigations: network first, cached shell only as an offline fallback.
  if (request.mode === "navigate") {
    event.respondWith(
      fetch(request).catch(() =>
        caches.match(request).then((hit) => hit ?? caches.match("/home")).then(
          (hit) =>
            hit ??
            new Response("Offline", {
              status: 503,
              headers: { "Content-Type": "text/plain" },
            }),
        ),
      ),
    );
    return;
  }

  // Static assets: cache first, then fill the cache in the background.
  event.respondWith(
    caches.match(request).then(
      (hit) =>
        hit ??
        fetch(request).then((res) => {
          if (res.ok && res.type === "basic") {
            const copy = res.clone();
            caches.open(CACHE).then((cache) => cache.put(request, copy));
          }
          return res;
        }),
    ),
  );
});
