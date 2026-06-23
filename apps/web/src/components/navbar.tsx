"use client";

import Link from "next/link";
import { motion } from "framer-motion";
import { useEffect, useState } from "react";
import { Search } from "lucide-react";
import { Logo } from "@/components/ui/logo";
import { Button } from "@/components/ui/button";
import { cn } from "@/lib/cn";

const LINKS = [
  { label: "Platform", href: "#platform" },
  { label: "Workflow", href: "#workflow" },
  { label: "Integrity", href: "#integrity" },
  { label: "Pricing", href: "#pricing" },
];

export function Navbar() {
  const [scrolled, setScrolled] = useState(false);

  useEffect(() => {
    const onScroll = () => setScrolled(window.scrollY > 24);
    onScroll();
    window.addEventListener("scroll", onScroll, { passive: true });
    return () => window.removeEventListener("scroll", onScroll);
  }, []);

  return (
    <motion.header
      initial={{ y: -28, opacity: 0 }}
      animate={{ y: 0, opacity: 1 }}
      transition={{ duration: 0.6, ease: "easeOut" }}
      className={cn(
        "fixed inset-x-0 top-0 z-50 transition-all duration-300",
        scrolled ? "border-b border-white/10 bg-cosmos-950/70 backdrop-blur-xl" : "bg-transparent",
      )}
    >
      <nav className="mx-auto flex h-20 max-w-6xl items-center justify-between px-6">
        <Link href="/">
          <Logo />
        </Link>

        <ul className="hidden items-center gap-9 md:flex">
          {LINKS.map((l) => (
            <li key={l.href}>
              <Link
                href={l.href}
                className="group relative text-sm font-medium uppercase tracking-wider text-slate-300 transition-colors hover:text-white"
              >
                {l.label}
                <span className="absolute -bottom-1.5 left-0 h-px w-0 bg-accent transition-all duration-300 group-hover:w-full" />
              </Link>
            </li>
          ))}
        </ul>

        <div className="flex items-center gap-4">
          <button
            aria-label="Search"
            className="grid h-9 w-9 place-items-center rounded-full border border-white/10 text-slate-300 transition-colors hover:border-accent/50 hover:text-accent"
          >
            <Search size={16} />
          </button>
          <Link href="/login" className="hidden sm:block">
            <Button variant="outline" size="sm">
              Sign in
            </Button>
          </Link>
        </div>
      </nav>
    </motion.header>
  );
}
