"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { api, type Institution } from "@/lib/api";

/** A single institution. Only DEPARTMENT_ADMIN+ may read it. */
export function useInstitution(id: string | null | undefined, enabled = true) {
  return useQuery({
    queryKey: ["institution", id],
    queryFn: () => api.getInstitution(id as string),
    enabled: !!id && enabled,
    staleTime: 5 * 60_000,
  });
}

export function useUpdateInstitution(id: string) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (b: { name?: string; country?: string; type?: string }) =>
      api.updateInstitution(id, b),
    onSuccess: (updated) => qc.setQueryData<Institution>(["institution", id], updated),
  });
}
