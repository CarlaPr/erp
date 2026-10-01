
ALTER TABLE receipts ADD COLUMN IF NOT EXISTS public_token VARCHAR(100);
UPDATE receipts SET public_token = gen_random_uuid()::text WHERE public_token IS NULL;
ALTER TABLE receipts ALTER COLUMN public_token SET NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS uq_receipts_public_token ON receipts(public_token);

ALTER TABLE receipts ADD COLUMN IF NOT EXISTS client_signature TEXT;
ALTER TABLE receipts ADD COLUMN IF NOT EXISTS client_signed_at TIMESTAMP;
