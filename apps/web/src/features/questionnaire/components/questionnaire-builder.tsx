"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { ArrowLeft, ChevronDown, ChevronUp, Copy, Download, Loader2, Plus, Trash2 } from "lucide-react";
import { GlassCard } from "@/components/ui/glass-card";
import { Button } from "@/components/ui/button";
import { api } from "@/lib/api";
import {
  useCloseQuestionnaire,
  usePublishQuestionnaire,
  useQuestionnaire,
  useQuestionnaireResponses,
  useUpdateQuestionnaire,
} from "../api/use-questionnaires";

const TYPES = [
  { v: "TEXT", label: "Short text" },
  { v: "LONG_TEXT", label: "Paragraph" },
  { v: "NUMBER", label: "Number" },
  { v: "BOOLEAN", label: "Yes / No" },
  { v: "SINGLE_CHOICE", label: "Single choice" },
  { v: "MULTI_CHOICE", label: "Multiple choice" },
  { v: "LIKERT", label: "Likert scale" },
];
const HAS_OPTIONS = new Set(["SINGLE_CHOICE", "MULTI_CHOICE", "LIKERT"]);

interface EditQ {
  type: string;
  prompt: string;
  choices: string; // comma-separated in the editor
  required: boolean;
}

export function QuestionnaireBuilder({ projectId, id }: { projectId: string; id: string }) {
  const q = useQuestionnaire(id);
  const update = useUpdateQuestionnaire(id);
  const publish = usePublishQuestionnaire(id);
  const close = useCloseQuestionnaire(id);
  const responses = useQuestionnaireResponses(id);

  const [title, setTitle] = useState("");
  const [consent, setConsent] = useState("");
  const [questions, setQuestions] = useState<EditQ[]>([]);
  const [link, setLink] = useState<string | null>(null);
  const [seeded, setSeeded] = useState(false);

  useEffect(() => {
    if (q.data && !seeded) {
      setTitle(q.data.questionnaire.title);
      setConsent(q.data.questionnaire.consentText ?? "");
      setQuestions(
        q.data.questions.map((x) => ({
          type: x.type,
          prompt: x.prompt,
          choices: Array.isArray(x.options?.choices) ? x.options.choices.join(", ") : "",
          required: x.required,
        })),
      );
      setSeeded(true);
    }
  }, [q.data, seeded]);

  function setQ(i: number, patch: Partial<EditQ>) {
    setQuestions((qs) => qs.map((x, idx) => (idx === i ? { ...x, ...patch } : x)));
  }
  function move(i: number, dir: -1 | 1) {
    setQuestions((qs) => {
      const j = i + dir;
      if (j < 0 || j >= qs.length) return qs;
      const copy = [...qs];
      [copy[i], copy[j]] = [copy[j], copy[i]];
      return copy;
    });
  }

  async function save() {
    await update.mutateAsync({
      title: title.trim() || "Untitled",
      consentText: consent,
      questions: questions
        .filter((x) => x.prompt.trim())
        .map((x) => ({
          type: x.type,
          prompt: x.prompt.trim(),
          required: x.required,
          options: HAS_OPTIONS.has(x.type)
            ? { choices: x.choices.split(",").map((c) => c.trim()).filter(Boolean) }
            : undefined,
        })),
    });
  }

  async function doPublish() {
    await save();
    const res = await publish.mutateAsync(undefined);
    setLink(`${window.location.origin}/s/${res.token}`);
  }

  if (q.isLoading) {
    return <div className="grid place-items-center py-32"><Loader2 className="h-8 w-8 animate-spin text-accent" /></div>;
  }
  if (!q.data) {
    return <GlassCard className="p-8 text-center text-sm text-rose-300">Couldn’t load this questionnaire.</GlassCard>;
  }

  const status = q.data.questionnaire.status;
  const respCount = responses.data?.length ?? 0;

  return (
    <div>
      <Link href={`/projects/${projectId}`} className="inline-flex items-center gap-2 text-sm text-slate-400 hover:text-white">
        <ArrowLeft size={16} /> Back to project
      </Link>

      <div className="mt-4 flex flex-wrap items-center justify-between gap-3">
        <h1 className="font-display text-2xl font-bold text-white">Questionnaire</h1>
        <div className="flex items-center gap-2">
          <Button size="sm" variant="outline" onClick={save} disabled={update.isPending}>
            {update.isPending ? "Saving…" : "Save"}
          </Button>
          {status !== "CLOSED" && (
            <Button size="sm" onClick={doPublish} disabled={publish.isPending}>
              {publish.isPending ? "Publishing…" : status === "PUBLISHED" ? "Re-issue link" : "Publish"}
            </Button>
          )}
        </div>
      </div>

      {link && (
        <div className="mt-3 flex items-center gap-2 rounded-xl border border-emerald-400/30 bg-emerald-400/[0.06] px-4 py-3 text-sm">
          <span className="truncate text-emerald-200">{link}</span>
          <button onClick={() => navigator.clipboard?.writeText(link)} className="ml-auto shrink-0 text-emerald-300 hover:text-white" title="Copy link">
            <Copy size={15} />
          </button>
        </div>
      )}

      <div className="mt-6 grid gap-6 lg:grid-cols-[1fr_300px]">
        {/* Builder */}
        <div className="space-y-4">
          <GlassCard className="p-5">
            <label className="block text-xs uppercase tracking-wider text-slate-400">Title</label>
            <input value={title} onChange={(e) => setTitle(e.target.value)} className="mt-1 w-full rounded-lg border border-white/10 bg-white/[0.05] px-3 py-2 text-sm text-white outline-none" />
            <label className="mt-4 block text-xs uppercase tracking-wider text-slate-400">Consent statement (optional)</label>
            <textarea value={consent} onChange={(e) => setConsent(e.target.value)} rows={2} placeholder="Respondents must agree before submitting…" className="mt-1 w-full rounded-lg border border-white/10 bg-white/[0.05] px-3 py-2 text-sm text-white outline-none" />
          </GlassCard>

          {questions.map((x, i) => (
            <GlassCard key={i} className="p-4">
              <div className="flex items-start gap-2">
                <div className="flex-1 space-y-2">
                  <input value={x.prompt} onChange={(e) => setQ(i, { prompt: e.target.value })} placeholder={`Question ${i + 1}`} className="w-full rounded-lg border border-white/10 bg-white/[0.05] px-3 py-2 text-sm text-white outline-none" />
                  <div className="flex flex-wrap items-center gap-2">
                    <select value={x.type} onChange={(e) => setQ(i, { type: e.target.value })} className="rounded-lg border border-white/10 bg-cosmos-900 px-2 py-1.5 text-xs text-slate-200 outline-none">
                      {TYPES.map((t) => <option key={t.v} value={t.v}>{t.label}</option>)}
                    </select>
                    <label className="flex items-center gap-1.5 text-xs text-slate-400">
                      <input type="checkbox" checked={x.required} onChange={(e) => setQ(i, { required: e.target.checked })} /> Required
                    </label>
                  </div>
                  {HAS_OPTIONS.has(x.type) && (
                    <input value={x.choices} onChange={(e) => setQ(i, { choices: e.target.value })} placeholder="Options, comma-separated (e.g. Strongly agree, Agree, Neutral)" className="w-full rounded-lg border border-white/10 bg-white/[0.05] px-3 py-1.5 text-xs text-white outline-none" />
                  )}
                </div>
                <div className="flex flex-col gap-1">
                  <button onClick={() => move(i, -1)} className="rounded border border-white/10 p-1 text-slate-400 hover:text-white"><ChevronUp size={13} /></button>
                  <button onClick={() => move(i, 1)} className="rounded border border-white/10 p-1 text-slate-400 hover:text-white"><ChevronDown size={13} /></button>
                  <button onClick={() => setQuestions((qs) => qs.filter((_, idx) => idx !== i))} className="rounded border border-white/10 p-1 text-slate-400 hover:text-rose-300"><Trash2 size={13} /></button>
                </div>
              </div>
            </GlassCard>
          ))}

          <Button variant="outline" onClick={() => setQuestions((qs) => [...qs, { type: "TEXT", prompt: "", choices: "", required: false }])}>
            <Plus size={14} /> Add question
          </Button>
        </div>

        {/* Sidebar: status + responses */}
        <div className="space-y-4">
          <GlassCard className="p-5">
            <p className="text-xs uppercase tracking-wider text-slate-400">Status</p>
            <p className="mt-1 text-lg font-semibold text-white">{status}</p>
            <p className="mt-3 text-xs uppercase tracking-wider text-slate-400">Responses</p>
            <p className="mt-1 text-2xl font-bold text-accent">{respCount}</p>
            <div className="mt-4 flex flex-col gap-2">
              <Button size="sm" variant="outline" onClick={() => api.downloadResponsesCsv(id)} disabled={respCount === 0}>
                <Download size={14} /> Export CSV
              </Button>
              {status === "PUBLISHED" && (
                <Button size="sm" variant="ghost" onClick={() => close.mutate()} disabled={close.isPending}>
                  Close survey
                </Button>
              )}
            </div>
          </GlassCard>
        </div>
      </div>
    </div>
  );
}
