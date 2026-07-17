"use client";

import Link from "next/link";
import { useMemo, useState } from "react";
import { useQueryClient } from "@tanstack/react-query";
import { motion } from "framer-motion";
import { ArrowLeft, Download, Loader2, ShieldCheck } from "lucide-react";
import { api, ApiError } from "@/lib/api";
import { GlassCard } from "@/components/ui/glass-card";
import { useProject } from "@/features/project/api/use-projects";
import { useMe } from "@/features/user/api/use-me";
import { documentKeys, useDocument } from "../api/use-documents";
import { DisclosureLedger } from "./disclosure-ledger";
import { SectionEditor } from "./section-editor";
import { SectionNav } from "./section-nav";
import { VersionHistory } from "./version-history";

export function DocumentEditor({ docId }: { docId: string }) {
  const qc = useQueryClient();
  const query = useDocument(docId);
  const me = useMe();
  const projectId = query.data?.document.projectId;
  const project = useProject(projectId ?? "");
  const [activeId, setActiveId] = useState<string | null>(null);
  const [historyOpen, setHistoryOpen] = useState(false);
  const [disclosureOpen, setDisclosureOpen] = useState(false);
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

  // Only the project OWNER can restructure the document (the backend enforces this too).
  const isOwner = useMemo(() => {
    const uid = me.data?.id;
    if (!uid || !project.data) return false;
    return project.data.members.some((m) => m.userId === uid && m.role === "OWNER");
  }, [me.data?.id, project.data]);

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

      <div className="mt-4 flex flex-wrap items-center justify-between gap-3">
        <motion.h1
          initial={{ opacity: 0, y: 12 }}
          animate={{ opacity: 1, y: 0 }}
          className="font-display text-2xl font-bold text-white"
        >
          {query.data.document.title}
        </motion.h1>
        <div className="flex items-center gap-2">
          <DownloadMenu docId={docId} />
          <button
            onClick={() => setDisclosureOpen(true)}
            className="inline-flex items-center gap-1.5 rounded-full border border-white/10 px-3 py-1.5 text-xs text-slate-300 transition-colors hover:border-white/30 hover:text-white"
          >
            <ShieldCheck size={14} /> AI disclosure
          </button>
        </div>
      </div>

      <div className="mt-8 grid gap-6 lg:grid-cols-[260px_1fr]">
        {/* Section navigation + structure editing (owner) */}
        <nav className="lg:sticky lg:top-24 lg:self-start">
          <SectionNav
            docId={docId}
            sections={sections}
            activeId={active?.id ?? null}
            onSelect={setActiveId}
            canManage={isOwner}
          />
        </nav>

        {/* Active section editor */}
        <div>
          {active ? (
            <SectionEditor
              key={`${active.id}:${reloadKey}`}
              docId={docId}
              projectId={projectId ?? ""}
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
      <DisclosureLedger docId={docId} open={disclosureOpen} onClose={() => setDisclosureOpen(false)} />
    </div>
  );
}

/** Download the document as .docx or .pdf. PDF may be unavailable if no conversion service is wired. */
function DownloadMenu({ docId }: { docId: string }) {
  const [open, setOpen] = useState(false);
  const [busy, setBusy] = useState<null | "docx" | "pdf">(null);
  const [error, setError] = useState<string | null>(null);

  async function download(format: "docx" | "pdf") {
    setBusy(format);
    setError(null);
    try {
      await api.downloadDocument(docId, format);
      setOpen(false);
    } catch (e) {
      setError(
        e instanceof ApiError && e.status === 503
          ? "PDF export isn’t available yet — try Word (.docx)."
          : "Download failed. Please try again.",
      );
    } finally {
      setBusy(null);
    }
  }

  return (
    <div className="relative">
      <button
        onClick={() => setOpen((v) => !v)}
        className="inline-flex items-center gap-1.5 rounded-full border border-white/10 px-3 py-1.5 text-xs text-slate-300 transition-colors hover:border-white/30 hover:text-white"
      >
        {busy ? <Loader2 size={14} className="animate-spin" /> : <Download size={14} />} Download
      </button>
      {open && (
        <>
          <div className="fixed inset-0 z-10" onClick={() => setOpen(false)} />
          <div className="absolute right-0 z-20 mt-1 w-44 overflow-hidden rounded-xl border border-white/10 bg-cosmos-900 shadow-xl">
            <button
              onClick={() => download("docx")}
              disabled={!!busy}
              className="block w-full px-4 py-2.5 text-left text-xs text-slate-200 hover:bg-white/5 disabled:opacity-50"
            >
              Word (.docx)
            </button>
            <button
              onClick={() => download("pdf")}
              disabled={!!busy}
              className="block w-full px-4 py-2.5 text-left text-xs text-slate-200 hover:bg-white/5 disabled:opacity-50"
            >
              PDF (.pdf)
            </button>
            {error && <p className="px-4 py-2 text-[11px] text-rose-400">{error}</p>}
          </div>
        </>
      )}
    </div>
  );
}
