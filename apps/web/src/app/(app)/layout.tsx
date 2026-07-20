"use client";

import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { useEffect } from "react";
import { LogOut } from "lucide-react";
import { Logo } from "@/components/ui/logo";
import { useLogout } from "@/features/auth/api/use-logout";
import { useAuth } from "@/lib/auth-store";

// Roles arrive as an unordered set; show the highest-privilege one deterministically.
const ROLE_PRIORITY = ["PLATFORM_ADMIN", "INSTITUTION_ADMIN", "DEPARTMENT_ADMIN", "SUPERVISOR", "STUDENT"];
function primaryRole(roles?: string[]): string {
  if (!roles?.length) return "STUDENT";
  return ROLE_PRIORITY.find((r) => roles.includes(r)) ?? roles[0];
}

export default function AppLayout({ children }: { children: React.ReactNode }) {
  const router = useRouter();
  const pathname = usePathname();
  const logout = useLogout();
  const { accessToken, hydrated, user } = useAuth();

  useEffect(() => {
    if (hydrated && !accessToken) router.replace("/login");
  }, [hydrated, accessToken, router]);

  if (!hydrated || !accessToken) {
    return (
      <div className="grid min-h-[100svh] place-items-center">
        <div className="h-10 w-10 animate-spin-slow rounded-full border-2 border-white/10 border-t-accent" />
      </div>
    );
  }

  return (
    <div className="min-h-[100svh]">
      <header className="sticky top-0 z-40 border-b border-white/10 bg-cosmos-950/70 backdrop-blur-xl">
        <div className="mx-auto flex h-16 max-w-6xl items-center justify-between px-6">
          <Link href="/dashboard">
            <Logo />
          </Link>
          <nav className="hidden gap-7 text-sm font-medium uppercase tracking-wider text-slate-300 md:flex">
            <Link
              href="/dashboard"
              className={pathname === "/dashboard" ? "text-white" : "hover:text-white"}
            >
              Dashboard
            </Link>
            <Link
              href="/reviews"
              className={pathname === "/reviews" ? "text-white" : "hover:text-white"}
            >
              Reviews
            </Link>
            <Link
              href="/settings"
              className={pathname === "/settings" ? "text-white" : "hover:text-white"}
            >
              Settings
            </Link>
            {user?.roles?.includes("PLATFORM_ADMIN") && (
              <Link href="/admin" className={pathname === "/admin" ? "text-white" : "hover:text-white"}>
                Admin
              </Link>
            )}
            <span className="cursor-not-allowed text-slate-600">Library</span>
          </nav>
          <div className="flex items-center gap-4">
            <span className="hidden text-sm text-slate-400 sm:block">
              {primaryRole(user?.roles)}
            </span>
            <button
              onClick={async () => {
                await logout();
                router.replace("/login");
              }}
              className="grid h-9 w-9 place-items-center rounded-full border border-white/10 text-slate-300 transition-colors hover:border-rose-400/50 hover:text-rose-400"
              aria-label="Log out"
            >
              <LogOut size={16} />
            </button>
          </div>
        </div>
      </header>
      <main className="mx-auto max-w-6xl px-6 py-10">{children}</main>
    </div>
  );
}
