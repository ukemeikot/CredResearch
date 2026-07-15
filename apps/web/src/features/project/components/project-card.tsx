"use client";

import { motion } from "framer-motion";
import { GlassCard } from "@/components/ui/glass-card";
import { type ProjectSummary } from "@/lib/api";
import { STATUS_COLOR } from "../model/project-status";

export function ProjectCard({ project, index = 0 }: { project: ProjectSummary; index?: number }) {
  return (
    <motion.div
      initial={{ opacity: 0, y: 20 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ delay: index * 0.06 }}
    >
      <GlassCard interactive className="flex h-full flex-col gap-4 p-5">
        <div className="flex items-start justify-between gap-3">
          <h3 className="font-display text-base font-semibold text-white">{project.title}</h3>
          <span className="shrink-0 rounded-full border px-2 py-0.5 text-[10px] uppercase tracking-wider text-slate-400">
            {project.level}
          </span>
        </div>
        <div className="mt-auto">
          <span
            className={`inline-flex rounded-full border px-3 py-1 text-xs font-medium ${
              STATUS_COLOR[project.status] ?? "text-slate-300 border-white/15"
            }`}
          >
            {project.status.replace(/_/g, " ")}
          </span>
        </div>
      </GlassCard>
    </motion.div>
  );
}
