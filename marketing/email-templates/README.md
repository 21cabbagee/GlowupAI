# GlowUp AI Email Templates

This directory contains all email templates for user onboarding and engagement.

## Email Sequence Overview

| Email | Trigger | Subject | Goal |
|-------|---------|---------|------|
| **00-welcome** | Account creation | "Welcome to GlowUp AI! Take your first photo 📸" | Onboard new users, explain first steps |
| **03-tips** | Day 3 after signup | "3 tips to get accurate results 💡" | Improve capture quality, build consistency |
| **07-milestone** | 7-day streak achieved | "🔥 One week streak! Here's what's next" | Celebrate milestone, introduce comparison mode |
| **14-engagement** | Day 14 after signup | "How's your skincare journey going?" | Check-in, gather feedback, share success stories |
| **30-milestone** | 30-day streak achieved | "30 days! Time to see your progress 🎉" | Major milestone celebration, upsell premium |
| **reengagement** | 7 days inactive | "We miss you! Your skin journey awaits" | Re-engage dormant users |

## File Structure

Each email has two versions:

- **`.html`** - Styled HTML version for email clients that support HTML
- **`.txt`** - Plain text fallback for email clients that don't support HTML (to be created if needed)

## Design System

### Colors
- **Primary gradient:** `#FFB84D` → `#FF8C42` (warm amber/orange)
- **Background:** `#ffffff` (container), `#f9f9f9` (page)
- **Accent boxes:** `#FFF8F0` (light warm background)
- **Text:** `#333` (primary), `#666` (secondary)

### Typography
- **Font stack:** `-apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif`
- **Headings:** 24-28px, color `#FF8C42`
- **Body text:** 16px, line-height 1.6

### Components
- **CTA Buttons:** Gradient background, white text, 14-16px padding, 8px border-radius
- **Feature boxes:** Light background `#FFF8F0`, left border `#FFB84D`, rounded corners
- **Milestone badges:** Gradient background, white text, centered, large font

## Implementation

### Variables to Replace

All templates include these variables that should be replaced with actual values:

- `{{unsubscribe_url}}` - User-specific unsubscribe link
- `{{days_since_last_capture}}` - For re-engagement email
- Deep links like `glowupai://capture` should be replaced with proper app deep links

### Deep Links

The templates use these deep link schemes (replace with actual implementation):

- `glowupai://capture` - Open capture screen
- `glowupai://insights` - Open insights/progress view
- `glowupai://home` - Open home screen

If your app uses different deep link schemes, find and replace these.

### Email Service Integration

These templates are designed to work with:
- SendGrid
- Mailgun
- AWS SES
- Any HTML email service

**Setup steps:**
1. Create email templates in your email service
2. Replace variables with service-specific merge tags
3. Set up triggers (day-based or event-based)
4. Test on multiple email clients (Gmail, Outlook, Apple Mail, mobile)

### Testing Checklist

Before sending, test each email for:

- [ ] Renders correctly in Gmail (desktop + mobile)
- [ ] Renders correctly in Outlook
- [ ] Renders correctly in Apple Mail
- [ ] All links work (especially deep links to app)
- [ ] Unsubscribe link works
- [ ] Images load (if any added)
- [ ] Text version exists and looks good
- [ ] No broken merge tags ({{variables}})
- [ ] Subject line isn't cut off (under 50 chars)
- [ ] Preview text is compelling

## Metrics to Track

For each email, monitor:

- **Open rate** (aim for >25%)
- **Click-through rate** (aim for >5%)
- **Unsubscribe rate** (keep under 0.5%)
- **App reactivation rate** (for re-engagement email)
- **Premium conversion rate** (for Day 30 email)

## Customization Ideas

### Personalization
Add these variables if you have the data:
- First name
- Current streak count
- Specific metrics that improved
- Products they're tracking
- Skin concern they selected

### A/B Testing Opportunities
- Subject lines (emoji vs no emoji)
- CTA button text
- Email length (short vs detailed)
- Sending time (morning vs evening)
- Tone (casual vs professional)

### Future Emails

Consider adding:
- **Day 60 milestone:** "Two months of tracking - here's what you've learned"
- **Day 90 milestone:** "Quarter 1 complete - your transformation"
- **Product launch:** Announce new features
- **Seasonal:** "Adjust your routine for winter/summer"
- **Educational series:** Weekly skincare tips
- **Survey:** "Help us improve GlowUp AI"

## Brand Voice Guidelines

All emails should:
- Be **encouraging** and **supportive**, not pushy
- Use **data and science** language (but make it accessible)
- Emphasize **privacy and control** (user owns their data)
- Celebrate **small wins** and **consistency** over perfection
- Be **honest** about limitations (not medical, takes time)
- Feel like **a friend helping**, not a company selling

### Tone Examples

✅ Good:
- "You've been tracking for a week - that's huge!"
- "Real skincare results take 4-6 weeks. You're on track."
- "Your photos stay on your device. Your data, your control."

❌ Avoid:
- "Amazing transformation in just 3 days!" (unrealistic)
- "You need to upgrade now!" (too pushy)
- "Our AI will diagnose your skin issues" (medical claims)

## Support & Feedback

All emails should:
- End with "Reply to this email" for feedback
- Include support@glowupai.app contact
- Link to Privacy Policy and Terms
- Have clear unsubscribe option

## Legal Requirements

Ensure compliance with:
- **CAN-SPAM Act** (US) - Unsubscribe link, physical address
- **GDPR** (EU) - Consent, right to unsubscribe, privacy policy link
- **CASL** (Canada) - Express or implied consent

Add your company's physical address to footer if required by law.

## Questions?

Contact: support@glowupai.app
