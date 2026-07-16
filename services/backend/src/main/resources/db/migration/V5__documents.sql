-- Phase 3: Template & Document Builder (templates, sections, format rules, documents,
-- document sections, version history). See docs/DATABASE_SCHEMA.md (Templates & documents)
-- and FUNCTIONAL_REQUIREMENTS.md §3 (FR-TMPL-1..3, FR-DOC-1..5).
-- Content is ProseMirror/Tiptap JSON stored as jsonb; enums stored as text (V2/V4 style).
-- App generates UUID v7 PKs; gen_random_uuid() is the seed/fallback default.

-- ── Templates ────────────────────────────────────────────────────────────────
CREATE TABLE templates (
    id             uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    institution_id uuid REFERENCES institutions(id),   -- null = global
    department_id  uuid REFERENCES departments(id),
    name           text NOT NULL,
    level          text,                                -- UG | MSc | PhD
    is_global      boolean NOT NULL DEFAULT false,
    citation_style text NOT NULL DEFAULT 'APA',
    created_at     timestamptz NOT NULL DEFAULT now(),
    updated_at     timestamptz NOT NULL DEFAULT now(),
    created_by     uuid,
    updated_by     uuid,
    deleted_at     timestamptz
);
CREATE INDEX idx_templates_institution ON templates (institution_id);

CREATE TABLE template_sections (
    id          uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    template_id uuid NOT NULL REFERENCES templates(id),
    order_index int NOT NULL,
    chapter     text,
    heading     text NOT NULL,
    guidance    text
);
CREATE INDEX idx_template_sections_template ON template_sections (template_id, order_index);

CREATE TABLE document_format_rules (
    id               uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    template_id      uuid NOT NULL REFERENCES templates(id),
    font_family      text NOT NULL DEFAULT 'Times New Roman',
    font_size_pt     numeric NOT NULL DEFAULT 12,
    line_spacing     numeric NOT NULL DEFAULT 2.0,
    margins_json     jsonb,
    heading_numbering text NOT NULL DEFAULT 'decimal',
    citation_style   text NOT NULL DEFAULT 'APA'
);
CREATE INDEX idx_format_rules_template ON document_format_rules (template_id);

-- ── Documents ────────────────────────────────────────────────────────────────
CREATE TABLE documents (
    id          uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id  uuid NOT NULL REFERENCES projects(id),
    template_id uuid NOT NULL REFERENCES templates(id),
    title       text NOT NULL,
    status      text NOT NULL DEFAULT 'DRAFT',
    search_tsv  tsvector,
    created_at  timestamptz NOT NULL DEFAULT now(),
    updated_at  timestamptz NOT NULL DEFAULT now(),
    created_by  uuid,
    updated_by  uuid,
    deleted_at  timestamptz
);
CREATE INDEX idx_documents_project ON documents (project_id);
CREATE INDEX idx_documents_search_tsv ON documents USING gin (search_tsv);

CREATE TABLE document_sections (
    id           uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    document_id  uuid NOT NULL REFERENCES documents(id),
    order_index  int NOT NULL,
    chapter      text,
    heading      text NOT NULL,
    content      jsonb,          -- ProseMirror/Tiptap JSON
    content_text text,           -- flattened text (FTS/similarity)
    version      int NOT NULL DEFAULT 1,   -- optimistic lock
    search_tsv   tsvector,
    created_at   timestamptz NOT NULL DEFAULT now(),
    updated_at   timestamptz NOT NULL DEFAULT now(),
    updated_by   uuid
);
CREATE INDEX idx_document_sections_document ON document_sections (document_id, order_index);
CREATE INDEX idx_document_sections_search_tsv ON document_sections USING gin (search_tsv);

CREATE TABLE document_versions (
    id                  uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    document_section_id uuid NOT NULL REFERENCES document_sections(id),
    version             int NOT NULL,
    content             jsonb,
    content_text        text,
    authored_by         uuid,
    created_at          timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT uq_document_version UNIQUE (document_section_id, version)
);
CREATE INDEX idx_document_versions_section ON document_versions (document_section_id, version);

-- ── Seed: three global templates (UG / MSc / PhD), Proposal + Ch1–3 (FR-TMPL-1/3) ──
INSERT INTO templates (id, institution_id, name, level, is_global, citation_style) VALUES
  ('11111111-1111-1111-1111-111111111111', NULL, 'Undergraduate Project',       'UG',  true, 'APA'),
  ('22222222-2222-2222-2222-222222222222', NULL, 'Master''s Dissertation',      'MSc', true, 'APA'),
  ('33333333-3333-3333-3333-333333333333', NULL, 'PhD Thesis',                  'PhD', true, 'IEEE');

INSERT INTO document_format_rules (template_id, font_family, font_size_pt, line_spacing, heading_numbering, citation_style, margins_json) VALUES
  ('11111111-1111-1111-1111-111111111111', 'Times New Roman', 12, 2.0, 'decimal', 'APA',  '{"top":1.0,"bottom":1.0,"left":1.5,"right":1.0,"unit":"in"}'),
  ('22222222-2222-2222-2222-222222222222', 'Times New Roman', 12, 2.0, 'decimal', 'APA',  '{"top":1.0,"bottom":1.0,"left":1.5,"right":1.0,"unit":"in"}'),
  ('33333333-3333-3333-3333-333333333333', 'Times New Roman', 12, 2.0, 'decimal', 'IEEE', '{"top":1.0,"bottom":1.0,"left":1.5,"right":1.0,"unit":"in"}');

INSERT INTO template_sections (id, template_id, order_index, chapter, heading, guidance)
SELECT gen_random_uuid(), t.id, s.ord, s.chapter, s.heading, s.guidance
FROM (VALUES
        ('11111111-1111-1111-1111-111111111111'::uuid),
        ('22222222-2222-2222-2222-222222222222'::uuid),
        ('33333333-3333-3333-3333-333333333333'::uuid)
     ) AS t(id)
CROSS JOIN (VALUES
    (0, 'Front Matter',              'Abstract',                 'A concise (150–300 word) summary of the study: problem, method, key findings, and conclusion.'),
    (1, 'Chapter 1 — Introduction',  'Background of the Study',  'Set the context. What is known, why the area matters, and the gap your work addresses.'),
    (2, 'Chapter 1 — Introduction',  'Statement of the Problem', 'State the specific problem clearly and its consequences if left unsolved.'),
    (3, 'Chapter 1 — Introduction',  'Aim and Objectives',       'One overarching aim, followed by 3–5 specific, measurable objectives.'),
    (4, 'Chapter 1 — Introduction',  'Research Questions',       'The questions your study will answer, aligned one-to-one with the objectives.'),
    (5, 'Chapter 1 — Introduction',  'Scope of the Study',       'Boundaries: what is covered and what is deliberately excluded.'),
    (6, 'Chapter 1 — Introduction',  'Significance of the Study','Who benefits from this research and how.'),
    (7, 'Chapter 2 — Literature Review', 'Review of Related Literature', 'Synthesize prior work thematically; end with the gap your study fills.'),
    (8, 'Chapter 3 — Methodology',   'Research Methodology',     'Design, population/sample, instruments, data collection, and analysis plan.')
) AS s(ord, chapter, heading, guidance);
