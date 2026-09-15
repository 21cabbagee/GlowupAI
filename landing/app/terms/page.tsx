export const metadata = {
  title: 'Terms of Service | GlowUp AI',
  description: 'Terms governing use of GlowUp AI.',
}

export default function TermsPage() {
  return (
    <main className="min-h-screen bg-honey-50 px-6 py-16 text-gray-800">
      <article className="mx-auto max-w-3xl rounded-2xl bg-white p-8 shadow-sm md:p-12">
        <a className="text-honey-700 hover:underline" href="/">← GlowUp AI</a>
        <h1 className="mt-6 text-4xl font-bold">Terms of Service</h1>
        <p className="mt-3 text-sm text-gray-500">Last updated: September 8, 2026</p>
        <div className="mt-8 space-y-6 leading-7 text-gray-700">
          <section><h2 className="text-xl font-semibold text-gray-900">Using GlowUp AI</h2><p>You may use GlowUp AI for personal skincare tracking. Keep your account secure and provide accurate information. Do not use the service to upload content you do not have the right to use.</p></section>
          <section><h2 className="text-xl font-semibold text-gray-900">No medical advice</h2><p>GlowUp AI provides cosmetic appearance tracking, not medical advice, diagnosis, or treatment. Consult a qualified clinician for health concerns.</p></section>
          <section><h2 className="text-xl font-semibold text-gray-900">Availability</h2><p>We may update, maintain, or change the service. Features can depend on your device, network, subscription, and third-party services.</p></section>
          <section><h2 className="text-xl font-semibold text-gray-900">Contact</h2><p>Questions about these terms can be sent to <a className="text-honey-700 underline" href="mailto:support@glowupai.app">support@glowupai.app</a>.</p></section>
        </div>
      </article>
    </main>
  )
}
