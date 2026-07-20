import type { ProjectStatus, ProjectSummary } from "@/lib/api";

/** Statuses considered "active" for dashboard stats. */
export const ACTIVE_STATUSES: ProjectStatus[] = [
  "PROPOSAL",
  "IN_PROGRESS",
  "UNDER_REVIEW",
  "REVISIONS",
];

/** Canonical lifecycle order (for progress display). */
export const STATUS_ORDER: ProjectStatus[] = [
  "DRAFT",
  "PROPOSAL",
  "IN_PROGRESS",
  "UNDER_REVIEW",
  "REVISIONS",
  "APPROVED",
  "COMPLETED",
];

/**
 * Legal status transitions — mirrors the backend `ProjectStatus` state machine
 * (FR-PROJ-4). The UI only offers these; the server is still the source of truth.
 */
const LEGAL_TRANSITIONS: Record<ProjectStatus, ProjectStatus[]> = {
  DRAFT: ["PROPOSAL"],
  PROPOSAL: ["IN_PROGRESS"],
  IN_PROGRESS: ["UNDER_REVIEW"],
  UNDER_REVIEW: ["REVISIONS", "APPROVED"],
  REVISIONS: ["UNDER_REVIEW"],
  APPROVED: ["COMPLETED"],
  COMPLETED: [],
};

/** Legal next statuses from the current one. */
export function nextStatuses(current: ProjectStatus): ProjectStatus[] {
  return LEGAL_TRANSITIONS[current] ?? [];
}

/** Human-readable status label. */
export function formatStatus(status: string): string {
  return status.replace(/_/g, " ");
}

/** Tailwind classes per project status (badge styling). */
export const STATUS_COLOR: Record<string, string> = {
  DRAFT: "text-slate-300 border-slate-200",
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
  let active = 0;
  let completed = 0;
  for (const p of list) {
    if (p.status === "COMPLETED") completed++;
    else if (ACTIVE_STATUSES.includes(p.status)) active++;
  }
  return { total: list.length, active, completed };
}
