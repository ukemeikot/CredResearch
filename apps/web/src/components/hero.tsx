"use client";

import Link from "next/link";
import { motion } from "framer-motion";
import { ArrowRight } from "lucide-react";
import { Button } from "@/components/ui/button";

export function Hero() {
  return (
    <section className="relative flex min-h-[100svh] items-center justify-center overflow-hidden px-6 pt-24">
      {/* Floating cosmic orb */}
      <motion.div
        aria-hidden
        initial={{ scale: 0.85, opacity: 0 }}
        animate={{ scale: 1, opacity: 1 }}
        transition={{ duration: 1.4, ease: "easeOut" }}
        className="pointer-events-none absolute top-1/2 left-1/2 -z-[1] h-[clamp(320px,46vw,640px)] w-[clamp(320px,46vw,640px)] -translate-x-1/2 -translate-y-1/2"
      >
        <div className="absolute inset-0 animate-float rounded-full bg-[radial-gradient(circle_at_32%_28%,#3a4b8f_0%,#141a3e_45%,#0a0e27_72%)] shadow-[0_0_120px_-10px_rgba(45,226,255,0.5)]" />
        <div className="absolute inset-0 rounded-full ring-1 ring-white/10" />
        <div className="absolute right-[12%] top-[14%] h-3 w-3 animate-pulse-glow rounded-full bg-accent" />
        <div className="absolute inset-[-6%] animate-spin-slow rounded-full border border-dashed border-white/10" />
      </motion.div>

      <div className="relative mx-auto max-w-3xl text-center">
        <motion.p
          initial={{ opacity: 0, y: 12 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.2 }}
          className="eyebrow"
        >
          AI-Powered Research Platform
        </motion.p>

        <motion.h1
          initial={{ opacity: 0, y: 18 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.32, duration: 0.7 }}
          className="mt-5 font-display text-6xl leading-[0.95] sm:text-7xl md:text-8xl"
        >
          <span className="font-bold text-white text-glow">RESEARCH!</span>
          <br />
          <span className="display-thin text-white/90">REVOLUTION</span>
        </motion.h1>

        <motion.div
          initial={{ opacity: 0, y: 16 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.5 }}
          className="mt-9 flex items-center justify-center gap-4"
        >
          <Link href="/register">
            <Button size="lg">
              Get started <ArrowRight size={18} />
            </Button>
          </Link>
          <Link href="#workflow">
            <Button variant="outline" size="lg">
              See the workflow
            </Button>
          </Link>
        </motion.div>

        <motion.div
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          transition={{ delay: 0.8 }}
          className="mx-auto mt-12 max-w-sm text-left"
        >
          <h3 className="font-display text-lg font-semibold uppercase tracking-wider text-white">
            Guided Writing
          </h3>
          <p className="mt-2 text-sm leading-relaxed text-slate-400">
            From idea to a structured, supervisor-reviewed, citation-supported document — with a
            transparent record of how AI was used. Built for low-bandwidth, real-world research.
          </p>
        </motion.div>
      </div>
    </section>
  );
}
