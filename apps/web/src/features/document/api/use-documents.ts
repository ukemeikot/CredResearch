"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { api, type DocumentDetail, type DocumentSummary } from "@/lib/api";

const keys = {
  templates: ["templates"] as const,
  template: (id: string) => ["template", id] as const,
  byProject: (projectId: string) => ["documents", projectId] as const,
  doc: (id: string) => ["document", id] as const,
  versions: (docId: string, sectionId: string) => ["document", docId, "versions", sectionId] as const,
};

export function useTemplates() {
  return useQuery({ queryKey: keys.templates, queryFn: api.listTemplates, staleTime: 10 * 60_000 });
}

export function useTemplate(id: string | null) {
  return useQuery({
    queryKey: keys.template(id ?? ""),
    queryFn: () => api.getTemplate(id as string),
    enabled: !!id,
    staleTime: 10 * 60_000,
  });
}

export function useProjectDocuments(projectId: string) {
  return useQuery({
    queryKey: keys.byProject(projectId),
    queryFn: () => api.listDocuments(projectId),
    enabled: !!projectId,
  });
}

export function useCreateDocument(projectId: string) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (b: { templateId: string; title?: string }) =>
      api.createDocument({ projectId, ...b }),
    onSuccess: (detail: DocumentDetail) => {
      qc.setQueryData<DocumentSummary[]>(keys.byProject(projectId), (old) => [
        detail.document,
        ...(old ?? []),
      ]);
      qc.setQueryData(keys.doc(detail.document.id), detail);
    },
  });
}

export function useDocument(id: string) {
  return useQuery({
    queryKey: keys.doc(id),
    queryFn: () => api.getDocument(id),
    enabled: !!id,
  });
}

export function useSectionVersions(docId: string, sectionId: string, enabled: boolean) {
  return useQuery({
    queryKey: keys.versions(docId, sectionId),
    queryFn: () => api.listSectionVersions(docId, sectionId),
    enabled: enabled && !!sectionId,
  });
}

export function useRestoreSection(docId: string) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ sectionId, versionId }: { sectionId: string; versionId: string }) =>
      api.restoreSection(docId, sectionId, versionId),
    onSuccess: () => qc.invalidateQueries({ queryKey: keys.doc(docId) }),
  });
}

// ── Section structure editing (add / rename / reorder / delete) ──────────────
export function useAddSection(docId: string) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (b: { heading: string; chapter?: string }) => api.addSection(docId, b),
    onSuccess: () => qc.invalidateQueries({ queryKey: keys.doc(docId) }),
  });
}

export function useUpdateSection(docId: string) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({
      sectionId,
      ...b
    }: {
      sectionId: string;
      heading?: string;
      chapter?: string;
      orderIndex?: number;
    }) => api.updateSection(docId, sectionId, b),
    onSuccess: () => qc.invalidateQueries({ queryKey: keys.doc(docId) }),
  });
}

export function useDeleteSection(docId: string) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (sectionId: string) => api.deleteSection(docId, sectionId),
    onSuccess: () => qc.invalidateQueries({ queryKey: keys.doc(docId) }),
  });
}

export const documentKeys = keys;
