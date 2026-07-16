"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useState } from "react";
import { motion } from "framer-motion";
import { ArrowRight } from "lucide-react";
import { Logo } from "@/components/ui/logo";
import { Button } from "@/components/ui/button";
import { GlassCard } from "@/components/ui/glass-card";
import { Field } from "@/components/ui/field";
import { ApiError } from "@/lib/api";
import { useLogin } from "../api/use-login";

export function LoginScreen() {
  const router = useRouter();
  const login = useLogin();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");

  async function onSubmit(e: React.FormEvent) {
    e.preventDefault();
    try {
      await login.mutateAsync({ email, password });
      router.push("/dashboard");
    } catch {
      /* error surfaced via login.error */
    }
  }

  const error =
    login.error instanceof ApiError ? login.error.message : login.isError ? "Something went wrong" : null;

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
          <p className="eyebrow">Welcome back</p>
          <h1 className="mt-2 font-display text-2xl font-bold text-white">Sign in</h1>

          <form onSubmit={onSubmit} className="mt-7 space-y-4">
            <Field label="Email" type="email" value={email} onChange={setEmail} placeholder="ada@example.com" />
            <Field label="Password" type="password" value={password} onChange={setPassword} placeholder="••••••••" />

            <div className="flex justify-end">
              <Link href="/forgot-password" className="text-xs text-slate-400 hover:text-accent">
                Forgot password?
              </Link>
            </div>

            {error && (
              <motion.p initial={{ opacity: 0 }} animate={{ opacity: 1 }} className="text-sm text-rose-400">
                {error}
              </motion.p>
            )}

            <Button type="submit" size="lg" className="w-full" disabled={login.isPending}>
              {login.isPending ? "Signing in…" : <>Sign in <ArrowRight size={18} /></>}
            </Button>
          </form>

          <p className="mt-6 text-center text-sm text-slate-400">
            New here?{" "}
            <Link href="/register" className="text-accent hover:text-accent-soft">
              Create an account
            </Link>
          </p>
        </GlassCard>
      </motion.div>
    </main>
  );
}
