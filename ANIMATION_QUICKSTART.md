# Animation Quick Start Guide

Get up and running with GlowUp AI animations in 5 minutes.

## 1. Navigation Transitions

**For any new screen**, add these 4 lines to your `composable()`:

```kotlin
import com.glowup.ai.core.ui.NavigationAnimations

NavHost(navController) {
    composable<YourScreen>(
        enterTransition = { NavigationAnimations.enterTransition() },
        exitTransition = { NavigationAnimations.exitTransition() },
        popEnterTransition = { NavigationAnimations.popEnterTransition() },
        popExitTransition = { NavigationAnimations.popExitTransition() }
    ) {
        YourScreenContent()
    }
}
```

**For tab switches**, use lighter fade:
```kotlin
enterTransition = { NavigationAnimations.tabEnterTransition() },
exitTransition = { NavigationAnimations.tabExitTransition() }
```

---

## 2. Buttons

**Already handled!** All `GlowButton` components have press animation + haptic built-in.

```kotlin
GlowButton(
    text = "Continue",
    onClick = { /* your action */ }
)
// Press animation is automatic ✅
```

For custom clickables:
```kotlin
Box(
    modifier = Modifier.animatedClickable(
        onClick = { /* action */ }
    )
) {
    // Your content
}
```

---

## 3. Lists

**For LazyColumn/LazyRow**, wrap items with `AnimatedListItem`:

```kotlin
import com.glowup.ai.core.ui.AnimatedListItem

LazyColumn {
    itemsIndexed(products) { index, product ->
        AnimatedListItem(index = index) {
            ProductCard(product = product)
        }
    }
}
```

**Lightweight alternative** (just fade):
```kotlin
itemsIndexed(items) { index, item ->
    Box(modifier = Modifier.listItemFadeIn(index)) {
        ItemContent(item)
    }
}
```

---

## 4. Loading States

**Use shimmer skeletons** for loading:

```kotlin
import com.glowup.ai.core.ui.HomeScreenShimmer

when (state) {
    is Loading -> HomeScreenShimmer()
    is Content -> ActualContent(state.data)
}
```

**Pre-built skeletons**:
- `HomeScreenShimmer()`
- `CardShimmer(height = 120.dp)`
- `TextShimmer(lines = 3)`
- `CircleShimmer(size = 48.dp)`
- `PhotoShimmer()`

---

## 5. Success & Error States

**Show errors** with shake animation:
```kotlin
import com.glowup.ai.core.ui.ErrorState

ErrorState(
    message = "Couldn't load data",
    onRetry = { retry() }
)
```

**Show success** with bounce:
```kotlin
import com.glowup.ai.core.ui.SuccessState

if (saveSuccess) {
    SuccessState(
        message = "Routine saved!",
        dismissLabel = "Got it",
        onDismiss = { saveSuccess = false }
    )
}
```

**Inline success indicator**:
```kotlin
SuccessIndicator(message = "Changes saved")
```

---

## 6. Expandable Content

**Create accordion sections**:

```kotlin
import com.glowup.ai.core.ui.ExpandableSection

ExpandableSection(
    title = "Advanced Options",
    initiallyExpanded = false
) {
    // Content that can be toggled
    Text("Hidden by default...")
}
```

**Controlled expansion**:
```kotlin
ExpandableSection(
    title = "Section",
    expanded = isExpanded,
    onExpandChange = { isExpanded = it }
) {
    // Your content
}
```

---

## Common Patterns

### Error Handling
```kotlin
when (result) {
    is Error -> ErrorState(
        message = result.message,
        onRetry = { retry() }
    )
    is Success -> SuccessState(
        message = "Done!",
        onDismiss = { }
    )
}
```

### Form Submission
```kotlin
Column {
    GlowTextField(...)
    
    GlowButton(
        text = "Save",
        loading = isSubmitting,
        onClick = { submit() }
    )
    
    if (submitSuccess) {
        SuccessIndicator("Saved")
    }
}
```

### Settings Screen
```kotlin
LazyColumn {
    item {
        ExpandableSection(
            title = "Account",
            initiallyExpanded = true
        ) {
            AccountSettings()
        }
    }
    item {
        ExpandableSection(
            title = "Privacy",
            initiallyExpanded = false
        ) {
            PrivacySettings()
        }
    }
}
```

---

## Testing Reduced Motion

**Enable in Android settings:**
```
Settings → Accessibility → Remove animations (ON)
```

**Or via ADB:**
```bash
adb shell settings put global animator_duration_scale 0
```

**All animations become instant** while maintaining functionality.

---

## Performance Tips

1. **Don't animate layout** - Use `graphicsLayer` for transforms
2. **Limit concurrent animations** - Max 3-4 at once  
3. **Use hardware layers** - `Modifier.graphicsLayer { }`
4. **Check reduced motion** - Already handled by utilities

---

## Decision Tree

**Need to animate something?**

- **Screen transition?** → `NavigationAnimations`
- **Button press?** → Use `GlowButton` (built-in)
- **List items?** → `AnimatedListItem`
- **Loading?** → Shimmer skeletons
- **Error?** → `ErrorState`
- **Success?** → `SuccessState`
- **Toggle content?** → `ExpandableSection`
- **Custom animation?** → Check `AnimationUtils.kt`

---

## Full Documentation

See **[ANIMATION_GUIDE.md](/ANIMATION_GUIDE.md)** for:
- Complete animation catalog
- Timing references
- Advanced patterns
- Performance guidelines
- Accessibility details

---

## Questions?

**Common issues:**

**Q: Animation feels too slow**  
A: Check if you're using the right timing:
- Touch feedback: 140ms (fast)
- Most transitions: 180ms (standard)  
- Navigation: 220ms (slow)

**Q: Animation not running**  
A: Check reduced motion setting. Animations become instant (expected behavior).

**Q: List items all appear at once**  
A: Use `AnimatedListItem(index)` to get staggered appearance.

**Q: Want different stagger delay**  
A: `AnimatedListItem(index, staggerDelayMs = 30)`

---

**Ready to animate!** 🎨✨

Start with navigation transitions, then add list animations, then polish with success/error states.
