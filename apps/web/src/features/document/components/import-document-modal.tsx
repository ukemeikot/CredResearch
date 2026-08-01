"use client";

import { AnimatePresence, motion } from "framer-motion";
import { useRouter } from "next/navigation";
import { useRef, useState } from "react";
import { FileUp, X } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Field } from "@/components/ui/field";
import { Portal } from "@/components/ui/portal";
import { ApiError } from "@/lib/api";
import { useImportDocument, useTemplates } from "../api/use-documents";

/** Upload a .docx/.pdf you're already working on and turn it into an editable document. */
export function ImportDocumentModal({
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
  const importDoc = useImportDocument(projectId);
  const [templateId, setTemplateId] = useState<string | null>(null);
  const [title, setTitle] = useState("");
  const [file, setFile] = useState<File | null>(null);
  const inputRef = useRef<HTMLInputElement>(null);

  async function submit(e: React.FormEvent) {
    e.preventDefault();
    if (!templateId || !file) return;
    try {
      const detail = await importDoc.mutateAsync({ templateId, title: title || undefined, file });
      onClose();
      setFile(null);
      setTitle("");
      router.push(`/projects/${projectId}/documents/${detail.document.id}`);
    } catch {
      /* surfaced via importDoc.error */
    }
  }

  const list = templates.data ?? [];
  const error = importDoc.error instanceof ApiError ? importDoc.error.message : null;

  return (
    <Portal>
      <AnimatePresence>
        {open && (
          <motion.div
            className="fixed inset-0 z-50 grid place-items-center bg-slate-900/40 px-6 py-10 backdrop-blur-sm"
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
                <h2 className="font-display text-xl font-bold text-slate-900">Import a document</h2>
                <button onClick={onClose} className="text-slate-500 hover:text-slate-900" aria-label="Close">
                  <X size={18} />
                </button>
              </div>
              <p className="mt-2 text-sm text-slate-500">
                Upload a Word (.docx) or PDF you’re already working on. Word keeps its headings and
                formatting; PDF comes in as text you can then structure.
              </p>

              <form onSubmit={submit} className="mt-6 space-y-5">
                {/* File picker */}
                <button
                  type="button"
                  onClick={() => inputRef.current?.click()}
                  className={`flex w-full items-center gap-3 rounded-xl border border-dashed px-4 py-4 text-left transition-all ${
                    file ? "border-accent/60 bg-accent/5" : "border-slate-300 hover:border-slate-400"
                  }`}
                >
                  <FileUp size={20} className="shrink-0 text-accent" />
                  <span className="min-w-0">
                    <span className="block truncate text-sm font-medium text-slate-900">
                      {file ? file.name : "Choose a .docx or .pdf file"}
                    </span>
                    <span className="text-[11px] uppercase tracking-wider text-slate-500">
                      {file ? `${(file.size / 1024).toFixed(0)} KB` : "Word or PDF"}
                    </span>
                  </span>
                </button>
                <input
                  ref={inputRef}
                  type="file"
                  accept=".docx,.pdf,application/pdf,application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                  className="hidden"
                  onChange={(e) => {
                    const f = e.target.files?.[0] ?? null;
                    setFile(f);
                    if (f && !title) setTitle(f.name.replace(/\.(docx|pdf)$/i, ""));
                  }}
                />

                <div>
                  <span className="mb-2 block text-xs font-medium uppercase tracking-wider text-slate-500">
                    Formatting template
                  </span>
                  {templates.isLoading ? (
                    <p className="text-sm text-slate-500">Loading templates…</p>
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
                              : "border-slate-200 hover:border-slate-300"
                          }`}
                        >
                          <span className="block text-sm font-medium text-slate-900">{t.name}</span>
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
                  placeholder="Defaults to the file name"
                  required={false}
                />

                {error && <p className="text-sm text-rose-600">{error}</p>}

                <Button
                  type="submit"
                  size="lg"
                  className="w-full"
                  disabled={importDoc.isPending || !templateId || !file}
                >
                  {importDoc.isPending ? "Importing…" : "Import & open"}
                </Button>
              </form>
            </motion.div>
          </motion.div>
        )}
      </AnimatePresence>
    </Portal>
  );
}
