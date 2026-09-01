'use client'

import { useState } from 'react'
import { motion } from 'framer-motion'

export default function Home() {
  const [email, setEmail] = useState('')

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault()
    // Handle email submission for iOS waitlist
    console.log('Email submitted:', email)
    alert('Thanks! We\'ll notify you when iOS launches.')
    setEmail('')
  }

  return (
    <main className="min-h-screen">
      {/* Hero Section */}
      <section className="relative overflow-hidden bg-gradient-to-br from-honey-50 via-white to-honey-100 py-20 px-6">
        <div className="max-w-6xl mx-auto">
          <div className="grid md:grid-cols-2 gap-12 items-center">
            <motion.div
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ duration: 0.6 }}
            >
              <h1 className="text-5xl md:text-6xl font-bold mb-6">
                Track your skin,{' '}
                <span className="text-gradient">with evidence.</span>
              </h1>
              <p className="text-xl text-gray-600 mb-8">
                See real change over time with photo-based tracking. No guessing, no influencer hype—just data about what works for YOUR skin.
              </p>
              <div className="flex flex-col sm:flex-row gap-4">
                <a
                  href="https://play.google.com/store/apps/details?id=com.glowup.ai"
                  className="inline-flex items-center justify-center px-8 py-4 text-lg font-semibold text-white bg-gradient-honey rounded-lg hover:opacity-90 transition-opacity shadow-lg"
                >
                  <svg className="w-6 h-6 mr-2" viewBox="0 0 24 24" fill="currentColor">
                    <path d="M3,20.5V3.5C3,2.91 3.34,2.39 3.84,2.15L13.69,12L3.84,21.85C3.34,21.6 3,21.09 3,20.5M16.81,15.12L6.05,21.34L14.54,12.85L16.81,15.12M20.16,10.81C20.5,11.08 20.75,11.5 20.75,12C20.75,12.5 20.53,12.9 20.18,13.18L17.89,14.5L15.39,12L17.89,9.5L20.16,10.81M6.05,2.66L16.81,8.88L14.54,11.15L6.05,2.66Z" />
                  </svg>
                  Download for Android
                </a>
                <button className="inline-flex items-center justify-center px-8 py-4 text-lg font-semibold text-honey-600 bg-white rounded-lg border-2 border-honey-400 hover:bg-honey-50 transition-colors">
                  iOS Coming Soon
                </button>
              </div>
              <p className="text-sm text-gray-500 mt-4">
                Free to download • Privacy-first • No credit card required
              </p>
            </motion.div>

            <motion.div
              initial={{ opacity: 0, scale: 0.95 }}
              animate={{ opacity: 1, scale: 1 }}
              transition={{ duration: 0.6, delay: 0.2 }}
              className="relative"
            >
              <div className="relative w-full max-w-md mx-auto">
                <div className="absolute inset-0 bg-gradient-honey opacity-20 blur-3xl rounded-full"></div>
                <div className="relative bg-white rounded-3xl shadow-2xl p-8 border border-honey-200">
                  <div className="text-center">
                    <div className="text-6xl mb-4">📸</div>
                    <h3 className="text-2xl font-bold mb-2">Start Tracking Today</h3>
                    <p className="text-gray-600">Take consistent photos and see what actually works</p>
                  </div>
                </div>
              </div>
            </motion.div>
          </div>
        </div>
      </section>

      {/* Features Section */}
      <section className="py-20 px-6 bg-white">
        <div className="max-w-6xl mx-auto">
          <div className="text-center mb-16">
            <h2 className="text-4xl font-bold mb-4">Why GlowUp AI?</h2>
            <p className="text-xl text-gray-600">Evidence-based tracking that shows real results</p>
          </div>

          <div className="grid md:grid-cols-3 gap-8">
            <motion.div
              initial={{ opacity: 0, y: 20 }}
              whileInView={{ opacity: 1, y: 0 }}
              viewport={{ once: true }}
              transition={{ duration: 0.5 }}
              className="bg-honey-50 rounded-2xl p-8 border border-honey-200"
            >
              <div className="text-5xl mb-4">📸</div>
              <h3 className="text-2xl font-bold mb-3">Consistent Tracking</h3>
              <p className="text-gray-700">
                Face detection ensures same lighting, same position every time. No more guessing if changes are real or just different lighting.
              </p>
            </motion.div>

            <motion.div
              initial={{ opacity: 0, y: 20 }}
              whileInView={{ opacity: 1, y: 0 }}
              viewport={{ once: true }}
              transition={{ duration: 0.5, delay: 0.1 }}
              className="bg-honey-50 rounded-2xl p-8 border border-honey-200"
            >
              <div className="text-5xl mb-4">📊</div>
              <h3 className="text-2xl font-bold mb-3">Evidence-Based</h3>
              <p className="text-gray-700">
                Track 6+ skin metrics automatically. See actual changes in redness, texture, pores, and more—not placebo effect.
              </p>
            </motion.div>

            <motion.div
              initial={{ opacity: 0, y: 20 }}
              whileInView={{ opacity: 1, y: 0 }}
              viewport={{ once: true }}
              transition={{ duration: 0.5, delay: 0.2 }}
              className="bg-honey-50 rounded-2xl p-8 border border-honey-200"
            >
              <div className="text-5xl mb-4">🔥</div>
              <h3 className="text-2xl font-bold mb-3">Build the Habit</h3>
              <p className="text-gray-700">
                Streak system keeps you motivated. Consistency is the secret to skincare success—we make it easy to stay on track.
              </p>
            </motion.div>
          </div>
        </div>
      </section>

      {/* How It Works */}
      <section className="py-20 px-6 bg-gradient-to-br from-honey-50 to-white">
        <div className="max-w-6xl mx-auto">
          <div className="text-center mb-16">
            <h2 className="text-4xl font-bold mb-4">How It Works</h2>
            <p className="text-xl text-gray-600">Start tracking in 3 simple steps</p>
          </div>

          <div className="grid md:grid-cols-3 gap-12">
            <div className="text-center">
              <div className="w-16 h-16 bg-gradient-honey text-white rounded-full flex items-center justify-center text-2xl font-bold mx-auto mb-6">
                1
              </div>
              <h3 className="text-xl font-bold mb-3">Take Daily Photos</h3>
              <p className="text-gray-600">
                Guided capture with face detection ensures consistency. Just 2 minutes, 3x per week.
              </p>
            </div>

            <div className="text-center">
              <div className="w-16 h-16 bg-gradient-honey text-white rounded-full flex items-center justify-center text-2xl font-bold mx-auto mb-6">
                2
              </div>
              <h3 className="text-xl font-bold mb-3">Track Metrics Automatically</h3>
              <p className="text-gray-600">
                AI analyzes your photos and tracks redness, texture, pores, and more—no manual input needed.
              </p>
            </div>

            <div className="text-center">
              <div className="w-16 h-16 bg-gradient-honey text-white rounded-full flex items-center justify-center text-2xl font-bold mx-auto mb-6">
                3
              </div>
              <h3 className="text-xl font-bold mb-3">Compare Progress</h3>
              <p className="text-gray-600">
                See before/after comparisons and trend charts. Discover what actually works for YOUR skin.
              </p>
            </div>
          </div>
        </div>
      </section>

      {/* Social Proof */}
      <section className="py-20 px-6 bg-white">
        <div className="max-w-4xl mx-auto text-center">
          <p className="text-2xl font-semibold text-gray-700 mb-12">
            Join 100+ early testers tracking their skin scientifically
          </p>

          <div className="grid md:grid-cols-2 gap-8">
            <div className="bg-honey-50 rounded-xl p-6 border border-honey-200">
              <p className="text-gray-700 italic mb-4">
                "Finally know which products are actually working. The comparison mode showed me my expensive serum wasn't doing anything!"
              </p>
              <p className="font-semibold">— Sarah, 28</p>
            </div>

            <div className="bg-honey-50 rounded-xl p-6 border border-honey-200">
              <p className="text-gray-700 italic mb-4">
                "The streak system keeps me consistent. I've tracked for 60 days and can see real improvement in my redness."
              </p>
              <p className="font-semibold">— Marcus, 34</p>
            </div>
          </div>
        </div>
      </section>

      {/* FAQ */}
      <section className="py-20 px-6 bg-gray-50">
        <div className="max-w-4xl mx-auto">
          <h2 className="text-4xl font-bold text-center mb-12">Frequently Asked Questions</h2>

          <div className="space-y-6">
            <details className="bg-white rounded-lg p-6 shadow-sm">
              <summary className="font-semibold text-lg cursor-pointer">
                Is this a medical diagnosis tool?
              </summary>
              <p className="mt-4 text-gray-600">
                No. GlowUp AI is for cosmetic tracking only, not medical diagnosis. We track relative changes in appearance to help you optimize your skincare routine. For any medical skin concerns, always consult a dermatologist.
              </p>
            </details>

            <details className="bg-white rounded-lg p-6 shadow-sm">
              <summary className="font-semibold text-lg cursor-pointer">
                How accurate are the metrics?
              </summary>
              <p className="mt-4 text-gray-600">
                Our metrics track relative changes over time, not absolute values. The key is consistency—same lighting, same position. We provide confidence scores for each metric and show trends rather than claiming perfect accuracy.
              </p>
            </details>

            <details className="bg-white rounded-lg p-6 shadow-sm">
              <summary className="font-semibold text-lg cursor-pointer">
                Is my data private?
              </summary>
              <p className="mt-4 text-gray-600">
                Yes! Photos are stored locally on your device by default. You control whether to enable cloud backup. We never share your data without explicit consent, and you can export or delete everything at any time.
              </p>
            </details>

            <details className="bg-white rounded-lg p-6 shadow-sm">
              <summary className="font-semibold text-lg cursor-pointer">
                How long until I see results?
              </summary>
              <p className="mt-4 text-gray-600">
                Most skincare products take 4-6 weeks to show visible results. With consistent tracking, you'll start seeing trends after 2-3 weeks, and meaningful comparisons after 30-60 days.
              </p>
            </details>

            <details className="bg-white rounded-lg p-6 shadow-sm">
              <summary className="font-semibold text-lg cursor-pointer">
                Is it really free?
              </summary>
              <p className="mt-4 text-gray-600">
                Yes! Core features are free forever: unlimited photo captures, basic metrics, 30-day history, and streak tracking. Premium ($9.99/month) unlocks unlimited history, experiments, and advanced features.
              </p>
            </details>
          </div>
        </div>
      </section>

      {/* Final CTA */}
      <section className="py-20 px-6 bg-gradient-honey text-white">
        <div className="max-w-4xl mx-auto text-center">
          <h2 className="text-4xl md:text-5xl font-bold mb-6">
            Ready to discover what works for YOUR skin?
          </h2>
          <p className="text-xl mb-8 opacity-90">
            Start tracking today. Free for Android.
          </p>
          <a
            href="https://play.google.com/store/apps/details?id=com.glowup.ai"
            className="inline-flex items-center justify-center px-8 py-4 text-lg font-semibold text-honey-600 bg-white rounded-lg hover:bg-gray-100 transition-colors shadow-lg"
          >
            <svg className="w-6 h-6 mr-2" viewBox="0 0 24 24" fill="currentColor">
              <path d="M3,20.5V3.5C3,2.91 3.34,2.39 3.84,2.15L13.69,12L3.84,21.85C3.34,21.6 3,21.09 3,20.5M16.81,15.12L6.05,21.34L14.54,12.85L16.81,15.12M20.16,10.81C20.5,11.08 20.75,11.5 20.75,12C20.75,12.5 20.53,12.9 20.18,13.18L17.89,14.5L15.39,12L17.89,9.5L20.16,10.81M6.05,2.66L16.81,8.88L14.54,11.15L6.05,2.66Z" />
            </svg>
            Download Now
          </a>

          <div className="mt-12">
            <p className="text-lg mb-4 opacity-90">Want iOS? Join the waitlist:</p>
            <form onSubmit={handleSubmit} className="flex flex-col sm:flex-row gap-4 max-w-md mx-auto">
              <input
                type="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                placeholder="your@email.com"
                required
                className="flex-1 px-4 py-3 rounded-lg text-gray-900 focus:outline-none focus:ring-2 focus:ring-white"
              />
              <button
                type="submit"
                className="px-6 py-3 bg-gray-900 text-white rounded-lg font-semibold hover:bg-gray-800 transition-colors"
              >
                Notify Me
              </button>
            </form>
          </div>
        </div>
      </section>

      {/* Footer */}
      <footer className="py-12 px-6 bg-gray-900 text-gray-400">
        <div className="max-w-6xl mx-auto">
          <div className="grid md:grid-cols-4 gap-8 mb-8">
            <div>
              <h3 className="text-white font-bold text-xl mb-4 text-gradient">GlowUp AI</h3>
              <p className="text-sm">
                Track what actually works for YOUR skin with evidence-based photo tracking.
              </p>
            </div>

            <div>
              <h4 className="text-white font-semibold mb-4">Product</h4>
              <ul className="space-y-2 text-sm">
                <li><a href="#features" className="hover:text-white transition-colors">Features</a></li>
                <li><a href="#how-it-works" className="hover:text-white transition-colors">How It Works</a></li>
                <li><a href="#faq" className="hover:text-white transition-colors">FAQ</a></li>
                <li><a href="#pricing" className="hover:text-white transition-colors">Pricing</a></li>
              </ul>
            </div>

            <div>
              <h4 className="text-white font-semibold mb-4">Legal</h4>
              <ul className="space-y-2 text-sm">
                <li><a href="/privacy" className="hover:text-white transition-colors">Privacy Policy</a></li>
                <li><a href="/terms" className="hover:text-white transition-colors">Terms of Service</a></li>
                <li><a href="/disclaimer" className="hover:text-white transition-colors">Medical Disclaimer</a></li>
              </ul>
            </div>

            <div>
              <h4 className="text-white font-semibold mb-4">Connect</h4>
              <ul className="space-y-2 text-sm">
                <li><a href="mailto:support@glowupai.app" className="hover:text-white transition-colors">Contact</a></li>
                <li><a href="https://twitter.com/glowupai" className="hover:text-white transition-colors">Twitter</a></li>
                <li><a href="https://github.com/glowupai" className="hover:text-white transition-colors">GitHub</a></li>
              </ul>
            </div>
          </div>

          <div className="border-t border-gray-800 pt-8 text-center text-sm">
            <p>&copy; 2026 GlowUp AI. All rights reserved.</p>
            <p className="mt-2 text-xs">
              GlowUp AI is a cosmetic tracking tool, not a medical device. Not intended for diagnosis or treatment.
            </p>
          </div>
        </div>
      </footer>
    </main>
  )
}
