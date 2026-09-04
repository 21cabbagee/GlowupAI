# Animated Icons - Quick Reference

## Import
```tsx
import {
  AnimatedRefreshIcon,
  NotificationBadge,
  PressableIcon,
} from "@/components/animated-icons";

import { RefreshIcon, BellIcon } from "@/components/icons";
```

## Components

### AnimatedRefreshIcon
Rotates 360° on click
```tsx
<AnimatedRefreshIcon
  onClick={handleRefresh}
  aria-label="Refresh data"
  size={20}
/>
```

### NotificationBadge
Pulses when has notifications
```tsx
<NotificationBadge
  count={3}
  onClick={handleClick}
  size="sm" // or "md"
/>

// Custom icon
<NotificationBadge count={5}>
  <CustomIcon />
</NotificationBadge>
```

### PressableIcon
Scales to 0.9 on press (generic wrapper)
```tsx
<PressableIcon onClick={handleClick} aria-label="Action">
  <AnyIcon />
</PressableIcon>
```

## New Icons

### RefreshIcon
```tsx
<RefreshIcon size={20} />
```

### BellIcon
```tsx
<BellIcon size={20} />
```

## Animation Details

| Feature | Spec |
|---------|------|
| Refresh rotation | 360° in 0.6s |
| Notification pulse | 2s infinite loop |
| Icon press | Scale to 0.9 |
| Button press | Scale to 0.9 (global) |

## Live Examples

- **Home page** (`/home`): Refresh button in header
- **Shell** (`components/shell.tsx`): Notification badge example
- **Demo** (`components/animated-icons-demo.tsx`): All animations

## Files

- **Components**: `components/animated-icons.tsx`
- **Demo**: `components/animated-icons-demo.tsx`
- **Full docs**: `components/ANIMATED_ICONS_README.md`
- **Icons**: `components/icons.tsx`
