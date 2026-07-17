-- Phase 6 (FR-SUP-1/2): magic-link review for external supervisors. A scoped, single-review token
-- (opaque, stored hashed) lets an account-less reviewer view+comment+decide on exactly one review.
ALTER TABLE review_requests ADD COLUMN IF NOT EXISTS review_token_hash TEXT;
ALTER TABLE review_requests ADD COLUMN IF NOT EXISTS token_expires_at  TIMESTAMPTZ;
CREATE INDEX IF NOT EXISTS idx_review_requests_token ON review_requests (review_token_hash);
