# GlowUp AI App Icon Design Specification

## Overview

The GlowUp AI app icon represents the intersection of personal skincare tracking and data-driven insights. It should feel scientific but approachable, premium but not pretentious, and optimistic about the user's "glow up" journey.

## Design Concept

**Primary Concept: "The Mirror + Data"**

A stylized face or measurement frame combined with subtle data visualization elements, symbolizing the core value proposition: tracking YOUR skin's journey with objective data.

## Visual Elements

### Main Icon Structure

**Option A: Profile with Progress Line**
- Stylized face silhouette in profile view (facing right, suggesting forward movement)
- Clean, minimalist outline with subtle facial features
- Upward-trending line graph overlaid across the face profile
- Small sparkle/glow element in top right corner

**Option B: Front View with Data Points**
- Simple face outline (front view, centered)
- Three small circular data points positioned along cheekbone
- Suggests measurement and tracking without being clinical
- More abstract, less literal than Option A

**Recommended:** Option B for better scalability and modern aesthetic

### Color Scheme

**Primary Colors:**
- Background: Warm gradient from Honey 400 (#FFD166) to Honey 500 (#FFBE2E)
- Face outline: Ink 900 (#14110B) - deep, warm dark
- Accent/sparkle: Honey 300 (#FFE29A) or white for contrast

**Alternative (Dark Mode Variant):**
- Background: Honey 500 (#FFBE2E) solid
- Face outline: Charcoal 900 (#0F0D0A)
- Works as monochrome on iOS-style dark mode home screens

### Shape & Format

**Container:**
- Rounded square with generous corner radius (20-25% of height)
- Modern, friendly, approachable
- Follows Android adaptive icon guidelines
- iOS: Standard rounded square (automatic)

**Dimensions:**
- Master artwork: 1024x1024px (required for both stores)
- Android adaptive icon layers: 432x432px safe zone within 512x512px canvas
- Notification icon: Silhouette only, white-on-transparent

## Style Guidelines

### DO
- Keep it clean and minimalist
- Use strong, recognizable silhouette
- Ensure readability at 48x48px (smallest common size)
- Balance between science (data) and self-care (face)
- Make it memorable and distinctive in the app drawer
- Use the honey color as a confident, warm statement
- Consider how it looks on both light and dark backgrounds

### DON'T
- Make it look medical/clinical (no stethoscopes, no diagnostic symbols)
- Use generic beauty tropes (no makeup brushes, no mirrors with sparkles)
- Include realistic face photos or detailed facial features
- Use purple-blue "AI gradients" or trendy glassmorphism
- Make it too complex (must work at tiny sizes)
- Use more than 3 colors in the main icon
- Include text or wordmarks in the icon itself

## Technical Specifications

### Android Adaptive Icon (API 26+)

**Foreground Layer (512x512px)**
- Face outline and data points
- All important elements within 432x432px safe zone (66px margin on all sides)
- Export as vector drawable (XML) or high-res PNG

**Background Layer (512x512px)**
- Solid color: Honey 500 (#FFBE2E) OR
- Simple gradient: Honey 400 → Honey 500 (vertical)
- Must be full bleed (512x512px, no safe zone margin)

**Format:** `ic_launcher_foreground.xml` + `ic_launcher_background.xml`

### Android Legacy Icon (Pre-API 26)

**Round Icon (512x512px)**
- Complete icon composition in circular frame
- All elements visible within circle
- Export: `ic_launcher_round.png`

**Square Icon (512x512px)**
- Complete icon with rounded corners
- Export: `ic_launcher.png`

### iOS App Icon (1024x1024px)

- Complete composition
- No transparency (solid background required)
- iOS automatically applies corner rounding
- Export as PNG, sRGB color space

### Notification Icon (Android)

**Small Icon (24x24dp = 96x96px @ xxhdpi)**
- Pure white silhouette on transparent background
- Simple face outline only (no data points, no sparkle)
- Must be clear at tiny sizes
- Export as vector drawable: `ic_notification.xml`

### Favicon / Web App Icon

**Sizes needed:**
- 512x512px (PWA manifest)
- 192x192px (PWA manifest)
- 180x180px (iOS web clip)
- 32x32px (browser favicon)
- 16x16px (browser favicon)

## Accessibility Considerations

1. **Contrast Ratio:** Honey 500 background with Ink 900 foreground = 11.35:1 (excellent contrast)
2. **Scalability:** Test at 48x48px, 96x96px, and 192x192px to ensure clarity
3. **Color Blindness:** Design works in grayscale (relies on shape, not just color)
4. **Dark Mode:** Icon should be visible on both light and dark home screens

## Icon Family Consistency

When creating app variations (if needed for different markets or test builds):
- Maintain the core face + data concept
- Keep the honey color palette
- Only vary small details (badge, corner accent) never the whole icon

## Mood & References

**Feels Like:**
- Health app clarity (Apple Health, Google Fit)
- Wellness app warmth (Calm, Headspace)
- Personal tracking confidence (Streaks, Habit)

**NOT Like:**
- Medical/diagnostic apps (SkinVision, DermExpert)
- Beauty/photo editing apps (Perfect365, FaceApp)
- Generic social/dating apps (Instagram, Tinder)

## Design Deliverables

### For Developer Handoff:

1. **Master Artwork**
   - 1024x1024px PNG (sRGB, no transparency)
   - Source file (Figma/Sketch/Illustrator)

2. **Android Resources**
   - `res/mipmap-xxxhdpi/ic_launcher.png` (192x192px)
   - `res/mipmap-xxxhdpi/ic_launcher_round.png` (192x192px)
   - `res/mipmap-xxhdpi/ic_launcher.png` (144x144px)
   - `res/mipmap-xxhdpi/ic_launcher_round.png` (144x144px)
   - `res/mipmap-xhdpi/ic_launcher.png` (96x96px)
   - `res/mipmap-xhdpi/ic_launcher_round.png` (96x96px)
   - `res/mipmap-hdpi/ic_launcher.png` (72x72px)
   - `res/mipmap-hdpi/ic_launcher_round.png` (72x72px)
   - `res/mipmap-mdpi/ic_launcher.png` (48x48px)
   - `res/mipmap-mdpi/ic_launcher_round.png` (48x48px)
   - `res/mipmap-anydpi-v26/ic_launcher.xml` (adaptive icon definition)
   - `res/drawable/ic_launcher_foreground.xml` (foreground layer)
   - `res/drawable/ic_launcher_background.xml` (background layer)
   - `res/drawable/ic_notification.xml` (notification icon)

3. **iOS Resources**
   - `AppIcon.appiconset/` folder with all required sizes
   - 1024x1024px for App Store

4. **Web Resources**
   - 512x512px PNG (PWA manifest)
   - 192x192px PNG (PWA manifest)
   - 180x180px PNG (Apple web clip)
   - favicon.ico (multi-size: 16px, 32px, 48px)

## Testing Checklist

- [ ] Looks clear at 48x48px on white background
- [ ] Looks clear at 48x48px on black background
- [ ] Looks clear at 48x48px on honey-colored background (self-test)
- [ ] Silhouette is recognizable without color
- [ ] No small details lost at tiny sizes
- [ ] Distinguishable from competitors in app store screenshots
- [ ] Adaptive icon works with all system shapes (circle, squircle, rounded square, teardrop)
- [ ] Notification icon is clearly identifiable in notification tray
- [ ] Icon family is consistent across all deliverables

## Notes for Designer

- The honey color (#FFBE2E) is non-negotiable brand anchor
- Dark ink (#14110B) provides excellent contrast
- Icon must work independently (no surrounding context)
- Consider animation potential for splash screen (face fading in + data line drawing)
- Keep source files organized for future updates
- Export all assets with proper naming conventions (see ICON_GENERATION.md)

---

**Questions or feedback?** support@glowup.ai

**Ready to start?** Review COLOR_PALETTE.md and COMPONENT_GUIDELINES.md for full brand context.
