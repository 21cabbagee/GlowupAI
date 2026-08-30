# GlowUp AI Component Guidelines

## Overview

This document defines the visual and interaction patterns for all UI components in GlowUp AI. Use these specifications to maintain consistency across screens and features.

**Design Principles:**
- One decision per screen (avoid cluttered choices)
- Primary actions are always visually dominant
- Motion communicates state, never decorates
- Scientific clarity with approachable warmth
- Touch targets ≥ 48dp for accessibility

---

## Spacing System

Consistent spacing creates visual rhythm and hierarchy.

### Spacing Scale

| Token | Value | Use Case |
|-------|-------|----------|
| **xs** | 4dp | Tight gaps, chip padding, icon padding |
| **sm** | 8dp | Compact vertical rhythm, list item internal spacing |
| **md** | 16dp | Default component spacing, paragraph gaps |
| **lg** | 24dp | Section spacing, card internal padding |
| **xl** | 32dp | Major section breaks, screen padding |
| **xxl** | 48dp | Hero element spacing, dramatic section breaks |

**Implementation:**
```kotlin
import com.glowup.ai.core.design.GlowSpacing

Spacer(modifier = Modifier.height(GlowSpacing.md)) // 16dp
```

### Layout Margins

| Screen Type | Horizontal Margin | Vertical Margin |
|-------------|------------------|-----------------|
| **Mobile** | 16dp (md) | 16dp (md) |
| **Tablet** | 24dp (lg) | 24dp (lg) |
| **Desktop** | 32dp (xl) | 32dp (xl) |

**Content Max Width (Desktop):**
- Onboarding flow: 560dp (centered, focused)
- Daily app screens: 1200dp (wider, more data)
- Reading content: 640dp (optimal line length)

---

## Buttons

### Primary Button

The main call-to-action on any screen. Only one primary button per screen.

**Visual:**
- Background: Honey 500 (#FFBE2E)
- Foreground: Ink 900 (#14110B)
- Height: 48dp minimum (56dp preferred for major actions)
- Corner radius: 24dp (pill shape for standalone, 16dp in tight layouts)
- Padding: 24dp horizontal, 14dp vertical
- Typography: Label Large (14sp, SemiBold)

**States:**
- Default: Honey 500 background
- Hover: Honey 400 background
- Press: Honey 600 background
- Disabled: Ink 600 @ 12% opacity background, Ink 600 @ 38% opacity text
- Focus: 2dp outline in Honey 700, 4dp offset

**Example:**
```kotlin
Button(
    onClick = { /* ... */ },
    colors = ButtonDefaults.buttonColors(
        containerColor = LocalGlowColors.current.honey500,
        contentColor = LocalGlowColors.current.ink900
    ),
    shape = RoundedCornerShape(24.dp),
    modifier = Modifier
        .fillMaxWidth()
        .height(56.dp)
) {
    Text("Continue", style = MaterialTheme.typography.labelLarge)
}
```

**Usage:**
- "Continue" (onboarding steps)
- "Take Capture" (home screen)
- "Save my routine" (setup flow)
- One per screen maximum

---

### Secondary Button

Supporting actions, less visually dominant than primary.

**Visual:**
- Background: Transparent
- Foreground: Honey 700 (#B87300)
- Border: None (text button style)
- Height: 48dp minimum
- Padding: 16dp horizontal, 14dp vertical
- Typography: Label Large (14sp, SemiBold)

**States:**
- Default: Honey 700 text
- Hover: Honey 700 text, Surface Variant background
- Press: Honey 700 text, Surface Variant @ 50% opacity
- Disabled: Ink 600 @ 38% opacity text
- Focus: 2dp outline in Outline color

**Example:**
```kotlin
TextButton(onClick = { /* ... */ }) {
    Text(
        text = "Skip for now",
        style = MaterialTheme.typography.labelLarge,
        color = LocalGlowColors.current.honey700
    )
}
```

**Usage:**
- "Skip for now"
- "Not now"
- "I'm starting fresh"
- "View details"

---

### Destructive Button

For dangerous actions (delete account, discard data).

**Visual:**
- Background: Rust (#C2453F)
- Foreground: Paper (#FFFDF8)
- Height: 48dp minimum
- Corner radius: 16dp (not pill, more serious)
- Padding: 24dp horizontal, 14dp vertical
- Typography: Label Large (14sp, SemiBold)

**States:**
- Default: Rust background
- Hover: Rust @ 90% opacity
- Press: Rust @ 80% opacity
- Disabled: Ink 600 @ 12% opacity background
- Focus: 2dp outline in Rust

**Example:**
```kotlin
Button(
    onClick = { /* ... */ },
    colors = ButtonDefaults.buttonColors(
        containerColor = LocalGlowColors.current.danger,
        contentColor = LocalGlowColors.current.paper
    )
) {
    Text("Delete my account")
}
```

**Usage:**
- Always require confirmation (dialog or typed phrase)
- Never as primary CTA
- Clear about consequences
- "Delete my account", "Discard changes", "Stop experiment"

---

### Icon Buttons

Small, square/circular buttons with icon only.

**Visual:**
- Size: 48x48dp (touch target)
- Icon size: 24x24dp
- Background: Transparent (default)
- Icon color: Ink 900 (light) / Warm White (dark)
- Corner radius: 24dp (circle)

**States:**
- Default: Transparent background
- Hover: Ink 600 @ 8% opacity background
- Press: Ink 600 @ 12% opacity background
- Disabled: Icon @ 38% opacity
- Focus: 2dp outline

**Example:**
```kotlin
IconButton(onClick = { /* ... */ }) {
    Icon(
        imageVector = Icons.Default.ArrowBack,
        contentDescription = "Back",
        tint = LocalGlowColors.current.ink900
    )
}
```

**Usage:**
- Back button (top-left)
- Close button (dialogs, sheets)
- Overflow menu trigger
- Small actions in cards

---

### Floating Action Button (FAB)

Prominent action, always visible (e.g., "Take Capture").

**Visual:**
- Size: 56x56dp (standard FAB)
- Background: Honey 500
- Icon: Ink 900, 24x24dp
- Corner radius: 16dp (rounded square, not circle)
- Elevation: 8dp (Level 3)

**States:**
- Default: Honey 500 background, 8dp elevation
- Hover: Honey 400 background, 10dp elevation
- Press: Honey 600 background, 6dp elevation
- Disabled: Ink 600 @ 12% opacity, 0dp elevation

**Example:**
```kotlin
FloatingActionButton(
    onClick = { /* ... */ },
    containerColor = LocalGlowColors.current.honey500,
    contentColor = LocalGlowColors.current.ink900,
    shape = RoundedCornerShape(16.dp)
) {
    Icon(Icons.Default.CameraAlt, contentDescription = "Take Capture")
}
```

**Usage:**
- Central "Capture" action in bottom nav
- "Add product" in routine screen
- One per screen maximum

---

## Cards

### Standard Card

Container for related content.

**Visual:**
- Background: Surface (light) / Charcoal 800 (dark)
- Corner radius: 16dp (GlowShapes.md)
- Padding: 16dp (md) for compact, 24dp (lg) for spacious
- Elevation: 2dp (Level 1) in light theme, none in dark (surface color differentiation)

**Content Structure:**
```
[Card Header: Title Large]
↓ 4dp space
[Card Body: Body Medium]
↓ 8dp space
[Card Metadata: Body Small, Ink 600]
↓ 12dp space (optional)
[Card Action: TextButton aligned end]
```

**Example:**
```kotlin
Card(
    modifier = Modifier.fillMaxWidth(),
    colors = CardDefaults.cardColors(
        containerColor = LocalGlowColors.current.surfaceCard
    ),
    shape = GlowShapes.md,
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
) {
    Column(modifier = Modifier.padding(GlowSpacing.lg)) {
        Text("Your next best step", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(4.dp))
        Text("Keep your routine steady...", style = MaterialTheme.typography.bodyMedium)
    }
}
```

**Usage:**
- Home feed items
- Experiment cards
- Product detail cards
- Summary cards

---

### Metric Card

Small card showing a single data point.

**Visual:**
- Background: Surface Variant (light) / Charcoal 700 (dark)
- Corner radius: 12dp (slightly tighter)
- Padding: 12dp
- Min height: 80dp
- Elevation: None (flat)

**Content Structure:**
```
[Metric Label: Body Small, Ink 600]
↓ 4dp space
[Metric Value: Headline Small]
↓ 2dp space
[Metric Trend: Body Small + Icon]
```

**Example:**
```kotlin
Card(
    colors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surfaceVariant
    ),
    shape = RoundedCornerShape(12.dp),
    modifier = Modifier.size(width = 160.dp, height = 80.dp)
) {
    Column(modifier = Modifier.padding(12.dp)) {
        Text("Redness", style = MaterialTheme.typography.bodySmall)
        Text("2.3", style = MaterialTheme.typography.headlineSmall)
        Row {
            Icon(Icons.Default.TrendingDown, contentDescription = null, modifier = Modifier.size(16.dp))
            Text("-12%", style = MaterialTheme.typography.bodySmall)
        }
    }
}
```

**Usage:**
- Metrics dashboard (6 metrics on result screen)
- Quick stats on home screen
- Compact data visualization

---

### Verdict Chip Card

Self-contained chip showing product verdict.

**Visual:**
- Background: Verdict-specific color (see COLOR_PALETTE.md)
- Foreground: Verdict-specific `onXxx` color
- Corner radius: 8dp (GlowShapes.sm)
- Padding: 8dp horizontal, 4dp vertical
- Typography: Label Medium (12sp, SemiBold)
- Elevation: None (flat, relies on color)

**Variants:**
- `keep`: Honey 500 bg, Ink 900 text
- `likely_useful`: Sage bg, Paper/White text
- `evidence_unclear`: Honey 700 bg, Ink 900 text
- `investigate`: Rust bg, Paper text
- `locked`: Ink 600 bg, Paper text

**Example:**
```kotlin
Surface(
    color = glowColors.verdictColor("likely_useful"),
    shape = GlowShapes.sm
) {
    Text(
        text = "Likely useful",
        style = MaterialTheme.typography.labelMedium,
        color = glowColors.onVerdictColor("likely_useful"),
        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
    )
}
```

**Usage:**
- Product verdict labels
- Experiment status badges
- Evidence state indicators

---

## Input Fields

### Text Input

Standard text input field.

**Visual:**
- Background: Surface Variant (light) / Charcoal 700 (dark)
- Border: None (default), 2dp Honey 500 (focused), 2dp Rust (error)
- Corner radius: 12dp
- Height: 56dp (single line)
- Padding: 16dp horizontal, 16dp vertical
- Typography: Body Large (16sp, Regular)

**Label:**
- Typography: Body Small (12sp, Regular)
- Color: Ink 600 (light) / Warm Grey (dark)
- Position: Above field, 8dp margin

**Supporting Text:**
- Typography: Body Small (12sp, Regular)
- Color: Ink 600 (default), Rust (error)
- Position: Below field, 4dp margin

**States:**
- Default: Surface Variant background, no border
- Focused: Honey 500 border (2dp)
- Error: Rust border (2dp), error text below
- Disabled: Ink 600 @ 12% opacity background, Ink 600 @ 38% opacity text

**Example:**
```kotlin
OutlinedTextField(
    value = name,
    onValueChange = { name = it },
    label = { Text("Your name") },
    supportingText = { Text("Only used to personalize your private space.") },
    colors = TextFieldDefaults.colors(
        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
        focusedIndicatorColor = LocalGlowColors.current.honey500
    ),
    modifier = Modifier.fillMaxWidth()
)
```

**Usage:**
- Name input (profile setup)
- Product search
- Note/description fields
- Q&A question input

---

### Multi-line Text Input

For longer text input (notes, questions).

**Visual:**
- Same as Text Input
- Min height: 120dp (3-4 lines visible)
- Max height: 240dp (scrollable beyond)
- Typography: Body Medium (14sp, Regular)

**Example:**
```kotlin
OutlinedTextField(
    value = note,
    onValueChange = { note = it },
    label = { Text("Anything else?") },
    placeholder = { Text("A sentence is enough.") },
    minLines = 4,
    maxLines = 8,
    modifier = Modifier.fillMaxWidth()
)
```

**Usage:**
- "Anything else?" field (goals screen)
- Q&A question input
- Product notes
- Experiment hypothesis

---

## Selection Controls

### Checkbox

Binary choice, often in lists.

**Visual:**
- Size: 24x24dp (icon), 48x48dp (touch target with padding)
- Unchecked: 2dp border, Outline color
- Checked: Honey 500 background, Ink 900 checkmark
- Corner radius: 4dp

**States:**
- Unchecked: Border only
- Checked: Honey 500 fill with checkmark
- Disabled: Ink 600 @ 12% opacity
- Focus: 2dp outline, 4dp offset

**Example:**
```kotlin
Checkbox(
    checked = consentChecked,
    onCheckedChange = { consentChecked = it },
    colors = CheckboxDefaults.colors(
        checkedColor = LocalGlowColors.current.honey500,
        checkmarkColor = LocalGlowColors.current.ink900
    )
)
```

**Usage:**
- Consent checkbox (onboarding)
- Multi-select lists
- Settings toggles (when binary)

---

### Switch (Toggle)

Binary on/off control, shows current state.

**Visual:**
- Track width: 52dp, height: 32dp
- Thumb diameter: 24dp (centered in track)
- Corner radius: 16dp (pill)
- Off: Ink 600 @ 38% opacity track, Paper thumb
- On: Honey 500 track, Ink 900 thumb

**States:**
- Off: Grey track, white thumb (left position)
- On: Honey 500 track, dark thumb (right position)
- Disabled: Ink 600 @ 12% opacity track, Ink 600 @ 38% thumb

**Example:**
```kotlin
Switch(
    checked = darkModeEnabled,
    onCheckedChange = { darkModeEnabled = it },
    colors = SwitchDefaults.colors(
        checkedTrackColor = LocalGlowColors.current.honey500,
        checkedThumbColor = LocalGlowColors.current.ink900
    )
)
```

**Usage:**
- Dark mode toggle
- Notification settings
- Privacy options (cloud backup on/off)
- Experiment active/paused

---

### Radio Button

Single selection from a small set (2-5 options).

**Visual:**
- Size: 24x24dp (icon), 48x48dp (touch target)
- Unselected: 2dp border, Outline color
- Selected: Honey 500 outer circle (20dp), Ink 900 inner dot (10dp)

**States:**
- Unselected: Border circle only
- Selected: Filled honey circle with dark center
- Disabled: Ink 600 @ 12% opacity
- Focus: 2dp outline, 4dp offset

**Example:**
```kotlin
RadioButton(
    selected = selectedOption == option,
    onClick = { selectedOption = option },
    colors = RadioButtonDefaults.colors(
        selectedColor = LocalGlowColors.current.honey500,
        unselectedColor = MaterialTheme.colorScheme.outline
    )
)
```

**Usage:**
- Routine slot selection (Morning / Evening / Both)
- Baseline context questions
- Preference selections (rare, prefer chips)

---

### Chips (Selection Pills)

Single or multi-select from a medium set (3-8 options).

**Visual (Unselected):**
- Background: Surface Variant (light) / Charcoal 700 (dark)
- Foreground: Ink 900 (light) / Warm White (dark)
- Border: None
- Corner radius: 16dp (pill)
- Height: 36dp
- Padding: 16dp horizontal, 8dp vertical
- Typography: Label Large (14sp, SemiBold)

**Visual (Selected):**
- Background: Honey 500
- Foreground: Ink 900
- Optional checkmark icon (leading, 18dp)

**States:**
- Default: Surface Variant background
- Hover: Surface Variant @ 80% opacity
- Selected: Honey 500 background
- Selected + Hover: Honey 400 background
- Disabled: Ink 600 @ 12% opacity background

**Example:**
```kotlin
FilterChip(
    selected = selected,
    onClick = { selected = !selected },
    label = { Text("Breakouts", style = MaterialTheme.typography.labelLarge) },
    colors = FilterChipDefaults.filterChipColors(
        selectedContainerColor = LocalGlowColors.current.honey500,
        selectedLabelColor = LocalGlowColors.current.ink900
    )
)
```

**Usage:**
- Goal selection (onboarding)
- Product category filters
- Concern selection
- Tag selection

---

## Navigation

### Bottom Navigation Bar (Mobile)

Primary navigation for daily app use.

**Visual:**
- Height: 80dp (64dp bar + 16dp padding for gesture nav)
- Background: Surface (light) / Charcoal 800 (dark)
- Elevation: 8dp (light), none (dark, uses color differentiation)
- Items: 5 total (Home, Routine, Capture, Insights, You)

**Navigation Item:**
- Icon: 24x24dp
- Label: Label Medium (12sp, SemiBold)
- Spacing: 4dp between icon and label
- Min width: 64dp, max width: 168dp

**States (Item):**
- Inactive: Ink 600 icon/label
- Active: Honey 500 icon, Ink 900 label
- Hover: Ink 600 @ 8% opacity background
- Press: Ink 600 @ 12% opacity background

**Center Capture Button:**
- Size: 56x56dp
- Background: Honey 500
- Icon: Ink 900, 28x28dp (slightly larger)
- Elevation: 8dp
- Position: Raised 8dp above bar baseline

**Example:**
```kotlin
NavigationBar(
    containerColor = LocalGlowColors.current.surfaceCard
) {
    items.forEach { item ->
        NavigationBarItem(
            selected = currentRoute == item.route,
            onClick = { navController.navigate(item.route) },
            icon = { Icon(item.icon, contentDescription = item.label) },
            label = { Text(item.label, style = MaterialTheme.typography.labelMedium) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = LocalGlowColors.current.honey500,
                selectedTextColor = LocalGlowColors.current.ink900,
                unselectedIconColor = LocalGlowColors.current.ink600,
                unselectedTextColor = LocalGlowColors.current.ink600
            )
        )
    }
}
```

---

### Top App Bar

Screen title and navigation (onboarding, detail screens).

**Visual:**
- Height: 64dp
- Background: Transparent (blends with page background)
- Title: Headline Small (24sp, SemiBold), Ink 900
- Back button: IconButton, top-left

**Variants:**
- **Small (default):** Title centered, back button left
- **Large (scrollable):** Title animates from large to small on scroll

**Example:**
```kotlin
TopAppBar(
    title = { Text("Profile", style = MaterialTheme.typography.headlineSmall) },
    navigationIcon = {
        IconButton(onClick = { navController.popBackStack() }) {
            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
        }
    },
    colors = TopAppBarDefaults.topAppBarColors(
        containerColor = Color.Transparent
    )
)
```

**Usage:**
- Onboarding screens (back button + progress indicator)
- Detail screens (back button + title)
- NOT in daily bottom-nav screens (use page title instead)

---

## Dialogs & Sheets

### Dialog (Alert/Confirmation)

Modal dialog for critical decisions.

**Visual:**
- Background: Surface (light) / Charcoal 800 (dark)
- Corner radius: 24dp (GlowShapes.lg)
- Max width: 320dp (mobile), 400dp (tablet/desktop)
- Padding: 24dp
- Elevation: 24dp (Level 3)

**Content Structure:**
```
[Icon: optional, 48x48dp, top-center]
↓ 16dp space
[Title: Headline Small, center-aligned]
↓ 8dp space
[Body: Body Medium, center or start-aligned]
↓ 24dp space
[Actions: Row, end-aligned or full-width stacked]
```

**Actions:**
- Primary action: Right position (or bottom in stacked)
- Secondary action: Left position (or top in stacked)
- Destructive action: Always confirm, full-width stacked

**Example:**
```kotlin
AlertDialog(
    onDismissRequest = { /* ... */ },
    title = { Text("Delete capture?", style = MaterialTheme.typography.headlineSmall) },
    text = { Text("This cannot be undone.", style = MaterialTheme.typography.bodyMedium) },
    confirmButton = {
        TextButton(onClick = { /* delete */ }) {
            Text("Delete", color = LocalGlowColors.current.danger)
        }
    },
    dismissButton = {
        TextButton(onClick = { /* dismiss */ }) {
            Text("Cancel")
        }
    }
)
```

**Usage:**
- Delete confirmations
- Consent decisions (explicit choice)
- Error recovery prompts
- Permission explanations

---

### Bottom Sheet

Contextual panel sliding from bottom (mobile) or side (desktop).

**Visual:**
- Background: Surface (light) / Charcoal 800 (dark)
- Corner radius: 24dp (top corners only)
- Handle: 32x4dp rounded pill, Ink 600 @ 20% opacity, top-center, 8dp margin
- Padding: 24dp (lg)
- Elevation: 16dp (Level 2)

**Content Structure:**
```
[Handle: drag indicator]
↓ 8dp space
[Title: Headline Small]
↓ 16dp space
[Content: scrollable body]
↓ 16dp space
[Actions: optional, full-width buttons]
```

**States:**
- Collapsed (peek height: 120dp)
- Expanded (full height or fit-content)
- Dismissed (swipe down or tap scrim)

**Example:**
```kotlin
ModalBottomSheet(
    onDismissRequest = { /* ... */ },
    sheetState = sheetState,
    containerColor = LocalGlowColors.current.surfaceCard
) {
    Column(modifier = Modifier.padding(GlowSpacing.lg)) {
        Text("Add product", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(16.dp))
        // Content...
    }
}
```

**Usage:**
- Product add flow
- Filter/sort options
- Secondary actions (share, export)
- Privacy policy/help text

---

## Progress Indicators

### Linear Progress Bar

Shows determinate progress (e.g., multi-step onboarding).

**Visual:**
- Height: 4dp
- Corner radius: 2dp (pill)
- Track color: Ink 600 @ 12% opacity
- Progress color: Honey 500

**Example:**
```kotlin
LinearProgressIndicator(
    progress = 0.6f, // 60%
    modifier = Modifier.fillMaxWidth(),
    color = LocalGlowColors.current.honey500,
    trackColor = LocalGlowColors.current.ink600.copy(alpha = 0.12f)
)
```

**Usage:**
- Onboarding step progress (top of screen)
- File upload progress
- Multi-stage operations

---

### Circular Progress Indicator

Shows indeterminate loading (unknown duration).

**Visual:**
- Size: 48dp (default), 24dp (small)
- Stroke width: 4dp
- Color: Honey 500

**Example:**
```kotlin
CircularProgressIndicator(
    color = LocalGlowColors.current.honey500
)
```

**Usage:**
- Full-screen loading states
- Button loading state (replace text with spinner)
- Card content loading

---

### Shimmer / Skeleton Loading

Content placeholder while loading.

**Visual:**
- Background: Shimmer color from palette
- Shape: Matches final content shape (text lines, card rectangles)
- Animation: Subtle shimmer sweep, 1.5s duration

**Example:**
```kotlin
Box(
    modifier = Modifier
        .fillMaxWidth()
        .height(80.dp)
        .background(
            brush = Brush.linearGradient(
                colors = listOf(
                    LocalGlowColors.current.shimmer,
                    LocalGlowColors.current.shimmer.copy(alpha = 0.5f),
                    LocalGlowColors.current.shimmer
                )
            ),
            shape = GlowShapes.md
        )
)
```

**Usage:**
- Home feed loading state
- Metrics dashboard loading
- Profile loading

---

## Empty States

### Full-Screen Empty State

When major content is missing (no baseline, no routine).

**Visual:**
- Background: Paper (light) / Charcoal 900 (dark)
- Icon/Illustration: 120x120dp, center-aligned, Ink 600
- Heading: Display Small (36sp), center-aligned, 16dp below icon
- Body: Body Large (16sp), center-aligned, 8dp below heading, max width 440dp
- Primary CTA: 24dp below body, center-aligned

**Example:**
```kotlin
Column(
    modifier = Modifier.fillMaxSize().padding(GlowSpacing.xl),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center
) {
    Icon(
        imageVector = Icons.Default.CameraAlt,
        contentDescription = null,
        modifier = Modifier.size(120.dp),
        tint = LocalGlowColors.current.ink600
    )
    Spacer(Modifier.height(16.dp))
    Text(
        text = "Your history starts here",
        style = MaterialTheme.typography.displaySmall,
        textAlign = TextAlign.Center
    )
    Spacer(Modifier.height(8.dp))
    Text(
        text = "Take your first comparable frame to begin tracking.",
        style = MaterialTheme.typography.bodyLarge,
        textAlign = TextAlign.Center,
        modifier = Modifier.widthIn(max = 440.dp)
    )
    Spacer(Modifier.height(24.dp))
    Button(onClick = { /* ... */ }) {
        Text("Take baseline")
    }
}
```

**Usage:**
- No baseline captured yet
- No products in routine
- No experiments active

---

### Inline Empty State

When optional content is missing (no verdicts yet, no Q&A history).

**Visual:**
- Background: Surface Variant (light) / Charcoal 700 (dark)
- Corner radius: 12dp
- Padding: 24dp
- Icon: 48x48dp, Ink 600
- Text: Body Medium, Ink 600, center-aligned
- Optional CTA: TextButton below text

**Example:**
```kotlin
Card(
    colors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surfaceVariant
    ),
    shape = RoundedCornerShape(12.dp)
) {
    Column(
        modifier = Modifier.padding(24.dp).fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Default.Lightbulb, contentDescription = null, modifier = Modifier.size(48.dp))
        Spacer(Modifier.height(8.dp))
        Text("No verdicts yet", style = MaterialTheme.typography.bodyMedium)
        Text("Check back after 4+ weeks of tracking.", style = MaterialTheme.typography.bodySmall)
    }
}
```

**Usage:**
- No verdicts available yet
- No Q&A questions asked
- No recent activity in feed

---

## Motion & Animation

### Transition Durations

From `GlowMotion`:
- **Fast:** 140ms (micro-interactions, ripples)
- **Standard:** 180ms (default transitions, page changes)
- **Slow:** 220ms (dramatic entrances, full-screen transitions)

### Easing Curve

Cubic-bezier(0.2, 0.8, 0.2, 1) for all transitions.

### Common Animations

**Fade In/Out:**
```kotlin
AnimatedVisibility(
    visible = visible,
    enter = fadeIn(animationSpec = GlowMotion.standard),
    exit = fadeOut(animationSpec = GlowMotion.standard)
)
```

**Slide Transitions (Onboarding):**
```kotlin
// Forward navigation: slide in from right
AnimatedContent(
    targetState = currentScreen,
    transitionSpec = {
        slideInHorizontally(GlowMotion.standard) { it } with
        slideOutHorizontally(GlowMotion.standard) { -it }
    }
)
```

**Scale + Fade (Dialogs):**
```kotlin
AnimatedVisibility(
    visible = showDialog,
    enter = fadeIn(GlowMotion.standard) + scaleIn(GlowMotion.standard, initialScale = 0.9f),
    exit = fadeOut(GlowMotion.fast) + scaleOut(GlowMotion.fast, targetScale = 0.95f)
)
```

**Reduced Motion:**
Always respect user preference:
```kotlin
val reducedMotion = rememberReducedMotion()
val animationSpec = if (reducedMotion) snap() else GlowMotion.standard
```

---

## Accessibility Guidelines

### Touch Targets

Minimum touch target: **48x48dp** (Android), **44x44pt** (iOS)
- Buttons: 48dp height minimum, 56dp preferred
- Icon buttons: 48x48dp
- List items: 56dp height minimum
- Switches/checkboxes: 48dp touch area (icon can be smaller)

### Focus Indicators

All interactive elements must show clear focus state:
- Outline: 2dp width
- Color: Honey 700 (light) / Honey 400 (dark)
- Offset: 4dp outside element bounds
- Corner radius: Matches element shape

### Content Descriptions

All icons, images, and non-text elements must have:
- `contentDescription` (Android)
- `aria-label` or `alt` (Web)
- Meaningful description of action/content, not generic ("Back button" not "Arrow icon")

### Color Contrast

All text/background pairs verified to meet WCAG 2.1 AA:
- Body text: ≥4.5:1
- Large text: ≥3:1
- Non-text UI: ≥3:1

### Screen Reader Support

- Logical reading order (top to bottom, left to right)
- Grouped related content with `semantics`
- Announce state changes ("Loading", "Error", "Success")
- Clear hierarchy (headings, lists, sections)

---

## Component Quick Reference

| Component | Primary Use | Key Visual | Interaction |
|-----------|-------------|------------|-------------|
| Primary Button | Main CTA | Honey 500 bg, 56dp height | Tap, keyboard |
| Secondary Button | Supporting action | Honey 700 text, transparent | Tap, keyboard |
| Card | Content container | 16dp corners, 2dp elevation | Tap (if interactive) |
| Metric Card | Data point | 12dp corners, flat | Visual only |
| Text Input | User text entry | 56dp height, 12dp corners | Tap, keyboard |
| Chip | Multi-select | 36dp height, pill shape | Tap to toggle |
| Switch | Binary toggle | 52x32dp, pill track | Tap, drag |
| Bottom Nav | Primary nav | 5 items, center raised FAB | Tap |
| Dialog | Critical decision | 24dp corners, 320dp width | Tap actions |
| Bottom Sheet | Contextual panel | Top-rounded, drag handle | Swipe, tap |
| Progress | Loading state | 4dp bar or 48dp circle | Visual only |
| Empty State | No content | Icon + text + CTA | Tap CTA |

---

## Implementation Checklist

When implementing a new component:
- [ ] Uses GlowSpacing for all spacing
- [ ] Uses GlowShapes for corner radius
- [ ] Uses GlowColors for all colors (no hardcoded hex)
- [ ] Uses GlowTypography for all text
- [ ] Uses GlowMotion for all animations
- [ ] Respects reduced motion preference
- [ ] Touch target ≥ 48dp for all interactive elements
- [ ] Clear focus indicator on all interactive elements
- [ ] Content descriptions on all icons/images
- [ ] Color contrast verified (≥4.5:1 for text)
- [ ] Tested at 1.3x and 1.5x font scale
- [ ] Tested in light and dark themes
- [ ] Keyboard navigation supported
- [ ] Screen reader announces states correctly

---

## Questions?

For component usage questions or new component requests: support@glowup.ai

**See also:**
- `COLOR_PALETTE.md` — Color system
- `TYPOGRAPHY.md` — Type system
- `app/src/main/java/com/glowup/ai/core/design/` — Design system code
