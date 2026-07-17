"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { api } from "@/lib/api";

const CREDITS_KEY = ["ai-credits"] as const;

/** This month's AI credit usage for the caller's plan. */
export function useAiCredits() {
  return useQuery({ queryKey: CREDITS_KEY, queryFn: api.aiCredits, staleTime: 30_000 });
}

/** Draft/improve a document section via the AI worker; refreshes credit usage after. */
export function useSectionAssist() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: api.aiSectionAssist,
    onSuccess: () => qc.invalidateQueries({ queryKey: CREDITS_KEY }),
  });
}

export function useAiTopics() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: api.aiTopics,
    onSuccess: () => qc.invalidateQueries({ queryKey: CREDITS_KEY }),
  });
}

export function useAiAlignment() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: api.aiAlignment,
    onSuccess: () => qc.invalidateQueries({ queryKey: CREDITS_KEY }),
  });
}

// ── AI-Use Disclosure Ledger ─────────────────────────────────────────────────
export function useDisclosure(docId: string, enabled: boolean) {
  return useQuery({
    queryKey: ["disclosure", docId],
    queryFn: () => api.disclosureList(docId),
    enabled: enabled && !!docId,
  });
}

export function useDisclosureAppend(docId: string) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (b: {
      documentSectionId?: string;
      featureKey: string;
      model?: string;
      suggestionSummary?: string;
      action?: string;
    }) => api.disclosureAppend(docId, b),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["disclosure", docId] }),
  });
}
