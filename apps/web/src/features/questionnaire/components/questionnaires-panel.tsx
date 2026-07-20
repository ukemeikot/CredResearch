"use client";

import { useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { ClipboardList, Loader2, Plus, Sparkles } from "lucide-react";
import { GlassCard } from "@/components/ui/glass-card";
import { Button } from "@/components/ui/button";
import { ApiError } from "@/lib/api";
import { useCreateQuestionnaire, useGenerateQuestionnaire, useQuestionnaires } from "../api/use-questionnaires";

const STATUS_STYLE: Record<string, string> = {
  DRAFT: "border-white/15 text-slate-400",
  PUBLISHED: "border-emerald-400/40 text-emerald-300",
  CLOSED: "border-rose-400/40 text-rose-300",
};

export function QuestionnairesPanel({ projectId }: { projectId: string }) {
  const list = useQuestionnaires(projectId);
  const create = useCreateQuestionnaire(projectId);
  const generate = useGenerateQuestionnaire(projectId);
  const router = useRouter();
  const [title, setTitle] = useState("");
  const [adding, setAdding] = useState(false);
  const [genOpen, setGenOpen] = useState(false);
  const [topic, setTopic] = useState("");
  const [genError, setGenError] = useState<string | null>(null);

  const items = list.data ?? [];

  async function add() {
    if (!title.trim()) return;
    await create.mutateAsync({ projectId, title: title.trim() });
    setTitle("");
    setAdding(false);
  }

  async function aiGenerate() {
    if (!topic.trim()) return;
    setGenError(null);
    try {
      const res = await generate.mutateAsync({ projectId, topic: topic.trim() });
      setGenOpen(false);
      setTopic("");
      router.push(`/projects/${projectId}/questionnaires/${res.questionnaire.id}`);
    } catch (e) {
      setGenError(e instanceof ApiError ? e.message : "Couldn’t generate");
    }
  }

  return (
    <GlassCard className="p-6">
      <div className="flex items-center justify-between gap-3">
        <h3 className="flex items-center gap-2 font-display text-lg font-semibold text-white">
          <ClipboardList size={18} className="text-accent" /> Questionnaires
        </h3>
        <div className="flex items-center gap-2">
          <Button size="sm" variant="ghost" onClick={() => setGenOpen((v) => !v)}>
            <Sparkles size={14} /> AI
          </Button>
          <Button size="sm" variant="outline" onClick={() => setAdding((v) => !v)}>
            <Plus size={14} /> New
          </Button>
        </div>
      </div>

      {genOpen && (
        <div className="mt-3 rounded-xl border border-accent/20 bg-accent/[0.04] p-3">
          <p className="text-xs text-accent">Generate a draft questionnaire with AI</p>
          <div className="mt-2 flex gap-2">
            <input
              autoFocus
              value={topic}
              onChange={(e) => setTopic(e.target.value)}
              onKeyDown={(e) => e.key === "Enter" && aiGenerate()}
              placeholder="Research topic (e.g. rural solar adoption)"
              className="min-w-0 flex-1 rounded-lg border border-white/10 bg-white/[0.05] px-3 py-2 text-sm text-white outline-none"
            />
            <Button size="sm" onClick={aiGenerate} disabled={!topic.trim() || generate.isPending}>
              {generate.isPending ? <Loader2 size={14} className="animate-spin" /> : "Generate"}
            </Button>
          </div>
          {genError && <p className="mt-1 text-xs text-rose-400">{genError}</p>}
        </div>
      )}

      {adding && (
        <div className="mt-3 flex gap-2">
          <input
            autoFocus
            value={title}
            onChange={(e) => setTitle(e.target.value)}
            onKeyDown={(e) => e.key === "Enter" && add()}
            placeholder="Questionnaire title"
            className="min-w-0 flex-1 rounded-lg border border-white/10 bg-white/[0.05] px-3 py-2 text-sm text-white outline-none"
          />
          <Button size="sm" onClick={add} disabled={!title.trim() || create.isPending}>
            {create.isPending ? <Loader2 size={14} className="animate-spin" /> : "Create"}
          </Button>
        </div>
      )}

      <div className="mt-4 space-y-2">
        {list.isLoading ? (
          <p className="text-sm text-slate-500">Loading…</p>
        ) : items.length === 0 ? (
          <p className="rounded-xl border border-dashed border-white/10 px-4 py-6 text-center text-sm text-slate-500">
            No questionnaires yet. Create one to collect survey data.
          </p>
        ) : (
          items.map((q) => (
            <Link
              key={q.id}
              href={`/projects/${projectId}/questionnaires/${q.id}`}
              className="flex items-center justify-between gap-3 rounded-xl border border-white/10 bg-white/[0.02] px-4 py-3 transition-colors hover:border-white/30"
            >
              <span className="truncate text-sm text-white">{q.title}</span>
              <span className={`shrink-0 rounded-full border px-2 py-0.5 text-[10px] uppercase tracking-wider ${STATUS_STYLE[q.status] ?? "border-white/10 text-slate-400"}`}>
                {q.status}
              </span>
            </Link>
          ))
        )}
      </div>
    </GlassCard>
  );
}
