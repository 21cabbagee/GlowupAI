"use client";

/**
 * Demo component showing animated icon usage.
 * This demonstrates:
 * 1. AnimatedRefreshIcon - rotates 360° on click
 * 2. NotificationBadge - pulses when has notifications
 * 3. PressableIcon - scales to 0.9 on press
 *
 * Import and use in any page:
 * import { AnimatedRefreshIcon, NotificationBadge, PressableIcon } from "@/components/animated-icons";
 */

import { useState } from "react";
import { Card, CardTitle } from "./ui";
import {
  AnimatedRefreshIcon,
  NotificationBadge,
  PressableIcon,
} from "./animated-icons";
import { HomeIcon, AccountIcon } from "./icons";

export function AnimatedIconsDemo() {
  const [refreshCount, setRefreshCount] = useState(0);
  const [notificationCount, setNotificationCount] = useState(3);

  const handleRefresh = () => {
    setRefreshCount((c) => c + 1);
    // Simulate data refresh
    console.log("Refreshing data...");
  };

  const handleNotificationClick = () => {
    setNotificationCount(0);
    console.log("Opening notifications...");
  };

  const handleIconPress = () => {
    console.log("Icon pressed!");
  };

  return (
    <Card className="mt-4">
      <CardTitle>Animated Icons Demo</CardTitle>

      <div className="space-y-6">
        {/* Refresh Icon Demo */}
        <div>
          <p className="mb-2 text-[13px] font-semibold">
            1. Refresh Icon (rotates 360° on click)
          </p>
          <div className="flex items-center gap-4">
            <AnimatedRefreshIcon
              onClick={handleRefresh}
              aria-label="Refresh data"
            />
            <span className="text-[13px] text-muted">
              Clicked {refreshCount} times
            </span>
          </div>
        </div>

        {/* Notification Badge Demo */}
        <div>
          <p className="mb-2 text-[13px] font-semibold">
            2. Notification Badge (pulses when has notifications)
          </p>
          <div className="flex items-center gap-4">
            <NotificationBadge
              count={notificationCount}
              onClick={handleNotificationClick}
              aria-label="View notifications"
            />
            <span className="text-[13px] text-muted">
              {notificationCount > 0
                ? `${notificationCount} unread notifications`
                : "No notifications"}
            </span>
            <button
              onClick={() => setNotificationCount((c) => c + 1)}
              className="rounded-[var(--radius)] border border-line px-3 py-1.5 text-[12px] font-semibold hover:bg-surface-2"
            >
              Add notification
            </button>
          </div>
        </div>

        {/* Pressable Icon Demo */}
        <div>
          <p className="mb-2 text-[13px] font-semibold">
            3. Pressable Icons (scale to 0.9 on press)
          </p>
          <div className="flex items-center gap-2">
            <PressableIcon onClick={handleIconPress} aria-label="Home">
              <HomeIcon />
            </PressableIcon>
            <PressableIcon onClick={handleIconPress} aria-label="Account">
              <AccountIcon />
            </PressableIcon>
          </div>
        </div>

        {/* Code Example */}
        <div className="rounded-[var(--radius)] border border-line bg-surface-2 p-4">
          <p className="mb-2 text-[12px] font-bold">Usage Example:</p>
          <pre className="overflow-x-auto text-[11px] text-muted">
            {`import {
  AnimatedRefreshIcon,
  NotificationBadge,
  PressableIcon,
} from "@/components/animated-icons";

// 1. Refresh icon
<AnimatedRefreshIcon
  onClick={handleRefresh}
  aria-label="Refresh data"
/>

// 2. Notification badge
<NotificationBadge
  count={3}
  onClick={handleNotifications}
/>

// 3. Pressable icon wrapper
<PressableIcon onClick={handleClick}>
  <SomeIcon />
</PressableIcon>`}
          </pre>
        </div>
      </div>
    </Card>
  );
}
