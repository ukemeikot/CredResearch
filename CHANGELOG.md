# Changelog

All notable changes to this project are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added
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
- CI: removed the `pnpm/action-setup` `version:` input that clashed with `packageManager` in
  `package.json` (ERR_PNPM_BAD_PM_VERSION) in the `web` and `contract` workflows.

### Changed
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
