"use client";

import { useMemo } from "react";
import { useMe } from "@/features/user/api/use-me";
import type { ProjectMemberRole } from "@/lib/api";
import { useProject } from "./use-projects";

/**
 * Shared project + caller-role lookup used across the project's sidebar pages. The caller's
 * project-scoped role drives which controls show (the backend enforces the same rules regardless).
 */
export function useProjectRole(id: string) {
  const me = useMe();
  const query = useProject(id);
  const myRole: ProjectMemberRole | null = useMemo(() => {
    const uid = me.data?.id;
    if (!uid || !query.data) return null;
    return query.data.members.find((m) => m.userId === uid)?.role ?? null;
  }, [me.data?.id, query.data]);
  return {
    query,
    myRole,
    isOwner: myRole === "OWNER",
    canManage: myRole === "OWNER" || myRole === "SUPERVISOR",
  };
}
