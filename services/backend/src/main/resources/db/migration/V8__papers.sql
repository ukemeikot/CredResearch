-- Phase 5 — Papers, Literature Review & Citations (FR-LIT-*).
-- First increment: uploaded papers with extracted text + bibliographic metadata, from which a
-- formatted reference list is rendered. (Chunks/embeddings for RAG and in-text citations follow.)

CREATE TABLE IF NOT EXISTS papers (
    id                UUID PRIMARY KEY,
    project_id        UUID NOT NULL,
    uploaded_by       UUID,
    filename          TEXT,
    title             TEXT,
    authors           TEXT,
    year              INT,
    doi               TEXT,
    journal           TEXT,
    extraction_status TEXT NOT NULL DEFAULT 'PENDING',
    text_content      TEXT,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_papers_project ON papers (project_id, created_at);
