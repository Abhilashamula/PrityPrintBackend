ALTER TABLE printers ALTER COLUMN agent_key_hash DROP NOT NULL;

ALTER TABLE printers
  ADD COLUMN IF NOT EXISTS active BOOLEAN NOT NULL DEFAULT TRUE,
  ADD COLUMN IF NOT EXISTS manufacturer VARCHAR(80),
  ADD COLUMN IF NOT EXISTS model VARCHAR(120),
  ADD COLUMN IF NOT EXISTS provider VARCHAR(32) NOT NULL DEFAULT 'LOCAL_AGENT';

CREATE TABLE printer_provider_connections (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  printer_id UUID NOT NULL UNIQUE REFERENCES printers(id) ON DELETE CASCADE,
  provider VARCHAR(32) NOT NULL,
  provider_device_id VARCHAR(255),
  encrypted_access_token TEXT NOT NULL,
  encrypted_refresh_token TEXT NOT NULL,
  access_token_expires_at TIMESTAMPTZ NOT NULL,
  refresh_token_expires_at TIMESTAMPTZ,
  reauthorization_required BOOLEAN NOT NULL DEFAULT FALSE,
  connected BOOLEAN NOT NULL DEFAULT FALSE,
  capabilities_json TEXT,
  capabilities_refreshed_at TIMESTAMPTZ,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE printer_media_config (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  printer_id UUID NOT NULL REFERENCES printers(id) ON DELETE CASCADE,
  paper_source VARCHAR(128) NOT NULL,
  paper_size VARCHAR(128) NOT NULL,
  paper_type VARCHAR(128) NOT NULL,
  print_quality VARCHAR(64) NOT NULL,
  borderless BOOLEAN NOT NULL DEFAULT FALSE,
  duplex_supported BOOLEAN NOT NULL DEFAULT FALSE,
  color_supported BOOLEAN NOT NULL DEFAULT TRUE,
  mono_supported BOOLEAN NOT NULL DEFAULT TRUE,
  enabled BOOLEAN NOT NULL DEFAULT TRUE,
  price_bw_minor BIGINT CHECK (price_bw_minor > 0),
  price_color_minor BIGINT CHECK (price_color_minor > 0),
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  UNIQUE (printer_id, paper_source, paper_size, paper_type)
);

CREATE TABLE epson_oauth_states (
  state_hash VARCHAR(128) PRIMARY KEY,
  printer_id UUID NOT NULL REFERENCES printers(id) ON DELETE CASCADE,
  expires_at TIMESTAMPTZ NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

ALTER TABLE print_orders DROP CONSTRAINT IF EXISTS print_orders_status_check;
ALTER TABLE print_orders
  ADD CONSTRAINT print_orders_status_check CHECK (status IN (
    'CREATED', 'PAYMENT_PENDING', 'PAID', 'QUEUED', 'PRINTING', 'COMPLETED',
    'PRINT_FAILED', 'PRINT_STATUS_UNKNOWN', 'REFUND_PENDING', 'REFUNDED', 'CANCELLED', 'FAILED'
  )),
  ADD COLUMN IF NOT EXISTS media_config_id UUID REFERENCES printer_media_config(id),
  ADD COLUMN IF NOT EXISTS paper_source VARCHAR(128),
  ADD COLUMN IF NOT EXISTS print_quality VARCHAR(64),
  ADD COLUMN IF NOT EXISTS payment_status VARCHAR(24) NOT NULL DEFAULT 'PENDING';

ALTER TABLE print_jobs DROP CONSTRAINT IF EXISTS print_jobs_status_check;
ALTER TABLE print_jobs
  ADD CONSTRAINT print_jobs_status_check CHECK (status IN (
    'QUEUED', 'SUBMITTING', 'SUBMITTED', 'PRINTING', 'COMPLETED', 'FAILED', 'STATUS_UNKNOWN'
  )),
  ADD COLUMN IF NOT EXISTS provider VARCHAR(32),
  ADD COLUMN IF NOT EXISTS provider_job_id VARCHAR(255),
  ADD COLUMN IF NOT EXISTS claimed_by VARCHAR(128),
  ADD COLUMN IF NOT EXISTS claimed_at TIMESTAMPTZ,
  ADD COLUMN IF NOT EXISTS started_at TIMESTAMPTZ,
  ADD COLUMN IF NOT EXISTS completed_at TIMESTAMPTZ,
  ADD COLUMN IF NOT EXISTS next_attempt_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  ADD COLUMN IF NOT EXISTS attempt_count INTEGER NOT NULL DEFAULT 0,
  ADD COLUMN IF NOT EXISTS provider_status VARCHAR(64),
  ADD COLUMN IF NOT EXISTS provider_status_reason VARCHAR(255);

CREATE UNIQUE INDEX IF NOT EXISTS uq_print_jobs_provider_job
  ON print_jobs(provider, provider_job_id) WHERE provider_job_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_print_jobs_claimable
  ON print_jobs(status, next_attempt_at, created_at);

ALTER TABLE payment_transactions
  ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  ADD COLUMN IF NOT EXISTS refund_id VARCHAR(255),
  ADD COLUMN IF NOT EXISTS refund_amount_minor BIGINT,
  ADD COLUMN IF NOT EXISTS refund_reason VARCHAR(255),
  ADD COLUMN IF NOT EXISTS refunded_at TIMESTAMPTZ;

CREATE UNIQUE INDEX IF NOT EXISTS uq_payment_refund_id
  ON payment_transactions(refund_id) WHERE refund_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_media_printer_enabled
  ON printer_media_config(printer_id, enabled);
