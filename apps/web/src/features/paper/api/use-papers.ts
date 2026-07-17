"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { api } from "@/lib/api";

const papersKey = (projectId: string) => ["papers", projectId] as const;
const refsKey = (projectId: string, style: string) => ["references", projectId, style] as const;

export function usePapers(projectId: string) {
  return useQuery({ queryKey: papersKey(projectId), queryFn: () => api.listPapers(projectId), enabled: !!projectId });
}

export function useReferences(projectId: string, style: string) {
  return useQuery({
    queryKey: refsKey(projectId, style),
    queryFn: () => api.references(projectId, style),
    enabled: !!projectId,
  });
}

export function useUploadPaper(projectId: string) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (file: File) => api.uploadPaper(projectId, file),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: papersKey(projectId) });
      qc.invalidateQueries({ queryKey: ["references", projectId] });
    },
  });
}

export function useUpdatePaper(projectId: string) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (v: {
      id: string;
      body: { title?: string; authors?: string; year?: number; doi?: string; journal?: string };
    }) => api.updatePaper(v.id, v.body),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: papersKey(projectId) });
      qc.invalidateQueries({ queryKey: ["references", projectId] });
    },
  });
}

export function useAskPapers(projectId: string) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (question: string) => api.askPapers(projectId, question),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["ai-credits"] }),
  });
}

export function useSummarizePaper(projectId: string) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (id: string) => api.summarizePaper(id),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: papersKey(projectId) });
      qc.invalidateQueries({ queryKey: ["ai-credits"] });
    },
  });
}

export function useDeletePaper(projectId: string) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (id: string) => api.deletePaper(id),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: papersKey(projectId) });
      qc.invalidateQueries({ queryKey: ["references", projectId] });
    },
  });
}
