"use client";

import { useMutation } from "@tanstack/react-query";
import { api } from "@/lib/api";
import { useAuth } from "@/lib/auth-store";

/** Login mutation: authenticates and stores the session. */
export function useLogin() {
  const setSession = useAuth((s) => s.setSession);
  return useMutation({
    mutationFn: api.login,
    onSuccess: (res) =>
      setSession({ accessToken: res.accessToken, refreshToken: res.refreshToken, user: res.user }),
  });
}
