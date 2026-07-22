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
  const setUser = useAuth((s) => s.setUser);
  return useMutation({
    mutationFn: async (input: RegisterInput) => {
      await api.register(input);
      return api.login({ email: input.email, password: input.password });
    },
    onSuccess: (res) => setUser(res.user),
  });
}
