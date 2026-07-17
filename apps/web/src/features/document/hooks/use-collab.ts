"use client";

import { useEffect, useMemo, useRef, useState } from "react";
import { HocuspocusProvider } from "@hocuspocus/provider";
import * as Y from "yjs";
import { useAuth } from "@/lib/auth-store";

/**
 * Real-time collaboration wiring (Yjs). Enabled only where a collab server is reachable — local
 * dev and staging — so production keeps the optimistic-lock autosave model untouched (the same web
 * image serves both; the gate is on the host). See services/collab.
 */

export interface CollabPeer {
  clientId: number;
  name: string;
  color: string;
}

/** Resolve the collab WebSocket endpoint for the current host, or null if collab is disabled here. */
export function collabEndpoint(): string | null {
  if (typeof window === "undefined") return null;
  const host = window.location.hostname;
  if (host === "localhost" || host === "127.0.0.1") return "ws://localhost:1234";
  if (host.startsWith("staging.")) return `wss://${window.location.host}/collab`;
  return null; // production (and anything else): disabled
}

// Deterministic, readable cursor colour per user.
const COLORS = ["#6366f1", "#ec4899", "#14b8a6", "#f59e0b", "#8b5cf6", "#ef4444", "#10b981", "#3b82f6"];
function colorFor(id: string): string {
  let h = 0;
  for (let i = 0; i < id.length; i++) h = (h * 31 + id.charCodeAt(i)) >>> 0;
  return COLORS[h % COLORS.length];
}

export interface CollabState {
  enabled: boolean; // a collab server is expected on this host
  unavailable: boolean; // enabled, but the server didn't connect in time → fall back to autosave
  ydoc: Y.Doc | null;
  provider: HocuspocusProvider | null;
  isLeader: boolean; // exactly one connected client persists to the API
  peers: CollabPeer[];
  synced: boolean;
  connected: boolean;
  user: { name: string; color: string };
}

// If the collab socket hasn't connected within this window, fall back to plain autosave editing
// (e.g. the collab service isn't deployed in this environment yet) rather than blocking the editor.
const CONNECT_TIMEOUT_MS = 8000;

/**
 * Creates a Yjs document + Hocuspocus provider for one section (room = `${docId}:${sectionId}`),
 * tracks connected peers via awareness, and elects a single leader (lowest client id) responsible
 * for persistence. Torn down and rebuilt when the section changes.
 */
export function useCollab(docId: string, sectionId: string, userName: string, userId: string): CollabState {
  const endpoint = useMemo(() => collabEndpoint(), []);
  const enabled = !!endpoint;
  const token = useAuth((s) => s.accessToken);
  const user = useMemo(() => ({ name: userName || "Anonymous", color: colorFor(userId || sectionId) }), [
    userName,
    userId,
    sectionId,
  ]);

  const [provider, setProvider] = useState<HocuspocusProvider | null>(null);
  const [ydoc, setYdoc] = useState<Y.Doc | null>(null);
  const [isLeader, setIsLeader] = useState(false);
  const [synced, setSynced] = useState(false);
  const [connected, setConnected] = useState(false);
  const [unavailable, setUnavailable] = useState(false);
  const [peers, setPeers] = useState<CollabPeer[]>([]);
  const clientIdRef = useRef<number | null>(null);

  useEffect(() => {
    if (!enabled || !endpoint || !token) return;
    setConnected(false);
    setUnavailable(false);
    setSynced(false);
    const doc = new Y.Doc();
    clientIdRef.current = doc.clientID;
    const p = new HocuspocusProvider({
      url: endpoint,
      name: `${docId}:${sectionId}`,
      document: doc,
      token,
    });
    setYdoc(doc);
    setProvider(p);

    // Give up (fall back to plain autosave) if we never connect — e.g. no collab server here.
    const timeout = setTimeout(() => setUnavailable(true), CONNECT_TIMEOUT_MS);
    p.on("synced", () => setSynced(true));
    p.on("status", ({ status }: { status: string }) => {
      if (status === "connected") {
        clearTimeout(timeout); // connected in time → never fall back
        setConnected(true);
        setUnavailable(false);
      } else {
        setConnected(false);
      }
    });

    const awareness = p.awareness;
    if (awareness) {
      awareness.setLocalStateField("user", user);
      const onChange = () => {
        const states = awareness.getStates();
        const ids = [...states.keys()];
        const min = ids.length ? Math.min(...ids) : doc.clientID;
        setIsLeader(doc.clientID === min);
        setPeers(
          ids
            .filter((id) => id !== doc.clientID)
            .map((id) => {
              const u = (states.get(id)?.user ?? {}) as { name?: string; color?: string };
              return { clientId: id, name: u.name ?? "Anonymous", color: u.color ?? "#94a3b8" };
            }),
        );
      };
      awareness.on("change", onChange);
      onChange();
      return () => {
        clearTimeout(timeout);
        awareness.off("change", onChange);
        p.destroy();
        doc.destroy();
        setProvider(null);
        setYdoc(null);
        setSynced(false);
        setIsLeader(false);
        setConnected(false);
        setPeers([]);
      };
    }

    return () => {
      clearTimeout(timeout);
      p.destroy();
      doc.destroy();
      setProvider(null);
      setYdoc(null);
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [enabled, endpoint, token, docId, sectionId]);

  return { enabled, unavailable, ydoc, provider, isLeader, peers, synced, connected, user };
}
