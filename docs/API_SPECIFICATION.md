# API Specification — CredResearch

Related: [Technical Specification](./TECHNICAL_SPECIFICATION.md) · [ERD](./ERD.md) · [Security & Compliance](./SECURITY_AND_COMPLIANCE.md)

Base path: `/api/v1`. Auth: `Authorization: Bearer <access_token>` unless noted. Errors: RFC 7807 `application/problem+json`. The full machine-readable contract lives in `packages/api-contract/openapi.yaml` (OpenAPI 3.1) — this document is the human-readable summary.

**Rate-limit tiers:** `public` (unauthenticated, strict), `standard` (authenticated), `ai` (plan-credit gated), `webhook` (provider-scoped). Idempotency noted per endpoint; all webhooks are idempotent by provider event id.

---

## `/auth`
| Method | Path | Purpose | Role | Tier |
|---|---|---|---|---|
| POST | `/auth/register` | Create account | public | public |
| POST | `/auth/login` | Get access+refresh tokens | public | public |
| POST | `/auth/refresh` | Rotate tokens | public(refresh) | public |
| POST | `/auth/logout` | Revoke current refresh token | any | standard |
| POST | `/auth/logout-all` | Revoke all sessions | any | standard |
| POST | `/auth/verify-email` | Confirm email | public | public |
| POST | `/auth/password/forgot` | Send reset link | public | public |
| POST | `/auth/password/reset` | Reset via token | public | public |

```json
// POST /auth/login -> 200
{ "accessToken": "eyJ...", "refreshToken": "opaque", "expiresIn": 900,
  "user": { "id": "...", "roles": ["STUDENT"], "institutionId": "...", "plan": "FREE" } }
```

## `/users`
| Method | Path | Purpose | Role |
|---|---|---|---|
| GET | `/users/me` | Current profile | any |
| PATCH | `/users/me` | Update profile | any |
| GET | `/users/{id}` | Get user (same tenant) | DEPARTMENT_ADMIN+ |
| GET | `/users` | List/search (tenant) | DEPARTMENT_ADMIN+ |
| POST | `/users/{id}/roles` | Assign role | INSTITUTION_ADMIN+ |
| POST | `/users/{id}/suspend` | Suspend | INSTITUTION_ADMIN+ |

## `/institutions`
| Method | Path | Purpose | Role |
|---|---|---|---|
| POST | `/institutions` | Create | PLATFORM_ADMIN (or self-serve) |
| GET | `/institutions/{id}` | Get | INSTITUTION_ADMIN+ |
| PATCH | `/institutions/{id}` | Update | INSTITUTION_ADMIN+ |
| POST | `/institutions/{id}/students/import` | Bulk CSV import | INSTITUTION_ADMIN |

## `/departments`
| Method | Path | Purpose | Role |
|---|---|---|---|
| POST | `/departments` | Create | INSTITUTION_ADMIN |
| GET | `/departments` | List (tenant) | DEPARTMENT_ADMIN+ |
| PATCH | `/departments/{id}` | Update | DEPARTMENT_ADMIN+ |

## `/projects`
| Method | Path | Purpose | Role | Idempotent |
|---|---|---|---|---|
| POST | `/projects` | Create project | STUDENT/CONSULTANT | yes (key) |
| GET | `/projects` | List my/visible projects | any | yes |
| GET | `/projects/{id}` | Project detail + dashboard | member | yes |
| PATCH | `/projects/{id}` | Update | OWNER | yes |
| POST | `/projects/{id}/members` | Add member/supervisor | OWNER | yes |
| DELETE | `/projects/{id}/members/{userId}` | Remove member | OWNER | yes |
| POST | `/projects/{id}/milestones` | Add milestone | OWNER/SUPERVISOR | yes |
| GET | `/projects/{id}/activities` | Activity feed | member | yes |
| POST | `/projects/{id}/status` | Transition status | OWNER/SUPERVISOR | yes |

```json
// POST /projects
{ "title": "Phytochemical screening of Phyllanthus amarus", "level": "UG",
  "departmentId": "..." }
```

## `/templates`
| Method | Path | Purpose | Role |
|---|---|---|---|
| GET | `/templates` | List (global + tenant) | any |
| GET | `/templates/{id}` | Get with sections + format rules | any |
| POST | `/templates` | Create/clone | DEPARTMENT_ADMIN+ |
| PATCH | `/templates/{id}` | Update sections/rules | DEPARTMENT_ADMIN+ |

## `/documents`
| Method | Path | Purpose | Role | Idempotent |
|---|---|---|---|---|
| POST | `/documents` | Create from template | OWNER | yes (key) |
| GET | `/documents/{id}` | Get with sections | member | yes |
| GET | `/documents/{id}/sections/{sectionId}` | Get section | member | yes |
| PUT | `/documents/{id}/sections/{sectionId}` | Autosave (optimistic lock via `version`) | OWNER | no |
| GET | `/documents/{id}/sections/{sectionId}/versions` | Version history | member | yes |
| POST | `/documents/{id}/sections/{sectionId}/restore` | Restore a version | OWNER | yes |
| POST | `/documents/{id}/export` | Export DOCX/PDF/ZIP (async job) | member | yes (key) |

```json
// PUT .../sections/{id}  (autosave)
{ "content": { "type": "doc", "content": [/* ProseMirror JSON */] }, "version": 7 }
// 409 if server version != 7  -> client resolves
```

## `/reviews`
| Method | Path | Purpose | Role |
|---|---|---|---|
| POST | `/reviews/requests` | Submit section/document for review | OWNER |
| GET | `/reviews/requests` | List (mine as student or supervisor) | member/supervisor |
| GET | `/reviews/requests/{id}` | Detail + comments + decisions | participant |
| POST | `/reviews/requests/{id}/comments` | Add inline comment | supervisor/consultant |
| POST | `/reviews/requests/{id}/comments/{cid}/resolve` | Resolve thread | participant |
| POST | `/reviews/requests/{id}/decision` | Record decision | supervisor |
| GET | `/reviews/inbox` | Supervisor pending queue | SUPERVISOR |

Magic-link supervisors authenticate with a scoped review token (header `X-Review-Token`) on `/reviews/requests/{id}*` only.

## `/invitations`
| Method | Path | Purpose | Role | Auth |
|---|---|---|---|---|
| POST | `/invitations` | Invite supervisor/admin/student | OWNER/ADMIN | bearer |
| POST | `/invitations/accept` | Accept (create/link account) | public | token |
| GET | `/invitations/{token}` | Resolve invite context | public | token |

## `/papers`
| Method | Path | Purpose | Role |
|---|---|---|---|
| POST | `/papers` | Register upload → signed PUT URL | member |
| POST | `/papers/{id}/ingest` | Parse + embed (async job) | member |
| GET | `/papers` | List project papers | member |
| GET | `/papers/{id}` | Paper + extracted summary | member |
| POST | `/papers/import` | Import BibTeX/RIS | member |
| GET | `/papers/{id}/export` | Export BibTeX/RIS | member |
| POST | `/papers/literature-matrix` | Generate matrix over selected papers (async) | member |

```json
// POST /papers -> 201
{ "fileId": "...", "uploadUrl": "https://r2.../signed", "expiresIn": 600 }
```

## `/citations`
| Method | Path | Purpose | Role |
|---|---|---|---|
| GET | `/citations` | List project citations | member |
| POST | `/citations` | Add citation (manual/from paper) | member |
| PATCH | `/citations/{id}` | Edit | member |
| DELETE | `/citations/{id}` | Remove | member |
| GET | `/citations/reference-list?style=APA\|IEEE\|HARVARD` | Rendered reference list (CSL) | member |

## `/ai`  (tier: ai, plan-credit gated, async)
| Method | Path | Purpose | Role |
|---|---|---|---|
| POST | `/ai/topics` | Topic suggestions + feasibility | member |
| POST | `/ai/proposal-outline` | Proposal outline | member |
| POST | `/ai/objectives` | Aim/objectives/RQs/hypotheses | member |
| POST | `/ai/methodology` | Methodology suggestion | member |
| POST | `/ai/problem-statement` | Refine problem statement | member |
| POST | `/ai/alignment` | Run Research Alignment Engine | member |
| POST | `/ai/paper-summary` | Summarize a paper | member |
| POST | `/ai/rag/query` | RAG over uploaded papers (cited) | member |

```json
// POST /ai/objectives -> 202
{ "jobId": "...", "creditsRemaining": 23 }
// GET /jobs/{jobId} -> 200 when done
{ "status": "SUCCEEDED",
  "result": { "aim": "...", "objectives": ["..."], "researchQuestions": ["..."],
              "hypotheses": ["..."], "disclosureEntryId": "..." } }
```

## `/jobs`
| Method | Path | Purpose | Role |
|---|---|---|---|
| GET | `/jobs/{id}` | Poll async job status/result | requester |
| GET | `/jobs/{id}/events` | SSE progress stream (optional) | requester |

## `/disclosure`
| Method | Path | Purpose | Role |
|---|---|---|---|
| GET | `/disclosure/documents/{documentId}` | Ledger entries + per-section signal | member/supervisor |
| POST | `/disclosure/documents/{documentId}/statement` | Generate AI-Use Disclosure Statement (PDF) | member |

## `/questionnaires` (MVP+)
| Method | Path | Purpose | Role |
|---|---|---|---|
| POST | `/questionnaires/generate` | Draft from objectives | member |
| POST | `/questionnaires` | Create/edit | member |
| GET | `/questionnaires/{id}` | Get | member |
| POST | `/questionnaires/{id}/links` | Publish public link | member |
| GET | `/s/{token}` | Public survey render | public |
| POST | `/s/{token}/responses` | Submit response (consent) | public |
| GET | `/questionnaires/{id}/responses.csv` | Export CSV | member |

## `/datasets` & `/analysis` (MVP+)
| Method | Path | Purpose | Role |
|---|---|---|---|
| POST | `/datasets` | Upload CSV → signed URL | member |
| GET | `/datasets/{id}` | Preview + columns + missing | member |
| POST | `/analysis` | Run descriptive analysis (async) | member |
| GET | `/analysis/{id}` | Results + charts | member |
| POST | `/analysis/{id}/chapter4` | Generate Chapter 4 starter | member |

## `/similarity` (MVP+)
| Method | Path | Purpose | Role |
|---|---|---|---|
| POST | `/similarity/checks` | Run internal pre-check (async) | member |
| GET | `/similarity/checks/{id}` | Matches + report | member |

## `/billing`
| Method | Path | Purpose | Role |
|---|---|---|---|
| GET | `/billing/plans` | List plans (multi-currency) | any |
| GET | `/billing/subscription` | Current subscription + usage | any |
| POST | `/billing/checkout` | Start Paystack/Flutterwave checkout | any |
| POST | `/billing/invoice-request` | Request manual invoice (institution) | INSTITUTION_ADMIN |
| GET | `/billing/payments` | Payment history | any |

## `/webhooks`  (tier: webhook, no bearer; signature-verified, idempotent)
| Method | Path | Purpose |
|---|---|---|
| POST | `/webhooks/paystack` | Paystack events (verify `x-paystack-signature`) |
| POST | `/webhooks/flutterwave` | Flutterwave events (verify `verif-hash`) |

## `/notifications`
| Method | Path | Purpose | Role |
|---|---|---|---|
| GET | `/notifications` | List | any |
| POST | `/notifications/{id}/read` | Mark read | any |
| POST | `/notifications/read-all` | Mark all read | any |

## `/admin`
| Method | Path | Purpose | Role |
|---|---|---|---|
| GET | `/admin/users` | Manage users | PLATFORM_ADMIN |
| GET | `/admin/institutions` | Manage institutions | PLATFORM_ADMIN |
| GET | `/admin/payments` | View payments | PLATFORM_ADMIN |
| GET | `/admin/usage` | Usage + AI-cost dashboards | PLATFORM_ADMIN |
| GET | `/admin/stats/projects` | Project statistics | PLATFORM_ADMIN |
| GET | `/admin/plans` | Manage plans | PLATFORM_ADMIN |
| POST | `/admin/feature-flags` | Toggle flags | PLATFORM_ADMIN |

## Operational
| Method | Path | Purpose | Auth |
|---|---|---|---|
| GET | `/health` | Liveness (Actuator) | none |
| GET | `/ready` | Readiness (deps healthy) | none |
