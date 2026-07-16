"use client";

import { useState } from "react";
import { motion } from "framer-motion";
import { GlassCard } from "@/components/ui/glass-card";
import { Button } from "@/components/ui/button";
import { Field } from "@/components/ui/field";
import { DepartmentsSection } from "@/features/org/components/departments-section";
import { InstitutionSection } from "@/features/org/components/institution-section";
import { ApiError, type Profile } from "@/lib/api";
import { useMe } from "../api/use-me";
import { useUpdateProfile } from "../api/use-update-profile";

const ORG_ROLES = ["DEPARTMENT_ADMIN", "INSTITUTION_ADMIN", "PLATFORM_ADMIN"];

export function ProfileSettings() {
  const me = useMe();

  if (me.isLoading) {
    return (
      <div className="grid place-items-center py-32">
        <div className="h-10 w-10 animate-spin-slow rounded-full border-2 border-white/10 border-t-accent" />
      </div>
    );
  }
  if (me.isError || !me.data) {
    return (
      <GlassCard className="p-8 text-center text-sm text-rose-300">Couldn’t load your profile.</GlassCard>
    );
  }

  const profile = me.data;
  const roles = profile.roles ?? [];
  const isOrgAdmin = roles.some((r) => ORG_ROLES.includes(r));
  const canEditInstitution = roles.includes("INSTITUTION_ADMIN") || roles.includes("PLATFORM_ADMIN");

  return (
    <div className="space-y-8">
      <motion.div initial={{ opacity: 0, y: 16 }} animate={{ opacity: 1, y: 0 }}>
        <p className="eyebrow">Account</p>
        <h1 className="mt-1 font-display text-3xl font-bold text-white">Settings</h1>
      </motion.div>

      <ProfileForm profile={profile} />

      {isOrgAdmin && <InstitutionSection institutionId={profile.institutionId} canEdit={canEditInstitution} />}
      {isOrgAdmin && <DepartmentsSection canCreate={roles.includes("INSTITUTION_ADMIN")} />}
    </div>
  );
}

/** Editable profile form; mounted only once the profile is loaded so state seeds correctly. */
function ProfileForm({ profile }: { profile: Profile }) {
  const update = useUpdateProfile();
  const [fullName, setFullName] = useState(profile.fullName ?? "");
  const [academicLevel, setAcademicLevel] = useState(profile.academicLevel ?? "");
  const [fieldOfStudy, setFieldOfStudy] = useState(profile.fieldOfStudy ?? "");
  const [orcid, setOrcid] = useState(profile.orcid ?? "");

  async function submit(e: React.FormEvent) {
    e.preventDefault();
    try {
      await update.mutateAsync({ fullName, academicLevel, fieldOfStudy, orcid });
    } catch {
      /* surfaced via update.error */
    }
  }

  const error = update.error instanceof ApiError ? update.error.message : null;

  return (
    <GlassCard className="p-7">
      <h2 className="font-display text-lg font-semibold text-white">Profile</h2>
      <p className="mt-1 text-sm text-slate-400">{profile.email}</p>

      <form onSubmit={submit} className="mt-5 space-y-4">
        <Field label="Full name" type="text" value={fullName} onChange={setFullName} />
        <div className="grid gap-4 sm:grid-cols-2">
          <Field label="Academic level" type="text" value={academicLevel} onChange={setAcademicLevel} placeholder="e.g. PhD" required={false} />
          <Field label="Field of study" type="text" value={fieldOfStudy} onChange={setFieldOfStudy} placeholder="e.g. Computer Science" required={false} />
        </div>
        <Field label="ORCID" type="text" value={orcid} onChange={setOrcid} placeholder="0000-0000-0000-0000" required={false} />

        {error && <p className="text-sm text-rose-400">{error}</p>}

        <Button type="submit" size="md" disabled={update.isPending || !fullName}>
          {update.isPending ? "Saving…" : update.isSuccess ? "Saved" : "Save changes"}
        </Button>
      </form>
    </GlassCard>
  );
}
