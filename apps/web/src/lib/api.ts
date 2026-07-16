import { useAuth } from "./auth-store";

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

async function request<T>(path: string, init: RequestInit = {}, retry = true): Promise<T> {
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
    if (await refreshOnce()) return request<T>(path, init, false);
  }

  if (res.status === 204) return undefined as T;

  const body = await res.json().catch(() => ({}));
  if (!res.ok) {
    throw new ApiError(res.status, body.detail || body.message || res.statusText, body.code);
  }
  return body as T;
}

const json = (method: string, body?: unknown): RequestInit => ({
  method,
  ...(body === undefined ? {} : { body: JSON.stringify(body) }),
});

// ── Types (mirror the backend DTOs) ──────────────────────────────────────────
export type ProjectStatus =
  | "DRAFT"
  | "PROPOSAL"
  | "IN_PROGRESS"
  | "UNDER_REVIEW"
  | "REVISIONS"
  | "APPROVED"
  | "COMPLETED";

export type ProjectMemberRole = "OWNER" | "SUPERVISOR" | "CONSULTANT" | "VIEWER";

export interface TokenResponse {
  accessToken: string;
  refreshToken: string;
  expiresIn: number;
  user: { id: string; roles: string[]; institutionId: string; plan: string };
}

export interface Profile {
  id: string;
  email: string;
  fullName: string;
  institutionId: string;
  departmentId: string | null;
  academicLevel: string | null;
  fieldOfStudy: string | null;
  orcid: string | null;
  emailVerified: boolean;
  roles: string[];
}

export interface ProjectSummary {
  id: string;
  institutionId: string;
  departmentId: string | null;
  ownerUserId: string;
  title: string;
  level: string | null;
  status: ProjectStatus;
  abstractText: string | null;
}

export interface Milestone {
  id: string;
  projectId: string;
  title: string;
  dueDate: string | null;
  status: string | null;
  completedAt: string | null;
}

export interface ProjectMember {
  id: string;
  projectId: string;
  userId: string;
  role: ProjectMemberRole;
}

export interface Activity {
  id: string;
  projectId: string;
  actorUserId: string;
  type: string;
  payload: string | null;
  createdAt: string;
}

export interface Dashboard {
  status: ProjectStatus;
  totalMilestones: number;
  completedMilestones: number;
  nextMilestone: Milestone | null;
  memberCount: number;
}

export interface ProjectDetail {
  project: ProjectSummary;
  dashboard: Dashboard;
  members: ProjectMember[];
  milestones: Milestone[];
}

export interface Institution {
  id: string;
  name: string;
  country: string | null;
  type: string | null;
  personalTenant: boolean;
  status: string;
}

export interface Department {
  id: string;
  institutionId: string;
  name: string;
  code: string | null;
}

// ── API surface ───────────────────────────────────────────────────────────
export const api = {
  // Auth
  register: (b: { email: string; password: string; fullName: string }) =>
    request<{ userId: string; message: string }>("/auth/register", json("POST", b)),
  login: (b: { email: string; password: string; device?: string }) =>
    request<TokenResponse>("/auth/login", json("POST", b)),
  logout: (refreshToken: string) => request<void>("/auth/logout", json("POST", { refreshToken })),
  logoutAll: () => request<void>("/auth/logout-all", json("POST")),
  verifyEmail: (token: string) =>
    request<{ message: string }>("/auth/verify-email", json("POST", { token })),
  forgotPassword: (email: string) =>
    request<{ message: string }>("/auth/password/forgot", json("POST", { email })),
  resetPassword: (b: { token: string; password: string }) =>
    request<{ message: string }>("/auth/password/reset", json("POST", b)),

  // Users
  me: () => request<Profile>("/users/me"),
  updateProfile: (b: {
    fullName?: string;
    academicLevel?: string;
    fieldOfStudy?: string;
    orcid?: string;
  }) => request<Profile>("/users/me", json("PATCH", b)),

  // Projects
  listProjects: (p: { limit?: number; offset?: number } = {}) =>
    request<ProjectSummary[]>(`/projects?limit=${p.limit ?? 20}&offset=${p.offset ?? 0}`),
  createProject: (b: {
    title: string;
    level?: string;
    departmentId?: string;
    abstractText?: string;
  }) => request<ProjectSummary>("/projects", json("POST", b)),
  getProject: (id: string) => request<ProjectDetail>(`/projects/${id}`),
  updateProject: (
    id: string,
    b: { title?: string; level?: string; abstractText?: string; departmentId?: string },
  ) => request<ProjectSummary>(`/projects/${id}`, json("PATCH", b)),
  transitionStatus: (id: string, status: ProjectStatus) =>
    request<ProjectSummary>(`/projects/${id}/status`, json("POST", { status })),
  addMember: (id: string, b: { userId: string; role: ProjectMemberRole }) =>
    request<ProjectMember>(`/projects/${id}/members`, json("POST", b)),
  removeMember: (id: string, userId: string) =>
    request<void>(`/projects/${id}/members/${userId}`, json("DELETE")),
  addMilestone: (id: string, b: { title: string; dueDate?: string; status?: string }) =>
    request<Milestone>(`/projects/${id}/milestones`, json("POST", b)),
  listActivities: (id: string, p: { limit?: number; offset?: number } = {}) =>
    request<Activity[]>(
      `/projects/${id}/activities?limit=${p.limit ?? 50}&offset=${p.offset ?? 0}`,
    ),

  // Organisation
  getInstitution: (id: string) => request<Institution>(`/institutions/${id}`),
  createInstitution: (b: { name: string; country?: string; type?: string }) =>
    request<Institution>("/institutions", json("POST", b)),
  updateInstitution: (id: string, b: { name?: string; country?: string; type?: string }) =>
    request<Institution>(`/institutions/${id}`, json("PATCH", b)),
  listDepartments: () => request<Department[]>("/departments"),
  createDepartment: (b: { name: string; code?: string }) =>
    request<Department>("/departments", json("POST", b)),
  updateDepartment: (id: string, b: { name?: string; code?: string }) =>
    request<Department>(`/departments/${id}`, json("PATCH", b)),
};
