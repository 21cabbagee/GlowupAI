# Micro-Interactions Documentation

**Last Updated:** August 31, 2026  
**Author:** Claude (GlowUp AI Enhancement Agent)

## Overview

This document catalogs all micro-interactions added to GlowUp AI to create a more delightful and responsive user experience. All animations follow Material Motion principles and respect accessibility settings (reduced motion).

## Design Principles

1. **Snappy Timing:** All animations are 140-220ms for quick, responsive feel
2. **Material Motion Easing:** Using cubic-bezier(0.2, 0.8, 0.2, 1) for natural movement
3. **Accessibility First:** All animations respect `Settings.Global.ANIMATOR_DURATION_SCALE` (reduced motion)
4. **Subtle is Better:** Animations enhance, never distract
5. **Haptic Feedback:** Strategic use of haptic feedback for important interactions

## Animation Utilities

### Core Functions (`AnimationUtils.kt`)

#### `rememberPulseAnimation()`
Creates breathing/pulse effect for living elements.

```kotlin
val pulse = rememberPulseAnimation(
    enabled = true,
    minScale = 0.95f,
    maxScale = 1.05f,
    durationMillis = 1500
)
```

**Use Cases:**
- Streak flame icon
- Active indicators
- "Pay attention to me" elements

---

#### `rememberShakeAnimation()`
Creates shake effect for warnings or errors.

```kotlin
val shake = rememberShakeAnimation(trigger = showWarning)
// Returns horizontal offset value
```

**Use Cases:**
- Streak at-risk warning
- Form validation errors
- Attention-grabbing alerts

---

#### `rememberShineAnimation()`
Creates sweep shine effect for celebrating achievements.

```kotlin
val shine = rememberShineAnimation(trigger = isUnlocked)
// Returns position value 0-1 for gradient
```

**Use Cases:**
- Unlocked achievement badges
- Completed milestones
- New content reveals

---

#### `rememberCelebrationAnimation()`
Full celebration with scale + rotation for milestone events.

```kotlin
val celebration = rememberCelebrationAnimation(trigger = completed)
// Returns: CelebrationState(isAnimating, scale, rotation)
```

**Use Cases:**
- Freeze day used
- Major milestones reached
- Important achievements

---

#### `rememberFadeInAnimation()`
Staggered fade-in for list items and grids.

```kotlin
val alpha = rememberFadeInAnimation(delay = index * 50)
```

**Use Cases:**
- Calendar grid cells
- Achievement grid
- List item reveals

---

#### `rememberHighlightAnimation()`
Pulsing highlight for important elements.

```kotlin
val highlight = rememberHighlightAnimation(enabled = isToday)
```

**Use Cases:**
- "Today" indicator in calendar
- Current selection
- Active states

---

#### `rememberProgressAnimation()`
Smooth animated progress bar updates.

```kotlin
val progress = rememberProgressAnimation(
    targetProgress = 0.75f,
    durationMillis = 600
)
```

**Use Cases:**
- Achievement progress bars
- Experiment progress
- Loading states

---

#### `rememberBounceAnimation()`
Quick bounce for successful actions.

```kotlin
val bounce = rememberBounceAnimation(trigger = saved)
```

**Use Cases:**
- Save confirmations
- Item added feedback
- Success states

---

#### `Modifier.animatedClickable()`
Drop-in replacement for `clickable()` with scale animation and haptic feedback.

```kotlin
Modifier.animatedClickable(
    enableHaptic = true,
    onClick = { /* action */ }
)
```

---

## Component Enhancements

### 1. StreakCounter

**File:** `app/src/main/java/com/glowup/ai/core/ui/StreakCounter.kt`

#### Animations Added:

1. **Flame Pulse Animation**
   - Breathing effect on flame icon when streak > 0
   - Scale range: 0.97f to 1.03f
   - Duration: 2000ms
   - Creates "living flame" effect

2. **Shake Animation**
   - Triggered when `showWarning = true`
   - Horizontal shake: -15px to +15px
   - Duration: 600ms
   - Draws attention to at-risk streak

3. **Celebration Animation**
   - Triggered when freeze day button is used
   - Scale: 1.0f to 1.2f
   - Rotation: 0° to 360°
   - Includes haptic feedback (LONG_PRESS)

#### CompactStreakIndicator:
- Subtle pulse on flame icon (0.98f to 1.02f)
- `animatedClickable()` for press scale + haptic

**Before/After:**
- **Before:** Static flame, no feedback
- **After:** Living flame with breathing, shake warning, celebration on freeze

---

### 2. AchievementCard

**File:** `app/src/main/java/com/glowup/ai/core/ui/AchievementCard.kt`

#### Animations Added:

1. **Shine Effect**
   - Diagonal sweep shine on unlock
   - White gradient overlay (alpha 0.3)
   - Triggered by `achievement.isNew && achievement.isUnlocked`
   - Creates premium "reveal" moment

2. **Animated Progress Bar**
   - Smooth animation from 0 to current progress
   - Duration: 600ms
   - Updates respond to progress changes
   - No jarring jumps

3. **Bounce on Unlock**
   - Spring animation (medium bouncy)
   - Scale: 1.0f to 1.1f
   - Makes unlock feel impactful

#### AchievementCelebration Dialog:
- Respects reduced motion
- Scale pulse: 0.8f to 1.2f
- Rotation sway: -10° to +10°
- Creates excitement without nausea

**Before/After:**
- **Before:** Static card, instant progress updates
- **After:** Shine effect on unlock, smooth progress animation, satisfying unlock moment

---

### 3. CalendarHeatmap

**File:** `app/src/main/java/com/glowup/ai/core/ui/CalendarHeatmap.kt`

#### Animations Added:

1. **Staggered Fade-In**
   - Each cell fades in sequentially
   - Delay: `day * 15ms`
   - Creates wave effect across calendar
   - Alpha: 0f to 1f over 300ms

2. **Today Highlight Pulse**
   - Pulsing border on today's cell
   - Alpha: 0.7f to 1.0f
   - Duration: 1000ms
   - Subtle scale: 0.95f to 1.0f
   - Makes today easy to spot

3. **Ripple Effect on Tap**
   - Scale animation on press (via `animatedClickable()`)
   - Haptic feedback on tap
   - Target scale: 0.96f

**Before/After:**
- **Before:** Calendar pops in all at once, today blends in
- **After:** Elegant wave reveal, today pulses gently, satisfying tap feedback

---

### 4. GlowButton

**File:** `app/src/main/java/com/glowup/ai/core/ui/GlowButton.kt`

#### Animations Added:

1. **Press Scale Animation**
   - Scale: 1.0f to 0.96f on press
   - Duration: 140ms (fast)
   - Spring-back on release
   - Makes buttons feel "pressable"

2. **Haptic Feedback**
   - `KEYBOARD_TAP` on press
   - Only when enabled and not loading
   - Confirms interaction

3. **Loading State**
   - Smooth fade between text and spinner
   - Width remains stable (no layout shift)

**Before/After:**
- **Before:** Static button, no press feedback
- **After:** Satisfying press down, haptic confirmation, polished loading state

---

### 5. Loading States

**File:** `app/src/main/java/com/glowup/ai/core/ui/ShimmerSkeleton.kt`

#### Existing (Enhanced):
- Already had shimmer animation
- Now respects `rememberReducedMotion()`
- Falls back to static tinted block when reduced motion enabled
- Duration: 1100ms
- Smooth sweep using GlowMotion easing

**No changes needed** - already excellent!

---

## Haptic Feedback Strategy

### When to Use Haptic:

| Constant | Use Case | Component |
|----------|----------|-----------|
| `KEYBOARD_TAP` | Standard button press | GlowButton, animatedClickable |
| `CLICK` | Card selection, list item tap | Calendar cells (future) |
| `LONG_PRESS` | Major milestone, celebration | Freeze day, achievement unlock |
| `CONFIRM` | Success action | Bounce animation |

### When NOT to Use Haptic:
- Rapid repeated taps (e.g., scrolling)
- Background state changes
- Timer/polling updates
- Non-user-initiated changes

---

## Accessibility Considerations

### Reduced Motion Support

All animations check `rememberReducedMotion()` which reads:
```kotlin
Settings.Global.ANIMATOR_DURATION_SCALE
```

When reduced motion is enabled:
- Pulse animations → static
- Fade-in animations → instant
- Shine effects → disabled
- Scale animations → snap to final state
- Shake animations → no offset

### Testing Reduced Motion

```bash
# Enable reduced motion
adb shell settings put global animator_duration_scale 0

# Disable reduced motion (default)
adb shell settings put global animator_duration_scale 1
```

---

## Performance Considerations

### Memoization
- Calendar calculations memoized (see PERFORMANCE_OPTIMIZATIONS.md §2.3)
- Animation states use `remember { }` to avoid recomposition
- `rememberInfiniteTransition` for looping animations

### Animation Cost
- All animations use `graphicsLayer` (hardware accelerated)
- No layout changes during animation (stable layout)
- Compose handles frame scheduling

### Best Practices Followed:
1. ✅ Avoid `Modifier.offset()` → use `graphicsLayer { translationX }`
2. ✅ Avoid `Modifier.rotation()` → use `graphicsLayer { rotationZ }`
3. ✅ Avoid `Modifier.size()` animations → use `scale`
4. ✅ Single `graphicsLayer` per modifier chain
5. ✅ Haptic feedback wrapped in `LaunchedEffect` (not recomposition)

---

## Testing Checklist

### Manual Testing:

- [ ] Test all animations on physical device (emulator haptics are fake)
- [ ] Enable reduced motion → verify graceful degradation
- [ ] Test on low-end device (e.g., API 26 minimum)
- [ ] Test in dark mode (ensure animations visible)
- [ ] Test rapid interactions (no animation stutter)
- [ ] Test with TalkBack enabled (animations don't break semantics)

### Specific Component Tests:

#### StreakCounter:
- [ ] Flame pulses when streak > 0
- [ ] Flame static when streak = 0
- [ ] Card shakes on warning
- [ ] Freeze button celebrates on tap

#### AchievementCard:
- [ ] Shine effect plays once on unlock
- [ ] Progress bar animates smoothly (0 → 100%)
- [ ] Locked cards have correct alpha

#### CalendarHeatmap:
- [ ] Cells fade in sequentially (wave effect)
- [ ] Today cell pulses
- [ ] Tapping cell has haptic feedback
- [ ] Future dates don't pulse

#### GlowButton:
- [ ] All variants scale on press
- [ ] Haptic feedback on tap
- [ ] Loading spinner smooth transition
- [ ] Disabled state has no animation

---

## Inspiration Sources

### Apps Referenced:
1. **Duolingo** - Streak mechanics, celebration animations
2. **Headspace** - Breathing animations, calming pulses
3. **Strava** - Achievement unlocks, progress animations
4. **Things 3** - Smooth completion animations
5. **Apollo for Reddit** - Haptic feedback excellence

### Design Systems:
- **Material Motion** - Easing curves, timing
- **Apple HIG** - Haptic feedback guidelines
- **Stripe iOS** - Button press animations

---

## Future Enhancements

### Potential Additions (Not Implemented):

1. **Confetti Animation**
   - On major milestones (30 day streak, first experiment complete)
   - Use Canvas for particle effects
   - Consider performance impact

2. **Drag-to-Reorder**
   - For routine products
   - Haptic feedback on drop
   - Spring animation to final position

3. **Pull-to-Refresh**
   - Custom animation with GlowUp branding
   - Honey drop animation?

4. **Photo Capture Feedback**
   - Shutter animation
   - Flash effect
   - Success checkmark

5. **Swipe Gestures**
   - Swipe-to-delete with undo
   - Swipe between photos
   - Haptic at 50% threshold

---

## Code Examples

### Adding Pulse to Any Icon:

```kotlin
val pulse = rememberPulseAnimation(
    enabled = isActive,
    minScale = 0.97f,
    maxScale = 1.03f,
    durationMillis = 1500
)

Icon(
    imageVector = Icons.Default.Star,
    contentDescription = null,
    modifier = Modifier.scale(pulse)
)
```

### Adding Shine Effect:

```kotlin
val shine = rememberShineAnimation(trigger = isNew)

Box(
    modifier = Modifier
        .drawWithContent {
            drawContent()
            if (shine >= 0f) {
                val shineGradient = Brush.linearGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color.White.copy(alpha = 0.3f),
                        Color.Transparent
                    ),
                    start = Offset(shine * size.width - size.width, 0f),
                    end = Offset(shine * size.width, size.height)
                )
                drawRect(brush = shineGradient)
            }
        }
) {
    // Content
}
```

### Adding Shake to Any Component:

```kotlin
val shake = rememberShakeAnimation(trigger = hasError)

Card(
    modifier = Modifier.graphicsLayer {
        translationX = shake
    }
) {
    // Content
}
```

---

## Maintenance Notes

### When Adding New Animations:

1. **Always check reduced motion**
   ```kotlin
   val reducedMotion = rememberReducedMotion()
   if (reducedMotion) return 1f // or snap to final state
   ```

2. **Use GlowMotion timing**
   ```kotlin
   animationSpec = GlowMotion.respectingReducedMotion(
       GlowMotion.fast, // or .standard, .slow
       reducedMotion
   )
   ```

3. **Add to this document**
   - Document the animation
   - Explain when it triggers
   - Note accessibility considerations

4. **Test on device**
   - Emulator haptics are fake
   - Low-end devices show perf issues
   - Dark mode may need color adjustments

### Common Pitfalls:

❌ **Don't:**
- Animate layout properties (width, height, padding)
- Use `remember { }` without keys for state-dependent values
- Ignore reduced motion
- Add haptic to every interaction
- Chain multiple animations without `graphicsLayer`

✅ **Do:**
- Use `graphicsLayer` for scale, rotation, translation
- Memoize with proper keys
- Respect reduced motion
- Use haptic strategically
- Batch transformations in single `graphicsLayer`

---

## Questions or Issues?

If animations behave unexpectedly:

1. Check reduced motion setting on device
2. Verify animation keys in `remember { }` are correct
3. Look for recomposition storms (use Layout Inspector)
4. Test on physical device (emulator is not reliable for perf)
5. Check for conflicting modifiers (multiple scales, transforms)

For performance issues:
- Profile with Android Studio Profiler
- Check frame timing in `adb shell dumpsys gfxinfo`
- Look for >16ms frames (60fps target)

---

## Conclusion

These micro-interactions make GlowUp AI feel **alive, responsive, and delightful** without compromising accessibility or performance. Every animation has a purpose, follows design system standards, and degrades gracefully.

The result is an app that feels as good as it looks.

**Shipped with:** All animations respect reduced motion, use efficient hardware-accelerated transforms, and follow Material Motion timing.

**Next steps:** Test on real devices, gather user feedback, iterate on timing/intensity as needed.

---

*Generated by Claude - August 31, 2026*
*Part of GlowUp AI v1.0 Enhancement Sprint*
