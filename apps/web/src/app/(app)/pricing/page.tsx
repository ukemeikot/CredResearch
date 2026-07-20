"use client";

import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { Check, Loader2 } from "lucide-react";
import { api, ApiError, type Plan } from "@/lib/api";
import { GlassCard } from "@/components/ui/glass-card";
import { Button } from "@/components/ui/button";

function features(metadata?: string | null): string[] {
  if (!metadata) return [];
  try {
    return (JSON.parse(metadata).features as string[]) ?? [];
  } catch {
    return [];
  }
}

function price(p: Plan): string {
  if (p.price_minor === 0) return "Free";
  const major = p.price_minor / 100;
  const sym = p.currency === "NGN" ? "₦" : p.currency === "USD" ? "$" : p.currency + " ";
  return `${sym}${major.toLocaleString()}/mo`;
}

export default function PricingPage() {
  const plans = useQuery({ queryKey: ["billing-plans"], queryFn: api.billingPlans });
  const sub = useQuery({ queryKey: ["billing-subscription"], queryFn: api.billingSubscription });
  const [busy, setBusy] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>(null);

  async function upgrade(code: string) {
    setBusy(code);
    setNotice(null);
    try {
      const r = await api.billingCheckout(code);
      if (r.authorizationUrl) window.location.href = r.authorizationUrl;
      else setNotice(r.message ?? "Checkout is not available yet.");
    } catch (e) {
      setNotice(e instanceof ApiError ? e.message : "Could not start checkout.");
    } finally {
      setBusy(null);
    }
  }

  const current = sub.data?.plan;

  return (
    <div className="mx-auto max-w-5xl">
      <h1 className="font-display text-2xl font-bold text-white">Plans &amp; pricing</h1>
      <p className="mt-1 text-sm text-slate-400">
        Choose the plan that fits your research. {sub.data && <>You’re on <span className="text-accent">{current}</span>.</>}
      </p>
      {notice && <p className="mt-3 rounded-lg border border-white/10 bg-white/[0.03] px-4 py-2 text-sm text-slate-300">{notice}</p>}

      <div className="mt-6 grid gap-5 md:grid-cols-3">
        {plans.isLoading ? (
          <p className="text-sm text-slate-500">Loading…</p>
        ) : (
          (plans.data ?? []).map((p) => {
            const isCurrent = p.code === current;
            return (
              <GlassCard key={p.code} className={`flex flex-col p-6 ${isCurrent ? "border-accent/50" : ""}`}>
                <p className="text-sm uppercase tracking-wider text-slate-400">{p.name}</p>
                <p className="mt-2 font-display text-3xl font-bold text-white">{price(p)}</p>
                <p className="mt-1 text-xs text-slate-500">{p.ai_monthly_credits} AI credits / month</p>
                <ul className="mt-4 flex-1 space-y-2">
                  {features(p.metadata).map((f) => (
                    <li key={f} className="flex items-start gap-2 text-sm text-slate-300">
                      <Check size={15} className="mt-0.5 shrink-0 text-accent" /> {f}
                    </li>
                  ))}
                </ul>
                <div className="mt-5">
                  {isCurrent ? (
                    <Button variant="outline" className="w-full" disabled>Current plan</Button>
                  ) : p.price_minor === 0 ? (
                    <Button variant="outline" className="w-full" disabled>Free tier</Button>
                  ) : (
                    <Button className="w-full" onClick={() => upgrade(p.code)} disabled={busy === p.code}>
                      {busy === p.code ? <Loader2 size={15} className="animate-spin" /> : `Upgrade to ${p.name}`}
                    </Button>
                  )}
                </div>
              </GlassCard>
            );
          })
        )}
      </div>
    </div>
  );
}
