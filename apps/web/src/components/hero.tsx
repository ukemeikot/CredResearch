"use client";

import Link from "next/link";
import { motion } from "framer-motion";
import { ArrowRight, BookOpen, ShieldCheck, Users } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Globe } from "@/components/globe";
import { AppMockup } from "@/components/app-mockup";

const CHIPS = [
  { icon: ShieldCheck, label: "AI-use disclosure" },
  { icon: Users, label: "Supervisor-reviewed" },
  { icon: BookOpen, label: "APA · IEEE · Harvard" },
];

export function Hero() {
  return (
    <section className="relative overflow-hidden px-6 pb-20 pt-32">
      {/* Soft animated globe, behind the mockup */}
      <motion.div
        aria-hidden
        initial={{ scale: 0.85, opacity: 0 }}
        animate={{ scale: 1, opacity: 1 }}
        transition={{ duration: 1.4, ease: "easeOut" }}
        className="animate-float pointer-events-none absolute -right-24 top-24 -z-[1] w-[min(70vw,460px)] opacity-70 lg:opacity-100"
      >
        <Globe />
      </motion.div>

      <div className="mx-auto grid max-w-6xl items-center gap-14 lg:grid-cols-[1.05fr_1fr]">
        {/* Copy */}
        <div>
          <motion.p
            initial={{ opacity: 0, y: 12 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: 0.15 }}
            className="eyebrow"
          >
            For African universities &amp; researchers
          </motion.p>

          <motion.h1
            initial={{ opacity: 0, y: 18 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: 0.28, duration: 0.7 }}
            className="mt-4 font-display text-4xl font-bold leading-[1.05] text-slate-900 sm:text-5xl md:text-6xl"
          >
            Research with integrity,
            <br />
            <span className="text-accent">from idea to submission.</span>
          </motion.h1>

          <motion.p
            initial={{ opacity: 0, y: 16 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: 0.42 }}
            className="mt-6 max-w-xl text-base leading-relaxed text-slate-600"
          >
            CredResearch guides students from a first idea to a structured, supervisor-reviewed,
            citation-supported thesis — with an AI assistant that helps you write, never ghostwrites.
            Every AI interaction is recorded in a tamper-evident disclosure ledger.
          </motion.p>

          <motion.div
            initial={{ opacity: 0, y: 16 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: 0.55 }}
            className="mt-8 flex flex-wrap items-center gap-4"
          >
            <Link href="/register">
              <Button size="lg">
                Start free <ArrowRight size={18} />
              </Button>
            </Link>
            <Link href="#platform">
              <Button variant="outline" size="lg">
                See how it works
              </Button>
            </Link>
          </motion.div>

          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            transition={{ delay: 0.75 }}
            className="mt-8 flex flex-wrap gap-x-5 gap-y-2"
          >
            {CHIPS.map((c) => (
              <span key={c.label} className="inline-flex items-center gap-1.5 text-xs font-medium text-slate-500">
                <c.icon size={14} className="text-accent" /> {c.label}
              </span>
            ))}
          </motion.div>
        </div>

        {/* Product preview */}
        <div className="relative">
          <AppMockup />
        </div>
      </div>
    </section>
  );
}
