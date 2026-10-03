ALTER TABLE payment_transactions
  ADD COLUMN IF NOT EXISTS refund_status VARCHAR(24),
  ADD COLUMN IF NOT EXISTS refund_failure_reason VARCHAR(500);

UPDATE payment_transactions
SET refund_status = 'PROCESSED'
WHERE status = 'REFUNDED' AND refund_id IS NOT NULL AND refund_status IS NULL;

CREATE INDEX IF NOT EXISTS idx_payment_refund_status
  ON payment_transactions(refund_status, updated_at DESC)
  WHERE refund_status IS NOT NULL;
