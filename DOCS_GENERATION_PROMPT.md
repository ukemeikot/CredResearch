# Documentation Generation Prompt — African Academic Research SaaS Platform (v2)

> This is a revised version of the original prompt. Changes are integrated cleanly so it can be pasted directly. New additions versus v1 are marked inline with **[ADDED]** so you can see what changed; remove the markers before final use if you prefer.

---

You are a senior product manager, software architect, backend engineer, AI systems engineer, and technical documentation expert.

I want you to generate a complete product and technical documentation package for an African academic research SaaS platform.

The product is an AI-powered academic research workflow and supervision platform for African universities, undergraduates, postgraduate students, PhD researchers, lecturers, supervisors, departments, institutions, and private research consultants.

The platform should help users move from research idea to properly structured, supervisor-reviewed, citation-supported, formatted academic document. It must **not** be positioned as an "AI thesis writer." It is a research workflow, supervision, academic formatting, literature review, citation, originality pre-check, data collection, and research quality platform.

**[ADDED] Core positioning safeguard:** The product's defensibility against "ghostwriting" accusations is a first-class, built-in feature — an **AI-Use Disclosure & Academic Integrity Ledger** (see Module 13). Every document carries a transparent, exportable record of how AI was used. Treat this as a primary differentiator, not an afterthought.

Generate all documentation as separate Markdown files inside a `/docs` folder.

## Files to create

1. `README.md` **[ADDED — docs index linking every file below, with a one-line description and a suggested reading order]**
2. `PRD.md`
3. `FUNCTIONAL_REQUIREMENTS.md`
4. `NON_FUNCTIONAL_REQUIREMENTS.md` **[ADDED]**
5. `TECHNICAL_SPECIFICATION.md`
6. `SYSTEM_ARCHITECTURE.md`
7. `ERD.md`
8. `DATABASE_SCHEMA.md`
9. `API_SPECIFICATION.md`
10. `AI_SYSTEM_DESIGN.md`
11. `SECURITY_AND_COMPLIANCE.md`
12. `TEST_STRATEGY.md` **[ADDED]**
13. `MVP_ROADMAP.md`
14. `DEPLOYMENT_AND_INFRASTRUCTURE.md`
15. `ENGINEERING_DECISIONS.md`

End the package with a summary checklist of all generated documents (in `README.md` and repeated at the close of the final document).

---

## Production-grade stack

**Frontend**
* Next.js (App Router)
* TypeScript
* Tailwind CSS
* shadcn/ui
* Tiptap editor for academic document editing
* TanStack Query for server state
* Zustand only where lightweight client state is needed
* **[ADDED]** PWA-ready / low-bandwidth-tolerant: offline-safe autosave buffering, optimistic UI, and graceful degradation on intermittent connectivity (a core African-market constraint). No native mobile app at MVP, but the web app must be fully usable on low-end Android devices over 3G.

**Backend**
* Java 21
* Spring Boot
* Spring Security
* Spring Data JPA
* PostgreSQL
* Flyway
* Redis
* REST API first (**[ADDED]** contract-first: a maintained `openapi.yaml` (OpenAPI 3.1) is the source of truth for the API spec)
* Modular monolith architecture for MVP

**AI/Data Worker**
* Python FastAPI
* Used for AI generation, document parsing, embeddings, RAG, citation extraction, literature matrix generation, data analysis, and PDF/DOCX intelligence
* Communicates with the Java backend through HTTP first, later queue-based jobs
* **[ADDED]** Sits behind a provider-agnostic **LLM Gateway** (see ADR): the worker never hard-codes a single model vendor. It supports model routing (cheap model for simple/structured tasks, stronger model for reasoning-heavy tasks), response/embedding caching, token budgeting, and a path to self-hosted open models later.
* **[ADDED]** PII redaction layer: prompts sent to external LLM providers must strip or pseudonymize personal data (names, emails, institution identifiers) where not strictly required.

**Database**
* PostgreSQL
* pgvector for embeddings and RAG (**[ADDED]** document the chosen embedding dimensionality and index type, e.g. HNSW, and the chosen embedding model — a small multilingual model is preferred for pan-African content)
* PostgreSQL full-text search for MVP
* OpenSearch or Meilisearch can be considered after MVP

**Storage**
* MinIO for local development
* S3-compatible storage in production (AWS S3, Cloudflare R2, DigitalOcean Spaces, or production MinIO)
* **[ADDED]** All file access via short-lived signed URLs; bucket versioning enabled for recovery.

**Cache/Queue**
* Redis for cache, rate limiting, and initial background job coordination
* RabbitMQ can be introduced later if workload grows
* Kafka should not be used at MVP stage unless there is a strong reason

**Deployment**
* Docker Compose for local development and first deployment
* VPS deployment for early MVP
* Kubernetes only after the product matures
* Nginx reverse proxy
* Let's Encrypt SSL
* GitHub Actions for CI/CD
* **[ADDED]** A `gotenberg` (or headless LibreOffice) service for HTML/DOCX → PDF conversion.
* **[ADDED]** Defined `staging` environment in addition to local + production.

**Monitoring**
* Spring Boot Actuator
* **[ADDED]** Sentry wired in from day one (frontend + backend + worker) — it is cheap and catches MVP issues early.
* OpenTelemetry later
* Prometheus and Grafana later
* Loki for logs later

**Payments**
* Paystack
* Flutterwave
* Bank transfer / manual invoice support for institution plans
* **[ADDED]** Multi-currency support (NGN primary; design for additional African currencies + USD for diaspora/consultants).
* **[ADDED]** Webhook handling must verify provider signatures and be idempotent (idempotency keys) to prevent double-processing.

**[ADDED] Notifications & Messaging**
* Provider-agnostic notification layer with at least: in-app, email (e.g. Resend/SES or a local SMTP provider), and SMS (e.g. Termii, widely used in Nigeria). WhatsApp as an optional channel given regional usage patterns.
* Transactional templates for: supervisor invite, review decision, milestone due, payment receipt, account events.

**[ADDED] Citation engine**
* Use **CSL (Citation Style Language) / citeproc** so citation styles are configuration, not code. APA, IEEE, Harvard at MVP; new styles added by dropping in CSL files.
* Support import/export of **BibTeX and RIS**, and import from Zotero/Mendeley libraries (MVP-plus) to reduce adoption friction.

**[ADDED] Document export engine**
* DOCX generated in the Python worker (e.g. `python-docx` or a DOCX templating approach driven by the structured document model).
* PDF via Gotenberg / headless LibreOffice from the rendered document.

---

## MVP core value (unchanged)

A student can create a research project, select an institution/department template, generate or refine a topic/proposal, build Chapter 1–3, upload academic papers, generate a literature matrix, manage citations, invite a supervisor, receive review comments, revise work, and export a formatted DOCX/PDF.

Do not overbuild the MVP. Clearly separate MVP features from post-MVP features.

---

## Target users (unchanged)
Undergraduate students, Master's students, PhD students, lecturers/supervisors, private research consultants, department admins, institution admins, platform admins.

---

## Core product modules to document

### 1. User, Role, and Institution Management
* User registration/login
* JWT auth with refresh tokens
* Role-based access control
* Institutions, departments, academic profiles
* Multi-tenancy using shared database / shared schema with `institution_id` / `tenant_id`
* **[ADDED]** Institution onboarding flow: institution admin invites department admins and supervisors; bulk student import; seedable demo institution.

Roles: `STUDENT`, `SUPERVISOR`, `CONSULTANT`, `DEPARTMENT_ADMIN`, `INSTITUTION_ADMIN`, `PLATFORM_ADMIN`.

### 2. Project Workspace
Create research project; assign academic level; assign department/institution; add supervisor or consultant (**[ADDED]** support multiple/co-supervisors); track project status; milestones; activity feed.

### 3. Research Template and Document Builder
Institution/department-specific templates; UG project, Master's dissertation, PhD thesis templates; Chapter 1–5 support (MVP focuses on proposal + Chapter 1–3); section management; rich text editing; autosave; version history; formatting rules; export to DOCX/PDF.
* **[ADDED]** Canonical section content is **Tiptap/ProseMirror JSON**, stored in `document_sections`. Autosave uses optimistic locking (version number) with conflict resolution. Real-time collaborative editing (Yjs) is explicitly deferred post-MVP (document the decision).

### 4. AI Research Assistant
Topic suggestions; topic feasibility score; proposal outline; aim/objective/research-question generation; methodology suggestion; problem-statement refinement; research alignment checker; literature review support. Must use **structured JSON responses**, include academic guardrails, must **not** invent citations, must **not** invent data, must **not** present itself as a ghostwriting engine.
* **[ADDED]** Every AI interaction is written to the Academic Integrity Ledger (Module 13). The assistant suggests and explains; the student authors. Outputs are clearly labelled as suggestions.

### 5. Research Alignment Engine (key innovative feature)
Checks consistency across: title, problem statement, aim, objectives, research questions, hypotheses, methodology, questionnaire, analysis method. Generates a research quality/alignment report.

### 6. Paper Upload, Literature Review, and Citation Module
Upload PDF/DOCX; extract text + metadata; summarize; extract methodology/findings/limitations/gaps; generate literature matrix; manage citations; support APA, IEEE, Harvard (via CSL); generate reference list; RAG over uploaded papers; store chunks + embeddings.
* **[ADDED]** BibTeX/RIS import/export; deduplicate papers; flag low-quality OCR/extraction for user review.

### 7. Supervisor Workflow
Invite supervisor; assign to project; submit document section for review; inline comments; review decisions (approved / needs revision / rejected); review history; notifications; supervisor dashboard; student revision workflow.
* **[ADDED] Email-first / magic-link review:** external supervisors who do not yet have an account can review and comment via a secure tokenized link without full onboarding. This is critical for adoption. Capture this in the ERD via an `invitations` entity.

### 8. Questionnaire Builder *(MVP-plus)*
Generate questionnaire from objectives; Likert/multiple-choice/short-answer/open-ended; consent text; public survey links; response collection; CSV/Excel export.

### 9. Basic Data Analysis *(MVP-plus)*
CSV upload; data preview; missing-value detection; frequency tables; mean/percentage/SD; basic charts; AI-assisted interpretation based **only** on uploaded data; Chapter 4 starter draft.

### 10. Basic Similarity and Originality Pre-check *(MVP or MVP-plus)*
Internal similarity check; compare against user documents and institution repository; detect repeated paragraphs; detect citation risk; generate similarity report. **Do not claim to replace Turnitin.**

### 11. Billing and Subscription
Plans: Free, Student Basic, Student Pro, Consultant, Department, Institution. Usage limits on projects, AI credits, paper uploads, exports, similarity checks, supervisor invitations.
* **[ADDED]** Usage metering enforced via Redis counters tied to `usage_limits`; idempotent, signature-verified payment webhooks; multi-currency pricing; institution manual-invoice path.

### 12. Admin Dashboard
Manage users, institutions, departments, templates; view payments, usage, project statistics; manage plans.
* **[ADDED]** Feature flags for staged rollout; platform-wide health and AI-cost dashboards.

### 13. **[ADDED] AI-Use Disclosure & Academic Integrity Ledger** *(MVP)*
* Per-document, append-only record of every AI interaction: timestamp, prompt category, model used, the suggestion returned, and whether the student accepted, edited, or rejected it.
* Computes an indicative "AI-assisted vs. human-authored" signal per section (clearly framed as indicative, not forensic).
* Exportable **AI-Use Disclosure Statement** that students can attach to submissions and supervisors/examiners can review — aligning the product with emerging university AI-disclosure policies and reinforcing the "not a ghostwriter" stance.

---

## Required architecture decisions to document

1. Modular monolith for MVP, not microservices.
2. Java Spring Boot for the core backend.
3. Python FastAPI worker for AI/data/document tasks.
4. PostgreSQL + pgvector for relational data and vector search.
5. MinIO locally, S3/R2-compatible storage in production.
6. Redis for cache, rate limiting, and initial job coordination.
7. Structured JSON document model (**[ADDED]** specifically Tiptap/ProseMirror JSON) for thesis/document content.
8. Tiptap editor for frontend rich text editing.
9. JWT access token + refresh token auth.
10. RBAC and tenant isolation.
11. DOCX export first, then PDF export.
12. PostgreSQL full-text search first; postpone OpenSearch/Meilisearch.
13. Internal similarity pre-check first; do not claim Turnitin-level detection.
14. Docker Compose for local development and early deployment.
15. Postpone Kubernetes, mobile app, full offline mode, ERP integrations, and full SPSS/Turnitin replacement until after MVP.
16. **[ADDED]** Provider-agnostic LLM gateway with model routing and caching; never hard-code a single vendor.
17. **[ADDED]** CSL/citeproc for citation styles; defer custom citation parsers.
18. **[ADDED]** python-docx + Gotenberg/LibreOffice-headless for export.
19. **[ADDED]** Signed internal token (or mTLS) for Java ↔ FastAPI service-to-service auth.
20. **[ADDED]** Idempotency keys + provider signature verification for all payment webhooks and long-running AI jobs.
21. **[ADDED]** Defer real-time collaborative editing (Yjs) until post-MVP.
22. **[ADDED]** Notification provider abstraction (in-app + email + SMS, WhatsApp optional).

---

## Required ERD content

Generate a detailed ERD in Mermaid syntax and explain each entity in text. **[ADDED]** Document the shared base-entity convention (`id`, `created_at`, `updated_at`, `created_by`, `updated_by`, soft-delete `deleted_at`, and `institution_id`/`tenant_id` on tenant-scoped tables).

Entities (at minimum):

**Identity and organization:** institutions, departments, users, roles, permissions, user_roles, role_permissions, refresh_tokens, audit_logs, **[ADDED]** invitations, feature_flags

**Projects:** projects, project_members, project_milestones, project_activities, project_status_history

**Templates and documents:** templates, template_sections, document_format_rules, documents, document_sections, document_versions

**Reviews:** review_requests, review_comments, review_decisions, notifications, **[ADDED]** notification_outbox

**Papers, literature, citations:** files, papers, paper_chunks, paper_embeddings, citations, literature_matrix_entries

**AI:** ai_requests, ai_responses, ai_prompt_templates, ai_usage_logs, research_alignment_reports, **[ADDED]** ai_jobs (generic async job tracking: status, attempts, payload ref, result ref), **[ADDED]** ai_disclosure_entries (the Integrity Ledger)

**Questionnaires:** questionnaires, questions, survey_links, survey_responses, survey_answers

**Data analysis:** datasets, dataset_columns, analysis_jobs, analysis_results, analysis_charts

**Similarity:** similarity_checks, similarity_matches, similarity_reports, institution_repositories

**Billing:** plans, subscriptions, payments, usage_limits, usage_events, **[ADDED]** webhook_events (idempotency + provider event log)

Relationships to show (all v1 relationships retained), plus **[ADDED]**:
* Invitations belong to a project and resolve to a user on acceptance.
* AI disclosure entries belong to a document (and optionally a document section and an ai_request).
* Webhook events relate to payments; processed-once via idempotency key.
* Notification outbox rows fan out per channel.

(Retain all original relationships: institution→departments, department→users, users→roles via user_roles, roles→permissions via role_permissions, users→projects via project_members, projects→milestones/activities/documents/papers/citations/questionnaires/datasets/ai_requests/alignment_reports, templates→template_sections, documents→sections, sections→versions+review_requests, review_requests→comments+decisions, papers→chunks, chunks→embeddings, projects→literature_matrix_entries, questionnaires→questions+responses, survey_responses→answers, datasets→analysis_jobs, analysis_jobs→results+charts, subscriptions→users/institutions, payments→subscriptions.)

---

## Required API specification

Generate REST API groups and endpoints for:
`/api/v1/auth`, `/api/v1/users`, `/api/v1/institutions`, `/api/v1/departments`, `/api/v1/projects`, `/api/v1/templates`, `/api/v1/documents`, `/api/v1/reviews`, `/api/v1/papers`, `/api/v1/citations`, `/api/v1/ai`, `/api/v1/questionnaires`, `/api/v1/datasets`, `/api/v1/analysis`, `/api/v1/similarity`, `/api/v1/billing`, `/api/v1/notifications`, `/api/v1/admin`,
**[ADDED]** `/api/v1/invitations` (create + accept via token), `/api/v1/jobs` (async job status polling for AI/analysis/export), `/api/v1/webhooks` (`/paystack`, `/flutterwave` — signature-verified, idempotent), `/api/v1/disclosure` (Integrity Ledger + disclosure statement export), and operational `GET /health` + `GET /ready` (Actuator-backed).

For each endpoint include: path, HTTP method, purpose, required role/permission, request body example where useful, response body example where useful. **[ADDED]** Note rate-limit tier and whether the endpoint is idempotent. **[ADDED]** State that the full contract lives in `openapi.yaml`.

---

## Required MVP roadmap

Keep Phases 0–10 as in v1. **[ADDED]** Adjustments:

* **Phase 0 additions:** maintained `openapi.yaml`; LLM gateway + notification provider abstractions stubbed; Sentry + Actuator wired; test harness (Testcontainers, Playwright) and AI eval/golden-test scaffold; secrets/env strategy.
* **MVP definition (refined):** Phases 0–6 **plus** the AI-Use Disclosure Ledger (Module 13) **plus** the export/payment/admin slices of Phase 10. Treat Phases 7–9 as MVP-plus unless capacity allows.
* **[ADDED] Adoption levers in MVP:** email-first/magic-link supervisor review (Phase 6) and BibTeX/RIS import (Phase 5) — small effort, large adoption impact.

---

## Required system architecture document

Include: high-level system diagram (Mermaid); request lifecycle; AI task lifecycle (**[ADDED]** including async job status + LLM gateway + caching); file upload lifecycle (**[ADDED]** signed URLs); document export lifecycle (**[ADDED]** worker → DOCX → Gotenberg → PDF); supervisor review lifecycle (**[ADDED]** including magic-link path); background job lifecycle; local development architecture; early production deployment architecture; later scale architecture. **[ADDED]** Service-to-service auth (Java ↔ FastAPI) shown on diagrams.

---

## Required AI system design

Include: AI worker responsibilities; RAG flow; embedding strategy (**[ADDED]** named embedding model, dimensions, chunking strategy, pgvector index choice); prompt template strategy; AI guardrails; JSON output formats; AI usage logging; citation safety rules (no hallucinated citations); no invented datasets/results; alignment-checker algorithm; literature-matrix generation flow; paper-summarization flow; data-interpretation rules.
**[ADDED]:** LLM gateway and model-routing strategy; response + embedding caching; token budgeting and per-plan AI-credit accounting; PII redaction before external calls; the AI-Use Disclosure Ledger write path; AI evaluation/golden-test approach (how prompt regressions are caught in CI).

---

## Required security document

Include: authentication; authorization; tenant isolation; password hashing (**[ADDED]** name the algorithm, e.g. Argon2id/bcrypt with parameters); refresh-token storage (hashed, rotated, rev{ocable}); file access security; signed URLs; rate limiting; audit logs; AI data privacy; payment security; backups; disaster recovery; data retention; institution privacy; abuse prevention; academic-integrity positioning.
**[ADDED]:** Named compliance frameworks — **Nigeria Data Protection Act 2023 / NDPR**, plus POPIA (South Africa) and GDPR (diaspora) where relevant; data-residency stance; service-to-service auth (signed internal token / mTLS); webhook signature verification + idempotency; PII minimization/redaction in AI prompts; consent capture for survey respondents; secrets management (env at MVP → SOPS/Doppler/Vault later).

---

## Required deployment document

Include: local Docker Compose architecture; required services — frontend, backend, ai-worker, postgres-with-pgvector, redis, minio, **[ADDED]** gotenberg (PDF), and a stub mailer for local dev; environment variables (**[ADDED]** include LLM provider keys, embedding config, Sentry DSN, object-storage creds, payment keys + webhook secrets, SMS/email provider keys, internal service secret); production VPS deployment; Nginx reverse proxy; SSL; GitHub Actions CI/CD; backup strategy (**[ADDED]** pg WAL archiving / pgBackRest or WAL-G, bucket versioning, restore drills); monitoring strategy; Flyway migration strategy; **[ADDED]** staging environment and a documented restore/runbook.

---

## Required engineering decisions document (ADR-style)

For each decision include: Decision, Context, Options considered, Final choice, Reason, Consequences, Future migration path.

Important ADRs (all v1 ADRs retained): Java Spring Boot over Node/NestJS; modular monolith over microservices; Python worker for AI/data; PostgreSQL + pgvector over a separate vector DB for MVP; shared-schema multi-tenancy over DB-per-tenant; Tiptap over plain textarea; structured JSON document model; MinIO/S3-compatible storage; Redis queue first; DOCX-first export; basic internal similarity engine first; Docker Compose before Kubernetes.

**[ADDED] New ADRs:**
* Provider-agnostic LLM gateway (vs. hard-coding one vendor).
* CSL/citeproc for citation styles (vs. bespoke citation code).
* python-docx + Gotenberg/LibreOffice for export (vs. client-side or pure-Java export).
* Signed internal token / mTLS for Java↔FastAPI (vs. open internal network).
* Idempotent, signature-verified payment webhooks (vs. naive handlers).
* Tiptap/ProseMirror JSON + optimistic locking for autosave (vs. last-write-wins or HTML).
* Defer real-time collaboration / Yjs to post-MVP.
* AI-Use Disclosure Ledger as a core integrity feature (vs. relying on positioning alone).
* Notification provider abstraction with SMS/WhatsApp (vs. email-only).
* NDPA/NDPR-first compliance and data-residency stance.

---

## Output requirements

* Create the `/docs` folder if it does not exist.
* Write each document as a separate `.md` file.
* Use professional product-manager and software-architect language.
* Use tables where helpful.
* Use Mermaid diagrams for ERD and architecture diagrams.
* Be detailed enough for an engineering team to start implementation.
* Do not generate application code yet unless needed for examples.
* Do not skip small but important decisions.
* Keep the MVP realistic; avoid unnecessary enterprise complexity.
* Make the documents coherent and cross-linked (each doc links to related docs; `README.md` is the index).
* **[ADDED]** Keep all African-market constraints visible throughout: low bandwidth, intermittent connectivity, mobile-first low-end devices, cost sensitivity, local payment rails, and NDPA compliance.
* End with a summary checklist of all generated documents.
