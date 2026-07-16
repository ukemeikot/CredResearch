"use client";

import { useMutation } from "@tanstack/react-query";
import { api } from "@/lib/api";

/** Draft/improve a document section via the AI worker. */
export function useSectionAssist() {
  return useMutation({ mutationFn: api.aiSectionAssist });
}

export function useAiTopics() {
  return useMutation({ mutationFn: api.aiTopics });
}

export function useAiAlignment() {
  return useMutation({ mutationFn: api.aiAlignment });
}
