# PRD — CredResearch

Related: [Functional Requirements](./FUNCTIONAL_REQUIREMENTS.md) · [MVP Roadmap](./MVP_ROADMAP.md) · [AI System Design](./AI_SYSTEM_DESIGN.md)

## 1. Vision

Every African student and researcher should be able to move from a vague idea to a rigorous, well-structured, properly cited, supervisor-approved academic document — without ghostwriting, without paying a "project consultant" to do the work for them, and without depending on tools designed for well-resourced Western institutions.

CredResearch is the **workflow and supervision layer** for African academic research. AI assists, structures, checks, and explains — but the student remains the author, and every document carries a transparent record of how AI was used.

## 2. Problem

African research workflows are broken in specific, addressable ways:

| Problem | Today | Cost |
|---|---|---|
| Unstructured process | Students work in Word + WhatsApp + email | Lost work, no version history, chaos |
| Weak supervision loops | Feedback over printed pages and untracked email | Slow cycles, lost comments, disputes |
| Citation/formatting pain | Manual referencing, inconsistent styles | Rejected chapters, integrity flags |
| Research incoherence | Title, objectives, methodology, analysis drift apart | Failed defenses, major rewrites |
| Ghostwriting industry | "Hire someone to write my project" | Integrity collapse, no learning |
| Tool mismatch | Turnitin/SPSS expensive; tools assume fast internet & laptops | Exclusion |

## 3. Why now

- Generative AI makes ghostwriting trivially easy — institutions urgently need **transparency and structure**, not another black-box generator.
- Local payment rails (Paystack, Flutterwave) and cheap S3-compatible storage (R2, Spaces) make an affordable SaaS viable in-region.
- Smartphone penetration is high; laptop + reliable broadband penetration is not — the product must be mobile-first and bandwidth-tolerant.

## 4. Target users & primary jobs-to-be-done

| User | Primary job |
|---|---|
| Undergraduate | "Help me produce a coherent, well-cited final-year project and survive my defense." |
| Master's student | "Help me structure a dissertation and keep my supervisor in the loop." |
| PhD researcher | "Help me manage literature, keep my argument aligned, and track revisions over years." |
| Supervisor / lecturer | "Let me review and approve student work quickly, with comments that stick." |
| Private consultant | "Let me run multiple clients' projects professionally and transparently." |
| Department admin | "Let me standardize templates and see my students' progress." |
| Institution admin | "Let me onboard departments, enforce formatting, and manage billing." |
| Platform admin | "Let me operate the platform: users, plans, usage, health." |

## 5. Product principles

1. **Workflow, not ghostwriting.** The student authors; AI suggests, structures, checks, explains.
2. **Transparency by default.** Every AI interaction is logged into an exportable disclosure ledger.
3. **Coherence is the moat.** The Research Alignment Engine is the feature competitors don't have.
4. **Bandwidth is a first-class constraint.** Works on a cheap Android phone on 3G.
5. **Affordable and local.** Naira-first pricing, local payment methods, generous free tier.
6. **Supervisor adoption is existential.** Reviewing must be possible without forcing onboarding.
7. **Honest about limits.** We do not claim to be Turnitin or SPSS.

## 6. MVP scope

### In scope (MVP)
- Auth, RBAC, institutions/departments, multi-tenancy.
- Project workspace, milestones, members, activity feed.
- Template + structured document builder (proposal + Ch 1–3), Tiptap editor, autosave, versioning.
- AI Research Assistant (topic, proposal outline, objectives/RQs, methodology, problem statement) with guardrails + structured JSON.
- Research Alignment Engine (alignment report).
- Paper upload → extract → summarize → literature matrix → citations (APA/IEEE/Harvard via CSL) → reference list → RAG.
- Supervisor workflow incl. **magic-link review** for external supervisors.
- **AI-Use Disclosure & Academic Integrity Ledger.**
- DOCX + PDF export.
- Billing (Paystack/Flutterwave), plans, usage limits.
- Admin dashboard.

### MVP-plus (capacity permitting)
- Questionnaire builder + survey collection.
- Basic data analysis + Chapter 4 starter.
- Internal similarity / originality pre-check.

### Out of scope (post-MVP)
- Kubernetes, native mobile apps, full offline mode, ERP/SIS integrations, full SPSS/Turnitin replacement, real-time collaborative editing.

## 7. Differentiators

1. **Research Alignment Engine** — checks title ↔ problem ↔ aim ↔ objectives ↔ RQs ↔ hypotheses ↔ methodology ↔ instrument ↔ analysis for consistency.
2. **AI-Use Disclosure Ledger** — turns the biggest integrity risk into a selling point to institutions.
3. **Magic-link supervision** — supervisors review without friction.
4. **Africa-native** — bandwidth-tolerant, Naira-first, local rails, NDPA-compliant.

## 8. Success metrics

**Activation**
- % of new students who create a project and complete a proposal within 7 days (target ≥ 40%).
- % of projects that get a supervisor attached (target ≥ 35%).

**Engagement / value**
- Median review-cycle time (submit → decision) — target < 72h.
- % of documents exported with an attached disclosure statement (target ≥ 60%).
- Alignment report generated per project (target ≥ 1 before first export).

**Retention / revenue**
- W4 student retention ≥ 25%; paid conversion (Free → Basic/Pro) ≥ 5%.
- ≥ 3 paying institutions/departments within 6 months of GA.

**Reliability / cost**
- p95 API latency < 500ms (non-AI), AI task p95 < 12s.
- AI gross margin ≥ 60% via model routing + caching.

## 9. Risks & mitigations

| Risk | Mitigation |
|---|---|
| Perceived as a cheating tool | Disclosure Ledger + guardrails + institution positioning |
| Supervisors won't onboard | Magic-link review |
| AI cost erodes margin | LLM gateway, model routing, caching, per-plan credits |
| Hallucinated citations | Citations only from uploaded/verified sources; no free-text references |
| Low connectivity churn | Offline-safe autosave, small payloads, PWA |
| Multi-tenant data leak | `institution_id` scoping + automated tenant-isolation tests |
| Regulatory (NDPA) exposure | Consent, data minimization, retention policy, regional storage |

## 10. Non-goals

- We do not generate complete chapters unprompted.
- We do not certify originality at Turnitin level.
- We do not replace statistical software for advanced inferential analysis (MVP does descriptive stats only).
