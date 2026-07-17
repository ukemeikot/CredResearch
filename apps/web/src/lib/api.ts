import { z } from "zod";
import { useAuth } from "./auth-store";
import * as S from "./schemas";
import type { ProjectMemberRole, ProjectStatus, TokenResponse } from "./schemas";

const BASE_URL =
  process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:18080/api/v1";

export class ApiError extends Error {
  status: number;
  code?: string;
  constructor(status: number, message: string, code?: string) {
    super(message);
    this.status = status;
    this.code = code;
  }
}

/** Exchange the refresh token for a fresh session. Returns true on success. */
async function tryRefresh(): Promise<boolean> {
  const refreshToken = useAuth.getState().refreshToken;
  if (!refreshToken) return false;
  try {
    const res = await fetch(`${BASE_URL}/auth/refresh`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ refreshToken }),
      cache: "no-store",
    });
    if (!res.ok) {
      useAuth.getState().clear();
      return false;
    }
    const data = (await res.json()) as TokenResponse;
    useAuth.getState().setSession({
      accessToken: data.accessToken,
      refreshToken: data.refreshToken,
      user: data.user,
    });
    return true;
  } catch {
    useAuth.getState().clear();
    return false;
  }
}

// A single in-flight refresh is shared by all concurrent 401s so we never fire
// N parallel /auth/refresh calls (which would rotate the token N times and log
// the user out). The first 401 starts it; the rest await the same promise.
let refreshInFlight: Promise<boolean> | null = null;
function refreshOnce(): Promise<boolean> {
  if (!refreshInFlight) {
    refreshInFlight = tryRefresh().finally(() => {
      refreshInFlight = null;
    });
  }
  return refreshInFlight;
}

/**
 * Force a token refresh (e.g. after institution onboarding changes the caller's tenant/roles, which
 * are embedded in the access token). Returns true if a new session was issued.
 */
export function refreshSession(): Promise<boolean> {
  return refreshOnce();
}

async function request<T>(
  path: string,
  init: RequestInit = {},
  schema?: z.ZodType<T>,
  retry = true,
): Promise<T> {
  const token = useAuth.getState().accessToken;
  const res = await fetch(`${BASE_URL}${path}`, {
    ...init,
    headers: {
      "Content-Type": "application/json",
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...(init.headers ?? {}),
    },
    cache: "no-store",
  });

  // Access token expired → refresh once and retry (never for /auth/* endpoints).
  if (res.status === 401 && retry && !path.startsWith("/auth/")) {
    if (await refreshOnce()) return request<T>(path, init, schema, false);
  }

  if (res.status === 204) return undefined as T;

  const body = await res.json().catch(() => ({}));
  if (!res.ok) {
    throw new ApiError(res.status, body.detail || body.message || res.statusText, body.code);
  }
  // Validate the response against its zod schema at the boundary. On drift we log (so contract
  // mismatches surface in the console) but still return the payload, so a benign backend change
  // never hard-breaks a working screen.
  if (schema) {
    const parsed = schema.safeParse(body);
    if (parsed.success) return parsed.data;
    if (process.env.NODE_ENV !== "production") {
      console.warn(`[api] response validation failed for ${path}:`, parsed.error.issues);
    }
    return body as T;
  }
  return body as T;
}

/** Fetch a binary file with auth (+ one refresh-retry) and trigger a browser download. */
async function downloadFile(path: string, fallbackName: string, retry = true): Promise<void> {
  const token = useAuth.getState().accessToken;
  const res = await fetch(`${BASE_URL}${path}`, {
    headers: { ...(token ? { Authorization: `Bearer ${token}` } : {}) },
    cache: "no-store",
  });
  if (res.status === 401 && retry) {
    if (await refreshOnce()) return downloadFile(path, fallbackName, false);
  }
  if (!res.ok) {
    const body = await res.json().catch(() => ({}));
    throw new ApiError(res.status, body.detail || body.message || res.statusText, body.code);
  }
  // Prefer the server-provided filename from Content-Disposition.
  const cd = res.headers.get("Content-Disposition") ?? "";
  const match = /filename\*?=(?:UTF-8''|")?([^";]+)/i.exec(cd);
  const name = match ? decodeURIComponent(match[1].replace(/"/g, "")) : fallbackName;
  const blob = await res.blob();
  const url = URL.createObjectURL(blob);
  const a = document.createElement("a");
  a.href = url;
  a.download = name;
  document.body.appendChild(a);
  a.click();
  a.remove();
  URL.revokeObjectURL(url);
}

const json = (method: string, body?: unknown): RequestInit => ({
  method,
  ...(body === undefined ? {} : { body: JSON.stringify(body) }),
});

// ── Types (mirror the backend DTOs) ──────────────────────────────────────────
// Types are inferred from the zod schemas in ./schemas (single source of truth). Responses are
// validated against those schemas at the fetch boundary (see `request` above).
export type {
  ProjectStatus,
  ProjectMemberRole,
  TokenResponse,
  Profile,
  ProjectSummary,
  Milestone,
  ProjectMember,
  Activity,
  Dashboard,
  ProjectDetail,
  Institution,
  Department,
  Template,
  TemplateSection,
  FormatRule,
  TemplateDetail,
  DocumentSummary,
  DocSection,
  DocumentDetail,
  DocVersion,
  Invitation,
  AiTopic,
  AiObjectives,
  AiAlignmentFinding,
  AiAlignment,
  AiCredits,
  DisclosureEntry,
} from "./schemas";

// ── API surface ───────────────────────────────────────────────────────────
// Each response-returning call passes its zod schema so the payload is validated at the boundary.
export const api = {
  // Auth
  register: (b: { email: string; password: string; fullName: string }) =>
    request("/auth/register", json("POST", b), S.RegisterResponseSchema),
  login: (b: { email: string; password: string; device?: string }) =>
    request("/auth/login", json("POST", b), S.TokenResponseSchema),
  logout: (refreshToken: string) => request<void>("/auth/logout", json("POST", { refreshToken })),
  logoutAll: () => request<void>("/auth/logout-all", json("POST")),
  verifyEmail: (token: string) =>
    request("/auth/verify-email", json("POST", { token }), S.MessageSchema),
  forgotPassword: (email: string) =>
    request("/auth/password/forgot", json("POST", { email }), S.MessageSchema),
  resetPassword: (b: { token: string; password: string }) =>
    request("/auth/password/reset", json("POST", b), S.MessageSchema),

  // Users
  me: () => request("/users/me", undefined, S.ProfileSchema),
  updateProfile: (b: {
    fullName?: string;
    academicLevel?: string;
    fieldOfStudy?: string;
    orcid?: string;
  }) => request("/users/me", json("PATCH", b), S.ProfileSchema),

  // Projects
  listProjects: (p: { limit?: number; offset?: number } = {}) =>
    request(`/projects?limit=${p.limit ?? 20}&offset=${p.offset ?? 0}`, undefined, z.array(S.ProjectSummarySchema)),
  createProject: (b: {
    title: string;
    level?: string;
    departmentId?: string;
    abstractText?: string;
  }) => request("/projects", json("POST", b), S.ProjectSummarySchema),
  getProject: (id: string) => request(`/projects/${id}`, undefined, S.ProjectDetailSchema),
  updateProject: (
    id: string,
    b: { title?: string; level?: string; abstractText?: string; departmentId?: string },
  ) => request(`/projects/${id}`, json("PATCH", b), S.ProjectSummarySchema),
  transitionStatus: (id: string, status: ProjectStatus) =>
    request(`/projects/${id}/status`, json("POST", { status }), S.ProjectSummarySchema),
  addMember: (id: string, b: { userId: string; role: ProjectMemberRole }) =>
    request(`/projects/${id}/members`, json("POST", b), S.ProjectMemberSchema),
  removeMember: (id: string, userId: string) =>
    request<void>(`/projects/${id}/members/${userId}`, json("DELETE")),
  addMilestone: (id: string, b: { title: string; dueDate?: string; status?: string }) =>
    request(`/projects/${id}/milestones`, json("POST", b), S.MilestoneSchema),
  listActivities: (id: string, p: { limit?: number; offset?: number } = {}) =>
    request(
      `/projects/${id}/activities?limit=${p.limit ?? 50}&offset=${p.offset ?? 0}`,
      undefined,
      z.array(S.ActivitySchema),
    ),

  // Organisation
  getInstitution: (id: string) => request(`/institutions/${id}`, undefined, S.InstitutionSchema),
  createInstitution: (b: { name: string; country?: string; type?: string }) =>
    request("/institutions", json("POST", b), S.InstitutionSchema),
  updateInstitution: (id: string, b: { name?: string; country?: string; type?: string }) =>
    request(`/institutions/${id}`, json("PATCH", b), S.InstitutionSchema),
  listDepartments: () => request("/departments", undefined, z.array(S.DepartmentSchema)),
  createDepartment: (b: { name: string; code?: string }) =>
    request("/departments", json("POST", b), S.DepartmentSchema),
  updateDepartment: (id: string, b: { name?: string; code?: string }) =>
    request(`/departments/${id}`, json("PATCH", b), S.DepartmentSchema),

  // Onboarding
  onboardInstitution: (b: { name: string; country?: string; type?: string }) =>
    request("/onboarding/institution", json("POST", b), S.OnboardInstitutionResponseSchema),

  // AI Research Assistant (Phase 4) — backend proxies to the private worker
  aiTopics: (b: { field: string; interests?: string; level?: string }) =>
    request("/ai/topics", json("POST", b), S.AiTopicsResponseSchema),
  aiObjectives: (b: { topic: string; problem?: string; level?: string }) =>
    request("/ai/objectives", json("POST", b), S.AiObjectivesSchema),
  aiProblemStatement: (b: { topic: string; context?: string }) =>
    request("/ai/problem-statement", json("POST", b), S.AiProblemStatementResponseSchema),
  aiSectionAssist: (b: {
    heading: string;
    guidance?: string;
    current_text?: string;
    instruction?: string;
  }) => request("/ai/section-assist", json("POST", b), S.AiSectionAssistResponseSchema),
  aiAlignment: (b: {
    title: string;
    abstract?: string;
    objectives?: string[];
    sections?: { heading: string; text: string }[];
  }) => request("/ai/alignment", json("POST", b), S.AiAlignmentSchema),
  aiCredits: () => request("/ai/credits", undefined, S.AiCreditsSchema),

  // AI-Use Disclosure Ledger (Phase 4)
  disclosureList: (docId: string) =>
    request(`/disclosure/documents/${docId}`, undefined, z.array(S.DisclosureEntrySchema)),
  disclosureAppend: (
    docId: string,
    b: {
      documentSectionId?: string;
      aiRequestId?: string;
      featureKey: string;
      model?: string;
      suggestionSummary?: string;
      action?: string;
    },
  ) => request(`/disclosure/documents/${docId}/entries`, json("POST", b), S.DisclosureEntrySchema),

  // Templates
  listTemplates: () => request("/templates", undefined, z.array(S.TemplateSchema)),
  getTemplate: (id: string) => request(`/templates/${id}`, undefined, S.TemplateDetailSchema),

  // Documents
  listDocuments: (projectId: string) =>
    request(`/documents?projectId=${projectId}`, undefined, z.array(S.DocumentSummarySchema)),
  createDocument: (b: { projectId: string; templateId: string; title?: string }) =>
    request("/documents", json("POST", b), S.DocumentDetailSchema),
  getDocument: (id: string) => request(`/documents/${id}`, undefined, S.DocumentDetailSchema),
  getSection: (docId: string, sectionId: string) =>
    request(`/documents/${docId}/sections/${sectionId}`, undefined, S.DocSectionSchema),
  autosaveSection: (docId: string, sectionId: string, b: { content: unknown; version: number }) =>
    request(`/documents/${docId}/sections/${sectionId}`, json("PUT", b), S.DocSectionSchema),
  addSection: (docId: string, b: { heading: string; chapter?: string }) =>
    request(`/documents/${docId}/sections`, json("POST", b), S.DocSectionSchema),
  updateSection: (
    docId: string,
    sectionId: string,
    b: { heading?: string; chapter?: string; orderIndex?: number },
  ) => request(`/documents/${docId}/sections/${sectionId}`, json("PATCH", b), S.DocSectionSchema),
  deleteSection: (docId: string, sectionId: string) =>
    request<void>(`/documents/${docId}/sections/${sectionId}`, json("DELETE")),
  listSectionVersions: (docId: string, sectionId: string) =>
    request(`/documents/${docId}/sections/${sectionId}/versions`, undefined, z.array(S.DocVersionSchema)),
  restoreSection: (docId: string, sectionId: string, versionId: string) =>
    request(`/documents/${docId}/sections/${sectionId}/restore`, json("POST", { versionId }), S.DocSectionSchema),
  downloadDocument: (docId: string, format: "docx" | "pdf") =>
    downloadFile(`/documents/${docId}/export?format=${format}`, `document.${format}`),

  // Invitations
  listInvitations: (projectId: string) =>
    request(`/projects/${projectId}/invitations`, undefined, z.array(S.InvitationSchema)),
  invite: (projectId: string, b: { email: string; role: ProjectMemberRole }) =>
    request(`/projects/${projectId}/invitations`, json("POST", b), S.InvitationSchema),
  revokeInvite: (projectId: string, invitationId: string) =>
    request<void>(`/projects/${projectId}/invitations/${invitationId}`, json("DELETE")),
  acceptInvite: (token: string) =>
    request("/invitations/accept", json("POST", { token }), S.AcceptInviteResponseSchema),
};
