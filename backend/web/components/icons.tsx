/**
 * Inline icon set — 24px grid, 1.75 stroke, currentColor.
 * Hand-rolled rather than an icon package so the weight matches the type and
 * nothing is an emoji.
 */

export type IconProps = {
  className?: string;
  size?: number;
};

function Svg({
  className,
  size = 22,
  children,
}: IconProps & { children: React.ReactNode }) {
  return (
    <svg
      viewBox="0 0 24 24"
      width={size}
      height={size}
      fill="none"
      stroke="currentColor"
      strokeWidth={1.75}
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden="true"
      className={className}
      focusable="false"
    >
      {children}
    </svg>
  );
}

export const HomeIcon = (p: IconProps) => (
  <Svg {...p}>
    <path d="M3.5 10.4 12 3.8l8.5 6.6V19a1.5 1.5 0 0 1-1.5 1.5h-3.6v-5.3H9.6v5.3H5A1.5 1.5 0 0 1 3.5 19z" />
  </Svg>
);

export const CameraIcon = (p: IconProps) => (
  <Svg {...p}>
    <path d="M4 8.5h2.4l1.3-2h8.6l1.3 2H20a1.5 1.5 0 0 1 1.5 1.5v8A1.5 1.5 0 0 1 20 19.5H4A1.5 1.5 0 0 1 2.5 18v-8A1.5 1.5 0 0 1 4 8.5Z" />
    <circle cx="12" cy="14" r="3.4" />
  </Svg>
);

export const RoutineIcon = (p: IconProps) => (
  <Svg {...p}>
    <path d="M9 3.5h6v3l2.2 3.1a4 4 0 0 1 .7 2.3v7.6A1.5 1.5 0 0 1 16.4 21H7.6a1.5 1.5 0 0 1-1.5-1.5v-7.6a4 4 0 0 1 .7-2.3L9 6.5z" />
    <path d="M6.4 14.2h11.2" />
  </Svg>
);

export const InsightIcon = (p: IconProps) => (
  <Svg {...p}>
    <path d="M21 11.5a8 8 0 0 1-11.6 7.1L4 20.5l1.5-4.6A8 8 0 1 1 21 11.5Z" />
    <path d="M9.2 11.5h.01M12.5 11.5h.01M15.8 11.5h.01" strokeWidth={2.4} />
  </Svg>
);

export const DiscoverIcon = (p: IconProps) => (
  <Svg {...p}>
    <circle cx="12" cy="12" r="8.5" />
    <path d="M15.4 8.6l-2 4.6-4.8 1.8 2-4.6z" />
  </Svg>
);

export const AccountIcon = (p: IconProps) => (
  <Svg {...p}>
    <circle cx="12" cy="8.5" r="3.8" />
    <path d="M4.8 20.2a7.4 7.4 0 0 1 14.4 0" />
  </Svg>
);

export const FlameIcon = (p: IconProps) => (
  <Svg {...p}>
    <path d="M12 21c3.6 0 6-2.4 6-5.7 0-4.4-4-5.6-4-9.3-2 .8-3.4 2.6-3.4 4.6 0 .9.3 1.6.3 2.1 0 1-.7 1.7-1.6 1.7-1 0-1.7-.8-1.8-2A6.7 6.7 0 0 0 6 15.3C6 18.6 8.4 21 12 21Z" />
  </Svg>
);

export const TrendIcon = (p: IconProps) => (
  <Svg {...p}>
    <path d="M3.5 16.5l5-5 3.5 3.5 6-6.5" />
    <path d="M14.5 8.5h4v4" />
  </Svg>
);

export const ShieldIcon = (p: IconProps) => (
  <Svg {...p}>
    <path d="M12 3.2l7 2.6v5.6c0 4.2-2.8 7.6-7 9.4-4.2-1.8-7-5.2-7-9.4V5.8z" />
    <path d="M9.2 12.1l2 2 3.6-3.9" />
  </Svg>
);

export const CheckIcon = (p: IconProps) => (
  <Svg {...p}>
    <path d="M5 12.8l4.2 4.2L19 7.2" />
  </Svg>
);

export const PlusIcon = (p: IconProps) => (
  <Svg {...p}>
    <path d="M12 5.5v13M5.5 12h13" />
  </Svg>
);

export const ChevronRightIcon = (p: IconProps) => (
  <Svg {...p}>
    <path d="M9.5 5.5l6.5 6.5-6.5 6.5" />
  </Svg>
);

export const ChevronLeftIcon = (p: IconProps) => (
  <Svg {...p}>
    <path d="M14.5 5.5L8 12l6.5 6.5" />
  </Svg>
);

export const CloseIcon = (p: IconProps) => (
  <Svg {...p}>
    <path d="M6 6l12 12M18 6L6 18" />
  </Svg>
);

export const SunIcon = (p: IconProps) => (
  <Svg {...p}>
    <circle cx="12" cy="12" r="4" />
    <path d="M12 3v2.2M12 18.8V21M3 12h2.2M18.8 12H21M5.6 5.6l1.6 1.6M16.8 16.8l1.6 1.6M18.4 5.6l-1.6 1.6M7.2 16.8l-1.6 1.6" />
  </Svg>
);

export const MoonIcon = (p: IconProps) => (
  <Svg {...p}>
    <path d="M20 14.4A8.3 8.3 0 0 1 9.6 4a8.5 8.5 0 1 0 10.4 10.4Z" />
  </Svg>
);

export const DownloadIcon = (p: IconProps) => (
  <Svg {...p}>
    <path d="M12 4v10.5M7.6 10.6L12 15l4.4-4.4M4.5 19.5h15" />
  </Svg>
);

export const TrashIcon = (p: IconProps) => (
  <Svg {...p}>
    <path d="M4.5 7.5h15M9.5 7.5V5.2h5v2.3M6.5 7.5l.9 12.1h9.2l.9-12.1" />
  </Svg>
);

export const SparkIcon = (p: IconProps) => (
  <Svg {...p}>
    <path d="M12 3.5l1.9 5.1 5.1 1.9-5.1 1.9L12 17.5l-1.9-5.1L5 10.5l5.1-1.9z" />
  </Svg>
);

export const LockIcon = (p: IconProps) => (
  <Svg {...p}>
    <rect x="4.8" y="10.5" width="14.4" height="9.7" rx="2.2" />
    <path d="M8.5 10.5V8a3.5 3.5 0 0 1 7 0v2.5" />
  </Svg>
);

export const LogoMark = ({ size = 30, className }: IconProps) => (
  <svg
    viewBox="0 0 32 32"
    width={size}
    height={size}
    aria-hidden="true"
    className={className}
    focusable="false"
  >
    {/* Squircle in honey with an ink checkmark — "measured, proven" */}
    <rect x="0" y="0" width="32" height="32" rx="9" fill="var(--honey-500)" />
    <path
      d="M8.5 16.8l4.4 4.4 10-10.4"
      fill="none"
      stroke="var(--ink-900)"
      strokeWidth="3.1"
      strokeLinecap="round"
      strokeLinejoin="round"
    />
  </svg>
);
