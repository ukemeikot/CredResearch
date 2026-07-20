"use client";

import { motion, type HTMLMotionProps } from "framer-motion";
import { cn } from "@/lib/cn";

type Variant = "primary" | "outline" | "ghost";
type Size = "sm" | "md" | "lg";

const variants: Record<Variant, string> = {
  primary:
    "bg-accent text-white font-semibold shadow-glow hover:bg-accent-deep hover:shadow-glow-lg",
  outline:
    "border border-accent/50 text-accent hover:border-accent hover:bg-accent/10",
  ghost: "text-slate-600 hover:text-slate-900 hover:bg-slate-100",
};

const sizes: Record<Size, string> = {
  sm: "h-9 px-4 text-xs",
  md: "h-11 px-6 text-sm",
  lg: "h-14 px-8 text-base",
};

interface ButtonProps extends HTMLMotionProps<"button"> {
  variant?: Variant;
  size?: Size;
}

export function Button({
  variant = "primary",
  size = "md",
  className,
  children,
  ...props
}: ButtonProps) {
  return (
    <motion.button
      whileHover={{ scale: 1.03 }}
      whileTap={{ scale: 0.97 }}
      transition={{ type: "spring", stiffness: 400, damping: 22 }}
      className={cn(
        "inline-flex items-center justify-center gap-2 rounded-full tracking-wide transition-colors duration-200 focus:outline-none focus-visible:ring-2 focus-visible:ring-accent/70 disabled:cursor-not-allowed disabled:opacity-50",
        variants[variant],
        sizes[size],
        className,
      )}
      {...props}
    >
      {children}
    </motion.button>
  );
}
