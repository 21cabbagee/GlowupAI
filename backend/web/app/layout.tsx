import type { Metadata, Viewport } from "next";
import "./globals.css";
import { ServiceWorker } from "@/components/pwa";
import { SessionProvider } from "@/lib/session";
import { THEME_SCRIPT, ThemeProvider } from "@/lib/theme";

export const metadata: Metadata = {
  title: {
    default: "SkinProof — Measured, not remembered",
    template: "%s · SkinProof",
  },
  description:
    "Calibrated captures, one-variable routine experiments, and honest product verdicts built from your own appearance history.",
  applicationName: "SkinProof",
  manifest: "/manifest.webmanifest",
  appleWebApp: { capable: true, title: "SkinProof", statusBarStyle: "default" },
  icons: {
    // app/favicon.ico supplies the multi-resolution browser icon. Keep this
    // explicit because the PWA images live in /public rather than app/.
    icon: [
      { url: "/favicon.ico", sizes: "any" },
      { url: "/favicon-48.png", type: "image/png", sizes: "48x48" },
    ],
    shortcut: "/favicon.ico",
    apple: [{ url: "/icon-192.png", type: "image/png", sizes: "192x192" }],
  },
};

export const viewport: Viewport = {
  width: "device-width",
  initialScale: 1,
  // Installed-app feel without trapping pinch-zoom for low-vision users.
  maximumScale: 5,
  viewportFit: "cover",
  themeColor: [
    { media: "(prefers-color-scheme: light)", color: "#fffdf8" },
    { media: "(prefers-color-scheme: dark)", color: "#0f0d0a" },
  ],
};

export default function RootLayout({ children }: LayoutProps<"/">) {
  return (
    <html lang="en" suppressHydrationWarning>
      <head>
        {/* Stamps the stored theme before paint so dark mode never flashes. */}
        <script dangerouslySetInnerHTML={{ __html: THEME_SCRIPT }} />
      </head>
      <body>
        <ThemeProvider>
          <SessionProvider>{children}</SessionProvider>
        </ThemeProvider>
        <ServiceWorker />
      </body>
    </html>
  );
}
