"use client";

import { motion } from "motion/react";
import { useState } from "react";
import { BellIcon, RefreshIcon, type IconProps } from "./icons";

/**
 * Animated icon components with interactive feedback.
 * - AnimatedRefreshIcon: rotates 360° on click
 * - NotificationBadge: pulses when hasNotifications is true
 * - PressableIcon: scales to 0.9 on press for any icon
 */

interface AnimatedRefreshIconProps extends IconProps {
  onClick?: () => void;
  "aria-label"?: string;
}

/**
 * Refresh icon that rotates 360° on click.
 * Usage: <AnimatedRefreshIcon onClick={handleRefresh} aria-label="Refresh data" />
 */
export function AnimatedRefreshIcon({
  onClick,
  className,
  size,
  "aria-label": ariaLabel = "Refresh",
}: AnimatedRefreshIconProps) {
  const [isSpinning, setIsSpinning] = useState(false);

  const handleClick = () => {
    if (isSpinning) return;
    setIsSpinning(true);
    onClick?.();
    // Reset after animation completes
    setTimeout(() => setIsSpinning(false), 600);
  };

  return (
    <button
      onClick={handleClick}
      aria-label={ariaLabel}
      className="grid place-items-center rounded-[var(--radius-full)] p-2 text-muted transition-colors hover:bg-surface-2 hover:text-fg active:scale-90"
      disabled={isSpinning}
    >
      <motion.span
        animate={{ rotate: isSpinning ? 360 : 0 }}
        transition={{
          duration: 0.6,
          ease: [0.2, 0.8, 0.2, 1],
        }}
        className="block"
      >
        <RefreshIcon size={size} className={className} />
      </motion.span>
    </button>
  );
}

interface NotificationBadgeProps {
  count?: number;
  hasNotifications?: boolean;
  size?: "sm" | "md";
  className?: string;
  children?: React.ReactNode;
  onClick?: () => void;
  "aria-label"?: string;
}

/**
 * Notification badge with pulsing animation when has notifications.
 * Usage:
 * <NotificationBadge count={3} hasNotifications onClick={handleClick}>
 *   <BellIcon />
 * </NotificationBadge>
 */
export function NotificationBadge({
  count = 0,
  hasNotifications = count > 0,
  size = "md",
  className,
  children,
  onClick,
  "aria-label": ariaLabel = "Notifications",
}: NotificationBadgeProps) {
  const sizes = {
    sm: { icon: 18, badge: "size-4 text-[9px]" },
    md: { icon: 20, badge: "size-5 text-[10px]" },
  };

  const content = (
    <span className="relative inline-block">
      {children || <BellIcon size={sizes[size].icon} />}
      {hasNotifications && (
        <motion.span
          animate={{
            scale: [1, 1.15, 1],
            opacity: [1, 0.8, 1],
          }}
          transition={{
            duration: 2,
            repeat: Infinity,
            ease: "easeInOut",
          }}
          className={`absolute -top-1 -right-1 grid ${sizes[size].badge} place-items-center rounded-full bg-investigate text-paper font-bold`}
          aria-hidden="true"
        >
          {count > 0 && count < 100 ? count : count >= 100 ? "99+" : ""}
        </motion.span>
      )}
    </span>
  );

  if (onClick) {
    return (
      <button
        onClick={onClick}
        aria-label={
          hasNotifications
            ? `${ariaLabel} (${count} unread)`
            : ariaLabel
        }
        className={`grid place-items-center rounded-[var(--radius-full)] p-2 text-muted transition-colors hover:bg-surface-2 hover:text-fg active:scale-90 ${className ?? ""}`}
      >
        {content}
      </button>
    );
  }

  return <span className={className}>{content}</span>;
}

interface PressableIconProps {
  children: React.ReactNode;
  onClick?: () => void;
  className?: string;
  "aria-label"?: string;
  disabled?: boolean;
}

/**
 * Wrapper that adds scale-to-0.9 press animation to any icon.
 * Usage:
 * <PressableIcon onClick={handleClick} aria-label="Action">
 *   <SomeIcon />
 * </PressableIcon>
 */
export function PressableIcon({
  children,
  onClick,
  className,
  "aria-label": ariaLabel,
  disabled = false,
}: PressableIconProps) {
  return (
    <motion.button
      whileTap={{ scale: 0.9 }}
      onClick={onClick}
      aria-label={ariaLabel}
      disabled={disabled}
      transition={{ duration: 0.1 }}
      className={`grid place-items-center rounded-[var(--radius-full)] p-2 text-muted transition-colors hover:bg-surface-2 hover:text-fg disabled:pointer-events-none disabled:opacity-45 ${className ?? ""}`}
    >
      {children}
    </motion.button>
  );
}
