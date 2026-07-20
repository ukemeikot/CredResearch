"use client";

import { useState } from "react";
import { Plus } from "lucide-react";
import { GlassCard } from "@/components/ui/glass-card";
import { Button } from "@/components/ui/button";
import { Field } from "@/components/ui/field";
import { ApiError } from "@/lib/api";
import { useCreateDepartment, useDepartments } from "../api/use-departments";

/** Departments management. Rendered only for DEPARTMENT_ADMIN+; creation needs INSTITUTION_ADMIN. */
export function DepartmentsSection({ canCreate }: { canCreate: boolean }) {
  const departments = useDepartments();
  const create = useCreateDepartment();
  const [name, setName] = useState("");
  const [code, setCode] = useState("");

  async function submit(e: React.FormEvent) {
    e.preventDefault();
    try {
      await create.mutateAsync({ name, code: code || undefined });
      setName("");
      setCode("");
    } catch {
      /* surfaced via create.error */
    }
  }

  const list = departments.data ?? [];
  const error = create.error instanceof ApiError ? create.error.message : null;

  return (
    <GlassCard className="p-7">
      <h2 className="font-display text-lg font-semibold text-slate-900">Departments</h2>
      <p className="mt-1 text-sm text-slate-500">Departments within your institution.</p>

      {departments.isLoading ? (
        <p className="mt-4 text-sm text-slate-500">Loading…</p>
      ) : list.length === 0 ? (
        <p className="mt-4 text-sm text-slate-500">No departments yet.</p>
      ) : (
        <ul className="mt-4 divide-y divide-slate-100">
          {list.map((d) => (
            <li key={d.id} className="flex items-center justify-between py-2.5">
              <span className="text-sm text-slate-900">{d.name}</span>
              {d.code && (
                <span className="rounded-full border border-slate-200 px-2 py-0.5 text-[11px] uppercase tracking-wider text-slate-500">
                  {d.code}
                </span>
              )}
            </li>
          ))}
        </ul>
      )}

      {canCreate && (
        <form onSubmit={submit} className="mt-5 flex flex-wrap items-end gap-3 border-t border-slate-200 pt-5">
          <div className="min-w-[180px] flex-1">
            <Field label="New department" type="text" value={name} onChange={setName} placeholder="e.g. Computer Science" />
          </div>
          <div className="w-28">
            <Field label="Code" type="text" value={code} onChange={setCode} placeholder="CSC" required={false} />
          </div>
          <Button type="submit" size="md" disabled={create.isPending || !name}>
            <Plus size={16} /> {create.isPending ? "Adding…" : "Add"}
          </Button>
          {error && <p className="w-full text-sm text-rose-600">{error}</p>}
        </form>
      )}
    </GlassCard>
  );
}
