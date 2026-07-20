"use client";

import Link from "next/link";
import { useSearchParams } from "next/navigation";
import { useEffect, useRef, useState } from "react";
import { motion } from "framer-motion";
import { ArrowRight, CheckCircle2, Loader2, XCircle } from "lucide-react";
import { Logo } from "@/components/ui/logo";
import { Button } from "@/components/ui/button";
import { GlassCard } from "@/components/ui/glass-card";
import { api, ApiError } from "@/lib/api";

type State = "loading" | "success" | "error";

export function VerifyEmailScreen() {
  const token = useSearchParams().get("token");
  const [state, setState] = useState<State>("loading");
  const [message, setMessage] = useState("");
  const ran = useRef(false);

  useEffect(() => {
    if (ran.current) return;
    ran.current = true;
    if (!token) {
      setState("error");
      setMessage("This verification link is missing its token.");
      return;
    }
    api
      .verifyEmail(token)
      .then(() => setState("success"))
      .catch((e) => {
        setState("error");
        setMessage(e instanceof ApiError ? e.message : "Verification failed.");
      });
  }, [token]);

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
        <GlassCard className="flex flex-col items-center gap-5 p-8 text-center">
          {state === "loading" && (
            <>
              <Loader2 className="animate-spin text-accent" size={40} />
              <p className="text-slate-600">Verifying your email…</p>
            </>
          )}
          {state === "success" && (
            <>
              <CheckCircle2 className="text-emerald-600" size={48} />
              <div>
                <h1 className="font-display text-2xl font-bold text-slate-900">Email verified</h1>
                <p className="mt-2 text-sm text-slate-500">
                  Your email is confirmed — you now have full access.
                </p>
              </div>
              <Link href="/dashboard" className="w-full">
                <Button size="lg" className="w-full">
                  Go to dashboard <ArrowRight size={18} />
                </Button>
              </Link>
            </>
          )}
          {state === "error" && (
            <>
              <XCircle className="text-rose-600" size={48} />
              <div>
                <h1 className="font-display text-2xl font-bold text-slate-900">Verification failed</h1>
                <p className="mt-2 text-sm text-slate-500">{message}</p>
              </div>
              <Link href="/login" className="w-full">
                <Button variant="outline" size="lg" className="w-full">
                  Back to sign in
                </Button>
              </Link>
            </>
          )}
        </GlassCard>
      </motion.div>
    </main>
  );
}
