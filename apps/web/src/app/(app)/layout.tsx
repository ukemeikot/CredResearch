"use client";

import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { useEffect, useState } from "react";
import { AnimatePresence, motion } from "framer-motion";
import { LogOut, Menu, X } from "lucide-react";
import { Logo } from "@/components/ui/logo";
import { useLogout } from "@/features/auth/api/use-logout";
import { useAuth } from "@/lib/auth-store";

// Roles arrive as an unordered set; show the highest-privilege one deterministically.
const ROLE_PRIORITY = ["PLATFORM_ADMIN", "INSTITUTION_ADMIN", "DEPARTMENT_ADMIN", "SUPERVISOR", "STUDENT"];
function primaryRole(roles?: string[]): string {
  if (!roles?.length) return "STUDENT";
  return ROLE_PRIORITY.find((r) => roles.includes(r)) ?? roles[0];
}

const NAV = [
  { href: "/dashboard", label: "Dashboard" },
  { href: "/reviews", label: "Reviews" },
  { href: "/pricing", label: "Pricing" },
  { href: "/settings", label: "Settings" },
];

export default function AppLayout({ children }: { children: React.ReactNode }) {
  const router = useRouter();
  const pathname = usePathname();
  const logout = useLogout();
  const { hydrated, user } = useAuth();
  const [menuOpen, setMenuOpen] = useState(false);

  useEffect(() => {
    if (hydrated && !user) router.replace("/login");
  }, [hydrated, user, router]);

  // Close the mobile menu on navigation.
  useEffect(() => {
    setMenuOpen(false);
  }, [pathname]);

  if (!hydrated || !user) {
    return (
      <div className="grid min-h-[100svh] place-items-center">
        <div className="h-10 w-10 animate-spin-slow rounded-full border-2 border-slate-200 border-t-accent" />
      </div>
    );
  }

  const links = user.roles?.includes("PLATFORM_ADMIN")
    ? [...NAV, { href: "/admin", label: "Admin" }]
    : NAV;
  const linkClass = (href: string) =>
    pathname === href ? "text-slate-900" : "hover:text-slate-900";

  return (
    <div className="min-h-[100svh]">
      <header className="sticky top-0 z-40 border-b border-slate-200 bg-white/80 backdrop-blur-xl">
        <div className="mx-auto flex h-16 max-w-6xl items-center justify-between px-4 sm:px-6">
          <Link href="/dashboard">
            <Logo />
          </Link>
          <nav className="hidden gap-7 text-sm font-medium uppercase tracking-wider text-slate-600 md:flex">
            {links.map((l) => (
              <Link key={l.href} href={l.href} className={linkClass(l.href)}>
                {l.label}
              </Link>
            ))}
            <span className="cursor-not-allowed text-slate-600">Library</span>
          </nav>
          <div className="flex items-center gap-3 sm:gap-4">
            <span className="hidden text-sm text-slate-500 sm:block">{primaryRole(user.roles)}</span>
            <button
              onClick={async () => {
                await logout();
                router.replace("/login");
              }}
              className="grid h-9 w-9 place-items-center rounded-full border border-slate-200 text-slate-600 transition-colors hover:border-rose-400/50 hover:text-rose-600"
              aria-label="Log out"
            >
              <LogOut size={16} />
            </button>
            <button
              onClick={() => setMenuOpen((o) => !o)}
              className="grid h-9 w-9 place-items-center rounded-full border border-slate-200 text-slate-700 md:hidden"
              aria-label="Toggle menu"
              aria-expanded={menuOpen}
            >
              {menuOpen ? <X size={16} /> : <Menu size={16} />}
            </button>
          </div>
        </div>

        {/* Mobile menu */}
        <AnimatePresence>
          {menuOpen && (
            <motion.div
              initial={{ height: 0, opacity: 0 }}
              animate={{ height: "auto", opacity: 1 }}
              exit={{ height: 0, opacity: 0 }}
              transition={{ duration: 0.25 }}
              className="overflow-hidden border-t border-slate-200 bg-white/95 backdrop-blur-md md:hidden"
            >
              <ul className="flex flex-col gap-1 px-4 py-4">
                <li className="px-3 pb-1 text-xs uppercase tracking-wider text-slate-400">
                  {primaryRole(user.roles)}
                </li>
                {links.map((l) => (
                  <li key={l.href}>
                    <Link
                      href={l.href}
                      className={`block rounded-lg px-3 py-3 text-sm font-medium uppercase tracking-wider ${
                        pathname === l.href
                          ? "bg-slate-100 text-slate-900"
                          : "text-slate-600 hover:bg-slate-100 hover:text-slate-900"
                      }`}
                    >
                      {l.label}
                    </Link>
                  </li>
                ))}
              </ul>
            </motion.div>
          )}
        </AnimatePresence>
      </header>
      <main className="mx-auto max-w-6xl px-4 py-8 sm:px-6 sm:py-10">{children}</main>
    </div>
  );
}
