import { cn } from "@/lib/cn";

/** CredResearch mark: an indigo tile with a graduation-cap glyph + wordmark. */
export function Logo({ className }: { className?: string }) {
  return (
    <span className={cn("flex items-center gap-2.5 font-display", className)}>
      <span className="grid h-9 w-9 place-items-center rounded-xl bg-gradient-to-br from-accent to-accent-deep shadow-glow">
        <svg viewBox="0 0 24 24" className="h-5 w-5" fill="none" aria-hidden>
          {/* Mortarboard */}
          <path d="M12 4 L22 9 L12 14 L2 9 Z" fill="#ffffff" />
          <path d="M6 11 V15 C6 16.6 8.7 18 12 18 C15.3 18 18 16.6 18 15 V11" stroke="#ffffff" strokeWidth="1.6" strokeLinecap="round" />
          {/* Tassel */}
          <path d="M22 9 V13.5" stroke="#ffffff" strokeWidth="1.4" strokeLinecap="round" />
          <circle cx="22" cy="14.4" r="1.1" fill="#ffffff" />
        </svg>
      </span>
      <span className="text-lg font-bold tracking-tight text-slate-900">
        Cred<span className="text-accent">Research</span>
      </span>
    </span>
  );
}
