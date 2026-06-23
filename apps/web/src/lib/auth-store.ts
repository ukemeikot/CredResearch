"use client";

import { create } from "zustand";
import { persist } from "zustand/middleware";

export interface SessionUser {
  id: string;
  roles: string[];
  institutionId: string;
  plan: string;
}

interface AuthState {
  accessToken: string | null;
  refreshToken: string | null;
  user: SessionUser | null;
  hydrated: boolean;
  setSession: (t: { accessToken: string; refreshToken: string; user: SessionUser }) => void;
  clear: () => void;
}

export const useAuth = create<AuthState>()(
  persist(
    (set) => ({
      accessToken: null,
      refreshToken: null,
      user: null,
      hydrated: false,
      setSession: ({ accessToken, refreshToken, user }) =>
        set({ accessToken, refreshToken, user }),
      clear: () => set({ accessToken: null, refreshToken: null, user: null }),
    }),
    {
      name: "credresearch-auth",
      onRehydrateStorage: () => (state) => {
        if (state) state.hydrated = true;
      },
    },
  ),
);

/** Read token outside React (for the fetch client). */
export function getAccessToken(): string | null {
  if (typeof window === "undefined") return null;
  try {
    const raw = localStorage.getItem("credresearch-auth");
    if (!raw) return null;
    return JSON.parse(raw)?.state?.accessToken ?? null;
  } catch {
    return null;
  }
}
