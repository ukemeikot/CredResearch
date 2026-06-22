# Changelog

All notable changes to this project are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

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
