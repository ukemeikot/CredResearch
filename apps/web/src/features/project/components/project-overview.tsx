"use client";

import { GlassCard } from "@/components/ui/glass-card";
import { useProjectRole } from "../api/use-project-role";
import { ActivityFeed } from "./activity-feed";
import { MilestonesPanel } from "./milestones-panel";
import { StatCard } from "./stat-card";
import { StatusControl } from "./status-control";

export function ProjectOverview({ id }: { id: string }) {
  const { query, canManage } = useProjectRole(id);
  if (!query.data) return null; // loading/error handled by the layout
  const { project, dashboard, milestones } = query.data;

  return (
    <div className="space-y-8">
      {project.abstractText && (
        <p className="max-w-3xl text-sm leading-relaxed text-slate-600">{project.abstractText}</p>
      )}

      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <StatCard label="Milestones" value={dashboard.totalMilestones} />
        <StatCard label="Completed" value={dashboard.completedMilestones} />
        <StatCard label="Members" value={dashboard.memberCount} />
        <GlassCard className="p-6">
          <p className="text-xs font-medium uppercase tracking-wider text-slate-500">Next milestone</p>
          <p className="mt-2 truncate font-display text-lg font-semibold text-slate-900">
            {dashboard.nextMilestone?.title ?? "—"}
          </p>
          {dashboard.nextMilestone?.dueDate && (
            <p className="mt-1 text-xs text-slate-500">due {dashboard.nextMilestone.dueDate}</p>
          )}
        </GlassCard>
      </div>

      <div className="grid items-start gap-6 lg:grid-cols-2">
        <StatusControl id={id} status={project.status} canManage={canManage} />
        <MilestonesPanel id={id} milestones={milestones} canManage={canManage} />
      </div>

      <ActivityFeed id={id} />
    </div>
  );
}
