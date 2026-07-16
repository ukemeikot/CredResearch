"use client";

import { AnimatePresence, motion } from "framer-motion";
import { RotateCcw, X } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Portal } from "@/components/ui/portal";
import { useRestoreSection, useSectionVersions } from "../api/use-documents";

export function VersionHistory({
  docId,
  sectionId,
  open,
  onClose,
  onRestored,
}: {
  docId: string;
  sectionId: string;
  open: boolean;
  onClose: () => void;
  onRestored: () => void;
}) {
  const versions = useSectionVersions(docId, sectionId, open);
  const restore = useRestoreSection(docId);

  async function onRestore(versionId: string) {
    await restore.mutateAsync({ sectionId, versionId });
    onRestored(); // re-init the editor with the restored content
    onClose();
  }

  const items = versions.data ?? [];

  return (
    <Portal>
    <AnimatePresence>
      {open && (
        <motion.div
          className="fixed inset-0 z-50 flex justify-end bg-cosmos-950/60 backdrop-blur-sm"
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          exit={{ opacity: 0 }}
          onClick={onClose}
        >
          <motion.aside
            initial={{ x: 360 }}
            animate={{ x: 0 }}
            exit={{ x: 360 }}
            transition={{ type: "spring", stiffness: 320, damping: 32 }}
            onClick={(e) => e.stopPropagation()}
            className="glass h-full w-full max-w-sm overflow-y-auto p-6"
          >
            <div className="flex items-center justify-between">
              <h3 className="font-display text-lg font-semibold text-white">Version history</h3>
              <button onClick={onClose} className="text-slate-400 hover:text-white" aria-label="Close">
                <X size={18} />
              </button>
            </div>

            {versions.isLoading ? (
              <p className="mt-6 text-sm text-slate-400">Loading…</p>
            ) : items.length === 0 ? (
              <p className="mt-6 text-sm text-slate-500">No saved versions yet. Edits appear here as you write.</p>
            ) : (
              <ul className="mt-6 space-y-3">
                {items.map((v) => (
                  <li
                    key={v.id}
                    className="flex items-center justify-between gap-3 rounded-xl border border-white/5 bg-white/[0.02] px-4 py-3"
                  >
                    <div>
                      <p className="text-sm text-white">Version {v.version}</p>
                      <p className="text-[11px] text-slate-500">{new Date(v.createdAt).toLocaleString()}</p>
                    </div>
                    <Button
                      size="sm"
                      variant="ghost"
                      disabled={restore.isPending}
                      onClick={() => onRestore(v.id)}
                    >
                      <RotateCcw size={14} /> Restore
                    </Button>
                  </li>
                ))}
              </ul>
            )}
          </motion.aside>
        </motion.div>
      )}
    </AnimatePresence>
    </Portal>
  );
}
