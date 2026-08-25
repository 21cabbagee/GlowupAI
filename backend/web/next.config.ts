import type { NextConfig } from "next";

/**
 * The FastAPI app owns every `/api/*` route. Proxying through Next keeps the
 * browser on a single origin, so there is no CORS config and no API base URL
 * to thread through the client.
 */
const API_ORIGIN = process.env.SKINPROOF_API_ORIGIN ?? "http://127.0.0.1:8000";

const nextConfig: NextConfig = {
  // Pin the workspace root. Without this, Turbopack walks up to the home
  // directory because a stray package-lock.json lives there.
  turbopack: { root: __dirname },

  async rewrites() {
    return [{ source: "/api/:path*", destination: `${API_ORIGIN}/api/:path*` }];
  },
};

export default nextConfig;
