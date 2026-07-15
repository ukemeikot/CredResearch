"use client";

import { useQuery } from "@tanstack/react-query";
import { api } from "@/lib/api";

/** Current authenticated user's profile. */
export function useMe() {
  return useQuery({ queryKey: ["me"], queryFn: api.me });
}
