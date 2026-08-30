# GlowUp AI Animation Guide

A comprehensive guide to animations in GlowUp AI. Every animation communicates state, never decorates.

## Table of Contents

- [Principles](#principles)
- [Timing & Easing](#timing--easing)
- [Animation Catalog](#animation-catalog)
  - [Navigation Transitions](#navigation-transitions)
  - [Button & Touch Feedback](#button--touch-feedback)
  - [List Animations](#list-animations)
  - [Loading States](#loading-states)
  - [Success & Error States](#success--error-states)
  - [Expandable Content](#expandable-content)
  - [Micro-interactions](#micro-interactions)
- [Accessibility](#accessibility)
- [Performance](#performance)
- [Code Examples](#code-examples)

---

## Principles

**1. Purpose Over Decoration**
Every animation must communicate something:
- State changes (loading → content)
- User feedback (button press)
- Relationships (parent-child hierarchy)
- Attention (error states)

**2. Respect Reduced Motion**
All animations honor the system's "Remove animations" setting. When reduced motion is enabled:
- Animations become instant (snap)
- State changes still occur
- Information hierarchy remains clear

**3. Consistent Timing**
- **Fast (140ms)**: Touch feedback, micro-interactions
- **Standard (180ms)**: Most transitions, state changes
- **Slow (220ms)**: Navigation, complex transitions

**4. 60fps Target**
All animations must maintain 60fps on mid-range devices. No jank.

---

## Timing & Easing

### Motion Specs

```kotlin
GlowMotion.fast      // 140ms
GlowMotion.standard  // 180ms
GlowMotion.slow      // 220ms
```

### Easing Curve

Custom cubic-bezier: `cubic-bezier(0.2, 0.8, 0.2, 1)`

```kotlin
val easing = CubicBezierEasing(0.2f, 0.8f, 0.2f, 1f)
```

This easing creates a natural, Material-like motion that:
- Starts quickly (responsive feel)
- Eases smoothly into the end state
- Feels organic and intentional

---

## Animation Catalog

### Navigation Transitions

#### Forward Navigation
**Duration**: 220ms  
**Effect**: Slide in from right + fade

```kotlin
import com.glowup.ai.core.ui.NavigationAnimations

NavHost(navController = navController) {
    composable<Destination>(
        enterTransition = { NavigationAnimations.enterTransition() },
        exitTransition = { NavigationAnimations.exitTransition() },
        popEnterTransition = { NavigationAnimations.popEnterTransition() },
        popExitTransition = { NavigationAnimations.popExitTransition() }
    ) {
        // Screen content
    }
}
```

**Types:**
- **Forward**: Slide in from right (25% offset) + fade
- **Back**: Slide out to right (25% offset) + fade
- **Tab switch**: Fade only (180ms)
- **Modal**: Slide up from bottom (33% offset) + fade

#### When to Use
- Forward: Navigating deeper into hierarchy
- Back: Returning to previous screen
- Tab: Bottom nav switches
- Modal: Overlays, bottom sheets

---

### Button & Touch Feedback

#### Press Scale
**Duration**: 140ms  
**Effect**: Scale to 96% on press + haptic feedback

```kotlin
GlowButton(
    text = "Continue",
    onClick = { /* action */ }
)
// Press animation is built-in
```

**Details:**
- Scales to 96% when pressed
- Returns to 100% on release
- Haptic feedback on press (KEYBOARD_TAP)
- Respects reduced motion (instant snap)

#### Custom Clickable with Animation
```kotlin
Box(
    modifier = Modifier.animatedClickable(
        onClick = { /* action */ }
    )
) {
    // Content
}
```

---

### List Animations

#### Staggered Fade-In
**Duration**: 220ms per item  
**Stagger**: 50ms between items

```kotlin
LazyColumn {
    itemsIndexed(items) { index, item ->
        AnimatedListItem(index = index) {
            ItemCard(item = item)
        }
    }
}
```

**Effect**: Items appear with:
- Fade in (0 → 100% opacity)
- Slide up (25% offset)
- Staggered by 50ms per item

#### Simple Fade Modifier
```kotlin
Box(
    modifier = Modifier.listItemFadeIn(index = index)
) {
    // Item content
}
```

#### Scale + Fade
```kotlin
Box(
    modifier = Modifier.listItemScaleIn(index = index)
) {
    // Item content with subtle pop
}
```

#### Item Removal
```kotlin
AnimatedListItemRemoval(visible = !item.isDeleted) {
    ItemCard(item = item)
}
```

**When to Use:**
- Staggered: Initial list load, page transitions
- Simple fade: Lightweight lists, frequent updates
- Scale: High-engagement lists (achievements, products)
- Removal: Delete operations, filtering

---

### Loading States

#### Shimmer Skeleton
**Duration**: 1100ms loop  
**Effect**: Gradient sweep

```kotlin
ShimmerSkeleton(
    height = 120.dp,
    cornerRadius = 16.dp
)
```

**Pre-built Skeletons:**
```kotlin
HomeScreenShimmer()          // Full home screen
CardShimmer(height = 120.dp) // Single card
TextShimmer(lines = 3)       // Text lines
CircleShimmer(size = 48.dp)  // Avatar/icon
PhotoShimmer()               // Image placeholder
```

**When to Use:**
- Initial data load
- Pagination loading
- Lazy image loading

**Don't:**
- Loop indefinitely without feedback (max 10 seconds)
- Show shimmer for <200ms operations (use instant content)

---

### Success & Error States

#### Error with Shake
**Duration**: 600ms  
**Effect**: Horizontal shake + fade in

```kotlin
ErrorState(
    message = "Couldn't load data",
    onRetry = { /* retry action */ }
)
```

**Animation:**
- Shakes horizontally on appearance (-15px → 15px → 0)
- Draws attention to error
- Only shakes once per appearance

#### Success with Bounce
**Duration**: 400ms  
**Effect**: Scale bounce + fade in

```kotlin
SuccessState(
    message = "Routine saved!",
    dismissLabel = "Got it",
    onDismiss = { /* dismiss */ }
)
```

**Animation:**
- Scales: 0.8 → 1.0 with medium bounce
- Spring physics feel natural
- Celebrates successful action

#### Success Indicator (Inline)
```kotlin
SuccessIndicator(
    message = "Changes saved"
)
```

**When to Use:**
- Error: Failed operations, validation errors
- Success: Form submissions, data saves
- Indicator: Inline confirmations, autosave feedback

---

### Expandable Content

#### Accordion Section
**Duration**: 220ms expand, 180ms collapse  
**Effect**: Vertical expand + fade + chevron rotation

```kotlin
ExpandableSection(
    title = "Advanced Settings",
    initiallyExpanded = false
) {
    // Content that can be toggled
}
```

**Animation:**
- Chevron rotates 0° → 180°
- Content expands vertically
- Fades in/out
- Maintains smooth 60fps

#### Controlled Expansion
```kotlin
ExpandableSection(
    title = "Section",
    expanded = isExpanded,
    onExpandChange = { isExpanded = it }
) {
    // Content
}
```

**When to Use:**
- FAQs, help sections
- Settings categories
- Optional information
- Long content that should be hidden by default

---

### Micro-interactions

#### Pulse (Breathing)
**Duration**: 1500ms loop  
**Effect**: Scale oscillation (0.95 → 1.05)

```kotlin
val pulseScale = rememberPulseAnimation(enabled = true)
Icon(
    modifier = Modifier.scale(pulseScale),
    // ...
)
```

**Use for:** Flame icons, active indicators, "hot" items

#### Bounce (Success)
**Duration**: 400ms  
**Effect**: Keyframe bounce with overshoot

```kotlin
val bounceScale = rememberBounceAnimation(trigger = success)
Box(modifier = Modifier.scale(bounceScale)) {
    // Content bounces on success
}
```

**Use for:** Achievement unlocks, milestone completions

#### Shake (Warning)
**Duration**: 600ms  
**Effect**: Horizontal oscillation

```kotlin
val shakeOffset = rememberShakeAnimation(trigger = error)
Box(modifier = Modifier.offset(x = shakeOffset.dp)) {
    // Content shakes on error
}
```

**Use for:** Form validation errors, warnings

#### Highlight Pulse
**Duration**: 1000ms loop  
**Effect**: Alpha oscillation (0.7 → 1.0)

```kotlin
val highlightAlpha = rememberHighlightAnimation(enabled = true)
Box(modifier = Modifier.alpha(highlightAlpha)) {
    // "Today" indicator, active state
}
```

**Use for:** Current date, active selection, attention

#### Progress Animation
**Duration**: 600ms  
**Effect**: Smooth progress value interpolation

```kotlin
val animatedProgress = rememberProgressAnimation(
    targetProgress = 0.75f
)
LinearProgressIndicator(progress = animatedProgress)
```

---

## Accessibility

### Reduced Motion Support

All animations automatically respect the system's "Remove animations" setting:

```kotlin
val reducedMotion = rememberReducedMotion()

val spec = if (reducedMotion) {
    snap() // Instant
} else {
    tween(220, easing = GlowMotion.easing)
}
```

**What Changes:**
- All animations become instant (0ms)
- State changes still occur
- Content remains accessible
- No loss of functionality

### Testing Reduced Motion

**Android:**
```
Settings → Accessibility → Remove animations (ON)
```

**ADB:**
```bash
adb shell settings put global animator_duration_scale 0
```

---

## Performance

### Optimization Guidelines

**1. Use Composition over Recomposition**
```kotlin
// ✅ Good: animateFloatAsState caches animation
val alpha by animateFloatAsState(targetValue = if (visible) 1f else 0f)

// ❌ Bad: recalculates every frame
Modifier.alpha(if (visible) 1f else 0f)
```

**2. Avoid Expensive Operations in Animation**
```kotlin
// ❌ Bad: Layout in animation
AnimatedVisibility {
    HeavyLayoutContent()
}

// ✅ Good: Pre-compose, animate alpha/scale
Box(modifier = Modifier.alpha(alpha)) {
    HeavyLayoutContent()
}
```

**3. Use Hardware Layer for Complex Animations**
```kotlin
Modifier
    .graphicsLayer {
        translationX = offset
        scaleX = scale
        alpha = alpha
    }
```

**4. Limit Concurrent Animations**
- Max 3-4 animations simultaneously
- Stagger list items to avoid spikes
- Use lower delays for long lists (30ms vs 50ms)

### Performance Targets

- **60fps** on Pixel 4a / equivalent (mid-range)
- **Frame drops**: <5% acceptable, <2% target
- **Animation stutter**: Zero on 60Hz displays

### Profiling

Use Android Studio Profiler:
1. Record GPU rendering
2. Check for jank (missed frames)
3. Identify expensive animations
4. Optimize or simplify

---

## Code Examples

### Complete Screen with Animations

```kotlin
@Composable
fun AnimatedScreen(items: List<Item>) {
    val reducedMotion = rememberReducedMotion()
    
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header with fade
        item {
            Text(
                text = "My Items",
                modifier = Modifier.listItemFadeIn(index = 0)
            )
        }
        
        // Staggered list items
        itemsIndexed(items) { index, item ->
            AnimatedListItem(index = index + 1) {
                ItemCard(
                    item = item,
                    onClick = { /* handle */ }
                )
            }
        }
    }
}
```

### Custom Animated Component

```kotlin
@Composable
fun AnimatedCard(
    expanded: Boolean,
    onToggle: () -> Unit
) {
    val reducedMotion = rememberReducedMotion()
    
    // Smooth rotation
    val rotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = GlowMotion.respectingReducedMotion(
            GlowMotion.standard,
            reducedMotion
        ) as AnimationSpec<Float>
    )
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animatedClickable(onClick = onToggle)
    ) {
        Row {
            Text("Toggle")
            Icon(
                imageVector = Icons.Default.ExpandMore,
                modifier = Modifier.rotate(rotation),
                contentDescription = null
            )
        }
        
        AnimatedVisibility(visible = expanded) {
            Text("Expanded content")
        }
    }
}
```

### Navigation with Transitions

```kotlin
@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    
    NavHost(
        navController = navController,
        startDestination = "home"
    ) {
        composable(
            route = "home",
            enterTransition = { NavigationAnimations.tabEnterTransition() },
            exitTransition = { NavigationAnimations.tabExitTransition() }
        ) {
            HomeScreen()
        }
        
        composable(
            route = "details",
            enterTransition = { NavigationAnimations.enterTransition() },
            exitTransition = { NavigationAnimations.exitTransition() },
            popEnterTransition = { NavigationAnimations.popEnterTransition() },
            popExitTransition = { NavigationAnimations.popExitTransition() }
        ) {
            DetailsScreen()
        }
    }
}
```

---

## Animation Decision Tree

```
Need animation? 
├─ User input feedback?
│  └─ Use: pressScale, animatedClickable
│
├─ Navigation?
│  ├─ Tab switch? → NavigationAnimations.tabEnterTransition()
│  ├─ Forward? → NavigationAnimations.enterTransition()
│  └─ Modal? → NavigationAnimations.modalEnterTransition()
│
├─ Loading state?
│  ├─ >2 seconds? → ShimmerSkeleton
│  └─ <2 seconds? → None (show instantly)
│
├─ Success/Error?
│  ├─ Error → ErrorState (with shake)
│  └─ Success → SuccessState (with bounce)
│
├─ List?
│  ├─ Initial load? → AnimatedListItem (staggered)
│  ├─ Frequent updates? → listItemFadeIn
│  └─ High engagement? → listItemScaleIn
│
├─ Expand/Collapse?
│  └─ ExpandableSection
│
└─ Attention/Status?
   ├─ Active indicator → rememberPulseAnimation
   ├─ Today/Current → rememberHighlightAnimation
   └─ Achievement → rememberBounceAnimation
```

---

## Best Practices

**DO:**
- ✅ Always check reduced motion
- ✅ Use consistent timing (GlowMotion.fast/standard/slow)
- ✅ Animate for purpose, not decoration
- ✅ Test on mid-range devices
- ✅ Keep animations under 300ms
- ✅ Use haptic feedback for important interactions

**DON'T:**
- ❌ Animate layout changes (use graphicsLayer)
- ❌ Run >4 animations simultaneously
- ❌ Create custom easings (use GlowMotion.easing)
- ❌ Ignore reduced motion
- ❌ Loop indefinitely without feedback
- ❌ Animate purely for aesthetics

---

## Quick Reference

| Animation | Duration | Use Case | File |
|-----------|----------|----------|------|
| Navigation | 220ms | Screen transitions | NavigationAnimations.kt |
| Button press | 140ms | Touch feedback | GlowButton.kt |
| List stagger | 220ms + 50ms | List appear | ListAnimations.kt |
| Shimmer | 1100ms | Loading | ShimmerSkeleton.kt |
| Error shake | 600ms | Error feedback | ErrorState.kt |
| Success bounce | 400ms | Success feedback | SuccessState.kt |
| Expand/collapse | 220ms/180ms | Toggleable content | ExpandableSection.kt |
| Pulse | 1500ms | Breathing effect | AnimationUtils.kt |
| Highlight | 1000ms | Active state | AnimationUtils.kt |
| Progress | 600ms | Value change | AnimationUtils.kt |

---

## Resources

**Files:**
- `/app/src/main/java/com/glowup/ai/core/design/Motion.kt` - Motion specs
- `/app/src/main/java/com/glowup/ai/core/ui/NavigationAnimations.kt` - Navigation
- `/app/src/main/java/com/glowup/ai/core/ui/AnimationUtils.kt` - Micro-interactions
- `/app/src/main/java/com/glowup/ai/core/ui/ListAnimations.kt` - List animations
- `/app/src/main/java/com/glowup/ai/core/ui/ExpandableSection.kt` - Expandable content

**External:**
- [Material Motion](https://m3.material.io/styles/motion/overview) - Reference
- [Compose Animation](https://developer.android.com/jetpack/compose/animation) - Official docs
- [Reduced Motion](https://web.dev/prefers-reduced-motion/) - Accessibility

---

**Last Updated**: August 31, 2026  
**Maintainer**: GlowUp AI Design System Team
