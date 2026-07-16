"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
  api,
  type ProjectDetail,
  type ProjectMemberRole,
  type ProjectStatus,
  type ProjectSummary,
} from "@/lib/api";

const keys = {
  all: ["projects"] as const,
  detail: (id: string) => ["project", id] as const,
  activities: (id: string) => ["project", id, "activities"] as const,
};

// The backend paginates; request a generous page so the workspace dashboard's
// counts/grid are accurate without a dedicated pagination UI yet.
const LIST_PAGE = 100;

export function useProjects() {
  return useQuery({ queryKey: keys.all, queryFn: () => api.listProjects({ limit: LIST_PAGE }) });
}

export function useProject(id: string) {
  return useQuery({
    queryKey: keys.detail(id),
    queryFn: () => api.getProject(id),
    enabled: !!id,
  });
}

export function useActivities(id: string) {
  return useQuery({
    queryKey: keys.activities(id),
    queryFn: () => api.listActivities(id),
    enabled: !!id,
  });
}

export function useCreateProject() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: api.createProject,
    onSuccess: (created) =>
      qc.setQueryData<ProjectSummary[]>(keys.all, (old) => [created, ...(old ?? [])]),
  });
}

/** Keep the summary in the list cache in sync when the detail's project changes. */
function patchList(qc: ReturnType<typeof useQueryClient>, updated: ProjectSummary) {
  qc.setQueryData<ProjectSummary[]>(keys.all, (old) =>
    old?.map((p) => (p.id === updated.id ? updated : p)),
  );
}

export function useUpdateProject(id: string) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (b: { title?: string; level?: string; abstractText?: string; departmentId?: string }) =>
      api.updateProject(id, b),
    onSuccess: (updated) => {
      qc.setQueryData<ProjectDetail>(keys.detail(id), (old) =>
        old ? { ...old, project: updated } : old,
      );
      patchList(qc, updated);
    },
  });
}

export function useTransitionStatus(id: string) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (status: ProjectStatus) => api.transitionStatus(id, status),
    onSuccess: (updated) => {
      qc.setQueryData<ProjectDetail>(keys.detail(id), (old) =>
        old ? { ...old, project: updated, dashboard: { ...old.dashboard, status: updated.status } } : old,
      );
      patchList(qc, updated);
      // The transition is recorded as an activity server-side.
      qc.invalidateQueries({ queryKey: keys.activities(id) });
    },
  });
}

// invalidateQueries matches by key prefix, so invalidating ["project", id] also
// refetches ["project", id, "activities"] — one call covers detail + feed.
export function useAddMember(id: string) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (b: { userId: string; role: ProjectMemberRole }) => api.addMember(id, b),
    onSuccess: () => qc.invalidateQueries({ queryKey: keys.detail(id) }),
  });
}

export function useRemoveMember(id: string) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (userId: string) => api.removeMember(id, userId),
    onSuccess: () => qc.invalidateQueries({ queryKey: keys.detail(id) }),
  });
}

export function useAddMilestone(id: string) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (b: { title: string; dueDate?: string; status?: string }) => api.addMilestone(id, b),
    onSuccess: () => qc.invalidateQueries({ queryKey: keys.detail(id) }),
  });
}
