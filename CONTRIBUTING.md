# Contributing & Repo Management

## Branching

- `main` is always releasable and protected — no direct pushes; merge via pull request.
- Branch per change, named `type/short-description`, e.g. `feat/auth-login`, `fix/flaky-export`.

## Commit messages — [Conventional Commits](https://www.conventionalcommits.org/)

```
<type>(<optional scope>): <summary in imperative mood>

<optional body>

<optional footer, e.g. BREAKING CHANGE: ...>
```

Types: `feat`, `fix`, `docs`, `refactor`, `test`, `build`, `ci`, `chore`, `perf`, `style`, `revert`.
Scopes follow the modules/services, e.g. `feat(identity):`, `fix(ai-worker):`, `ci(contract):`.

`feat` → MINOR bump, `fix` → PATCH bump, `BREAKING CHANGE` → MAJOR bump
([SemVer](https://semver.org/)).

## Changelog discipline

- Every user-facing change adds an entry under `## [Unreleased]` in `CHANGELOG.md`, grouped as
  **Added / Changed / Deprecated / Removed / Fixed / Security** (Keep a Changelog).
- On release, rename `[Unreleased]` to the new version + date and start a fresh `[Unreleased]`.

## Releases

1. Ensure CI is green on `main`.
2. Move `[Unreleased]` entries under a new `## [x.y.z] - YYYY-MM-DD` heading.
3. Tag: `git tag -a vX.Y.Z -m "vX.Y.Z"` and push tags.
4. Cut a GitHub Release from the tag, pasting that version's changelog section.

## Pull requests

- Keep PRs scoped to one logical change; reference the relevant `FR-*`/phase where useful.
- CI must pass (lint, unit, integration, contract). Update docs and `CHANGELOG.md` in the same PR.
- Two non-negotiable gates from day one: **tenant isolation** and **AI guardrails**.
