"use client";

import { AnimatePresence, motion } from "framer-motion";
import { Check, Sparkles, X } from "lucide-react";
import { useState } from "react";
import { useAiTopics } from "@/features/document/api/use-ai";
import { Button } from "@/components/ui/button";
import { Field } from "@/components/ui/field";
import { Portal } from "@/components/ui/portal";
import { ApiError, type AiTopic } from "@/lib/api";
import { useCreateProject } from "../api/use-projects";

const LEVELS = ["UG", "MSc", "PhD"];

const FEASIBILITY_STYLE: Record<AiTopic["feasibility"], string> = {
  HIGH: "border-emerald-400/40 text-emerald-300",
  MEDIUM: "border-amber-400/40 text-amber-300",
  LOW: "border-rose-400/40 text-rose-300",
};

export function CreateProjectModal({ open, onClose }: { open: boolean; onClose: () => void }) {
  const create = useCreateProject();
  const topics = useAiTopics();
  const [title, setTitle] = useState("");
  const [level, setLevel] = useState("UG");
  const [aiOpen, setAiOpen] = useState(false);
  const [field, setField] = useState("");
  const [interests, setInterests] = useState("");

  async function submit(e: React.FormEvent) {
    e.preventDefault();
    try {
      await create.mutateAsync({ title, level });
      reset();
      onClose();
    } catch {
      /* error surfaced via create.error */
    }
  }

  function reset() {
    setTitle("");
    setField("");
    setInterests("");
    setAiOpen(false);
    topics.reset();
  }

  async function suggest() {
    if (!field.trim()) return;
    try {
      await topics.mutateAsync({ field: field.trim(), interests: interests.trim() || undefined, level });
    } catch {
      /* surfaced via topics.error */
    }
  }

  const error =
    create.error instanceof ApiError ? create.error.message : create.isError ? "Could not create project" : null;
  const aiError =
    topics.error instanceof ApiError ? topics.error.message : topics.isError ? "Could not get suggestions" : null;
  const suggestions = topics.data?.topics ?? [];

  return (
    <Portal>
      <AnimatePresence>
        {open && (
          <motion.div
            className="fixed inset-0 z-50 grid place-items-center overflow-y-auto bg-cosmos-950/70 px-6 py-10 backdrop-blur-sm"
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            onClick={() => {
              reset();
              onClose();
            }}
          >
            <motion.div
              initial={{ opacity: 0, scale: 0.92, y: 20 }}
              animate={{ opacity: 1, scale: 1, y: 0 }}
              exit={{ opacity: 0, scale: 0.95, y: 10 }}
              transition={{ type: "spring", stiffness: 320, damping: 26 }}
              onClick={(e) => e.stopPropagation()}
              className="glass my-auto w-full max-w-md rounded-2xl p-7 shadow-card"
            >
              <div className="flex items-center justify-between">
                <h2 className="font-display text-xl font-bold text-white">New project</h2>
                <button
                  onClick={() => {
                    reset();
                    onClose();
                  }}
                  className="text-slate-400 hover:text-white"
                  aria-label="Close"
                >
                  <X size={18} />
                </button>
              </div>

              <form onSubmit={submit} className="mt-6 space-y-4">
                <Field
                  label="Title"
                  type="text"
                  value={title}
                  onChange={setTitle}
                  placeholder="Working title (you can change this later)"
                />
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

                {/* AI topic assistant */}
                <div className="rounded-xl border border-accent/20 bg-accent/[0.04]">
                  <button
                    type="button"
                    onClick={() => setAiOpen((v) => !v)}
                    className="flex w-full items-center gap-2 px-4 py-2.5 text-left text-sm text-accent"
                  >
                    <Sparkles size={15} /> Not sure what to research? Get AI topic ideas
                  </button>
                  {aiOpen && (
                    <div className="space-y-3 border-t border-accent/10 px-4 py-3">
                      <Field
                        label="Field of study"
                        type="text"
                        value={field}
                        onChange={setField}
                        placeholder="e.g. Renewable energy, Public health, Fintech"
                      />
                      <Field
                        label="Interests (optional)"
                        type="text"
                        value={interests}
                        onChange={setInterests}
                        placeholder="e.g. rural access, machine learning"
                      />
                      <Button
                        type="button"
                        size="sm"
                        variant="outline"
                        className="w-full"
                        onClick={suggest}
                        disabled={topics.isPending || !field.trim()}
                      >
                        {topics.isPending ? "Thinking…" : "Suggest topics"}
                      </Button>
                      {aiError && <p className="text-xs text-rose-400">{aiError}</p>}
                      {suggestions.length > 0 && (
                        <ul className="space-y-2">
                          {suggestions.map((t, i) => (
                            <li
                              key={i}
                              className="rounded-lg border border-white/10 bg-white/[0.02] p-3"
                            >
                              <div className="flex items-start justify-between gap-2">
                                <p className="text-sm font-medium text-white">{t.title}</p>
                                <span
                                  className={`shrink-0 rounded-full border px-2 py-0.5 text-[10px] uppercase tracking-wider ${FEASIBILITY_STYLE[t.feasibility]}`}
                                >
                                  {t.feasibility}
                                </span>
                              </div>
                              {t.rationale && (
                                <p className="mt-1 line-clamp-2 text-xs text-slate-400">{t.rationale}</p>
                              )}
                              <button
                                type="button"
                                onClick={() => {
                                  setTitle(t.title);
                                  setAiOpen(false);
                                }}
                                className="mt-2 inline-flex items-center gap-1 text-xs text-accent hover:underline"
                              >
                                <Check size={12} /> Use this title
                              </button>
                            </li>
                          ))}
                        </ul>
                      )}
                    </div>
                  )}
                </div>

                {error && <p className="text-sm text-rose-400">{error}</p>}

                <Button type="submit" size="lg" className="w-full" disabled={create.isPending || !title}>
                  {create.isPending ? "Creating…" : "Create project"}
                </Button>
              </form>
            </motion.div>
          </motion.div>
        )}
      </AnimatePresence>
    </Portal>
  );
}
