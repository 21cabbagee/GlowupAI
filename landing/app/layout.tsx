import type { Metadata } from 'next'
import './globals.css'

export const metadata: Metadata = {
  title: 'GlowUp AI - Track Your Skin Progress with Evidence',
  description: 'See real change over time with photo-based tracking. Scientific skincare experiments. Privacy-first. Free for Android.',
  keywords: 'skincare tracker, skin tracking app, skincare progress, before after photos, evidence-based skincare',
  openGraph: {
    title: 'GlowUp AI - Track Your Skin Progress with Evidence',
    description: 'See real change over time with photo-based tracking. Scientific approach to skincare.',
    url: 'https://glowupai.app',
    siteName: 'GlowUp AI',
    locale: 'en_US',
    type: 'website',
  },
  twitter: {
    card: 'summary_large_image',
    title: 'GlowUp AI - Track Your Skin Progress',
    description: 'Evidence-based skincare tracking. Free for Android.',
  },
}

export default function RootLayout({
  children,
}: {
  children: React.ReactNode
}) {
  return (
    <html lang="en">
      <body>{children}</body>
    </html>
  )
}
