"use client";

import { useRef, useState } from "react";
import { BarChart3, FileSpreadsheet, Loader2, Sparkles, Upload } from "lucide-react";
import { GlassCard } from "@/components/ui/glass-card";
import { Button } from "@/components/ui/button";
import { api, ApiError, type AnalysisColumn, type AnalysisResult } from "@/lib/api";

/** Descriptive data analysis (Phase 8, FR-DATA): upload a CSV → stats + charts → grounded AI text. */
export function DataAnalysisPanel({ projectId, projectTitle }: { projectId: string; projectTitle: string }) {
  const fileRef = useRef<HTMLInputElement>(null);
  const [result, setResult] = useState<AnalysisResult | null>(null);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [interpretation, setInterpretation] = useState<string | null>(null);
  const [chapter4, setChapter4] = useState<string | null>(null);
  const [aiBusy, setAiBusy] = useState<null | "interpret" | "chapter4">(null);

  async function onFile(e: React.ChangeEvent<HTMLInputElement>) {
    const file = e.target.files?.[0];
    e.target.value = "";
    if (!file) return;
    setError(null); setInterpretation(null); setChapter4(null); setBusy(true);
    try {
      const r = await api.analyzeCsv(projectId, file);
      if (r.error) setError(r.error);
      setResult(r);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Analysis failed");
    } finally {
      setBusy(false);
    }
  }

  async function runAi(kind: "interpret" | "chapter4") {
    if (!result) return;
    setAiBusy(kind); setError(null);
    try {
      if (kind === "interpret") {
        const r = await api.interpretData(projectId, projectTitle, result);
        setInterpretation(r.interpretation);
      } else {
        const r = await api.chapter4Draft(projectId, projectTitle, result);
        setChapter4(r.draft);
      }
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "AI step failed");
    } finally {
      setAiBusy(null);
    }
  }

  return (
    <GlassCard className="p-6">
      <div className="flex items-center justify-between gap-3">
        <h3 className="flex items-center gap-2 font-display text-lg font-semibold text-white">
          <BarChart3 size={18} className="text-accent" /> Data analysis
        </h3>
        <input ref={fileRef} type="file" accept=".csv" hidden onChange={onFile} />
        <Button size="sm" onClick={() => fileRef.current?.click()} disabled={busy}>
          {busy ? <Loader2 size={14} className="animate-spin" /> : <Upload size={14} />} Upload CSV
        </Button>
      </div>
      <p className="mt-1 text-xs text-slate-400">Upload survey/response data (CSV) for descriptive statistics.</p>
      {error && <p className="mt-2 text-xs text-rose-400">{error}</p>}

      {result && !result.error && (
        <>
          <p className="mt-4 text-xs text-slate-400">
            <FileSpreadsheet size={12} className="mr-1 inline" />
            {result.row_count} rows · {result.column_count} columns
          </p>
          <div className="mt-3 space-y-3">
            {result.columns.map((c) => <ColumnCard key={c.name} col={c} />)}
          </div>

          <div className="mt-5 flex flex-wrap gap-2">
            <Button size="sm" variant="outline" onClick={() => runAi("interpret")} disabled={aiBusy !== null}>
              {aiBusy === "interpret" ? <Loader2 size={14} className="animate-spin" /> : <Sparkles size={14} />} AI interpretation
            </Button>
            <Button size="sm" variant="outline" onClick={() => runAi("chapter4")} disabled={aiBusy !== null}>
              {aiBusy === "chapter4" ? <Loader2 size={14} className="animate-spin" /> : <Sparkles size={14} />} Chapter 4 draft
            </Button>
          </div>

          {interpretation && (
            <div className="mt-3 whitespace-pre-wrap rounded-xl border border-accent/15 bg-accent/[0.03] p-4 text-sm text-slate-200">
              {interpretation}
            </div>
          )}
          {chapter4 && (
            <div className="mt-3 rounded-xl border border-white/10 bg-white/[0.02] p-4">
              <p className="text-xs uppercase tracking-wider text-slate-500">Chapter 4 — draft</p>
              <div className="mt-1 whitespace-pre-wrap text-sm text-slate-300">{chapter4}</div>
            </div>
          )}
        </>
      )}
    </GlassCard>
  );
}

function ColumnCard({ col }: { col: AnalysisColumn }) {
  return (
    <div className="rounded-xl border border-white/5 bg-white/[0.02] px-4 py-3">
      <div className="flex items-center justify-between gap-2">
        <span className="text-sm font-medium text-white">{col.name}</span>
        <span className="rounded-full border border-white/10 px-2 py-0.5 text-[10px] uppercase tracking-wider text-slate-400">
          {col.type}{col.missing > 0 ? ` · ${col.missing} missing` : ""}
        </span>
      </div>
      {col.type === "numeric" && col.stats && (
        <div className="mt-1.5 flex flex-wrap gap-x-4 gap-y-0.5 text-xs text-slate-400">
          <span>mean {col.stats.mean}</span>
          <span>median {col.stats.median}</span>
          <span>std {col.stats.std}</span>
          <span>min {col.stats.min}</span>
          <span>max {col.stats.max}</span>
        </div>
      )}
      {col.type === "categorical" && col.frequencies && (
        <div className="mt-2 space-y-1">
          {col.frequencies.slice(0, 6).map((f) => (
            <div key={f.value} className="flex items-center gap-2 text-xs">
              <span className="w-28 truncate text-slate-300">{f.value}</span>
              <div className="h-2 flex-1 overflow-hidden rounded-full bg-white/5">
                <div className="h-full rounded-full bg-accent/60" style={{ width: `${f.pct}%` }} />
              </div>
              <span className="w-14 text-right text-slate-500">{f.count} ({f.pct}%)</span>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
