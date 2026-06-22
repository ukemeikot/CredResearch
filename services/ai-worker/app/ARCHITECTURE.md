# AI/Data Worker — Clean Architecture (FastAPI)

Stateless Python service for AI, RAG, parsing, export and analysis. It accepts only calls signed
with a short-lived internal JWT from the backend (ADR-016) and never holds end-user tokens.

Dependency rule points inward: `interfaces → application → domain`; `infrastructure` implements the
`domain/port` interfaces. `domain` and `application` import no FastAPI, SDK, or DB code.

```
app/
├── domain/
│   ├── model/            # Domain types (e.g. Chunk, Summary, AlignmentReport, ExportSpec)
│   └── port/             # Interfaces: LlmGateway, EmbeddingPort, StoragePort, RepositoryPort...
├── application/
│   ├── usecase/          # generate_topics, summarize_paper, run_rag, build_matrix, export_docx...
│   └── dto/              # command/result objects
├── infrastructure/
│   ├── llm/              # provider-agnostic gateway: routing, Redis cache, retry/fallback (ADR-013)
│   ├── rag/              # retrieval over pgvector, grounding, cite-only-retrieved
│   ├── parsing/          # PDF/DOCX extraction, OCR fallback, chunking
│   ├── export/           # ProseMirror JSON -> DOCX (python-docx); Gotenberg PDF; CSL refs
│   ├── storage/          # S3/MinIO signed GET/PUT client
│   ├── persistence/      # SQLAlchemy/psycopg adapters implementing RepositoryPort
│   ├── security/         # internal-JWT verification middleware
│   └── config/           # settings, DI wiring
├── interfaces/
│   ├── routers/          # FastAPI routers — thin, call use cases
│   ├── schemas/          # Pydantic request/response models
│   └── middleware/       # request-id, auth, error -> problem+json
└── main.py               # app factory, wiring, /health
```

Guardrails (FR-AI-7) and JSON-schema validation + repair-retry live in `application/usecase` and the
`llm` adapter. Prompt templates (versioned) live in `../prompts`.
