"use client";

import { AnimatePresence, motion } from "framer-motion";
import { useRouter } from "next/navigation";
import { useState } from "react";
import { X } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Field } from "@/components/ui/field";
import { ApiError } from "@/lib/api";
import { useCreateDocument, useTemplates } from "../api/use-documents";

export function CreateDocumentModal({
  projectId,
  open,
  onClose,
}: {
  projectId: string;
  open: boolean;
  onClose: () => void;
}) {
  const router = useRouter();
  const templates = useTemplates();
  const create = useCreateDocument(projectId);
  const [templateId, setTemplateId] = useState<string | null>(null);
  const [title, setTitle] = useState("");

  async function submit(e: React.FormEvent) {
    e.preventDefault();
    if (!templateId) return;
    try {
      const detail = await create.mutateAsync({ templateId, title: title || undefined });
      onClose();
      router.push(`/projects/${projectId}/documents/${detail.document.id}`);
    } catch {
      /* surfaced via create.error */
    }
  }

  const list = templates.data ?? [];
  const error = create.error instanceof ApiError ? create.error.message : null;

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
            className="glass w-full max-w-lg rounded-2xl p-7 shadow-card"
          >
            <div className="flex items-center justify-between">
              <h2 className="font-display text-xl font-bold text-white">New document</h2>
              <button onClick={onClose} className="text-slate-400 hover:text-white" aria-label="Close">
                <X size={18} />
              </button>
            </div>

            <form onSubmit={submit} className="mt-6 space-y-5">
              <div>
                <span className="mb-2 block text-xs font-medium uppercase tracking-wider text-slate-400">
                  Choose a template
                </span>
                {templates.isLoading ? (
                  <p className="text-sm text-slate-400">Loading templates…</p>
                ) : (
                  <div className="grid gap-2 sm:grid-cols-2">
                    {list.map((t) => (
                      <button
                        type="button"
                        key={t.id}
                        onClick={() => setTemplateId(t.id)}
                        className={`rounded-xl border px-4 py-3 text-left transition-all ${
                          templateId === t.id
                            ? "border-accent/60 bg-accent/10 shadow-glow"
                            : "border-white/10 hover:border-white/30"
                        }`}
                      >
                        <span className="block text-sm font-medium text-white">{t.name}</span>
                        <span className="mt-0.5 block text-[11px] uppercase tracking-wider text-slate-500">
                          {t.level} · {t.citationStyle}
                        </span>
                      </button>
                    ))}
                  </div>
                )}
              </div>

              <Field
                label="Title (optional)"
                type="text"
                value={title}
                onChange={setTitle}
                placeholder="Defaults to the template name"
                required={false}
              />

              {error && <p className="text-sm text-rose-400">{error}</p>}

              <Button type="submit" size="lg" className="w-full" disabled={create.isPending || !templateId}>
                {create.isPending ? "Creating…" : "Create document"}
              </Button>
            </form>
          </motion.div>
        </motion.div>
      )}
    </AnimatePresence>
  );
}
