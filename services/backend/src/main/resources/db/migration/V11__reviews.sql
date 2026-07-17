-- Phase 6 — Supervisor Review & Collaboration (FR-SUP-3..9). Submit → review → decide → revise.
-- reviewer_user_id null (+ reviewer_email set) reserves the row for a magic-link external reviewer
-- (wired in the next increment); author_user_id null likewise marks an external reviewer's comment.

CREATE TABLE IF NOT EXISTS review_requests (
    id                  UUID PRIMARY KEY,
    document_id         UUID NOT NULL,
    document_section_id UUID,                       -- null = whole document
    requested_by        UUID NOT NULL,
    reviewer_user_id    UUID,                        -- null = external (magic-link)
    reviewer_email      TEXT,
    status              TEXT NOT NULL DEFAULT 'PENDING',  -- PENDING|APPROVED|NEEDS_REVISION|REJECTED
    note                TEXT,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    decided_at          TIMESTAMPTZ
);
CREATE INDEX IF NOT EXISTS idx_review_requests_document ON review_requests (document_id, created_at);
CREATE INDEX IF NOT EXISTS idx_review_requests_reviewer ON review_requests (reviewer_user_id, status);

CREATE TABLE IF NOT EXISTS review_comments (
    id                UUID PRIMARY KEY,
    review_request_id UUID NOT NULL,
    author_user_id    UUID,                          -- null = external reviewer
    author_label      TEXT,
    anchor_start      INT,
    anchor_end        INT,
    quote             TEXT,
    body              TEXT NOT NULL,
    resolved          BOOLEAN NOT NULL DEFAULT FALSE,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_review_comments_request ON review_comments (review_request_id, created_at);

CREATE TABLE IF NOT EXISTS review_decisions (
    id                UUID PRIMARY KEY,
    review_request_id UUID NOT NULL,
    decision          TEXT NOT NULL,                 -- APPROVED|NEEDS_REVISION|REJECTED
    summary           TEXT,
    decided_by        UUID,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_review_decisions_request ON review_decisions (review_request_id, created_at);
