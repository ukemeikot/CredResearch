"use client";

import { useEffect, useState } from "react";
import { GlassCard } from "@/components/ui/glass-card";
import { Button } from "@/components/ui/button";
import { Field } from "@/components/ui/field";
import { ApiError } from "@/lib/api";
import { useInstitution, useUpdateInstitution } from "../api/use-institution";

/** Institution details. Read for DEPARTMENT_ADMIN+; edit for INSTITUTION_ADMIN / PLATFORM_ADMIN. */
export function InstitutionSection({
  institutionId,
  canEdit,
}: {
  institutionId: string;
  canEdit: boolean;
}) {
  const institution = useInstitution(institutionId);
  const update = useUpdateInstitution(institutionId);
  const [name, setName] = useState("");
  const [country, setCountry] = useState("");
  const [type, setType] = useState("");

  // Seed the form once the institution loads (and when it changes server-side).
  useEffect(() => {
    if (institution.data) {
      setName(institution.data.name);
      setCountry(institution.data.country ?? "");
      setType(institution.data.type ?? "");
    }
  }, [institution.data]);

  async function submit(e: React.FormEvent) {
    e.preventDefault();
    try {
      await update.mutateAsync({ name, country: country || undefined, type: type || undefined });
    } catch {
      /* surfaced via update.error */
    }
  }

  const error = update.error instanceof ApiError ? update.error.message : null;

  return (
    <GlassCard className="p-7">
      <h2 className="font-display text-lg font-semibold text-white">Institution</h2>

      {institution.isLoading ? (
        <p className="mt-4 text-sm text-slate-500">Loading…</p>
      ) : institution.isError || !institution.data ? (
        <p className="mt-4 text-sm text-rose-300">Couldn’t load institution.</p>
      ) : institution.data.personalTenant ? (
        <p className="mt-2 text-sm text-slate-400">
          You’re in a personal workspace — join or get invited to an institution to collaborate.
        </p>
      ) : (
        <form onSubmit={submit} className="mt-5 space-y-4">
          <Field label="Name" type="text" value={name} onChange={setName} disabled={!canEdit} />
          <div className="grid gap-4 sm:grid-cols-2">
            <Field label="Country" type="text" value={country} onChange={setCountry} required={false} disabled={!canEdit} />
            <Field label="Type" type="text" value={type} onChange={setType} placeholder="university" required={false} disabled={!canEdit} />
          </div>
          {error && <p className="text-sm text-rose-400">{error}</p>}
          {canEdit && (
            <Button type="submit" size="md" disabled={update.isPending || !name}>
              {update.isPending ? "Saving…" : update.isSuccess ? "Saved" : "Save institution"}
            </Button>
          )}
        </form>
      )}
    </GlassCard>
  );
}
