# New Components Integration Guide

This guide explains how to use the newly created components in the GlowUp Android app.

## 1. TrendChart Component

### Purpose
Display metric trends over time with interactive line charts powered by Vico.

### Location
`/app/src/main/java/com/glowup/ai/core/ui/TrendChart.kt`

### Usage
```kotlin
@Composable
fun MyScreen() {
    val dataPoints = listOf(
        TrendDataPoint(
            timestamp = "2026-08-01T12:00:00Z",
            value = 45.5f,
            label = "Good"
        ),
        // ... more points
    )
    
    TrendChart(
        title = "Redness Score",
        dataPoints = dataPoints,
        metricLabel = "Redness",
        subtitle = "Last 30 days",
        showTrend = true,
        onDataPointClick = { point ->
            // Handle tap on data point
            Log.d("Chart", "Tapped: ${point.value}")
        }
    )
}
```

### Parameters
- `title: String` - Chart title (e.g., "Redness Score")
- `dataPoints: List<TrendDataPoint>` - Data to plot
- `metricLabel: String` - Label for metric (shown in trend indicator)
- `subtitle: String?` - Optional subtitle (e.g., "Last 30 days")
- `showTrend: Boolean` - Show/hide trend indicator
- `onDataPointClick: ((TrendDataPoint) -> Unit)?` - Optional tap handler

### Features
- Automatic trend calculation (up/down/stable)
- Percentage change display
- Zoomable charts
- Date formatting on X-axis
- Responsive to GlowTheme colors

---

## 2. MilestoneDialog Component

### Purpose
Celebrate streak milestones with confetti animation and progress tracking.

### Location
`/app/src/main/java/com/glowup/ai/core/ui/MilestoneDialog.kt`

### Usage
```kotlin
@Composable
fun MyScreen() {
    var showMilestoneDialog by remember { mutableStateOf(false) }
    
    // Trigger when user reaches milestone
    LaunchedEffect(currentStreak) {
        if (currentStreak in listOf(7, 14, 30, 60, 90)) {
            showMilestoneDialog = true
        }
    }
    
    if (showMilestoneDialog) {
        MilestoneDialog(
            milestone = 30,
            message = "You've tracked your skin consistently for a month! Keep it up!",
            onDismiss = { showMilestoneDialog = false },
            onShare = {
                // Share to social media
                shareProgress()
            }
        )
    }
}
```

### Parameters
- `milestone: Int` - Number of days achieved (e.g., 7, 30, 90)
- `message: String` - Congratulatory message
- `onDismiss: () -> Unit` - Called when user dismisses dialog
- `onShare: (() -> Unit)?` - Optional share handler

### Features
- Animated confetti canvas
- Respects reduced motion settings
- Shows next milestone progress
- Share button (optional)
- Scales based on milestone importance

---

## 3. MilestoneProgressCard Component

### Purpose
Show progress toward next streak milestone on Home screen.

### Location
`/app/src/main/java/com/glowup/ai/core/ui/MilestoneDialog.kt`

### Usage
```kotlin
@Composable
fun HomeScreen() {
    LazyColumn {
        item {
            MilestoneProgressCard(
                currentStreak = 23,
                nextMilestone = 30
            )
        }
    }
}
```

### Parameters
- `currentStreak: Int` - Current days in streak
- `nextMilestone: Int` - Next target milestone

### Features
- Visual progress bar
- Days remaining count
- Automatic color theming
- Responsive layout

### Milestone Tiers
- 3 days (Getting Started)
- 7 days (Week Warrior)
- 14 days (Two Weeks Strong)
- 30 days (Monthly Master)
- 60 days (Two Months)
- 90 days (Quarterly Champion)
- 180 days (Half Year)
- 365 days (Year Long)

---

## 4. InsightsEnhancedScreen

### Purpose
Show comprehensive metric trends, summaries, and product recommendations.

### Location
`/app/src/main/java/com/glowup/ai/feature/insights/InsightsEnhancedScreen.kt`

### Integration Steps

#### Step 1: Add to GlowDestination
```kotlin
// In GlowDestination.kt
@Serializable data object InsightsEnhanced : GlowDestination
```

#### Step 2: Add to Navigation Graph
```kotlin
// In GlowNavGraph.kt
composable<GlowDestination.InsightsEnhanced> {
    InsightsEnhancedRoute(
        onNavigateBack = { navController.popBackStack() },
        onNavigateToRoutine = { 
            navController.navigate(GlowDestination.Routine) 
        }
    )
}
```

#### Step 3: Navigate to Screen
```kotlin
// From any screen
Button(onClick = { 
    navController.navigate(GlowDestination.InsightsEnhanced) 
}) {
    Text("View Insights")
}
```

### Features
- Metric selector (Redness, Texture, Blemishes, Dark Spots)
- Time range filter (7/14/30/90 days)
- Trend charts with Vico
- Weekly/monthly summaries
- Smart product recommendations
- Pull-to-refresh support
- Error handling

### ViewModel Access
```kotlin
@HiltViewModel
class MyViewModel @Inject constructor(
    private val captureRepository: CaptureRepository
) : ViewModel() {
    // Access capture history
    // Generate insights
}
```

---

## 5. Enhanced Achievement System

### New Achievement: BEFORE_AFTER

#### Location
`/app/src/main/java/com/glowup/ai/domain/model/Achievement.kt`

#### Details
- **ID**: `before_after`
- **Title**: "Before & After"
- **Description**: "Used comparison mode to track progress"
- **Tier**: Silver
- **Icon**: `Icons.Filled.CompareArrows`
- **Requirement**: `AchievementRequirement.UsedComparison`

#### Integration
```kotlin
// In comparison screen ViewModel
fun onComparisonViewed() {
    // Mark comparison as used
    achievementRepository.checkAchievement(AchievementType.BEFORE_AFTER)
}
```

#### Backend Integration
```kotlin
// Server should track comparison usage
POST /api/user-achievements
{
    "achievement_id": "before_after",
    "unlocked_at": "2026-08-31T12:00:00Z"
}
```

---

## Design System Integration

All components follow GlowUp design system:

### Colors
```kotlin
val colors = LocalGlowColors.current
colors.honey500    // Primary accent
colors.honey700    // Dark accent
colors.ink900      // Primary text
colors.ink600      // Secondary text
colors.softGreen   // Success/positive
colors.danger      // Error/negative
```

### Spacing
```kotlin
import com.glowup.ai.core.design.GlowSpacing

padding(GlowSpacing.xs)   // 4dp
padding(GlowSpacing.sm)   // 8dp
padding(GlowSpacing.md)   // 16dp
padding(GlowSpacing.lg)   // 24dp
padding(GlowSpacing.xl)   // 32dp
```

### Typography
```kotlin
Text(
    text = "Title",
    style = MaterialTheme.typography.titleLarge,
    fontWeight = FontWeight.Bold
)

Text(
    text = "Body",
    style = MaterialTheme.typography.bodyMedium
)
```

### Shapes
```kotlin
import com.glowup.ai.core.design.GlowShapes

Card(shape = GlowShapes.sm)  // 8dp
Card(shape = GlowShapes.md)  // 16dp
Card(shape = GlowShapes.lg)  // 24dp
```

---

## Accessibility

All components support:

### Reduced Motion
```kotlin
val reducedMotion = isReducedMotionEnabled()

if (!reducedMotion) {
    // Show animations
} else {
    // Static display
}
```

### Screen Readers
```kotlin
modifier = Modifier.semantics {
    contentDescription = "Detailed description for screen readers"
}
```

### Touch Targets
```kotlin
// Minimum 48dp touch targets
Button(
    modifier = Modifier.heightIn(min = 48.dp)
)
```

---

## Testing

### Unit Tests
```kotlin
@Test
fun `milestone progress calculates correctly`() {
    val progress = calculateProgress(23, 30)
    assertEquals(0.767f, progress, 0.01f)
}
```

### UI Tests
```kotlin
@Test
fun `trend chart displays correctly`() {
    composeTestRule.setContent {
        TrendChart(
            title = "Test Chart",
            dataPoints = testData,
            metricLabel = "Test"
        )
    }
    
    composeTestRule.onNodeWithText("Test Chart").assertIsDisplayed()
}
```

### Screenshot Tests
Use Paparazzi or similar for visual regression testing.

---

## Performance Considerations

### TrendChart
- Uses `derivedStateOf` to avoid recomposition
- Lazy loads data points
- Efficient date parsing with caching

### MilestoneDialog
- Conditionally renders confetti only if motion enabled
- Uses `remember` for static data
- Cleans up animations on dismiss

### InsightsEnhanced
- Fetches data once on init
- Caches metric calculations
- Uses StateFlow for reactive updates

---

## Common Issues & Solutions

### Issue: Chart not showing data
**Solution**: Ensure timestamps are valid ISO 8601 format
```kotlin
val timestamp = Instant.now().toString() // ✓ Correct
val timestamp = "2026-08-31" // ✗ Incorrect
```

### Issue: Milestone dialog appears too often
**Solution**: Track shown milestones to avoid duplicates
```kotlin
val shownMilestones = remember { mutableSetOf<Int>() }

if (currentStreak == 30 && !shownMilestones.contains(30)) {
    showDialog = true
    shownMilestones.add(30)
}
```

### Issue: Colors don't match design
**Solution**: Always use `LocalGlowColors.current`
```kotlin
// ✓ Correct
val colors = LocalGlowColors.current
color = colors.honey500

// ✗ Incorrect
color = Color(0xFFFFA500)
```

---

## Next Steps

1. **Add to Navigation**: Integrate InsightsEnhanced screen
2. **Backend Integration**: Connect to real capture history API
3. **Analytics**: Track component usage events
4. **A/B Testing**: Test different milestone thresholds
5. **Localization**: Add string resources for i18n

---

## Resources

- [Vico Charts Documentation](https://patrykandpatrick.com/vico/)
- [Material 3 Guidelines](https://m3.material.io/)
- [Compose Animation](https://developer.android.com/jetpack/compose/animation)
- [Accessibility Testing](https://developer.android.com/guide/topics/ui/accessibility/testing)

---

## Support

For issues or questions:
1. Check inline code documentation
2. Review existing similar components
3. Reference `IMPLEMENTATION_SUMMARY.md`
4. Ask the team in #android-dev channel

Happy coding! 🚀
