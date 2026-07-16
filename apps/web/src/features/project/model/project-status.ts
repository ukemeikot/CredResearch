import type { ProjectSummary } from "@/lib/api";

/** Statuses considered "active" for dashboard stats. */
export const ACTIVE_STATUSES = ["PROPOSAL", "IN_PROGRESS", "UNDER_REVIEW", "REVISIONS"];

/** Tailwind classes per project status (badge styling). */
export const STATUS_COLOR: Record<string, string> = {
  DRAFT: "text-slate-300 border-white/15",
  PROPOSAL: "text-accent border-accent/40",
  IN_PROGRESS: "text-emerald-300 border-emerald-400/40",
  UNDER_REVIEW: "text-amber-300 border-amber-400/40",
  REVISIONS: "text-orange-300 border-orange-400/40",
  APPROVED: "text-cyan-300 border-cyan-400/40",
  COMPLETED: "text-violet-300 border-violet-400/40",
};

export interface ProjectStats {
  total: number;
  active: number;
  completed: number;
}

/** Pure aggregation of dashboard stats from a project list. */
export function computeProjectStats(list: ProjectSummary[]): ProjectStats {
  return {
    total: list.length,
    active: list.filter((p) => ACTIVE_STATUSES.includes(p.status)).length,
    completed: list.filter((p) => p.status === "COMPLETED").length,
  };
}
