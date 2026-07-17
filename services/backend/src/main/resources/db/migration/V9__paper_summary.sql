-- Phase 5 (FR-LIT-4): persist a paper's AI-generated summary (method/findings/limitations/gaps).
ALTER TABLE papers ADD COLUMN IF NOT EXISTS summary_json JSONB;
