# Test Strategy — CredResearch

Related: [Technical Specification](./TECHNICAL_SPECIFICATION.md) · [AI System Design](./AI_SYSTEM_DESIGN.md) · [Non-Functional Requirements](./NON_FUNCTIONAL_REQUIREMENTS.md)

## 1. Goals & test pyramid

Fast feedback, high confidence on critical paths, and explicit coverage of the two riskiest areas: **tenant isolation** and **AI guardrails**.

```
        e2e (Playwright)            few, critical journeys
     contract + integration         API + DB (Testcontainers), Java↔FastAPI contract
   unit (JUnit / pytest / vitest)    many, fast, isolated
```

## 2. Backend (Java / Spring Boot)

- **Unit:** JUnit 5 + Mockito on domain/service logic (alignment scoring aggregation, credit accounting, status transitions, optimistic-lock conflict handling).
- **Integration:** `@SpringBootTest` with **Testcontainers** (real PostgreSQL + pgvector + Redis) — repositories, JPA tenant filters, Flyway migrations apply cleanly, signed-URL issuance.
- **Web layer:** `MockMvc`/`WebTestClient` for controller auth, RBAC `@PreAuthorize`, validation, and problem+json error shapes.
- **Migration tests:** every PR runs Flyway against a fresh container; migrations are forward-only and idempotent where backfilling.

## 3. AI/Data worker (Python / FastAPI)

- **Unit:** pytest on parsing, chunking, CSL rendering, DOCX rendering, statistics (deterministic, no LLM).
- **Gateway tests:** routing, caching (hit/miss), token budgeting, retry/fallback — LLM provider mocked.
- **Schema validation:** every feature's output validated against its JSON schema; repair-retry path covered.

## 4. AI evaluation / golden tests *(critical)*

Prompts are product surface area; regressions are silent and dangerous, so they are gated in CI.

- A versioned **eval set** per `feature_key`: representative inputs + rubric assertions.
- Assertions per case:
  - Output is valid JSON against the declared schema.
  - **Guardrails:** no fabricated citations (every reference resolves to a known source); no invented numbers in data interpretation; ghostwriting requests are refused/reframed.
  - **Alignment engine:** seeded misaligned fixtures must be flagged; aligned fixtures must score high.
  - RAG answers cite only retrieved chunks; "insufficient context" returned when appropriate.
- Runs on any change to prompts (`ai_prompt_templates`), schemas, or gateway routing. Failing evals block merge.
- Provider calls in CI use a recorded/fixture mode or a cheap model with tolerance bands to control cost and flakiness.

## 5. Contract tests (Java ↔ FastAPI)

- The internal task contract is schema-pinned (JSON Schema/Pydantic ↔ Java DTO). Contract tests assert both sides agree; a breaking change on one side fails the other's build.
- The public API contract (`openapi.yaml`) is validated in CI; the generated TS client compiling against it is itself a contract check.

## 6. Frontend (Next.js / TypeScript)

- **Unit/component:** Vitest + React Testing Library (editor citation mark, autosave hook with offline buffering, forms, guards).
- **Type safety:** generated API client types must compile against `openapi.yaml`.
- **a11y:** axe checks on key pages; keyboard-navigation tests for the editor and review flows.

## 7. End-to-end (Playwright)

Critical journeys, run against a Docker Compose stack:
1. Register → verify → create project → pick template → build Ch1 sections (autosave + version).
2. AI: generate objectives → accept/edit → confirm **disclosure ledger** entry created.
3. Upload paper → ingest → literature matrix → add citation → render reference list.
4. Invite supervisor (magic-link) → submit section → comment → decision → revise.
5. Export DOCX/PDF/ZIP with disclosure statement attached.
6. Billing: checkout (sandbox) → webhook (signed) → subscription active → usage limit enforced.

## 8. Tenant-isolation test suite *(critical)*

A dedicated suite seeds two tenants and asserts, for **every** resource type, that tenant A cannot list/read/update/delete/export tenant B's data via any endpoint or signed URL. This suite must pass on every PR.

## 9. Security & abuse tests

- AuthN/Z: token expiry/rotation/revocation; magic-link token scope (cannot act outside its review).
- Rate limiting and credit gating return correct 429/402.
- Webhook signature verification rejects forged events; replay is idempotent.
- Input fuzzing on upload and editor content; ProseMirror schema rejects disallowed nodes.

## 10. Performance & load

- k6/Gatling against key endpoints to validate NFR latency budgets.
- Soak test paper ingestion + AI jobs under queue depth to validate worker scaling and DLQ behaviour.
- Lighthouse CI for the editor route on throttled (3G, mid-tier mobile) profiles against NFR-PERF-7/8.

## 11. Data quality / extraction tests

- A corpus of varied PDFs (clean, scanned, multi-column, non-English) asserts extraction quality flags fire and OCR fallback engages.

## 12. CI gates (GitHub Actions)

Per PR: lint + typecheck → unit → integration (Testcontainers) → contract → AI evals → build images. Per merge to main: e2e on Compose + deploy to staging. Release to prod is manual-approval after staging checks. Coverage thresholds enforced (service/domain ≥ 70%); critical suites (tenant isolation, AI guardrails) are non-negotiable gates.

## 13. Test data & environments

- Deterministic seed factory (institutions, users per role, templates, plans, demo project).
- Ephemeral DB per integration run (Testcontainers); no shared mutable test DB.
- Secrets in CI via masked environment; provider sandboxes for Paystack/Flutterwave.
