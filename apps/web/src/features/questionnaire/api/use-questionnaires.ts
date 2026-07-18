"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { api } from "@/lib/api";

export function useQuestionnaires(projectId: string) {
  return useQuery({
    queryKey: ["questionnaires", projectId],
    queryFn: () => api.listQuestionnaires(projectId),
    enabled: !!projectId,
  });
}

export function useQuestionnaire(id: string) {
  return useQuery({
    queryKey: ["questionnaire", id],
    queryFn: () => api.getQuestionnaire(id),
    enabled: !!id,
  });
}

export function useGenerateQuestionnaire(projectId: string) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (b: { projectId: string; topic: string; objectives?: string[] }) => api.generateQuestionnaire(b),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["questionnaires", projectId] }),
  });
}

export function useCreateQuestionnaire(projectId: string) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (b: { projectId: string; title: string; consentText?: string }) => api.createQuestionnaire(b),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["questionnaires", projectId] }),
  });
}

export function useUpdateQuestionnaire(id: string) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (b: {
      title?: string;
      consentText?: string;
      questions?: { type: string; prompt: string; options?: unknown; required: boolean }[];
    }) => api.updateQuestionnaire(id, b),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["questionnaire", id] }),
  });
}

export function usePublishQuestionnaire(id: string) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (expiresDays?: number) => api.publishQuestionnaire(id, expiresDays),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["questionnaire", id] }),
  });
}

export function useCloseQuestionnaire(id: string) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: () => api.closeQuestionnaire(id),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["questionnaire", id] }),
  });
}

export function useQuestionnaireResponses(id: string) {
  return useQuery({
    queryKey: ["questionnaire-responses", id],
    queryFn: () => api.questionnaireResponses(id),
    enabled: !!id,
  });
}
