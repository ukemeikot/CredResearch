"use client";

import { AnimatePresence, motion } from "framer-motion";
import { X } from "lucide-react";
import { useState } from "react";
import { Button } from "@/components/ui/button";
import { Field } from "@/components/ui/field";
import { api, ApiError, type ProjectSummary } from "@/lib/api";

const LEVELS = ["UG", "MSc", "PhD"];

export function CreateProjectModal({
  open,
  onClose,
  onCreated,
}: {
  open: boolean;
  onClose: () => void;
  onCreated: (p: ProjectSummary) => void;
}) {
  const [title, setTitle] = useState("");
  const [level, setLevel] = useState("UG");
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  async function submit(e: React.FormEvent) {
    e.preventDefault();
    setError(null);
    setLoading(true);
    try {
      const created = await api.createProject({ title, level });
      setTitle("");
      onCreated(created);
      onClose();
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Could not create project");
    } finally {
      setLoading(false);
    }
  }

  return (
    <AnimatePresence>
      {open && (
        <motion.div
          className="fixed inset-0 z-50 grid place-items-center bg-cosmos-950/70 px-6 backdrop-blur-sm"
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
              <h2 className="font-display text-xl font-bold text-white">New project</h2>
              <button onClick={onClose} className="text-slate-400 hover:text-white" aria-label="Close">
                <X size={18} />
              </button>
            </div>

            <form onSubmit={submit} className="mt-6 space-y-4">
              <Field label="Title" type="text" value={title} onChange={setTitle} placeholder="Working title (you can change this later)" />
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

              {error && <p className="text-sm text-rose-400">{error}</p>}

              <Button type="submit" size="lg" className="w-full" disabled={loading || !title}>
                {loading ? "Creating…" : "Create project"}
              </Button>
            </form>
          </motion.div>
        </motion.div>
      )}
    </AnimatePresence>
  );
}
