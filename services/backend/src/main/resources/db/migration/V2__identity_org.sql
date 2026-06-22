-- Phase 1: Identity & Organization (auth, RBAC, multi-tenancy).
-- See docs/DATABASE_SCHEMA.md. App generates UUID v7 PKs; gen_random_uuid() is a
-- fallback default for seed/direct inserts.
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- ── Tenancy root ─────────────────────────────────────────────────────────────
CREATE TABLE institutions (
    id                 uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    name               text NOT NULL,
    country            char(2),
    type               text,
    logo_file_id       uuid,
    is_personal_tenant boolean NOT NULL DEFAULT false,
    status             text NOT NULL DEFAULT 'active',
    created_at         timestamptz NOT NULL DEFAULT now(),
    updated_at         timestamptz NOT NULL DEFAULT now(),
    deleted_at         timestamptz
);

CREATE TABLE departments (
    id             uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    institution_id uuid NOT NULL REFERENCES institutions(id),
    name           text NOT NULL,
    code           text,
    created_at     timestamptz NOT NULL DEFAULT now(),
    updated_at     timestamptz NOT NULL DEFAULT now(),
    deleted_at     timestamptz,
    CONSTRAINT uq_department_name UNIQUE (institution_id, name)
);

-- ── Users ────────────────────────────────────────────────────────────────────
CREATE TABLE users (
    id                uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    institution_id    uuid NOT NULL REFERENCES institutions(id),
    department_id     uuid REFERENCES departments(id),
    email             citext NOT NULL,
    password_hash     text,
    full_name         text,
    email_verified_at timestamptz,
    academic_level    text,
    field_of_study    text,
    orcid             text,
    status            text NOT NULL DEFAULT 'active',
    created_at        timestamptz NOT NULL DEFAULT now(),
    updated_at        timestamptz NOT NULL DEFAULT now(),
    created_by        uuid,
    updated_by        uuid,
    deleted_at        timestamptz,
    CONSTRAINT uq_users_email UNIQUE (email)
);
CREATE INDEX idx_users_institution ON users (institution_id);

-- ── RBAC ─────────────────────────────────────────────────────────────────────
CREATE TABLE roles (
    id          uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    code        text NOT NULL UNIQUE,
    description text,
    created_at  timestamptz NOT NULL DEFAULT now(),
    updated_at  timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE permissions (
    id          uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    code        text NOT NULL UNIQUE,
    description text,
    created_at  timestamptz NOT NULL DEFAULT now(),
    updated_at  timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE user_roles (
    id             uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id        uuid NOT NULL REFERENCES users(id),
    role_id        uuid NOT NULL REFERENCES roles(id),
    institution_id uuid,
    created_at     timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT uq_user_role UNIQUE (user_id, role_id)
);
CREATE INDEX idx_user_roles_user ON user_roles (user_id);

CREATE TABLE role_permissions (
    id            uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    role_id       uuid NOT NULL REFERENCES roles(id),
    permission_id uuid NOT NULL REFERENCES permissions(id),
    CONSTRAINT uq_role_permission UNIQUE (role_id, permission_id)
);

-- ── Tokens ───────────────────────────────────────────────────────────────────
CREATE TABLE refresh_tokens (
    id         uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id    uuid NOT NULL REFERENCES users(id),
    token_hash text NOT NULL,
    device     text,
    user_agent text,
    expires_at timestamptz NOT NULL,
    revoked_at timestamptz,
    created_at timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX idx_refresh_tokens_user ON refresh_tokens (user_id);
CREATE INDEX idx_refresh_tokens_hash ON refresh_tokens (token_hash);

-- Single-use, expiring tokens for email verification and password reset.
CREATE TABLE auth_tokens (
    id         uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id    uuid NOT NULL REFERENCES users(id),
    type       text NOT NULL,            -- EMAIL_VERIFY | PASSWORD_RESET
    token_hash text NOT NULL,
    expires_at timestamptz NOT NULL,
    used_at    timestamptz,
    created_at timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX idx_auth_tokens_hash ON auth_tokens (token_hash);
CREATE INDEX idx_auth_tokens_user ON auth_tokens (user_id);

-- ── Audit ────────────────────────────────────────────────────────────────────
CREATE TABLE audit_logs (
    id            uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    institution_id uuid,
    actor_user_id uuid,
    action        text NOT NULL,
    target_type   text,
    target_id     uuid,
    metadata      jsonb,
    ip            text,
    created_at    timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX idx_audit_logs_institution ON audit_logs (institution_id, created_at);
CREATE INDEX idx_audit_logs_actor ON audit_logs (actor_user_id);

-- ── Invitations & feature flags (tables now; mapped in later phases) ──────────
CREATE TABLE invitations (
    id                uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    institution_id    uuid REFERENCES institutions(id),
    project_id        uuid,
    email             citext,
    role_code         text,
    token_hash        text,
    status            text NOT NULL DEFAULT 'PENDING',
    expires_at        timestamptz,
    accepted_user_id  uuid REFERENCES users(id),
    created_at        timestamptz NOT NULL DEFAULT now(),
    updated_at        timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE feature_flags (
    id             uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    key            text NOT NULL UNIQUE,
    enabled        boolean NOT NULL DEFAULT false,
    institution_id uuid,
    plan_code      text,
    metadata       jsonb,
    created_at     timestamptz NOT NULL DEFAULT now(),
    updated_at     timestamptz NOT NULL DEFAULT now()
);
