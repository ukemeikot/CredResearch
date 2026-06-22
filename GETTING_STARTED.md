# Getting Started — CredResearch

A multi-tenant academic-research SaaS. Monorepo with three deployables and supporting infra.
Architecture: **Clean Architecture inside a modular monolith** (backend) + a Python AI/data worker.
See `docs/` for the full spec; per-layer rules live in each service's `ARCHITECTURE.md`.

## 1. Repository layout

```
academic-research-saas/
├── apps/web/                 Next.js (App Router) PWA — feature-sliced + layered
├── services/backend/         Spring Boot modular monolith (Java 21) — clean arch per module
├── services/ai-worker/       FastAPI worker — clean arch (AI/RAG/parse/export)
├── packages/api-contract/    openapi.yaml (source of truth) + generated TS client
├── packages/ui/              shared UI tokens
├── infra/                    docker-compose(.prod).yml + nginx
├── .github/workflows/        CI/CD
└── docs/                     product + technical documentation
```

## 2. Prerequisites

| Tool | Version | For |
|---|---|---|
| Docker + Docker Compose | latest | everything (recommended path) |
| JDK | 21 | backend |
| Node.js | 20 LTS + pnpm | web / packages |
| Python | 3.11+ | ai-worker |
| OpenSSL | any | generating JWT keys |

## 3. One-time setup

```bash
cp .env.example .env        # then fill in secrets (LLM_API_KEY, payment sandbox keys)
```

## 4. Generate RS256 JWT keys (backend access tokens)

```bash
openssl genpkey -algorithm RSA -out jwt_private.pem -pkeyopt rsa_keygen_bits:2048
openssl rsa -in jwt_private.pem -pubout -out jwt_public.pem
# Paste the PEM contents into JWT_PRIVATE_KEY / JWT_PUBLIC_KEY in .env (or mount the files).
```

## 5. Run the stack with Docker (recommended)

The Phase-0 compose runs the **two application deployables** plus the datastores they need:
`backend`, `ai-worker`, `postgres` (pgvector), `redis`. (Web, MinIO, Gotenberg, MailHog are added in
later phases — see `docs/DEPLOYMENT_AND_INFRASTRUCTURE.md`.)

```bash
# from the repo root
docker compose -f infra/docker-compose.yml up --build -d     # build + start detached
docker compose -f infra/docker-compose.yml ps                # see health
docker compose -f infra/docker-compose.yml logs -f backend   # follow a service
docker compose -f infra/docker-compose.yml down              # stop  (add -v to wipe the DB volume)
```

| Service | URL | Notes |
|---|---|---|
| Backend API | http://localhost:18080/api/v1 | host 18080 → container 8080 (8080 was already taken on this machine) |
| Backend health | http://localhost:18080/actuator/health | |
| AI worker health | http://localhost:8001/health | |
| Postgres | localhost:5432 | credresearch / devpass |
| Redis | localhost:6379 | |

Flyway runs migrations automatically on backend startup (`V1__extensions.sql` → enables `vector` + `citext`).

### Smoke-test the two deployables

```bash
curl http://localhost:18080/actuator/health   # {"status":"UP", ...}
curl http://localhost:18080/api/v1/ping        # {"service":"backend","status":"ok"}
curl http://localhost:8001/health              # {"status":"ok","service":"ai-worker"}
```

## 6. Run a single service for development

**Backend**
```bash
cd services/backend
./gradlew bootRun        # needs postgres + redis from compose running
```

**AI worker**
```bash
cd services/ai-worker
python -m venv .venv && source .venv/bin/activate   # Windows: .venv\Scripts\activate
pip install -e .
uvicorn app.main:app --reload --port 8001
```

**Web**
```bash
cd apps/web
pnpm install
pnpm dev                 # http://localhost:3000
```

## 7. Contract-first workflow

The API is defined in `packages/api-contract/openapi.yaml` **before** the UI consumes it.
Regenerate the typed TS client into `packages/api-contract/generated` whenever the spec changes,
and the backend DTOs/controllers conform to the same contract.

## 8. Build order (per IMPLEMENTATION_PLAN.md)

Phase 0 (foundation: compose + CI + health) → 1 (auth/RBAC/tenancy) → 2 (projects) →
3 (template/document editor) → 4 (AI assistant + alignment + disclosure ledger) →
5 (papers/citations/RAG) → 6 (supervisor review + magic-link) → 10-MVP (export/billing/admin) →
integration/UAT. Build vertical slices (frontend → backend → worker → DB), test as you go;
the two non-negotiable gates from day one are **tenant isolation** and **AI guardrails**.

## 9. Tests

```bash
# backend: unit + integration (Testcontainers)
cd services/backend && ./gradlew test
# worker
cd services/ai-worker && pytest
# web unit + e2e (Playwright)
cd apps/web && pnpm test && pnpm e2e
```
