"use client";

import { useMutation, useQueryClient } from "@tanstack/react-query";
import { api, type Profile } from "@/lib/api";

/** Update the current user's profile; refreshes the cached `me` on success. */
export function useUpdateProfile() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: api.updateProfile,
    onSuccess: (updated) => qc.setQueryData<Profile>(["me"], updated),
  });
}
