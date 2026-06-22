# Repository Strategy — monorepo, split-ready

Related: [Technical Specification §3](./TECHNICAL_SPECIFICATION.md) · [ADR-002](./ENGINEERING_DECISIONS.md) · [System Architecture §10](./SYSTEM_ARCHITECTURE.md)

## Decision

Stay a **monorepo** for now (matches [Technical Spec §3](./TECHNICAL_SPECIFICATION.md) and the
small-team rationale in [ADR-002](./ENGINEERING_DECISIONS.md)), but keep it **split-ready** so the
frontend and backend can move to separate repos later with mechanical effort, not a rewrite.

This is a *source-layout* decision only. At runtime the services are already independent: separate
containers, communicating solely over HTTP/REST. The monorepo never couples them at runtime.

## What couples them today — and how it's handled

| Coupling | Where | Split-ready handling |
|---|---|---|
| API contract | `packages/api-contract` (`@credresearch/api-client`) | Versioned package; web consumes via `workspace:*` now, swappable to a published semver |
| Build paths | `infra/docker-compose*.yml` (`build: ../services/...`) | Mechanical to repoint at images/submodules |
| One CI | `.github/workflows/*` | Already **path-filtered** per service, so each builds independently |

Nothing else couples them — no shared code, no shared process, no shared DB connection.

## Hygiene already in place

- **Versioned contract package** `@credresearch/api-client` (OpenAPI 3.1 source of truth + generated TS types).
- **Path-filtered CI**: `backend.yml`, `ai-worker.yml`, `web.yml`, `contract.yml` each trigger only on
  their own paths — a backend change never rebuilds web.
- **Contract drift guard**: `contract.yml` lints the spec and fails if the generated client is stale.
- **Clean module boundaries** in the backend (no cross-module table access) so a *backend module*
  (e.g. `ai`, `paper`) can also be extracted into its own service later — see [ADR-002](./ENGINEERING_DECISIONS.md).

## Extraction runbook — splitting web into its own repo (when the time comes)

1. **Publish the contract.** Enable the publish step so a change to `packages/api-contract` publishes
   `@credresearch/api-client` to a registry (GitHub Packages / npm) on version bump.
2. **Move `apps/web`** into a new repo, keeping its `package.json`.
3. **Swap the dependency** from `"@credresearch/api-client": "workspace:*"` to `"^<version>"`.
4. **Carry over** `web.yml` and `.env` keys (`API_BASE_URL`, `SENTRY_DSN`, …) to the new repo's CI/secrets.
5. **Repoint compose/deploy**: the prod compose pulls the web image from the registry instead of
   building `../apps/web`.
6. **Add cross-repo contract testing** (openapi-diff / Pact) to replace the safety the monorepo gave
   for free, so the two repos can't silently drift.

The same pattern extracts the backend or a single backend module.

## When to actually split

Split when you have **separate teams owning separate deploy cadences** for web vs backend — that's
what polyrepo buys. Until then the monorepo's atomic cross-cutting changes and single contract check
are a net win for a small team; a premature split mostly adds version-skew and cross-repo-PR overhead.
