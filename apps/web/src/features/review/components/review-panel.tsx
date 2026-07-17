"use client";

import { useState } from "react";
import { AnimatePresence, motion } from "framer-motion";
import { Check, MessageSquare, Send, X } from "lucide-react";
import { Portal } from "@/components/ui/portal";
import { Button } from "@/components/ui/button";
import { useProject } from "@/features/project/api/use-projects";
import { useMe } from "@/features/user/api/use-me";
import type { ReviewThread } from "@/lib/api";
import {
  useAddComment,
  useDecide,
  useResolveComment,
  useResubmit,
  useReviews,
  useSubmitReview,
  useSubmitReviewExternal,
} from "../api/use-reviews";

const STATUS_STYLE: Record<string, string> = {
  PENDING: "border-amber-400/40 text-amber-300",
  APPROVED: "border-emerald-400/40 text-emerald-300",
  NEEDS_REVISION: "border-orange-400/40 text-orange-300",
  REJECTED: "border-rose-400/40 text-rose-300",
};

export function ReviewPanel({
  docId,
  projectId,
  open,
  onClose,
}: {
  docId: string;
  projectId: string;
  open: boolean;
  onClose: () => void;
}) {
  const me = useMe();
  const project = useProject(projectId);
  const reviews = useReviews(docId);
  const submit = useSubmitReview(docId);
  const submitExternal = useSubmitReviewExternal(docId);
  const [reviewer, setReviewer] = useState("");
  const [note, setNote] = useState("");
  const [mode, setMode] = useState<"member" | "external">("member");
  const [email, setEmail] = useState("");

  const uid = me.data?.id;
  const members = (project.data?.members ?? []).filter((m) => m.userId !== uid);
  const threads = reviews.data ?? [];

  async function requestReview() {
    if (mode === "member") {
      if (!reviewer) return;
      await submit.mutateAsync({ documentId: docId, reviewerUserId: reviewer, note: note.trim() || undefined });
      setReviewer("");
    } else {
      if (!email.trim()) return;
      await submitExternal.mutateAsync({ documentId: docId, email: email.trim(), note: note.trim() || undefined });
      setEmail("");
    }
    setNote("");
  }

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
              initial={{ x: 420 }}
              animate={{ x: 0 }}
              exit={{ x: 420 }}
              transition={{ type: "spring", stiffness: 320, damping: 32 }}
              onClick={(e) => e.stopPropagation()}
              className="glass h-full w-full max-w-lg overflow-y-auto p-6"
            >
              <div className="flex items-center justify-between">
                <h3 className="flex items-center gap-2 font-display text-lg font-semibold text-white">
                  <MessageSquare size={18} className="text-accent" /> Reviews
                </h3>
                <button onClick={onClose} className="text-slate-400 hover:text-white" aria-label="Close">
                  <X size={18} />
                </button>
              </div>

              {/* Request a review */}
              <div className="mt-4 rounded-xl border border-white/10 bg-white/[0.02] p-4">
                <p className="text-sm font-medium text-white">Request a review</p>
                <div className="mt-2 flex rounded-lg border border-white/10 p-0.5 text-xs">
                  {(["member", "external"] as const).map((m) => (
                    <button
                      key={m}
                      onClick={() => setMode(m)}
                      className={`flex-1 rounded-md px-2 py-1 transition-colors ${mode === m ? "bg-accent/20 text-white" : "text-slate-400 hover:text-white"}`}
                    >
                      {m === "member" ? "Project member" : "External (email link)"}
                    </button>
                  ))}
                </div>
                <div className="mt-2 space-y-2">
                  {mode === "member" ? (
                    members.length === 0 ? (
                      <p className="text-xs text-slate-500">Add a collaborator to the project first (Team panel), or invite one by email.</p>
                    ) : (
                      <select
                        value={reviewer}
                        onChange={(e) => setReviewer(e.target.value)}
                        className="w-full rounded-lg border border-white/10 bg-cosmos-900 px-3 py-2 text-sm text-white outline-none"
                      >
                        <option value="">Choose a reviewer…</option>
                        {members.map((m) => (
                          <option key={m.userId} value={m.userId}>
                            {m.role} · {m.userId.slice(0, 8)}
                          </option>
                        ))}
                      </select>
                    )
                  ) : (
                    <input
                      type="email"
                      value={email}
                      onChange={(e) => setEmail(e.target.value)}
                      placeholder="supervisor@university.edu"
                      className="w-full rounded-lg border border-white/10 bg-white/[0.05] px-3 py-2 text-sm text-white outline-none"
                    />
                  )}
                  <input
                    value={note}
                    onChange={(e) => setNote(e.target.value)}
                    placeholder="Note for the reviewer (optional)"
                    className="w-full rounded-lg border border-white/10 bg-white/[0.05] px-3 py-2 text-sm text-white outline-none"
                  />
                  <Button
                    size="sm"
                    onClick={requestReview}
                    disabled={(mode === "member" ? !reviewer : !email.trim()) || submit.isPending || submitExternal.isPending}
                  >
                    <Send size={13} /> {submit.isPending || submitExternal.isPending ? "Sending…" : "Request review"}
                  </Button>
                </div>
              </div>

              {/* Threads */}
              <div className="mt-5 space-y-4">
                {reviews.isLoading ? (
                  <p className="text-sm text-slate-500">Loading…</p>
                ) : threads.length === 0 ? (
                  <p className="text-sm text-slate-500">No reviews requested yet.</p>
                ) : (
                  threads.map((t) => (
                    <ThreadCard key={t.request.id} docId={docId} thread={t} uid={uid} />
                  ))
                )}
              </div>
            </motion.aside>
          </motion.div>
        )}
      </AnimatePresence>
    </Portal>
  );
}

function ThreadCard({ docId, thread, uid }: { docId: string; thread: ReviewThread; uid?: string }) {
  const addComment = useAddComment(docId);
  const resolve = useResolveComment(docId);
  const decide = useDecide(docId);
  const resubmit = useResubmit(docId);
  const [body, setBody] = useState("");
  const [summary, setSummary] = useState("");

  const { request: r, comments, decisions } = thread;
  const isReviewer = uid && r.reviewerUserId === uid;
  const isRequester = uid && r.requestedBy === uid;

  return (
    <div className="rounded-xl border border-white/10 bg-white/[0.02] p-4">
      <div className="flex items-center justify-between gap-2">
        <span className="text-xs text-slate-400">{new Date(r.createdAt).toLocaleString()}</span>
        <span className={`rounded-full border px-2 py-0.5 text-[10px] uppercase tracking-wider ${STATUS_STYLE[r.status] ?? "border-white/10 text-slate-400"}`}>
          {r.status.replace("_", " ")}
        </span>
      </div>
      {r.note && <p className="mt-1 text-sm text-slate-300">{r.note}</p>}

      {/* Comments */}
      <div className="mt-3 space-y-2">
        {comments.map((c) => (
          <div key={c.id} className={`rounded-lg border px-3 py-2 text-xs ${c.resolved ? "border-white/5 bg-white/[0.01] opacity-60" : "border-white/10 bg-white/[0.03]"}`}>
            <div className="flex items-center justify-between gap-2">
              <span className="font-medium text-slate-200">{c.authorLabel ?? "Reviewer"}</span>
              <button
                onClick={() => resolve.mutate({ commentId: c.id, resolved: !c.resolved })}
                className="text-[10px] text-slate-400 hover:text-white"
              >
                {c.resolved ? "Reopen" : "Resolve"}
              </button>
            </div>
            {c.quote && <p className="mt-1 border-l-2 border-accent/40 pl-2 italic text-slate-400">“{c.quote}”</p>}
            <p className="mt-1 text-slate-300">{c.body}</p>
          </div>
        ))}
      </div>

      {/* Add comment */}
      <div className="mt-2 flex gap-2">
        <input
          value={body}
          onChange={(e) => setBody(e.target.value)}
          onKeyDown={(e) => {
            if (e.key === "Enter" && body.trim()) {
              addComment.mutate({ reviewId: r.id, body: body.trim() });
              setBody("");
            }
          }}
          placeholder="Add a comment…"
          className="min-w-0 flex-1 rounded-lg border border-white/10 bg-white/[0.05] px-3 py-1.5 text-xs text-white outline-none"
        />
        <button
          onClick={() => {
            if (body.trim()) {
              addComment.mutate({ reviewId: r.id, body: body.trim() });
              setBody("");
            }
          }}
          className="rounded-lg border border-white/10 px-2 text-slate-300 hover:border-white/30"
        >
          <Send size={13} />
        </button>
      </div>

      {/* Decision history */}
      {decisions.length > 0 && (
        <div className="mt-3 space-y-1">
          {decisions.map((d) => (
            <p key={d.id} className="text-[11px] text-slate-400">
              <span className="font-semibold text-slate-300">{d.decision.replace("_", " ")}</span>
              {d.summary ? ` — ${d.summary}` : ""}
            </p>
          ))}
        </div>
      )}

      {/* Reviewer decision controls */}
      {isReviewer && r.status === "PENDING" && (
        <div className="mt-3 rounded-lg border border-accent/20 bg-accent/[0.03] p-2">
          <input
            value={summary}
            onChange={(e) => setSummary(e.target.value)}
            placeholder="Decision summary (optional)"
            className="mb-2 w-full rounded-lg border border-white/10 bg-white/[0.05] px-2.5 py-1.5 text-xs text-white outline-none"
          />
          <div className="flex flex-wrap gap-2">
            {[
              { d: "APPROVED", label: "Approve", cls: "border-emerald-400/40 text-emerald-300" },
              { d: "NEEDS_REVISION", label: "Needs revision", cls: "border-orange-400/40 text-orange-300" },
              { d: "REJECTED", label: "Reject", cls: "border-rose-400/40 text-rose-300" },
            ].map((b) => (
              <button
                key={b.d}
                onClick={() => decide.mutate({ reviewId: r.id, decision: b.d, summary: summary.trim() || undefined })}
                disabled={decide.isPending}
                className={`inline-flex items-center gap-1 rounded-lg border px-2.5 py-1 text-xs hover:bg-white/5 disabled:opacity-50 ${b.cls}`}
              >
                <Check size={12} /> {b.label}
              </button>
            ))}
          </div>
        </div>
      )}

      {/* Requester resubmit after revision */}
      {isRequester && r.status === "NEEDS_REVISION" && (
        <Button size="sm" variant="outline" className="mt-3" onClick={() => resubmit.mutate({ reviewId: r.id })}>
          Resubmit for review
        </Button>
      )}
    </div>
  );
}
