-- Phase 5 (FR-LIT-8): RAG over uploaded papers. Chunked paper text + embeddings (pgvector) so
-- questions are answered grounded in the project's own corpus. Embedding dim = 768 (nomic-embed-text).
CREATE TABLE IF NOT EXISTS paper_chunks (
    id          UUID PRIMARY KEY,
    paper_id    UUID NOT NULL,
    project_id  UUID NOT NULL,
    chunk_index INT  NOT NULL,
    content     TEXT NOT NULL,
    embedding   vector(768)
);

CREATE INDEX IF NOT EXISTS idx_paper_chunks_project ON paper_chunks (project_id);
