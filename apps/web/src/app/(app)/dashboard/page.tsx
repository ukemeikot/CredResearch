"use client";

import { useQuery, useQueryClient } from "@tanstack/react-query";
import { motion } from "framer-motion";
import { FolderPlus, Plus } from "lucide-react";
import { useState } from "react";
import { Button } from "@/components/ui/button";
import { GlassCard } from "@/components/ui/glass-card";
import { AnimatedCounter } from "@/components/ui/animated-counter";
import { CreateProjectModal } from "@/components/create-project-modal";
import { api, type ProjectSummary } from "@/lib/api";
import { useAuth } from "@/lib/auth-store";

const STATUS_COLOR: Record<string, string> = {
  DRAFT: "text-slate-300 border-white/15",
  PROPOSAL: "text-accent border-accent/40",
  IN_PROGRESS: "text-emerald-300 border-emerald-400/40",
  UNDER_REVIEW: "text-amber-300 border-amber-400/40",
  REVISIONS: "text-orange-300 border-orange-400/40",
  APPROVED: "text-cyan-300 border-cyan-400/40",
  COMPLETED: "text-violet-300 border-violet-400/40",
};

export default function DashboardPage() {
  const qc = useQueryClient();
  const user = useAuth((s) => s.user);
  const [modalOpen, setModalOpen] = useState(false);

  const me = useQuery({ queryKey: ["me"], queryFn: api.me });
  const projects = useQuery({ queryKey: ["projects"], queryFn: api.listProjects });

  const list = projects.data ?? [];
  const active = list.filter((p) => ["IN_PROGRESS", "PROPOSAL", "UNDER_REVIEW", "REVISIONS"].includes(p.status)).length;
  const completed = list.filter((p) => p.status === "COMPLETED").length;

  const onCreated = (p: ProjectSummary) =>
    qc.setQueryData<ProjectSummary[]>(["projects"], (old) => [p, ...(old ?? [])]);

  return (
    <div>
      <motion.div
        initial={{ opacity: 0, y: 16 }}
        animate={{ opacity: 1, y: 0 }}
        className="flex flex-wrap items-end justify-between gap-4"
      >
        <div>
          <p className="eyebrow">Workspace</p>
          <h1 className="mt-1 font-display text-3xl font-bold text-white">
            Welcome{me.data?.fullName ? `, ${me.data.fullName.split(" ")[0]}` : ""}
          </h1>
          <p className="mt-1 text-sm text-slate-400">
            {me.data?.email ?? user?.id}
            {me.data && !me.data.emailVerified && (
              <span className="ml-2 rounded-full border border-amber-400/40 px-2 py-0.5 text-xs text-amber-300">
                email unverified
              </span>
            )}
          </p>
        </div>
        <Button onClick={() => setModalOpen(true)}>
          <Plus size={18} /> New project
        </Button>
      </motion.div>

      {/* Stats */}
      <div className="mt-8 grid gap-4 sm:grid-cols-3">
        <Stat label="Total projects" value={list.length} />
        <Stat label="Active" value={active} />
        <Stat label="Completed" value={completed} />
      </div>

      {/* Projects */}
      <h2 className="mt-12 mb-4 font-display text-lg font-semibold uppercase tracking-wider text-white">
        Your projects
      </h2>

      {projects.isLoading ? (
        <p className="text-sm text-slate-400">Loading…</p>
      ) : projects.isError ? (
        <GlassCard className="p-6 text-sm text-rose-300">
          Couldn’t load projects. Is the backend running on :18080?
        </GlassCard>
      ) : list.length === 0 ? (
        <GlassCard className="flex flex-col items-center gap-4 p-12 text-center">
          <FolderPlus className="text-accent" size={40} />
          <div>
            <p className="font-display text-lg text-white">No projects yet</p>
            <p className="mt-1 text-sm text-slate-400">Create your first research project to get started.</p>
          </div>
          <Button onClick={() => setModalOpen(true)} variant="outline">
            <Plus size={16} /> New project
          </Button>
        </GlassCard>
      ) : (
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {list.map((p, i) => (
            <motion.div
              key={p.id}
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: i * 0.06 }}
            >
              <GlassCard interactive className="flex h-full flex-col gap-4 p-5">
                <div className="flex items-start justify-between gap-3">
                  <h3 className="font-display text-base font-semibold text-white">{p.title}</h3>
                  <span className="shrink-0 rounded-full border px-2 py-0.5 text-[10px] uppercase tracking-wider text-slate-400">
                    {p.level}
                  </span>
                </div>
                <div className="mt-auto">
                  <span
                    className={`inline-flex rounded-full border px-3 py-1 text-xs font-medium ${
                      STATUS_COLOR[p.status] ?? "text-slate-300 border-white/15"
                    }`}
                  >
                    {p.status.replace(/_/g, " ")}
                  </span>
                </div>
              </GlassCard>
            </motion.div>
          ))}
        </div>
      )}

      <CreateProjectModal open={modalOpen} onClose={() => setModalOpen(false)} onCreated={onCreated} />
    </div>
  );
}

function Stat({ label, value }: { label: string; value: number }) {
  return (
    <GlassCard className="p-6">
      <p className="text-xs font-medium uppercase tracking-wider text-slate-400">{label}</p>
      <p className="mt-2 font-display text-4xl font-bold text-white">
        <AnimatedCounter value={value} />
      </p>
    </GlassCard>
  );
}
