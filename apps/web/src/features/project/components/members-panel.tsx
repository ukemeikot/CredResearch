"use client";

import { useState } from "react";
import { MailPlus, Trash2, UserRound, X } from "lucide-react";
import { GlassCard } from "@/components/ui/glass-card";
import { Button } from "@/components/ui/button";
import { Field } from "@/components/ui/field";
import { useMe } from "@/features/user/api/use-me";
import { ApiError, type ProjectMember, type ProjectMemberRole } from "@/lib/api";
import { useRemoveMember } from "../api/use-projects";
import { useInvitations, useInvite, useRevokeInvite } from "../api/use-invitations";

const INVITE_ROLES: ProjectMemberRole[] = ["SUPERVISOR", "CONSULTANT", "VIEWER"];

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
  const remove = useRemoveMember(id);
  const invite = useInvite(id);
  const revoke = useRevokeInvite(id);
  const invitations = useInvitations(id, isOwner);
  const [open, setOpen] = useState(false);
  const [email, setEmail] = useState("");
  const [role, setRole] = useState<ProjectMemberRole>("SUPERVISOR");

  async function submit(e: React.FormEvent) {
    e.preventDefault();
    try {
      await invite.mutateAsync({ email: email.trim(), role });
      setEmail("");
      setOpen(false);
    } catch {
      /* surfaced via invite.error */
    }
  }

  const pending = invitations.data ?? [];
  const error =
    invite.error instanceof ApiError
      ? invite.error.message
      : remove.error instanceof ApiError
        ? remove.error.message
        : null;

  return (
    <GlassCard className="p-6">
      <div className="flex items-center justify-between">
        <p className="text-xs font-medium uppercase tracking-wider text-slate-500">Team</p>
        {isOwner && !open && (
          <button
            onClick={() => setOpen(true)}
            className="inline-flex items-center gap-1 text-xs text-accent hover:text-accent-soft"
          >
            <MailPlus size={14} /> Invite
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
              className="flex items-center justify-between gap-3 rounded-xl border border-slate-100 bg-slate-50 px-3 py-2"
            >
              <div className="flex min-w-0 items-center gap-2.5">
                <span className="grid h-8 w-8 shrink-0 place-items-center rounded-full border border-slate-200 text-slate-500">
                  <UserRound size={15} />
                </span>
                <div className="min-w-0">
                  <p className="truncate font-mono text-xs text-slate-600">
                    {m.userId.slice(0, 8)}…{isMe && <span className="ml-1 text-accent">(you)</span>}
                  </p>
                  <p className="text-[11px] uppercase tracking-wider text-slate-500">{m.role}</p>
                </div>
              </div>
              {removable && (
                <button
                  onClick={() => remove.mutate(m.userId)}
                  disabled={remove.isPending}
                  className="text-slate-500 transition-colors hover:text-rose-600 disabled:opacity-40"
                  aria-label="Remove member"
                >
                  <Trash2 size={15} />
                </button>
              )}
            </li>
          );
        })}
      </ul>

      {/* Pending invitations (owner only) */}
      {isOwner && pending.length > 0 && (
        <div className="mt-4">
          <p className="mb-2 text-[11px] font-medium uppercase tracking-wider text-slate-500">Pending invites</p>
          <ul className="space-y-2">
            {pending.map((inv) => (
              <li
                key={inv.id}
                className="flex items-center justify-between gap-3 rounded-xl border border-dashed border-slate-200 px-3 py-2"
              >
                <div className="min-w-0">
                  <p className="truncate text-xs text-slate-600">{inv.email}</p>
                  <p className="text-[11px] uppercase tracking-wider text-slate-500">{inv.role} · invited</p>
                </div>
                {isOwner && (
                  <button
                    onClick={() => revoke.mutate(inv.id)}
                    disabled={revoke.isPending}
                    className="text-slate-500 transition-colors hover:text-rose-600 disabled:opacity-40"
                    aria-label="Revoke invitation"
                  >
                    <X size={15} />
                  </button>
                )}
              </li>
            ))}
          </ul>
        </div>
      )}

      {open && (
        <form onSubmit={submit} className="mt-4 space-y-3 border-t border-slate-200 pt-4">
          <Field
            label="Invite by email"
            type="email"
            value={email}
            onChange={setEmail}
            placeholder="colleague@university.edu"
          />
          <label className="block">
            <span className="mb-1.5 block text-xs font-medium uppercase tracking-wider text-slate-500">Role</span>
            <div className="flex gap-2">
              {INVITE_ROLES.map((r) => (
                <button
                  type="button"
                  key={r}
                  onClick={() => setRole(r)}
                  className={`flex-1 rounded-xl border px-2 py-2 text-[11px] uppercase tracking-wider transition-all ${
                    role === r
                      ? "border-accent/60 bg-accent/10 text-slate-900"
                      : "border-slate-200 text-slate-500 hover:border-slate-300"
                  }`}
                >
                  {r}
                </button>
              ))}
            </div>
          </label>
          {error && <p className="text-sm text-rose-600">{error}</p>}
          <div className="flex gap-2">
            <Button type="submit" size="sm" disabled={invite.isPending || !email.trim()}>
              {invite.isPending ? "Sending…" : "Send invite"}
            </Button>
            <Button type="button" size="sm" variant="ghost" onClick={() => setOpen(false)}>
              Cancel
            </Button>
          </div>
        </form>
      )}

      {!open && error && <p className="mt-3 text-sm text-rose-600">{error}</p>}
    </GlassCard>
  );
}
