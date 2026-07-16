"use client";

import Link from "next/link";
import { useMemo, useState } from "react";
import { useQueryClient } from "@tanstack/react-query";
import { motion } from "framer-motion";
import { ArrowLeft, FileText } from "lucide-react";
import { GlassCard } from "@/components/ui/glass-card";
import { documentKeys, useDocument } from "../api/use-documents";
import { SectionEditor } from "./section-editor";
import { VersionHistory } from "./version-history";

export function DocumentEditor({ docId }: { docId: string }) {
  const qc = useQueryClient();
  const query = useDocument(docId);
  const [activeId, setActiveId] = useState<string | null>(null);
  const [historyOpen, setHistoryOpen] = useState(false);
  // Bumped on an explicit reload (conflict/restore) to force the editor to re-init from the
  // freshly-refetched content, since normal saves intentionally don't remount it.
  const [reloadKey, setReloadKey] = useState(0);

  async function reload() {
    await qc.invalidateQueries({ queryKey: documentKeys.doc(docId) });
    setReloadKey((n) => n + 1);
  }

  const sections = useMemo(() => query.data?.sections ?? [], [query.data]);
  const active = useMemo(
    () => sections.find((s) => s.id === activeId) ?? sections[0] ?? null,
    [sections, activeId],
  );

  const projectId = query.data?.document.projectId;

  if (query.isLoading) {
    return (
      <div className="grid place-items-center py-32">
        <div className="h-10 w-10 animate-spin-slow rounded-full border-2 border-white/10 border-t-accent" />
      </div>
    );
  }
  if (query.isError || !query.data) {
    return (
      <GlassCard className="p-8 text-center text-sm text-rose-300">
        Couldn’t load this document. You may not have access, or it no longer exists.
      </GlassCard>
    );
  }

  return (
    <div>
      <Link
        href={projectId ? `/projects/${projectId}` : "/dashboard"}
        className="inline-flex items-center gap-2 text-sm text-slate-400 transition-colors hover:text-white"
      >
        <ArrowLeft size={16} /> Back to project
      </Link>

      <motion.h1
        initial={{ opacity: 0, y: 12 }}
        animate={{ opacity: 1, y: 0 }}
        className="mt-4 font-display text-2xl font-bold text-white"
      >
        {query.data.document.title}
      </motion.h1>

      <div className="mt-8 grid gap-6 lg:grid-cols-[260px_1fr]">
        {/* Section navigation */}
        <nav className="lg:sticky lg:top-24 lg:self-start">
          <GlassCard className="p-3">
            <ul className="space-y-0.5">
              {sections.map((s) => {
                const isActive = active?.id === s.id;
                return (
                  <li key={s.id}>
                    <button
                      onClick={() => setActiveId(s.id)}
                      className={`flex w-full items-start gap-2 rounded-xl px-3 py-2 text-left text-sm transition-colors ${
                        isActive ? "bg-accent/10 text-white" : "text-slate-400 hover:bg-white/5 hover:text-white"
                      }`}
                    >
                      <FileText size={14} className="mt-0.5 shrink-0 opacity-70" />
                      <span className="min-w-0">
                        {s.chapter && (
                          <span className="block truncate text-[10px] uppercase tracking-wider text-slate-500">
                            {s.chapter}
                          </span>
                        )}
                        <span className="block truncate">{s.heading}</span>
                      </span>
                    </button>
                  </li>
                );
              })}
            </ul>
          </GlassCard>
        </nav>

        {/* Active section editor */}
        <div>
          {active ? (
            <SectionEditor
              key={`${active.id}:${reloadKey}`}
              docId={docId}
              section={active}
              onReload={reload}
              onOpenHistory={() => setHistoryOpen(true)}
            />
          ) : (
            <GlassCard className="p-8 text-center text-sm text-slate-400">This document has no sections.</GlassCard>
          )}
        </div>
      </div>

      {active && (
        <VersionHistory
          docId={docId}
          sectionId={active.id}
          open={historyOpen}
          onClose={() => setHistoryOpen(false)}
          onRestored={reload}
        />
      )}
    </div>
  );
}
