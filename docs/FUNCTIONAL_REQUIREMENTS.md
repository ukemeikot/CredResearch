# Functional Requirements — CredResearch

Related: [PRD](./PRD.md) · [API Specification](./API_SPECIFICATION.md) · [ERD](./ERD.md)

Legend: **[MVP]** ships in MVP · **[MVP+]** capacity-permitting · **[POST]** post-MVP.
Requirement IDs: `FR-<MODULE>-<n>`.

---

## 1. User, Role & Institution Management

| ID | Requirement | Priority |
|---|---|---|
| FR-AUTH-1 | Users register with email + password; email verification required before AI features. | MVP |
| FR-AUTH-2 | Login issues a short-lived JWT access token (15 min) + rotating refresh token (30 days). | MVP |
| FR-AUTH-3 | Refresh tokens are revocable; logout and "log out all devices" supported. | MVP |
| FR-AUTH-4 | Password reset via email magic-link; rate-limited. | MVP |
| FR-AUTH-5 | Passwords hashed with Argon2id. | MVP |
| FR-RBAC-1 | RBAC with roles STUDENT, SUPERVISOR, CONSULTANT, DEPARTMENT_ADMIN, INSTITUTION_ADMIN, PLATFORM_ADMIN. | MVP |
| FR-RBAC-2 | Roles map to permissions; a user may hold multiple roles (e.g. lecturer who supervises and admins a department). | MVP |
| FR-ORG-1 | Institutions can be created (self-serve or platform-admin provisioned) with name, country, type, logo. | MVP |
| FR-ORG-2 | Departments belong to an institution. | MVP |
| FR-ORG-3 | Institution admin can invite department admins and supervisors; bulk student import via CSV. | MVP |
| FR-ORG-4 | Independent consultants/students with no institution are assigned a synthetic personal tenant. | MVP |
| FR-ORG-5 | Academic profile per user: level, department, field of study, ORCID (optional). | MVP |
| FR-TEN-1 | Every tenant-scoped query is filtered by `institution_id`; cross-tenant access is impossible via API. | MVP |
| FR-ORG-6 | Seedable demo institution for onboarding/sales. | MVP |

## 2. Project Workspace

| ID | Requirement | Priority |
|---|---|---|
| FR-PROJ-1 | A user creates a research project: title (provisional ok), academic level, department/institution. | MVP |
| FR-PROJ-2 | A project has members with project-roles (owner/student, supervisor, consultant, viewer). | MVP |
| FR-PROJ-3 | Multiple/co-supervisors supported per project. | MVP |
| FR-PROJ-4 | Project status lifecycle: `DRAFT → PROPOSAL → IN_PROGRESS → UNDER_REVIEW → REVISIONS → APPROVED → COMPLETED`; transitions recorded in status history. | MVP |
| FR-PROJ-5 | Milestones with title, due date, status; milestone due notifications. | MVP |
| FR-PROJ-6 | Activity feed records create/edit/submit/review/export events. | MVP |
| FR-PROJ-7 | Project dashboard summarizes progress, next milestone, pending reviews, alignment score. | MVP |

## 3. Template & Document Builder

| ID | Requirement | Priority |
|---|---|---|
| FR-TMPL-1 | Built-in templates: UG project, Master's dissertation, PhD thesis. | MVP |
| FR-TMPL-2 | Institution/department admins can clone and customize templates (section order, headings, format rules). | MVP |
| FR-TMPL-3 | A template defines ordered `template_sections` (e.g. Abstract, Ch1 Introduction → Background/Problem/Aim/Objectives/RQs/Scope/Significance, Ch2, Ch3…). MVP focuses on Proposal + Ch1–3. | MVP |
| FR-DOC-1 | Creating a document from a template instantiates its sections. | MVP |
| FR-DOC-2 | Each section is edited in a Tiptap rich-text editor; content stored as ProseMirror JSON. | MVP |
| FR-DOC-3 | Autosave with optimistic locking; conflicting concurrent edits surface a merge/overwrite prompt. | MVP |
| FR-DOC-4 | Version history per section; restore any prior version. | MVP |
| FR-DOC-5 | Format rules (font, spacing, heading numbering, margins, citation style) attached to template; applied on export. | MVP |
| FR-DOC-6 | Export full document or selected sections to DOCX. | MVP |
| FR-DOC-7 | Export to PDF (from rendered document via Gotenberg/LibreOffice). | MVP |
| FR-DOC-8 | Export a ZIP bundle (DOCX + PDF + reference list + disclosure statement). | MVP |
| FR-DOC-9 | Real-time multi-user co-editing. | POST |

## 4. AI Research Assistant

All AI features return **structured JSON**, are labelled as suggestions, and are written to the Disclosure Ledger (Module 13). See [AI System Design](./AI_SYSTEM_DESIGN.md).

| ID | Requirement | Priority |
|---|---|---|
| FR-AI-1 | Topic suggestions given field, interests, level; returns 5–10 candidates with rationale. | MVP |
| FR-AI-2 | Topic feasibility score (data availability, scope, novelty, time) with explanation. | MVP |
| FR-AI-3 | Proposal outline generation from a topic. | MVP |
| FR-AI-4 | Generate aim, objectives, research questions, (optional) hypotheses from problem statement. | MVP |
| FR-AI-5 | Methodology suggestion appropriate to RQs and level. | MVP |
| FR-AI-6 | Problem-statement refinement (clarity, gap articulation) — edits suggested, never silently applied. | MVP |
| FR-AI-7 | Guardrails: never invents citations, never invents data/results, refuses "write my whole chapter," frames outputs as drafts/suggestions. | MVP |
| FR-AI-8 | Each AI call decrements AI credits per the user's plan; over-limit returns a clear upgrade prompt. | MVP |

## 5. Research Alignment Engine *(key differentiator)*

| ID | Requirement | Priority |
|---|---|---|
| FR-ALIGN-1 | On demand, evaluate consistency across: title, problem statement, aim, objectives, research questions, hypotheses, methodology, questionnaire (if any), analysis method. | MVP |
| FR-ALIGN-2 | Produce a structured alignment report: overall score (0–100), per-pair findings, specific misalignments, and actionable fixes. | MVP |
| FR-ALIGN-3 | Flag objectives with no matching RQ, RQs with no matching analysis, methodology mismatched to RQ type, etc. | MVP |
| FR-ALIGN-4 | Persist reports; show score trend over time on the project dashboard. | MVP |
| FR-ALIGN-5 | Block-free but warned export when alignment score is below threshold. | MVP |

## 6. Paper Upload, Literature Review & Citations

| ID | Requirement | Priority |
|---|---|---|
| FR-LIT-1 | Upload PDF/DOCX papers (per-plan size/quantity limits). | MVP |
| FR-LIT-2 | Extract full text + metadata (title, authors, year, DOI, journal) where present. | MVP |
| FR-LIT-3 | Flag low-quality extraction/OCR for user confirmation. | MVP |
| FR-LIT-4 | Summarize a paper; extract methodology, findings, limitations, identified gaps. | MVP |
| FR-LIT-5 | Generate a literature matrix across selected papers (author/year, objective, method, findings, gap). | MVP |
| FR-LIT-6 | Manage citations; insert in-text citations into sections via the editor. | MVP |
| FR-LIT-7 | Support APA, IEEE, Harvard via CSL; generate a formatted reference list. | MVP |
| FR-LIT-8 | RAG over uploaded papers; store chunks + embeddings (pgvector); answers cite only uploaded sources. | MVP |
| FR-LIT-9 | Import/export BibTeX and RIS. | MVP |
| FR-LIT-10 | Deduplicate papers (DOI/title hash). | MVP |
| FR-LIT-11 | Import from Zotero/Mendeley. | MVP+ |

## 7. Supervisor Workflow

| ID | Requirement | Priority |
|---|---|---|
| FR-SUP-1 | Student invites a supervisor by email; existing users are linked, new ones receive a magic-link. | MVP |
| FR-SUP-2 | **Magic-link review:** an external supervisor can view a submitted section and leave inline comments and a decision without creating a full account; optional later upgrade to a full account. | MVP |
| FR-SUP-3 | Student submits a section (or whole document) for review → creates a review request. | MVP |
| FR-SUP-4 | Supervisor leaves inline comments anchored to text ranges. | MVP |
| FR-SUP-5 | Supervisor records a decision: APPROVED / NEEDS_REVISION / REJECTED, with summary note. | MVP |
| FR-SUP-6 | Review history per section; comment threads resolvable. | MVP |
| FR-SUP-7 | Supervisor dashboard lists pending reviews across all their students. | MVP |
| FR-SUP-8 | Student revision workflow: address comments, resubmit; prior decisions preserved. | MVP |
| FR-SUP-9 | Notifications on invite, submission, decision, new comment. | MVP |

## 8. Questionnaire Builder *(MVP+)*

| ID | Requirement | Priority |
|---|---|---|
| FR-Q-1 | Generate a draft questionnaire from research objectives. | MVP+ |
| FR-Q-2 | Question types: Likert, multiple choice, short answer, open-ended. | MVP+ |
| FR-Q-3 | Editable consent text block. | MVP+ |
| FR-Q-4 | Publish a public survey link (tokenized). | MVP+ |
| FR-Q-5 | Collect responses; respondent consent captured. | MVP+ |
| FR-Q-6 | Export responses to CSV/Excel. | MVP+ |

## 9. Basic Data Analysis *(MVP+)*

| ID | Requirement | Priority |
|---|---|---|
| FR-DATA-1 | Upload CSV; preview rows; infer column types. | MVP+ |
| FR-DATA-2 | Detect missing values; suggest cleaning. | MVP+ |
| FR-DATA-3 | Frequency tables; mean, percentage, standard deviation (descriptive only). | MVP+ |
| FR-DATA-4 | Basic charts (bar, pie, histogram). | MVP+ |
| FR-DATA-5 | AI interpretation grounded strictly in uploaded data (no invented numbers). | MVP+ |
| FR-DATA-6 | Generate a Chapter 4 starter draft from results. | MVP+ |

## 10. Similarity & Originality Pre-check *(MVP+)*

| ID | Requirement | Priority |
|---|---|---|
| FR-SIM-1 | Internal similarity check against the user's own documents. | MVP+ |
| FR-SIM-2 | Compare against the institution repository (opt-in corpus). | MVP+ |
| FR-SIM-3 | Detect repeated/duplicated paragraphs. | MVP+ |
| FR-SIM-4 | Detect citation risk (quoted text without citation). | MVP+ |
| FR-SIM-5 | Generate a similarity report; **explicitly not** marketed as Turnitin-equivalent. | MVP+ |

## 11. Billing & Subscription

| ID | Requirement | Priority |
|---|---|---|
| FR-BILL-1 | Plans: Free, Student Basic, Student Pro, Consultant, Department, Institution. | MVP |
| FR-BILL-2 | Per-plan usage limits: projects, AI credits, paper uploads, exports, similarity checks, supervisor invitations. | MVP |
| FR-BILL-3 | Usage metered via Redis counters; soft warnings near limit, hard block past limit. | MVP |
| FR-BILL-4 | Checkout via Paystack and Flutterwave; multi-currency (NGN primary, USD for diaspora). | MVP |
| FR-BILL-5 | Webhooks verify provider signatures and are idempotent. | MVP |
| FR-BILL-6 | Institution/Department plans support manual invoice / bank transfer with admin activation. | MVP |
| FR-BILL-7 | Subscription lifecycle: active, past_due, canceled, expired; grace period on failed renewal. | MVP |
| FR-BILL-8 | Receipts emailed; invoice history in account. | MVP |

## 12. Admin Dashboard

| ID | Requirement | Priority |
|---|---|---|
| FR-ADM-1 | Manage users (search, suspend, reset, role assign). | MVP |
| FR-ADM-2 | Manage institutions and departments. | MVP |
| FR-ADM-3 | Manage templates and format rules. | MVP |
| FR-ADM-4 | View payments and subscriptions. | MVP |
| FR-ADM-5 | View usage and AI cost dashboards. | MVP |
| FR-ADM-6 | View project statistics (counts by status, by institution). | MVP |
| FR-ADM-7 | Manage plans and feature flags. | MVP |

## 13. AI-Use Disclosure & Academic Integrity Ledger *(differentiator, MVP)*

| ID | Requirement | Priority |
|---|---|---|
| FR-LEDGER-1 | Append-only record per document of every AI interaction: timestamp, prompt category, model, suggestion summary, and the user's action (accepted / edited / rejected). | MVP |
| FR-LEDGER-2 | Compute an **indicative** per-section "AI-assisted vs human-authored" signal (clearly framed as indicative, not forensic). | MVP |
| FR-LEDGER-3 | Generate an exportable **AI-Use Disclosure Statement** (PDF) for attachment to submissions. | MVP |
| FR-LEDGER-4 | Supervisors and institution admins can view a project's ledger summary. | MVP |
| FR-LEDGER-5 | Ledger entries are immutable and tamper-evident (hash-chained). | MVP |

## Cross-cutting functional requirements

| ID | Requirement | Priority |
|---|---|---|
| FR-X-1 | All long-running operations (AI, export, analysis) run as async jobs with pollable status. | MVP |
| FR-X-2 | All notifications fan out through a provider-agnostic layer (in-app, email, SMS; WhatsApp optional). | MVP |
| FR-X-3 | All destructive actions are soft-deletes with `deleted_at`. | MVP |
| FR-X-4 | All mutating admin/security actions write to `audit_logs`. | MVP |
| FR-X-5 | All list endpoints are paginated, filterable, and tenant-scoped. | MVP |
