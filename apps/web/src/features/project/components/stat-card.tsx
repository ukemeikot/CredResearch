import { GlassCard } from "@/components/ui/glass-card";
import { AnimatedCounter } from "@/components/ui/animated-counter";

export function StatCard({ label, value }: { label: string; value: number }) {
  return (
    <GlassCard className="p-6">
      <p className="text-xs font-medium uppercase tracking-wider text-slate-500">{label}</p>
      <p className="mt-2 font-display text-4xl font-bold text-slate-900">
        <AnimatedCounter value={value} />
      </p>
    </GlassCard>
  );
}
