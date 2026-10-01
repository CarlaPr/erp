
ALTER TABLE receipts ALTER COLUMN accounts_receivable_id DROP NOT NULL;
ALTER TABLE receipts ALTER COLUMN work_order_id DROP NOT NULL;


ALTER TABLE receipts DROP CONSTRAINT IF EXISTS fk_receipt_work_order;
ALTER TABLE receipts
    ADD CONSTRAINT fk_receipt_work_order
        FOREIGN KEY (work_order_id) REFERENCES work_orders(id) ON DELETE SET NULL;

ALTER TABLE receipts ADD COLUMN IF NOT EXISTS profile_id UUID;
ALTER TABLE receipts ADD COLUMN IF NOT EXISTS received_date DATE;
ALTER TABLE receipts ADD COLUMN IF NOT EXISTS discount NUMERIC(12,2) NOT NULL DEFAULT 0;
ALTER TABLE receipts ADD COLUMN IF NOT EXISTS warranty TEXT;
ALTER TABLE receipts ADD COLUMN IF NOT EXISTS payment_terms TEXT;
ALTER TABLE receipts ADD COLUMN IF NOT EXISTS description TEXT;
ALTER TABLE receipts ADD COLUMN IF NOT EXISTS created_by_name VARCHAR(100);

ALTER TABLE receipts
    ADD CONSTRAINT fk_receipt_profile
        FOREIGN KEY (profile_id) REFERENCES profiles(id) ON DELETE SET NULL;

CREATE INDEX IF NOT EXISTS idx_receipt_profile_id ON receipts(profile_id);


CREATE TABLE IF NOT EXISTS receipt_items (
    id UUID NOT NULL PRIMARY KEY,
    receipt_id UUID NOT NULL,
    sort_order INTEGER NOT NULL DEFAULT 0,
    description TEXT NOT NULL,
    quantity NUMERIC(10,2) NOT NULL DEFAULT 1,
    unit_price NUMERIC(12,2) NOT NULL DEFAULT 0,
    CONSTRAINT fk_receipt_items_receipt
        FOREIGN KEY (receipt_id) REFERENCES receipts(id) ON DELETE CASCADE
);
CREATE INDEX IF NOT EXISTS idx_receipt_items_receipt ON receipt_items(receipt_id);

CREATE TABLE IF NOT EXISTS receipt_photos (
    id UUID NOT NULL PRIMARY KEY,
    receipt_id UUID NOT NULL,
    sort_order INTEGER NOT NULL DEFAULT 0,
    file_name VARCHAR(255) NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    content BYTEA NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_receipt_photos_receipt
        FOREIGN KEY (receipt_id) REFERENCES receipts(id) ON DELETE CASCADE
);
CREATE INDEX IF NOT EXISTS idx_receipt_photos_receipt ON receipt_photos(receipt_id);


CREATE SEQUENCE IF NOT EXISTS receipt_number_seq START WITH 1001 INCREMENT BY 1;
