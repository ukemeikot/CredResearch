"use client";

import Link from "next/link";
import { useRouter, useSearchParams } from "next/navigation";
import { useEffect, useState } from "react";
import { motion } from "framer-motion";
import { ArrowRight, CheckCircle2 } from "lucide-react";
import { Logo } from "@/components/ui/logo";
import { Button } from "@/components/ui/button";
import { GlassCard } from "@/components/ui/glass-card";
import { Field } from "@/components/ui/field";
import { ApiError } from "@/lib/api";
import { useResetPassword } from "../api/use-password-reset";

export function ResetPasswordScreen() {
  const router = useRouter();
  const token = useSearchParams().get("token");
  const reset = useResetPassword();
  const [password, setPassword] = useState("");
  const [confirm, setConfirm] = useState("");
  const [mismatch, setMismatch] = useState(false);

  async function onSubmit(e: React.FormEvent) {
    e.preventDefault();
    if (password !== confirm) {
      setMismatch(true);
      return;
    }
    setMismatch(false);
    if (!token) return;
    try {
      await reset.mutateAsync({ token, password });
    } catch {
      /* surfaced via reset.error */
    }
  }

  // Redirect to sign-in shortly after a successful reset; cleared on unmount.
  useEffect(() => {
    if (!reset.isSuccess) return;
    const t = setTimeout(() => router.push("/login"), 1500);
    return () => clearTimeout(t);
  }, [reset.isSuccess, router]);

  const error = mismatch
    ? "Passwords don’t match."
    : reset.error instanceof ApiError
      ? reset.error.message
      : null;

  return (
    <main className="grid min-h-[100svh] place-items-center px-6 py-16">
      <motion.div
        initial={{ opacity: 0, y: 24 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.6, ease: "easeOut" }}
        className="w-full max-w-md"
      >
        <Link href="/" className="mb-8 flex justify-center">
          <Logo />
        </Link>
        <GlassCard className="p-8">
          {reset.isSuccess ? (
            <div className="flex flex-col items-center gap-4 text-center">
              <CheckCircle2 className="text-emerald-400" size={48} />
              <div>
                <h1 className="font-display text-2xl font-bold text-white">Password updated</h1>
                <p className="mt-2 text-sm text-slate-400">Redirecting you to sign in…</p>
              </div>
            </div>
          ) : !token ? (
            <div className="text-center">
              <h1 className="font-display text-2xl font-bold text-white">Invalid reset link</h1>
              <p className="mt-2 text-sm text-slate-400">This link is missing its token.</p>
              <Link href="/forgot-password" className="mt-4 inline-block text-sm text-accent hover:text-accent-soft">
                Request a new link
              </Link>
            </div>
          ) : (
            <>
              <p className="eyebrow">Password reset</p>
              <h1 className="mt-2 font-display text-2xl font-bold text-white">Choose a new password</h1>

              <form onSubmit={onSubmit} className="mt-7 space-y-4">
                <Field label="New password" type="password" value={password} onChange={setPassword} placeholder="At least 8 characters" />
                <Field label="Confirm password" type="password" value={confirm} onChange={setConfirm} placeholder="Re-enter password" />
                {error && <p className="text-sm text-rose-400">{error}</p>}
                <Button type="submit" size="lg" className="w-full" disabled={reset.isPending || password.length < 8}>
                  {reset.isPending ? "Updating…" : <>Update password <ArrowRight size={18} /></>}
                </Button>
              </form>
            </>
          )}
        </GlassCard>
      </motion.div>
    </main>
  );
}
