# @credresearch/api-client — the API contract

`openapi.yaml` is the **single source of truth** for the CredResearch REST API. The backend
implements it; the web app consumes the generated TypeScript types. This package is what
keeps frontend and backend decoupled — and what makes a future repo split mechanical.

## Contract-first workflow

1. Edit `openapi.yaml` for the new/changed endpoint **first**.
2. `pnpm --filter @credresearch/api-client build` → lints the spec and regenerates `generated/schema.ts`.
3. Backend implements the endpoint to match; web consumes it via the generated types.
4. CI (`.github/workflows/contract.yml`) lints the spec, checks for breaking changes, and
   fails if `generated/` is stale (`pnpm run check`).

## How the web app consumes it (today: monorepo)

In `apps/web/package.json`:

```json
{ "dependencies": { "@credresearch/api-client": "workspace:*" } }
```

```ts
import type { paths, components } from "@credresearch/api-client";
type Ping = components["schemas"]["PingResponse"];
```

`workspace:*` resolves to this local package via pnpm — no publish step needed in the monorepo.

## Split-ready: how this becomes a separate repo later

The **only** change required to split frontend and backend into different repos:

1. Version-bump this package and **publish** it to a registry (GitHub Packages / npm):
   `pnpm publish` from CI on a contract change (a publish workflow stub is ready to enable).
2. In the (now separate) web repo, change the dependency from `workspace:*` to a real semver:
   `"@credresearch/api-client": "^0.1.0"`.
3. Point `infra/docker-compose*.yml` build contexts at images/submodules instead of sibling paths.

That's it — because the services already communicate only over HTTP, nothing else couples them.
See `docs/REPO_STRATEGY.md` for the full extraction runbook.
