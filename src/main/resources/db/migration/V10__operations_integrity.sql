CREATE UNIQUE INDEX IF NOT EXISTS uq_payment_provider_order
  ON payment_transactions(provider, provider_order_id)
  WHERE provider_order_id IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_payment_status_created
  ON payment_transactions(status, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_orders_printer_created
  ON print_orders(printer_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_jobs_status_created
  ON print_jobs(status, created_at DESC);

ALTER TABLE printers
  ADD COLUMN IF NOT EXISTS archived BOOLEAN NOT NULL DEFAULT FALSE,
  ADD COLUMN IF NOT EXISTS archived_at TIMESTAMPTZ;

-- LOCAL_AGENT never had a PrintProvider implementation. Keep historical rows,
-- but prevent them from being offered to students until a compatible agent exists.
UPDATE printers SET active = FALSE
WHERE provider = 'LOCAL_AGENT' AND active = TRUE;

ALTER TABLE documents
  ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMPTZ;

ALTER TABLE print_orders
  ADD COLUMN IF NOT EXISTS order_type VARCHAR(16) NOT NULL DEFAULT 'STUDENT';
ALTER TABLE print_orders DROP CONSTRAINT IF EXISTS print_orders_amount_minor_check;
ALTER TABLE print_orders
  ADD CONSTRAINT print_orders_amount_minor_check CHECK (
    (order_type = 'STUDENT' AND amount_minor > 0) OR
    (order_type = 'TEST' AND amount_minor = 0)
  );

CREATE TABLE admin_audit_log (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  action VARCHAR(64) NOT NULL,
  target_type VARCHAR(64) NOT NULL,
  target_id UUID,
  summary VARCHAR(500) NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_admin_audit_created ON admin_audit_log(created_at DESC);
