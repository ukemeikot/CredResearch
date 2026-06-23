# Changelog

All notable changes to this project are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added
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

### Changed
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
