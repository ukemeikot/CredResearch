"use client";

import Link from "next/link";
import { useRouter, useSearchParams } from "next/navigation";
import { useEffect, useRef, useState } from "react";
import { motion } from "framer-motion";
import { CheckCircle2, Loader2, XCircle } from "lucide-react";
import { Logo } from "@/components/ui/logo";
import { Button } from "@/components/ui/button";
import { GlassCard } from "@/components/ui/glass-card";
import { ApiError } from "@/lib/api";
import { useAuth } from "@/lib/auth-store";
import { useAcceptInvite } from "../api/use-invitations";

type State = "loading" | "needs-auth" | "accepting" | "success" | "error";

export function AcceptInviteScreen() {
  const router = useRouter();
  const token = useSearchParams().get("token");
  const { hydrated, user } = useAuth();
  const accept = useAcceptInvite();
  const [state, setState] = useState<State>("loading");
  const [message, setMessage] = useState("");
  const ran = useRef(false);
  // Carry this exact accept URL through login/register so the user returns here and the
  // invitation is accepted automatically (rather than landing on the dashboard, token lost).
  const nextParam = encodeURIComponent(`/invite/accept?token=${token ?? ""}`);

  useEffect(() => {
    if (!hydrated || ran.current) return;
    if (!token) {
      setState("error");
      setMessage("This invitation link is missing its token.");
      return;
    }
    if (!user) {
      setState("needs-auth");
      return;
    }
    ran.current = true;
    setState("accepting");
    accept
      .mutateAsync(token)
      .then((res) => {
        setState("success");
        setTimeout(() => router.replace(`/projects/${res.projectId}`), 1200);
      })
      .catch((e) => {
        setState("error");
        setMessage(e instanceof ApiError ? e.message : "Could not accept this invitation.");
      });
  }, [hydrated, user, token, accept, router]);

  return (
    <main className="grid min-h-[100svh] place-items-center px-6 py-16">
      <motion.div
        initial={{ opacity: 0, y: 24 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.5, ease: "easeOut" }}
        className="w-full max-w-md"
      >
        <Link href="/" className="mb-8 flex justify-center">
          <Logo />
        </Link>
        <GlassCard className="flex flex-col items-center gap-5 p-8 text-center">
          {(state === "loading" || state === "accepting") && (
            <>
              <Loader2 className="animate-spin text-accent" size={40} />
              <p className="text-slate-600">
                {state === "accepting" ? "Joining the project…" : "Checking your invitation…"}
              </p>
            </>
          )}

          {state === "needs-auth" && (
            <>
              <h1 className="font-display text-2xl font-bold text-slate-900">You’re invited</h1>
              <p className="text-sm text-slate-500">
                Sign in (or create an account with the invited email) and you’ll be brought straight back here
                to join the project.
              </p>
              <div className="flex w-full gap-3">
                <Link href={`/login?next=${nextParam}`} className="flex-1">
                  <Button size="lg" className="w-full">Sign in</Button>
                </Link>
                <Link href={`/register?next=${nextParam}`} className="flex-1">
                  <Button variant="outline" size="lg" className="w-full">Register</Button>
                </Link>
              </div>
            </>
          )}

          {state === "success" && (
            <>
              <CheckCircle2 className="text-emerald-600" size={48} />
              <div>
                <h1 className="font-display text-2xl font-bold text-slate-900">You’re in</h1>
                <p className="mt-2 text-sm text-slate-500">Taking you to the project…</p>
              </div>
            </>
          )}

          {state === "error" && (
            <>
              <XCircle className="text-rose-600" size={48} />
              <div>
                <h1 className="font-display text-2xl font-bold text-slate-900">Invitation problem</h1>
                <p className="mt-2 text-sm text-slate-500">{message}</p>
              </div>
              <Link href="/dashboard" className="w-full">
                <Button variant="outline" size="lg" className="w-full">Go to dashboard</Button>
              </Link>
            </>
          )}
        </GlassCard>
      </motion.div>
    </main>
  );
}
