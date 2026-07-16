"use client";

import Link from "next/link";
import { motion } from "framer-motion";
import { ArrowRight } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Globe } from "@/components/globe";

export function Hero() {
  return (
    <section className="relative flex min-h-[100svh] items-center justify-center overflow-hidden px-6 pt-28 pb-16">
      {/* Floating planet */}
      <motion.div
        aria-hidden
        initial={{ scale: 0.85, opacity: 0 }}
        animate={{ scale: 1, opacity: 1 }}
        transition={{ duration: 1.4, ease: "easeOut" }}
        className="animate-float pointer-events-none absolute top-1/2 left-1/2 -z-[1] w-[min(78vw,540px)] -translate-x-1/2 -translate-y-1/2"
      >
        <Globe />
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
          className="mt-5 font-display text-5xl leading-[0.95] sm:text-7xl md:text-8xl"
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
