# Database Schema — CredResearch

Related: [ERD](./ERD.md) · [Technical Specification](./TECHNICAL_SPECIFICATION.md) · [Deployment](./DEPLOYMENT_AND_INFRASTRUCTURE.md)

PostgreSQL 16 + `pgvector`. All DDL via **Flyway** (`V<version>__<desc>.sql`). No manual edits in any environment.

## Conventions

- **PK:** `id uuid PRIMARY KEY DEFAULT gen_uuid_v7()` (or app-generated v7).
- **Audit columns (all tables):** `created_at timestamptz NOT NULL DEFAULT now()`, `updated_at timestamptz NOT NULL DEFAULT now()`.
- **User-data tables also:** `created_by uuid`, `updated_by uuid`, `deleted_at timestamptz` (soft delete; partial indexes use `WHERE deleted_at IS NULL`).
- **Tenant column:** `institution_id uuid NOT NULL` on tenant-scoped tables (FK → institutions).
- **Money:** `amount_minor bigint`, `currency char(3)`.
- **Enums:** Postgres enum types or `text` + `CHECK`. Listed below as logical enums.
- **Search:** `tsvector` GIN indexes on searchable text; HNSW on vectors.

## Logical enums

| Enum | Values |
|---|---|
| role_code | STUDENT, SUPERVISOR, CONSULTANT, DEPARTMENT_ADMIN, INSTITUTION_ADMIN, PLATFORM_ADMIN |
| project_status | DRAFT, PROPOSAL, IN_PROGRESS, UNDER_REVIEW, REVISIONS, APPROVED, COMPLETED |
| project_member_role | OWNER, SUPERVISOR, CONSULTANT, VIEWER |
| review_decision | APPROVED, NEEDS_REVISION, REJECTED |
| job_status | PENDING, RUNNING, SUCCEEDED, FAILED, DEAD_LETTER |
| invitation_status | PENDING, ACCEPTED, EXPIRED, REVOKED |
| subscription_status | TRIALING, ACTIVE, PAST_DUE, CANCELED, EXPIRED |
| payment_status | INITIATED, SUCCESS, FAILED, REFUNDED |
| citation_style | APA, IEEE, HARVARD |
| disclosure_action | ACCEPTED, EDITED, REJECTED |

## Identity & organization

### institutions
| Column | Type | Notes |
|---|---|---|
| id | uuid PK | |
| name | text NOT NULL | |
| country | char(2) | ISO-3166 |
| type | text | university / polytechnic / personal |
| logo_file_id | uuid | FK files |
| is_personal_tenant | boolean DEFAULT false | for independent users |
| status | text | active/suspended |

### departments
`id, institution_id FK, name, code` — UNIQUE `(institution_id, name)`.

### users
| Column | Type | Notes |
|---|---|---|
| id | uuid PK | |
| institution_id | uuid FK NOT NULL | tenant |
| department_id | uuid FK | |
| email | citext NOT NULL | UNIQUE |
| password_hash | text | Argon2id; null for magic-link-only |
| full_name | text | |
| email_verified_at | timestamptz | |
| academic_level | text | UG/MSc/PhD/staff |
| field_of_study | text | |
| orcid | text | optional |
| status | text | active/suspended |

Indexes: UNIQUE(email); INDEX(institution_id).

### roles / permissions / user_roles / role_permissions
- `roles(id, code role_code UNIQUE, description)`
- `permissions(id, code text UNIQUE, description)`
- `user_roles(id, user_id FK, role_id FK, institution_id)` UNIQUE(user_id, role_id)
- `role_permissions(id, role_id FK, permission_id FK)` UNIQUE(role_id, permission_id)

### refresh_tokens
`id, user_id FK, token_hash text NOT NULL, device text, user_agent text, expires_at timestamptz, revoked_at timestamptz` — INDEX(user_id), INDEX(token_hash).

### audit_logs
`id, institution_id, actor_user_id, action text, target_type text, target_id uuid, metadata jsonb, ip inet, created_at` — INDEX(institution_id, created_at), INDEX(actor_user_id).

### invitations
| Column | Type | Notes |
|---|---|---|
| id | uuid PK | |
| institution_id | uuid FK | |
| project_id | uuid FK | nullable (org invites) |
| email | citext | |
| role_code | text | |
| token_hash | text | magic-link |
| status | invitation_status | |
| expires_at | timestamptz | |
| accepted_user_id | uuid FK | on acceptance |

### feature_flags
`id, key text UNIQUE, enabled boolean, institution_id uuid NULL, plan_code text NULL, metadata jsonb`.

## Projects

### projects
`id, institution_id FK, department_id FK, owner_user_id FK, title text, level text, status project_status, abstract text, search_tsv tsvector` — INDEX(institution_id, status), GIN(search_tsv).

### project_members
`id, project_id FK, user_id FK, role project_member_role` — UNIQUE(project_id, user_id).

### project_milestones
`id, project_id FK, title text, due_date date, status text, completed_at timestamptz`.

### project_activities
`id, project_id FK, actor_user_id, type text, payload jsonb, created_at` — INDEX(project_id, created_at).

### project_status_history
`id, project_id FK, from_status project_status, to_status project_status, changed_by uuid, created_at`.

## Templates & documents

### templates
`id, institution_id FK NULL (null = global), department_id FK NULL, name text, level text, is_global boolean, citation_style citation_style`.

### template_sections
`id, template_id FK, order_index int, chapter text, heading text, guidance text` — INDEX(template_id, order_index).

### document_format_rules
`id, template_id FK, font_family text, font_size_pt numeric, line_spacing numeric, margins_json jsonb, heading_numbering text, citation_style citation_style`.

### documents
`id, project_id FK, template_id FK, title text, status text, search_tsv tsvector` — INDEX(project_id), GIN(search_tsv).

### document_sections
| Column | Type | Notes |
|---|---|---|
| id | uuid PK | |
| document_id | uuid FK | |
| order_index | int | |
| chapter | text | |
| heading | text | |
| content | jsonb | ProseMirror/Tiptap JSON |
| content_text | text | flattened text (for FTS/similarity) |
| version | int NOT NULL DEFAULT 1 | optimistic lock |
| search_tsv | tsvector | GIN |

INDEX(document_id, order_index), GIN(search_tsv).

### document_versions
`id, document_section_id FK, version int, content jsonb, content_text text, authored_by uuid, created_at` — UNIQUE(document_section_id, version).

## Reviews & notifications

### review_requests
`id, document_id FK, document_section_id FK NULL, requested_by FK, assignee_user_id FK NULL, assignee_invitation_id FK NULL, status text, submitted_at timestamptz`.

### review_comments
`id, review_request_id FK, author_user_id FK NULL, author_invitation_id FK NULL, anchor_json jsonb, body text, resolved boolean, created_at`.

### review_decisions
`id, review_request_id FK, decision review_decision, note text, decided_by_user_id FK NULL, decided_by_invitation_id FK NULL, created_at`.

### notifications
`id, user_id FK, type text, title text, body text, read_at timestamptz, payload jsonb, created_at` — INDEX(user_id, read_at).

### notification_outbox
`id, notification_id FK NULL, channel text (in_app/email/sms/whatsapp), recipient text, template_key text, payload jsonb, status text, attempts int, sent_at timestamptz, error text` — INDEX(status).

## Papers, literature & citations

### files
`id, institution_id, project_id FK NULL, owner_user_id FK, bucket text, object_key text, content_type text, size_bytes bigint, checksum_sha256 text, status text` — INDEX(project_id).

### papers
`id, project_id FK, file_id FK, title text, authors_json jsonb, year int, doi text, journal text, summary text, methodology text, findings text, limitations text, gaps text, extraction_quality text, search_tsv tsvector` — INDEX(project_id), GIN(search_tsv), INDEX(doi).

### paper_chunks
`id, paper_id FK, chunk_index int, content text, token_count int` — INDEX(paper_id, chunk_index).

### paper_embeddings
`id, paper_chunk_id FK UNIQUE, embedding vector(1024), model text` — `CREATE INDEX ON paper_embeddings USING hnsw (embedding vector_cosine_ops);`

> Embedding dimension `1024` is a placeholder for the chosen multilingual model; set to match the model and keep consistent across the corpus.

### citations
`id, project_id FK, paper_id FK NULL, csl_json jsonb (CSL-JSON item), source text (paper/manual/bibtex/ris), created_by uuid` — INDEX(project_id).

### literature_matrix_entries
`id, project_id FK, paper_id FK, objective_ref text, method text, findings text, gap text, notes text` — INDEX(project_id).

## AI & disclosure

### ai_prompt_templates
`id, feature_key text, version int, system_prompt text, user_template text, output_schema jsonb, active boolean` — UNIQUE(feature_key, version).

### ai_requests
`id, institution_id, project_id FK NULL, document_id FK NULL, user_id FK, feature_key text, prompt_template_id FK, input_ref jsonb, model text, status text, created_at` — INDEX(project_id), INDEX(user_id, created_at).

### ai_responses
`id, ai_request_id FK UNIQUE, output_json jsonb, finish_reason text, created_at`.

### ai_usage_logs
`id, ai_request_id FK, model text, input_tokens int, output_tokens int, cost_minor bigint, currency char(3), latency_ms int, cache_hit boolean` — INDEX(created_at).

### ai_jobs
`id, type text, ref_id uuid, status job_status, attempts int DEFAULT 0, max_attempts int DEFAULT 5, payload_ref jsonb, result_ref jsonb, error text, locked_by text, locked_at timestamptz` — INDEX(status), INDEX(type, status).

### research_alignment_reports
`id, project_id FK, document_id FK NULL, overall_score int, findings_json jsonb, created_by uuid, created_at` — INDEX(project_id, created_at).

### ai_disclosure_entries
| Column | Type | Notes |
|---|---|---|
| id | uuid PK | |
| document_id | uuid FK | |
| document_section_id | uuid FK NULL | |
| ai_request_id | uuid FK NULL | |
| feature_key | text | prompt category |
| model | text | |
| suggestion_summary | text | |
| action | disclosure_action | accepted/edited/rejected |
| prev_hash | text | hash chain |
| entry_hash | text | sha256(prev_hash + payload) |
| created_at | timestamptz | append-only |

INDEX(document_id, created_at). Application enforces append-only + hash chaining.

## Questionnaires & survey (MVP+)
- `questionnaires(id, project_id FK, title, consent_text, status)`
- `questions(id, questionnaire_id FK, order_index, type, prompt, options_json, required)`
- `survey_links(id, questionnaire_id FK, token_hash, active, expires_at)`
- `survey_responses(id, survey_link_id FK, consent_given boolean, submitted_at, respondent_meta jsonb)`
- `survey_answers(id, survey_response_id FK, question_id FK, value_json)`

## Data analysis (MVP+)
- `datasets(id, project_id FK, file_id FK, row_count int, status)`
- `dataset_columns(id, dataset_id FK, name, inferred_type, missing_count int)`
- `analysis_jobs(id, dataset_id FK, type, params_json, status job_status)`
- `analysis_results(id, analysis_job_id FK, result_json)`
- `analysis_charts(id, analysis_job_id FK, chart_type, spec_json, image_file_id FK NULL)`

## Similarity (MVP+)
- `institution_repositories(id, institution_id FK, name, status)`
- `similarity_checks(id, document_id FK, scope text, status job_status, created_at)`
- `similarity_matches(id, similarity_check_id FK, source_ref text, matched_span text, score numeric)`
- `similarity_reports(id, similarity_check_id FK UNIQUE, overall_score numeric, summary_json jsonb)`

## Billing
- `plans(id, code text UNIQUE, name, price_minor bigint, currency char(3), interval text, metadata jsonb)`
- `subscriptions(id, plan_id FK, user_id FK NULL, institution_id FK NULL, status subscription_status, current_period_end timestamptz, provider text, provider_ref text)` — CHECK exactly one of user_id/institution_id set.
- `payments(id, subscription_id FK, provider text, provider_ref text, amount_minor bigint, currency char(3), status payment_status, idempotency_key text, created_at)` — UNIQUE(provider, provider_ref).
- `usage_limits(id, plan_id FK, metric text, limit_value int)` — UNIQUE(plan_id, metric).
- `usage_events(id, institution_id, subscription_id FK, metric text, amount int, created_at)` — INDEX(subscription_id, metric, created_at).
- `webhook_events(id, provider text, event_id text, type text, payload jsonb, processed_at timestamptz, status text)` — UNIQUE(provider, event_id).

## Migration strategy

- One Flyway migration per change; forward-only. Backfills as separate, idempotent migrations.
- pgvector + extensions enabled in an early migration (`CREATE EXTENSION IF NOT EXISTS vector; CREATE EXTENSION IF NOT EXISTS citext;`).
- Destructive changes done in expand→migrate→contract steps to stay zero-downtime-friendly.
- Seed migrations for roles, permissions, default global templates, plans, and the demo institution.
