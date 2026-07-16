"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { api, type Invitation, type ProjectMemberRole } from "@/lib/api";

const key = (projectId: string) => ["invitations", projectId] as const;

export function useInvitations(projectId: string, enabled = true) {
  return useQuery({
    queryKey: key(projectId),
    queryFn: () => api.listInvitations(projectId),
    enabled: enabled && !!projectId,
  });
}

export function useInvite(projectId: string) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (b: { email: string; role: ProjectMemberRole }) => api.invite(projectId, b),
    onSuccess: (created) =>
      qc.setQueryData<Invitation[]>(key(projectId), (old) => [created, ...(old ?? [])]),
  });
}

export function useRevokeInvite(projectId: string) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (invitationId: string) => api.revokeInvite(projectId, invitationId),
    onSuccess: (_r, invitationId) =>
      qc.setQueryData<Invitation[]>(key(projectId), (old) =>
        old?.filter((i) => i.id !== invitationId),
      ),
  });
}

export function useAcceptInvite() {
  return useMutation({ mutationFn: (token: string) => api.acceptInvite(token) });
}
