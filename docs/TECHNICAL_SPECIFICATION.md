# Technical Specification — CredResearch

Related: [System Architecture](./SYSTEM_ARCHITECTURE.md) · [API Specification](./API_SPECIFICATION.md) · [Engineering Decisions](./ENGINEERING_DECISIONS.md)

## 1. Stack summary

| Layer | Technology |
|---|---|
| Frontend | Next.js (App Router), TypeScript, Tailwind, shadcn/ui, Tiptap, TanStack Query, Zustand (light client state) |
| Backend (core) | Java 21, Spring Boot, Spring Security, Spring Data JPA, Flyway |
| AI/Data worker | Python, FastAPI |
| Database | PostgreSQL + pgvector; PostgreSQL full-text search |
| Cache/queue | Redis (cache, rate limiting, job coordination) |
| Object storage | MinIO (dev), S3-compatible (prod: R2 / Spaces / S3 / MinIO) |
| Export | python-docx (DOCX) + Gotenberg/LibreOffice (PDF) |
| Citations | CSL / citeproc |
| Payments | Paystack, Flutterwave, manual invoice |
| Notifications | in-app, email (SMTP/SES/Resend), SMS (Termii), WhatsApp (optional) |
| Reverse proxy | Nginx + Let's Encrypt |
| CI/CD | GitHub Actions |
| Error monitoring | Sentry (all three services) |

## 2. Architecture style

**Modular monolith** for the Java backend (one deployable, internal module boundaries), plus a **separate Python AI/data worker**. This is two services, not microservices. See [ADR-002](./ENGINEERING_DECISIONS.md) and [ADR-003](./ENGINEERING_DECISIONS.md).

Backend modules (package-by-feature, each with its own controller/service/repository/domain):
`identity`, `org`, `project`, `template`, `document`, `review`, `paper`, `citation`, `ai`, `alignment`, `questionnaire`, `dataset`, `analysis`, `similarity`, `billing`, `notification`, `disclosure`, `admin`, `common` (shared: tenant context, security, errors, jobs).

Modules communicate **in-process via service interfaces only** (no module reaches into another's tables). This keeps a clean seam for future extraction.

## 3. Repository structure (monorepo)

```
credresearch/
├── docs/                      # this package
├── apps/
│   ├── web/                   # Next.js frontend
│   └── ...
├── services/
│   ├── backend/               # Spring Boot modular monolith
│   │   ├── src/main/java/africa/credresearch/...
│   │   ├── src/main/resources/db/migration/  # Flyway V__*.sql
│   │   └── build.gradle
│   └── ai-worker/             # FastAPI
│       ├── app/
│       │   ├── routers/  services/  llm/  rag/  export/  parsing/
│       │   └── main.py
│       └── pyproject.toml
├── packages/
│   ├── api-contract/          # openapi.yaml + generated TS client
│   └── ui/                    # shared UI tokens (optional)
├── infra/
│   ├── docker-compose.yml     # local
│   ├── docker-compose.prod.yml
│   └── nginx/
└── .github/workflows/         # CI/CD
```

## 4. API conventions

- REST, JSON, versioned under `/api/v1`.
- **Contract-first:** `packages/api-contract/openapi.yaml` (OpenAPI 3.1) is the source of truth; the TS client and request/response DTOs are generated from it.
- Auth: `Authorization: Bearer <access_token>`.
- Pagination: cursor-based (`?cursor=&limit=`) on large collections; `limit` default 20, max 100.
- Filtering/sorting via explicit query params; no arbitrary query languages.
- Errors: RFC 7807 `application/problem+json`:
  ```json
  { "type": "https://credresearch/errors/limit-exceeded", "title": "AI credit limit reached",
    "status": 402, "detail": "Upgrade to Student Pro to continue.", "instance": "/api/v1/ai/topics",
    "code": "AI_CREDITS_EXCEEDED" }
  ```
- Idempotency: mutating endpoints that can be retried accept `Idempotency-Key`; webhooks are idempotent by provider event id.
- Correlation: every request carries/propagates `X-Request-Id`.

## 5. Authentication & authorization

- JWT access token (15 min) signed RS256; claims: `sub`, `institution_id`, `roles`, `plan`, `exp`.
- Refresh token (30 days) opaque, stored **hashed** in `refresh_tokens`, rotated on use, revocable.
- `TenantContext` derived from the token's `institution_id` on every request; injected into JPA filters.
- Method-level `@PreAuthorize` on controllers; permission checks via RBAC tables.
- Magic-link review tokens are scoped, single-purpose, expiring JWTs (see [Security](./SECURITY_AND_COMPLIANCE.md)).

## 6. Canonical document model

- A `document` belongs to a project and is created from a `template`.
- A `document` has ordered `document_sections`. Each section's `content` is **Tiptap/ProseMirror JSON** (`jsonb`).
- Section edits create `document_versions` (immutable snapshots) and bump the section `version` integer (optimistic lock).
- Format rules live on the template (`document_format_rules`) and are applied at export.
- In-text citations are ProseMirror marks referencing `citations.id`; the reference list is rendered from citations via CSL at export time.

Why JSON over HTML: structured, queryable, safely transformable to DOCX/PDF, and supports inline citation marks. See [ADR-007](./ENGINEERING_DECISIONS.md).

## 7. Service-to-service (Java ↔ FastAPI)

- Phase 1: synchronous HTTPS. Backend calls the worker with a **signed internal JWT** (short-lived, audience `ai-worker`, shared signing key) — the worker rejects unsigned/expired calls.
- Phase 2: queue-based. Backend enqueues jobs (Redis list/stream → later RabbitMQ); worker consumes, writes results, and the backend polls/notifies. `ai_jobs` tracks status/attempts/result ref.
- The worker never holds end-user JWTs; it receives only the minimum task payload (with PII redacted where possible).

## 8. Background jobs

- Generic `ai_jobs` / `analysis_jobs` rows: `PENDING → RUNNING → SUCCEEDED | FAILED` with `attempts`, `error`, `payload_ref`, `result_ref`.
- Coordination via Redis at MVP (lists/streams + a single worker consumer); RabbitMQ later under load.
- Clients poll `GET /api/v1/jobs/{id}`; optional SSE push for progress.
- Idempotent and retry-safe; poison messages dead-lettered after N attempts.

## 9. Caching & rate limiting

- Redis caches: LLM responses (keyed by prompt hash + model), embeddings (keyed by content hash), reference renderings, hot reads.
- Rate limiting: token-bucket per user + per IP in Redis; AI endpoints additionally gated by plan credits.

## 10. Storage & files

- Uploads go to object storage via backend-issued **signed PUT URLs**; the `files` table stores key, bucket, content type, checksum, owner, tenant.
- Downloads via signed GET URLs (short TTL). The API never streams large binaries itself (bandwidth).
- Bucket versioning on; lifecycle rules expire orphaned uploads and stale exports.

## 11. Search

- PostgreSQL full-text search (`tsvector` + GIN) for documents, papers, projects at MVP.
- pgvector (HNSW) for semantic search / RAG over paper chunks.
- OpenSearch/Meilisearch deferred until FTS proves insufficient. See [ADR](./ENGINEERING_DECISIONS.md).

## 12. Frontend conventions

- App Router; server components for read-heavy pages, client components for the editor.
- TanStack Query owns server state (caching, retries, optimistic updates); Zustand only for ephemeral UI state (editor toolbar, modals).
- Generated typed API client from `openapi.yaml`.
- Tiptap editor configured with the citation mark extension and an autosave hook (debounced, offline-buffered).
- PWA: service worker caches the app shell; route-level code splitting; small initial bundle.

## 13. Configuration & environments

- Twelve-factor config via env vars; `.env` for local, secrets manager later.
- Environments: `local` (Docker Compose) → `staging` → `production`.
- Flyway runs migrations on backend startup (and as a CI gate). No manual schema edits anywhere.

## 14. Error handling & resilience

- Worker calls wrapped with timeouts, retries (bounded), and circuit-breaking; backend degrades gracefully if the worker is unavailable (editing/review/export still work).
- All unhandled errors → Sentry with correlation id; user sees a generic problem+json, never a stack trace.

## 15. Versioning & compatibility

- API versioned by URL (`/api/v1`); additive changes are non-breaking; breaking changes bump the version.
- Document model migrations (ProseMirror schema) are versioned; old documents migrated lazily on open.
