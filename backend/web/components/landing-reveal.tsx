"use client";

import { motion, useReducedMotion } from "motion/react";
import type { ReactNode } from "react";

const EASE: [number, number, number, number] = [0.2, 0.8, 0.2, 1];

/** A one-time, low-motion viewport entrance for the marketing landing page. */
export function LandingReveal({
  children,
  className,
  delay = 0,
}: {
  children: ReactNode;
  className?: string;
  delay?: number;
}) {
  const reducedMotion = useReducedMotion();

  return (
    <motion.div
      className={className}
      initial={reducedMotion ? false : { opacity: 0, y: 8 }}
      whileInView={reducedMotion ? undefined : { opacity: 1, y: 0 }}
      viewport={{ once: true, amount: 0.2 }}
      transition={{ duration: 0.26, delay, ease: EASE }}
    >
      {children}
    </motion.div>
  );
}