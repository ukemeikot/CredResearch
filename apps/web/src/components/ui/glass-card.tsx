"use client";

import { motion, type HTMLMotionProps } from "framer-motion";
import { cn } from "@/lib/cn";

interface GlassCardProps extends HTMLMotionProps<"div"> {
  interactive?: boolean;
}

export function GlassCard({ interactive, className, children, ...props }: GlassCardProps) {
  return (
    <motion.div
      whileHover={interactive ? { y: -6 } : undefined}
      transition={{ type: "spring", stiffness: 300, damping: 24 }}
      className={cn(
        "glass rounded-2xl shadow-card",
        interactive && "glass-hover cursor-pointer",
        className,
      )}
      {...props}
    >
      {children}
    </motion.div>
  );
}
