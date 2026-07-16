"use client";

import { GlassCard } from "@/components/ui/glass-card";
import { Button } from "@/components/ui/button";
import { ApiError, type ProjectStatus } from "@/lib/api";
import { useTransitionStatus } from "../api/use-projects";
import { formatStatus, nextStatuses } from "../model/project-status";

export function StatusControl({
  id,
  status,
  canManage,
}: {
  id: string;
  status: ProjectStatus;
  canManage: boolean;
}) {
  const transition = useTransitionStatus(id);
  const options = nextStatuses(status);

  const error = transition.error instanceof ApiError ? transition.error.message : null;

  return (
    <GlassCard className="p-6">
      <p className="text-xs font-medium uppercase tracking-wider text-slate-400">Lifecycle</p>
      <p className="mt-2 font-display text-lg text-white">{formatStatus(status)}</p>

      {!canManage ? (
        <p className="mt-3 text-xs text-slate-500">
          Only the owner or a supervisor can advance the status.
        </p>
      ) : options.length === 0 ? (
        <p className="mt-3 text-xs text-slate-500">This project has reached the end of its lifecycle.</p>
      ) : (
        <div className="mt-4 flex flex-wrap gap-2">
          {options.map((next) => (
            <Button
              key={next}
              size="sm"
              variant="outline"
              disabled={transition.isPending}
              onClick={() => transition.mutate(next)}
            >
              {transition.isPending ? "…" : `Move to ${formatStatus(next)}`}
            </Button>
          ))}
        </div>
      )}

      {error && <p className="mt-3 text-sm text-rose-400">{error}</p>}
    </GlassCard>
  );
}
