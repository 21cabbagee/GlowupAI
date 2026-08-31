# Play Store Screenshots Specifications

## Overview

Complete technical specifications for GlowUp AI's Google Play Store screenshots and promotional graphics. These assets showcase the app's core features and establish visual brand identity.

**Total Assets Required:**
- 8 phone screenshots (1080x2400px each)
- 1 feature graphic (1024x500px)
- Optional: 1 promo video (30-60 seconds)

---

## Technical Requirements

### Screenshot Dimensions

**Phone Screenshots:**
- Resolution: 1080 x 2400 pixels (9:19.5 aspect ratio)
- Format: PNG or JPEG
- Color space: sRGB
- File size: < 8MB per image
- Quantity: 2-8 screenshots (we're using 8)

**Feature Graphic:**
- Resolution: 1024 x 500 pixels (approximately 2:1 aspect ratio)
- Format: PNG or JPEG
- Color space: sRGB
- File size: < 1MB
- Required: Yes (displays at top of listing)

### Device Frame

**Recommended Device:** Google Pixel 8 or generic Android frame
- Use realistic device bezel
- Include subtle shadow for depth
- Show status bar and navigation bar
- Frame color: Obsidian (black) preferred

**Status Bar Configuration:**
- Time: 9:41 (Apple tradition, widely recognized as "screenshot time")
- Signal: Full bars (5/5)
- WiFi: Connected, full signal
- Battery: 85-95% (not 100%, more realistic)
- No notifications visible
- Use system font for status icons

**Navigation Bar:**
- Show Android gesture bar (modern, pill-shaped indicator)
- Color: Charcoal 900 (#0F0D0A) on dark theme
- Height: 48dp standard

---

## Design System Guidelines

### Color Palette

**Primary Colors (from COLOR_PALETTE.md):**
- Honey 500: #FFBE2E (primary brand, CTAs, active states)
- Honey 400: #FFD166 (hover, secondary accent)
- Honey 300: #FFE29A (tints, chart fills)
- Honey 600: #F0A400 (press states)
- Honey 700: #B87300 (clay, chart lines)

**Backgrounds:**
- Paper: #FFFDF8 (light theme main background)
- Surface: #FFFFFF (light theme cards)
- Surface Variant: #F3ECDD (subtle backgrounds)

**Text:**
- Ink 900: #14110B (primary text)
- Ink 600: #57503F (secondary text)

**Semantic Colors:**
- Sage: #3F7D5C (success, improvement)
- Rust: #C2453F (error, investigate)
- Neutral: #57503F (locked, insufficient data)

### Typography

**Font Family:** Inter (Android system font alternative: Roboto)

**Type Scale:**
- Display Large: 57sp, Regular
- Headline Large: 32sp, SemiBold
- Headline Medium: 28sp, SemiBold
- Title Large: 22sp, SemiBold
- Title Medium: 16sp, SemiBold
- Body Large: 16sp, Regular
- Body Medium: 14sp, Regular
- Label Large: 14sp, SemiBold
- Label Medium: 12sp, SemiBold

### Layout & Spacing

**Screen Margins:** 16dp horizontal, 16dp vertical
**Card Padding:** 24dp
**Component Spacing:** 16dp between major elements
**Corner Radius:** 16dp for cards, 24dp for buttons

---

## Screenshot Specifications

### Screenshot 1: Hero Home Screen

**Purpose:** First impression - showcase main dashboard and core value proposition

**Device State:**
- Status bar: 9:41, full signal, 90% battery
- Screen: HomeScreen.kt
- Theme: Light mode

**Content Layout (Top to Bottom):**

1. **Top Bar (56dp height)**
   - Left: "GlowUp AI" wordmark (Headline Large, Ink 900)
   - Right: Profile avatar (40dp circle, placeholder with first initial)

2. **Streak Banner (80dp height, full width)**
   - Background: Honey 500
   - Icon: Fire emoji 🔥 (24dp)
   - Main text: "28 day streak" (Headline Medium, Ink 900)
   - Subtext: "Keep it going!" (Body Medium, Ink 900)
   - Positioned 16dp from top bar

3. **Latest Capture Card (240dp height)**
   - Background: Surface (white)
   - Elevation: Level 1 shadow
   - Corner radius: 16dp
   - Content:
     - Photo thumbnail (120x120dp, circular mask, left aligned)
     - Date label: "Today, 8:32 AM" (Label Medium, Ink 600)
     - Main metric: "Redness: 2.1" (Title Large, Ink 900)
     - Change indicator: "↓ 15% this month" (Body Medium, Sage with up arrow)
     - Small trend sparkline (honey 700 line, 100x30dp)
   - Position: 16dp below streak banner

4. **Quick Stats Grid (160dp height)**
   - 3 stat cards in horizontal row
   - Each card: 
     - 104dp width × 72dp height
     - Background: Surface Variant (#F3ECDD)
     - Padding: 12dp
     - Layout:
       - Icon (24dp, sage/honey/rust)
       - Label (Label Medium, Ink 600)
       - Value (Title Medium, Ink 900)
   - Cards:
     - Card 1: "Total Captures", "47", camera icon
     - Card 2: "Active Days", "28/30", calendar icon
     - Card 3: "Products", "8", bottle icon
   - Position: 16dp below capture card

5. **Primary CTA Button (56dp height)**
   - Background: Honey 500
   - Text: "Take Capture" (Label Large, Ink 900)
   - Full width minus margins
   - Camera icon (20dp) + text
   - Position: 24dp below quick stats

6. **Recent Activity Section**
   - Header: "Recent Activity" (Title Medium, Ink 900)
   - 2-3 activity items shown (list preview)
   - Each item: icon + text + timestamp
   - Position: 24dp below CTA button

**Annotations (optional callouts with arrows):**
- Arrow to streak banner: "Build consistent habits"
- Arrow to trend sparkline: "Track improvements"
- Arrow to CTA button: "Easy daily capture"

**Caption (below screenshot in store listing):**
"Track your progress at a glance"

---

### Screenshot 2: Guided Capture Experience

**Purpose:** Show the camera interface and face detection technology

**Device State:**
- Status bar: 9:41, full signal, 88% battery
- Screen: CaptureScreen.kt (camera active)
- Theme: Dark overlay (camera UI)

**Content Layout:**

1. **Camera Viewfinder (Full screen minus top/bottom bars)**
   - Live camera preview (simulated with placeholder face)
   - ML Kit face detection overlay visible

2. **Face Detection Overlay (Center screen)**
   - Oval outline (280dp × 360dp)
   - Color: Honey 500 with 50% opacity
   - Stroke: 3dp
   - Corner indicators at cardinal points (small arcs)
   - Face mesh points visible (subtle, 16 key landmarks)
   - Positioning guides: "Move closer" or "Perfect position ✓"

3. **Top Bar (translucent dark background)**
   - Close button (X icon, top left)
   - Help icon (?, top right)
   - Title: "Capture #48" (Label Large, Warm White)

4. **Lighting Quality Indicator (Top, below title)**
   - Background: Translucent Surface (80% opacity)
   - Padding: 12dp horizontal, 8dp vertical
   - Corner radius: 20dp
   - Content:
     - Icon: Green checkmark ✓
     - Text: "Lighting quality: Good" (Label Medium, Sage)
     - Aligned center, below top bar

5. **Positioning Feedback (Center, below face oval)**
   - Background: Translucent Surface
   - Text: "Perfect positioning" (Label Large, Sage)
   - Icon: ✓ checkmark
   - Pill shape, centered

6. **Bottom Controls Bar (120dp height)**
   - Background: Charcoal 800 (80% opacity)
   - Capture button (72dp circle, center)
     - Outer ring: Honey 500
     - Inner fill: White
     - Pulsing animation suggestion (show one frame)
   - Gallery thumbnail (48dp square, bottom left corner)
   - Flash toggle (icon, bottom right)

**Annotations:**
- Arrow to face detection: "Face detection guides positioning"
- Arrow to lighting indicator: "Quality checks ensure consistency"

**Caption:**
"Consistent photos, reliable results"

---

### Screenshot 3: Analysis Results (Metrics Screen)

**Purpose:** Show the automatic skin metric analysis

**Device State:**
- Status bar: 9:41, full signal, 87% battery
- Screen: CaptureResultScreen.kt
- Theme: Light mode

**Content Layout:**

1. **Top Bar**
   - Left: Back arrow
   - Title: "Analysis Results" (Headline Medium, Ink 900)
   - Right: Share icon (optional)

2. **Photo Preview (Top)**
   - Circular photo (160dp diameter)
   - Centered
   - Timestamp below: "Today at 8:35 AM" (Label Medium, Ink 600)
   - Position: 24dp below top bar

3. **Metrics Grid (2×3 grid)**
   - 6 metric cards
   - Each card: 156dp × 140dp
   - Spacing: 12dp between cards
   - Padding: 16dp within each card
   
   **Card Structure (all cards):**
   - Background: Surface (white)
   - Elevation: Level 1
   - Corner radius: 16dp
   - Layout:
     - Icon (24dp, top left, semantic color)
     - Metric name (Label Large, Ink 900, below icon)
     - Value (Display Large, 48sp, Ink 900, prominent)
     - Change indicator (Label Medium, with ↑/↓ arrow, Sage/Rust)
     - Mini sparkline (100×24dp, bottom of card, Honey 700)

**Individual Metric Cards:**

**Row 1:**

1. **Redness Card**
   - Icon: Circle filled (red-ish, but using Rust color)
   - Label: "Redness"
   - Value: "2.1"
   - Change: "↓ 0.3 since last" (Sage - improvement)
   - Sparkline: Downward trend

2. **Texture Card**
   - Icon: Grid pattern
   - Label: "Texture"
   - Value: "3.8"
   - Change: "↑ 0.2 since last" (Sage - improvement)
   - Sparkline: Upward trend

**Row 2:**

3. **Tone Card**
   - Icon: Gradient circle
   - Label: "Tone Evenness"
   - Value: "4.2"
   - Change: "↑ 0.5 since last" (Sage)
   - Sparkline: Upward trend

4. **Hydration Card**
   - Icon: Droplet
   - Label: "Hydration"
   - Value: "3.5"
   - Change: "→ No change" (Neutral gray)
   - Sparkline: Flat line

**Row 3:**

5. **Pores Card**
   - Icon: Dots pattern
   - Label: "Pore Visibility"
   - Value: "2.8"
   - Change: "↓ 0.1 since last" (Sage)
   - Sparkline: Slight downward trend

6. **Breakouts Card**
   - Icon: Alert circle
   - Label: "Breakouts"
   - Value: "1.2"
   - Change: "↓ 0.4 since last" (Sage - fewer breakouts)
   - Sparkline: Downward trend

**Note:** Values are on 0-5 scale. Lower is better for Redness, Pores, Breakouts. Higher is better for Texture, Tone, Hydration.

4. **Bottom CTA**
   - Button: "View Trends" (Secondary button style)
   - Positioned 24dp from bottom

**Annotations:**
- Arrow to change indicators: "Track improvements"
- Arrow to sparklines: "Spot trends at a glance"

**Caption:**
"6 key metrics tracked automatically"

---

### Screenshot 4: Before & After Comparison

**Purpose:** Show side-by-side comparison feature

**Device State:**
- Status bar: 9:41, full signal, 91% battery
- Screen: PhotoComparisonScreen.kt
- Theme: Light mode

**Content Layout:**

1. **Top Bar**
   - Left: Back arrow
   - Title: "Before & After" (Headline Medium, Ink 900)
   - Right: Options menu (3 dots)

2. **Date Range Selector (Below top bar)**
   - Background: Surface Variant
   - Horizontal pill-shaped selector
   - Buttons: "7d", "30d", "60d", "90d", "All"
   - Selected: "60d" (Honey 500 background, Ink 900 text)
   - Others: Transparent background, Ink 600 text
   - Height: 40dp
   - Padding: 8dp

3. **Photo Comparison View (Main content area)**
   - Layout: Side-by-side split screen
   - Total height: 480dp
   - Vertical divider line in center (2dp, Outline color)
   
   **Left Panel:**
   - Photo from 60 days ago
   - Label at top: "Day 1" (Label Large, white text with dark shadow)
   - Positioned: 2dp padding
   - Date: "Jul 1, 2026" (Label Medium, bottom left corner)
   
   **Right Panel:**
   - Recent photo (today)
   - Label at top: "Day 60" (Label Large, white text with dark shadow)
   - Positioned: 2dp padding
   - Date: "Aug 30, 2026" (Label Medium, bottom right corner)
   
   **Slider Control:**
   - Vertical slider handle in center
   - Circle (48dp diameter)
   - Background: Honey 500
   - Icon: Left/right arrows
   - Allows dragging to reveal more of either photo

4. **Metrics Comparison Card (Below photos)**
   - Background: Surface (white)
   - Elevation: Level 1
   - Padding: 20dp
   - Corner radius: 16dp
   - Content:
     - Title: "Overall Improvement" (Title Medium, Ink 900)
     - Metric grid (2 columns × 3 rows):
       
       | Metric | Change |
       |--------|--------|
       | Redness | ↓ 18% (Sage) |
       | Texture | ↑ 12% (Sage) |
       | Tone | ↑ 15% (Sage) |
       | Hydration | ↑ 8% (Sage) |
       | Pores | ↓ 11% (Sage) |
       | Breakouts | ↓ 22% (Sage) |
     
     - Each row: metric name (Body Medium, Ink 900) + change (Label Large, Sage, with arrow)

5. **Bottom Section**
   - Text: "Amazing progress!" (Body Large, Ink 900)
   - Icon: Celebration emoji 🎉
   - CTA: "Share Your Results" button (Secondary, outlined)

**Annotations:**
- Arrow to slider: "Swipe to compare"
- Arrow to metrics card: "See exact improvements"

**Caption:**
"See your real progress over time"

---

### Screenshot 5: Experiments (A/B Testing)

**Purpose:** Showcase the scientific experiment feature

**Device State:**
- Status bar: 9:41, full signal, 86% battery
- Screen: ExperimentsScreen.kt
- Theme: Light mode

**Content Layout:**

1. **Top Bar**
   - Left: Back arrow
   - Title: "Experiments" (Headline Medium, Ink 900)
   - Right: Add button (+ icon in circle)

2. **Active Experiment Card (Featured, larger)**
   - Background: Honey 500 (brand color for emphasis)
   - Elevation: Level 2
   - Padding: 24dp
   - Corner radius: 20dp
   - Width: Full width minus margins
   - Height: 320dp
   
   **Card Content:**
   - Badge: "In Progress" (Label Medium, Ink 900, top left)
   - Title: "Vitamin C Serum Test" (Headline Large, Ink 900)
   - Divider line (1dp, Ink 900 @ 20% opacity)
   
   - **Hypothesis Section:**
     - Label: "HYPOTHESIS" (Label Small, Ink 600, uppercase)
     - Text: "Adding vitamin C serum will reduce redness by 20% in 2 weeks" (Body Large, Ink 900)
   
   - **Progress Section:**
     - Progress bar (Honey 700 filled portion, Ink 900 @ 20% unfilled)
     - Text above bar: "Day 9 of 14" (Label Large, Ink 900)
     - Percentage: "64% complete" (Label Medium, Ink 600)
   
   - **Current Results (Mini preview):**
     - Two small metric cards side by side:
       - "Redness: 2.3" with "↓ 0.5" (Sage)
       - "Texture: 3.9" with "↑ 0.3" (Sage)
   
   - **Timeline Visualization:**
     - Horizontal timeline with dots
     - Start point, current point (Honey 700), end point
     - Dates labeled: "Aug 21", "Today", "Sep 4"
     - Line connecting dots (2dp, Honey 700)

3. **Past Experiments Section Header**
   - Text: "Past Experiments" (Title Medium, Ink 900)
   - Positioned 24dp below active card

4. **Completed Experiment Cards (2 shown)**
   - Each card: 336dp width × 140dp height
   - Background: Surface (white)
   - Elevation: Level 1
   - Padding: 20dp
   
   **Card 1:**
   - Badge: "Completed" (Label Small, Sage background, white text)
   - Title: "Niacinamide Comparison" (Title Medium, Ink 900)
   - Dates: "Jul 15 - Aug 1" (Label Medium, Ink 600)
   - Verdict chip:
     - Background: Sage
     - Text: "Keep - Improved redness 15%" (Label Medium, white)
     - Icon: Checkmark
   
   **Card 2:**
   - Badge: "Completed" (Label Small, Sage background, white text)
   - Title: "Retinol Tolerance Test" (Title Medium, Ink 900)
   - Dates: "Jun 20 - Jul 10" (Label Medium, Ink 600)
   - Verdict chip:
     - Background: Honey 700
     - Text: "Evidence Unclear" (Label Medium, Ink 900)
     - Icon: Question mark

5. **Bottom FAB (Floating Action Button)**
   - Position: Fixed bottom right, 16dp margin
   - Size: 56dp diameter
   - Background: Honey 500
   - Icon: Plus symbol (white)
   - Elevation: Level 2 shadow

**Annotations:**
- Arrow to hypothesis: "Set clear goals"
- Arrow to progress bar: "Track experiment status"
- Arrow to verdict chips: "Get data-driven results"

**Caption:**
"A/B test products scientifically"

---

### Screenshot 6: Routine & Product Tracking

**Purpose:** Show product management and routine tracking

**Device State:**
- Status bar: 9:41, full signal, 92% battery
- Screen: ProductDetailScreen.kt (or routine screen with products visible)
- Theme: Light mode

**Content Layout:**

1. **Top Bar**
   - Left: Back arrow
   - Title: "Morning Routine" (Headline Medium, Ink 900)
   - Right: Edit icon

2. **Tab Selector**
   - Two tabs: "Morning" (selected), "Evening"
   - Selected tab: Honey 500 underline (3dp)
   - Unselected tab: Ink 600 text, no underline
   - Position: Below top bar

3. **Product List (Vertical scrolling)**
   
   **Product Card 1 (Featured with impact):**
   - Background: Surface (white)
   - Elevation: Level 1
   - Height: 160dp
   - Padding: 20dp
   - Corner radius: 16dp
   - Layout: Horizontal (image left, info right)
   
   - Product image: 100×100dp, rounded corners (8dp)
   - Photo: Vitamin C serum bottle
   
   - Info section:
     - Name: "The Ordinary Vitamin C" (Title Medium, Ink 900)
     - Category: "Serum" (Label Medium, Ink 600)
     - Started date: "Started 23 days ago" (Label Medium, Ink 600)
     
     - **Impact Badge (prominent):**
       - Background: Sage (success color)
       - Text: "Impact: +12%" (Label Large, white)
       - Icon: Upward trend arrow
       - Positioned top right of card
     
     - Verdict chip below:
       - Background: Honey 500
       - Text: "Keep - Likely Useful" (Label Medium, Ink 900)

   **Product Card 2:**
   - Similar layout, 160dp height
   - Product: Cleanser
   - Name: "CeraVe Hydrating Cleanser"
   - Category: "Cleanser"
   - Started: "Started 45 days ago"
   - Impact: "Impact: +5%" (smaller improvement, Sage)
   - No verdict chip (neutral)

   **Product Card 3:**
   - Similar layout
   - Product: Moisturizer
   - Name: "La Roche-Posay Toleriane"
   - Category: "Moisturizer"
   - Started: "Started 60 days ago"
   - Impact: "Impact: -3%" (slight negative, Rust color)
   - Verdict chip:
     - Background: Honey 700 (warning color)
     - Text: "Investigate" (Ink 900)
     - Icon: Warning triangle

4. **Add Product FAB**
   - Position: Bottom right, 16dp margin
   - Size: 56dp diameter
   - Background: Honey 500
   - Icon: Barcode scanner icon (white)
   - Text hint bubble: "Scan to add" (appears on first use)
   - Elevation: Level 2

5. **Empty State (if showing 0 products - NOT for this screenshot)**
   - Skip for this screenshot (show full routine)

**Annotations:**
- Arrow to impact badge: "See what's working"
- Arrow to verdict chip: "Data-driven recommendations"
- Arrow to scanner FAB: "Quick barcode scanning"

**Caption:**
"Track what's helping vs hurting"

---

### Screenshot 7: Achievements & Habit Tracking

**Purpose:** Show streak tracking, calendar heatmap, and achievements

**Device State:**
- Status bar: 9:41, full signal, 89% battery
- Screen: AchievementsScreen.kt
- Theme: Light mode

**Content Layout:**

1. **Top Bar**
   - Left: Back arrow
   - Title: "Your Progress" (Headline Medium, Ink 900)
   - Right: Trophy icon (decorative)

2. **Streak Hero Section**
   - Background: Honey 500
   - Height: 160dp
   - Corner radius: 20dp (top only, extends to edges)
   - Padding: 32dp
   - Content:
     - Fire emoji 🔥 (48dp, centered top)
     - Main text: "28 Day Streak" (Display Large, 57sp, Ink 900, centered)
     - Subtext: "You're on fire!" (Body Large, Ink 900, centered)
     - Small text below: "Best streak: 45 days" (Label Medium, Ink 600)

3. **Calendar Heatmap (Below streak section)**
   - Title: "August 2026" (Title Medium, Ink 900)
   - Grid layout: 7 columns (days of week) × 5 rows (weeks)
   - Day labels: M T W T F S S (Label Small, Ink 600)
   - Each cell: 36×36dp square, 4dp spacing
   - Cell colors based on activity:
     - No capture: Surface Variant (#F3ECDD)
     - 1 capture: Honey 300 (light)
     - 2+ captures: Honey 500 (full)
     - Today: Honey 600 (darker) with subtle ring
   - Most cells filled (showing 28/31 days completed)
   - 3 empty cells (days not captured)
   - Position: 24dp below streak section

4. **Achievements Grid**
   - Title: "Achievements" (Title Medium, Ink 900)
   - Grid: 3 columns × 3 rows (showing 9 achievements)
   - Each achievement card: 104×120dp
   - Background: Surface (white)
   - Elevation: Level 1
   - Corner radius: 12dp
   - Padding: 12dp
   - Spacing: 8dp between cards
   
   **Achievement Card Layout:**
   - Icon: 40dp, centered (emoji or custom icon)
   - Title: (Label Medium, Ink 900, centered)
   - Subtitle: (Label Small, Ink 600, centered)
   - Optional badge: "New!" (for recent unlocks)

   **Achievement Cards (Unlocked - 6 shown):**
   
   1. **First Capture**
      - Icon: Camera emoji 📸
      - Title: "First Step"
      - Subtitle: "1 capture"
      - Status: Unlocked
   
   2. **Week Warrior**
      - Icon: Fire emoji 🔥
      - Title: "Week Warrior"
      - Subtitle: "7 day streak"
      - Status: Unlocked
   
   3. **Monthly Master**
      - Icon: Calendar emoji 📅
      - Title: "Monthly Master"
      - Subtitle: "30 day streak"
      - Status: Unlocked, Badge: "New!"
   
   4. **Scientist**
      - Icon: Microscope emoji 🔬
      - Title: "Scientist"
      - Subtitle: "1 experiment"
      - Status: Unlocked
   
   5. **Product Pro**
      - Icon: Package emoji 📦
      - Title: "Product Pro"
      - Subtitle: "Track 5 products"
      - Status: Unlocked
   
   6. **Consistency King**
      - Icon: Crown emoji 👑
      - Title: "Consistent"
      - Subtitle: "14 days in a row"
      - Status: Unlocked

   **Achievement Cards (Locked - 3 shown):**
   
   7. **Quarter Champ**
      - Icon: Trophy emoji 🏆 (greyed out, 50% opacity)
      - Title: "Quarter Champ"
      - Subtitle: "90 day streak"
      - Status: Locked
      - Background: Surface Variant (less prominent)
   
   8. **Experiment Expert**
      - Icon: Beaker emoji 🧪 (greyed out)
      - Title: "Expert"
      - Subtitle: "5 experiments"
      - Status: Locked
   
   9. **Routine Royalty**
      - Icon: Star emoji ⭐ (greyed out)
      - Title: "Routine Royalty"
      - Subtitle: "Perfect month"
      - Status: Locked

5. **Progress Summary Card (Bottom)**
   - Background: Surface (white)
   - Padding: 20dp
   - Content:
     - "Your Journey So Far" (Title Medium, Ink 900)
     - Stats grid:
       - "47 Total Captures"
       - "28 Days Active"
       - "3 Experiments Completed"
       - "8 Products Tracked"
     - Each stat: Body Medium font

**Annotations:**
- Arrow to heatmap: "Visual habit tracking"
- Arrow to achievements: "Earn rewards for consistency"

**Caption:**
"Build consistent skincare habits"

---

### Screenshot 8: Trend Charts & Insights

**Purpose:** Show data visualization and long-term progress

**Device State:**
- Status bar: 9:41, full signal, 84% battery
- Screen: Insights screen with trend chart
- Theme: Light mode

**Content Layout:**

1. **Top Bar**
   - Left: Back arrow
   - Title: "Redness Trends" (Headline Medium, Ink 900)
   - Right: Filter icon (funnel)

2. **Time Range Selector**
   - Pills: "7d", "30d", "60d", "90d"
   - Selected: "90d" (Honey 500 background)
   - Height: 40dp
   - Position: Below top bar

3. **Main Line Chart (Primary content)**
   - Background: Surface (white)
   - Size: Full width × 320dp height
   - Padding: 20dp
   - Corner radius: 16dp
   - Elevation: Level 1
   
   **Chart Elements:**
   - X-axis: Dates (Jun 1 → Aug 30)
   - Y-axis: Redness score (0-5 scale)
   - Grid lines: Ink 600 @ 15% opacity (horizontal only)
   - Data line: Honey 700 (#B87300), 3dp stroke width
   - Fill: Honey 300 @ 30% opacity (area under line)
   - Data points: Honey 600 circles (6dp diameter) at each capture
   
   **Trend:**
   - Starts at ~4.2 (Jun 1)
   - General downward trend
   - Ends at ~2.1 (Aug 30)
   - Some minor fluctuations (realistic, not perfectly smooth)
   - Shows clear improvement over 90 days
   
   **Annotations on Chart:**
   - Event marker at day 30: "Started Vitamin C" (vertical dashed line, Sage color)
   - Arrow pointing to dip after marker: "↓ 15% after change"
   - Event marker at day 60: "Added Niacinamide"
   
   **Chart Labels:**
   - Y-axis labels: 0, 1, 2, 3, 4, 5 (Label Small, Ink 600)
   - X-axis labels: Jun, Jul, Aug (Label Small, Ink 600)
   - Title above chart: "Redness Over Time" (Title Medium, Ink 900)

4. **Summary Stats Card (Below chart)**
   - Background: Honey 500
   - Padding: 24dp
   - Corner radius: 16dp
   - Content:
     - Title: "90-Day Summary" (Title Large, Ink 900)
     - Grid of stats (2×2):
       
       | Stat | Value |
       |------|-------|
       | Average Redness | 3.1 → 2.2 |
       | Overall Change | ↓ 29% |
       | Best Day | Aug 28 (1.9) |
       | Worst Day | Jun 3 (4.5) |
     
     - Each stat: Label above (Label Medium, Ink 600), Value below (Title Medium, Ink 900)

5. **Additional Metrics (Small multiples)**
   - Title: "Other Metrics" (Title Medium, Ink 900)
   - Grid: 2 columns × 2 rows
   - Each cell: 156×100dp mini chart
   - Background: Surface Variant
   - Padding: 12dp
   
   **Mini Charts:**
   1. **Texture**: Small sparkline, upward trend, "↑ 8%"
   2. **Tone**: Small sparkline, upward trend, "↑ 12%"
   3. **Hydration**: Small sparkline, flat, "→ 0%"
   4. **Breakouts**: Small sparkline, downward trend, "↓ 18%"
   
   - Each shows: Metric name, tiny trend line, percentage change

6. **Insight Card (Bottom)**
   - Background: Sage (success color)
   - Padding: 20dp
   - Corner radius: 16dp
   - Content:
     - Icon: Lightbulb 💡
     - Text: "Your skin has improved significantly since adding Vitamin C. Consider keeping it in your routine." (Body Large, white text)
     - CTA: "Learn More" (text link, white, underlined)

**Annotations:**
- Arrow to event marker: "Track product changes"
- Arrow to trend line: "See real improvements"

**Caption:**
"Data-driven skincare decisions"

---

## Feature Graphic Specification

### Dimensions & Format

- Size: 1024 × 500 pixels
- Format: PNG (preferred) or JPEG
- Color space: sRGB
- File size: < 1MB

### Purpose

The feature graphic appears at the top of your Play Store listing. It's a wide banner that showcases your app's brand and key value proposition.

### Design Layout

**Background:**
- Gradient: Honey 300 (#FFE29A) → Honey 500 (#FFBE2E)
- Direction: Left to right (or radial from center)
- Alternative: Solid Honey 500

**Left Section (40% width):**
- App icon (256×256px, scaled down to fit)
- Position: Centered vertically, 80px from left edge
- Add subtle shadow for depth

**Center/Right Section (60% width):**

1. **Headline Text:**
   - Text: "Track What Actually Works"
   - Font: Inter Bold or Roboto Bold
   - Size: 54px
   - Color: Ink 900 (#14110B)
   - Position: Top line

2. **Subheadline Text:**
   - Text: "For YOUR Skin"
   - Font: Inter Bold
   - Size: 48px
   - Color: Ink 900
   - Position: Second line, aligned with headline

3. **Tagline:**
   - Text: "Scientific experiments · Privacy-first · Real results"
   - Font: Inter Medium
   - Size: 22px
   - Color: Ink 600 (#57503F)
   - Position: Below subheadline, 20px gap
   - Separators: · (middle dot)

4. **Visual Elements (right side):**
   - Small mockup screenshots (3 phones, staggered)
   - Each phone: 140px height (scaled down)
   - Show: Home screen, Capture screen, Results screen
   - Position: Right edge, 40px from top
   - Slight rotation (±3°) for dynamic feel
   - Subtle shadows

**Safe Zone:**
- Keep all text and critical elements within center 924×400px
- 50px margin on all sides
- Text should not extend to the edges (may be cropped on some devices)

**Design Notes:**
- Avoid small text (won't be readable)
- Focus on brand and value proposition
- No detailed feature lists
- High contrast for readability
- Works on both light and dark backgrounds

---

## Design Guidelines

### General Principles

1. **Consistency:**
   - All screenshots use the same device frame
   - Same status bar configuration
   - Consistent color palette (honey design system)
   - Same typography scale

2. **Realism:**
   - Use realistic data (not perfect, but improving)
   - Show diverse skin tones across screenshots
   - Realistic streak numbers (28 days, not 365)
   - Believable product names and dates

3. **Clarity:**
   - One main focus per screenshot
   - Clear hierarchy (large text for important info)
   - Minimal UI chrome (focus on content)
   - Annotations should be subtle, not distracting

4. **Accessibility:**
   - High contrast text (meet WCAG AA standards minimum)
   - Large touch targets (48dp minimum)
   - Clear icons with labels
   - Readable at small sizes

5. **Professionalism:**
   - No personal information (use placeholder names)
   - No real user photos (use stock photos or illustrations)
   - Clean, polished UI
   - No debug info or lorem ipsum

### Color Usage

**DO:**
- Use Honey 500 for primary CTAs
- Use Sage for positive trends/improvements
- Use Ink 900 for all primary text
- Use Paper for light theme backgrounds
- Maintain warm tone throughout

**DON'T:**
- Use generic red/green for success/error
- Use pure black (#000) or pure white (#FFF) for text
- Use neon or oversaturated colors
- Mix cool and warm tones

### Typography

**DO:**
- Use clear hierarchy (large headlines, readable body text)
- Use SemiBold for emphasis
- Left-align most text
- Use consistent spacing

**DON'T:**
- Use more than 3 font sizes per screenshot
- Use all caps for long text
- Use italic for emphasis (use SemiBold instead)
- Center-align body text

### Photography (for face placeholders)

**DO:**
- Use diverse skin tones (2-3 different tones across 8 screenshots)
- Use neutral expressions
- Use consistent lighting
- Use front-facing headshots

**DON'T:**
- Use real user photos without permission
- Use celebrity faces
- Use stock photos with watermarks
- Use filtered or beauty-edited photos

---

## Deliverables Checklist

### Before Creating Screenshots:

- [ ] Review all screen mockup data (see SCREENSHOT_MOCKUP_DATA.md)
- [ ] Prepare device frame assets (Pixel 8 frame)
- [ ] Set up color palette in design tool
- [ ] Install Inter or Roboto font
- [ ] Gather placeholder photos (diverse skin tones)
- [ ] Create icon asset (1024×1024px)

### For Each Screenshot:

- [ ] Correct dimensions (1080×2400px)
- [ ] Device frame applied
- [ ] Status bar configured (9:41, full signal, ~85-90% battery)
- [ ] Navigation bar included (gesture pill)
- [ ] All text is readable at full size
- [ ] Colors match design system
- [ ] Data matches SCREENSHOT_MOCKUP_DATA.md
- [ ] No placeholder lorem ipsum
- [ ] No personal information
- [ ] Exported as PNG, sRGB

### Feature Graphic:

- [ ] Correct dimensions (1024×500px)
- [ ] Text within safe zone (50px margins)
- [ ] App icon included
- [ ] Tagline is clear and compelling
- [ ] No text cutoff on edges
- [ ] High contrast for readability
- [ ] Exported as PNG, < 1MB

### Final Review:

- [ ] All 8 screenshots tell a cohesive story
- [ ] Progression makes sense (1→8)
- [ ] Captions are concise and compelling
- [ ] No spelling or grammar errors
- [ ] Consistent branding across all assets
- [ ] Files named correctly (screenshot-1.png, etc.)
- [ ] Feature graphic aligns with screenshot style
- [ ] All assets under file size limits

---

## File Naming Convention

```
play-store-assets/
├── screenshots/
│   ├── screenshot-1-home-hero.png
│   ├── screenshot-2-capture-guided.png
│   ├── screenshot-3-analysis-results.png
│   ├── screenshot-4-before-after.png
│   ├── screenshot-5-experiments.png
│   ├── screenshot-6-routine-products.png
│   ├── screenshot-7-achievements-streak.png
│   └── screenshot-8-trends-insights.png
├── feature-graphic/
│   └── feature-graphic-1024x500.png
└── source-files/
    └── glowup-screenshots-source.fig (or .sketch, .xd)
```

---

## Notes for Designer

1. **Design Tool Recommendations:**
   - Figma (preferred, has device mockup plugins)
   - Sketch (Mac only, good Android plugins)
   - Adobe XD (cross-platform)
   - Photoshop (works but slower workflow)

2. **Useful Plugins:**
   - Mockup generators (Artboard Studio, Rotato)
   - Chart generators (ChartMogul, Figma charts)
   - Color contrast checkers (Stark, Able)

3. **Speed Tips:**
   - Create a master component library first
   - Use consistent spacing tokens
   - Create text styles for all typography
   - Use color styles for brand colors
   - Duplicate and modify rather than rebuild

4. **Testing:**
   - View screenshots at 50% size (simulates store listing)
   - Test on actual Android device if possible
   - Share with team for feedback
   - Check all text for typos

5. **Localization (Future):**
   - Keep text in separate layer (easy to translate)
   - Avoid text in images
   - Plan for text expansion (some languages are 30% longer)

---

## Questions?

For clarification on any specifications, refer to:
- `COLOR_PALETTE.md` - Complete color system
- `SCREENSHOT_MOCKUP_DATA.md` - Exact content for each screen
- `COMPONENT_GUIDELINES.md` - UI component specs
- `APP_ICON_SPEC.md` - Icon design guidelines

Ready to create amazing Play Store screenshots! 🎨
