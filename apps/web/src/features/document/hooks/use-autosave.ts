"use client";

import { useCallback, useEffect, useRef, useState } from "react";
import { useQueryClient } from "@tanstack/react-query";
import { ApiError, api, type DocSection, type DocumentDetail } from "@/lib/api";
import { documentKeys } from "../api/use-documents";

export type SaveStatus = "idle" | "saving" | "saved" | "offline" | "conflict" | "error";

const KEY_PREFIX = "cr-doc-buffer:";
const bufferKey = (sectionId: string) => `${KEY_PREFIX}${sectionId}`;

/** Reads any locally-buffered (offline/failed) content for a section, so the editor can seed from it. */
export function readSectionBuffer(sectionId: string): unknown | null {
  if (typeof localStorage === "undefined") return null;
  try {
    const raw = localStorage.getItem(bufferKey(sectionId));
    return raw ? JSON.parse(raw) : null;
  } catch {
    return null;
  }
}

/**
 * Section autosave with optimistic locking (FR-DOC-3). Saves against the last known version and
 * writes the server result back into the document cache so navigating between sections always
 * shows the latest content and never sends a stale version. Concurrent saves are serialized (the
 * latest queued content runs after the in-flight one). Edits made offline are buffered in
 * localStorage and flushed on reconnect, so authoring survives a flaky connection.
 */
export function useSectionAutosave(docId: string, section: DocSection, options?: { collab?: boolean }) {
  const collab = options?.collab ?? false;
  const qc = useQueryClient();
  const [status, setStatus] = useState<SaveStatus>("idle");
  const versionRef = useRef(section.version);
  const inFlight = useRef(false);
  const queued = useRef<{ content: unknown } | null>(null);

  // Seed the working version when switching to a different section. NOT keyed on section.version,
  // so the hook's own save-driven version bumps (written back to cache) don't reset our ref.
  useEffect(() => {
    versionRef.current = section.version;
    setStatus("idle");
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [section.id]);

  const doSave = useCallback(
    async (content: unknown) => {
      const key = bufferKey(section.id);
      if (typeof navigator !== "undefined" && !navigator.onLine) {
        try {
          localStorage.setItem(key, JSON.stringify(content));
        } catch {
          /* best effort */
        }
        setStatus("offline");
        return;
      }
      setStatus("saving");
      try {
        const res = await api.autosaveSection(docId, section.id, {
          content,
          version: versionRef.current,
        });
        versionRef.current = res.version;
        // Keep the document cache current so section navigation shows saved content + version.
        qc.setQueryData<DocumentDetail>(documentKeys.doc(docId), (old) =>
          old
            ? {
                ...old,
                sections: old.sections.map((s) =>
                  s.id === section.id ? { ...s, content: res.content, version: res.version } : s,
                ),
              }
            : old,
        );
        try {
          localStorage.removeItem(key);
        } catch {
          /* ignore */
        }
        setStatus("saved");
      } catch (e) {
        if (e instanceof ApiError && e.status === 409) {
          // In collaboration mode Yjs has already merged everyone's edits, so a version conflict
          // just means our cached version is stale (e.g. leadership just changed hands). Re-sync
          // the version from the server and retry once, silently — no conflict UI.
          if (collab) {
            try {
              const latest = await api.getSection(docId, section.id);
              versionRef.current = latest.version;
              const res = await api.autosaveSection(docId, section.id, {
                content,
                version: versionRef.current,
              });
              versionRef.current = res.version;
              qc.setQueryData<DocumentDetail>(documentKeys.doc(docId), (old) =>
                old
                  ? {
                      ...old,
                      sections: old.sections.map((s) =>
                        s.id === section.id ? { ...s, content: res.content, version: res.version } : s,
                      ),
                    }
                  : old,
              );
              setStatus("saved");
            } catch {
              setStatus("error");
            }
            return;
          }
          setStatus("conflict");
          return;
        }
        try {
          localStorage.setItem(key, JSON.stringify(content));
        } catch {
          /* ignore */
        }
        setStatus("error");
      }
    },
    [docId, section.id, qc, collab],
  );

  // Serialize saves: never let two run concurrently with the same (stale) version. A save that
  // arrives while another is in flight is coalesced to the latest content and run afterwards.
  const save = useCallback(
    (content: unknown) => {
      if (inFlight.current) {
        queued.current = { content };
        return;
      }
      inFlight.current = true;
      void doSave(content).finally(() => {
        inFlight.current = false;
        if (queued.current) {
          const next = queued.current.content;
          queued.current = null;
          save(next);
        }
      });
    },
    [doSave],
  );

  // Flush buffered offline edits on mount and whenever connectivity returns.
  useEffect(() => {
    const key = bufferKey(section.id);
    function flush() {
      if (typeof navigator !== "undefined" && !navigator.onLine) return;
      const raw = typeof localStorage !== "undefined" ? localStorage.getItem(key) : null;
      if (!raw) return;
      try {
        save(JSON.parse(raw));
      } catch {
        try {
          localStorage.removeItem(key);
        } catch {
          /* ignore */
        }
      }
    }
    window.addEventListener("online", flush);
    flush();
    return () => window.removeEventListener("online", flush);
  }, [section.id, save]);

  return { status, save };
}
