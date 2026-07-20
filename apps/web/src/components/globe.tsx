import { cn } from "@/lib/cn";

/** A soft academic accent orb: indigo gradient sphere + rotating surface + halo + orbiting dot. */
export function Globe({ className }: { className?: string }) {
  return (
    <div className={cn("relative aspect-square", className)}>
      {/* Halo */}
      <div className="absolute inset-[-14%] rounded-full bg-[radial-gradient(circle_at_60%_35%,rgba(99,102,241,0.28),transparent_62%)] blur-2xl" />

      {/* Sphere */}
      <div className="absolute inset-0 overflow-hidden rounded-full ring-1 ring-indigo-200 shadow-[inset_-28px_-18px_60px_rgba(67,56,202,0.28),inset_18px_14px_46px_rgba(255,255,255,0.6)]">
        {/* Light indigo base */}
        <div className="absolute inset-0 bg-[radial-gradient(circle_at_62%_38%,#eef2ff,#c7d2fe_72%)]" />
        {/* Rotating soft bands */}
        <div className="globe-surface absolute inset-y-0 left-0 opacity-70" />
        {/* Gentle shading */}
        <div className="absolute inset-0 rounded-full bg-[radial-gradient(circle_at_74%_26%,rgba(255,255,255,0.7),transparent_40%),radial-gradient(circle_at_30%_80%,rgba(67,56,202,0.22),transparent_55%)]" />
      </div>

      {/* Specular highlight */}
      <div className="absolute right-[16%] top-[13%] h-8 w-8 rounded-full bg-white/80 blur-lg sm:h-12 sm:w-12" />

      {/* Orbiting accent dot */}
      <div className="animate-spin-slow absolute inset-[-7%] rounded-full">
        <span className="absolute left-1/2 top-0 h-2.5 w-2.5 -translate-x-1/2 rounded-full bg-accent shadow-glow" />
      </div>
      <div className="absolute inset-[-7%] rounded-full border border-dashed border-indigo-200" />
    </div>
  );
}
