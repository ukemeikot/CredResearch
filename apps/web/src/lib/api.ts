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
    if (await tryRefresh()) return request<T>(path, init, false);
  }

  if (res.status === 204) return undefined as T;

  const body = await res.json().catch(() => ({}));
  if (!res.ok) {
    throw new ApiError(res.status, body.detail || body.message || res.statusText, body.code);
  }
  return body as T;
}

// ── Auth ────────────────────────────────────────────────────────────────────
export interface TokenResponse {
  accessToken: string;
  refreshToken: string;
  expiresIn: number;
  user: { id: string; roles: string[]; institutionId: string; plan: string };
}

export const api = {
  register: (b: { email: string; password: string; fullName: string }) =>
    request<{ userId: string; message: string }>("/auth/register", {
      method: "POST",
      body: JSON.stringify(b),
    }),

  login: (b: { email: string; password: string }) =>
    request<TokenResponse>("/auth/login", { method: "POST", body: JSON.stringify(b) }),

  verifyEmail: (token: string) =>
    request<{ message: string }>("/auth/verify-email", {
      method: "POST",
      body: JSON.stringify({ token }),
    }),

  me: () =>
    request<{
      id: string;
      email: string;
      fullName: string;
      institutionId: string;
      roles: string[];
      emailVerified: boolean;
    }>("/users/me"),

  // ── Projects (Phase 2) ──────────────────────────────────────────────────
  listProjects: () => request<ProjectSummary[]>("/projects"),
  createProject: (b: { title: string; level: string; departmentId?: string }) =>
    request<ProjectSummary>("/projects", { method: "POST", body: JSON.stringify(b) }),
  getProject: (id: string) => request<ProjectDetail>(`/projects/${id}`),
};

export interface ProjectSummary {
  id: string;
  title: string;
  level: string;
  status: string;
}

export interface ProjectDetail extends ProjectSummary {
  memberCount?: number;
  nextMilestone?: { title: string; dueDate: string } | null;
}
