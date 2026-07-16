"use client";

import { AnimatePresence, motion } from "framer-motion";
import { X } from "lucide-react";
import { useEffect, useState } from "react";
import { Button } from "@/components/ui/button";
import { Field } from "@/components/ui/field";
import { ApiError, type ProjectSummary } from "@/lib/api";
import { useUpdateProject } from "../api/use-projects";

const LEVELS = ["UG", "MSc", "PhD"];

export function EditProjectModal({
  project,
  open,
  onClose,
}: {
  project: ProjectSummary;
  open: boolean;
  onClose: () => void;
}) {
  const update = useUpdateProject(project.id);
  const [title, setTitle] = useState(project.title);
  const [level, setLevel] = useState(project.level ?? "UG");
  const [abstractText, setAbstractText] = useState(project.abstractText ?? "");

  // The modal stays mounted (for exit animation); reseed fields whenever it
  // opens or the underlying project changes, so it never shows stale values.
  useEffect(() => {
    if (open) {
      setTitle(project.title);
      setLevel(project.level ?? "UG");
      setAbstractText(project.abstractText ?? "");
    }
  }, [open, project.title, project.level, project.abstractText]);

  async function submit(e: React.FormEvent) {
    e.preventDefault();
    try {
      await update.mutateAsync({ title, level, abstractText });
      onClose();
    } catch {
      /* surfaced via update.error */
    }
  }

  const error = update.error instanceof ApiError ? update.error.message : null;

  return (
    <AnimatePresence>
      {open && (
        <motion.div
          className="fixed inset-0 z-50 grid place-items-center bg-cosmos-950/70 px-6 py-10 backdrop-blur-sm"
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          exit={{ opacity: 0 }}
          onClick={onClose}
        >
          <motion.div
            initial={{ opacity: 0, scale: 0.92, y: 20 }}
            animate={{ opacity: 1, scale: 1, y: 0 }}
            exit={{ opacity: 0, scale: 0.95, y: 10 }}
            transition={{ type: "spring", stiffness: 320, damping: 26 }}
            onClick={(e) => e.stopPropagation()}
            className="glass w-full max-w-md rounded-2xl p-7 shadow-card"
          >
            <div className="flex items-center justify-between">
              <h2 className="font-display text-xl font-bold text-white">Edit project</h2>
              <button onClick={onClose} className="text-slate-400 hover:text-white" aria-label="Close">
                <X size={18} />
              </button>
            </div>

            <form onSubmit={submit} className="mt-6 space-y-4">
              <Field label="Title" type="text" value={title} onChange={setTitle} />
              <label className="block">
                <span className="mb-1.5 block text-xs font-medium uppercase tracking-wider text-slate-400">
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
                          ? "border-accent/60 bg-accent/10 text-white shadow-glow"
                          : "border-white/10 text-slate-400 hover:border-white/30"
                      }`}
                    >
                      {l}
                    </button>
                  ))}
                </div>
              </label>
              <label className="block">
                <span className="mb-1.5 block text-xs font-medium uppercase tracking-wider text-slate-400">
                  Abstract
                </span>
                <textarea
                  value={abstractText}
                  onChange={(e) => setAbstractText(e.target.value)}
                  rows={4}
                  placeholder="A short summary of the research"
                  className="w-full resize-y rounded-xl border border-white/10 bg-white/[0.03] px-4 py-3 text-sm text-white placeholder:text-slate-500 outline-none transition-all focus:border-accent/60 focus:bg-white/[0.06] focus:shadow-glow"
                />
              </label>

              {error && <p className="text-sm text-rose-400">{error}</p>}

              <Button type="submit" size="lg" className="w-full" disabled={update.isPending || !title}>
                {update.isPending ? "Saving…" : "Save changes"}
              </Button>
            </form>
          </motion.div>
        </motion.div>
      )}
    </AnimatePresence>
  );
}
