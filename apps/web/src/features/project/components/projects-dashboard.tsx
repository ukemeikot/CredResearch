"use client";

import { motion } from "framer-motion";
import { FolderPlus, Plus } from "lucide-react";
import { useState } from "react";
import { Button } from "@/components/ui/button";
import { GlassCard } from "@/components/ui/glass-card";
import { useMe } from "@/features/user/api/use-me";
import { useProjects } from "../api/use-projects";
import { computeProjectStats } from "../model/project-status";
import { CreateProjectModal } from "./create-project-modal";
import { ProjectCard } from "./project-card";
import { StatCard } from "./stat-card";

export function ProjectsDashboard() {
  const me = useMe();
  const projects = useProjects();
  const [modalOpen, setModalOpen] = useState(false);

  const list = projects.data ?? [];
  const stats = computeProjectStats(list);

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
            {me.data?.email}
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

      <div className="mt-8 grid gap-4 sm:grid-cols-3">
        <StatCard label="Total projects" value={stats.total} />
        <StatCard label="Active" value={stats.active} />
        <StatCard label="Completed" value={stats.completed} />
      </div>

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
            <ProjectCard key={p.id} project={p} index={i} />
          ))}
        </div>
      )}

      <CreateProjectModal open={modalOpen} onClose={() => setModalOpen(false)} />
    </div>
  );
}
