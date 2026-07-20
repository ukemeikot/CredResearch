"use client";

import Link from "next/link";
import { motion } from "framer-motion";
import { Navbar } from "@/components/navbar";
import { Hero } from "@/components/hero";
import { FeatureSection } from "@/components/feature-section";
import { Button } from "@/components/ui/button";

export default function LandingPage() {
  return (
    <main className="relative">
      <Navbar />
      <Hero />

      {/* Integrity quote band */}
      <section id="integrity" className="mx-auto max-w-3xl px-6 py-24 text-center">
        <motion.div
          initial={{ opacity: 0, y: 24 }}
          whileInView={{ opacity: 1, y: 0 }}
          viewport={{ once: true }}
          transition={{ duration: 0.7 }}
        >
          <p className="eyebrow">Academic Integrity</p>
          <p className="mt-5 font-display text-3xl leading-snug text-slate-900 sm:text-4xl">
            <span className="text-accent">“</span>
            AI assists — it never ghostwrites. Every interaction is recorded in a tamper-evident
            disclosure ledger.
            <span className="text-accent">”</span>
          </p>
          <p className="mt-4 text-sm text-slate-500">
            Turning the biggest risk of generative AI into an institutional advantage.
          </p>
        </motion.div>
      </section>

      <FeatureSection />

      {/* CTA band */}
      <section id="workflow" className="mx-auto max-w-6xl px-6 py-24">
        <div className="grid items-center gap-10 md:grid-cols-2">
          <motion.div
            initial={{ opacity: 0, x: -30 }}
            whileInView={{ opacity: 1, x: 0 }}
            viewport={{ once: true }}
            transition={{ duration: 0.6 }}
            className="relative h-56 overflow-hidden rounded-2xl border border-indigo-200 bg-[radial-gradient(circle_at_30%_20%,#818cf8,#4338ca)]"
          >
            <div className="absolute inset-0 animate-gradient-pan bg-[linear-gradient(120deg,transparent,rgba(255,255,255,0.28),transparent)] bg-[length:200%_100%]" />
            <div className="absolute bottom-5 left-6 font-display text-sm uppercase tracking-[0.3em] text-white/85">
              Idea → Document
            </div>
          </motion.div>

          <motion.div
            initial={{ opacity: 0, x: 30 }}
            whileInView={{ opacity: 1, x: 0 }}
            viewport={{ once: true }}
            transition={{ duration: 0.6 }}
          >
            <h2 className="font-display text-4xl font-bold leading-tight text-slate-900">
              RESEARCH WITH INTEGRITY,
              <br />
              NOT SHORTCUTS.
            </h2>
            <p className="mt-4 max-w-md text-sm leading-relaxed text-slate-500">
              Projects, supervision, literature, citations, alignment and export — one workflow,
              built for African universities and researchers, payable in Naira.
            </p>
            <div className="mt-7 flex gap-4">
              <Link href="/register">
                <Button>Get it now</Button>
              </Link>
              <Link href="/login">
                <Button variant="outline">More details</Button>
              </Link>
            </div>
          </motion.div>
        </div>
      </section>

      <footer className="border-t border-slate-200 py-8 text-center text-xs uppercase tracking-[0.25em] text-slate-500">
        © {new Date().getFullYear()} CredResearch — All rights reserved
      </footer>
    </main>
  );
}
