import { cn } from "@/lib/cn";

/**
 * Animated globe: a shaded indigo sphere with a static lat/long wireframe and a continents layer
 * that scrolls horizontally inside the sphere clip — reads as a rotating planet. Pure CSS/SVG,
 * GPU-friendly, respects reduced-motion (see globals.css).
 */
export function Globe({ className }: { className?: string }) {
  const meridians = [10, 22, 35, 48]; // half-widths for longitude ellipses
  const parallels = [
    { cy: 26, rx: 30, ry: 4 },
    { cy: 38, rx: 44, ry: 6 },
    { cy: 50, rx: 48, ry: 7 },
    { cy: 62, rx: 44, ry: 6 },
    { cy: 74, rx: 30, ry: 4 },
  ];

  return (
    <div className={cn("relative aspect-square", className)}>
      {/* Halo */}
      <div className="absolute inset-[-12%] rounded-full bg-[radial-gradient(circle_at_50%_44%,rgba(99,102,241,0.28),transparent_62%)] blur-2xl" />

      {/* Sphere */}
      <div className="absolute inset-0 overflow-hidden rounded-full bg-[radial-gradient(circle_at_62%_36%,#eef2ff,#c7d2fe_78%)] ring-1 ring-indigo-200 shadow-[inset_-26px_-20px_60px_rgba(67,56,202,0.30),inset_20px_16px_46px_rgba(255,255,255,0.70)]">
        {/* Rotating continents */}
        <div className="globe-surface absolute inset-y-0 left-0 opacity-80" aria-hidden />

        {/* Lat/long wireframe */}
        <svg viewBox="0 0 100 100" className="absolute inset-0 h-full w-full" preserveAspectRatio="none" aria-hidden>
          <g fill="none" stroke="#6366f1" strokeWidth="0.4" opacity="0.5">
            {parallels.map((p) => (
              <ellipse key={p.cy} cx="50" cy={p.cy} rx={p.rx} ry={p.ry} />
            ))}
            {meridians.map((rx) => (
              <ellipse key={rx} cx="50" cy="50" rx={rx} ry="48" />
            ))}
            <circle cx="50" cy="50" r="48" strokeWidth="0.6" />
          </g>
        </svg>

        {/* Spherical shading: highlight upper-right, shadow lower-left */}
        <div className="absolute inset-0 rounded-full bg-[radial-gradient(circle_at_72%_26%,rgba(255,255,255,0.65),transparent_42%),radial-gradient(circle_at_28%_82%,rgba(55,48,163,0.30),transparent_55%)]" />
      </div>

      {/* Specular highlight */}
      <div className="absolute right-[18%] top-[14%] h-6 w-6 rounded-full bg-white/85 blur-md sm:h-10 sm:w-10" />

      {/* Orbit ring + satellite */}
      <div className="animate-spin-slow absolute inset-[-8%] rounded-full">
        <span className="absolute left-1/2 top-0 h-2.5 w-2.5 -translate-x-1/2 rounded-full bg-accent shadow-glow" />
      </div>
      <div className="absolute inset-[-8%] rounded-full border border-dashed border-indigo-200" />
    </div>
  );
}
