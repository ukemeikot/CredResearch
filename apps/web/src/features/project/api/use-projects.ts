"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { api, type ProjectSummary } from "@/lib/api";

export function useProjects() {
  return useQuery({ queryKey: ["projects"], queryFn: api.listProjects });
}

export function useCreateProject() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: api.createProject,
    onSuccess: (created) =>
      qc.setQueryData<ProjectSummary[]>(["projects"], (old) => [created, ...(old ?? [])]),
  });
}
