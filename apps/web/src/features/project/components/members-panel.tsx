"use client";

import { useState } from "react";
import { Plus, Trash2, UserRound } from "lucide-react";
import { GlassCard } from "@/components/ui/glass-card";
import { Button } from "@/components/ui/button";
import { Field } from "@/components/ui/field";
import { useMe } from "@/features/user/api/use-me";
import { ApiError, type ProjectMember, type ProjectMemberRole } from "@/lib/api";
import { useAddMember, useRemoveMember } from "../api/use-projects";

const ADDABLE_ROLES: ProjectMemberRole[] = ["SUPERVISOR", "CONSULTANT", "VIEWER"];

export function MembersPanel({
  id,
  members,
  ownerUserId,
  isOwner,
}: {
  id: string;
  members: ProjectMember[];
  ownerUserId: string;
  isOwner: boolean;
}) {
  const me = useMe();
  const add = useAddMember(id);
  const remove = useRemoveMember(id);
  const [open, setOpen] = useState(false);
  const [userId, setUserId] = useState("");
  const [role, setRole] = useState<ProjectMemberRole>("SUPERVISOR");

  async function submit(e: React.FormEvent) {
    e.preventDefault();
    try {
      await add.mutateAsync({ userId: userId.trim(), role });
      setUserId("");
      setOpen(false);
    } catch {
      /* surfaced via add.error */
    }
  }

  const error =
    add.error instanceof ApiError
      ? add.error.message
      : remove.error instanceof ApiError
        ? remove.error.message
        : null;

  return (
    <GlassCard className="p-6">
      <div className="flex items-center justify-between">
        <p className="text-xs font-medium uppercase tracking-wider text-slate-400">Team</p>
        {isOwner && !open && (
          <button
            onClick={() => setOpen(true)}
            className="inline-flex items-center gap-1 text-xs text-accent hover:text-accent-soft"
          >
            <Plus size={14} /> Add member
          </button>
        )}
      </div>

      <ul className="mt-4 space-y-2">
        {members.map((m) => {
          const isMe = m.userId === me.data?.id;
          const removable = isOwner && m.userId !== ownerUserId;
          return (
            <li
              key={m.id}
              className="flex items-center justify-between gap-3 rounded-xl border border-white/5 bg-white/[0.02] px-3 py-2"
            >
              <div className="flex min-w-0 items-center gap-2.5">
                <span className="grid h-8 w-8 shrink-0 place-items-center rounded-full border border-white/10 text-slate-400">
                  <UserRound size={15} />
                </span>
                <div className="min-w-0">
                  <p className="truncate font-mono text-xs text-slate-300">
                    {m.userId.slice(0, 8)}…{isMe && <span className="ml-1 text-accent">(you)</span>}
                  </p>
                  <p className="text-[11px] uppercase tracking-wider text-slate-500">{m.role}</p>
                </div>
              </div>
              {removable && (
                <button
                  onClick={() => remove.mutate(m.userId)}
                  disabled={remove.isPending}
                  className="text-slate-500 transition-colors hover:text-rose-400 disabled:opacity-40"
                  aria-label="Remove member"
                >
                  <Trash2 size={15} />
                </button>
              )}
            </li>
          );
        })}
      </ul>

      {open && (
        <form onSubmit={submit} className="mt-4 space-y-3 border-t border-white/10 pt-4">
          <Field
            label="User ID"
            type="text"
            value={userId}
            onChange={setUserId}
            placeholder="UUID of the user to add"
          />
          <label className="block">
            <span className="mb-1.5 block text-xs font-medium uppercase tracking-wider text-slate-400">
              Role
            </span>
            <div className="flex gap-2">
              {ADDABLE_ROLES.map((r) => (
                <button
                  type="button"
                  key={r}
                  onClick={() => setRole(r)}
                  className={`flex-1 rounded-xl border px-2 py-2 text-[11px] uppercase tracking-wider transition-all ${
                    role === r
                      ? "border-accent/60 bg-accent/10 text-white"
                      : "border-white/10 text-slate-400 hover:border-white/30"
                  }`}
                >
                  {r}
                </button>
              ))}
            </div>
          </label>
          {error && <p className="text-sm text-rose-400">{error}</p>}
          <div className="flex gap-2">
            <Button type="submit" size="sm" disabled={add.isPending || !userId.trim()}>
              {add.isPending ? "Adding…" : "Add"}
            </Button>
            <Button type="button" size="sm" variant="ghost" onClick={() => setOpen(false)}>
              Cancel
            </Button>
          </div>
        </form>
      )}

      {!open && error && <p className="mt-3 text-sm text-rose-400">{error}</p>}
    </GlassCard>
  );
}
