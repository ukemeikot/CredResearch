"use client";

import Link from "next/link";
import { useParams, usePathname } from "next/navigation";
import { motion } from "framer-motion";
import {
  ArrowLeft,
  BarChart3,
  BookOpen,
  ClipboardList,
  FileText,
  LayoutDashboard,
  Settings,
  Users,
} from "lucide-react";
import { GlassCard } from "@/components/ui/glass-card";
import { useProjectRole } from "@/features/project/api/use-project-role";
import { formatStatus, STATUS_COLOR } from "@/features/project/model/project-status";

const TABS = [
  { seg: "", label: "Overview", icon: LayoutDashboard },
  { seg: "documents", label: "Documents", icon: FileText },
  { seg: "references", label: "References", icon: BookOpen },
  { seg: "questionnaires", label: "Questionnaires", icon: ClipboardList },
  { seg: "analysis", label: "Analysis", icon: BarChart3 },
  { seg: "team", label: "Team", icon: Users },
  { seg: "settings", label: "Settings", icon: Settings, ownerOnly: true },
] as const;

export default function ProjectLayout({ children }: { children: React.ReactNode }) {
  const { id } = useParams<{ id: string }>();
  const pathname = usePathname();
  const { query, isOwner } = useProjectRole(id);

  // The deep detail routes (document editor, questionnaire builder) are full-screen experiences
  // with their own navigation — render them without the project shell.
  const rest = pathname.split(`/projects/${id}`)[1] ?? "";
  const parts = rest.split("/").filter(Boolean);
  const isDeep = parts.length >= 2 && (parts[0] === "documents" || parts[0] === "questionnaires");
  if (isDeep) return <>{children}</>;

  const activeSeg = parts[0] ?? "";

  if (query.isLoading) {
    return (
      <div className="grid place-items-center py-32">
        <div className="h-10 w-10 animate-spin-slow rounded-full border-2 border-slate-200 border-t-accent" />
      </div>
    );
  }

  if (query.isError || !query.data) {
    const status = query.error && "status" in query.error ? (query.error as { status: number }).status : 0;
    return (
      <div className="py-16">
        <BackLink />
        <GlassCard className="mt-6 p-8 text-center">
          <p className="font-display text-lg text-slate-900">
            {status === 404
              ? "Project not found"
              : status === 403
                ? "You don’t have access to this project"
                : "Couldn’t load project"}
          </p>
          <p className="mt-2 text-sm text-slate-500">
            {status === 404
              ? "It may have been removed, or it belongs to another workspace."
              : status === 403
                ? "Ask the project owner to add you as a member."
                : "Please try again in a moment."}
          </p>
        </GlassCard>
      </div>
    );
  }

  const { project } = query.data;
  const tabs = TABS.filter((t) => !("ownerOnly" in t && t.ownerOnly) || isOwner);

  return (
    <div>
      <BackLink />

      <motion.div
        initial={{ opacity: 0, y: 12 }}
        animate={{ opacity: 1, y: 0 }}
        className="mt-4 flex flex-wrap items-center gap-3"
      >
        <h1 className="font-display text-2xl font-bold text-slate-900 sm:text-3xl">{project.title}</h1>
        {project.level && (
          <span className="rounded-full border border-slate-200 px-2.5 py-0.5 text-[11px] uppercase tracking-wider text-slate-500">
            {project.level}
          </span>
        )}
        <span
          className={`inline-flex rounded-full border px-3 py-1 text-xs font-medium ${
            STATUS_COLOR[project.status] ?? "text-slate-600 border-slate-200"
          }`}
        >
          {formatStatus(project.status)}
        </span>
      </motion.div>

      <div className="mt-8 grid gap-8 lg:grid-cols-[220px_1fr]">
        {/* Sidebar */}
        <nav className="lg:sticky lg:top-24 lg:self-start">
          <ul className="flex gap-1 overflow-x-auto pb-2 lg:flex-col lg:gap-0.5 lg:overflow-visible lg:pb-0">
            {tabs.map((t) => {
              const href = t.seg ? `/projects/${id}/${t.seg}` : `/projects/${id}`;
              const active = activeSeg === t.seg;
              return (
                <li key={t.seg || "overview"} className="shrink-0">
                  <Link
                    href={href}
                    className={`flex items-center gap-2.5 whitespace-nowrap rounded-xl px-3.5 py-2.5 text-sm font-medium transition-colors ${
                      active
                        ? "bg-accent/10 text-accent"
                        : "text-slate-600 hover:bg-slate-100 hover:text-slate-900"
                    }`}
                  >
                    <t.icon size={17} className={active ? "text-accent" : "text-slate-400"} />
                    {t.label}
                  </Link>
                </li>
              );
            })}
          </ul>
        </nav>

        {/* Section content */}
        <motion.div
          key={activeSeg}
          initial={{ opacity: 0, y: 8 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.25 }}
          className="min-w-0"
        >
          {children}
        </motion.div>
      </div>
    </div>
  );
}

function BackLink() {
  return (
    <Link
      href="/dashboard"
      className="inline-flex items-center gap-1.5 text-sm text-slate-500 transition-colors hover:text-slate-900"
    >
      <ArrowLeft size={16} /> Projects
    </Link>
  );
}
