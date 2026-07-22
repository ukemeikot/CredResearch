"use client";

import { useMutation } from "@tanstack/react-query";
import { api } from "@/lib/api";
import { useAuth } from "@/lib/auth-store";

/** Login mutation: authenticates and stores the session. */
export function useLogin() {
  const setUser = useAuth((s) => s.setUser);
  return useMutation({
    mutationFn: api.login,
    onSuccess: (res) =>
      setUser(res.user),
  });
}
