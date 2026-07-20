"use client";

import { useQuery } from "@tanstack/react-query";
import { api } from "@/lib/api";
import { useMe } from "@/features/user/api/use-me";
import { GlassCard } from "@/components/ui/glass-card";

export default function AdminPage() {
  const me = useMe();
  const isAdmin = me.data?.roles?.includes("PLATFORM_ADMIN");
  const stats = useQuery({ queryKey: ["admin-stats"], queryFn: api.adminStats, enabled: !!isAdmin });

  if (me.isLoading) return <div className="py-20 text-center text-slate-500">Loading…</div>;
  if (!isAdmin) {
    return (
      <div className="mx-auto max-w-2xl py-20 text-center">
        <h1 className="font-display text-2xl font-bold text-slate-900">Admin</h1>
        <p className="mt-2 text-sm text-slate-500">This area is for platform administrators only.</p>
      </div>
    );
  }

  const s = stats.data;
  const cards = [
    { label: "Users", value: s?.users },
    { label: "Institutions", value: s?.institutions },
    { label: "Projects", value: s?.projects },
    { label: "Documents", value: s?.documents },
    { label: "Papers", value: s?.papers },
    { label: "Questionnaires", value: s?.questionnaires },
    { label: "AI requests (month)", value: s?.aiRequestsThisMonth },
  ];

  return (
    <div className="mx-auto max-w-5xl">
      <h1 className="font-display text-2xl font-bold text-slate-900">Platform admin</h1>
      <p className="mt-1 text-sm text-slate-500">Headline statistics across the platform.</p>
      <div className="mt-6 grid grid-cols-2 gap-4 sm:grid-cols-3 lg:grid-cols-4">
        {cards.map((c) => (
          <GlassCard key={c.label} className="p-5">
            <p className="text-3xl font-bold text-slate-900">{stats.isLoading ? "…" : (c.value ?? 0)}</p>
            <p className="mt-1 text-xs uppercase tracking-wider text-slate-500">{c.label}</p>
          </GlassCard>
        ))}
      </div>
    </div>
  );
}
