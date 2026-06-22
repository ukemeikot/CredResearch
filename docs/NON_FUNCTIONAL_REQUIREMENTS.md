# Non-Functional Requirements — CredResearch

Related: [Functional Requirements](./FUNCTIONAL_REQUIREMENTS.md) · [Security & Compliance](./SECURITY_AND_COMPLIANCE.md) · [Deployment](./DEPLOYMENT_AND_INFRASTRUCTURE.md)

IDs: `NFR-<CATEGORY>-<n>`.

## 1. Performance & latency budgets

| ID | Requirement | Target |
|---|---|---|
| NFR-PERF-1 | p95 latency for non-AI REST endpoints | < 500 ms |
| NFR-PERF-2 | p99 latency for non-AI REST endpoints | < 1.2 s |
| NFR-PERF-3 | Autosave round-trip | < 400 ms |
| NFR-PERF-4 | AI assistant task (single-shot) p95 | < 12 s |
| NFR-PERF-5 | Paper ingestion (extract+embed) per 20-page PDF | < 60 s async |
| NFR-PERF-6 | DOCX export p95 | < 8 s |
| NFR-PERF-7 | First Contentful Paint on mid-range Android over 3G | < 3 s |
| NFR-PERF-8 | Initial JS payload (gzipped) for editor route | < 350 KB |

## 2. Bandwidth & connectivity (African-market constraints)

| ID | Requirement |
|---|---|
| NFR-BW-1 | The web app is fully usable on a low-end Android device over a 3G connection. |
| NFR-BW-2 | PWA-installable; static shell cached; route-level code splitting. |
| NFR-BW-3 | Autosave buffers locally when offline and flushes on reconnect (no data loss on a dropped connection). |
| NFR-BW-4 | API responses are paginated and field-trimmed; avoid over-fetching; support `?fields=` projection on heavy resources. |
| NFR-BW-5 | Large assets (papers, exports) delivered via signed URLs directly from object storage, not proxied through the API. |
| NFR-BW-6 | Images and uploads compressed; export downloads resumable where the CDN supports range requests. |

## 3. Availability & reliability

| ID | Requirement | Target |
|---|---|---|
| NFR-AVAIL-1 | MVP single-VPS availability | ≥ 99.0% monthly |
| NFR-AVAIL-2 | Post-MVP multi-node availability | ≥ 99.5% monthly |
| NFR-AVAIL-3 | RPO (max data loss) | ≤ 15 min (WAL archiving) |
| NFR-AVAIL-4 | RTO (recovery time) | ≤ 2 h (documented runbook) |
| NFR-AVAIL-5 | Graceful degradation: if the AI worker is down, core editing/review/export still work. |

## 4. Scalability

| ID | Requirement |
|---|---|
| NFR-SCALE-1 | MVP target: 5,000 registered users, 500 concurrent, on a single well-sized VPS. |
| NFR-SCALE-2 | Stateless backend and worker (sessions in Redis/JWT) so horizontal scaling needs no code change. |
| NFR-SCALE-3 | AI worker scales independently of the core backend. |
| NFR-SCALE-4 | pgvector index (HNSW) sized for ≥ 5M chunks before considering a dedicated vector store. |

## 5. Accessibility (a11y)

| ID | Requirement |
|---|---|
| NFR-A11Y-1 | Conform to WCAG 2.1 AA for all primary flows. |
| NFR-A11Y-2 | Full keyboard navigation; visible focus states. |
| NFR-A11Y-3 | Screen-reader labels on all interactive controls and editor toolbar. |
| NFR-A11Y-4 | Colour contrast ≥ 4.5:1 for text; never colour-only status signalling. |
| NFR-A11Y-5 | Respect OS "reduce motion"; honor dynamic type / browser zoom to 200%. |
| NFR-A11Y-6 | Minimum touch target 44×44 px. |

## 6. Internationalization (i18n)

| ID | Requirement |
|---|---|
| NFR-I18N-1 | English at launch; UI strings externalized for translation (no hard-coded copy). |
| NFR-I18N-2 | Design for French, Portuguese, Arabic (RTL-ready layout) in roadmap. |
| NFR-I18N-3 | Embedding model and RAG handle multilingual content. |
| NFR-I18N-4 | Locale-aware dates, numbers, and currency formatting. |

## 7. Security & privacy (summary; full detail in Security doc)

| ID | Requirement |
|---|---|
| NFR-SEC-1 | All traffic over TLS 1.2+. |
| NFR-SEC-2 | Argon2id password hashing; refresh tokens stored hashed and rotated. |
| NFR-SEC-3 | Strict tenant isolation; automated tests assert no cross-tenant access. |
| NFR-SEC-4 | NDPA 2023 / NDPR compliance; data minimization; documented retention. |
| NFR-SEC-5 | PII redacted/pseudonymized before external LLM calls where not strictly needed. |

## 8. Observability

| ID | Requirement |
|---|---|
| NFR-OBS-1 | Sentry error capture on frontend, backend, worker from day one. |
| NFR-OBS-2 | Structured JSON logs with correlation/request IDs propagated across services. |
| NFR-OBS-3 | Actuator health/readiness; uptime monitoring with alerting. |
| NFR-OBS-4 | AI cost/token usage tracked per request and aggregated per plan. |
| NFR-OBS-5 | OpenTelemetry traces, Prometheus/Grafana, Loki are roadmap (post-MVP). |

## 9. Maintainability & quality

| ID | Requirement |
|---|---|
| NFR-MAINT-1 | Modular monolith with clear module boundaries (package-by-feature). |
| NFR-MAINT-2 | OpenAPI 3.1 is the source of truth; client types generated from it. |
| NFR-MAINT-3 | Backend coverage ≥ 70% on service/domain layers; critical paths covered by integration tests. |
| NFR-MAINT-4 | AI prompts versioned and covered by golden/eval tests in CI (see Test Strategy). |
| NFR-MAINT-5 | All migrations via Flyway; no manual schema edits in any environment. |

## 10. Cost efficiency

| ID | Requirement |
|---|---|
| NFR-COST-1 | AI gross margin ≥ 60% via model routing, caching, and per-plan credit caps. |
| NFR-COST-2 | Embeddings cached and deduplicated; never re-embed unchanged content. |
| NFR-COST-3 | Object storage lifecycle rules archive/expire stale exports and orphaned uploads. |

## 11. Compliance & data residency

| ID | Requirement |
|---|---|
| NFR-COMP-1 | Comply with Nigeria Data Protection Act 2023 / NDPR; POPIA and GDPR where applicable. |
| NFR-COMP-2 | Prefer storage regions close to users; document residency stance per institution contract. |
| NFR-COMP-3 | Survey respondent consent captured and retained with responses. |
| NFR-COMP-4 | Data export and deletion (DSAR) supported within statutory timelines. |
