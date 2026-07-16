"use client";

import { useMutation } from "@tanstack/react-query";
import { api } from "@/lib/api";

/** Request a password-reset email. Always resolves (never reveals if the email exists). */
export function useForgotPassword() {
  return useMutation({ mutationFn: (email: string) => api.forgotPassword(email) });
}

/** Consume a reset token and set a new password. */
export function useResetPassword() {
  return useMutation({ mutationFn: api.resetPassword });
}
