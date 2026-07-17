-- Phase 4 completion: AI usage tracking, per-plan credits, and the AI-Use Disclosure Ledger.
-- See docs/DATABASE_SCHEMA.md (AI & disclosure) + FUNCTIONAL_REQUIREMENTS (FR-AI, FR-LEDGER).
-- Idempotent (IF NOT EXISTS + guarded seed); enums stored as text (V2/V4 convention).

-- ── Plans (per-plan monthly AI credits) ─────────────────────────────────────
CREATE TABLE IF NOT EXISTS plans (
    id                 uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    code               text NOT NULL UNIQUE,
    name               text NOT NULL,
    ai_monthly_credits int  NOT NULL DEFAULT 50,
    price_minor        bigint NOT NULL DEFAULT 0,
    currency           char(3) NOT NULL DEFAULT 'USD',
    metadata           jsonb,
    created_at         timestamptz NOT NULL DEFAULT now()
);

INSERT INTO plans (code, name, ai_monthly_credits, price_minor) VALUES
  ('FREE',        'Free',            50,   0),
  ('STUDENT',     'Student',        500,   0),
  ('INSTITUTION', 'Institution',  5000,    0)
ON CONFLICT (code) DO NOTHING;

-- ── AI requests / responses (usage tracking) ────────────────────────────────
CREATE TABLE IF NOT EXISTS ai_requests (
    id             uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    institution_id uuid,
    project_id     uuid REFERENCES projects(id),
    document_id    uuid REFERENCES documents(id),
    user_id        uuid NOT NULL REFERENCES users(id),
    feature_key    text NOT NULL,
    model          text,
    status         text NOT NULL DEFAULT 'OK',   -- OK | ERROR
    created_at     timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_ai_requests_user ON ai_requests (user_id, created_at);
CREATE INDEX IF NOT EXISTS idx_ai_requests_project ON ai_requests (project_id);

CREATE TABLE IF NOT EXISTS ai_responses (
    id            uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    ai_request_id uuid NOT NULL UNIQUE REFERENCES ai_requests(id),
    output_json   jsonb,
    finish_reason text,
    created_at    timestamptz NOT NULL DEFAULT now()
);

-- ── AI-Use Disclosure Ledger (append-only, hash-chained per document) ───────
CREATE TABLE IF NOT EXISTS ai_disclosure_entries (
    id                  uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    document_id         uuid NOT NULL REFERENCES documents(id),
    document_section_id uuid REFERENCES document_sections(id),
    ai_request_id       uuid REFERENCES ai_requests(id),
    user_id             uuid REFERENCES users(id),
    feature_key         text,
    model               text,
    suggestion_summary  text,
    action              text NOT NULL DEFAULT 'accepted',  -- accepted | edited | rejected
    prev_hash           text,
    entry_hash          text NOT NULL,
    created_at          timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_disclosure_document ON ai_disclosure_entries (document_id, created_at);
