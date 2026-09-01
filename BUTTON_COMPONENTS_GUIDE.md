# GlowUp AI Button Components Guide

## Overview

The `GlowButtons.kt` file provides three polished button components that follow Cal.ai-level quality standards and implement the design specifications from `UI_REDESIGN_MASTER_PLAN.md`.

**File Location:** `app/src/main/java/com/glowup/ai/core/ui/GlowButtons.kt`

---

## Components

### 1. GlowPrimaryButton

Primary action button with amber-to-orange gradient background.

**Features:**
- ✅ Gradient background (amber #F59E0B → orange #F97316)
- ✅ 2dp elevation with soft shadow
- ✅ Press animation (scales to 96%, alpha to 80%)
- ✅ Icon support with 8dp spacing
- ✅ Loading state with spinner
- ✅ Disabled state with 50% opacity
- ✅ Haptic feedback on press
- ✅ Accessibility support with reduced motion

**Usage:**

```kotlin
// Basic usage
GlowPrimaryButton(
    text = "Continue",
    onClick = { /* action */ }
)

// With icon
GlowPrimaryButton(
    text = "Continue with Google",
    onClick = { /* action */ },
    icon = Icons.Default.Star
)

// Loading state
GlowPrimaryButton(
    text = "Processing",
    onClick = { /* action */ },
    loading = true
)

// Disabled state
GlowPrimaryButton(
    text = "Submit",
    onClick = { /* action */ },
    enabled = false
)
```

**Parameters:**
- `text: String` - Button label text
- `onClick: () -> Unit` - Click handler
- `modifier: Modifier = Modifier` - Optional modifier
- `icon: ImageVector? = null` - Optional leading icon
- `enabled: Boolean = true` - Whether button is interactive
- `loading: Boolean = false` - Whether to show loading spinner
- `contentDescription: String? = null` - Accessibility description

---

### 2. GlowSecondaryButton

Secondary action button with white background and subtle border.

**Features:**
- ✅ White background with 1.5dp border (#E4E4E7)
- ✅ Press animation (scales to 96%, alpha to 80%)
- ✅ Icon support with 8dp spacing
- ✅ Loading state with spinner
- ✅ Disabled state with 50% opacity
- ✅ Haptic feedback on press
- ✅ Accessibility support

**Usage:**

```kotlin
// Basic usage
GlowSecondaryButton(
    text = "Continue with email",
    onClick = { /* action */ }
)

// With icon
GlowSecondaryButton(
    text = "Cancel",
    onClick = { /* action */ },
    icon = Icons.Default.Close
)

// Loading state
GlowSecondaryButton(
    text = "Saving",
    onClick = { /* action */ },
    loading = true
)
```

**Parameters:**
Same as `GlowPrimaryButton`

---

### 3. GlowFloatingActionButton

Floating action button with gradient background and elevated appearance.

**Features:**
- ✅ Gradient background (amber → orange)
- ✅ 6dp elevation with pronounced shadow
- ✅ Press animation (scales to 96% with 2° rotation)
- ✅ 56dp diameter (Material Design standard)
- ✅ Icon-only design
- ✅ Loading state with spinner
- ✅ Disabled state
- ✅ Haptic feedback on press

**Usage:**

```kotlin
// Basic usage
GlowFloatingActionButton(
    onClick = { /* action */ },
    icon = Icons.Default.Add,
    contentDescription = "Add product"
)

// Loading state
GlowFloatingActionButton(
    onClick = { /* action */ },
    icon = Icons.Default.Edit,
    loading = true,
    contentDescription = "Saving"
)

// Disabled state
GlowFloatingActionButton(
    onClick = { /* action */ },
    icon = Icons.Default.Add,
    enabled = false,
    contentDescription = "Add (disabled)"
)
```

**Parameters:**
- `onClick: () -> Unit` - Click handler
- `icon: ImageVector` - Icon to display
- `modifier: Modifier = Modifier` - Optional modifier
- `enabled: Boolean = true` - Whether button is interactive
- `loading: Boolean = false` - Whether to show loading spinner
- `contentDescription: String? = null` - Accessibility description

---

## Design Specifications

All buttons follow the exact specifications from `UI_REDESIGN_MASTER_PLAN.md`:

### Colors
- **Primary Gradient:** `#F59E0B` (amber) → `#F97316` (orange)
- **Text Color:** `#18181B` (ink)
- **Border Color:** `#E4E4E7` (zinc-200)

### Dimensions
- **Height:** 56dp (Primary & Secondary)
- **Diameter:** 56dp (FAB)
- **Border Radius:** 16dp
- **Horizontal Padding:** 24dp
- **Vertical Padding:** 20dp

### Elevation
- **Primary Button:** 2dp default, 0dp when pressed
- **Secondary Button:** No elevation (outline style)
- **FAB:** 6dp default, 8dp when pressed/hovered

### Animations
- **Press Scale:** 96% (0.96f)
- **Press Alpha:** 80% (0.8f) for Primary/Secondary
- **FAB Rotation:** 2° when pressed
- **Duration:** 140ms (GlowMotion.fast)
- **Easing:** cubic-bezier(0.2, 0.8, 0.2, 1)

---

## Best Practices

### When to Use Each Button

**GlowPrimaryButton:**
- Primary actions (e.g., "Continue", "Submit", "Save")
- Account creation/login CTAs
- High-priority actions that drive user flow

**GlowSecondaryButton:**
- Secondary actions (e.g., "Cancel", "Skip", "Back")
- Alternative authentication methods
- Lower-priority actions

**GlowFloatingActionButton:**
- Create/Add actions (e.g., "Add product", "New entry")
- Quick access to primary creation flows
- Position at bottom-right with 16dp margin

### Layout Examples

**Welcome Screen:**
```kotlin
Column(
    modifier = Modifier
        .fillMaxWidth()
        .padding(24.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp)
) {
    GlowPrimaryButton(
        text = "Continue with Google",
        onClick = { /* Google auth */ },
        icon = Icons.Default.Star
    )
    
    GlowSecondaryButton(
        text = "Continue with email",
        onClick = { /* Email auth */ }
    )
}
```

**Routine Screen with FAB:**
```kotlin
Scaffold(
    floatingActionButton = {
        GlowFloatingActionButton(
            onClick = { /* Add product */ },
            icon = Icons.Default.Add,
            contentDescription = "Add product"
        )
    },
    floatingActionButtonPosition = FabPosition.End
) { padding ->
    // Screen content
}
```

---

## Accessibility

All buttons support:
- ✅ Screen readers (contentDescription)
- ✅ Reduced motion (respects system preference)
- ✅ Disabled state semantics
- ✅ Haptic feedback
- ✅ Minimum touch target size (48dp)
- ✅ High contrast (WCAG compliant colors)

---

## Preview

The file includes comprehensive preview composables:
- `GlowButtonsPreviewLight` - Light theme preview
- `GlowButtonsPreviewDark` - Dark theme preview

Run these in Android Studio's preview pane to see all button states.

---

## Migration from Existing Buttons

If you're migrating from the existing `GlowButton`:

**Old:**
```kotlin
GlowButton(
    text = "Continue",
    onClick = {},
    variant = GlowButtonVariant.Primary
)
```

**New:**
```kotlin
GlowPrimaryButton(
    text = "Continue",
    onClick = {}
)
```

**Changes:**
- `variant` parameter removed (use specific button component)
- Gradient instead of solid honey500 color
- New colors match master plan specifications
- Enhanced animations and elevations
- Better loading/disabled states

---

## Technical Notes

### Dependencies
- Jetpack Compose (Material 3)
- Material Icons
- GlowUp AI Design System (Colors, Spacing, Shapes, Motion)

### Performance
- All animations respect reduced motion preferences
- Efficient recomposition with `remember` and `derivedStateOf`
- No unnecessary allocations in hot paths

### Testing
Test all button states in your screens:
1. Normal state
2. Pressed state (animation)
3. Loading state
4. Disabled state
5. With icons
6. Without icons
7. Long text wrapping
8. Accessibility mode

---

## Questions?

See `UI_REDESIGN_MASTER_PLAN.md` for design rationale and full specifications.
