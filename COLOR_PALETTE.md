# GlowUp AI Color Palette

## Brand Identity

GlowUp AI uses the "Honey" design system: warm, confident, and scientifically grounded. Colors evoke natural light, careful observation, and optimistic progress without superficial beauty-app clichés.

**Core Philosophy:**
- Warm paper, deep ink, restrained honey accent
- Sage and rust for semantic states (not generic red/green)
- No purple-blue AI gradients, no glassmorphism
- Dark theme is a warm charcoal redefinition, never a grey inversion

---

## Primary Colors

### Honey Scale (Brand Yellows)

The honey palette is the brand's signature. Use it for interactive surfaces (buttons, active tabs, focus states) — **NEVER as text or icon color** (except Honey 700).

| Token | Hex | RGB | Use Case |
|-------|-----|-----|----------|
| **Honey 300** | `#FFE29A` | 255, 226, 154 | Tints, chart fills, light accents |
| **Honey 400** | `#FFD166` | 255, 209, 102 | Hover states on dark theme, secondary accent |
| **Honey 500** | `#FFBE2E` | 255, 190, 46 | **PRIMARY** — CTAs, active nav, focus rings |
| **Honey 600** | `#F0A400` | 240, 164, 0 | Press/active state, slightly deeper |
| **Honey 700** | `#B87300` | 184, 115, 0 | "Clay" — the ONLY honey shade allowed as text (light backgrounds only) |

**Contrast Ratios (Light Theme):**
- Ink 900 on Honey 500: **11.35:1** (AAA body text)
- Ink 900 on Honey 300: **14.89:1** (AAA body text)
- Ink 900 on Honey 700: **4.93:1** (AA large text)

**Usage Rules:**
1. Honey 500 is always a *surface* (button background, active tab indicator)
2. Ink 900 (#14110B) is the label/icon on top of any honey surface
3. Never use honey 300-600 as text color on arbitrary backgrounds
4. Honey 700 is permitted as text on light backgrounds only (3.76:1 vs Paper)

---

### Ink (Text & Key Lines)

Primary foreground colors for text, icons, and structural elements.

| Token | Hex | RGB | Use Case |
|-------|-----|-----|----------|
| **Ink 900** | `#14110B` | 20, 17, 11 | **PRIMARY TEXT** — body copy, headlines, icons (light theme) |
| **Ink 600** | `#57503F` | 87, 80, 63 | Secondary text, supporting labels, disabled states (light theme) |

**Contrast Ratios (Light Theme):**
- Ink 900 on Paper (#FFFDF8): **18.53:1** (AAA)
- Ink 900 on Surface (#FFFFFF): **18.84:1** (AAA)
- Ink 600 on Paper: **6.80:1** (AA body text)

---

### Paper & Surface (Backgrounds)

| Token | Hex | RGB | Use Case |
|-------|-----|-----|----------|
| **Paper** | `#FFFDF8` | 255, 253, 248 | **LIGHT THEME BACKGROUND** — main app canvas, warm off-white |
| **Surface** | `#FFFFFF` | 255, 255, 255 | **LIGHT THEME CARDS** — elevated cards, sheets, dialogs |
| **Surface Variant** | `#F3ECDD` | 243, 236, 221 | Subtle differentiation, input backgrounds, inactive tabs |

**Light Theme Surface Hierarchy:**
- App background: Paper (#FFFDF8)
- Elevated cards: Surface (#FFFFFF)
- Subtle surfaces: Surface Variant (#F3ECDD)

---

## Dark Theme Colors

Dark theme uses warm charcoal tones, not cold greys. It's a redefinition of the brand, not an inversion.

### Dark Backgrounds

| Token | Hex | RGB | Use Case |
|-------|-----|-----|----------|
| **Charcoal 900** | `#0F0D0A` | 15, 13, 10 | **DARK THEME BACKGROUND** — main app canvas |
| **Charcoal 800** | `#1E1911` | 30, 25, 17 | **DARK THEME CARDS** — elevated cards, sheets |
| **Charcoal 700** | `#2A241A` | 42, 36, 26 | Surface variant, subtle backgrounds |

### Dark Foregrounds

| Token | Hex | RGB | Use Case |
|-------|-----|-----|----------|
| **Warm White** | `#F7F1E4` | 247, 241, 228 | **DARK THEME PRIMARY TEXT** — body copy, headlines |
| **Warm Grey** | `#C9BFA9` | 201, 191, 169 | **DARK THEME SECONDARY TEXT** — supporting labels, captions |

**Contrast Ratios (Dark Theme):**
- Warm White on Charcoal 900: **17.24:1** (AAA)
- Warm White on Charcoal 800: **15.52:1** (AAA)
- Warm Grey on Charcoal 900: **8.43:1** (AAA)

**Dark Theme Honey Usage:**
- Honey 500 surfaces remain the same (#FFBE2E) with Ink 900 (#14110B) labels
- Honey 400 for hover states on dark backgrounds
- Brand chips (verdicts) keep identical colors for consistency

---

## Semantic Colors

Non-honey colors for specific meaning. Chosen to complement honey without clashing.

### Success (Sage)

| Token | Hex | RGB | Use Case |
|-------|-----|-----|----------|
| **Sage** | `#3F7D5C` | 63, 125, 92 | Success states, "likely_useful" verdicts, improvement indicators |
| **On Sage (Light)** | `#FFFDF8` (Paper) | 255, 253, 248 | Text on sage background (4.80:1) |
| **On Sage (Dark)** | `#FFFFFF` (White) | 255, 255, 255 | Text on sage background dark theme (4.88:1) |

**Usage:** Progress indicators, positive trends, keep/useful verdicts

### Warning (Honey 600)

| Token | Hex | RGB | Use Case |
|-------|-----|-----|----------|
| **Warning** | `#F0A400` (Honey 600) | 240, 164, 0 | Caution, evidence unclear, review needed |
| **On Warning** | `#14110B` (Ink 900) | 20, 17, 11 | Text on warning background (8.98:1) |

**Usage:** Capture quality issues, unclear evidence verdicts, non-critical alerts

### Danger (Rust)

| Token | Hex | RGB | Use Case |
|-------|-----|-----|----------|
| **Rust** | `#C2453F` | 194, 69, 63 | Error states, "investigate" verdicts, negative trends |
| **On Rust** | `#FFFDF8` (Paper) | 255, 253, 248 | Text on rust background (4.90:1) |

**Usage:** Errors, product investigate labels, significant negative changes

### Locked/Neutral (Ink 600)

| Token | Hex | RGB | Use Case |
|-------|-----|-----|----------|
| **Neutral** | `#57503F` (Ink 600) | 87, 80, 63 | Locked features, insufficient data, neutral state |
| **On Neutral** | `#FFFDF8` (Paper) | 255, 253, 248 | Text on neutral background (7.87:1) |

**Usage:** Premium-locked verdicts, insufficient data states, disabled features

---

## Verdict Chip Colors

Backend verdict labels map to specific brand colors. Each chip is self-contained (carries its own foreground color).

| Verdict Label | Background | Foreground | Light Contrast | Dark Contrast |
|---------------|------------|------------|----------------|---------------|
| `keep` | Honey 500 (#FFBE2E) | Ink 900 (#14110B) | 11.35:1 | 11.35:1 |
| `likely_useful` | Sage (#3F7D5C) | Paper (Light) / White (Dark) | 4.80:1 | 4.88:1 |
| `evidence_unclear` | Honey 700 (#B87300) | Ink 900 (#14110B) | 4.93:1 | 4.93:1 |
| `investigate` | Rust (#C2453F) | Paper (#FFFDF8) | 4.90:1 | 4.90:1 |
| `locked` | Ink 600 (#57503F) | Paper (#FFFDF8) | 7.87:1 | 7.87:1 |

**Implementation Note:** Verdict chips keep identical colors in both light and dark themes for consistent brand recognition.

---

## Chart & Data Visualization Colors

### Light Theme Charts

| Token | Value | Use Case |
|-------|-------|----------|
| **Chart Grid** | Ink 600 @ 15% opacity | Decorative gridlines (non-text) |
| **Chart Line** | Honey 700 (#B87300) | Trend lines, data series (3.76:1 vs Paper) |
| **Chart Fill** | Honey 300 @ 30% opacity | Area fills, bar fills |
| **Shimmer** | `#ECE3CF` | Loading skeleton backgrounds |

### Dark Theme Charts

| Token | Value | Use Case |
|-------|-------|----------|
| **Chart Grid** | Warm White @ 12% opacity | Decorative gridlines (non-text) |
| **Chart Line** | Honey 400 (#FFD166) | Trend lines, data series (13.46:1 vs Charcoal 900) |
| **Chart Fill** | Honey 400 @ 20% opacity | Area fills, bar fills |
| **Shimmer** | `#2A241A` | Loading skeleton backgrounds |

**Multi-Series Charts:**
When showing multiple data series, use this priority:
1. Primary series: Chart Line (Honey 700 or 400)
2. Secondary series: Sage (#3F7D5C)
3. Tertiary series: Rust (#C2453F)
4. Additional: Ink 600 with 50% opacity

---

## Elevation & Shadows

GlowUp uses subtle elevation, not heavy drop shadows.

### Light Theme Elevation

| Level | Elevation | Shadow |
|-------|-----------|--------|
| **Level 0** | 0dp | None (Paper background) |
| **Level 1** | 2dp | `rgba(20, 17, 11, 0.08)` offset 0px 2px, blur 4px |
| **Level 2** | 4dp | `rgba(20, 17, 11, 0.12)` offset 0px 4px, blur 8px |
| **Level 3** | 8dp | `rgba(20, 17, 11, 0.16)` offset 0px 8px, blur 16px |

**Usage:**
- Cards: Level 1
- Floating action buttons: Level 2
- Dialogs, bottom sheets: Level 3

### Dark Theme Elevation

In dark theme, elevation is expressed through lightness, not shadows.

| Level | Surface Color |
|-------|---------------|
| **Level 0** | Charcoal 900 (#0F0D0A) |
| **Level 1** | Charcoal 800 (#1E1911) |
| **Level 2** | Charcoal 700 (#2A241A) |
| **Level 3** | `#352E22` (Charcoal 700 + 10% white overlay) |

---

## Outline & Divider Colors

### Light Theme

| Token | Value | Use Case |
|-------|-------|----------|
| **Outline** | `#8A8065` | Interactive borders, focus rings (3.86:1 vs Paper) |
| **Outline Variant** | `#E4DCC8` | Decorative dividers, inactive borders (non-text) |

### Dark Theme

| Token | Value | Use Case |
|-------|-------|----------|
| **Outline** | `#9A8F73` | Interactive borders, focus rings (6.05:1 vs Charcoal 900) |
| **Outline Variant** | `#3A362B` | Decorative dividers, inactive borders (non-text) |

---

## Accessibility & Contrast Compliance

All text/background pairs meet **WCAG 2.1 AA standards minimum**:
- Body text: 4.5:1 or higher
- Large text (18pt+): 3:1 or higher
- Non-text UI elements: 3:1 or higher

Most GlowUp pairs exceed AA and reach **AAA standards (7:1+)**.

### Testing Your Combinations

Before using a new color combination:
1. Use WebAIM Contrast Checker: https://webaim.org/resources/contrastchecker/
2. Measure relative luminance: `(L_light + 0.05) / (L_dark + 0.05)`
3. Ensure ratio meets minimum:
   - Body text: ≥4.5:1
   - Large/bold text: ≥3:1
   - Interactive UI: ≥3:1

**Pre-Approved High-Contrast Pairs:**
- Ink 900 on Paper: 18.53:1
- Ink 900 on Honey 500: 11.35:1
- Warm White on Charcoal 900: 17.24:1
- Sage with Paper/White text: ~4.8:1
- Rust with Paper text: 4.90:1

---

## Color Usage Guidelines

### DO

✅ Use Honey 500 for primary CTAs and active states  
✅ Use Ink 900 as foreground on all honey surfaces  
✅ Use Paper as light theme background (not pure white)  
✅ Use Charcoal 900 (not grey #333) for dark theme  
✅ Use Sage/Rust for semantic meaning, not just decoration  
✅ Test all custom combinations for contrast compliance  
✅ Maintain warm tone across all surfaces  

### DON'T

❌ Never use Honey 300-600 as text or icon color  
❌ Never use generic red (#FF0000) or green (#00FF00)  
❌ Never invert light theme to create dark theme (use defined dark colors)  
❌ Never use purple-blue "AI gradients"  
❌ Never use pure black (#000000) for text or backgrounds  
❌ Never use Material You dynamic colors (brand must stay consistent)  
❌ Never use more than 3 brand colors in a single screen  

---

## Color Tokens in Code

### Android (Compose)

```kotlin
// Access via LocalGlowColors.current
val glowColors = LocalGlowColors.current

// Usage examples:
Button(
    colors = ButtonDefaults.buttonColors(
        containerColor = glowColors.honey500,
        contentColor = glowColors.ink900
    )
)

Text(
    text = "Body text",
    color = glowColors.ink900
)

Surface(
    color = glowColors.paper
) {
    // Content
}
```

### Android (XML)

```xml
<color name="honey_500">@color/honey_500</color>
<color name="ink_900">@color/ink_900</color>
<color name="paper">@color/paper</color>
```

Defined in: `app/src/main/res/values/colors.xml`

### Web (CSS Custom Properties)

```css
:root {
  --honey-300: #FFE29A;
  --honey-400: #FFD166;
  --honey-500: #FFBE2E;
  --honey-600: #F0A400;
  --honey-700: #B87300;
  --ink-900: #14110B;
  --ink-600: #57503F;
  --paper: #FFFDF8;
  --sage: #3F7D5C;
  --rust: #C2453F;
}

@media (prefers-color-scheme: dark) {
  :root {
    --paper: #0F0D0A;
    --surface: #1E1911;
    --ink-900: #F7F1E4;
    --ink-600: #C9BFA9;
  }
}
```

---

## Color Palette Export Formats

### Design Tools

**Figma:** Import `design/glowup-colors.json`  
**Sketch:** Use `design/glowup-colors.sketchpalette`  
**Adobe XD:** Import `design/glowup-colors.ase`  

### Developer Handoff

**Android:**
- `app/src/main/res/values/colors.xml` (light theme)
- `app/src/main/res/values-night/colors.xml` (dark theme fallback)
- `app/src/main/java/com/glowup/ai/core/design/Tokens.kt` (Compose source of truth)

**iOS:**
- `Assets.xcassets/Colors/` folder with `.colorset` definitions
- Supports light/dark variants automatically

**Web/PWA:**
- `styles/tokens.css` (CSS custom properties)
- `theme.config.js` (Tailwind/styled-components config)

---

## Questions?

For color usage questions or contrast verification: support@glowup.ai

**See also:**
- `APP_ICON_SPEC.md` — Icon color usage
- `TYPOGRAPHY.md` — Text color pairings
- `COMPONENT_GUIDELINES.md` — Component-specific color usage
