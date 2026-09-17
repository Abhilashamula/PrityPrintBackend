CREATE TABLE printers (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  name VARCHAR(120) NOT NULL,
  location VARCHAR(255) NOT NULL,
  agent_key_hash VARCHAR(128) NOT NULL UNIQUE,
  status VARCHAR(24) NOT NULL DEFAULT 'OFFLINE' CHECK (status IN ('ONLINE', 'BUSY', 'OFFLINE', 'PAPER_OUT', 'ERROR')),
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE print_orders (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id UUID REFERENCES app_users(id),
  printer_id UUID NOT NULL REFERENCES printers(id),
  file_name VARCHAR(255) NOT NULL,
  file_storage_key VARCHAR(512) NOT NULL,
  total_pages INTEGER NOT NULL CHECK (total_pages > 0),
  copies INTEGER NOT NULL CHECK (copies > 0),
  color_mode VARCHAR(16) NOT NULL CHECK (color_mode IN ('BW', 'COLOR')),
  paper_size VARCHAR(16) NOT NULL,
  amount_minor BIGINT NOT NULL CHECK (amount_minor > 0),
  currency VARCHAR(3) NOT NULL DEFAULT 'INR',
  razorpay_order_id VARCHAR(255) UNIQUE,
  razorpay_payment_id VARCHAR(255) UNIQUE,
  status VARCHAR(24) NOT NULL DEFAULT 'CREATED' CHECK (status IN ('CREATED', 'PAID', 'QUEUED', 'PRINTING', 'COMPLETED', 'FAILED', 'REFUNDED', 'CANCELLED')),
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  paid_at TIMESTAMPTZ,
  completed_at TIMESTAMPTZ
);

CREATE TABLE print_jobs (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  order_id UUID NOT NULL UNIQUE REFERENCES print_orders(id) ON DELETE CASCADE,
  status VARCHAR(24) NOT NULL DEFAULT 'QUEUED' CHECK (status IN ('QUEUED', 'PRINTING', 'COMPLETED', 'FAILED')),
  pages_completed INTEGER NOT NULL DEFAULT 0 CHECK (pages_completed >= 0),
  failure_reason VARCHAR(255),
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE payment_transactions (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  order_id UUID NOT NULL REFERENCES print_orders(id),
  provider VARCHAR(32) NOT NULL,
  provider_order_id VARCHAR(255),
  provider_payment_id VARCHAR(255),
  amount_minor BIGINT NOT NULL CHECK (amount_minor > 0),
  status VARCHAR(24) NOT NULL CHECK (status IN ('CREATED', 'CAPTURED', 'FAILED', 'REFUNDED')),
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  UNIQUE(provider, provider_payment_id)
);

CREATE INDEX idx_print_orders_created ON print_orders(created_at DESC);
CREATE INDEX idx_print_orders_status ON print_orders(status);
CREATE INDEX idx_print_jobs_status ON print_jobs(status);