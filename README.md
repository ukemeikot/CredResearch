# Academic Research SaaS — Documentation Prompt Package

This folder contains the master prompt used to generate the full product and
technical documentation package for the African academic research SaaS platform.

## Contents

- `DOCS_GENERATION_PROMPT.md` — the complete, finalized prompt (v2). Paste it into
  an AI assistant to generate the `/docs` package.
- `docs/` — empty target folder. The generated `.md` files land here.

## How to use

1. Open `DOCS_GENERATION_PROMPT.md`.
2. (Optional) Strip the `[ADDED]` markers — they only flag what changed vs. v1.
3. Run the prompt. It generates these files into `docs/`:

   README.md · PRD.md · FUNCTIONAL_REQUIREMENTS.md · NON_FUNCTIONAL_REQUIREMENTS.md ·
   TECHNICAL_SPECIFICATION.md · SYSTEM_ARCHITECTURE.md · ERD.md · DATABASE_SCHEMA.md ·
   API_SPECIFICATION.md · AI_SYSTEM_DESIGN.md · SECURITY_AND_COMPLIANCE.md ·
   TEST_STRATEGY.md · MVP_ROADMAP.md · DEPLOYMENT_AND_INFRASTRUCTURE.md ·
   ENGINEERING_DECISIONS.md

## Two decisions to confirm before running

- Ship the **AI-Use Disclosure & Academic Integrity Ledger** in MVP? (Recommended: yes.)
- Keep **email-first / magic-link supervisor review** in MVP or move to MVP-plus?
