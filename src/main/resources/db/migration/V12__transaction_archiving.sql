ALTER TABLE payment_transactions
  ADD COLUMN IF NOT EXISTS archived BOOLEAN NOT NULL DEFAULT FALSE,
  ADD COLUMN IF NOT EXISTS archived_at TIMESTAMPTZ;

CREATE INDEX IF NOT EXISTS idx_payment_archived_created
  ON payment_transactions(archived, created_at DESC);
