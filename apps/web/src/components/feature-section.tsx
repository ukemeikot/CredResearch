"use client";

import { motion } from "framer-motion";
import { GsapReveal } from "@/components/gsap-reveal";

const COLUMNS = [
  {
    title: "Write & structure",
    body: "Draft your proposal and chapters in a rich editor with autosave, version history, and live co-editing — plus an AI assistant that drafts and refines sections on request.",
  },
  {
    title: "Cite & review",
    body: "Upload papers, ask questions grounded in your own sources, build a literature matrix, and export APA / IEEE / Harvard references. Invite a supervisor — even by magic link — to comment and approve.",
  },
  {
    title: "Analyze & submit",
    body: "Field a questionnaire, run descriptive analysis with grounded interpretation, check similarity, then export a submission bundle with an AI-use disclosure statement.",
  },
];

const FIGURES = [
  {
    label: "Assist",
    svg: (
      <polygon points="32,6 58,46 6,46" fill="none" stroke="currentColor" strokeWidth="1.4" />
    ),
  },
  {
    label: "Supervise",
    svg: (
      <g fill="none" stroke="currentColor" strokeWidth="1.2">
        <circle cx="32" cy="32" r="24" />
        <path d="M32 8 L52 44 L12 44 Z" />
        <path d="M32 8 L32 56 M8 32 L56 32" opacity="0.5" />
      </g>
    ),
  },
  {
    label: "Disclose",
    svg: (
      <g fill="none" stroke="currentColor" strokeWidth="1.3">
        <path d="M16 20 L32 12 L48 20 L48 40 L32 50 L16 40 Z" />
        <path d="M16 20 L32 28 L48 20 M32 28 L32 50" opacity="0.6" />
      </g>
    ),
  },
];

const reveal = {
  hidden: { opacity: 0, y: 28 },
  show: (i: number) => ({
    opacity: 1,
    y: 0,
    transition: { delay: i * 0.12, duration: 0.6, ease: "easeOut" as const },
  }),
};

export function FeatureSection() {
  return (
    <section id="platform" className="relative mx-auto max-w-5xl px-6 py-28">
      <div className="grid gap-10 md:grid-cols-3 md:divide-x md:divide-slate-200">
        {COLUMNS.map((c, i) => (
          <motion.div
            key={c.title}
            custom={i}
            variants={reveal}
            initial="hidden"
            whileInView="show"
            viewport={{ once: true, margin: "-60px" }}
            className="px-2 md:px-8"
          >
            <h3 className="font-display text-xl font-semibold uppercase tracking-[0.2em] text-accent">
              {c.title}
            </h3>
            <p className="mt-4 text-sm leading-relaxed text-slate-500">{c.body}</p>
          </motion.div>
        ))}
      </div>

      {/* GSAP-driven staggered scroll reveal for the figure row */}
      <GsapReveal className="mt-20 grid gap-10 md:grid-cols-3" stagger={0.12}>
        {FIGURES.map((f) => (
          <div key={f.label} className="group flex flex-col items-center gap-5">
            <svg
              viewBox="0 0 64 64"
              className="h-20 w-20 text-slate-400 transition-all duration-500 group-hover:text-accent group-hover:drop-shadow-[0_0_14px_rgba(99,102,241,0.55)] group-hover:[transform:rotate(8deg)]"
            >
              {f.svg}
            </svg>
            <div className="flex flex-col items-center gap-2">
              <span className="h-px w-8 bg-accent" />
              <span className="text-xs font-semibold uppercase tracking-[0.28em] text-slate-900">
                {f.label}
              </span>
            </div>
          </div>
        ))}
      </GsapReveal>
    </section>
  );
}
