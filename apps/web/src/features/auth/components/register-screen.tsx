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
import { useRegister } from "../api/use-register";

export function RegisterScreen() {
  const router = useRouter();
  const register = useRegister();
  const [fullName, setFullName] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");

  async function onSubmit(e: React.FormEvent) {
    e.preventDefault();
    try {
      await register.mutateAsync({ email, password, fullName });
      router.push("/dashboard");
    } catch {
      /* error surfaced via register.error */
    }
  }

  const error =
    register.error instanceof ApiError
      ? register.error.message
      : register.isError
        ? "Something went wrong"
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
          <p className="eyebrow">Join the revolution</p>
          <h1 className="mt-2 font-display text-2xl font-bold text-slate-900">Create your account</h1>

          <form onSubmit={onSubmit} className="mt-7 space-y-4">
            <Field label="Full name" type="text" value={fullName} onChange={setFullName} placeholder="Ada Lovelace" />
            <Field label="Email" type="email" value={email} onChange={setEmail} placeholder="ada@example.com" />
            <Field label="Password" type="password" value={password} onChange={setPassword} placeholder="At least 8 characters" />

            {error && (
              <motion.p initial={{ opacity: 0 }} animate={{ opacity: 1 }} className="text-sm text-rose-600">
                {error}
              </motion.p>
            )}

            <Button type="submit" size="lg" className="w-full" disabled={register.isPending}>
              {register.isPending ? "Creating…" : <>Create account <ArrowRight size={18} /></>}
            </Button>
          </form>

          <p className="mt-6 text-center text-sm text-slate-500">
            Already have an account?{" "}
            <Link href="/login" className="text-accent hover:text-accent-soft">
              Sign in
            </Link>
          </p>
        </GlassCard>
      </motion.div>
    </main>
  );
}
