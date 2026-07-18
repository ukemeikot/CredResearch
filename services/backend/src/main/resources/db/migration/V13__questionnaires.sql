-- Phase 7 — Questionnaire Builder & Data Collection (FR-Q-*): build → publish (tokenized public
-- link) → collect responses (with consent) → export CSV.

CREATE TABLE IF NOT EXISTS questionnaires (
    id           UUID PRIMARY KEY,
    project_id   UUID NOT NULL,
    title        TEXT NOT NULL,
    consent_text TEXT,
    status       TEXT NOT NULL DEFAULT 'DRAFT',   -- DRAFT | PUBLISHED | CLOSED
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_questionnaires_project ON questionnaires (project_id, created_at);

CREATE TABLE IF NOT EXISTS questions (
    id               UUID PRIMARY KEY,
    questionnaire_id UUID NOT NULL,
    order_index      INT NOT NULL DEFAULT 0,
    type             TEXT NOT NULL,               -- TEXT|LONG_TEXT|SINGLE_CHOICE|MULTI_CHOICE|LIKERT|NUMBER|BOOLEAN
    prompt           TEXT NOT NULL,
    options_json     JSONB,                        -- choice options / likert scale
    required         BOOLEAN NOT NULL DEFAULT FALSE
);
CREATE INDEX IF NOT EXISTS idx_questions_questionnaire ON questions (questionnaire_id, order_index);

CREATE TABLE IF NOT EXISTS survey_links (
    id               UUID PRIMARY KEY,
    questionnaire_id UUID NOT NULL,
    token_hash       TEXT NOT NULL,
    active           BOOLEAN NOT NULL DEFAULT TRUE,
    expires_at       TIMESTAMPTZ,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_survey_links_token ON survey_links (token_hash);

CREATE TABLE IF NOT EXISTS survey_responses (
    id              UUID PRIMARY KEY,
    survey_link_id  UUID NOT NULL,
    consent_given   BOOLEAN NOT NULL DEFAULT FALSE,
    submitted_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    respondent_meta JSONB
);
CREATE INDEX IF NOT EXISTS idx_survey_responses_link ON survey_responses (survey_link_id);

CREATE TABLE IF NOT EXISTS survey_answers (
    id                 UUID PRIMARY KEY,
    survey_response_id UUID NOT NULL,
    question_id        UUID NOT NULL,
    value_json         JSONB
);
CREATE INDEX IF NOT EXISTS idx_survey_answers_response ON survey_answers (survey_response_id);
