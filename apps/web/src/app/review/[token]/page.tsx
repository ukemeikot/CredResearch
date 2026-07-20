"use client";

import { use, useEffect, useState } from "react";
import { Check, Loader2, MessageSquare, ShieldCheck } from "lucide-react";
import { api, ApiError, type ExternalReview } from "@/lib/api";
import { Button } from "@/components/ui/button";

/** Public magic-link review surface — an external supervisor reviews without an account (FR-SUP-2). */
export default function ReviewAccessPage({ params }: { params: Promise<{ token: string }> }) {
  const { token } = use(params);
  const [view, setView] = useState<ExternalReview | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [body, setBody] = useState("");
  const [summary, setSummary] = useState("");
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    api
      .reviewAccessView(token)
      .then(setView)
      .catch((e) => setError(e instanceof ApiError ? e.message : "This review link is invalid or expired."))
      .finally(() => setLoading(false));
  }, [token]);

  async function addComment() {
    if (!body.trim()) return;
    setBusy(true);
    try {
      setView(await api.reviewAccessComment(token, body.trim()));
      setBody("");
    } catch (e) {
      setError(e instanceof ApiError ? e.message : "Couldn’t add comment");
    } finally {
      setBusy(false);
    }
  }

  async function decide(decision: string) {
    setBusy(true);
    try {
      setView(await api.reviewAccessDecide(token, decision, summary.trim() || undefined));
      setSummary("");
    } catch (e) {
      setError(e instanceof ApiError ? e.message : "Couldn’t record decision");
    } finally {
      setBusy(false);
    }
  }

  if (loading) {
    return (
      <div className="grid min-h-screen place-items-center bg-white">
        <Loader2 className="h-8 w-8 animate-spin text-accent" />
      </div>
    );
  }
  if (error && !view) {
    return (
      <div className="grid min-h-screen place-items-center bg-white px-6 text-center">
        <div>
          <p className="text-lg font-semibold text-slate-900">Review link unavailable</p>
          <p className="mt-2 text-sm text-slate-500">{error}</p>
        </div>
      </div>
    );
  }
  if (!view) return null;

  const r = view.request;
  const decided = r.status !== "PENDING";

  return (
    <div className="min-h-screen bg-white px-4 py-10 text-slate-700">
      <div className="mx-auto max-w-2xl">
        <div className="flex items-center gap-2 text-sm text-accent">
          <ShieldCheck size={16} /> CredResearch review
        </div>
        <h1 className="mt-2 font-display text-2xl font-bold text-slate-900">
          {view.sectionHeading || "Document review"}
        </h1>
        <p className="mt-1 text-xs text-slate-500">
          You were invited to review this as {r.reviewerEmail}. No account needed.
          {r.note ? ` — “${r.note}”` : ""}
        </p>

        {/* Section content */}
        <div className="mt-6 whitespace-pre-wrap rounded-2xl border border-slate-200 bg-slate-50 p-5 text-sm leading-relaxed">
          {flatten(view.sectionContent) || "(This section has no content yet.)"}
        </div>

        {/* Decision status / history */}
        <div className="mt-6">
          <p className="text-sm font-semibold text-slate-900">
            Status: <span className="text-accent">{r.status.replace("_", " ")}</span>
          </p>
          {view.decisions.map((d) => (
            <p key={d.id} className="mt-1 text-xs text-slate-500">
              {d.decision.replace("_", " ")}{d.summary ? ` — ${d.summary}` : ""}
            </p>
          ))}
        </div>

        {/* Comments */}
        <div className="mt-6">
          <p className="flex items-center gap-2 text-sm font-semibold text-slate-900">
            <MessageSquare size={15} /> Comments
          </p>
          <div className="mt-2 space-y-2">
            {view.comments.length === 0 && <p className="text-xs text-slate-500">No comments yet.</p>}
            {view.comments.map((c) => (
              <div key={c.id} className="rounded-lg border border-slate-200 bg-slate-50 px-3 py-2 text-xs">
                <span className="font-medium text-slate-700">{c.authorLabel || "Reviewer"}</span>
                <p className="mt-1 text-slate-600">{c.body}</p>
              </div>
            ))}
          </div>
          <div className="mt-2 flex gap-2">
            <input
              value={body}
              onChange={(e) => setBody(e.target.value)}
              onKeyDown={(e) => e.key === "Enter" && addComment()}
              placeholder="Add a comment…"
              className="min-w-0 flex-1 rounded-lg border border-slate-200 bg-slate-50 px-3 py-2 text-sm text-slate-900 outline-none"
            />
            <Button size="sm" onClick={addComment} disabled={busy || !body.trim()}>Send</Button>
          </div>
        </div>

        {/* Decision */}
        {!decided && (
          <div className="mt-6 rounded-2xl border border-accent/20 bg-accent/[0.03] p-4">
            <p className="text-sm font-semibold text-slate-900">Record your decision</p>
            <input
              value={summary}
              onChange={(e) => setSummary(e.target.value)}
              placeholder="Summary note (optional)"
              className="mt-2 w-full rounded-lg border border-slate-200 bg-slate-50 px-3 py-2 text-sm text-slate-900 outline-none"
            />
            <div className="mt-2 flex flex-wrap gap-2">
              {[
                { d: "APPROVED", label: "Approve", cls: "border-emerald-400/40 text-emerald-600" },
                { d: "NEEDS_REVISION", label: "Needs revision", cls: "border-orange-400/40 text-orange-600" },
                { d: "REJECTED", label: "Reject", cls: "border-rose-400/40 text-rose-600" },
              ].map((b) => (
                <button
                  key={b.d}
                  onClick={() => decide(b.d)}
                  disabled={busy}
                  className={`inline-flex items-center gap-1 rounded-lg border px-3 py-1.5 text-sm hover:bg-slate-100 disabled:opacity-50 ${b.cls}`}
                >
                  <Check size={13} /> {b.label}
                </button>
              ))}
            </div>
          </div>
        )}
        {error && <p className="mt-3 text-xs text-rose-600">{error}</p>}
      </div>
    </div>
  );
}

/** Flatten ProseMirror/Tiptap JSON (stored as a string) to readable text for the review surface. */
function flatten(contentJson: string | null): string {
  if (!contentJson) return "";
  try {
    const doc = JSON.parse(contentJson);
    const out: string[] = [];
    const walk = (n: { type?: string; text?: string; content?: unknown[] }) => {
      if (n.text) out.push(n.text);
      if (Array.isArray(n.content)) {
        n.content.forEach((c) => walk(c as { type?: string; text?: string; content?: unknown[] }));
        if (n.type && ["paragraph", "heading", "listItem", "blockquote"].includes(n.type)) out.push("\n");
      }
    };
    walk(doc);
    return out.join("").trim();
  } catch {
    return "";
  }
}
