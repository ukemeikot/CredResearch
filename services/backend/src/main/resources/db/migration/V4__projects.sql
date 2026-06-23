-- Phase 2: Project Workspace (projects, members, milestones, activities, status history).
-- See docs/DATABASE_SCHEMA.md (Projects) and FUNCTIONAL_REQUIREMENTS.md §2 (FR-PROJ-1..7).
-- App generates UUID v7 PKs; gen_random_uuid() is a fallback default for seed/direct inserts.
-- Status enums are stored as text (validated in the application state machine), matching V2 style.

-- ── Projects ─────────────────────────────────────────────────────────────────
CREATE TABLE projects (
    id             uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    institution_id uuid NOT NULL REFERENCES institutions(id),
    department_id  uuid REFERENCES departments(id),
    owner_user_id  uuid REFERENCES users(id),
    title          text NOT NULL,
    level          text,
    status         text NOT NULL DEFAULT 'DRAFT',
    abstract       text,
    search_tsv     tsvector,
    created_at     timestamptz NOT NULL DEFAULT now(),
    updated_at     timestamptz NOT NULL DEFAULT now(),
    created_by     uuid,
    updated_by     uuid,
    deleted_at     timestamptz
);
CREATE INDEX idx_projects_institution_status ON projects (institution_id, status);
CREATE INDEX idx_projects_search_tsv ON projects USING gin (search_tsv);

-- ── Project members (project-roles) ──────────────────────────────────────────
CREATE TABLE project_members (
    id         uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id uuid NOT NULL REFERENCES projects(id),
    user_id    uuid NOT NULL REFERENCES users(id),
    role       text NOT NULL,   -- OWNER | SUPERVISOR | CONSULTANT | VIEWER
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    deleted_at timestamptz,
    CONSTRAINT uq_project_member UNIQUE (project_id, user_id)
);
CREATE INDEX idx_project_members_project ON project_members (project_id);
CREATE INDEX idx_project_members_user ON project_members (user_id);

-- ── Milestones ───────────────────────────────────────────────────────────────
CREATE TABLE project_milestones (
    id           uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id   uuid NOT NULL REFERENCES projects(id),
    title        text NOT NULL,
    due_date     date,
    status       text NOT NULL DEFAULT 'PENDING',
    completed_at timestamptz,
    created_at   timestamptz NOT NULL DEFAULT now(),
    updated_at   timestamptz NOT NULL DEFAULT now(),
    deleted_at   timestamptz
);
CREATE INDEX idx_project_milestones_project ON project_milestones (project_id);
CREATE INDEX idx_project_milestones_due ON project_milestones (due_date);

-- ── Activity feed ────────────────────────────────────────────────────────────
CREATE TABLE project_activities (
    id            uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id    uuid NOT NULL REFERENCES projects(id),
    actor_user_id uuid,
    type          text NOT NULL,
    payload       jsonb,
    created_at    timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX idx_project_activities_project ON project_activities (project_id, created_at);

-- ── Status history ───────────────────────────────────────────────────────────
CREATE TABLE project_status_history (
    id          uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id  uuid NOT NULL REFERENCES projects(id),
    from_status text,
    to_status   text NOT NULL,
    changed_by  uuid,
    created_at  timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX idx_project_status_history_project ON project_status_history (project_id, created_at);
