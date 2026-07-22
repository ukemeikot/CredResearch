"use client";

import { useEffect, useState } from "react";
import { Trash2 } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Field } from "@/components/ui/field";
import { GlassCard } from "@/components/ui/glass-card";
import { ApiError } from "@/lib/api";
import { useProjectRole } from "../api/use-project-role";
import { useUpdateProject } from "../api/use-projects";
import { DeleteProjectModal } from "./delete-project-modal";

const LEVELS = ["UG", "MSc", "PhD"];

export function ProjectSettingsSection({ id }: { id: string }) {
  const { query, isOwner } = useProjectRole(id);
  const update = useUpdateProject(id);
  const [title, setTitle] = useState("");
  const [level, setLevel] = useState("UG");
  const [abstractText, setAbstractText] = useState("");
  const [deleteOpen, setDeleteOpen] = useState(false);
  const [saved, setSaved] = useState(false);

  const project = query.data?.project;
  useEffect(() => {
    if (project) {
      setTitle(project.title);
      setLevel(project.level ?? "UG");
      setAbstractText(project.abstractText ?? "");
    }
  }, [project]);

  if (!project) return null;

  if (!isOwner) {
    return (
      <GlassCard className="p-8 text-center text-sm text-slate-500">
        Only the project owner can change these settings.
      </GlassCard>
    );
  }

  async function save(e: React.FormEvent) {
    e.preventDefault();
    setSaved(false);
    try {
      await update.mutateAsync({ title, level, abstractText });
      setSaved(true);
    } catch {
      /* surfaced via update.error */
    }
  }

  const error = update.error instanceof ApiError ? update.error.message : null;

  return (
    <div className="space-y-8">
      <div>
        <h2 className="font-display text-xl font-bold text-slate-900">Project settings</h2>
        <p className="mt-1 text-sm text-slate-500">Update the project’s details or delete it.</p>
      </div>

      <GlassCard className="p-6">
        <form onSubmit={save} className="space-y-4">
          <Field label="Title" type="text" value={title} onChange={setTitle} />
          <label className="block">
            <span className="mb-1.5 block text-xs font-medium uppercase tracking-wider text-slate-500">
              Academic level
            </span>
            <div className="flex gap-2">
              {LEVELS.map((l) => (
                <button
                  type="button"
                  key={l}
                  onClick={() => setLevel(l)}
                  className={`flex-1 rounded-xl border px-4 py-2.5 text-sm transition-all ${
                    level === l
                      ? "border-accent/60 bg-accent/10 text-slate-900 shadow-glow"
                      : "border-slate-200 text-slate-500 hover:border-slate-300"
                  }`}
                >
                  {l}
                </button>
              ))}
            </div>
          </label>
          <label className="block">
            <span className="mb-1.5 block text-xs font-medium uppercase tracking-wider text-slate-500">
              Abstract
            </span>
            <textarea
              value={abstractText}
              onChange={(e) => setAbstractText(e.target.value)}
              rows={4}
              placeholder="A short summary of the research"
              className="w-full resize-y rounded-xl border border-slate-200 bg-slate-50 px-4 py-3 text-sm text-slate-900 placeholder:text-slate-500 outline-none transition-all focus:border-accent/60 focus:bg-white/[0.06] focus:shadow-glow"
            />
          </label>
          {error && <p className="text-sm text-rose-600">{error}</p>}
          {saved && !error && <p className="text-sm text-emerald-600">Saved.</p>}
          <Button type="submit" disabled={update.isPending || !title}>
            {update.isPending ? "Saving…" : "Save changes"}
          </Button>
        </form>
      </GlassCard>

      {/* Danger zone */}
      <GlassCard className="border-rose-200 p-6">
        <h3 className="font-display text-base font-semibold text-rose-600">Danger zone</h3>
        <p className="mt-1 text-sm text-slate-500">
          Deleting a project permanently removes all of its documents, references, questionnaires,
          survey responses and reviews. This cannot be undone.
        </p>
        <Button
          variant="outline"
          className="mt-4 !border-rose-300 !text-rose-600 hover:!bg-rose-50"
          onClick={() => setDeleteOpen(true)}
        >
          <Trash2 size={15} /> Delete project
        </Button>
      </GlassCard>

      <DeleteProjectModal project={project} open={deleteOpen} onClose={() => setDeleteOpen(false)} />
    </div>
  );
}
