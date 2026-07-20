import { cn } from "@/lib/cn";

export function Logo({ className }: { className?: string }) {
  return (
    <span className={cn("flex items-center gap-2 font-display", className)}>
      <span className="relative grid h-9 w-9 place-items-center">
        <svg viewBox="0 0 40 40" className="h-9 w-9">
          <path
            d="M20 3 L36 33 L4 33 Z"
            fill="none"
            stroke="#4f46e5"
            strokeWidth="2"
            strokeLinejoin="round"
          />
          <path d="M20 12 L28 28 L12 28 Z" fill="rgba(79,70,229,0.15)" />
        </svg>
        <span className="absolute text-sm font-bold text-accent">C</span>
      </span>
      <span className="text-lg font-bold tracking-wide text-slate-900">
        CRED<span className="font-light text-accent">RESEARCH</span>
      </span>
    </span>
  );
}
