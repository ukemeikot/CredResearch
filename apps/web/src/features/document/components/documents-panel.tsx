"use client";

import Link from "next/link";
import { useState } from "react";
import { FileText, Plus } from "lucide-react";
import { GlassCard } from "@/components/ui/glass-card";
import { useProjectDocuments } from "../api/use-documents";
import { CreateDocumentModal } from "./create-document-modal";

/** Documents for a project, shown in the project workspace. Owners can create from a template. */
export function DocumentsPanel({ projectId, canCreate }: { projectId: string; canCreate: boolean }) {
  const docs = useProjectDocuments(projectId);
  const [open, setOpen] = useState(false);
  const list = docs.data ?? [];

  return (
    <GlassCard className="p-6">
      <div className="flex items-center justify-between">
        <p className="text-xs font-medium uppercase tracking-wider text-slate-400">Documents</p>
        {canCreate && (
          <button
            onClick={() => setOpen(true)}
            className="inline-flex items-center gap-1 text-xs text-accent hover:text-accent-soft"
          >
            <Plus size={14} /> New
          </button>
        )}
      </div>

      {docs.isLoading ? (
        <p className="mt-3 text-sm text-slate-500">Loading…</p>
      ) : list.length === 0 ? (
        <p className="mt-3 text-sm text-slate-500">
          No documents yet.{canCreate ? " Create one from a template to start writing." : ""}
        </p>
      ) : (
        <ul className="mt-4 space-y-2">
          {list.map((d) => (
            <li key={d.id}>
              <Link
                href={`/projects/${projectId}/documents/${d.id}`}
                className="flex items-center gap-3 rounded-xl border border-white/5 bg-white/[0.02] px-3 py-2.5 transition-colors hover:border-white/20 hover:bg-white/[0.04]"
              >
                <span className="grid h-8 w-8 shrink-0 place-items-center rounded-lg border border-white/10 text-accent">
                  <FileText size={15} />
                </span>
                <span className="min-w-0">
                  <span className="block truncate text-sm text-white">{d.title}</span>
                  <span className="block text-[11px] uppercase tracking-wider text-slate-500">{d.status}</span>
                </span>
              </Link>
            </li>
          ))}
        </ul>
      )}

      {canCreate && <CreateDocumentModal projectId={projectId} open={open} onClose={() => setOpen(false)} />}
    </GlassCard>
  );
}
