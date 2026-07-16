"use client";

import { useState } from "react";
import { useQueryClient } from "@tanstack/react-query";
import { Building2 } from "lucide-react";
import { GlassCard } from "@/components/ui/glass-card";
import { Button } from "@/components/ui/button";
import { Field } from "@/components/ui/field";
import { ApiError, refreshSession } from "@/lib/api";
import { useOnboardInstitution } from "../api/use-onboarding";

/**
 * Shown to users who aren't part of an institution yet. Creating one makes the caller an
 * INSTITUTION_ADMIN; we then refresh the session so the new tenant + role take effect immediately.
 */
export function OnboardInstitutionCard() {
  const qc = useQueryClient();
  const onboard = useOnboardInstitution();
  const [name, setName] = useState("");
  const [country, setCountry] = useState("");
  const [type, setType] = useState("university");
  const [done, setDone] = useState(false);

  const [refreshFailed, setRefreshFailed] = useState(false);

  async function submit(e: React.FormEvent) {
    e.preventDefault();
    setRefreshFailed(false);
    try {
      await onboard.mutateAsync({ name, country: country || undefined, type });
      // New access token must carry the new institution + INSTITUTION_ADMIN role. If the refresh
      // fails, the old session is cleared — send them to sign in again rather than claim success.
      const refreshed = await refreshSession();
      if (!refreshed) {
        setRefreshFailed(true);
        return;
      }
      await qc.invalidateQueries(); // refetch profile + tenant-scoped data under the new token
      setDone(true);
    } catch {
      /* surfaced via onboard.error */
    }
  }

  const error = onboard.error instanceof ApiError ? onboard.error.message : null;

  return (
    <GlassCard className="p-7">
      <div className="flex items-center gap-3">
        <span className="grid h-10 w-10 place-items-center rounded-xl border border-white/10 text-accent">
          <Building2 size={18} />
        </span>
        <div>
          <h2 className="font-display text-lg font-semibold text-white">Set up your institution</h2>
          <p className="text-sm text-slate-400">
            Create an institution to manage departments and invite colleagues. You’ll become its admin.
          </p>
        </div>
      </div>

      {done ? (
        <p className="mt-5 rounded-xl border border-emerald-400/30 bg-emerald-400/10 px-4 py-3 text-sm text-emerald-200">
          Institution created — you’re now an institution admin. Manage it below.
        </p>
      ) : (
        <form onSubmit={submit} className="mt-5 space-y-4">
          <Field label="Institution name" type="text" value={name} onChange={setName} placeholder="University of Lagos" />
          <div className="grid gap-4 sm:grid-cols-2">
            <Field label="Country" type="text" value={country} onChange={setCountry} placeholder="Nigeria" required={false} />
            <Field label="Type" type="text" value={type} onChange={setType} placeholder="university" required={false} />
          </div>
          {error && <p className="text-sm text-rose-400">{error}</p>}
          {refreshFailed && (
            <p className="text-sm text-amber-300">
              Institution created, but your session needs a refresh —{" "}
              <a href="/login" className="underline hover:text-amber-200">sign in again</a> to continue.
            </p>
          )}
          <Button type="submit" size="md" disabled={onboard.isPending || !name}>
            {onboard.isPending ? "Creating…" : "Create institution"}
          </Button>
        </form>
      )}
    </GlassCard>
  );
}
