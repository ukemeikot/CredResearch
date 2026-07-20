"use client";

import { motion } from "framer-motion";
import { FileText, MessageSquare, ShieldCheck, Sparkles } from "lucide-react";

/** A lightweight, self-contained product preview (fake app window) for the landing hero. */
export function AppMockup() {
  return (
    <motion.div
      initial={{ opacity: 0, y: 24 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.8, ease: "easeOut", delay: 0.2 }}
      className="relative w-full rounded-2xl border border-slate-200 bg-white shadow-card"
    >
      {/* Window chrome */}
      <div className="flex items-center gap-1.5 border-b border-slate-100 px-4 py-3">
        <span className="h-2.5 w-2.5 rounded-full bg-rose-300" />
        <span className="h-2.5 w-2.5 rounded-full bg-amber-300" />
        <span className="h-2.5 w-2.5 rounded-full bg-emerald-300" />
        <span className="ml-3 rounded-md bg-slate-100 px-2 py-0.5 text-[10px] text-slate-500">
          Chapter 1 — Introduction
        </span>
      </div>

      <div className="grid grid-cols-[92px_1fr] gap-0">
        {/* Section nav */}
        <div className="space-y-1.5 border-r border-slate-100 p-3">
          {["1 Introduction", "2 Literature", "3 Methodology", "4 Results"].map((s, i) => (
            <div
              key={s}
              className={`rounded-md px-2 py-1 text-[9px] ${i === 0 ? "bg-accent/10 text-accent" : "text-slate-400"}`}
            >
              {s}
            </div>
          ))}
        </div>

        {/* Editor body */}
        <div className="p-4">
          <div className="flex items-center gap-2 text-[10px] text-slate-400">
            <FileText size={11} /> Background of the study
          </div>
          <div className="mt-2 space-y-1.5">
            <div className="h-2 w-11/12 rounded bg-slate-100" />
            <div className="h-2 w-full rounded bg-slate-100" />
            {/* highlighted AI-inserted line */}
            <div className="h-2 w-10/12 rounded bg-accent/25" />
            <div className="h-2 w-9/12 rounded bg-slate-100" />
          </div>

          {/* AI assist chip */}
          <div className="mt-3 inline-flex items-center gap-1 rounded-full border border-accent/30 bg-accent/5 px-2 py-1 text-[9px] text-accent">
            <Sparkles size={10} /> AI drafted this passage
          </div>

          {/* Floating badges */}
          <div className="mt-4 flex flex-wrap gap-2">
            <span className="inline-flex items-center gap-1 rounded-lg border border-slate-200 px-2 py-1 text-[9px] text-slate-600">
              <ShieldCheck size={10} className="text-emerald-500" /> Logged to disclosure ledger
            </span>
            <span className="inline-flex items-center gap-1 rounded-lg border border-slate-200 px-2 py-1 text-[9px] text-slate-600">
              <MessageSquare size={10} className="text-accent" /> Supervisor comment
            </span>
          </div>
        </div>
      </div>
    </motion.div>
  );
}
