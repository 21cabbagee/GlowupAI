# Animated Icons Guide

This guide explains the animated icon components available in the GlowupAI web app.

## Overview

Three animated icon components have been added to enhance user interaction:

1. **AnimatedRefreshIcon** - Rotates 360° on click
2. **NotificationBadge** - Pulses when has notifications
3. **PressableIcon** - Generic wrapper that scales to 0.9 on press

All components are built with `motion/react` (Framer Motion) and follow the existing design system.

## Components

### 1. AnimatedRefreshIcon

Displays a refresh icon that rotates 360° when clicked. Perfect for data refresh actions.

**Props:**
- `onClick?: () => void` - Handler called when clicked
- `aria-label?: string` - Accessibility label (default: "Refresh")
- `className?: string` - Additional CSS classes
- `size?: number` - Icon size in pixels

**Animation:**
- Rotation: 360° over 0.6 seconds
- Easing: Custom cubic-bezier [0.2, 0.8, 0.2, 1]
- Press scale: 0.9
- Disabled during animation to prevent multiple triggers

**Example:**
```tsx
import { AnimatedRefreshIcon } from "@/components/animated-icons";

<AnimatedRefreshIcon
  onClick={handleRefresh}
  aria-label="Refresh dashboard"
/>
```

**Live Example:**
- Home page (`/home`) - Added to PageHeading action area

---

### 2. NotificationBadge

Displays a notification icon with an animated badge that pulses when there are unread notifications.

**Props:**
- `count?: number` - Number of notifications (default: 0)
- `hasNotifications?: boolean` - Whether to show badge (default: count > 0)
- `size?: "sm" | "md"` - Size variant (default: "md")
- `className?: string` - Additional CSS classes
- `children?: React.ReactNode` - Custom icon (defaults to BellIcon)
- `onClick?: () => void` - Handler for clicking the badge
- `aria-label?: string` - Accessibility label (default: "Notifications")

**Animation:**
- Pulse: Scale from 1 → 1.15 → 1
- Opacity: 1 → 0.8 → 1
- Duration: 2 seconds, infinite loop
- Easing: easeInOut

**Badge Display:**
- Shows count if 1-99
- Shows "99+" if count >= 100
- Shows empty badge if hasNotifications but no count

**Example:**
```tsx
import { NotificationBadge } from "@/components/animated-icons";

// With count
<NotificationBadge
  count={3}
  onClick={handleNotifications}
/>

// Custom icon
<NotificationBadge
  count={5}
  onClick={handleNotifications}
>
  <CustomIcon />
</NotificationBadge>
```

**Live Example:**
- Mobile header (`shell.tsx`) - Commented example ready to connect to real data

---

### 3. PressableIcon

Generic wrapper that adds press animation (scale to 0.9) to any icon.

**Props:**
- `children: React.ReactNode` - Icon or content to wrap
- `onClick?: () => void` - Click handler
- `className?: string` - Additional CSS classes
- `aria-label?: string` - Accessibility label
- `disabled?: boolean` - Whether button is disabled (default: false)

**Animation:**
- Press scale: 0.9
- Duration: 0.1 seconds
- Uses Framer Motion's whileTap

**Example:**
```tsx
import { PressableIcon } from "@/components/animated-icons";
import { HomeIcon } from "@/components/icons";

<PressableIcon onClick={handleClick} aria-label="Go home">
  <HomeIcon />
</PressableIcon>
```

---

## Global Icon Press Animation

All interactive icons throughout the app now scale to 0.9 on press:

**Updated components:**
- **Button component** (`ui.tsx`): `active:scale-90`
- **Capture FAB** (`shell.tsx`): `active:scale-[0.9]`
- **Tab icons** (via Button base class)

---

## Available Icons

New icons added to `components/icons.tsx`:

### RefreshIcon
Circular arrow icon for refresh actions.
```tsx
import { RefreshIcon } from "@/components/icons";
<RefreshIcon size={20} />
```

### BellIcon
Bell icon for notifications.
```tsx
import { BellIcon } from "@/components/icons";
<BellIcon size={20} />
```

---

## Demo Component

A complete demo component is available at `components/animated-icons-demo.tsx` showing all three animated components in action with interactive examples.

**Usage:**
```tsx
import { AnimatedIconsDemo } from "@/components/animated-icons-demo";

export default function MyPage() {
  return (
    <div>
      <AnimatedIconsDemo />
    </div>
  );
}
```

---

## Implementation Examples

### Example 1: Data Refresh Button
```tsx
import { AnimatedRefreshIcon } from "@/components/animated-icons";

function MyComponent() {
  const [data, setData] = useState(null);

  const loadData = async () => {
    const result = await api.fetchData();
    setData(result);
  };

  return (
    <div>
      <h1>My Data</h1>
      <AnimatedRefreshIcon onClick={loadData} />
      {/* ... render data ... */}
    </div>
  );
}
```

### Example 2: Notification System
```tsx
import { NotificationBadge } from "@/components/animated-icons";

function Header() {
  const [notifications, setNotifications] = useState([]);

  const handleNotificationClick = () => {
    // Open notifications panel
    router.push('/notifications');
  };

  return (
    <header>
      <NotificationBadge
        count={notifications.length}
        onClick={handleNotificationClick}
        size="sm"
      />
    </header>
  );
}
```

### Example 3: Interactive Icon Buttons
```tsx
import { PressableIcon } from "@/components/animated-icons";
import { HomeIcon, AccountIcon } from "@/components/icons";

function Navigation() {
  return (
    <nav>
      <PressableIcon onClick={() => router.push('/home')} aria-label="Home">
        <HomeIcon />
      </PressableIcon>
      <PressableIcon onClick={() => router.push('/account')} aria-label="Account">
        <AccountIcon />
      </PressableIcon>
    </nav>
  );
}
```

---

## Animation Specifications

All animations follow the design system:

- **Duration**: Fast interactions (0.1-0.6s)
- **Easing**: Custom cubic-bezier `[0.2, 0.8, 0.2, 1]` or built-in easeInOut
- **Scale on press**: 0.9 (uniform across all interactive icons)
- **Respects reduced motion**: Handled by Framer Motion automatically

---

## Accessibility

All animated components include proper accessibility features:

- `aria-label` attributes for screen readers
- Keyboard navigation support (button elements)
- Disabled states when appropriate
- Badge counts announced to screen readers
- Semantic HTML (button elements for clickable icons)

---

## Files Modified/Created

**Created:**
- `/components/animated-icons.tsx` - Main animated components
- `/components/animated-icons-demo.tsx` - Demo component
- `/components/ANIMATED_ICONS_README.md` - This documentation

**Modified:**
- `/components/icons.tsx` - Added RefreshIcon and BellIcon
- `/components/ui.tsx` - Updated button scale to 0.9
- `/components/shell.tsx` - Added NotificationBadge import and example usage
- `/app/(product)/home/page.tsx` - Added AnimatedRefreshIcon to page heading

---

## Next Steps

To use these components in your pages:

1. Import the component you need from `@/components/animated-icons`
2. Import any icons you need from `@/components/icons`
3. Add the component with appropriate props and handlers
4. Test the animation and accessibility

For questions or issues, refer to the demo component or existing implementations.
