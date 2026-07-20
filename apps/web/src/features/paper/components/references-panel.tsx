"use client";

import { useRef, useState } from "react";
import { AlertTriangle, Copy, Download, FileText, Loader2, Pencil, Search, Sparkles, Trash2, Upload } from "lucide-react";
import { GlassCard } from "@/components/ui/glass-card";
import { Button } from "@/components/ui/button";
import { api, ApiError, type Paper } from "@/lib/api";
import {
  useAskPapers,
  useDeletePaper,
  usePapers,
  useReferences,
  useSummarizePaper,
  useUpdatePaper,
  useUploadPaper,
} from "../api/use-papers";
import { LiteratureMatrix } from "./literature-matrix";

const STYLES = ["APA", "IEEE", "HARVARD"] as const;

export function ReferencesPanel({ projectId }: { projectId: string }) {
  const papers = usePapers(projectId);
  const upload = useUploadPaper(projectId);
  const [style, setStyle] = useState<(typeof STYLES)[number]>("APA");
  const refs = useReferences(projectId, style);
  const fileRef = useRef<HTMLInputElement>(null);
  const [uploadError, setUploadError] = useState<string | null>(null);

  async function onFile(e: React.ChangeEvent<HTMLInputElement>) {
    const file = e.target.files?.[0];
    e.target.value = ""; // allow re-selecting the same file
    if (!file) return;
    setUploadError(null);
    try {
      await upload.mutateAsync(file);
    } catch (err) {
      setUploadError(err instanceof ApiError ? err.message : "Upload failed");
    }
  }

  const list = papers.data ?? [];
  const references = refs.data?.references ?? [];

  return (
    <GlassCard className="p-6">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <h3 className="font-display text-lg font-semibold text-slate-900">Papers &amp; references</h3>
          <p className="text-xs text-slate-500">
            Upload papers (PDF/DOCX) — we extract the details and build your reference list.
          </p>
        </div>
        <input ref={fileRef} type="file" accept=".pdf,.docx" hidden onChange={onFile} />
        <Button size="sm" onClick={() => fileRef.current?.click()} disabled={upload.isPending}>
          {upload.isPending ? <Loader2 size={14} className="animate-spin" /> : <Upload size={14} />}
          {upload.isPending ? "Extracting…" : "Upload paper"}
        </Button>
      </div>
      {uploadError && <p className="mt-2 text-xs text-rose-600">{uploadError}</p>}

      {/* Uploaded papers */}
      <div className="mt-5 space-y-2">
        {papers.isLoading ? (
          <p className="text-sm text-slate-500">Loading…</p>
        ) : list.length === 0 ? (
          <p className="rounded-xl border border-dashed border-slate-200 px-4 py-6 text-center text-sm text-slate-500">
            No papers yet. Upload your first source to get started.
          </p>
        ) : (
          list.map((p) => <PaperRow key={p.id} projectId={projectId} paper={p} />)
        )}
      </div>

      {/* Reference list */}
      {list.length > 0 && (
        <div className="mt-8">
          <div className="flex flex-wrap items-center justify-between gap-3">
            <h4 className="text-sm font-semibold text-slate-900">Reference list</h4>
            <div className="flex items-center gap-2">
              <div className="flex rounded-lg border border-slate-200 p-0.5">
                {STYLES.map((s) => (
                  <button
                    key={s}
                    onClick={() => setStyle(s)}
                    className={`rounded-md px-2.5 py-1 text-xs transition-colors ${
                      style === s ? "bg-accent/20 text-slate-900" : "text-slate-500 hover:text-slate-900"
                    }`}
                  >
                    {s}
                  </button>
                ))}
              </div>
              <button
                onClick={() => navigator.clipboard?.writeText(references.map((r) => r.text).join("\n"))}
                className="inline-flex items-center gap-1.5 rounded-lg border border-slate-200 px-2.5 py-1.5 text-xs text-slate-600 hover:border-slate-300 hover:text-slate-900"
                title="Copy all"
              >
                <Copy size={13} /> Copy
              </button>
              <button
                onClick={() => api.exportReferences(projectId, "bibtex")}
                className="inline-flex items-center gap-1.5 rounded-lg border border-slate-200 px-2.5 py-1.5 text-xs text-slate-600 hover:border-slate-300 hover:text-slate-900"
                title="Download BibTeX"
              >
                <Download size={13} /> BibTeX
              </button>
              <button
                onClick={() => api.exportReferences(projectId, "ris")}
                className="inline-flex items-center gap-1.5 rounded-lg border border-slate-200 px-2.5 py-1.5 text-xs text-slate-600 hover:border-slate-300 hover:text-slate-900"
                title="Download RIS"
              >
                <Download size={13} /> RIS
              </button>
            </div>
          </div>
          <ol className="mt-3 space-y-2">
            {references.map((r) => (
              <li key={r.paperId} className="rounded-lg border border-slate-100 bg-slate-50 px-4 py-2.5 text-sm text-slate-600">
                {r.text}
              </li>
            ))}
          </ol>
        </div>
      )}

      {list.length > 0 && <AskPapers projectId={projectId} />}
      {list.length > 0 && <LiteratureMatrix papers={list} />}
    </GlassCard>
  );
}

/** Ask a question answered from the project's uploaded papers (RAG, FR-LIT-8). */
function AskPapers({ projectId }: { projectId: string }) {
  const ask = useAskPapers(projectId);
  const [q, setQ] = useState("");
  const [error, setError] = useState<string | null>(null);

  async function submit() {
    if (!q.trim()) return;
    setError(null);
    try {
      await ask.mutateAsync(q.trim());
    } catch (e) {
      setError(e instanceof ApiError ? e.message : "Couldn’t answer that");
    }
  }

  return (
    <div className="mt-8">
      <h4 className="flex items-center gap-2 text-sm font-semibold text-slate-900">
        <Search size={15} className="text-accent" /> Ask your papers
      </h4>
      <p className="mt-0.5 text-xs text-slate-500">Answers are grounded only in the papers you’ve uploaded.</p>
      <div className="mt-3 flex gap-2">
        <input
          value={q}
          onChange={(e) => setQ(e.target.value)}
          onKeyDown={(e) => e.key === "Enter" && submit()}
          placeholder="e.g. What methods are used to measure rural energy access?"
          className="min-w-0 flex-1 rounded-lg border border-slate-200 bg-slate-50 px-3 py-2 text-sm text-slate-900 outline-none focus:border-accent/50"
        />
        <Button size="sm" onClick={submit} disabled={ask.isPending || !q.trim()}>
          {ask.isPending ? <Loader2 size={14} className="animate-spin" /> : "Ask"}
        </Button>
      </div>
      {error && <p className="mt-2 text-xs text-rose-600">{error}</p>}
      {ask.data && (
        <div className="mt-3 rounded-xl border border-accent/15 bg-accent/[0.03] p-4">
          <p className="whitespace-pre-wrap text-sm text-slate-700">{ask.data.answer}</p>
          {ask.data.used_sources.length > 0 && (
            <p className="mt-2 flex flex-wrap gap-1.5">
              {ask.data.used_sources.map((sname, i) => (
                <span key={i} className="rounded-full border border-slate-200 px-2 py-0.5 text-[10px] text-slate-500">
                  {sname}
                </span>
              ))}
            </p>
          )}
        </div>
      )}
    </div>
  );
}

function SummaryList({ label, items }: { label: string; items: string[] }) {
  if (!items || items.length === 0) return null;
  return (
    <div>
      <p className="text-[10px] font-semibold uppercase tracking-wider text-accent/80">{label}</p>
      <ul className="mt-0.5 list-inside list-disc text-slate-500">
        {items.map((it, i) => (
          <li key={i}>{it}</li>
        ))}
      </ul>
    </div>
  );
}

function PaperRow({ projectId, paper }: { projectId: string; paper: Paper }) {
  const update = useUpdatePaper(projectId);
  const del = useDeletePaper(projectId);
  const summarize = useSummarizePaper(projectId);
  const [editing, setEditing] = useState(false);
  const [summaryError, setSummaryError] = useState<string | null>(null);
  const summary = paper.summary ?? null;
  const [form, setForm] = useState({
    title: paper.title ?? "",
    authors: paper.authors ?? "",
    year: paper.year ? String(paper.year) : "",
    journal: paper.journal ?? "",
    doi: paper.doi ?? "",
  });
  const lowConfidence = paper.extractionStatus === "LOW_CONFIDENCE";

  async function save() {
    await update.mutateAsync({
      id: paper.id,
      body: {
        title: form.title || undefined,
        authors: form.authors || undefined,
        year: form.year ? Number(form.year) : undefined,
        journal: form.journal || undefined,
        doi: form.doi || undefined,
      },
    });
    setEditing(false);
  }

  const field = (label: string, key: keyof typeof form, placeholder = "") => (
    <label className="block">
      <span className="text-[10px] uppercase tracking-wider text-slate-500">{label}</span>
      <input
        value={form[key]}
        onChange={(e) => setForm({ ...form, [key]: e.target.value })}
        placeholder={placeholder}
        className="mt-0.5 w-full rounded-lg border border-slate-200 bg-slate-50 px-2.5 py-1.5 text-sm text-slate-900 outline-none focus:border-accent/50"
      />
    </label>
  );

  return (
    <div className="rounded-xl border border-slate-100 bg-slate-50 px-4 py-3">
      <div className="flex items-start justify-between gap-3">
        <div className="min-w-0">
          <p className="flex items-center gap-2 text-sm font-medium text-slate-900">
            <FileText size={14} className="shrink-0 text-slate-500" />
            <span className="truncate">{paper.title || paper.filename || "Untitled"}</span>
          </p>
          <p className="mt-0.5 truncate text-xs text-slate-500">
            {[paper.authors, paper.year, paper.journal].filter(Boolean).join(" · ") || "No metadata extracted"}
          </p>
          {lowConfidence && !editing && (
            <p className="mt-1 inline-flex items-center gap-1 text-[11px] text-amber-600">
              <AlertTriangle size={11} /> Low-confidence extraction — please review
            </p>
          )}
        </div>
        <div className="flex shrink-0 items-center gap-1">
          <button
            onClick={async () => {
              setSummaryError(null);
              try {
                await summarize.mutateAsync(paper.id);
              } catch (e) {
                setSummaryError(e instanceof ApiError ? e.message : "Could not summarize");
              }
            }}
            disabled={summarize.isPending}
            className="inline-flex items-center gap-1 rounded-lg border border-accent/40 px-2 py-1.5 text-[11px] text-accent hover:bg-accent/10 disabled:opacity-50"
            title="AI summary (method, findings, gaps)"
          >
            {summarize.isPending ? <Loader2 size={12} className="animate-spin" /> : <Sparkles size={12} />}
            Summarize
          </button>
          <button onClick={() => setEditing((v) => !v)} className="rounded-lg border border-slate-200 p-1.5 text-slate-500 hover:border-slate-300 hover:text-slate-900" title="Edit details">
            <Pencil size={13} />
          </button>
          <button onClick={() => del.mutate(paper.id)} className="rounded-lg border border-slate-200 p-1.5 text-slate-500 hover:border-rose-400/40 hover:text-rose-600" title="Delete">
            <Trash2 size={13} />
          </button>
        </div>
      </div>

      {summaryError && <p className="mt-1 text-[11px] text-rose-600">{summaryError}</p>}

      {summary && (summary.summary || summary.findings.length > 0) && (
        <div className="mt-3 space-y-2 rounded-lg border border-accent/15 bg-accent/[0.03] p-3 text-xs">
          {summary.summary && <p className="text-slate-600">{summary.summary}</p>}
          <SummaryList label="Methodology" items={summary.methodology ? [summary.methodology] : []} />
          <SummaryList label="Findings" items={summary.findings} />
          <SummaryList label="Limitations" items={summary.limitations} />
          <SummaryList label="Research gaps" items={summary.gaps} />
        </div>
      )}

      {editing && (
        <div className="mt-3 grid grid-cols-2 gap-2">
          <div className="col-span-2">{field("Title", "title")}</div>
          {field("Authors", "authors", "Surname, A.")}
          {field("Year", "year", "2024")}
          {field("Journal / source", "journal")}
          {field("DOI", "doi")}
          <div className="col-span-2 mt-1 flex justify-end gap-2">
            <button onClick={() => setEditing(false)} className="px-2 text-xs text-slate-500 hover:text-slate-900">Cancel</button>
            <Button size="sm" onClick={save} disabled={update.isPending}>
              {update.isPending ? "Saving…" : "Save"}
            </Button>
          </div>
        </div>
      )}
    </div>
  );
}
