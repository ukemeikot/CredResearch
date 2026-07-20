"use client";

import { useEffect, useMemo, useRef, useState } from "react";
import { EditorContent, useEditor, type Content } from "@tiptap/react";
import StarterKit from "@tiptap/starter-kit";
import Collaboration from "@tiptap/extension-collaboration";
import CollaborationCaret from "@tiptap/extension-collaboration-caret";
import { Color, FontFamily, FontSize, TextStyle } from "@tiptap/extension-text-style";
import {
  AlertTriangle,
  Ban,
  Bold,
  Check,
  CloudOff,
  Heading2,
  Heading3,
  History,
  Italic,
  List,
  ListOrdered,
  BookMarked,
  Loader2,
  MessageSquarePlus,
  Palette,
  Quote,
  Sparkles,
  Users,
} from "lucide-react";
import { Button } from "@/components/ui/button";
import { ApiError, type DocSection, type Paper } from "@/lib/api";
import { useMe } from "@/features/user/api/use-me";
import { usePapers } from "@/features/paper/api/use-papers";
import { citationLabel, inTextCitation } from "@/features/paper/model/cite";
import { useAddComment, useReviews } from "@/features/review/api/use-reviews";
import { useAiCredits, useDisclosureAppend, useSectionAssist } from "../api/use-ai";
import { ReviewHighlights, refreshReviewHighlights } from "../extensions/review-highlights";
import { readSectionBuffer, useSectionAutosave } from "../hooks/use-autosave";
import { useCollab, type CollabPeer } from "../hooks/use-collab";

const AUTOSAVE_DELAY = 1200;

// Rich-text style marks: typeface, size, and colour (TextStyle is the shared base the others attach to).
const STYLE_EXTENSIONS = [TextStyle, FontFamily, FontSize, Color];

const FONT_FAMILIES: { label: string; value: string }[] = [
  { label: "Default", value: "" },
  { label: "Serif", value: "Georgia, 'Times New Roman', serif" },
  { label: "Sans", value: "Inter, system-ui, sans-serif" },
  { label: "Mono", value: "'JetBrains Mono', ui-monospace, monospace" },
  { label: "Times", value: "'Times New Roman', Times, serif" },
];
const FONT_SIZES = ["12px", "14px", "16px", "18px", "24px", "30px"];
const TEXT_COLORS = ["#e2e8f0", "#ffffff", "#f87171", "#fb923c", "#fbbf24", "#34d399", "#38bdf8", "#a78bfa", "#f472b6"];

export function SectionEditor({
  docId,
  projectId,
  section,
  onReload,
  onOpenHistory,
}: {
  docId: string;
  projectId: string;
  section: DocSection;
  onReload: () => void;
  onOpenHistory: () => void;
}) {
  const me = useMe();
  const collab = useCollab(docId, section.id, me.data?.fullName ?? "", me.data?.id ?? "");
  // "active" = we expect collab here and haven't fallen back; "ready" = actually connected.
  const collabActive = collab.enabled && !collab.unavailable;
  const collabReady = collabActive && collab.connected && !!collab.ydoc && !!collab.provider;
  const { status, save } = useSectionAutosave(docId, section, { collab: collab.enabled });
  const assist = useSectionAssist();
  const disclosure = useDisclosureAppend(docId);
  const credits = useAiCredits();
  const timer = useRef<ReturnType<typeof setTimeout> | null>(null);
  const latest = useRef<unknown>(null); // most recent editor JSON (may be un-flushed)
  const dirty = useRef(false);
  const saveRef = useRef(save);
  saveRef.current = save;
  // In collaboration mode exactly one client (the leader) persists to the API; keep the flag in a
  // ref so the debounced save closure always reads the current value.
  const isLeaderRef = useRef(collab.isLeader);
  isLeaderRef.current = collab.isLeader;
  const seededRef = useRef(false);
  const [, force] = useState(0);
  const papersQ = usePapers(projectId);
  const reviewsQ = useReviews(docId);
  const addReviewComment = useAddComment(docId);
  const anchorRangesRef = useRef<{ from: number; to: number }[]>([]);
  const [citeOpen, setCiteOpen] = useState(false);
  const [aiOpen, setAiOpen] = useState(false);
  const [commentBody, setCommentBody] = useState("");
  const [pendingAnchor, setPendingAnchor] = useState<{ from: number; to: number; quote: string } | null>(null);

  // Review threads for THIS section, and the review request an inline comment attaches to.
  const sectionThreads = useMemo(
    () => (reviewsQ.data ?? []).filter((t) => t.request.documentSectionId === section.id),
    [reviewsQ.data, section.id],
  );
  const activeReview = useMemo(() => {
    const pending = sectionThreads.filter((t) => t.request.status === "PENDING");
    const pool = pending.length ? pending : sectionThreads;
    return pool.length ? pool[0].request : null; // list is newest-first
  }, [sectionThreads]);
  const [instruction, setInstruction] = useState("");

  // Prefer locally-buffered offline edits over the (possibly older) server content.
  const initialContent = (readSectionBuffer(section.id) ?? section.content ?? "") as Content;

  function scheduleSave(json: unknown) {
    latest.current = json;
    dirty.current = true;
    if (timer.current) clearTimeout(timer.current);
    timer.current = setTimeout(() => {
      dirty.current = false;
      // Non-collab (or collab fallback): always persist. Live collab: only the leader writes back.
      if (!collabActive || isLeaderRef.current) saveRef.current(json);
    }, AUTOSAVE_DELAY);
  }

  const editor = useEditor(
    {
      extensions: collabReady
        ? [
            // Collaboration provides its own (shared) undo history — disable StarterKit's.
            StarterKit.configure({ undoRedo: false }),
            ...STYLE_EXTENSIONS,
            ReviewHighlights.configure({ getRanges: () => anchorRangesRef.current }),
            Collaboration.configure({ document: collab.ydoc! }),
            CollaborationCaret.configure({ provider: collab.provider!, user: collab.user }),
          ]
        : [StarterKit, ...STYLE_EXTENSIONS, ReviewHighlights.configure({ getRanges: () => anchorRangesRef.current })],
      // In collab mode content comes from Yjs (seeded once, below) — don't set it here.
      content: collabReady ? undefined : initialContent,
      immediatelyRender: false,
      editorProps: {
        attributes: {
          class: "tiptap min-h-[52vh] w-full max-w-none px-1 py-2 text-slate-700 outline-none",
        },
      },
      onUpdate: ({ editor }) => scheduleSave(editor.getJSON()),
      onSelectionUpdate: () => force((n) => n + 1),
    },
    [section.id, collabReady],
  );

  // Seed the shared Yjs doc from the persisted content the first time it's opened (empty fragment).
  // Only the leader seeds, so peers don't each insert a copy.
  useEffect(() => {
    if (!collabReady || !editor || !collab.synced || !collab.isLeader || seededRef.current) return;
    const fragment = collab.ydoc!.getXmlFragment("default");
    if (fragment.length === 0 && initialContent) {
      editor.commands.setContent(initialContent);
    }
    seededRef.current = true;
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [collabReady, editor, collab.synced, collab.isLeader]);

  // While actively connecting to a live session, prevent typing (avoids edits that wouldn't be
  // shared). Once connected (ready) or fallen back (unavailable), editing is allowed.
  useEffect(() => {
    if (!editor) return;
    editor.setEditable(!(collabActive && !collabReady));
  }, [editor, collabActive, collabReady]);

  // A fresh Yjs doc is built per section — allow the next section to seed again.
  useEffect(() => {
    seededRef.current = false;
  }, [section.id]);

  // Keep the anchored-comment highlights in sync with the section's review comments (FR-SUP-4).
  useEffect(() => {
    anchorRangesRef.current = sectionThreads
      .flatMap((t) => t.comments)
      .filter((c) => c.anchorStart != null && c.anchorEnd != null)
      .map((c) => ({ from: c.anchorStart as number, to: c.anchorEnd as number }));
    refreshReviewHighlights(editor);
  }, [sectionThreads, editor]);

  // Flush a pending debounced save when the section/page unmounts, so the last edits aren't lost.
  useEffect(() => {
    return () => {
      if (timer.current) clearTimeout(timer.current);
      if (dirty.current && latest.current != null) {
        dirty.current = false;
        saveRef.current(latest.current);
      }
    };
  }, []);

  async function runAssist() {
    if (!editor) return;
    try {
      const res = await assist.mutateAsync({
        heading: section.heading,
        current_text: editor.getText(),
        instruction: instruction.trim() || "Draft or improve this section.",
      });
      const html = res.suggestion
        .split(/\n{2,}/)
        .filter(Boolean)
        .map((p) => `<p>${p.replace(/&/g, "&amp;").replace(/</g, "&lt;")}</p>`)
        .join("");
      editor.chain().focus().insertContent(html).run();
      // Record AI use in the document's disclosure ledger (FR-LEDGER).
      disclosure.mutate({
        documentSectionId: section.id,
        featureKey: "section-assist",
        suggestionSummary: res.suggestion.slice(0, 200),
        action: "accepted",
      });
      setAiOpen(false);
      setInstruction("");
    } catch {
      /* surfaced via assist.error */
    }
  }

  function startComment() {
    if (!editor || !activeReview) return;
    const { from, to } = editor.state.selection;
    if (from >= to) return;
    const quote = editor.state.doc.textBetween(from, to, " ").slice(0, 300);
    setPendingAnchor({ from, to, quote });
    setCommentBody("");
  }

  async function submitComment() {
    if (!pendingAnchor || !activeReview || !commentBody.trim()) return;
    await addReviewComment.mutateAsync({
      reviewId: activeReview.id,
      body: commentBody.trim(),
      anchorStart: pendingAnchor.from,
      anchorEnd: pendingAnchor.to,
      quote: pendingAnchor.quote,
    });
    setPendingAnchor(null);
    setCommentBody("");
  }

  const hasSelection = !!editor && !editor.state.selection.empty;
  const creditsLow = credits.data && credits.data.remaining <= 0;

  const aiError = assist.error instanceof ApiError ? assist.error.message : null;

  return (
    <div>
      <div className="mb-4 flex flex-wrap items-center justify-between gap-3">
        <div>
          {section.chapter && (
            <p className="text-[11px] font-medium uppercase tracking-wider text-slate-500">
              {section.chapter}
            </p>
          )}
          <h2 className="font-display text-xl font-bold text-slate-900">{section.heading}</h2>
        </div>
        <div className="flex items-center gap-3">
          {collabActive && <Presence peers={collab.peers} connecting={!collabReady} />}
          <SaveIndicator status={status} />
          <button
            onClick={onOpenHistory}
            className="inline-flex items-center gap-1.5 rounded-full border border-slate-200 px-3 py-1.5 text-xs text-slate-600 transition-colors hover:border-slate-300 hover:text-slate-900"
          >
            <History size={14} /> History
          </button>
        </div>
      </div>

      {collabActive && !collabReady && (
        <div className="mb-4 flex items-center gap-2 rounded-xl border border-slate-200 bg-slate-50 px-4 py-3 text-sm text-slate-500">
          <Loader2 size={16} className="animate-spin" /> Connecting to the live editing session…
        </div>
      )}

      {status === "conflict" && (
        <div className="mb-4 flex items-center justify-between gap-3 rounded-xl border border-amber-400/40 bg-amber-400/10 px-4 py-3 text-sm text-amber-700">
          <span className="flex items-center gap-2">
            <AlertTriangle size={16} /> This section changed elsewhere. Reload to get the latest, then re-apply your edits.
          </span>
          <button onClick={onReload} className="shrink-0 rounded-lg bg-amber-400/20 px-3 py-1 font-medium hover:bg-amber-400/30">
            Reload
          </button>
        </div>
      )}

      {editor && <Toolbar editor={editor} />}

      <div className="mt-2">
        {!aiOpen ? (
          <div className="flex items-center gap-3">
            <button
              onClick={() => setAiOpen(true)}
              className="inline-flex items-center gap-1.5 rounded-full border border-accent/40 px-3 py-1.5 text-xs text-accent transition-colors hover:bg-accent/10"
            >
              <Sparkles size={13} /> AI assist
            </button>
            {/* Insert an in-text citation from the project's papers (FR-LIT-6) */}
            <div className="relative">
              <button
                onClick={() => setCiteOpen((v) => !v)}
                className="inline-flex items-center gap-1.5 rounded-full border border-slate-200 px-3 py-1.5 text-xs text-slate-600 transition-colors hover:border-slate-300 hover:text-slate-900"
              >
                <BookMarked size={13} /> Cite
              </button>
              {citeOpen && (
                <>
                  <div className="fixed inset-0 z-10" onClick={() => setCiteOpen(false)} />
                  <div className="absolute left-0 z-20 mt-1 max-h-64 w-72 overflow-y-auto rounded-xl border border-slate-200 bg-white p-1 shadow-xl">
                    {(papersQ.data ?? []).length === 0 ? (
                      <p className="px-3 py-2 text-xs text-slate-500">
                        No papers yet — upload sources in the project’s Papers &amp; references panel.
                      </p>
                    ) : (
                      (papersQ.data ?? []).map((p: Paper) => (
                        <button
                          key={p.id}
                          onClick={() => {
                            editor?.chain().focus().insertContent(`${inTextCitation(p)} `).run();
                            setCiteOpen(false);
                          }}
                          className="block w-full truncate rounded-lg px-3 py-2 text-left text-xs text-slate-700 hover:bg-slate-100"
                          title={citationLabel(p)}
                        >
                          <span className="text-accent">{inTextCitation(p)}</span> {citationLabel(p)}
                        </button>
                      ))
                    )}
                  </div>
                </>
              )}
            </div>
            {/* Anchor a review comment to the selected text (FR-SUP-4) */}
            {activeReview && (
              <button
                onClick={startComment}
                disabled={!hasSelection}
                title={hasSelection ? "Comment on the selected text" : "Select text to comment on it"}
                className="inline-flex items-center gap-1.5 rounded-full border border-slate-200 px-3 py-1.5 text-xs text-slate-600 transition-colors hover:border-slate-300 hover:text-slate-900 disabled:opacity-40"
              >
                <MessageSquarePlus size={13} /> Comment
              </button>
            )}
            {credits.data && (
              <span className={`text-[11px] ${creditsLow ? "text-rose-600" : "text-slate-500"}`}>
                {credits.data.remaining}/{credits.data.limit} AI credits left
              </span>
            )}
          </div>
        ) : (
          <div className="flex flex-wrap items-center gap-2 rounded-xl border border-accent/30 bg-accent/5 p-2">
            <Sparkles size={14} className="ml-1 shrink-0 text-accent" />
            <input
              autoFocus
              value={instruction}
              onChange={(e) => setInstruction(e.target.value)}
              onKeyDown={(e) => {
                if (e.key === "Enter") runAssist();
                if (e.key === "Escape") setAiOpen(false);
              }}
              placeholder="Draft this section, make it more concise, add examples…"
              className="min-w-0 flex-1 rounded-lg border border-slate-200 bg-slate-50 px-3 py-1.5 text-sm text-slate-900 outline-none"
            />
            <Button size="sm" onClick={runAssist} disabled={assist.isPending || !!creditsLow}>
              {assist.isPending ? "Generating…" : creditsLow ? "No credits" : "Generate"}
            </Button>
            <button onClick={() => setAiOpen(false)} className="px-1 text-xs text-slate-500 hover:text-slate-900">
              Cancel
            </button>
          </div>
        )}
        {aiError && <p className="mt-1 text-xs text-rose-600">{aiError}</p>}

        {/* Inline review-comment composer, anchored to the selected text */}
        {pendingAnchor && (
          <div className="mt-2 rounded-xl border border-amber-400/30 bg-amber-400/[0.06] p-2.5">
            <p className="line-clamp-2 border-l-2 border-amber-400/50 pl-2 text-xs italic text-slate-600">
              “{pendingAnchor.quote}”
            </p>
            <div className="mt-2 flex items-center gap-2">
              <input
                autoFocus
                value={commentBody}
                onChange={(e) => setCommentBody(e.target.value)}
                onKeyDown={(e) => {
                  if (e.key === "Enter") submitComment();
                  if (e.key === "Escape") setPendingAnchor(null);
                }}
                placeholder="Comment on this passage…"
                className="min-w-0 flex-1 rounded-lg border border-slate-200 bg-slate-50 px-3 py-1.5 text-sm text-slate-900 outline-none"
              />
              <Button size="sm" onClick={submitComment} disabled={!commentBody.trim() || addReviewComment.isPending}>
                {addReviewComment.isPending ? "Saving…" : "Comment"}
              </Button>
              <button onClick={() => setPendingAnchor(null)} className="px-1 text-xs text-slate-500 hover:text-slate-900">
                Cancel
              </button>
            </div>
          </div>
        )}
      </div>

      <div className="mt-3 rounded-2xl border border-slate-200 bg-slate-50 p-5">
        <EditorContent editor={editor} />
      </div>
    </div>
  );
}

function Presence({ peers, connecting }: { peers: CollabPeer[]; connecting: boolean }) {
  if (connecting) return null;
  return (
    <span className="inline-flex items-center gap-1.5 text-xs text-slate-500" title="People editing now">
      <Users size={13} />
      {peers.length === 0 ? (
        "Only you"
      ) : (
        <span className="flex items-center -space-x-1.5">
          {peers.slice(0, 4).map((p) => (
            <span
              key={p.clientId}
              className="grid h-5 w-5 place-items-center rounded-full border border-cosmos-900 text-[9px] font-semibold text-slate-900"
              style={{ backgroundColor: p.color }}
              title={p.name}
            >
              {p.name.charAt(0).toUpperCase()}
            </span>
          ))}
          {peers.length > 4 && <span className="pl-2 text-[10px]">+{peers.length - 4}</span>}
        </span>
      )}
    </span>
  );
}

function SaveIndicator({ status }: { status: string }) {
  const map: Record<string, { icon: React.ReactNode; text: string; cls: string }> = {
    saving: { icon: <Loader2 size={13} className="animate-spin" />, text: "Saving…", cls: "text-slate-500" },
    saved: { icon: <Check size={13} />, text: "Saved", cls: "text-emerald-600" },
    offline: { icon: <CloudOff size={13} />, text: "Offline — will sync", cls: "text-amber-600" },
    error: { icon: <CloudOff size={13} />, text: "Save failed — buffered", cls: "text-rose-600" },
    conflict: { icon: <AlertTriangle size={13} />, text: "Conflict", cls: "text-amber-600" },
  };
  const s = map[status];
  if (!s) return null;
  return <span className={`inline-flex items-center gap-1.5 text-xs ${s.cls}`}>{s.icon} {s.text}</span>;
}

function Toolbar({ editor }: { editor: NonNullable<ReturnType<typeof useEditor>> }) {
  const btn = (active: boolean) =>
    `grid h-8 w-8 place-items-center rounded-lg border text-slate-600 transition-colors ${
      active ? "border-accent/60 bg-accent/10 text-slate-900" : "border-slate-200 hover:border-slate-300"
    }`;
  return (
    <div className="flex flex-wrap gap-1.5">
      <button type="button" className={btn(editor.isActive("bold"))} onClick={() => editor.chain().focus().toggleBold().run()} aria-label="Bold"><Bold size={15} /></button>
      <button type="button" className={btn(editor.isActive("italic"))} onClick={() => editor.chain().focus().toggleItalic().run()} aria-label="Italic"><Italic size={15} /></button>
      <button type="button" className={btn(editor.isActive("heading", { level: 2 }))} onClick={() => editor.chain().focus().toggleHeading({ level: 2 }).run()} aria-label="Heading 2"><Heading2 size={15} /></button>
      <button type="button" className={btn(editor.isActive("heading", { level: 3 }))} onClick={() => editor.chain().focus().toggleHeading({ level: 3 }).run()} aria-label="Heading 3"><Heading3 size={15} /></button>
      <button type="button" className={btn(editor.isActive("bulletList"))} onClick={() => editor.chain().focus().toggleBulletList().run()} aria-label="Bullet list"><List size={15} /></button>
      <button type="button" className={btn(editor.isActive("orderedList"))} onClick={() => editor.chain().focus().toggleOrderedList().run()} aria-label="Ordered list"><ListOrdered size={15} /></button>
      <button type="button" className={btn(editor.isActive("blockquote"))} onClick={() => editor.chain().focus().toggleBlockquote().run()} aria-label="Quote"><Quote size={15} /></button>

      <span className="mx-0.5 w-px self-stretch bg-slate-100" />

      {/* Typeface */}
      <select
        aria-label="Font"
        className="h-8 rounded-lg border border-slate-200 bg-white px-2 text-xs text-slate-600 outline-none hover:border-slate-300"
        value={editor.getAttributes("textStyle").fontFamily ?? ""}
        onChange={(e) => {
          const v = e.target.value;
          if (v) editor.chain().focus().setFontFamily(v).run();
          else editor.chain().focus().unsetFontFamily().run();
        }}
      >
        {FONT_FAMILIES.map((f) => (
          <option key={f.label} value={f.value}>{f.label}</option>
        ))}
      </select>

      {/* Font size */}
      <select
        aria-label="Font size"
        className="h-8 rounded-lg border border-slate-200 bg-white px-2 text-xs text-slate-600 outline-none hover:border-slate-300"
        value={editor.getAttributes("textStyle").fontSize ?? ""}
        onChange={(e) => {
          const v = e.target.value;
          if (v) editor.chain().focus().setFontSize(v).run();
          else editor.chain().focus().unsetFontSize().run();
        }}
      >
        <option value="">Size</option>
        {FONT_SIZES.map((s) => (
          <option key={s} value={s}>{s.replace("px", "")}</option>
        ))}
      </select>

      {/* Text colour */}
      <div className="flex items-center gap-1">
        <label className={`${btn(false)} relative cursor-pointer`} aria-label="Text colour" title="Text colour">
          <Palette size={15} />
          <input
            type="color"
            className="absolute inset-0 cursor-pointer opacity-0"
            value={editor.getAttributes("textStyle").color ?? "#e2e8f0"}
            onChange={(e) => editor.chain().focus().setColor(e.target.value).run()}
          />
        </label>
        {TEXT_COLORS.map((c) => (
          <button
            key={c}
            type="button"
            aria-label={`Colour ${c}`}
            title={c}
            onClick={() => editor.chain().focus().setColor(c).run()}
            className="h-5 w-5 rounded-full border border-slate-300 transition-transform hover:scale-110"
            style={{ backgroundColor: c }}
          />
        ))}
        <button
          type="button"
          onClick={() => editor.chain().focus().unsetColor().run()}
          className="grid h-8 w-8 place-items-center rounded-lg border border-slate-200 text-slate-500 transition-colors hover:border-slate-300"
          aria-label="Clear colour"
          title="Clear colour"
        >
          <Ban size={14} />
        </button>
      </div>
    </div>
  );
}
