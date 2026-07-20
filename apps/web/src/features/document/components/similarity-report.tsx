"use client";

import { useState } from "react";
import { AnimatePresence, motion } from "framer-motion";
import { AlertTriangle, ScanSearch, X } from "lucide-react";
import { Portal } from "@/components/ui/portal";
import { Button } from "@/components/ui/button";
import { api, ApiError, type SimilarityReport as Report } from "@/lib/api";

/** Internal similarity pre-check report (Phase 9, FR-SIM). Explicitly not a Turnitin equivalent. */
export function SimilarityReport({ docId, open, onClose }: { docId: string; open: boolean; onClose: () => void }) {
  const [report, setReport] = useState<Report | null>(null);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function run() {
    setBusy(true);
    setError(null);
    try {
      setReport(await api.similarityCheck(docId));
    } catch (e) {
      setError(e instanceof ApiError ? e.message : "Similarity check failed");
    } finally {
      setBusy(false);
    }
  }

  const scoreColor = report && report.overall_score >= 30 ? "text-rose-600" : report && report.overall_score >= 10 ? "text-amber-600" : "text-emerald-600";

  return (
    <Portal>
      <AnimatePresence>
        {open && (
          <motion.div
            className="fixed inset-0 z-50 flex justify-end bg-slate-900/40 backdrop-blur-sm"
            initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }} onClick={onClose}
          >
            <motion.aside
              initial={{ x: 420 }} animate={{ x: 0 }} exit={{ x: 420 }}
              transition={{ type: "spring", stiffness: 320, damping: 32 }}
              onClick={(e) => e.stopPropagation()}
              className="glass h-full w-full max-w-lg overflow-y-auto p-6"
            >
              <div className="flex items-center justify-between">
                <h3 className="flex items-center gap-2 font-display text-lg font-semibold text-slate-900">
                  <ScanSearch size={18} className="text-accent" /> Similarity check
                </h3>
                <button onClick={onClose} className="text-slate-500 hover:text-slate-900" aria-label="Close"><X size={18} /></button>
              </div>

              {!report && (
                <div className="mt-6 text-center">
                  <p className="text-sm text-slate-500">Check this document for repeated passages against your other project documents.</p>
                  <Button className="mt-4" onClick={run} disabled={busy}>
                    {busy ? "Checking…" : "Run internal check"}
                  </Button>
                </div>
              )}
              {error && <p className="mt-3 text-xs text-rose-600">{error}</p>}

              {report && (
                <div className="mt-5">
                  <div className="rounded-2xl border border-slate-200 bg-slate-50 p-5 text-center">
                    <p className={`font-display text-4xl font-bold ${scoreColor}`}>{report.overall_score}%</p>
                    <p className="mt-1 text-xs text-slate-500">
                      {report.matched_paragraphs} of {report.checked_paragraphs} paragraphs matched your other documents
                    </p>
                  </div>
                  <p className="mt-3 flex items-start gap-2 rounded-lg border border-amber-400/20 bg-amber-400/[0.06] p-3 text-[11px] text-amber-700/90">
                    <AlertTriangle size={13} className="mt-0.5 shrink-0" /> {report.disclaimer}
                  </p>

                  <div className="mt-4 space-y-2">
                    {report.matches.length === 0 ? (
                      <p className="text-sm text-slate-500">No significant repeated passages found.</p>
                    ) : (
                      report.matches.map((m, i) => (
                        <div key={i} className="rounded-xl border border-slate-200 bg-slate-50 px-4 py-3">
                          <div className="flex items-center justify-between gap-2">
                            <span className="truncate text-xs text-slate-600">↔ {m.source_title}</span>
                            <span className="shrink-0 rounded-full border border-slate-200 px-2 py-0.5 text-[10px] text-slate-500">
                              {Math.round(m.score * 100)}% match
                            </span>
                          </div>
                          <p className="mt-1.5 line-clamp-2 border-l-2 border-slate-200 pl-2 text-xs text-slate-500">{m.target_snippet}</p>
                          {m.citation_risk && (
                            <p className="mt-1.5 inline-flex items-center gap-1 text-[11px] text-rose-600">
                              <AlertTriangle size={11} /> Citation risk — matched text has no nearby citation
                            </p>
                          )}
                        </div>
                      ))
                    )}
                  </div>
                  <Button variant="outline" size="sm" className="mt-4" onClick={run} disabled={busy}>
                    {busy ? "Checking…" : "Re-run check"}
                  </Button>
                </div>
              )}
            </motion.aside>
          </motion.div>
        )}
      </AnimatePresence>
    </Portal>
  );
}
