export const metadata = {
  title: 'Privacy Policy | GlowUp AI',
  description: 'How GlowUp AI handles your account, photo, and usage data.',
}

export default function PrivacyPage() {
  return (
    <main className="min-h-screen bg-honey-50 px-6 py-16 text-gray-800">
      <article className="mx-auto max-w-3xl rounded-2xl bg-white p-8 shadow-sm md:p-12">
        <a className="text-honey-700 hover:underline" href="/">← GlowUp AI</a>
        <h1 className="mt-6 text-4xl font-bold">Privacy Policy</h1>
        <p className="mt-3 text-sm text-gray-500">Last updated: September 8, 2026</p>
        <div className="mt-8 space-y-6 leading-7 text-gray-700">
          <section><h2 className="text-xl font-semibold text-gray-900">What we collect</h2><p>GlowUp AI stores the account and routine information you provide, capture metadata, and photos that you choose to submit for analysis. Photos are sensitive personal data and are used only to provide the requested tracking features.</p></section>
          <section><h2 className="text-xl font-semibold text-gray-900">How we use it</h2><p>We use your information to authenticate your account, process captures, show your history, improve app reliability, and respond to support requests. We do not sell personal information or use photos for advertising.</p></section>
          <section><h2 className="text-xl font-semibold text-gray-900">Your choices</h2><p>You can manage consent in the app, export your data, and request deletion of your account and associated data. Contact <a className="text-honey-700 underline" href="mailto:support@glowupai.app">support@glowupai.app</a> for help with a privacy request.</p></section>
          <section><h2 className="text-xl font-semibold text-gray-900">Security</h2><p>We use access controls and private storage for submitted photos. No security measure is absolute, so keep your account credentials private and report suspected access promptly.</p></section>
        </div>
      </article>
    </main>
  )
}
