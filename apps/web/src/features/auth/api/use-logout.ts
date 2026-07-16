"use client";

import { useQueryClient } from "@tanstack/react-query";
import { useCallback } from "react";
import { api } from "@/lib/api";
import { useAuth } from "@/lib/auth-store";

/**
 * Log out: best-effort revoke the refresh token server-side, then clear local
 * session + cached server state. Never throws — logout must always succeed
 * locally even if the network call fails.
 */
export function useLogout() {
  const qc = useQueryClient();
  return useCallback(async () => {
    const { refreshToken, clear } = useAuth.getState();
    try {
      if (refreshToken) await api.logout(refreshToken);
    } catch {
      /* ignore — revoke is best-effort */
    } finally {
      clear();
      qc.clear();
    }
  }, [qc]);
}
