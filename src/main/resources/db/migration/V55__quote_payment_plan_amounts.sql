ALTER TABLE quotes
    ADD COLUMN IF NOT EXISTS payment_entry_amount NUMERIC(12,2),
    ADD COLUMN IF NOT EXISTS payment_delivery_amount NUMERIC(12,2);
