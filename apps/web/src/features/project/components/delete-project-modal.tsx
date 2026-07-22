"use client";

import { AnimatePresence, motion } from "framer-motion";
import { useRouter } from "next/navigation";
import { AlertTriangle, X } from "lucide-react";
import { useEffect, useState } from "react";
import { Button } from "@/components/ui/button";
import { Field } from "@/components/ui/field";
import { Portal } from "@/components/ui/portal";
import { ApiError, type ProjectSummary } from "@/lib/api";
import { useDeleteProject } from "../api/use-projects";

/**
 * Destructive confirmation for deleting a project. Requires the owner to type the exact project
 * title before the button unlocks, because the delete cascades to every document, paper,
 * questionnaire, survey response and review under the project and cannot be undone.
 */
export function DeleteProjectModal({
  project,
  open,
  onClose,
}: {
  project: ProjectSummary;
  open: boolean;
  onClose: () => void;
}) {
  const router = useRouter();
  const del = useDeleteProject(project.id);
  const [confirmText, setConfirmText] = useState("");

  useEffect(() => {
    if (open) setConfirmText("");
  }, [open]);

  const matches = confirmText.trim() === project.title.trim();

  async function submit(e: React.FormEvent) {
    e.preventDefault();
    if (!matches) return;
    try {
      await del.mutateAsync();
      onClose();
      router.push("/dashboard");
    } catch {
      /* surfaced via del.error */
    }
  }

  const error = del.error instanceof ApiError ? del.error.message : null;

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
              className="glass w-full max-w-md rounded-2xl p-7 shadow-card"
            >
              <div className="flex items-center justify-between">
                <h2 className="flex items-center gap-2 font-display text-xl font-bold text-rose-600">
                  <AlertTriangle size={20} /> Delete project
                </h2>
                <button onClick={onClose} className="text-slate-500 hover:text-slate-900" aria-label="Close">
                  <X size={18} />
                </button>
              </div>

              <p className="mt-4 text-sm leading-relaxed text-slate-600">
                This permanently deletes <span className="font-semibold text-slate-900">{project.title}</span> and
                everything in it — documents, references, questionnaires and survey responses, reviews, and the
                AI-use disclosure ledger. This <span className="font-semibold text-rose-600">cannot be undone</span>.
              </p>

              <form onSubmit={submit} className="mt-6 space-y-4">
                <Field
                  label={`Type the project name to confirm`}
                  type="text"
                  value={confirmText}
                  onChange={setConfirmText}
                  placeholder={project.title}
                />

                {error && <p className="text-sm text-rose-600">{error}</p>}

                <div className="flex gap-3">
                  <Button type="button" variant="outline" size="lg" className="flex-1" onClick={onClose}>
                    Cancel
                  </Button>
                  <Button
                    type="submit"
                    size="lg"
                    className="flex-1 !bg-rose-600 !text-white hover:!bg-rose-700 disabled:!bg-rose-600/40"
                    disabled={!matches || del.isPending}
                  >
                    {del.isPending ? "Deleting…" : "Delete forever"}
                  </Button>
                </div>
              </form>
            </motion.div>
          </motion.div>
        )}
      </AnimatePresence>
    </Portal>
  );
}
