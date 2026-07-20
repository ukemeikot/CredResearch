"use client";

import { useState } from "react";
import { CheckCircle2, Circle, Plus } from "lucide-react";
import { GlassCard } from "@/components/ui/glass-card";
import { Button } from "@/components/ui/button";
import { Field } from "@/components/ui/field";
import { ApiError, type Milestone } from "@/lib/api";
import { useAddMilestone } from "../api/use-projects";

export function MilestonesPanel({
  id,
  milestones,
  canManage,
}: {
  id: string;
  milestones: Milestone[];
  canManage: boolean;
}) {
  const add = useAddMilestone(id);
  const [open, setOpen] = useState(false);
  const [title, setTitle] = useState("");
  const [dueDate, setDueDate] = useState("");

  async function submit(e: React.FormEvent) {
    e.preventDefault();
    try {
      await add.mutateAsync({ title, dueDate: dueDate || undefined });
      setTitle("");
      setDueDate("");
      setOpen(false);
    } catch {
      /* surfaced via add.error */
    }
  }

  const error = add.error instanceof ApiError ? add.error.message : null;

  return (
    <GlassCard className="p-6">
      <div className="flex items-center justify-between">
        <p className="text-xs font-medium uppercase tracking-wider text-slate-500">Milestones</p>
        {canManage && !open && (
          <button
            onClick={() => setOpen(true)}
            className="inline-flex items-center gap-1 text-xs text-accent hover:text-accent-soft"
          >
            <Plus size={14} /> Add
          </button>
        )}
      </div>

      {milestones.length === 0 && !open ? (
        <p className="mt-3 text-sm text-slate-500">No milestones yet.</p>
      ) : (
        <ul className="mt-4 space-y-3">
          {milestones.map((m) => {
            const done = m.status === "DONE" || !!m.completedAt;
            return (
              <li key={m.id} className="flex items-start gap-3">
                {done ? (
                  <CheckCircle2 size={18} className="mt-0.5 shrink-0 text-emerald-600" />
                ) : (
                  <Circle size={18} className="mt-0.5 shrink-0 text-slate-500" />
                )}
                <div className="min-w-0">
                  <p className={`text-sm ${done ? "text-slate-500 line-through" : "text-slate-900"}`}>
                    {m.title}
                  </p>
                  {m.dueDate && <p className="text-xs text-slate-500">due {m.dueDate}</p>}
                </div>
              </li>
            );
          })}
        </ul>
      )}

      {open && (
        <form onSubmit={submit} className="mt-4 space-y-3 border-t border-slate-200 pt-4">
          <Field label="Title" type="text" value={title} onChange={setTitle} placeholder="e.g. Literature review" />
          <Field label="Due date" type="date" value={dueDate} onChange={setDueDate} required={false} />
          {error && <p className="text-sm text-rose-600">{error}</p>}
          <div className="flex gap-2">
            <Button type="submit" size="sm" disabled={add.isPending || !title}>
              {add.isPending ? "Adding…" : "Add milestone"}
            </Button>
            <Button type="button" size="sm" variant="ghost" onClick={() => setOpen(false)}>
              Cancel
            </Button>
          </div>
        </form>
      )}
    </GlassCard>
  );
}
