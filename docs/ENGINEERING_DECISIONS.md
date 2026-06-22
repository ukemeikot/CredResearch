# Engineering Decisions (ADRs) — CredResearch

Related: [Technical Specification](./TECHNICAL_SPECIFICATION.md) · [System Architecture](./SYSTEM_ARCHITECTURE.md) · [AI System Design](./AI_SYSTEM_DESIGN.md)

Each ADR: **Decision · Context · Options · Final choice · Reason · Consequences · Future migration path.** Status: Accepted unless noted.

---

## ADR-001 — Java Spring Boot for the core backend
- **Context:** Need a robust, well-typed, transactional core API with strong security and data tooling; team comfort with JVM.
- **Options:** Spring Boot (Java 21); NestJS (Node/TS); Django; Go.
- **Final choice:** Spring Boot.
- **Reason:** Mature security (Spring Security), JPA/Flyway, transactions, and a strong concurrency story for a transactional academic workflow; clean modular-monolith support.
- **Consequences:** Higher baseline memory than Node; more boilerplate. Acceptable for the reliability gained.
- **Migration path:** Module boundaries allow extracting a service later if ever needed.

## ADR-002 — Modular monolith over microservices (MVP)
- **Context:** Small team, early product, need velocity and simple ops.
- **Options:** Modular monolith; microservices.
- **Final choice:** Modular monolith (one Java deployable) + one separate Python worker.
- **Reason:** Microservices add network, deployment, and data-consistency overhead with no MVP benefit. Module seams keep future extraction cheap.
- **Consequences:** One scaling unit for core; discipline needed to keep module boundaries clean (no cross-module table access).
- **Migration path:** Extract a hot module into its own service when a clear scaling/ownership boundary emerges.

## ADR-003 — Python FastAPI worker for AI/data/document tasks
- **Context:** AI, embeddings, PDF/DOCX parsing, stats, and rendering live in the Python ecosystem.
- **Options:** Do it in Java; separate Python worker; third-party only.
- **Final choice:** Separate FastAPI worker.
- **Reason:** Best-in-class Python libraries (parsing, pandas, python-docx, ML clients); isolates AI cost/latency; scales independently; keeps the Java core lean.
- **Consequences:** Two languages, a service contract, and internal auth to maintain.
- **Migration path:** HTTP now → queue-based later; can split into multiple workers by task type.

## ADR-004 — PostgreSQL + pgvector over a separate vector DB (MVP)
- **Context:** Need relational data + vector search for RAG.
- **Options:** Postgres + pgvector; Postgres + dedicated vector DB (Pinecone/Weaviate/Qdrant).
- **Final choice:** Postgres + pgvector (HNSW).
- **Reason:** One database to operate/back up; transactional consistency between content and vectors; sufficient at MVP scale.
- **Consequences:** Vector scale ceiling lower than specialized stores; tuning HNSW needed.
- **Migration path:** Move embeddings to a dedicated vector DB when corpus/latency outgrows pgvector (≳ several million chunks).

## ADR-005 — Shared-schema multi-tenancy over DB-per-tenant
- **Context:** Many institutions/independent users; need isolation without operational explosion.
- **Options:** Shared DB/shared schema with `institution_id`; schema-per-tenant; DB-per-tenant.
- **Final choice:** Shared schema with mandatory `institution_id` filtering.
- **Reason:** Simplest to operate, migrate, and back up; isolation enforced in code + tested.
- **Consequences:** Tenant isolation is an application responsibility → must be rigorously tested.
- **Migration path:** Largest institutions can be promoted to a dedicated schema/DB if contractually required.

## ADR-006 — Tiptap editor over a plain textarea
- **Context:** Academic editing needs structure, citations, headings, versioning.
- **Options:** Plain textarea/markdown; Tiptap (ProseMirror); other rich editors.
- **Final choice:** Tiptap.
- **Reason:** Structured ProseMirror model, inline citation marks, extensible, transformable to DOCX/PDF.
- **Consequences:** More frontend complexity; schema versioning required.
- **Migration path:** ProseMirror schema migrations; Yjs collaborative layer can be added later.

## ADR-007 — Structured JSON document model (ProseMirror) over HTML
- **Context:** Documents must be queryable, transformable, and safe.
- **Options:** Store HTML; store ProseMirror JSON; store markdown.
- **Final choice:** ProseMirror JSON (`jsonb`) + flattened text for FTS/similarity.
- **Reason:** Clean transforms to DOCX/PDF, inline citation marks as first-class nodes, sanitizable, diffable for versions.
- **Consequences:** Need a JSON→DOCX renderer and schema discipline.
- **Migration path:** Versioned schema; lazy migration on document open.

## ADR-008 — MinIO/S3-compatible storage
- **Context:** Need affordable, portable object storage with signed URLs.
- **Options:** Cloud-specific (AWS S3 only); S3-compatible abstraction (MinIO/R2/Spaces).
- **Final choice:** S3-compatible everywhere (MinIO local; R2/Spaces/S3/MinIO prod).
- **Reason:** Avoids lock-in; cheap egress options (R2) matter in a cost-sensitive market; identical API across envs.
- **Consequences:** Must keep to the common S3 feature set.
- **Migration path:** Swap providers by config; cross-region/CDN as scale grows.

## ADR-009 — Redis queue first (RabbitMQ later)
- **Context:** Need background-job coordination for AI/export/analysis.
- **Options:** Redis lists/streams; RabbitMQ; Kafka.
- **Final choice:** Redis at MVP (already present for cache/rate-limit); RabbitMQ later; **no Kafka** at MVP.
- **Reason:** One fewer system to run; sufficient for MVP throughput; clean upgrade path.
- **Consequences:** Weaker delivery guarantees than a real broker; mitigated by `ai_jobs` status + idempotency + DLQ.
- **Migration path:** Introduce RabbitMQ when queue depth/reliability demands it; Kafka only with a strong event-streaming case.

## ADR-010 — DOCX-first export, then PDF
- **Context:** Students need editable Word output and a fixed PDF.
- **Options:** PDF-only; DOCX-only; both (DOCX-first).
- **Final choice:** DOCX first (python-docx), then PDF via Gotenberg/LibreOffice.
- **Reason:** DOCX is what supervisors/institutions actually mark up; PDF derives cleanly from it.
- **Consequences:** Maintain a DOCX renderer + a PDF conversion service.
- **Migration path:** Add LaTeX/templated styles per institution later.

## ADR-011 — Basic internal similarity engine first (not Turnitin)
- **Context:** Originality awareness needed; full plagiarism detection is out of reach/scope.
- **Options:** Integrate Turnitin; build internal pre-check; nothing.
- **Final choice:** Internal pre-check (own docs + opt-in institution repository); explicitly not Turnitin-equivalent.
- **Reason:** Useful, honest, affordable; avoids overpromising and licensing cost.
- **Consequences:** Limited corpus → limited recall; must message clearly.
- **Migration path:** Optional third-party integration for institutions that require it.

## ADR-012 — Docker Compose before Kubernetes
- **Context:** Small team, single-region MVP.
- **Options:** Kubernetes now; Docker Compose on a VPS.
- **Final choice:** Docker Compose on a VPS for MVP.
- **Reason:** K8s ops overhead is unjustified early; Compose is enough for the target load.
- **Consequences:** Manual scaling; HA limited at MVP.
- **Migration path:** Containers are K8s-ready; adopt K8s once ops maturity/scale justify it.

---

## ADR-013 — Provider-agnostic LLM gateway (no single-vendor lock-in)
- **Context:** AI cost is existential in a price-sensitive market; provider availability/pricing varies.
- **Options:** Hard-code one provider SDK; build a thin gateway abstraction.
- **Final choice:** Gateway with `complete()`/`embed()` interface, model routing, and caching.
- **Reason:** Swap providers/models freely, route cheap vs strong tasks, cache to protect margin, add self-hosted open models later.
- **Consequences:** Small abstraction to maintain; must normalize provider differences.
- **Migration path:** Add providers/self-hosted models behind the same interface; tune routing by cost/quality.

## ADR-014 — CSL/citeproc for citation styles
- **Context:** Need APA/IEEE/Harvard now and more later without bespoke code per style.
- **Options:** Hand-write per-style formatters; use CSL/citeproc.
- **Final choice:** CSL-JSON citation records + citeproc rendering.
- **Reason:** New styles become config (drop in a CSL file); industry-standard; interops with BibTeX/RIS/Zotero.
- **Consequences:** Must store citations as CSL-JSON and maintain a citeproc renderer.
- **Migration path:** Add any CSL style on demand; import/export reference managers.

## ADR-015 — python-docx + Gotenberg/LibreOffice for export
- **Context:** Need reliable DOCX and PDF from the structured document model.
- **Options:** Client-side export; pure-Java (docx4j); Python (python-docx) + headless converter.
- **Final choice:** python-docx in the worker for DOCX; Gotenberg/LibreOffice for PDF.
- **Reason:** Python tooling is strong and already in the worker; clean separation; consistent server-side rendering.
- **Consequences:** Run a conversion service; keep renderer in sync with the ProseMirror schema.
- **Migration path:** Templated/branded styles; LaTeX route if precise typesetting is required.

## ADR-016 — Signed internal token (or mTLS) for Java ↔ FastAPI
- **Context:** The worker must only accept calls from the backend.
- **Options:** Open internal network; shared static secret; signed short-lived internal JWT; mTLS.
- **Final choice:** Signed short-lived internal JWT (audience `ai-worker`) now; mTLS path documented.
- **Reason:** Strong, simple service auth without exposing the worker; revocable, time-bound.
- **Consequences:** Clock-skew handling and key management.
- **Migration path:** mTLS when the worker fleet scales across hosts.

## ADR-017 — Idempotent, signature-verified payment webhooks
- **Context:** Payment provider webhooks can be retried, delayed, or forged.
- **Options:** Trust client redirect; naive webhook handler; verified + idempotent handler.
- **Final choice:** Verify provider signature; persist `webhook_events(provider, event_id)` UNIQUE; reconcile state from verified events.
- **Reason:** Prevents double-charging/duplicate subscriptions and forged-event abuse.
- **Consequences:** Slightly more handling logic.
- **Migration path:** Same pattern for any future provider.

## ADR-018 — Optimistic locking for autosave
- **Context:** Single-author editing on flaky connections; possible concurrent tabs.
- **Options:** Last-write-wins; pessimistic locks; optimistic version check.
- **Final choice:** Optimistic locking via section `version`; conflict → client resolves.
- **Reason:** No lost updates without the cost/UX of locks; fits offline-buffered autosave.
- **Consequences:** Clients must handle 409 conflicts.
- **Migration path:** Yjs/CRDT real-time collaboration post-MVP (ADR-019).

## ADR-019 — Defer real-time collaborative editing (Yjs)
- **Context:** Multi-user live editing is desirable but complex; MVP editing is single-author with supervisor review.
- **Options:** Build Yjs now; defer.
- **Final choice:** Defer to post-MVP.
- **Reason:** Review-based collaboration meets MVP needs; Yjs adds infra and conflict-model complexity.
- **Consequences:** No simultaneous co-editing at MVP.
- **Migration path:** Tiptap supports a Yjs collaboration layer; add a sync server later.

## ADR-020 — AI-Use Disclosure Ledger as a core integrity feature
- **Context:** Generative AI makes ghostwriting trivial; institutions need transparency, not just claims.
- **Options:** Rely on positioning/copy; build an enforced, exportable ledger.
- **Final choice:** Hash-chained, append-only per-document ledger + exportable disclosure statement, in MVP.
- **Reason:** Turns the biggest integrity/legal risk into a differentiator and institutional selling point.
- **Consequences:** Must capture user accept/edit/reject actions and maintain tamper-evidence.
- **Migration path:** Add institution-policy templates and signed/verifiable statements later.

## ADR-021 — Notification provider abstraction (in-app + email + SMS, WhatsApp optional)
- **Context:** Regional reach needs more than email; SMS/WhatsApp matter in Nigeria.
- **Options:** Email-only; multi-channel via a provider-agnostic outbox.
- **Final choice:** `notification_outbox` fan-out across channels behind a provider abstraction (email/SES/Resend, SMS/Termii, WhatsApp optional).
- **Reason:** Reliability, reach, and provider flexibility.
- **Consequences:** Outbox processing + per-channel templates to maintain.
- **Migration path:** Swap/add providers by config; add channels as needed.

## ADR-022 — PostgreSQL full-text search before OpenSearch/Meilisearch
- **Context:** Need search over documents/papers/projects at MVP.
- **Options:** Postgres FTS; OpenSearch; Meilisearch.
- **Final choice:** Postgres FTS (`tsvector` + GIN) first.
- **Reason:** No extra infra; good enough at MVP scale; pairs with pgvector for semantic search.
- **Consequences:** Fewer relevance features than a dedicated engine.
- **Migration path:** Introduce Meilisearch/OpenSearch when relevance/scale demands it.

## ADR-023 — NDPA/NDPR-first compliance & data-residency stance
- **Context:** Nigerian primary market; African + diaspora users.
- **Options:** GDPR-only; ad hoc; NDPA-first with POPIA/GDPR overlays.
- **Final choice:** NDPA 2023 / NDPR as the baseline; POPIA/GDPR where applicable; prefer in-region storage.
- **Reason:** Matches the primary user base and institutional procurement requirements.
- **Consequences:** Consent, retention, DSAR, and residency processes to maintain.
- **Migration path:** Per-institution residency commitments; regional storage as contracts require.
