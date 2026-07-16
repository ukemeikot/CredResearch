"use client";

import { useEffect, useState } from "react";
import { createPortal } from "react-dom";

/**
 * Renders children into document.body so fixed-position overlays (modals, drawers) anchor to the
 * viewport — not to a transformed ancestor (framer-motion elements set `transform`, which makes
 * `position: fixed` resolve against that ancestor instead of the viewport).
 */
export function Portal({ children }: { children: React.ReactNode }) {
  const [mounted, setMounted] = useState(false);
  useEffect(() => setMounted(true), []);
  if (!mounted) return null;
  return createPortal(children, document.body);
}
