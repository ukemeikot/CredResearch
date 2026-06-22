-- Phase 0 baseline: required PostgreSQL extensions (see TECHNICAL_SPECIFICATION.md).
-- pgvector for embeddings/RAG; citext for case-insensitive emails/identifiers.
CREATE EXTENSION IF NOT EXISTS vector;
CREATE EXTENSION IF NOT EXISTS citext;
