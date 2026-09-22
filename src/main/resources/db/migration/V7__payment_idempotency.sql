ALTER TABLE payment_transactions
  ADD COLUMN IF NOT EXISTS failure_reason VARCHAR(255),
  ADD COLUMN IF NOT EXISTS provider_event_id VARCHAR(255);

CREATE TABLE webhook_events (
  id VARCHAR(255) PRIMARY KEY,
  provider VARCHAR(32) NOT NULL,
  event_type VARCHAR(128) NOT NULL,
  received_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);