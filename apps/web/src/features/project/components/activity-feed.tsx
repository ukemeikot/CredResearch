"use client";

import { Activity as ActivityIcon } from "lucide-react";
import { GlassCard } from "@/components/ui/glass-card";
import type { Activity } from "@/lib/api";
import { useActivities } from "../api/use-projects";
import { formatStatus } from "../model/project-status";

/** Turn an activity row into a short human sentence. */
function describe(a: Activity): string {
  const type = formatStatus(a.type).toLowerCase();
  const actor = a.actorUserId ? `${a.actorUserId.slice(0, 8)}…` : "someone";
  return `${actor} · ${type}`;
}

function timeAgo(iso: string): string {
  const then = new Date(iso).getTime();
  if (Number.isNaN(then)) return "";
  const secs = Math.max(0, Math.round((Date.now() - then) / 1000));
  if (secs < 60) return "just now";
  const mins = Math.round(secs / 60);
  if (mins < 60) return `${mins}m ago`;
  const hrs = Math.round(mins / 60);
  if (hrs < 24) return `${hrs}h ago`;
  return `${Math.round(hrs / 24)}d ago`;
}

export function ActivityFeed({ id }: { id: string }) {
  const query = useActivities(id);
  const items = query.data ?? [];

  return (
    <GlassCard className="p-6">
      <p className="text-xs font-medium uppercase tracking-wider text-slate-400">Activity</p>

      {query.isLoading ? (
        <p className="mt-3 text-sm text-slate-500">Loading…</p>
      ) : items.length === 0 ? (
        <p className="mt-3 text-sm text-slate-500">No activity yet.</p>
      ) : (
        <ul className="mt-4 space-y-3">
          {items.map((a) => (
            <li key={a.id} className="flex items-start gap-3">
              <span className="mt-0.5 grid h-6 w-6 shrink-0 place-items-center rounded-full border border-white/10 text-accent">
                <ActivityIcon size={12} />
              </span>
              <div className="min-w-0">
                <p className="truncate text-sm text-slate-300">{describe(a)}</p>
                <p className="text-[11px] text-slate-500">{timeAgo(a.createdAt)}</p>
              </div>
            </li>
          ))}
        </ul>
      )}
    </GlassCard>
  );
}
