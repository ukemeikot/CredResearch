"use client";

import { useMutation } from "@tanstack/react-query";
import { api } from "@/lib/api";
import { useAuth } from "@/lib/auth-store";

interface RegisterInput {
  email: string;
  password: string;
  fullName: string;
}

/** Register mutation: creates the account, logs in, and stores the session. */
export function useRegister() {
  const setSession = useAuth((s) => s.setSession);
  return useMutation({
    mutationFn: async (input: RegisterInput) => {
      await api.register(input);
      return api.login({ email: input.email, password: input.password });
    },
    onSuccess: (res) =>
      setSession({ accessToken: res.accessToken, refreshToken: res.refreshToken, user: res.user }),
  });
}
