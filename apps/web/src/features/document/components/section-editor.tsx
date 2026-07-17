"use client";

import { useEffect, useRef, useState } from "react";
import { EditorContent, useEditor, type Content } from "@tiptap/react";
import StarterKit from "@tiptap/starter-kit";
import Collaboration from "@tiptap/extension-collaboration";
import CollaborationCaret from "@tiptap/extension-collaboration-caret";
import {
  AlertTriangle,
  Bold,
  Check,
  CloudOff,
  Heading2,
  Heading3,
  History,
  Italic,
  List,
  ListOrdered,
  Loader2,
  Quote,
  Sparkles,
  Users,
} from "lucide-react";
import { Button } from "@/components/ui/button";
import { ApiError, type DocSection } from "@/lib/api";
import { useMe } from "@/features/user/api/use-me";
import { useAiCredits, useDisclosureAppend, useSectionAssist } from "../api/use-ai";
import { readSectionBuffer, useSectionAutosave } from "../hooks/use-autosave";
import { useCollab, type CollabPeer } from "../hooks/use-collab";

const AUTOSAVE_DELAY = 1200;

export function SectionEditor({
  docId,
  section,
  onReload,
  onOpenHistory,
}: {
  docId: string;
  section: DocSection;
  onReload: () => void;
  onOpenHistory: () => void;
}) {
  const me = useMe();
  const collab = useCollab(docId, section.id, me.data?.fullName ?? "", me.data?.id ?? "");
  const collabReady = collab.enabled && !!collab.ydoc && !!collab.provider;
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
  const [aiOpen, setAiOpen] = useState(false);
  const [instruction, setInstruction] = useState("");

  // Prefer locally-buffered offline edits over the (possibly older) server content.
  const initialContent = (readSectionBuffer(section.id) ?? section.content ?? "") as Content;

  function scheduleSave(json: unknown) {
    latest.current = json;
    dirty.current = true;
    if (timer.current) clearTimeout(timer.current);
    timer.current = setTimeout(() => {
      dirty.current = false;
      // Non-collab: always persist. Collab: only the leader writes back to the API.
      if (!collab.enabled || isLeaderRef.current) saveRef.current(json);
    }, AUTOSAVE_DELAY);
  }

  const editor = useEditor(
    {
      extensions: collabReady
        ? [
            // Collaboration provides its own (shared) undo history — disable StarterKit's.
            StarterKit.configure({ undoRedo: false }),
            Collaboration.configure({ document: collab.ydoc! }),
            CollaborationCaret.configure({ provider: collab.provider!, user: collab.user }),
          ]
        : [StarterKit],
      // In collab mode content comes from Yjs (seeded once, below) — don't set it here.
      content: collabReady ? undefined : initialContent,
      immediatelyRender: false,
      editorProps: {
        attributes: {
          class: "tiptap min-h-[52vh] w-full max-w-none px-1 py-2 text-slate-200 outline-none",
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

  // Prevent typing until the live session is connected (avoids edits that wouldn't be shared/saved).
  useEffect(() => {
    if (!editor) return;
    editor.setEditable(!(collab.enabled && !collabReady));
  }, [editor, collab.enabled, collabReady]);

  // A fresh Yjs doc is built per section — allow the next section to seed again.
  useEffect(() => {
    seededRef.current = false;
  }, [section.id]);

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
          <h2 className="font-display text-xl font-bold text-white">{section.heading}</h2>
        </div>
        <div className="flex items-center gap-3">
          {collab.enabled && <Presence peers={collab.peers} connecting={!collabReady} />}
          <SaveIndicator status={status} />
          <button
            onClick={onOpenHistory}
            className="inline-flex items-center gap-1.5 rounded-full border border-white/10 px-3 py-1.5 text-xs text-slate-300 transition-colors hover:border-white/30 hover:text-white"
          >
            <History size={14} /> History
          </button>
        </div>
      </div>

      {collab.enabled && !collabReady && (
        <div className="mb-4 flex items-center gap-2 rounded-xl border border-white/10 bg-white/[0.02] px-4 py-3 text-sm text-slate-400">
          <Loader2 size={16} className="animate-spin" /> Connecting to the live editing session…
        </div>
      )}

      {status === "conflict" && (
        <div className="mb-4 flex items-center justify-between gap-3 rounded-xl border border-amber-400/40 bg-amber-400/10 px-4 py-3 text-sm text-amber-200">
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
            {credits.data && (
              <span className={`text-[11px] ${creditsLow ? "text-rose-400" : "text-slate-500"}`}>
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
              className="min-w-0 flex-1 rounded-lg border border-white/10 bg-white/[0.05] px-3 py-1.5 text-sm text-white outline-none"
            />
            <Button size="sm" onClick={runAssist} disabled={assist.isPending || !!creditsLow}>
              {assist.isPending ? "Generating…" : creditsLow ? "No credits" : "Generate"}
            </Button>
            <button onClick={() => setAiOpen(false)} className="px-1 text-xs text-slate-400 hover:text-white">
              Cancel
            </button>
          </div>
        )}
        {aiError && <p className="mt-1 text-xs text-rose-400">{aiError}</p>}
      </div>

      <div className="mt-3 rounded-2xl border border-white/10 bg-white/[0.02] p-5">
        <EditorContent editor={editor} />
      </div>
    </div>
  );
}

function Presence({ peers, connecting }: { peers: CollabPeer[]; connecting: boolean }) {
  if (connecting) return null;
  return (
    <span className="inline-flex items-center gap-1.5 text-xs text-slate-400" title="People editing now">
      <Users size={13} />
      {peers.length === 0 ? (
        "Only you"
      ) : (
        <span className="flex items-center -space-x-1.5">
          {peers.slice(0, 4).map((p) => (
            <span
              key={p.clientId}
              className="grid h-5 w-5 place-items-center rounded-full border border-cosmos-900 text-[9px] font-semibold text-white"
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
    saving: { icon: <Loader2 size={13} className="animate-spin" />, text: "Saving…", cls: "text-slate-400" },
    saved: { icon: <Check size={13} />, text: "Saved", cls: "text-emerald-400" },
    offline: { icon: <CloudOff size={13} />, text: "Offline — will sync", cls: "text-amber-300" },
    error: { icon: <CloudOff size={13} />, text: "Save failed — buffered", cls: "text-rose-300" },
    conflict: { icon: <AlertTriangle size={13} />, text: "Conflict", cls: "text-amber-300" },
  };
  const s = map[status];
  if (!s) return null;
  return <span className={`inline-flex items-center gap-1.5 text-xs ${s.cls}`}>{s.icon} {s.text}</span>;
}

function Toolbar({ editor }: { editor: NonNullable<ReturnType<typeof useEditor>> }) {
  const btn = (active: boolean) =>
    `grid h-8 w-8 place-items-center rounded-lg border text-slate-300 transition-colors ${
      active ? "border-accent/60 bg-accent/10 text-white" : "border-white/10 hover:border-white/30"
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
    </div>
  );
}
