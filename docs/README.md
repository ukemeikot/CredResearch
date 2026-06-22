# CredResearch — Documentation Index

> **CredResearch** (working name) is an AI-powered academic research workflow and supervision platform for African universities and researchers. It takes a user from research idea → structured, supervisor-reviewed, citation-supported, formatted academic document. It is **not** an "AI thesis writer" — it is a research workflow, supervision, formatting, literature, citation, originality pre-check, and research-quality platform with a built-in **AI-Use Disclosure & Academic Integrity Ledger**.

This folder is the complete product + technical documentation package for the MVP and its near-term roadmap. It is written to be detailed enough for an engineering team to begin implementation.

## Suggested reading order

| # | Document | What it covers |
|---|----------|----------------|
| 1 | [PRD.md](./PRD.md) | Vision, problem, users, value, scope, success metrics |
| 2 | [FUNCTIONAL_REQUIREMENTS.md](./FUNCTIONAL_REQUIREMENTS.md) | Feature-by-feature behaviour, per module, MVP vs post-MVP |
| 3 | [NON_FUNCTIONAL_REQUIREMENTS.md](./NON_FUNCTIONAL_REQUIREMENTS.md) | Performance, availability, low-bandwidth, a11y, i18n, compliance targets |
| 4 | [TECHNICAL_SPECIFICATION.md](./TECHNICAL_SPECIFICATION.md) | Stack, repo structure, conventions, contracts, document model |
| 5 | [SYSTEM_ARCHITECTURE.md](./SYSTEM_ARCHITECTURE.md) | System diagram + all lifecycles + deployment topologies |
| 6 | [ERD.md](./ERD.md) | Mermaid ERD + entity-by-entity explanation |
| 7 | [DATABASE_SCHEMA.md](./DATABASE_SCHEMA.md) | Tables, columns, types, indexes, Flyway/migration conventions |
| 8 | [API_SPECIFICATION.md](./API_SPECIFICATION.md) | REST groups, endpoints, payloads, roles, rate limits |
| 9 | [AI_SYSTEM_DESIGN.md](./AI_SYSTEM_DESIGN.md) | Worker, RAG, embeddings, guardrails, JSON contracts, ledger |
| 10 | [SECURITY_AND_COMPLIANCE.md](./SECURITY_AND_COMPLIANCE.md) | AuthN/Z, tenant isolation, privacy, NDPA, payments, DR |
| 11 | [TEST_STRATEGY.md](./TEST_STRATEGY.md) | Unit, integration, contract, e2e, AI eval/golden tests |
| 12 | [MVP_ROADMAP.md](./MVP_ROADMAP.md) | Phase 0–10 build plan, MVP boundary, sequencing |
| 13 | [IMPLEMENTATION_PLAN.md](./IMPLEMENTATION_PLAN.md) | Per-phase build + test + acceptance playbook; golden journey that proves the goal |
| 14 | [DEPLOYMENT_AND_INFRASTRUCTURE.md](./DEPLOYMENT_AND_INFRASTRUCTURE.md) | Docker Compose, VPS, Nginx/SSL, CI/CD, backups, env vars |
| 15 | [ENGINEERING_DECISIONS.md](./ENGINEERING_DECISIONS.md) | ADRs with context, options, consequences, migration paths |

## MVP in one paragraph

A student creates a research project, picks an institution/department template, generates or refines a topic/proposal, builds Chapter 1–3 in a structured editor, uploads academic papers, generates a literature matrix, manages citations, invites a supervisor (account or magic-link), receives inline review comments and decisions, revises, sees a transparent AI-use disclosure for the document, and exports a formatted DOCX/PDF — on a low-end Android device over an intermittent connection, paying in Naira via Paystack/Flutterwave.

## MVP boundary

- **In MVP:** Phases 0–6 + the AI-Use Disclosure Ledger (Module 13) + the export/payment/admin slices of Phase 10.
- **MVP-plus (capacity permitting):** Questionnaire builder (7), Basic data analysis (8), Similarity pre-check (9).
- **Deferred post-MVP:** Kubernetes, native mobile, full offline, ERP integrations, full SPSS/Turnitin replacement, real-time collaborative editing.

## Conventions used across docs

- **Roles:** `STUDENT`, `SUPERVISOR`, `CONSULTANT`, `DEPARTMENT_ADMIN`, `INSTITUTION_ADMIN`, `PLATFORM_ADMIN`.
- **Tenant key:** every tenant-scoped row carries `institution_id` (nullable for independent consultants → a synthetic personal tenant).
- **IDs:** UUID v7 (time-ordered) primary keys.
- **Money:** integer minor units + ISO-4217 `currency`; never floats.
- **Time:** UTC, ISO-8601, `timestamptz`.
- **Requirement IDs:** `FR-<MODULE>-<n>` (functional), `NFR-<CATEGORY>-<n>` (non-functional), `ADR-<n>` (decisions).

## Document generation checklist

- [x] README.md (this file)
- [x] PRD.md
- [x] FUNCTIONAL_REQUIREMENTS.md
- [x] NON_FUNCTIONAL_REQUIREMENTS.md
- [x] TECHNICAL_SPECIFICATION.md
- [x] SYSTEM_ARCHITECTURE.md
- [x] ERD.md
- [x] DATABASE_SCHEMA.md
- [x] API_SPECIFICATION.md
- [x] AI_SYSTEM_DESIGN.md
- [x] SECURITY_AND_COMPLIANCE.md
- [x] TEST_STRATEGY.md
- [x] MVP_ROADMAP.md
- [x] IMPLEMENTATION_PLAN.md
- [x] DEPLOYMENT_AND_INFRASTRUCTURE.md
- [x] ENGINEERING_DECISIONS.md
