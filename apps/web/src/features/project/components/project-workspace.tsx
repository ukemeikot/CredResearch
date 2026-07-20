"use client";

import Link from "next/link";
import { useMemo, useState } from "react";
import { motion } from "framer-motion";
import { ArrowLeft, Pencil } from "lucide-react";
import { Button } from "@/components/ui/button";
import { GlassCard } from "@/components/ui/glass-card";
import { DocumentsPanel } from "@/features/document/components/documents-panel";
import { ReferencesPanel } from "@/features/paper/components/references-panel";
import { QuestionnairesPanel } from "@/features/questionnaire/components/questionnaires-panel";
import { DataAnalysisPanel } from "@/features/analysis/components/data-analysis-panel";
import { useMe } from "@/features/user/api/use-me";
import type { ProjectMemberRole } from "@/lib/api";
import { useProject } from "../api/use-projects";
import { formatStatus, STATUS_COLOR } from "../model/project-status";
import { ActivityFeed } from "./activity-feed";
import { EditProjectModal } from "./edit-project-modal";
import { MembersPanel } from "./members-panel";
import { MilestonesPanel } from "./milestones-panel";
import { StatCard } from "./stat-card";
import { StatusControl } from "./status-control";

export function ProjectWorkspace({ id }: { id: string }) {
  const me = useMe();
  const query = useProject(id);
  const [editOpen, setEditOpen] = useState(false);

  // The caller's project-scoped role drives which controls are shown (the
  // backend enforces the same rules regardless).
  const myRole: ProjectMemberRole | null = useMemo(() => {
    const uid = me.data?.id;
    if (!uid || !query.data) return null;
    return query.data.members.find((m) => m.userId === uid)?.role ?? null;
  }, [me.data?.id, query.data]);

  if (query.isLoading) {
    return (
      <div className="grid place-items-center py-32">
        <div className="h-10 w-10 animate-spin-slow rounded-full border-2 border-slate-200 border-t-accent" />
      </div>
    );
  }

  if (query.isError || !query.data) {
    const notFound = query.error && "status" in query.error && (query.error as { status: number }).status === 404;
    const forbidden = query.error && "status" in query.error && (query.error as { status: number }).status === 403;
    return (
      <div className="py-16">
        <BackLink />
        <GlassCard className="mt-6 p-8 text-center">
          <p className="font-display text-lg text-slate-900">
            {notFound ? "Project not found" : forbidden ? "You don’t have access to this project" : "Couldn’t load project"}
          </p>
          <p className="mt-2 text-sm text-slate-500">
            {notFound
              ? "It may have been removed, or it belongs to another workspace."
              : forbidden
                ? "Ask the project owner to add you as a member."
                : "Please try again in a moment."}
          </p>
        </GlassCard>
      </div>
    );
  }

  const { project, dashboard, members, milestones } = query.data;
  const isOwner = myRole === "OWNER";
  const canManage = myRole === "OWNER" || myRole === "SUPERVISOR";

  return (
    <div>
      <BackLink />

      <motion.div
        initial={{ opacity: 0, y: 16 }}
        animate={{ opacity: 1, y: 0 }}
        className="mt-4 flex flex-wrap items-start justify-between gap-4"
      >
        <div className="min-w-0">
          <div className="flex flex-wrap items-center gap-3">
            <h1 className="font-display text-3xl font-bold text-slate-900">{project.title}</h1>
            {project.level && (
              <span className="rounded-full border border-slate-200 px-2.5 py-0.5 text-[11px] uppercase tracking-wider text-slate-500">
                {project.level}
              </span>
            )}
          </div>
          <span
            className={`mt-3 inline-flex rounded-full border px-3 py-1 text-xs font-medium ${
              STATUS_COLOR[project.status] ?? "text-slate-600 border-slate-200"
            }`}
          >
            {formatStatus(project.status)}
          </span>
        </div>
        {isOwner && (
          <Button variant="outline" size="sm" onClick={() => setEditOpen(true)}>
            <Pencil size={15} /> Edit
          </Button>
        )}
      </motion.div>

      {project.abstractText && (
        <p className="mt-5 max-w-3xl text-sm leading-relaxed text-slate-600">{project.abstractText}</p>
      )}

      <div className="mt-8 grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
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

      <div className="mt-10 grid items-start gap-6 lg:grid-cols-2">
        <div className="space-y-6">
          <StatusControl id={id} status={project.status} canManage={canManage} />
          <DocumentsPanel projectId={id} canCreate={isOwner} />
          <ReferencesPanel projectId={id} />
          <QuestionnairesPanel projectId={id} />
          <DataAnalysisPanel projectId={id} projectTitle={project.title} />
          <MilestonesPanel id={id} milestones={milestones} canManage={canManage} />
        </div>
        <div className="space-y-6">
          <MembersPanel id={id} members={members} ownerUserId={project.ownerUserId} isOwner={isOwner} />
          <ActivityFeed id={id} />
        </div>
      </div>

      {isOwner && <EditProjectModal project={project} open={editOpen} onClose={() => setEditOpen(false)} />}
    </div>
  );
}

function BackLink() {
  return (
    <Link
      href="/dashboard"
      className="inline-flex items-center gap-2 text-sm text-slate-500 transition-colors hover:text-slate-900"
    >
      <ArrowLeft size={16} /> Back to workspace
    </Link>
  );
}
