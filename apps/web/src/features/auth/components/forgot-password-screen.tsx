"use client";

import Link from "next/link";
import { useState } from "react";
import { motion } from "framer-motion";
import { ArrowRight, MailCheck } from "lucide-react";
import { Logo } from "@/components/ui/logo";
import { Button } from "@/components/ui/button";
import { GlassCard } from "@/components/ui/glass-card";
import { Field } from "@/components/ui/field";
import { useForgotPassword } from "../api/use-password-reset";

export function ForgotPasswordScreen() {
  const forgot = useForgotPassword();
  const [email, setEmail] = useState("");

  async function onSubmit(e: React.FormEvent) {
    e.preventDefault();
    try {
      await forgot.mutateAsync(email);
    } catch {
      /* the endpoint never reveals existence; treat as success below */
    }
  }

  // The API intentionally always succeeds; show the same confirmation either way.
  const sent = forgot.isSuccess || forgot.isError;

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
          {sent ? (
            <div className="flex flex-col items-center gap-4 text-center">
              <MailCheck className="text-accent" size={44} />
              <div>
                <h1 className="font-display text-2xl font-bold text-white">Check your inbox</h1>
                <p className="mt-2 text-sm text-slate-400">
                  If an account exists for <span className="text-slate-200">{email}</span>, a reset link is on its way.
                </p>
              </div>
              <Link href="/login" className="text-sm text-accent hover:text-accent-soft">
                Back to sign in
              </Link>
            </div>
          ) : (
            <>
              <p className="eyebrow">Password reset</p>
              <h1 className="mt-2 font-display text-2xl font-bold text-white">Forgot password</h1>
              <p className="mt-2 text-sm text-slate-400">
                Enter your email and we’ll send you a link to reset it.
              </p>

              <form onSubmit={onSubmit} className="mt-7 space-y-4">
                <Field label="Email" type="email" value={email} onChange={setEmail} placeholder="ada@example.com" />
                <Button type="submit" size="lg" className="w-full" disabled={forgot.isPending}>
                  {forgot.isPending ? "Sending…" : <>Send reset link <ArrowRight size={18} /></>}
                </Button>
              </form>

              <p className="mt-6 text-center text-sm text-slate-400">
                Remembered it?{" "}
                <Link href="/login" className="text-accent hover:text-accent-soft">
                  Sign in
                </Link>
              </p>
            </>
          )}
        </GlassCard>
      </motion.div>
    </main>
  );
}
