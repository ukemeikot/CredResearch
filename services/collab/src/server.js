// CredResearch real-time collaboration server (Yjs over WebSocket, via Hocuspocus).
//
// Documents live in memory only: clients converge here for live editing + presence, while the
// authoritative copy is persisted by the app (one client debounce-saves the ProseMirror JSON
// through the existing autosave API). When the last client disconnects the in-memory doc is
// dropped and re-seeded from that persisted content on the next open. This keeps the server
// stateless (no DB) — matching the staging-only, cost-conscious deployment.
//
// Every connection must present a valid RS256 access token (the same token the SPA uses for the
// API). We verify the signature with the backend's public key; an invalid/expired token is
// rejected before the socket joins a room.

import { Server } from "@hocuspocus/server";
import jwt from "jsonwebtoken";

const PORT = Number(process.env.PORT || 1234);

// The backend's RS256 public key (PEM). Env may deliver "\n" as a literal — normalise it.
const PUBLIC_KEY = (process.env.JWT_PUBLIC_KEY || "").replace(/\\n/g, "\n");

if (!PUBLIC_KEY) {
  // Fail fast rather than silently accepting everyone.
  console.error("[collab] JWT_PUBLIC_KEY is not set — refusing to start.");
  process.exit(1);
}

const server = Server.configure({
  port: PORT,
  name: "credresearch-collab",

  async onAuthenticate({ token, documentName }) {
    if (!token) {
      throw new Error("Missing access token");
    }
    let payload;
    try {
      payload = jwt.verify(token, PUBLIC_KEY, { algorithms: ["RS256"] });
    } catch (err) {
      throw new Error(`Invalid access token: ${err.message}`);
    }
    // Room names are `${documentId}:${sectionId}`; we trust a valid token holder here (staging).
    // Returning a context attaches it to the connection (available to other hooks if needed).
    return {
      userId: payload.sub,
      documentName,
    };
  },

  async onListen() {
    console.log(`[collab] listening on :${PORT}`);
  },
});

server.listen();
