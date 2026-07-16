"use client";

import { useState } from "react";
import { Check, ChevronDown, ChevronUp, FileText, Pencil, Plus, Trash2, X } from "lucide-react";
import { GlassCard } from "@/components/ui/glass-card";
import type { DocSection } from "@/lib/api";
import { useAddSection, useDeleteSection, useUpdateSection } from "../api/use-documents";

/**
 * Document outline: select a section, and (for the project owner) add / rename / reorder / delete
 * sections — i.e. edit the number of chapters and their headings.
 */
export function SectionNav({
  docId,
  sections,
  activeId,
  onSelect,
  canManage,
}: {
  docId: string;
  sections: DocSection[];
  activeId: string | null;
  onSelect: (id: string) => void;
  canManage: boolean;
}) {
  const add = useAddSection(docId);
  const update = useUpdateSection(docId);
  const del = useDeleteSection(docId);
  const [editingId, setEditingId] = useState<string | null>(null);
  const [draft, setDraft] = useState("");
  const [adding, setAdding] = useState(false);
  const [newHeading, setNewHeading] = useState("");

  const busy = add.isPending || update.isPending || del.isPending;

  function startRename(s: DocSection) {
    setEditingId(s.id);
    setDraft(s.heading);
  }
  async function saveRename(s: DocSection) {
    const heading = draft.trim();
    if (heading && heading !== s.heading) await update.mutateAsync({ sectionId: s.id, heading });
    setEditingId(null);
  }
  async function swap(a: DocSection, b: DocSection) {
    // Swap order_index with the neighbour to move a section up/down.
    await update.mutateAsync({ sectionId: a.id, orderIndex: b.orderIndex });
    await update.mutateAsync({ sectionId: b.id, orderIndex: a.orderIndex });
  }
  async function addSection(e: React.FormEvent) {
    e.preventDefault();
    const heading = newHeading.trim();
    if (!heading) return;
    await add.mutateAsync({ heading });
    setNewHeading("");
    setAdding(false);
  }

  return (
    <GlassCard className="p-3">
      <ul className="space-y-0.5">
        {sections.map((s, i) => {
          const isActive = activeId === s.id;
          const editing = editingId === s.id;
          return (
            <li key={s.id} className="group">
              {editing ? (
                <div className="flex items-center gap-1 px-2 py-1.5">
                  <input
                    autoFocus
                    value={draft}
                    onChange={(e) => setDraft(e.target.value)}
                    onKeyDown={(e) => {
                      if (e.key === "Enter") saveRename(s);
                      if (e.key === "Escape") setEditingId(null);
                    }}
                    className="min-w-0 flex-1 rounded-lg border border-accent/50 bg-white/[0.06] px-2 py-1 text-sm text-white outline-none"
                  />
                  <button onClick={() => saveRename(s)} className="text-emerald-400 hover:text-emerald-300" aria-label="Save"><Check size={15} /></button>
                  <button onClick={() => setEditingId(null)} className="text-slate-400 hover:text-white" aria-label="Cancel"><X size={15} /></button>
                </div>
              ) : (
                <div
                  className={`flex items-start gap-2 rounded-xl px-3 py-2 text-sm transition-colors ${
                    isActive ? "bg-accent/10 text-white" : "text-slate-400 hover:bg-white/5 hover:text-white"
                  }`}
                >
                  <button onClick={() => onSelect(s.id)} className="flex min-w-0 flex-1 items-start gap-2 text-left">
                    <FileText size={14} className="mt-0.5 shrink-0 opacity-70" />
                    <span className="min-w-0">
                      {s.chapter && (
                        <span className="block truncate text-[10px] uppercase tracking-wider text-slate-500">{s.chapter}</span>
                      )}
                      <span className="block truncate">{s.heading}</span>
                    </span>
                  </button>
                  {canManage && (
                    <span className="flex shrink-0 items-center gap-0.5 opacity-0 transition-opacity group-hover:opacity-100">
                      <button disabled={busy || i === 0} onClick={() => swap(s, sections[i - 1])} className="text-slate-500 hover:text-white disabled:opacity-20" aria-label="Move up"><ChevronUp size={14} /></button>
                      <button disabled={busy || i === sections.length - 1} onClick={() => swap(s, sections[i + 1])} className="text-slate-500 hover:text-white disabled:opacity-20" aria-label="Move down"><ChevronDown size={14} /></button>
                      <button disabled={busy} onClick={() => startRename(s)} className="text-slate-500 hover:text-white" aria-label="Rename"><Pencil size={13} /></button>
                      <button disabled={busy} onClick={() => { if (confirm(`Delete section "${s.heading}"? This removes its content and history.`)) del.mutate(s.id); }} className="text-slate-500 hover:text-rose-400" aria-label="Delete"><Trash2 size={13} /></button>
                    </span>
                  )}
                </div>
              )}
            </li>
          );
        })}
      </ul>

      {canManage && (
        <div className="mt-1 border-t border-white/5 pt-2">
          {adding ? (
            <form onSubmit={addSection} className="flex items-center gap-1 px-2">
              <input
                autoFocus
                value={newHeading}
                onChange={(e) => setNewHeading(e.target.value)}
                onKeyDown={(e) => e.key === "Escape" && setAdding(false)}
                placeholder="New section heading"
                className="min-w-0 flex-1 rounded-lg border border-accent/50 bg-white/[0.06] px-2 py-1 text-sm text-white outline-none"
              />
              <button type="submit" disabled={busy || !newHeading.trim()} className="text-emerald-400 hover:text-emerald-300 disabled:opacity-30" aria-label="Add"><Check size={15} /></button>
              <button type="button" onClick={() => setAdding(false)} className="text-slate-400 hover:text-white" aria-label="Cancel"><X size={15} /></button>
            </form>
          ) : (
            <button onClick={() => setAdding(true)} className="flex w-full items-center gap-2 rounded-xl px-3 py-2 text-sm text-accent hover:bg-white/5">
              <Plus size={14} /> Add section
            </button>
          )}
        </div>
      )}
    </GlassCard>
  );
}
