"use client";

import { useProjectRole } from "../api/use-project-role";
import { MembersPanel } from "./members-panel";

export function ProjectTeamSection({ id }: { id: string }) {
  const { query, isOwner } = useProjectRole(id);
  if (!query.data) return null;
  const { project, members } = query.data;
  return (
    <div className="space-y-4">
      <div>
        <h2 className="font-display text-xl font-bold text-slate-900">Team &amp; invitations</h2>
        <p className="mt-1 text-sm text-slate-500">
          Members, supervisors and pending invitations. {isOwner ? "You can invite people or revoke a pending invite." : "Only the owner can change the team."}
        </p>
      </div>
      <MembersPanel id={id} members={members} ownerUserId={project.ownerUserId} isOwner={isOwner} />
    </div>
  );
}
