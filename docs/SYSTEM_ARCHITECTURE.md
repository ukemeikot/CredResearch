# System Architecture — CredResearch

Related: [Technical Specification](./TECHNICAL_SPECIFICATION.md) · [AI System Design](./AI_SYSTEM_DESIGN.md) · [Deployment](./DEPLOYMENT_AND_INFRASTRUCTURE.md)

## 1. High-level system diagram

```mermaid
flowchart TB
    subgraph Client
        WEB["Next.js Web App (PWA)\nTiptap editor, TanStack Query"]
    end

    NGINX["Nginx reverse proxy\nTLS / Let's Encrypt"]

    subgraph Core["Spring Boot modular monolith (Java 21)"]
        API["REST API /api/v1"]
        SEC["Spring Security\nJWT + RBAC + TenantContext"]
        MOD["Modules: identity, org, project, template,\ndocument, review, paper, citation, ai,\nalignment, billing, notification, disclosure, admin"]
    end

    subgraph Worker["AI/Data Worker (Python FastAPI)"]
        AIW["LLM gateway, RAG, parsing,\nembeddings, export, analysis"]
    end

    subgraph Data
        PG[("PostgreSQL + pgvector\n+ full-text search")]
        REDIS[("Redis\ncache, rate limit, jobs")]
        OBJ[("Object storage\nMinIO / S3 / R2")]
    end

    subgraph External
        LLM["LLM providers\n(via gateway)"]
        PAY["Paystack / Flutterwave"]
        MSG["Email / SMS / WhatsApp"]
        GOT["Gotenberg / LibreOffice"]
    end

    WEB -->|HTTPS| NGINX --> API
    API --> SEC --> MOD
    MOD --> PG
    MOD --> REDIS
    MOD -->|signed PUT/GET URLs| OBJ
    MOD -->|signed internal JWT| AIW
    AIW --> PG
    AIW --> OBJ
    AIW -->|routed, cached| LLM
    AIW --> GOT
    MOD -->|webhooks verify+idempotent| PAY
    MOD --> MSG
```

## 2. Request lifecycle (synchronous read/write)

```mermaid
sequenceDiagram
    participant U as Web App
    participant N as Nginx
    participant A as Backend API
    participant S as Security/Tenant
    participant D as PostgreSQL
    U->>N: HTTPS request + Bearer JWT
    N->>A: forward (+ X-Request-Id)
    A->>S: validate JWT, resolve roles + institution_id
    S-->>A: TenantContext
    A->>A: @PreAuthorize permission check
    A->>D: tenant-scoped query/mutation
    D-->>A: rows
    A-->>U: problem+json on error / JSON on success
```

## 3. AI task lifecycle (async, gateway + cache)

```mermaid
sequenceDiagram
    participant U as Web App
    participant A as Backend
    participant R as Redis (jobs/cache)
    participant W as AI Worker
    participant G as LLM Gateway
    participant L as LLM Provider
    participant D as PostgreSQL
    U->>A: POST /ai/topics (request)
    A->>A: check plan credits
    A->>D: insert ai_jobs (PENDING) + ai_requests
    A->>R: enqueue job
    A-->>U: 202 { jobId }
    W->>R: dequeue
    W->>D: ai_jobs RUNNING
    W->>G: structured prompt
    G->>R: cache lookup (prompt+model hash)
    alt cache hit
        R-->>G: cached JSON
    else miss
        G->>L: route to model (cheap/strong)
        L-->>G: JSON
        G->>R: cache store
    end
    G-->>W: validated JSON
    W->>D: ai_responses + ai_usage_logs + ai_disclosure_entries
    W->>D: ai_jobs SUCCEEDED (result_ref)
    U->>A: GET /jobs/{id} (poll)
    A-->>U: result
```

## 4. File upload lifecycle

```mermaid
sequenceDiagram
    participant U as Web App
    participant A as Backend
    participant O as Object Storage
    participant W as AI Worker
    participant D as PostgreSQL
    U->>A: POST /papers (filename, size, type)
    A->>A: check plan upload limit
    A->>O: create signed PUT URL
    A->>D: files (PENDING_UPLOAD)
    A-->>U: { uploadUrl, fileId }
    U->>O: PUT file directly (bypasses API)
    U->>A: POST /papers/{id}/ingest
    A->>D: ai_jobs (parse+embed) PENDING
    W->>O: signed GET, download
    W->>W: extract text+metadata, chunk, embed
    W->>D: papers, paper_chunks, paper_embeddings
    W->>D: ai_jobs SUCCEEDED
```

## 5. Document export lifecycle

```mermaid
sequenceDiagram
    participant U as Web App
    participant A as Backend
    participant W as AI Worker
    participant G as Gotenberg/LibreOffice
    participant O as Object Storage
    U->>A: POST /documents/{id}/export?format=docx|pdf|zip
    A->>W: export job (sections JSON + format rules + citations)
    W->>W: render ProseMirror JSON -> DOCX (python-docx)
    W->>W: render reference list via CSL
    alt PDF requested
        W->>G: DOCX/HTML -> PDF
        G-->>W: PDF
    end
    W->>W: include AI-Use Disclosure Statement
    W->>O: store export, signed GET URL
    W-->>A: artifact ref
    A-->>U: download URL
```

## 6. Supervisor review lifecycle (incl. magic-link)

```mermaid
sequenceDiagram
    participant ST as Student
    participant A as Backend
    participant N as Notification layer
    participant SV as Supervisor
    ST->>A: invite supervisor (email)
    A->>A: existing user? link : create invitation (magic-link token)
    A->>N: send invite (email/SMS)
    N-->>SV: magic-link
    ST->>A: submit section for review (review_request)
    A->>N: notify supervisor
    SV->>A: open link (token-auth, scoped)
    SV->>A: inline review_comments
    SV->>A: review_decision (APPROVED | NEEDS_REVISION | REJECTED)
    A->>N: notify student
    ST->>A: revise + resubmit (history preserved)
```

## 7. Background job lifecycle

```mermaid
stateDiagram-v2
    [*] --> PENDING
    PENDING --> RUNNING: worker dequeues
    RUNNING --> SUCCEEDED: result stored
    RUNNING --> FAILED: error (attempts < max -> requeue)
    FAILED --> PENDING: retry
    FAILED --> DEAD_LETTER: attempts exhausted
    SUCCEEDED --> [*]
    DEAD_LETTER --> [*]
```

## 8. Local development architecture

Docker Compose services: `web`, `backend`, `ai-worker`, `postgres` (pgvector image), `redis`, `minio`, `gotenberg`, `mailhog` (stub mailer). Nginx optional locally; services talk over the compose network. See [Deployment](./DEPLOYMENT_AND_INFRASTRUCTURE.md).

```mermaid
flowchart LR
    web --> backend
    backend --> postgres
    backend --> redis
    backend --> minio
    backend --> ai-worker
    ai-worker --> postgres
    ai-worker --> minio
    ai-worker --> gotenberg
    backend --> mailhog
```

## 9. Early production deployment (single VPS)

```mermaid
flowchart TB
    Internet --> NGINX["Nginx + Let's Encrypt"]
    NGINX --> WEB["web (Next.js)"]
    NGINX --> BE["backend (Spring Boot)"]
    BE --> PG[("PostgreSQL + pgvector")]
    BE --> RD[("Redis")]
    BE --> AIW["ai-worker"]
    AIW --> PG
    BE --> S3OBJ[("S3-compatible storage\nR2 / Spaces / MinIO")]
    AIW --> S3OBJ
    AIW --> GOT["gotenberg"]
    BE -. backups .-> BKP[("Off-VPS backups\npg WAL + bucket versioning")]
```

All containers on one VPS via `docker-compose.prod.yml`; object storage and backups off-box; Sentry external.

## 10. Later-scale architecture (post-MVP)

```mermaid
flowchart TB
    CDN["CDN"] --> LB["Load balancer"]
    LB --> WEB1["web (n)"]
    LB --> BE1["backend (n, stateless)"]
    BE1 --> PGM[("PostgreSQL primary")]
    PGM --> PGR[("Read replica(s)")]
    BE1 --> RDC[("Redis (HA)")]
    BE1 --> MQ[["RabbitMQ"]]
    MQ --> AIW1["ai-worker pool (n)"]
    AIW1 --> PGM
    BE1 --> OBJ[("Object storage")]
    subgraph Observability
        OTEL["OpenTelemetry"] --> PROM["Prometheus/Grafana"]
        LOKI["Loki logs"]
    end
    note1["Kubernetes only once ops maturity justifies it"]
```

Triggers to evolve: sustained AI queue depth (→ RabbitMQ + worker pool), read pressure (→ read replicas), FTS limits (→ OpenSearch/Meilisearch), and team size/ops maturity (→ Kubernetes).
