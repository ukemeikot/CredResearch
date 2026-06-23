import { cn } from "@/lib/cn";

/** A CSS planet: shaded sphere + rotating surface texture + atmosphere + sun glint. */
export function Globe({ className }: { className?: string }) {
  return (
    <div className={cn("relative aspect-square", className)}>
      {/* Atmosphere glow */}
      <div className="absolute inset-[-14%] rounded-full bg-[radial-gradient(circle_at_70%_28%,rgba(45,226,255,0.32),transparent_62%)] blur-2xl" />

      {/* Sphere (clips the rotating surface) */}
      <div className="absolute inset-0 overflow-hidden rounded-full ring-1 ring-white/10 shadow-[inset_-34px_-22px_70px_rgba(0,0,0,0.75),inset_22px_16px_55px_rgba(90,130,255,0.22)]">
        {/* Static deep-ocean base */}
        <div className="absolute inset-0 bg-[radial-gradient(circle_at_60%_40%,#22356f,#0c1430_72%)]" />
        {/* Rotating landmass/cloud bands */}
        <div className="globe-surface absolute inset-y-0 left-0" />
        {/* Spherical shading: light from upper-right, dark terminator lower-left */}
        <div className="absolute inset-0 rounded-full bg-[radial-gradient(circle_at_72%_26%,rgba(190,225,255,0.5),transparent_38%),radial-gradient(circle_at_30%_78%,rgba(4,7,18,0.9),transparent_55%)]" />
        {/* Subtle latitude lines */}
        <div className="absolute inset-0 opacity-[0.12] [background-image:repeating-linear-gradient(0deg,rgba(255,255,255,0.6)_0_1px,transparent_1px_16px)]" />
      </div>

      {/* Specular sun glint */}
      <div className="absolute right-[15%] top-[12%] h-8 w-8 rounded-full bg-white/80 blur-lg sm:h-12 sm:w-12" />

      {/* Orbiting moon */}
      <div className="animate-spin-slow absolute inset-[-7%] rounded-full">
        <span className="absolute left-1/2 top-0 h-2.5 w-2.5 -translate-x-1/2 rounded-full bg-accent shadow-glow" />
      </div>
      <div className="absolute inset-[-7%] rounded-full border border-dashed border-white/10" />
    </div>
  );
}
