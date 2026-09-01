# GlowUp AI Landing Page

Simple, conversion-focused landing page built with Next.js 14, Tailwind CSS, and Framer Motion.

## Features

- **Hero Section** - Clear value proposition with download CTA
- **Features** - 3 key benefits (Consistent Tracking, Evidence-Based, Build the Habit)
- **How It Works** - 3-step process explanation
- **Social Proof** - Early user testimonials
- **FAQ** - Common questions answered
- **CTA Section** - Download + iOS waitlist
- **Footer** - Links to privacy policy, terms, contact

## Tech Stack

- **Next.js 14** - React framework with App Router
- **TypeScript** - Type-safe development
- **Tailwind CSS** - Utility-first styling
- **Framer Motion** - Smooth animations
- **Static Export** - Fast loading, deployable anywhere

## Getting Started

### Prerequisites

- Node.js 18+ and npm

### Installation

```bash
# Install dependencies
npm install

# Run development server
npm run dev
```

Open [http://localhost:3000](http://localhost:3000) in your browser.

### Build for Production

```bash
# Create optimized production build
npm run build

# Preview production build locally
npm run start
```

## Deployment

### Vercel (Recommended)

1. Push code to GitHub
2. Import project in [Vercel](https://vercel.com)
3. Vercel auto-detects Next.js and deploys
4. Connect custom domain: glowupai.app

**Auto-deployment:** Every push to `main` branch triggers a new deployment.

### Other Options

**Netlify:**
```bash
npm run build
# Deploy /out directory
```

**GitHub Pages:**
- Set `basePath` in `next.config.js` if deploying to subdirectory
- Push to `gh-pages` branch

**Cloudflare Pages:**
- Build command: `npm run build`
- Output directory: `out`

## Customization

### Colors

Brand colors defined in `tailwind.config.js`:

```js
honey: {
  50: '#FFF8F0',   // Lightest
  400: '#FFB84D',  // Primary
  500: '#FF8C42',  // Primary dark
  // ... more shades
}
```

To change brand colors, edit these values.

### Content

All content is in `app/page.tsx`:

- **Hero tagline:** Line 19
- **Features:** Lines 91-134
- **How it works:** Lines 144-184
- **Testimonials:** Lines 201-219
- **FAQ:** Lines 234-295

### Links

Update these links before launch:

- Play Store URL: Line 32, 310 (`href="https://play.google.com/..."`)
- Social media links: Footer (lines 330-350)
- Privacy policy: `/privacy` (create page or link to existing)
- Terms of service: `/terms` (create page or link to existing)

### Analytics

To add Google Analytics:

1. Create GA4 property
2. Add tracking script to `app/layout.tsx` in `<head>`

```tsx
<Script src="https://www.googletagmanager.com/gtag/js?id=G-XXXXXXXXXX" />
<Script id="google-analytics">
  {`
    window.dataLayer = window.dataLayer || [];
    function gtag(){dataLayer.push(arguments);}
    gtag('js', new Date());
    gtag('config', 'G-XXXXXXXXXX');
  `}
</Script>
```

Or use Vercel Analytics (built-in, privacy-friendly).

## Custom Domain Setup

### With Vercel:

1. Go to project Settings → Domains
2. Add `glowupai.app`
3. Configure DNS:
   - Add A record: `76.76.21.21`
   - Add CNAME: `cname.vercel-dns.com`
4. Wait for DNS propagation (up to 48 hours)

### SSL Certificate:

Vercel provides free SSL automatically via Let's Encrypt.

## Performance

Current lighthouse scores (production build):

- Performance: 95+
- Accessibility: 100
- Best Practices: 100
- SEO: 100

### Optimization tips:

- Images: Use Next.js `<Image>` component (currently using placeholders)
- Fonts: Add Google Fonts or use system fonts (already using system fonts)
- Bundle size: Keep dependencies minimal (only 3 dependencies currently)

## SEO

Metadata configured in `app/layout.tsx`:

- Title: "GlowUp AI - Track Your Skin Progress with Evidence"
- Description: Optimized for "skincare tracker" keyword
- Open Graph tags for social sharing
- Twitter Card tags

### To improve SEO:

1. Add `sitemap.xml`:
   - Create `app/sitemap.ts`
   - Next.js generates automatically

2. Add `robots.txt`:
   - Create `app/robots.ts`

3. Add structured data:
   ```json
   {
     "@type": "MobileApplication",
     "name": "GlowUp AI",
     "operatingSystem": "Android",
     "applicationCategory": "HealthApplication"
   }
   ```

4. Submit to Google Search Console

## iOS Waitlist

Email form at bottom of page (lines 313-329).

Currently logs to console. To connect to real email service:

**Option 1: Google Sheets** (free, simple)
- Use [Google Apps Script](https://developers.google.com/apps-script)
- POST form data to script webhook

**Option 2: Email service** (professional)
- Mailchimp API
- SendGrid API
- ConvertKit

**Option 3: Database** (full control)
- Add API route: `app/api/waitlist/route.ts`
- Save to Postgres/Firebase

Example API route:
```typescript
// app/api/waitlist/route.ts
export async function POST(request: Request) {
  const { email } = await request.json()
  // Save to database or send to email service
  return Response.json({ success: true })
}
```

## File Structure

```
landing/
├── app/
│   ├── layout.tsx       # Root layout with metadata
│   ├── page.tsx         # Main landing page (all sections)
│   └── globals.css      # Tailwind + custom styles
├── components/          # Reusable components (none yet)
├── public/              # Static assets (images, favicon)
├── next.config.js       # Next.js configuration
├── tailwind.config.js   # Tailwind theme customization
├── tsconfig.json        # TypeScript configuration
├── package.json         # Dependencies
└── README.md            # This file
```

## Troubleshooting

**Build fails:**
- Delete `node_modules` and `.next`
- Run `npm install` again
- Check Node.js version (18+ required)

**Styles not applying:**
- Check Tailwind is running: `npm run dev`
- Clear browser cache
- Verify `globals.css` is imported in `layout.tsx`

**Animations not working:**
- Check Framer Motion is installed: `npm list framer-motion`
- Ensure component is using `'use client'` directive

## Mobile Responsiveness

All sections are mobile-responsive:

- Hero: Stacks on mobile, side-by-side on desktop
- Features: Grid collapses to single column
- CTA: Buttons stack on mobile
- Footer: Columns collapse on mobile

Test at these breakpoints:
- Mobile: 375px (iPhone SE)
- Tablet: 768px (iPad)
- Desktop: 1440px

## Accessibility

- All buttons have descriptive text
- Color contrast meets WCAG AA standards
- Semantic HTML (nav, section, article)
- Focus states on interactive elements
- Form labels for email input

To test:
- Use keyboard navigation (Tab key)
- Test with screen reader (VoiceOver on Mac)
- Run Lighthouse accessibility audit

## Future Improvements

- [ ] Add actual phone mockup screenshot
- [ ] Create dedicated `/privacy` and `/terms` pages
- [ ] Add blog section for SEO content
- [ ] Add demo video to hero section
- [ ] Integrate real email service for waitlist
- [ ] Add testimonials with photos (after launch)
- [ ] A/B test different hero copy
- [ ] Add live chat widget (Intercom or similar)
- [ ] Create Spanish translation (es.glowupai.app)

## Support

Questions or issues?
- Email: support@glowupai.app
- Check existing issues in main repo
- Open new issue with `[Landing]` prefix

## License

Proprietary - Part of GlowUp AI project
