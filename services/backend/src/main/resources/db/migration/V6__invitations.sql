-- Phase 2 refinement: email invitations (org + project). Adding a project member is now an
-- email invite (token/magic-link), not a raw user id. See docs/DATABASE_SCHEMA.md (invitations).
-- status stored as text: PENDING | ACCEPTED | EXPIRED | REVOKED (V2/V4 enum-as-text style).

CREATE TABLE invitations (
    id                uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    institution_id    uuid NOT NULL REFERENCES institutions(id),
    project_id        uuid REFERENCES projects(id),   -- null = org-level invite
    email             citext NOT NULL,
    role_code         text NOT NULL,                  -- project: OWNER|SUPERVISOR|CONSULTANT|VIEWER
    token_hash        text NOT NULL,
    status            text NOT NULL DEFAULT 'PENDING',
    expires_at        timestamptz NOT NULL,
    accepted_user_id  uuid REFERENCES users(id),
    created_at        timestamptz NOT NULL DEFAULT now(),
    updated_at        timestamptz NOT NULL DEFAULT now(),
    created_by        uuid
);
CREATE INDEX idx_invitations_project ON invitations (project_id);
CREATE INDEX idx_invitations_email ON invitations (email);
CREATE UNIQUE INDEX idx_invitations_token ON invitations (token_hash);
-- At most one live invite per (project, email).
CREATE UNIQUE INDEX uq_invitation_pending
    ON invitations (project_id, email) WHERE status = 'PENDING';
