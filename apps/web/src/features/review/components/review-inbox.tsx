"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { Inbox, Loader2 } from "lucide-react";
import { GlassCard } from "@/components/ui/glass-card";
import { api } from "@/lib/api";
import { useReviewInbox } from "../api/use-reviews";

/** A reviewer's pending queue across all their students (FR-SUP-7). */
export function ReviewInbox() {
  const inbox = useReviewInbox();
  const router = useRouter();
  const [opening, setOpening] = useState<string | null>(null);
  const items = inbox.data ?? [];

  async function open(documentId: string) {
    setOpening(documentId);
    try {
      const doc = await api.getDocument(documentId);
      router.push(`/projects/${doc.document.projectId}/documents/${documentId}`);
    } finally {
      setOpening(null);
    }
  }

  return (
    <GlassCard className="p-6">
      <h2 className="flex items-center gap-2 font-display text-lg font-semibold text-slate-900">
        <Inbox size={18} className="text-accent" /> Review inbox
        {items.length > 0 && (
          <span className="rounded-full bg-accent/20 px-2 py-0.5 text-xs text-accent">{items.length}</span>
        )}
      </h2>
      <p className="mt-1 text-xs text-slate-500">Documents shared with you that are waiting for your review.</p>

      <div className="mt-4 space-y-2">
        {inbox.isLoading ? (
          <p className="text-sm text-slate-500">Loading…</p>
        ) : items.length === 0 ? (
          <p className="rounded-xl border border-dashed border-slate-200 px-4 py-6 text-center text-sm text-slate-500">
            Nothing to review right now.
          </p>
        ) : (
          items.map((r) => (
            <button
              key={r.id}
              onClick={() => open(r.documentId)}
              disabled={opening === r.documentId}
              className="flex w-full items-center justify-between gap-3 rounded-xl border border-slate-200 bg-slate-50 px-4 py-3 text-left transition-colors hover:border-slate-300"
            >
              <div className="min-w-0">
                <p className="truncate text-sm text-slate-900">{r.note || "Review request"}</p>
                <p className="text-xs text-slate-500">{new Date(r.createdAt).toLocaleString()}</p>
              </div>
              {opening === r.documentId ? (
                <Loader2 size={15} className="shrink-0 animate-spin text-accent" />
              ) : (
                <span className="shrink-0 text-xs text-accent">Open →</span>
              )}
            </button>
          ))
        )}
      </div>
    </GlassCard>
  );
}
