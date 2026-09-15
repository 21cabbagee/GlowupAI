export const metadata = {
  title: 'Medical Disclaimer | GlowUp AI',
  description: 'Important medical disclaimer for GlowUp AI.',
}

export default function DisclaimerPage() {
  return (
    <main className="min-h-screen bg-honey-50 px-6 py-16 text-gray-800">
      <article className="mx-auto max-w-3xl rounded-2xl bg-white p-8 shadow-sm md:p-12">
        <a className="text-honey-700 hover:underline" href="/">← GlowUp AI</a>
        <h1 className="mt-6 text-4xl font-bold">Medical Disclaimer</h1>
        <div className="mt-8 space-y-6 leading-7 text-gray-700">
          <p>GlowUp AI is a cosmetic tracking tool. It does not diagnose, treat, cure, or prevent any medical condition. Its capture analysis and trends can be affected by lighting, image quality, routine changes, and many other factors.</p>
          <p>Do not use GlowUp AI as a substitute for professional medical advice. If you have a rash, pain, rapid change in a lesion, signs of infection, or any other health concern, contact a licensed healthcare professional promptly.</p>
        </div>
      </article>
    </main>
  )
}
