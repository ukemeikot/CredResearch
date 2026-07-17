"use client";

import { useRef, useState } from "react";
import { AlertTriangle, Copy, Download, FileText, Loader2, Pencil, Trash2, Upload } from "lucide-react";
import { GlassCard } from "@/components/ui/glass-card";
import { Button } from "@/components/ui/button";
import { api, ApiError, type Paper } from "@/lib/api";
import { useDeletePaper, usePapers, useReferences, useUpdatePaper, useUploadPaper } from "../api/use-papers";

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
          <h3 className="font-display text-lg font-semibold text-white">Papers &amp; references</h3>
          <p className="text-xs text-slate-400">
            Upload papers (PDF/DOCX) — we extract the details and build your reference list.
          </p>
        </div>
        <input ref={fileRef} type="file" accept=".pdf,.docx" hidden onChange={onFile} />
        <Button size="sm" onClick={() => fileRef.current?.click()} disabled={upload.isPending}>
          {upload.isPending ? <Loader2 size={14} className="animate-spin" /> : <Upload size={14} />}
          {upload.isPending ? "Extracting…" : "Upload paper"}
        </Button>
      </div>
      {uploadError && <p className="mt-2 text-xs text-rose-400">{uploadError}</p>}

      {/* Uploaded papers */}
      <div className="mt-5 space-y-2">
        {papers.isLoading ? (
          <p className="text-sm text-slate-500">Loading…</p>
        ) : list.length === 0 ? (
          <p className="rounded-xl border border-dashed border-white/10 px-4 py-6 text-center text-sm text-slate-500">
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
            <h4 className="text-sm font-semibold text-white">Reference list</h4>
            <div className="flex items-center gap-2">
              <div className="flex rounded-lg border border-white/10 p-0.5">
                {STYLES.map((s) => (
                  <button
                    key={s}
                    onClick={() => setStyle(s)}
                    className={`rounded-md px-2.5 py-1 text-xs transition-colors ${
                      style === s ? "bg-accent/20 text-white" : "text-slate-400 hover:text-white"
                    }`}
                  >
                    {s}
                  </button>
                ))}
              </div>
              <button
                onClick={() => navigator.clipboard?.writeText(references.map((r) => r.text).join("\n"))}
                className="inline-flex items-center gap-1.5 rounded-lg border border-white/10 px-2.5 py-1.5 text-xs text-slate-300 hover:border-white/30 hover:text-white"
                title="Copy all"
              >
                <Copy size={13} /> Copy
              </button>
              <button
                onClick={() => api.exportReferences(projectId, "bibtex")}
                className="inline-flex items-center gap-1.5 rounded-lg border border-white/10 px-2.5 py-1.5 text-xs text-slate-300 hover:border-white/30 hover:text-white"
                title="Download BibTeX"
              >
                <Download size={13} /> BibTeX
              </button>
              <button
                onClick={() => api.exportReferences(projectId, "ris")}
                className="inline-flex items-center gap-1.5 rounded-lg border border-white/10 px-2.5 py-1.5 text-xs text-slate-300 hover:border-white/30 hover:text-white"
                title="Download RIS"
              >
                <Download size={13} /> RIS
              </button>
            </div>
          </div>
          <ol className="mt-3 space-y-2">
            {references.map((r) => (
              <li key={r.paperId} className="rounded-lg border border-white/5 bg-white/[0.02] px-4 py-2.5 text-sm text-slate-300">
                {r.text}
              </li>
            ))}
          </ol>
        </div>
      )}
    </GlassCard>
  );
}

function PaperRow({ projectId, paper }: { projectId: string; paper: Paper }) {
  const update = useUpdatePaper(projectId);
  const del = useDeletePaper(projectId);
  const [editing, setEditing] = useState(false);
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
        className="mt-0.5 w-full rounded-lg border border-white/10 bg-white/[0.05] px-2.5 py-1.5 text-sm text-white outline-none focus:border-accent/50"
      />
    </label>
  );

  return (
    <div className="rounded-xl border border-white/5 bg-white/[0.02] px-4 py-3">
      <div className="flex items-start justify-between gap-3">
        <div className="min-w-0">
          <p className="flex items-center gap-2 text-sm font-medium text-white">
            <FileText size={14} className="shrink-0 text-slate-500" />
            <span className="truncate">{paper.title || paper.filename || "Untitled"}</span>
          </p>
          <p className="mt-0.5 truncate text-xs text-slate-400">
            {[paper.authors, paper.year, paper.journal].filter(Boolean).join(" · ") || "No metadata extracted"}
          </p>
          {lowConfidence && !editing && (
            <p className="mt-1 inline-flex items-center gap-1 text-[11px] text-amber-300">
              <AlertTriangle size={11} /> Low-confidence extraction — please review
            </p>
          )}
        </div>
        <div className="flex shrink-0 items-center gap-1">
          <button onClick={() => setEditing((v) => !v)} className="rounded-lg border border-white/10 p-1.5 text-slate-400 hover:border-white/30 hover:text-white" title="Edit details">
            <Pencil size={13} />
          </button>
          <button onClick={() => del.mutate(paper.id)} className="rounded-lg border border-white/10 p-1.5 text-slate-400 hover:border-rose-400/40 hover:text-rose-300" title="Delete">
            <Trash2 size={13} />
          </button>
        </div>
      </div>

      {editing && (
        <div className="mt-3 grid grid-cols-2 gap-2">
          <div className="col-span-2">{field("Title", "title")}</div>
          {field("Authors", "authors", "Surname, A.")}
          {field("Year", "year", "2024")}
          {field("Journal / source", "journal")}
          {field("DOI", "doi")}
          <div className="col-span-2 mt-1 flex justify-end gap-2">
            <button onClick={() => setEditing(false)} className="px-2 text-xs text-slate-400 hover:text-white">Cancel</button>
            <Button size="sm" onClick={save} disabled={update.isPending}>
              {update.isPending ? "Saving…" : "Save"}
            </Button>
          </div>
        </div>
      )}
    </div>
  );
}
