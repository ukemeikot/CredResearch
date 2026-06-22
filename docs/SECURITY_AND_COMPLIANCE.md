# Security & Compliance — CredResearch

Related: [Non-Functional Requirements](./NON_FUNCTIONAL_REQUIREMENTS.md) · [Technical Specification](./TECHNICAL_SPECIFICATION.md) · [AI System Design](./AI_SYSTEM_DESIGN.md)

## 1. Authentication

- Email + password; **Argon2id** hashing (memory-hard parameters tuned to hardware; e.g. m=64MB, t=3, p=1 as a starting point, benchmarked).
- JWT access tokens, RS256, 15-minute TTL; minimal claims (`sub`, `institution_id`, `roles`, `plan`, `exp`).
- Refresh tokens: opaque, 30-day TTL, stored **hashed** in `refresh_tokens`, **rotated on every use**, revocable individually and en masse ("log out all devices").
- Email verification required before AI features. Password reset via single-use, expiring magic-link.
- Brute-force protection: per-account + per-IP login throttling in Redis; exponential backoff/lockout.

## 2. Authorization

- RBAC: roles → permissions; checks via Spring Security `@PreAuthorize` at the controller boundary and re-validated in services.
- Project-level authorization: membership and project-role checked for every project-scoped resource.
- **Magic-link review tokens** are scoped, single-purpose, short-lived JWTs (audience `review`, claim = specific `review_request_id`); they grant comment/decision rights on exactly one review and nothing else.

## 3. Tenant isolation

- Every tenant-scoped table carries `institution_id`; `TenantContext` from the JWT is applied as a mandatory filter (JPA filter/specification) on all queries.
- No endpoint accepts a client-supplied `institution_id` to widen scope.
- **Automated isolation tests** assert that a user from tenant A cannot read/write any tenant B resource across every resource type (see [Test Strategy](./TEST_STRATEGY.md)).
- Object-storage keys are namespaced by tenant; signed URLs are per-object and short-lived.

## 4. File access security

- Uploads via backend-issued **signed PUT** URLs (short TTL, content-type pinned, size-limited).
- Downloads via short-TTL **signed GET** URLs; the API never proxies large binaries.
- Stored objects are private; bucket policy denies public access; versioning enabled.
- Uploaded files are virus/type-validated; only allowed MIME types accepted.

## 5. Service-to-service security

- Backend → AI worker calls carry a **signed internal JWT** (short-lived, audience `ai-worker`); the worker rejects unsigned/expired/wrong-audience calls.
- Worker and backend communicate over the private network; the worker is not internet-exposed (Nginx routes only public paths).
- Path to **mTLS** documented for when the worker scales out.

## 6. Rate limiting & abuse prevention

- Token-bucket limits per user and per IP in Redis; stricter `public` tier for unauthenticated endpoints.
- AI endpoints additionally gated by per-plan credits.
- Survey/public endpoints rate-limited and bot-mitigated (proof-of-work/CAPTCHA if abused).
- Account-creation throttling; disposable-email heuristics.
- Anomaly signals (sudden export/upload spikes) logged for review.

## 7. Audit logging

- `audit_logs` capture security/admin-significant events: auth changes, role grants, suspensions, plan changes, institution config, data exports/deletions — with actor, target, before/after, IP, timestamp.
- Append-only; retained per policy; queryable in the admin dashboard.

## 8. AI data privacy

- **PII minimization/redaction** before external LLM calls: strip or pseudonymize names, emails, institution identifiers where not required for the task.
- The worker sends only the minimum task payload; no end-user credentials.
- LLM responses/embeddings cached by content hash, scoped to the tenant.
- Uploaded papers/datasets used only for that user's project; never pooled across tenants without explicit institution opt-in (similarity repository).
- A clear data-use notice explains what is sent to AI providers and how to opt out of optional features.

## 9. Payment security

- No card data touches our servers — checkout is hosted by Paystack/Flutterwave.
- **Webhooks verify provider signatures** (`x-paystack-signature`, Flutterwave `verif-hash`) and are **idempotent** via `webhook_events(provider, event_id)` UNIQUE.
- Payment state is reconciled from verified webhooks, not client redirects.
- `Idempotency-Key` on checkout initiation prevents duplicate subscriptions.

## 10. Secrets management

- Local: `.env` (never committed). Staging/prod: environment injected by the platform; migrate to **SOPS/Doppler/Vault** as the team grows.
- Distinct keys per environment; key rotation procedure documented.
- JWT signing keys, internal service key, provider keys, storage creds, Sentry DSN all treated as secrets.

## 11. Transport & application hardening

- TLS 1.2+ everywhere (Let's Encrypt); HSTS.
- Security headers (CSP, X-Content-Type-Options, Referrer-Policy, frame-ancestors).
- Input validation on all DTOs; output encoding; parameterized queries (JPA) → no SQL injection.
- Editor content sanitized; ProseMirror schema constrains allowed nodes/marks.
- Dependency scanning (Dependabot) and container image scanning in CI.

## 12. Backups & disaster recovery

- **PostgreSQL:** nightly base backups + continuous WAL archiving to off-box object storage (pgBackRest/WAL-G). RPO ≤ 15 min.
- **Object storage:** bucket versioning + lifecycle; cross-region copy for prod.
- **Restore drills:** periodic test restores into staging; documented runbook. RTO ≤ 2 h.
- Configuration/IaC and secrets-recovery procedures documented.

## 13. Data retention & DSAR

- Retention policy per data class (account, documents, papers, survey responses, logs).
- Users can export their data and request deletion; deletion cascades soft→hard per policy and statutory timelines.
- Survey respondent data retained with consent record; deletable on request.

## 14. Compliance frameworks

- **Nigeria Data Protection Act 2023 / NDPR** as the primary framework: lawful basis, consent, data minimization, retention, subject rights, breach notification.
- **POPIA** (South Africa) and **GDPR** (diaspora users) honored where applicable.
- **Data residency:** prefer in-region/near-region storage; residency commitments captured per institution contract.

## 15. Institution privacy

- Institution data (templates, rosters, repositories, project stats) visible only within that tenant.
- Cross-institution analytics are aggregate/anonymized only.
- Institution repository contribution to similarity is explicit opt-in.

## 16. Academic integrity positioning (security-relevant)

- The platform's integrity stance is enforced technically, not just in copy: guardrails (no ghostwriting / no fabricated citations or data), the hash-chained **disclosure ledger**, and the exportable disclosure statement.
- This is both an ethical commitment and risk mitigation: it gives institutions evidence of *how* AI was used and protects students and the platform from misuse claims.

## 17. Incident response

- Severity classification, on-call escalation, and communication templates documented.
- Sentry alerts + uptime alerts route to on-call.
- Post-incident reviews with action items tracked.
