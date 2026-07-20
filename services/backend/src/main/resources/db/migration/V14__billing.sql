-- Phase 10 — Billing (FR-BILL). Plan pricing (Naira), subscriptions, and webhook idempotency.
-- The paywall is NON-BINDING by default (credresearch.billing.enforce=false) — nothing is blocked
-- until enforcement is turned on. Prices are in minor units (kobo).

UPDATE plans SET price_minor = 0,        currency = 'NGN',
    metadata = '{"features":["1 project","50 AI credits/mo","Community support"]}'::jsonb            WHERE code = 'FREE';
UPDATE plans SET price_minor = 250000,   currency = 'NGN',
    metadata = '{"features":["Unlimited projects","500 AI credits/mo","Papers & RAG","Export DOCX/PDF","Priority support"]}'::jsonb WHERE code = 'STUDENT';
UPDATE plans SET price_minor = 15000000, currency = 'NGN',
    metadata = '{"features":["Everything in Student","5000 AI credits/mo","Institution admin","Department management","Onboarding support"]}'::jsonb WHERE code = 'INSTITUTION';

CREATE TABLE IF NOT EXISTS subscriptions (
    id                 UUID PRIMARY KEY,
    user_id            UUID NOT NULL,
    plan_code          TEXT NOT NULL,
    status             TEXT NOT NULL DEFAULT 'ACTIVE',   -- ACTIVE | CANCELLED | PAST_DUE
    provider           TEXT,                              -- paystack | manual
    provider_ref       TEXT,
    current_period_end TIMESTAMPTZ,
    created_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at         TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_subscriptions_user ON subscriptions (user_id);

-- Signature-verified webhooks are idempotent via a unique event id (FR-BILL-5).
CREATE TABLE IF NOT EXISTS webhook_events (
    id         UUID PRIMARY KEY,
    provider   TEXT NOT NULL,
    event_id   TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_webhook_event UNIQUE (provider, event_id)
);
