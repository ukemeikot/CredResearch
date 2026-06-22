# CredResearch

**An AI-powered academic research workflow and supervision platform for African universities and researchers.**

CredResearch takes a user from a research **idea → a structured, supervisor-reviewed, citation-supported, formatted academic document** — with a transparent record of how AI was used — working on a low-end Android device over an intermittent connection, paying in Naira.

It is **not** an "AI thesis writer." AI **assists** (guard-railed and credited); it does not ghostwrite. Every AI interaction is captured in a built-in, tamper-evident **AI-Use Disclosure & Academic Integrity Ledger** — turning the biggest integrity risk of generative AI into an institutional selling point.

## The problem

African students and researchers face fragmented tooling, limited supervision bandwidth, inconsistent formatting/citation standards, and patchy connectivity — while institutions worry that generative AI makes ghostwriting trivial and undetectable. CredResearch is a single workflow that supports *real* research while keeping integrity visible and enforceable.

## What it does

- **Project workspace** — research projects with milestones, members (incl. co-supervisors), and an activity feed.
- **Structured authoring** — proposal + Chapters 1–3 in a Tiptap/ProseMirror editor with offline-buffered autosave, version history, and institution/department templates.
- **AI research assistant** — topic/feasibility/outline/objectives/methodology help, structured and guard-railed, metered by plan credits.
- **Research Alignment Engine** — checks coherence across topic → objectives → methodology and flags misalignment.
- **Literature & citations** — upload papers, auto-summarize, build a literature matrix, manage citations, and render reference lists (APA/IEEE/Harvard via CSL), grounded by RAG that cites only retrieved sources.
- **Supervisor review** — submit → inline comments → decision → revise loop, including for an account-less external supervisor via a scoped **magic-link**.
- **AI-Use Disclosure Ledger** — hash-chained, append-only record of every AI accept/edit/reject, exportable as a disclosure statement.
- **Export** — DOCX + PDF + reference list + disclosure statement, bundled as a ZIP.
- **Billing & admin** — multi-currency checkout (Paystack/Flutterwave), usage metering, and an operator dashboard.

See [docs/PRD.md](docs/PRD.md) for the full vision and [docs/FUNCTIONAL_REQUIREMENTS.md](docs/FUNCTIONAL_REQUIREMENTS.md) for feature-by-feature behaviour.

## Architecture

**Clean Architecture inside a modular monolith** (one Java deployable) plus a **separate Python AI/data worker** — two services, not microservices ([ADR-002](docs/ENGINEERING_DECISIONS.md), [ADR-003](docs/ENGINEERING_DECISIONS.md)). Each backend feature module is a clean boundary (`domain → application → infrastructure → interfaces`, dependencies pointing inward), so a module can be extracted into its own service later without a rewrite.

| Layer | Technology |
|---|---|
| Frontend | Next.js (App Router), TypeScript, Tailwind, shadcn/ui, Tiptap, TanStack Query |
| Backend (core) | Java 21, Spring Boot, Spring Security, Spring Data JPA, Flyway |
| AI/data worker | Python, FastAPI |
| Database | PostgreSQL + pgvector, full-text search |
| Cache/queue | Redis |
| Object storage | S3-compatible (MinIO dev; R2/Spaces/S3 prod) |
| Export | python-docx + Gotenberg/LibreOffice |

Details: [docs/SYSTEM_ARCHITECTURE.md](docs/SYSTEM_ARCHITECTURE.md) · [docs/TECHNICAL_SPECIFICATION.md](docs/TECHNICAL_SPECIFICATION.md).

## Repository layout

```
academic-research-saas/
├── apps/web/                 Next.js (App Router) PWA — feature-sliced + layered
├── services/backend/         Spring Boot modular monolith (Java 21) — clean arch per module
├── services/ai-worker/       FastAPI worker — clean arch (AI/RAG/parse/export)
├── packages/api-contract/    @credresearch/api-client — OpenAPI 3.1 + generated TS types
├── packages/ui/              shared UI tokens
├── infra/                    docker-compose(.prod).yml + nginx
├── .github/workflows/        path-filtered CI (backend, ai-worker, web, contract)
└── docs/                     product + technical documentation
```

## Quick start

```bash
cp .env.example .env                                     # fill in secrets
docker compose -f infra/docker-compose.yml up --build -d # backend, ai-worker, postgres, redis
```

Smoke-test the two deployables:

```bash
curl http://localhost:18080/actuator/health   # backend  -> {"status":"UP", ...}
curl http://localhost:18080/api/v1/ping        # backend  -> {"service":"backend","status":"ok"}
curl http://localhost:8001/health              # ai-worker-> {"status":"ok","service":"ai-worker"}
```

Full setup (JWT keys, single-service dev, tests): [GETTING_STARTED.md](GETTING_STARTED.md).

## Documentation

Start at the [documentation index](docs/README.md). Key entry points: [PRD](docs/PRD.md) · [Functional Requirements](docs/FUNCTIONAL_REQUIREMENTS.md) · [System Architecture](docs/SYSTEM_ARCHITECTURE.md) · [Implementation Plan](docs/IMPLEMENTATION_PLAN.md) · [Engineering Decisions](docs/ENGINEERING_DECISIONS.md) · [Repo Strategy](docs/REPO_STRATEGY.md).

## Project status

**Phase 0 (foundation) complete** — monorepo scaffold, runnable backend + AI worker on Docker, versioned API contract, and CI in place. Next: Phase 1 (auth, RBAC, multi-tenancy). Build order and the end-to-end "golden journey" acceptance test are in the [Implementation Plan](docs/IMPLEMENTATION_PLAN.md); changes are tracked in [CHANGELOG.md](CHANGELOG.md).

## Contributing

Conventional Commits, branch-per-change, changelog discipline, and protected `main` — see [CONTRIBUTING.md](CONTRIBUTING.md).
