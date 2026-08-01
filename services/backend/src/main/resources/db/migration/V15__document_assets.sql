-- Binary assets embedded in a document (uploaded images + in-app drawings). Stored in the DB and
-- served by the backend so the editor can reference them by URL without a separate object store.
CREATE TABLE IF NOT EXISTS document_assets (
    id           uuid PRIMARY KEY,
    document_id  uuid NOT NULL,
    mime_type    text NOT NULL,
    bytes        bytea NOT NULL,
    created_by   uuid,
    created_at   timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_document_assets_document ON document_assets (document_id);
