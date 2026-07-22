"use client";

import { create } from "zustand";
import { persist } from "zustand/middleware";

export interface SessionUser {
  id: string;
  roles: string[];
  institutionId: string;
  plan: string;
}

/**
 * Client-side session state. Access/refresh tokens are NO LONGER stored here — they live in
 * HttpOnly cookies (set by the backend, unreadable by JS) so an XSS bug can't exfiltrate them.
 * We keep only the non-sensitive user summary (id, roles, institution, plan) for gating + display;
 * the cookies are the real proof of authentication.
 */
interface AuthState {
  user: SessionUser | null;
  hydrated: boolean;
  setUser: (user: SessionUser) => void;
  clear: () => void;
}

export const useAuth = create<AuthState>()(
  persist(
    (set) => ({
      user: null,
      hydrated: false,
      setUser: (user) => set({ user }),
      clear: () => set({ user: null }),
    }),
    {
      name: "credresearch-auth",
      // Persist only the user summary — never tokens.
      partialize: (s) => ({ user: s.user }),
      onRehydrateStorage: () => (state) => {
        if (state) state.hydrated = true;
      },
    },
  ),
);
