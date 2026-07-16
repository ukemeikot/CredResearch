"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { api, type Department } from "@/lib/api";

const KEY = ["departments"] as const;

/**
 * Departments in the caller's institution. Only DEPARTMENT_ADMIN+ may list them,
 * so callers pass `enabled` to avoid a guaranteed 403 for regular users.
 */
export function useDepartments(enabled = true) {
  return useQuery({
    queryKey: KEY,
    queryFn: api.listDepartments,
    enabled,
    staleTime: 5 * 60_000,
  });
}

export function useCreateDepartment() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: api.createDepartment,
    onSuccess: (created) =>
      qc.setQueryData<Department[]>(KEY, (old) => [...(old ?? []), created]),
  });
}

export function useUpdateDepartment() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ id, ...b }: { id: string; name?: string; code?: string }) =>
      api.updateDepartment(id, b),
    onSuccess: (updated) =>
      qc.setQueryData<Department[]>(KEY, (old) =>
        old?.map((d) => (d.id === updated.id ? updated : d)),
      ),
  });
}
