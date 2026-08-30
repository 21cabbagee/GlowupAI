# GlowUp AI Typography System

## Overview

GlowUp AI uses a refined typographic system built on system fonts for optimal readability and performance. The type scale balances scientific clarity with approachable warmth.

**Philosophy:**
- System fonts only (no webfonts, no custom fonts)
- Two weights: ExtraBold (800) for display, Regular (400) / SemiBold (600) for everything else
- Generous line height (1.55x) for body text readability
- Tight tracking (-0.04em) for display text impact

---

## Font Stack

### System Font Strategy

GlowUp uses the platform's native system font stack, which provides:
- **Android Stock:** Roboto
- **Samsung/OEM Android:** OEM system font (e.g., Samsung One UI font)
- **iOS:** San Francisco
- **Web:** System default (Segoe UI on Windows, SF Pro on macOS, Roboto on Android)

**Why system fonts?**
1. Zero download time (instant rendering)
2. Native platform feel
3. Optimal screen rendering
4. Better accessibility support
5. Consistent with platform conventions

### Font Weights Used

| Weight | Value | Usage |
|--------|-------|-------|
| **ExtraBold** | 800 | Display headlines only (Large, Medium, Small) |
| **SemiBold** | 600 | Headlines, titles, labels, emphasis |
| **Regular** | 400 | Body text, captions, most UI text |

**NOT USED:**
- Thin (100), Light (200), Book (300): Too fragile for data-heavy UI
- Bold (700): Unnecessary with SemiBold and ExtraBold
- Black (900): Too heavy, not needed

---

## Type Scale

The type scale follows Material 3 conventions with GlowUp-specific tuning.

### Display (ExtraBold, Tight Tracking)

Use for marketing moments, empty states, and impactful headlines. Never for regular screens.

| Token | Size | Line Height | Letter Spacing | Use Case |
|-------|------|-------------|----------------|----------|
| **Display Large** | 57sp | 58sp (1.02x) | -0.04em | Splash screen, major empty states |
| **Display Medium** | 45sp | 46sp (1.02x) | -0.04em | Onboarding hero headlines |
| **Display Small** | 36sp | 37sp (1.02x) | -0.04em | Section hero headlines |

**Color Pairing:**
- Light theme: Ink 900 (#14110B)
- Dark theme: Warm White (#F7F1E4)
- Never use display text in color (always use primary text color)

**Examples:**
- "Know your skin over time." (Welcome screen)
- "28 day streak 🔥" (Achievement callout)
- Empty state headlines

---

### Headline (SemiBold, Slightly Tight)

Primary headlines for screen titles, card headers, dialog titles.

| Token | Size | Line Height | Letter Spacing | Use Case |
|-------|------|-------------|----------------|----------|
| **Headline Large** | 32sp | 38sp (1.2x) | -0.02em | Screen titles (rare, desktop only) |
| **Headline Medium** | 28sp | 34sp (1.2x) | -0.02em | Major section headers |
| **Headline Small** | 24sp | 29sp (1.2x) | -0.02em | Screen titles, card headers |

**Color Pairing:**
- Primary: Ink 900 (light) / Warm White (dark)
- Secondary: Ink 600 (light) / Warm Grey (dark) — use sparingly

**Examples:**
- "What should we call you?" (Profile setup)
- "Your baseline is saved, Alex." (Result screen)
- "Routine" (Tab header)

---

### Title (SemiBold, Neutral Tracking)

Card titles, list item headers, button labels, tab labels.

| Token | Size | Line Height | Letter Spacing | Use Case |
|-------|------|-------------|----------------|----------|
| **Title Large** | 22sp | 28sp (1.25x) | 0em | Large card headers, bottom sheet titles |
| **Title Medium** | 16sp | 20sp (1.25x) | 0em | List item titles, input labels |
| **Title Small** | 14sp | 18sp (1.25x) | 0em | Dense list titles, small card headers |

**Color Pairing:**
- Primary: Ink 900 (light) / Warm White (dark)
- On Color Surfaces: Use appropriate `onXxx` color (e.g., Ink 900 on Honey 500)

**Examples:**
- "Vitamin C Serum Test" (Experiment card title)
- "Your next best step" (Home feed card header)
- "Morning Routine" (Section title)

---

### Body (Regular, Generous Line Height)

The workhorse of the app. Optimized for readability with 1.55x line height.

| Token | Size | Line Height | Letter Spacing | Use Case |
|-------|------|-------------|----------------|----------|
| **Body Large** | 16sp | 25sp (1.55x) | 0.02em | Primary reading text, descriptions, article content |
| **Body Medium** | 14sp | 22sp (1.55x) | 0.02em | Default body text, list supporting text |
| **Body Small** | 12sp | 19sp (1.55x) | 0.02em | Captions, footnotes, dense data tables |

**Color Pairing:**
- Primary: Ink 900 (light) / Warm White (dark)
- Secondary: Ink 600 (light) / Warm Grey (dark)
- Tertiary/Disabled: Ink 600 @ 60% opacity

**Examples:**
- "A private record of what changes, what stays steady..." (Onboarding copy)
- Product descriptions
- Help text under input fields
- "Started 23 days ago" (Metadata)

**Paragraph Spacing:**
- Between paragraphs: 16dp (1 × GlowSpacing.md)
- Within multi-line body text: Use line-height only (no extra spacing)

---

### Label (SemiBold, Compact)

Buttons, chips, badges, small UI controls. Designed for scannability.

| Token | Size | Line Height | Letter Spacing | Use Case |
|-------|------|-------------|----------------|----------|
| **Label Large** | 14sp | 19sp (1.35x) | 0.02em | Button text, primary action labels |
| **Label Medium** | 12sp | 16sp (1.35x) | 0.02em | Chip labels, small button text, tab labels |
| **Label Small** | 11sp | 15sp (1.35x) | 0.02em | Badges, tiny chips, metadata badges |

**Color Pairing:**
- On interactive surfaces: Use surface's `onXxx` color
- On neutral background: Ink 900 (light) / Warm White (dark)

**Examples:**
- "Continue" (Button label)
- "Premium" (Badge)
- "Day 9 of 14" (Status chip)
- "Add product" (Small button)

**Uppercase Labels:**
- Use sparingly (only for eyebrows/overlines)
- Use Label Medium with all-caps: `textTransform: uppercase` in code
- Example: "YOUR STARTING LINE" (Eyebrow above headline)

---

## Type Pairing Guidelines

### Screen Hierarchy (Typical)

```
Display Small (36sp, ExtraBold)
↓ 32dp space
Body Large (16sp, Regular)
↓ 24dp space
Headline Small (24sp, SemiBold)
↓ 8dp space
Body Medium (14sp, Regular)
↓ 16dp space
Label Large Button (14sp, SemiBold)
```

### Card Hierarchy (Typical)

```
Title Large (22sp, SemiBold) — Card header
↓ 4dp space
Body Medium (14sp, Regular) — Card body
↓ 8dp space
Label Medium Chip (12sp, SemiBold) — Metadata badge
```

### List Item Hierarchy

```
Title Medium (16sp, SemiBold) — Item title
↓ 2dp space
Body Small (12sp, Regular, Ink 600) — Supporting text
```

---

## Responsive Typography

Typography scales slightly at different screen sizes.

### Mobile (< 600dp width)
- Use the base scale as defined above
- Display Large: Use sparingly (full-screen empty states only)
- Headline Large: Skip (too large for small screens)

### Tablet (600-1240dp width)
- Use base scale
- Display Large: Safe to use for major moments
- Headline Large: Safe for screen titles

### Desktop (> 1240dp width)
- Optional: Scale up Display and Headline by 1.1x
- Display Large: 63sp (57sp × 1.1)
- Display Medium: 50sp (45sp × 1.1)
- Headline Large: 35sp (32sp × 1.1)
- Body and Label: Keep at base size (don't scale up)

**Implementation Note:** Use `LocalConfiguration.current.screenWidthDp` in Compose or CSS media queries for web.

---

## Accessibility Considerations

### Minimum Font Sizes

Never use text smaller than:
- **Body Small (12sp)** for essential reading text
- **Label Small (11sp)** for non-essential metadata
- **10sp** is the absolute floor (use only for legal/copyright footnotes)

### Dynamic Type / Font Scaling

Support user font size preferences:
- **Android:** Respect system font scale (automatically handled by `sp` units)
- **iOS:** Respect Dynamic Type (use UIFontMetrics)
- **Web:** Use relative units (`rem`, `em`) not `px`

**Testing:** Test layouts at 1x, 1.3x, and 1.5x font scale.

### Color Contrast

All type pairings must meet WCAG 2.1 standards:
- Body text (< 18pt): **4.5:1 minimum**
- Large text (≥ 18pt or bold ≥ 14pt): **3:1 minimum**

**Pre-approved pairings:**
- Ink 900 on Paper: 18.53:1 ✅
- Ink 600 on Paper: 6.80:1 ✅
- Warm White on Charcoal 900: 17.24:1 ✅
- Honey 700 on Paper: 3.76:1 ✅ (large text only)

### Line Length

Optimal readability: 50-75 characters per line (CPL)
- Mobile: 40-60 CPL (naturally constrained)
- Tablet: 55-75 CPL
- Desktop: 60-75 CPL (use `maxWidth` constraints)

**Implementation:**
```kotlin
// Compose
Text(
    text = longBodyText,
    modifier = Modifier.widthIn(max = 640.dp) // Constrains line length
)
```

---

## Text Emphasis Hierarchy

Use color and weight to create hierarchy, not just size.

| Emphasis Level | Weight | Color | Use Case |
|----------------|--------|-------|----------|
| **High Emphasis** | SemiBold (600) | Ink 900 / Warm White | Headlines, primary actions, key data |
| **Medium Emphasis** | Regular (400) | Ink 900 / Warm White | Body text, default UI text |
| **Low Emphasis** | Regular (400) | Ink 600 / Warm Grey | Supporting text, metadata, captions |
| **Disabled** | Regular (400) | Ink 600 @ 38% opacity | Disabled controls, inactive text |

**Examples:**

```kotlin
// High emphasis (headline)
Text(
    text = "Your baseline is saved",
    style = MaterialTheme.typography.headlineSmall,
    color = LocalGlowColors.current.ink900
)

// Medium emphasis (body)
Text(
    text = "This is a reference point...",
    style = MaterialTheme.typography.bodyLarge,
    color = LocalGlowColors.current.ink900
)

// Low emphasis (caption)
Text(
    text = "Started 23 days ago",
    style = MaterialTheme.typography.bodySmall,
    color = LocalGlowColors.current.ink600
)
```

---

## Special Text Treatments

### Eyebrows / Overlines

Small, uppercase labels above headlines.

**Style:**
- Label Medium (12sp, SemiBold)
- All caps (`textTransform: uppercase`)
- Color: Ink 600 (light) / Warm Grey (dark)
- Letter spacing: 0.1em (wider than default)

**Example:**
```
YOUR STARTING LINE
Your baseline is saved, Alex.
```

### Number Display

Large numbers (metrics, streak counts, data values).

**Style:**
- Display Medium or Headline Large
- Tabular numbers (`fontFeatureSettings: "tnum"`) if available
- Color: Ink 900 (light) / Warm White (dark)
- Optional unit in Body Small, Ink 600

**Example:**
```
28 (Display Medium)
day streak (Body Small, Ink 600)
```

### Inline Links

**Style:**
- Same size as surrounding text
- Color: Honey 700 (light) / Honey 400 (dark)
- Underline on hover/focus
- SemiBold weight for emphasis

**Example:**
```kotlin
Text(
    text = "Read the full privacy policy",
    style = MaterialTheme.typography.bodyMedium,
    color = LocalGlowColors.current.honey700,
    textDecoration = TextDecoration.Underline
)
```

### Code / Monospace

For technical content (rare in this app).

**Style:**
- FontFamily.Monospace
- Body Medium size (14sp)
- Background: Surface Variant
- Padding: 4dp horizontal, 2dp vertical

**Example:**
```
user_id: abc123def
```

---

## Typography Usage Guidelines

### DO

✅ Use system fonts for all text (Roboto on Android, SF on iOS)  
✅ Use ExtraBold (800) only for Display styles  
✅ Use SemiBold (600) for emphasis and headers  
✅ Use Regular (400) for body text and most UI  
✅ Maintain 1.55x line-height for body text readability  
✅ Use generous spacing between paragraphs (16dp+)  
✅ Test at 1.3x and 1.5x font scale  
✅ Support user font size preferences  
✅ Constrain line length on wide screens (max 640dp)  

### DON'T

❌ Never use custom fonts or webfonts  
❌ Never use more than 3 font weights in the app  
❌ Never use Light (300) or Thin (100) weights (too fragile)  
❌ Never use colored text for body copy (only headlines/accents)  
❌ Never use text smaller than 11sp  
❌ Never set line-height < 1.2x (readability suffers)  
❌ Never use all-caps for body text (only eyebrows/labels)  
❌ Never use italic for emphasis (use SemiBold weight instead)  
❌ Never force a specific font (respect system preferences)  

---

## Typography in Code

### Android (Jetpack Compose)

```kotlin
// Access Material 3 typography
val typography = MaterialTheme.typography

// Usage examples
Text(
    text = "Screen Title",
    style = typography.headlineSmall
)

Text(
    text = "Body copy with good readability",
    style = typography.bodyLarge,
    color = LocalGlowColors.current.ink900
)

Button(onClick = { /* ... */ }) {
    Text(
        text = "Continue",
        style = typography.labelLarge
    )
}
```

Defined in: `app/src/main/java/com/glowup/ai/core/design/Type.kt`

### Android (XML — Legacy Views)

```xml
<TextView
    android:text="Headline"
    android:textAppearance="?attr/textAppearanceHeadlineSmall"
    android:textColor="@color/ink_900" />

<TextView
    android:text="Body text"
    android:textAppearance="?attr/textAppearanceBodyLarge"
    android:textColor="@color/ink_900" />
```

### Web (CSS)

```css
/* Display */
.display-large {
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
  font-weight: 800;
  font-size: 3.5625rem; /* 57px */
  line-height: 1.02;
  letter-spacing: -0.04em;
}

/* Headline */
.headline-small {
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
  font-weight: 600;
  font-size: 1.5rem; /* 24px */
  line-height: 1.2;
  letter-spacing: -0.02em;
}

/* Body */
.body-large {
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
  font-weight: 400;
  font-size: 1rem; /* 16px */
  line-height: 1.55;
  letter-spacing: 0.02em;
}
```

---

## Type Scale Quick Reference

| Token | Size | Weight | Line Height | Use Case |
|-------|------|--------|-------------|----------|
| Display Large | 57sp | 800 | 1.02x | Splash, major empty states |
| Display Medium | 45sp | 800 | 1.02x | Onboarding heroes |
| Display Small | 36sp | 800 | 1.02x | Section heroes |
| Headline Large | 32sp | 600 | 1.2x | Screen titles (desktop) |
| Headline Medium | 28sp | 600 | 1.2x | Major section headers |
| Headline Small | 24sp | 600 | 1.2x | Screen titles, card headers |
| Title Large | 22sp | 600 | 1.25x | Large card headers |
| Title Medium | 16sp | 600 | 1.25x | List item titles |
| Title Small | 14sp | 600 | 1.25x | Dense list titles |
| Body Large | 16sp | 400 | 1.55x | Primary reading text |
| Body Medium | 14sp | 400 | 1.55x | Default body text |
| Body Small | 12sp | 400 | 1.55x | Captions, footnotes |
| Label Large | 14sp | 600 | 1.35x | Button text |
| Label Medium | 12sp | 600 | 1.35x | Chip labels, tabs |
| Label Small | 11sp | 600 | 1.35x | Badges, tiny chips |

---

## Questions?

For typography questions or implementation help: support@glowup.ai

**See also:**
- `COLOR_PALETTE.md` — Text color pairings
- `COMPONENT_GUIDELINES.md` — Typography in specific components
- `app/src/main/java/com/glowup/ai/core/design/Type.kt` — Compose type definitions
