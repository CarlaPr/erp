ALTER TABLE work_orders
    ADD COLUMN IF NOT EXISTS installation_address TEXT,
    ADD COLUMN IF NOT EXISTS site_contact VARCHAR(150),
    ADD COLUMN IF NOT EXISTS site_phone VARCHAR(50),
    ADD COLUMN IF NOT EXISTS access_instructions TEXT,
    ADD COLUMN IF NOT EXISTS technician_name VARCHAR(150),
    ADD COLUMN IF NOT EXISTS installation_notes TEXT,
    ADD COLUMN IF NOT EXISTS show_measurements BOOLEAN NOT NULL DEFAULT TRUE;

ALTER TABLE work_order_items
    ADD COLUMN IF NOT EXISTS quote_item_id UUID,
    ADD COLUMN IF NOT EXISTS technical_measurements TEXT,
    ADD COLUMN IF NOT EXISTS technical_notes TEXT;
