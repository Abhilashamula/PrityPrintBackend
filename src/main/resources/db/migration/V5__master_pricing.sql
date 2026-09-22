CREATE TABLE master_pricing (
  id SMALLINT PRIMARY KEY CHECK (id = 1),
  price_bw_minor BIGINT NOT NULL CHECK (price_bw_minor > 0),
  price_color_minor BIGINT NOT NULL CHECK (price_color_minor > 0),
  max_file_mb INTEGER NOT NULL CHECK (max_file_mb > 0),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

INSERT INTO master_pricing (id, price_bw_minor, price_color_minor, max_file_mb)
VALUES (1, 200, 1000, 20)
ON CONFLICT (id) DO NOTHING;