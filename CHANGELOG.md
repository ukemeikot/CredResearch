# Changelog

All notable changes to this project are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added
- **Real-time collaboration (Yjs), staging.** Sections can now be co-edited live, Google-Docs style:
  a new `collab` WebSocket service (Hocuspocus) syncs a shared Yjs document per section and shows
  live presence (who's editing, coloured caret avatars). Access tokens are verified (RS256) on
  connect. The server is stateless — one elected client persists the merged content through the
  existing autosave API, so version conflicts disappear during simultaneous editing. Deployed to
  **staging only** (an always-on service); production keeps the optimistic-lock autosave model
  (the same web image gates collab on the host). LB routes `/collab` when enabled. (FR-DOC-9.)
- **Document export / download (DOCX + PDF).** Any project member can download a document from the
  editor's **Download** menu. The worker renders the document (all sections, in order) to a **.docx**
  via `python-docx`, applying the template's format rule (font family/size, line spacing); **.pdf**
  is produced by a **Gotenberg (LibreOffice)** sidecar when wired (staging), and the UI degrades
  gracefully to "try Word" when PDF isn't available. Backend `GET /documents/{id}/export?format=`
  streams the file with a sensible filename. (FR-DOC-6/7.)
- **AI topic assistance when starting a project.** The New-Project dialog now has an *"Get AI topic
  ideas"* panel: enter a field of study (+ optional interests) and the AI **topic generator** returns
  candidate titles with a feasibility rating, rationale, and suggested methods — click one to use it
  as the project title. (FR-AI topic generator.)
- **Phase 4 completion — credits, usage tracking & AI-Use Disclosure Ledger.** Every AI call is
  recorded (`ai_requests`/`ai_responses`) and metered against a **per-plan monthly credit** limit
  (`plans` seeded FREE/STUDENT/INSTITUTION); the editor shows remaining credits and blocks at zero
  (402). Accepting an AI suggestion appends a tamper-evident, **hash-chained entry** to the
  document's **disclosure ledger** (`ai_disclosure_entries`), viewable from an "AI disclosure"
  drawer. Flyway `V7`. (FR-AI, FR-LEDGER.)

### Changed
- **Lenient AI output schemas** — a small self-hosted model's slightly-off JSON is coerced/accepted (normalised enums, generous defaults, extra fields ignored) instead of falling back to the stub.
- **AI worker tuning + resilience.** Bounded generation length (`num_predict`) and a warm
  keep-alive for the self-hosted model, a generous backend read timeout so slow CPU inferences
  aren't turned into spurious 503s, and diagnostic logging on AI-worker call failures.

### Added
- **Phase 4 — AI Research Assistant (v1).** The FastAPI worker now exposes AI features behind a
  provider-agnostic LLM gateway: **topic generator + feasibility**, **aim/objectives/research
  questions/hypotheses**, **problem-statement refinement**, **section drafting assistant**, and the
  **research alignment engine** — all returning validated JSON. The gateway targets a self-hosted
  open model (Ollama) via `LLM_BASE_URL`, and falls back to a deterministic stub when no model is
  wired, so the whole feature works end-to-end offline (graceful degradation, NFR-AVAIL-5). The Java
  backend proxies `/api/v1/ai/*` to the private worker (shared-secret; verified-email gate,
  FR-AUTH-1). Frontend: an **AI assist** control in the section editor that drafts/improves the
  current section and inserts the result. (Self-hosted Ollama VM, per-plan credits, and
  disclosure-ledger writes follow as the next increments.)
- **Editable document structure.** The project owner can now restructure a document from the
  outline — add a section, rename its heading, reorder (move up/down), and delete a section (with
  its version history). Owner-gated section CRUD (`POST/PATCH/DELETE /documents/{id}/sections`).

### Fixed
- **Modal overlays render correctly.** Modals/drawers now portal to `document.body` so they aren't
  trapped by a transformed (framer-motion) ancestor — the New Document modal was rendering mid-page
  behind the panels, making its template buttons unclickable and blocking document creation.

### Fixed (earlier)
- **Resilient DB migrations on rolling deploys.** A rolling replace interrupted mid-migration could
  leave the schema-history half-applied (object created, history row not finalised), then crash-loop
  the new backend on "relation already exists". Migrations are now idempotent (`IF NOT EXISTS` +
  guarded seeds) and a repair-then-migrate strategy realigns/repairs history on startup, so
  first-deploy migrations self-heal instead of needing manual DB surgery. The deploy also now waits
  for the MIG to become healthy, failing loudly rather than silently serving the old image.

### Added
- **Phase 3 — Template & Document Builder.** New `document` module (Clean Architecture): global
  UG/MSc/PhD templates with ordered sections + format rules (seeded); create a document from a
  template (instantiates its sections); per-section rich-text editing (Tiptap/ProseMirror JSON)
  with **optimistic-lock autosave** (409 on conflict), **offline buffering** with reconnect flush,
  and **per-section version history + restore** (FR-TMPL-1/2/3, FR-DOC-1..5). Flyway `V5__documents.sql`.
  Frontend: `/projects/[id]/documents/[docId]` editor with section nav, toolbar, save/offline/conflict
  indicators, template picker, and a Documents panel in the project workspace.
- **Team invitations by email.** Adding a member is now an email invite (tokenized magic-link),
  not a raw user id: `POST /projects/{id}/invitations`, pending list, revoke, and `POST
  /invitations/accept`. Flyway `V6__invitations.sql`. Acceptance is intra-institution (preserves
  tenant isolation). Frontend: invite-by-email UI, pending invites, and an `/invite/accept` page.
- **Institution onboarding.** A signed-in user can create an institution from Settings and become
  its INSTITUTION_ADMIN (`POST /onboarding/institution`), unlocking institution + department
  management. The client refreshes its session afterward to pick up the new tenant + role.
- **Frontend — full API integration.** The web app now consumes every backend endpoint. New
  **project workspace** at `/projects/[id]` (dashboard stats, milestones, team members with
  add/remove, activity feed, lifecycle status transitions honouring the server state machine, and
  project editing), a **/settings** page (profile update, plus role-gated institution and
  departments management), and **forgot-/reset-password** flows. Logout now revokes the refresh
  token server-side and clears cached state; the API client dedupes concurrent token refreshes into
  a single in-flight request. TanStack Query hooks per feature with targeted cache updates.
- **CI — MIG-aware deploys + rollback.** `deploy-gcp.yml` rolls web/backend via managed
  instance-group rolling-replace (zero-downtime) instead of resetting fixed VMs; new
  `rollback-gcp.yml` re-points `:latest` to a prior image SHA and rolls a chosen environment onto it.
- **Web production image + deploy pipeline**: `apps/web/Dockerfile` (Next standalone, built with a
  relative `NEXT_PUBLIC_API_BASE_URL`) and `.github/workflows/release.yml` — on push to `main` it
  builds/pushes the three service images to ECR and triggers the infra repo to sync (gated behind
  the `DEPLOY_ENABLED` repo variable). Infrastructure lives in the separate `credresearch-infra`
  Terraform repo (AWS: VM-per-service + RDS + ElastiCache + ALB; GCP scaffolded).
- **Phase 2 — Project Workspace (backend).** `modules/project` (Clean Architecture): create/list/get/
  update projects; status lifecycle with a validated state machine + history; members & co-supervisors;
  milestones; activity feed; dashboard aggregation. `ProjectAccessGuard` enforces tenant **and**
  membership (FR-PROJ-1..7). Flyway `V4__projects.sql`; `@Scheduled` milestone-reminder sweeper;
  unit tests (state machine, access guard, project service).
- **Frontend (Next.js App Router) — initial implementation.** Cosmic dark design system (Tailwind
  theme, animated canvas starfield, glassmorphism, Space Grotesk display font) with framer-motion
  microinteractions (button/card hover & tap, page/scroll reveals, animated counters, floating hero
  orb). Pages: landing, login, register (wired to the live auth API), and an authenticated project
  **dashboard** (stats + project grid + create-project modal) consuming the Phase 2 `/projects` API.
  TanStack Query for server state; Zustand for the session; typed fetch client.
- **Phase 1 — Auth, Roles & Multi-Tenant Base.**
  - Email/password auth: register, login, refresh-token rotation, logout, logout-all, email
    verification, and password reset (`FR-AUTH-1..5`). Passwords hashed with **Argon2id**;
    access tokens are **RS256 JWT** (15 min); refresh tokens are opaque, stored hashed, single-use
    (rotated on refresh), and revocable.
  - **RBAC** (`FR-RBAC-1..2`): six seeded roles + permission catalogue; method-level `@PreAuthorize`.
  - **Multi-tenancy** (`FR-TEN-1`): `TenantContext` from the JWT; tenant-scoped repositories; a
    synthetic personal tenant per self-registered user (`FR-ORG-4`).
  - **Organization** (`FR-ORG-1..6`): institutions, departments, user profile (`/users/me`),
    seeded demo institution.
  - Brute-force login throttling (Redis), audit logging (`audit_logs`), RFC 7807 problem+json
    errors, and SMTP notifications (MailHog locally).
  - Flyway `V2__identity_org.sql` + `V3__seed_rbac.sql`; unit tests for JWT, Argon2, token
    rotation, throttling, and tenant isolation.
  - Hardening: JWT issuer validation on parse; actuator health details restricted to
    authenticated callers (anonymous sees status only).
- **Swagger UI / live API docs** (springdoc-openapi) at `/swagger-ui/index.html`, with a Bearer-JWT
  Authorize button; OpenAPI JSON at `/v3/api-docs`.
- **Per-endpoint documentation**: every controller annotated with `@Tag`/`@Operation`/`@ApiResponses`
  (summaries, descriptions, response codes, role requirements) and request-field `@Schema`s; public
  auth endpoints marked as not requiring a bearer token.
- CodeRabbit config now on `dev` so it auto-reviews PRs into `dev` (and stays off for staging/main).
- **CI security**: secret scanning (gitleaks), dependency/config vulnerability scanning (Trivy),
  Dependabot (gradle/pip/npm/actions/docker), and a changelog-required check on every PR.

### Security
- Upgraded to **Next.js 16.2.10 + React 19** (from 14.2.15). Clears all outstanding Next.js
  advisories (CVE-2025-29927 auth-bypass and the 15/16-only DoS/SSRF items), so `.trivyignore`
  was removed. Peers bumped: framer-motion 12, lucide-react latest, `@types/react` 19.

### Fixed
- **CORS**: backend now allows the web-app origin (configurable via `CORS_ALLOWED_ORIGINS`,
  default `http://localhost:3000`) and handles preflight `OPTIONS` — the browser SPA could not
  call the API before (403 on preflight).
- CI: removed the `pnpm/action-setup` `version:` input that clashed with `packageManager` in
  `package.json` (ERR_PNPM_BAD_PM_VERSION) in the `web` and `contract` workflows.
- Backend Docker image build uses a BuildKit cache mount for the Gradle cache (faster rebuilds).

### Changed
- Web refactored to feature-sliced clean architecture: data/mutation logic moved out of page files
  into `features/<feature>/{api,components,model}` (TanStack Query hooks, screen components, pure
  stat/status helpers); `app/**/page.tsx` are now thin route entries.
- Frontend performance: dropped `background-attachment: fixed`, reduced glass `backdrop-blur`
  (xl→md), lighter starfield (lower density, pauses when the tab is hidden, capped DPR).
- Hero "globe" replaced with a real CSS planet (`Globe`): shaded sphere, rotating surface texture,
  atmosphere glow, sun glint, orbiting moon.
- Responsive pass: mobile nav menu, always-visible sign-in, fluid hero sizing, `overflow-x` guard,
  reduced-motion support.
- Rewrote the root `README.md` to describe the CredResearch product, architecture, and quick start.
- `docker-compose.yml`: added MailHog; wired backend email + app-base-url env.
- CI `security` workflow: run Trivy via `ghcr.io/aquasecurity/trivy` (fixes an invalid action version).
- Backend & ai-worker Dockerfiles run as a non-root `appuser` (Trivy DS-0002).
- CodeRabbit (`.coderabbit.yaml`): auto-review PRs into `dev` only; disabled for `staging`/`main`.

### Removed
- Untracked the internal authoring artifact `DOCS_GENERATION_PROMPT.md` (now git-ignored).

### Added
- **Phase 0 foundation.** Monorepo scaffold using Clean Architecture inside a modular monolith
  (backend) plus a separate Python AI/data worker.
- **Backend** (Spring Boot, Java 21): application bootstrap, Actuator health/readiness, Flyway with
  `V1__extensions.sql` (pgvector + citext), and a `/api/v1/ping` smoke endpoint.
- **AI worker** (FastAPI): application factory with a `/health` router.
- **Infra**: Docker Compose stack — backend, ai-worker, postgres (pgvector), redis — with
  healthchecks and ordered startup.
- **API contract**: versioned `@credresearch/api-client` package (OpenAPI 3.1 source of truth +
  generated TypeScript types).
- **CI**: path-filtered GitHub Actions workflows (`backend`, `ai-worker`, `web`, `contract`).
- **Docs**: full product/technical documentation set under `docs/`, `GETTING_STARTED.md`,
  `docs/REPO_STRATEGY.md`, and per-service `ARCHITECTURE.md` files.

[Unreleased]: https://github.com/ukemeikot/CredResearch/commits/main
