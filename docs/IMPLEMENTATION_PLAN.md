# Implementation Plan — CredResearch

The build-and-test playbook. Where [MVP_ROADMAP](./MVP_ROADMAP.md) says *what order*, this says *what to build, what to test, and what "done" means* for each phase — so the slices add up to the product goal.

Related: [MVP Roadmap](./MVP_ROADMAP.md) · [Functional Requirements](./FUNCTIONAL_REQUIREMENTS.md) · [Test Strategy](./TEST_STRATEGY.md) · [PRD](./PRD.md)

---

## 1. Product goal (the thing every phase must serve)

> A student moves from **idea → structured, supervisor-reviewed, citation-supported, formatted academic document**, with a **transparent record of how AI was used**, on a **low-end Android device over an intermittent connection**, paying in **Naira** — and an institution can onboard, standardize, supervise, and bill for this.

A phase is only "done" when it moves a real user measurably closer to that sentence. The **Final Integration** section (§5) is the explicit checkpoint where the slices are proven to come together.

## 2. How to use this document

- Each phase has: **Goal → Build (by workstream) → Data/Migrations → Deliverables → Test plan → Definition of Done → Demo**.
- **Definition of Ready (DoR)** for starting any phase: dependencies merged, API contract slice drafted in `openapi.yaml`, test fixtures identified, acceptance criteria agreed.
- **Definition of Done (DoD), global** (every phase, on top of phase-specific criteria):
  - Code merged behind passing CI (lint, unit, integration, contract, relevant evals).
  - API slice reflected in `openapi.yaml`; TS client regenerated and compiling.
  - Flyway migrations apply forward-only on a clean DB in CI.
  - New endpoints tenant-scoped + `@PreAuthorize`'d; **tenant-isolation suite still green**.
  - Sentry instrumented; structured logs with request IDs.
  - Docs updated; demo script runs on the Compose stack.
- **Build in vertical slices:** each phase ships a thin path through frontend → backend → (worker) → DB that a user can actually exercise, not a horizontal layer.

## 3. Delivery principles

1. **Test-as-you-go.** Tests are written in the same PR as the feature, not "later." The two non-negotiable gates from day one: **tenant isolation** and **AI guardrails**.
2. **Contract-first.** The endpoint exists in `openapi.yaml` before the UI consumes it.
3. **Degrade gracefully.** If the AI worker is down, editing/review/export still work — verified per relevant phase.
4. **Bandwidth realism.** Every UI phase is checked on a throttled 3G / mid-tier mobile profile.
5. **Demoable weekly.** Every phase ends with a script you can show a real student/supervisor.

---

## 4. Phase-by-phase build & test plan

> Legend for test types: **U** unit · **I** integration (Testcontainers) · **C** contract (Java↔FastAPI / OpenAPI) · **E** e2e (Playwright) · **A** AI eval/golden · **S** security/abuse · **ISO** tenant isolation · **P** performance/a11y.

### Phase 0 — Foundation
**Goal:** a developer can `docker compose up` and every service answers a healthcheck; CI runs and gates merges. Nothing user-facing yet, but it de-risks everything after.

**Build**
- *Infra:* monorepo skeleton; `docker-compose.yml` (web, backend, ai-worker, postgres+pgvector, redis, minio, gotenberg, mailhog); Nginx config stub; `.env` template; GHCR registry.
- *Backend:* Spring Boot bootstrap; Flyway wired; Actuator `/health` `/ready`; Sentry; problem+json error handler; `TenantContext` + security filter scaffolding (no users yet).
- *Worker:* FastAPI bootstrap; `/health`; Sentry; **LLM gateway interface stubbed** (`complete`/`embed` returning fixtures); internal-JWT verification middleware.
- *Frontend:* Next.js App Router shell; Tailwind/shadcn; PWA manifest + service worker; generated-client pipeline from `openapi.yaml`; Sentry.
- *Shared:* `openapi.yaml` skeleton; notification-provider interface stubbed; CI workflow (lint → unit → integration → contract → AI-eval scaffold → build); seed factory skeleton; **Playwright + Testcontainers harness**; AI golden-test scaffold.

**Data/Migrations:** `V1__extensions.sql` (`vector`, `citext`), baseline audit columns convention documented.

**Deliverables:** running Compose stack; green CI pipeline; empty-but-wired isolation + AI-eval suites; contributor README.

**Test plan:** U (error handler, gateway stub) · I (Flyway applies; health endpoints) · C (OpenAPI lints; client compiles) · CI smoke that the stack boots.

**Definition of Done:** `docker compose up` → all healthchecks 200; CI blocks a deliberately failing test; internal JWT rejects an unsigned worker call.

**Demo:** "Everything is wired; here's the pipeline failing then passing."

---

### Phase 1 — Auth, Roles & Multi-Tenant Base
**Goal:** real accounts, real roles, hard tenant boundaries. *Serves the goal:* institutions/students can exist safely and separately — the substrate for everything.

**Build**
- *Backend:* register, email verify, login, refresh-token rotation, logout/logout-all (`FR-AUTH-1..5`); Argon2id; RBAC tables + `@PreAuthorize` (`FR-RBAC-1..2`); institutions, departments, profiles (`FR-ORG-1..6`); `institution_id` JPA filter on all queries (`FR-TEN-1`); bulk student CSV import; synthetic personal tenant; demo-institution seed; `audit_logs` writes (`FR-X-4`).
- *Frontend:* auth screens, email-verify, role-aware nav shell, institution/department admin onboarding, CSV import UI.
- *Notifications:* email verification + reset via stubbed provider (mailhog locally).

**Data/Migrations:** identity & org tables; roles/permissions/plans/global-templates **seed**.

**Deliverables:** working auth + RBAC + tenancy; onboarding flow; **populated tenant-isolation suite**.

**Test plan:** U (token rotation, Argon2id params, RBAC resolution) · I (login/refresh/revoke against real Postgres+Redis; CSV import; audit writes) · **ISO (the suite goes live: tenant A cannot touch tenant B across users/depts/profiles)** · S (brute-force throttle, refresh reuse detection, reset-token single-use) · C (auth endpoints in OpenAPI) · E (register→verify→login→logout).

**Definition of Done:** all `FR-AUTH/RBAC/ORG/TEN` MVP items pass; isolation suite green; a suspended user is denied; magic-link token scaffolding present.

**Demo:** institution admin onboards a department + imports students; a student logs in and sees only their tenant.

---

### Phase 2 — Project Workspace
**Goal:** a student can create and run a project shell. *Serves the goal:* the container that all research work and supervision attaches to.

**Build**
- *Backend:* projects + status lifecycle + history (`FR-PROJ-1,4`); members incl. co-supervisors (`FR-PROJ-2,3`); milestones + due reminders (`FR-PROJ-5`); activity feed (`FR-PROJ-6`); dashboard aggregation (`FR-PROJ-7`).
- *Frontend:* project create wizard, dashboard (progress, next milestone, pending reviews placeholder, alignment placeholder), members UI, milestone UI, activity feed.
- *Notifications:* milestone-due reminder via outbox.

**Data/Migrations:** projects, project_members, project_milestones, project_activities, project_status_history.

**Deliverables:** end-to-end project management (minus documents).

**Test plan:** U (status-transition rules; dashboard aggregation) · I (membership + role enforcement; activity logging; reminder enqueue) · ISO (projects scoped to tenant; non-member denied) · C · E (create project → add co-supervisor → add milestone → see feed) · P (dashboard on 3G budget).

**Definition of Done:** `FR-PROJ-*` MVP pass; only members access a project; reminders enqueue.

**Demo:** student spins up a project, adds two supervisors, sets a milestone, watches the feed update.

---

### Phase 3 — Template & Document Builder
**Goal:** reliable structured authoring of proposal + Ch1–3, even on a flaky connection. *Serves the goal:* this **is** the "structured, formatted document" half of the promise.

**Build**
- *Backend:* templates + sections + format rules (`FR-TMPL-1..3`, `FR-DOC-5`); document instantiation (`FR-DOC-1`); section CRUD with **optimistic locking** (`FR-DOC-3`); version history + restore (`FR-DOC-4`); FTS `tsvector` on sections.
- *Frontend:* template picker; **Tiptap editor** (ProseMirror JSON) (`FR-DOC-2`); **offline-buffered autosave** with conflict (409) resolution UX; version-history drawer/restore; section navigator.
- *Worker:* none required yet (export comes in Phase 10), but JSON→text flattening helper shared.

**Data/Migrations:** templates, template_sections, document_format_rules, documents, document_sections (`content jsonb`, `version`, `content_text`, `search_tsv`), document_versions.

**Deliverables:** a student can author Ch1–3 sections that never silently lose data.

**Test plan:** U (optimistic-lock conflict; ProseMirror schema validation/sanitization) · I (autosave version bump; restore; FTS query) · ISO (sections scoped) · S (editor content sanitized; disallowed nodes rejected) · E (author → drop connection → reconnect → no data loss → restore an older version) · **P (editor route initial JS < 350 KB gz; FCP < 3 s on 3G; autosave RTT < 400 ms)**.

**Definition of Done:** `FR-TMPL/DOC` MVP pass; **offline autosave proven** (network-throttle test); 409 conflicts resolve cleanly.

**Demo:** author a chapter on a throttled connection, kill the network mid-edit, reconnect, restore a prior version.

---

### Phase 4 — AI Research Assistant + Alignment + Disclosure Ledger
**Goal:** AI **assists** (structured, guard-railed, credited) and **every interaction is logged to the ledger**; the Alignment Engine renders. *Serves the goal:* the "AI-powered, not ghostwriting" differentiator and the integrity backbone.

**Build**
- *Worker:* real **LLM gateway** (routing cheap/strong, Redis caching, token budgeting, retry/fallback); prompt templates (versioned) for topics/feasibility/outline/objectives/methodology/problem-statement (`FR-AI-1..6`); **guardrail enforcement** in prompt + code (`FR-AI-7`); JSON-schema validation + repair-retry; **Research Alignment Engine** (deterministic + semantic) (`FR-ALIGN-1..4`); usage logging.
- *Backend:* `/ai/*` endpoints as **async jobs** (`FR-X-1`); per-plan **credit decrement** (Redis, atomic) (`FR-AI-8`); `ai_requests/responses/usage_logs/jobs/prompt_templates`; alignment report persistence; **disclosure-ledger write path** (hash-chained, append-only) capturing accept/edit/reject (`FR-LEDGER-1,2,5`); `/jobs/{id}` polling.
- *Frontend:* AI panels (clearly labelled "suggestion"); accept/edit/reject actions that write the ledger; job-progress UI; alignment report view + dashboard score/trend; credit meter + upgrade prompt.

**Data/Migrations:** ai_prompt_templates (+seed), ai_requests, ai_responses, ai_usage_logs, ai_jobs, research_alignment_reports, ai_disclosure_entries.

**Deliverables:** working assistant + alignment + a tamper-evident ledger.

**Test plan:** U (credit math; hash-chain integrity; alignment scoring aggregation) · I (job lifecycle PENDING→SUCCEEDED/FAILED; credit gate returns 402; ledger append-only + chain verifies) · **A (AI golden tests go live: valid JSON per schema; no fabricated citations; no invented data; ghostwriting refused; seeded-misaligned fixtures get flagged; aligned fixtures score high)** · C (Java↔worker task contract) · S (prompt-injection fixtures treated as data; injection cannot exfiltrate) · E (generate objectives → edit → confirm a ledger entry exists) · graceful-degradation I (worker down → editing/review still work).

**Definition of Done:** `FR-AI/ALIGN/LEDGER` MVP pass; **AI-eval gate enforced in CI**; every document-linked AI action produces a verifiable ledger entry; over-limit returns a clear upgrade path.

**Demo:** generate objectives, tweak them, run the alignment report, open the ledger and show the chained record.

---

### Phase 5 — Papers, Literature Review & Citations
**Goal:** uploads become summarized, matrixed, citable knowledge, with grounded RAG. *Serves the goal:* the "citation-supported, literature-backed" half.

**Build**
- *Backend:* signed-URL upload registration (`FR-LIT-1`); ingest job trigger; `files/papers/paper_chunks/paper_embeddings`; citations CRUD (`FR-LIT-6`); **CSL reference-list render** APA/IEEE/Harvard (`FR-LIT-7`); **BibTeX/RIS import/export** (`FR-LIT-9`); dedup (`FR-LIT-10`); literature-matrix job (`FR-LIT-5`).
- *Worker:* PDF/DOCX extraction + metadata + **OCR fallback + quality flag** (`FR-LIT-2,3`); chunk + **embed (pgvector HNSW)**; paper summarization (method/findings/limitations/gaps) (`FR-LIT-4`); **RAG** grounded to project corpus, cite-only-retrieved (`FR-LIT-8`); matrix synthesis; **citation-safety validator**.
- *Frontend:* paper library, upload UX, extraction-quality review prompt, summary view, literature-matrix table, citation manager, in-editor citation insertion (ProseMirror mark), reference-list preview.

**Data/Migrations:** files, papers, paper_chunks, paper_embeddings (HNSW index), citations (CSL-JSON), literature_matrix_entries.

**Deliverables:** full literature → matrix → citations → reference list, with RAG.

**Test plan:** U (CSL render per style; BibTeX/RIS round-trip; dedup hash) · I (signed-URL issuance; ingest job; embed+store; matrix persistence) · **A (RAG cites only retrieved chunks; "insufficient context" path; summary grounded — no invented findings; citation validator rejects unknown references)** · data-quality (varied PDFs: clean/scanned/multi-column/non-English → quality flags fire, OCR engages) · ISO (papers/citations tenant- & project-scoped; signed URLs per-object) · S (only allowed MIME types; signed-URL TTL) · E (upload → ingest → summary → add citation → render APA list) · P (ingestion async, UI non-blocking).

**Definition of Done:** `FR-LIT-*` MVP pass; **no citation reaches export unless it resolves to a stored source**; RAG never cites outside the corpus.

**Demo:** upload two papers, generate the matrix, insert a citation into Chapter 2, render the reference list in IEEE then APA.

---

### Phase 6 — Supervisor Review & Collaboration (incl. magic-link)
**Goal:** a complete submit → review → decide → revise loop, including for an **account-less external supervisor**. *Serves the goal:* the "supervisor-reviewed" half and the key adoption lever.

**Build**
- *Backend:* invite supervisor; existing-user link vs **magic-link invitation** (`FR-SUP-1,2`); **scoped review tokens** (audience `review`, single `review_request_id`); review requests (`FR-SUP-3`); inline comments anchored to ranges (`FR-SUP-4`); decisions APPROVED/NEEDS_REVISION/REJECTED (`FR-SUP-5`); history + resolvable threads (`FR-SUP-6`); supervisor inbox (`FR-SUP-7`); revision workflow (`FR-SUP-8`); notifications on every transition (`FR-SUP-9`).
- *Frontend:* submit-for-review; supervisor inbox/dashboard; **magic-link review surface** (token-auth, no full account); inline comment UI; decision UI; revision view preserving prior decisions.
- *Notifications:* invite/submission/decision/comment via outbox (email/SMS).

**Data/Migrations:** review_requests, review_comments, review_decisions, notifications, notification_outbox; invitations (magic-link) finalized.

**Deliverables:** the full review cycle, account-required and magic-link.

**Test plan:** U (decision state machine; comment anchoring) · I (invite→link/accept; resubmission preserves history; outbox fan-out) · **S (review token cannot act outside its one review; expiry; cannot escalate to account scope)** · ISO (reviews scoped; magic-link supervisor sees only the shared section) · C · E (**two-actor journey:** student invites external supervisor by email → magic-link → comment → NEEDS_REVISION → student revises → resubmit → APPROVED).

**Definition of Done:** `FR-SUP-*` MVP pass; **external supervisor reviews with zero onboarding**; magic-link scope proven airtight by security tests.

**Demo:** invite a supervisor who has no account; they open the link, comment, request revision; student revises; approved.

> ✅ **End of Phase 6 + Phase 4 ledger + the Phase 10 export/billing/admin slice = shippable MVP.**

---

### Phase 10 (MVP slice) — Export, Billing & Production Hardening
> Pulled forward into the MVP because export + payment + admin are required to *realize* the goal and to charge for it.

**Goal:** the document leaves the building as DOCX/PDF **with a disclosure statement**, users can pay in Naira, and operators can run the platform.

**Build**
- *Worker:* **ProseMirror JSON → DOCX** (python-docx) applying format rules + CSL reference list (`FR-DOC-6`); **DOCX/HTML → PDF** via Gotenberg (`FR-DOC-7`); **ZIP bundle** (DOCX+PDF+references+**disclosure statement PDF**) (`FR-DOC-8`, `FR-LEDGER-3`).
- *Backend:* export jobs + signed download URLs; **billing**: plans/subscriptions/usage_limits (`FR-BILL-1,2`); **Redis usage metering** soft-warn/hard-block (`FR-BILL-3`); **Paystack/Flutterwave checkout**, multi-currency (`FR-BILL-4`); **signature-verified, idempotent webhooks** + `webhook_events` (`FR-BILL-5`); manual-invoice path (`FR-BILL-6`); subscription lifecycle + grace (`FR-BILL-7`); receipts (`FR-BILL-8`); **admin dashboard** (users/institutions/templates/payments/usage+AI-cost/stats/plans/flags) (`FR-ADM-1..7`); disclosure-statement endpoint (`FR-LEDGER-3,4`).
- *Frontend:* export dialog + progress + download; pricing/checkout; subscription & usage screens; admin dashboards; disclosure-statement download.

**Data/Migrations:** plans/subscriptions/payments/usage_limits/usage_events/webhook_events (+plan seed); feature_flags.

**Deliverables:** monetizable, operable, exportable MVP.

**Test plan:** U (DOCX render fidelity; usage-limit math; currency formatting) · I (export job → signed URL; **webhook signature verify + idempotent replay**; limit hard-block returns 402; subscription state from verified events) · **S (forged webhook rejected; replay is idempotent; export signed-URL scope/TTL)** · ISO (exports/billing tenant-scoped) · A (disclosure statement reflects the true ledger) · C · E (**money path:** checkout sandbox → signed webhook → subscription active → limit enforced; **export path:** export ZIP → open DOCX/PDF → disclosure statement present) · P (DOCX export p95 < 8 s).

**Definition of Done:** `FR-DOC-6..8`, `FR-BILL-*`, `FR-ADM-*`, `FR-LEDGER-3,4` pass; a paid user exports a formatted document **with disclosure statement**; webhooks are forgery- and replay-proof.

**Demo:** subscribe in sandbox, hit a usage limit, upgrade, export the full ZIP, open the disclosure statement.

---

### Phases 7–9 (MVP-plus) — condensed plan
Each follows the same template; ship only if capacity allows.

| Phase | Build core | Key tests | DoD |
|---|---|---|---|
| **7 Questionnaire** (`FR-Q-*`) | generate-from-objectives; question types; consent; tokenized public link; response collection; CSV export | I (public link tokens; consent capture); S (public endpoint rate-limit/bot-mitigation); E (build→publish→respond→export); ISO | A student fields a survey publicly and exports responses |
| **8 Data analysis** (`FR-DATA-*`) | CSV upload; type/missing detection; **descriptive stats in pandas**; charts; **grounded AI interpretation**; Chapter 4 starter | U (stats correctness); **A (interpretation introduces no numbers not in data)**; I (upload→analyze→chart); E | Descriptive results + a grounded Ch4 draft, zero invented figures |
| **9 Similarity** (`FR-SIM-*`) | internal check vs own docs + opt-in repo; repeated-paragraph + citation-risk; report (**not Turnitin**) | U (match scoring); I (check job→report); ISO (repo opt-in scoping); E | An honest internal similarity report with clear non-Turnitin framing |

---

## 5. Final Integration & Product-Goal Acceptance
This is the explicit checkpoint where the slices must **come together** into the product goal — not a phase that adds features, but one that proves the whole.

### 5.1 The golden end-to-end journey (must pass, automated in Playwright)
1. Institution admin onboards a department and imports students.
2. Student registers, verifies, logs in (correct tenant only).
3. Creates a project from the department's template; adds a co-supervisor.
4. Authors Chapter 1 in the Tiptap editor; survives a simulated connection drop; restores a version.
5. Uses the AI assistant to draft objectives; edits two; **a ledger entry is recorded per interaction**.
6. Runs the **Alignment Engine**; resolves a flagged misalignment; score improves.
7. Uploads two papers; generates the **literature matrix**; inserts citations; renders an APA reference list.
8. Invites an **external supervisor by email**; supervisor uses the **magic-link**, comments, requests revision.
9. Student revises and resubmits; supervisor **approves**.
10. Student **subscribes** (sandbox) in NGN; hits/clears a usage limit.
11. **Exports** the ZIP (DOCX + PDF + references + **AI-Use Disclosure Statement**).
12. Admin sees the project in stats and the AI-cost dashboard.

Passing this journey on a throttled mobile profile **is** the acceptance test for "the product works."

### 5.2 System acceptance gates (all green before launch)
- **Functional:** every MVP `FR-*` mapped to a passing test (see §6 traceability).
- **Isolation:** tenant-isolation suite green across every resource type.
- **AI integrity:** guardrail/golden evals green; no fabricated citations or data in the eval corpus; ghostwriting refused.
- **Security:** auth/token/magic-link scope; webhook forgery+replay; signed-URL scope; headers/CSP; dependency + image scan clean.
- **Performance/a11y:** NFR latency budgets met; editor route mobile/3G budgets met; WCAG 2.1 AA on primary flows (axe + keyboard).
- **Resilience:** worker-down degradation verified; job retry/DLQ verified; restore-from-backup drill completed (RPO ≤ 15 min, RTO ≤ 2 h).
- **Observability:** Sentry receiving from all three services; health/ready accurate; AI-cost dashboard populated.

### 5.3 User Acceptance Testing (UAT)
- Recruit 3–5 real students + 2 supervisors (ideally one off-campus on mobile data).
- Each completes the golden journey unaided; capture drop-offs and confusion.
- Supervisor UAT specifically validates the **magic-link** path and comment usefulness.
- Exit bar: ≥ 80% complete unaided; no Sev-1/Sev-2 open; disclosure statement understood by a supervisor.

### 5.4 Launch-readiness checklist
- Runbooks present (restore, secret rotation, cert renewal, worker/LLM outage, webhook reconciliation, DSAR).
- Backups + WAL archiving verified by a real restore.
- Rate limits + AI credit caps tuned to protect **AI margin ≥ 60%**.
- NDPA/NDPR: consent copy, retention policy, DSAR path live.
- Staging mirrors prod; rollback path tested; on-call + alerting wired.
- Seeded demo institution for sales/onboarding.

---

## 6. Traceability: phase → modules → requirements → goal

| Phase | Modules | Representative FR IDs | Goal contribution |
|---|---|---|---|
| 0 | platform/CI | (enablers) | De-risks delivery; quality gates exist |
| 1 | Identity/Org | FR-AUTH/RBAC/ORG/TEN | Safe, separated users & institutions |
| 2 | Project | FR-PROJ-* | The workspace work attaches to |
| 3 | Template/Document | FR-TMPL/DOC | Structured, formatted authoring |
| 4 | AI/Alignment/Ledger | FR-AI/ALIGN/LEDGER | AI assists transparently; coherence |
| 5 | Paper/Citation | FR-LIT-* | Literature-backed, citation-supported |
| 6 | Review/Notification | FR-SUP-* | Supervisor-reviewed; adoption lever |
| 10 (MVP) | Document/Billing/Admin | FR-DOC-6..8/BILL/ADM/LEDGER-3,4 | Exportable, payable, operable |
| 7–9 | Questionnaire/Data/Similarity | FR-Q/DATA/SIM | Deeper research support (MVP+) |
| Integration | all | golden journey + gates | **The goal, proven end-to-end** |

Every MVP `FR-*` in [Functional Requirements](./FUNCTIONAL_REQUIREMENTS.md) appears in exactly one phase's Definition of Done; the Integration phase asserts they co-operate.

## 7. Risk burn-down (retired by the phase that handles it)

| Risk (from [PRD](./PRD.md)) | Retired in | How |
|---|---|---|
| Perceived as cheating tool | 4 | Guardrails + ledger enforced & tested |
| Supervisors won't onboard | 6 | Magic-link review proven in e2e |
| AI cost erodes margin | 4, 10 | Gateway routing/caching + credit caps + cost dashboard |
| Hallucinated citations | 5 | Citation validator blocks unknown references |
| Low-connectivity churn | 3 | Offline autosave + mobile/3G perf gates |
| Multi-tenant leak | 1 (ongoing) | Isolation suite green every PR |
| Payment abuse/dupes | 10 | Signature-verified, idempotent webhooks |
| Regulatory (NDPA) | 10 + launch | Consent, retention, DSAR live |

## 8. Suggested sequencing & team
- **Order:** 0 → 1 → 2 → 3 → 4 → 5 → 6 → 10(MVP slice) → Integration/UAT → launch → 7/8/9 as demand dictates. Phases 7–9 are mutually independent.
- **Parallelization:** within a phase, backend + worker + frontend proceed against the agreed `openapi.yaml` slice; the contract is the coordination point.
- **Minimum team:** 1 backend (Java), 1 worker/AI (Python), 1 frontend (Next.js), shared QA/devops; a PM/owner guarding scope and the integration gates.

---

## Summary checklist (this package)

- [x] README.md
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
- [x] DEPLOYMENT_AND_INFRASTRUCTURE.md
- [x] ENGINEERING_DECISIONS.md
- [x] IMPLEMENTATION_PLAN.md  ← this document
