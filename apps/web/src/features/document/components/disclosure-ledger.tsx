"use client";

import { AnimatePresence, motion } from "framer-motion";
import { ShieldCheck, X } from "lucide-react";
import { Portal } from "@/components/ui/portal";
import { useDisclosure } from "../api/use-ai";

/** Read-only view of a document's AI-Use Disclosure Ledger (FR-LEDGER). */
export function DisclosureLedger({
  docId,
  open,
  onClose,
}: {
  docId: string;
  open: boolean;
  onClose: () => void;
}) {
  const q = useDisclosure(docId, open);
  const items = q.data ?? [];

  return (
    <Portal>
      <AnimatePresence>
        {open && (
          <motion.div
            className="fixed inset-0 z-50 flex justify-end bg-slate-900/40 backdrop-blur-sm"
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            onClick={onClose}
          >
            <motion.aside
              initial={{ x: 380 }}
              animate={{ x: 0 }}
              exit={{ x: 380 }}
              transition={{ type: "spring", stiffness: 320, damping: 32 }}
              onClick={(e) => e.stopPropagation()}
              className="glass h-full w-full max-w-md overflow-y-auto p-6"
            >
              <div className="flex items-center justify-between">
                <h3 className="flex items-center gap-2 font-display text-lg font-semibold text-slate-900">
                  <ShieldCheck size={18} className="text-accent" /> AI-Use Disclosure
                </h3>
                <button onClick={onClose} className="text-slate-500 hover:text-slate-900" aria-label="Close">
                  <X size={18} />
                </button>
              </div>
              <p className="mt-2 text-xs text-slate-500">
                An append-only, tamper-evident record of AI assistance used in this document.
              </p>

              {q.isLoading ? (
                <p className="mt-6 text-sm text-slate-500">Loading…</p>
              ) : items.length === 0 ? (
                <p className="mt-6 text-sm text-slate-500">No AI assistance recorded yet.</p>
              ) : (
                <ul className="mt-6 space-y-3">
                  {items.map((e) => (
                    <li key={e.id} className="rounded-xl border border-slate-100 bg-slate-50 px-4 py-3">
                      <div className="flex items-center justify-between gap-2">
                        <span className="text-sm text-slate-900">{e.featureKey.replace(/-/g, " ")}</span>
                        <span className="rounded-full border border-slate-200 px-2 py-0.5 text-[10px] uppercase tracking-wider text-slate-500">
                          {e.action}
                        </span>
                      </div>
                      {e.suggestionSummary && (
                        <p className="mt-1.5 line-clamp-3 text-xs text-slate-500">{e.suggestionSummary}</p>
                      )}
                      <p className="mt-1.5 font-mono text-[10px] text-slate-600">
                        {new Date(e.createdAt).toLocaleString()} · {e.entryHash.slice(0, 12)}…
                      </p>
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
