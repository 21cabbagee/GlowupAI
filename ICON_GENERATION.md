# Android Icon Generation Guide

## Overview

This guide covers generating all required icon assets for GlowUp AI on Android. Follow these instructions to ensure proper icon display across all device types, screen densities, and Android versions.

---

## Required Icon Types

Android requires three main icon types:

1. **App Launcher Icons** — The main app icon users see in the app drawer and home screen
2. **Adaptive Icons** (Android 8.0+) — Separate foreground and background layers that adapt to different device shapes
3. **Notification Icons** — Small monochrome icons shown in the notification tray
4. **Web Icons** (Optional) — For PWA/web app manifest

---

## Screen Density Qualifiers

Android uses density-independent pixels (dp) that scale to different screen densities:

| Qualifier | Scale | Example Size (48dp icon) | Use Case |
|-----------|-------|-------------------------|----------|
| **mdpi** | 1.0x | 48px | Low-density screens (rare today) |
| **hdpi** | 1.5x | 72px | Medium-density screens (older devices) |
| **xhdpi** | 2.0x | 96px | High-density screens (common) |
| **xxhdpi** | 3.0x | 144px | Extra-high density (most modern phones) |
| **xxxhdpi** | 4.0x | 192px | Extra-extra-high density (premium devices) |

**Best Practice:** Always design at the highest density (xxxhdpi / 192px for 48dp) and scale down.

---

## 1. Adaptive Icons (Android 8.0+ / API 26+)

Adaptive icons consist of two layers that the system combines and masks into various shapes (circle, squircle, rounded square, teardrop) based on device OEM.

### Foreground Layer

**Purpose:** Contains the main icon design (face + data points)

**Canvas Size:** 512x512px  
**Safe Zone:** 432x432px (66px margin on all sides)  
**Format:** Vector drawable (XML) preferred, or PNG

**Design Rules:**
- All critical elements MUST fit within the 432x432px safe zone
- Outer 66px on each side MAY be cropped depending on device mask
- Center 264x264px (132px from each edge) is guaranteed visible on all masks
- Design should work when cropped to circle (worst case)

**File Location:**
```
app/src/main/res/drawable/ic_launcher_foreground.xml
```

**Example (Vector XML):**
```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="512dp"
    android:height="512dp"
    android:viewportWidth="512"
    android:viewportHeight="512">
  
  <!-- Face outline and data points -->
  <!-- All paths scaled to fit 432x432 safe zone centered in 512x512 canvas -->
  
  <path
      android:fillColor="@color/ink_900"
      android:pathData="M256,140 C200,140 155,185 155,241 C155,297 200,342 256,342 C312,342 357,297 357,241 C357,185 312,140 256,140 Z" />
  
  <!-- More paths... -->
</vector>
```

**If using PNG:**
- Create at 512x512px
- Save with transparency
- Place in `drawable-anydpi-v26/` or multiple density folders

---

### Background Layer

**Purpose:** Solid color or simple gradient behind the foreground

**Canvas Size:** 512x512px (full bleed, no safe zone)  
**Format:** Vector drawable (XML) for solid color, or PNG for gradient

**Design Rules:**
- Must fill entire 512x512px canvas (no margins)
- Simple colors or gradients only (no complex patterns)
- Should complement foreground without being distracting

**File Location:**
```
app/src/main/res/drawable/ic_launcher_background.xml
```

**Example (Solid Color):**
```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="512dp"
    android:height="512dp"
    android:viewportWidth="512"
    android:viewportHeight="512">
  <path
      android:fillColor="@color/honey_500"
      android:pathData="M0,0 L512,0 L512,512 L0,512 Z" />
</vector>
```

**Example (Gradient):**
```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="512dp"
    android:height="512dp"
    android:viewportWidth="512"
    android:viewportHeight="512">
  <path
      android:pathData="M0,0 L512,0 L512,512 L0,512 Z">
    <aapt:attr name="android:fillColor">
      <gradient
          android:startX="256"
          android:startY="0"
          android:endX="256"
          android:endY="512"
          android:type="linear">
        <item android:offset="0" android:color="#FFD166" />
        <item android:offset="1" android:color="#FFBE2E" />
      </gradient>
    </aapt:attr>
  </path>
</vector>
```

---

### Adaptive Icon Definition

**File Location:**
```
app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml
app/src/main/res/mipmap-anydpi-v26/ic_launcher_round.xml
```

**Both files have identical content:**
```xml
<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@drawable/ic_launcher_background" />
    <foreground android:drawable="@drawable/ic_launcher_foreground" />
</adaptive-icon>
```

**Why two files?**
- `ic_launcher.xml` — Default adaptive icon
- `ic_launcher_round.xml` — Round variant (same content for consistency)

---

## 2. Legacy Launcher Icons (Pre-Android 8.0 / API 25 and below)

For devices that don't support adaptive icons, provide traditional raster icons.

### Icon Sizes

| Density | Folder | Size | Description |
|---------|--------|------|-------------|
| mdpi | `mipmap-mdpi/` | 48x48px | 1x baseline |
| hdpi | `mipmap-hdpi/` | 72x72px | 1.5x |
| xhdpi | `mipmap-xhdpi/` | 96x96px | 2x |
| xxhdpi | `mipmap-xxhdpi/` | 144x144px | 3x (most common) |
| xxxhdpi | `mipmap-xxxhdpi/` | 192x192px | 4x (premium devices) |

### Required Files

**Square Icons:**
```
app/src/main/res/mipmap-mdpi/ic_launcher.png       (48x48)
app/src/main/res/mipmap-hdpi/ic_launcher.png       (72x72)
app/src/main/res/mipmap-xhdpi/ic_launcher.png      (96x96)
app/src/main/res/mipmap-xxhdpi/ic_launcher.png     (144x144)
app/src/main/res/mipmap-xxxhdpi/ic_launcher.png    (192x192)
```

**Round Icons:**
```
app/src/main/res/mipmap-mdpi/ic_launcher_round.png       (48x48, circular)
app/src/main/res/mipmap-hdpi/ic_launcher_round.png       (72x72, circular)
app/src/main/res/mipmap-xhdpi/ic_launcher_round.png      (96x96, circular)
app/src/main/res/mipmap-xxhdpi/ic_launcher_round.png     (144x144, circular)
app/src/main/res/mipmap-xxxhdpi/ic_launcher_round.png    (192x192, circular)
```

### Design Rules

**Square Icons:**
- Full composition with rounded corners (system applies corner radius)
- Background: Honey 500
- Foreground: Face + data points in Ink 900
- All elements visible within the square
- Export as 24-bit PNG (no transparency in background)

**Round Icons:**
- Complete composition in circular frame
- All important elements within circle bounds
- Same color scheme as square
- Export as 24-bit PNG

**Note:** Android 7.1 (API 25) introduced round icons for devices with circular icon shapes. Always provide both square and round variants.

---

## 3. Notification Icons

Small monochrome icons displayed in the notification tray and status bar.

### Icon Sizes

| Density | Folder | Size (24dp baseline) |
|---------|--------|---------------------|
| mdpi | `drawable-mdpi/` | 24x24px |
| hdpi | `drawable-hdpi/` | 36x36px |
| xhdpi | `drawable-xhdpi/` | 48x48px |
| xxhdpi | `drawable-xxhdpi/` | 72x72px |
| xxxhdpi | `drawable-xxxhdpi/` | 96x96px |

### Required File

```
app/src/main/res/drawable/ic_notification.xml (vector, preferred)
```

OR raster versions:
```
app/src/main/res/drawable-mdpi/ic_notification.png     (24x24)
app/src/main/res/drawable-hdpi/ic_notification.png     (36x36)
app/src/main/res/drawable-xhdpi/ic_notification.png    (48x48)
app/src/main/res/drawable-xxhdpi/ic_notification.png   (72x72)
app/src/main/res/drawable-xxxhdpi/ic_notification.png  (96x96)
```

### Design Rules

1. **Pure White Silhouette:** Icon must be solid white (#FFFFFF) on transparent background
2. **Simple Shape:** Face outline only, no data points, no gradients, no shading
3. **No Background:** Transparent background (system provides background color)
4. **Recognizable:** Must be identifiable at 24x24px (smallest size)
5. **Alpha Channel Only:** System colorizes the icon, so only shape matters

**Example (Vector XML):**
```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp"
    android:height="24dp"
    android:viewportWidth="24"
    android:viewportHeight="24">
  <path
      android:fillColor="#FFFFFF"
      android:pathData="M12,4 C8.7,4 6,6.7 6,10 C6,13.3 8.7,16 12,16 C15.3,16 18,13.3 18,10 C18,6.7 15.3,4 12,4 Z" />
</vector>
```

**Testing Notification Icon:**
- Light backgrounds (Android 8+): Icon is colored by system
- Dark backgrounds (older): Icon appears white
- System may add circular background automatically (Android 5-7)

---

## 4. Play Store Icon

High-resolution icon for Google Play Store listing.

### Size & Format

- **Size:** 512x512px
- **Format:** 32-bit PNG with alpha channel
- **Color Space:** sRGB
- **Max File Size:** 1MB

### Design Rules

1. Full composition (background + foreground combined)
2. Rounded corners (use 20% radius: ~102px for 512px)
3. No transparency in main content area (background should be opaque)
4. Optimized for display at various sizes (from 48px to 512px)
5. Matches the adaptive icon appearance

**File Location:**
```
design/app-icon-playstore-512.png
```

**Submission:**
- Upload to Google Play Console under "Store Listing" → "Graphic assets"
- Used for store listing, search results, and promotional materials

---

## 5. Web App Icons (PWA / Progressive Web App)

If GlowUp AI has a web version or PWA, generate icons for the web app manifest.

### Required Sizes

| Size | Purpose |
|------|---------|
| 192x192px | Standard Android home screen |
| 512x512px | Android splash screen, high-res display |
| 180x180px | iOS web clip icon |
| 32x32px | Browser favicon |
| 16x16px | Browser tab icon |

### File Locations

```
public/icons/icon-192.png
public/icons/icon-512.png
public/icons/apple-touch-icon.png  (180x180)
public/favicon.ico  (multi-size: 16px, 32px, 48px)
```

### Manifest Reference

**manifest.json:**
```json
{
  "name": "GlowUp AI",
  "short_name": "GlowUp",
  "icons": [
    {
      "src": "/icons/icon-192.png",
      "sizes": "192x192",
      "type": "image/png",
      "purpose": "any maskable"
    },
    {
      "src": "/icons/icon-512.png",
      "sizes": "512x512",
      "type": "image/png",
      "purpose": "any maskable"
    }
  ]
}
```

**HTML head:**
```html
<link rel="icon" type="image/x-icon" href="/favicon.ico">
<link rel="apple-touch-icon" href="/icons/apple-touch-icon.png">
```

---

## Icon Generation Workflow

### Option 1: Manual Export (Design Tool)

**From Figma/Sketch/Illustrator:**

1. **Create Master Artwork**
   - Design at 512x512px (adaptive icon canvas)
   - Keep foreground elements within 432x432px safe zone
   - Export foreground layer separately (transparent background)
   - Export background layer separately (full bleed)

2. **Export Adaptive Layers**
   - Foreground: 512x512px PNG with transparency OR vector XML
   - Background: 512x512px PNG OR vector XML

3. **Export Legacy Icons**
   - Combine foreground + background
   - Add rounded corners (20% radius)
   - Export at all required sizes (48, 72, 96, 144, 192)
   - Create circular crop for `ic_launcher_round` variants

4. **Export Notification Icon**
   - Extract simple face outline
   - Convert to solid white silhouette
   - Export as vector XML OR at all sizes (24, 36, 48, 72, 96)

5. **Export Play Store Icon**
   - Combine foreground + background
   - Add rounded corners (~102px radius for 512px)
   - Export as 512x512px PNG (32-bit, sRGB)

---

### Option 2: Android Studio Image Asset Tool

**Automated generation for launcher icons:**

1. **Open Android Studio**
2. **Right-click** `app/src/main/res` folder
3. **New → Image Asset**
4. **Select Icon Type:**
   - "Launcher Icons (Adaptive and Legacy)"

5. **Configure Foreground Layer:**
   - Source: Upload 512x512px PNG with transparency
   - Resize: Adjust to fit safe zone (432x432)
   - Trim: Enable to remove extra padding

6. **Configure Background Layer:**
   - Color: Choose Honey 500 (#FFBE2E) OR
   - Asset: Upload background gradient PNG

7. **Preview Shapes:**
   - Check all mask shapes (Circle, Squircle, Rounded Square, Teardrop)
   - Ensure critical elements visible in all shapes

8. **Generate:**
   - Click "Next" → "Finish"
   - Tool generates all sizes + adaptive icon XML automatically

**Generated Files:**
- `mipmap-anydpi-v26/ic_launcher.xml`
- `mipmap-anydpi-v26/ic_launcher_round.xml`
- `mipmap-mdpi/` through `mipmap-xxxhdpi/` (all sizes)
- `drawable/ic_launcher_foreground.xml`
- `drawable/ic_launcher_background.xml`

**Limitations:**
- Notification icons: Must create separately (tool doesn't generate these)
- Play Store icon: Must export manually (tool only creates app resources)

---

### Option 3: Command-Line Batch Export

**Using ImageMagick for batch resizing:**

```bash
# Install ImageMagick
brew install imagemagick  # macOS
apt-get install imagemagick  # Linux

# Create all legacy icon sizes from master 192px icon
convert ic_launcher_192.png -resize 48x48 mipmap-mdpi/ic_launcher.png
convert ic_launcher_192.png -resize 72x72 mipmap-hdpi/ic_launcher.png
convert ic_launcher_192.png -resize 96x96 mipmap-xhdpi/ic_launcher.png
convert ic_launcher_192.png -resize 144x144 mipmap-xxhdpi/ic_launcher.png
convert ic_launcher_192.png -resize 192x192 mipmap-xxxhdpi/ic_launcher.png

# Create circular masks for round icons
convert ic_launcher_192.png -alpha set \
  \( +clone -distort DePolar 0 -virtual-pixel HorizontalTile -background None -distort Polar 0 \) \
  -compose Dst_In -composite -trim +repage \
  -resize 192x192 mipmap-xxxhdpi/ic_launcher_round.png

# Repeat for other densities...
```

**Using npm package (icon-gen):**

```bash
npm install -g icon-gen

# Generate all Android icons from single source
icon-gen --input icon_master.svg \
         --output ./res \
         --android \
         --android-manifest ./AndroidManifest.xml
```

---

## Testing Your Icons

### Visual Testing

1. **In Android Studio:**
   - Run app on emulator/device
   - Check icon in app drawer (multiple screens if available)
   - Long-press app icon to verify shape

2. **Adaptive Icon Preview:**
   - Android Studio → Resource Manager → Adaptive Icon
   - Preview all device mask shapes
   - Verify safe zone compliance

3. **Notification Testing:**
   - Send test notification from app
   - Check light and dark notification backgrounds
   - Verify icon is recognizable at small size

### Device Testing

**Test on Multiple Devices:**
- Pixel (stock Android, circular icons)
- Samsung (squircle icons)
- OnePlus (rounded square)
- Various Android versions (6.0, 8.0, 12+)

**Check These Scenarios:**
- Home screen (light and dark wallpapers)
- App drawer
- Recent apps screen
- Notification tray (expanded and collapsed)
- Settings → Apps list
- Play Store (if published)

---

## Troubleshooting

### Icon appears cropped on some devices

**Cause:** Foreground elements extend beyond 432x432px safe zone  
**Fix:** Redesign to fit all critical elements within safe zone

### Icon background doesn't match on notification

**Cause:** Notification icon has colored background  
**Fix:** Ensure notification icon is pure white on transparent background

### Icon looks pixelated

**Cause:** Missing xxxhdpi density or upscaling from lower density  
**Fix:** Provide all density sizes, especially xxxhdpi (192px)

### Adaptive icon doesn't appear (shows legacy icon)

**Cause:** Device running Android 7.1 or below (no adaptive icon support)  
**Fix:** This is expected. Ensure legacy icons look good as fallback.

### Round icon looks squished

**Cause:** Non-circular design cropped into circle  
**Fix:** Design with circular cropping in mind, or use same icon for both square and round

---

## Icon File Checklist

### Adaptive Icons (Android 8.0+)
- [ ] `drawable/ic_launcher_foreground.xml` or PNG (512x512)
- [ ] `drawable/ic_launcher_background.xml` or PNG (512x512)
- [ ] `mipmap-anydpi-v26/ic_launcher.xml`
- [ ] `mipmap-anydpi-v26/ic_launcher_round.xml`

### Legacy Launcher Icons (Pre-Android 8.0)
- [ ] `mipmap-mdpi/ic_launcher.png` (48x48)
- [ ] `mipmap-hdpi/ic_launcher.png` (72x72)
- [ ] `mipmap-xhdpi/ic_launcher.png` (96x96)
- [ ] `mipmap-xxhdpi/ic_launcher.png` (144x144)
- [ ] `mipmap-xxxhdpi/ic_launcher.png` (192x192)
- [ ] `mipmap-mdpi/ic_launcher_round.png` (48x48)
- [ ] `mipmap-hdpi/ic_launcher_round.png` (72x72)
- [ ] `mipmap-xhdpi/ic_launcher_round.png` (96x96)
- [ ] `mipmap-xxhdpi/ic_launcher_round.png` (144x144)
- [ ] `mipmap-xxxhdpi/ic_launcher_round.png` (192x192)

### Notification Icons
- [ ] `drawable/ic_notification.xml` (vector, preferred)
- [ ] OR `drawable-mdpi/` through `drawable-xxxhdpi/` PNG variants

### Play Store & Web
- [ ] Play Store icon: 512x512px PNG (32-bit, sRGB)
- [ ] Web icon: 192x192px PNG
- [ ] Web icon: 512x512px PNG
- [ ] iOS web clip: 180x180px PNG
- [ ] Favicon: 32x32px + 16x16px (multi-size .ico)

---

## Resources & Tools

### Design Tools
- **Figma:** https://figma.com (vector design)
- **Sketch:** https://sketch.com (macOS vector design)
- **Adobe Illustrator:** Vector editing
- **Inkscape:** Free vector editor

### Generation Tools
- **Android Studio Image Asset Tool:** Built-in, automated
- **ImageMagick:** Command-line image processing
- **icon-gen:** npm package for batch generation
- **Adaptive Icon Playground:** https://adapticon.tooo.io (preview tool)

### Testing Tools
- **Android Emulator:** Test multiple device shapes
- **Chrome DevTools:** Device mode for PWA icons
- **Real Devices:** Samsung, Pixel, OnePlus, etc.

### Reference
- **Material Design Icons:** https://m3.material.io/styles/icons
- **Android Icon Design:** https://developer.android.com/guide/practices/ui_guidelines/icon_design_launcher
- **Adaptive Icons Guide:** https://developer.android.com/guide/practices/ui_guidelines/icon_design_adaptive

---

## Quick Command Reference

**Create directory structure:**
```bash
mkdir -p app/src/main/res/{mipmap-mdpi,mipmap-hdpi,mipmap-xhdpi,mipmap-xxhdpi,mipmap-xxxhdpi,mipmap-anydpi-v26,drawable,drawable-mdpi,drawable-hdpi,drawable-xhdpi,drawable-xxhdpi,drawable-xxxhdpi}
```

**Verify icon files exist:**
```bash
find app/src/main/res -name "ic_launcher*" -o -name "ic_notification*"
```

**Check icon sizes:**
```bash
file app/src/main/res/mipmap-xxxhdpi/ic_launcher.png
# Should output: PNG image data, 192 x 192, ...
```

---

## Questions?

For icon generation questions or design feedback: support@glowup.ai

**See also:**
- `APP_ICON_SPEC.md` — Design specifications and concept
- `COLOR_PALETTE.md` — Brand colors for icon
- `COMPONENT_GUIDELINES.md` — UI consistency
