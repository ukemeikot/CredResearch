"use client";

import { useMutation } from "@tanstack/react-query";
import { api } from "@/lib/api";

/** Create an institution and become its admin. Caller must refresh the session afterward. */
export function useOnboardInstitution() {
  return useMutation({
    mutationFn: (b: { name: string; country?: string; type?: string }) => api.onboardInstitution(b),
  });
}
