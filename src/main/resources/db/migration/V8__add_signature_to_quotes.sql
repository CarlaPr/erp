ALTER TABLE quotes ADD COLUMN public_token VARCHAR(255);
ALTER TABLE quotes ADD COLUMN client_signature TEXT;

UPDATE quotes SET public_token = gen_random_uuid()::varchar WHERE public_token IS NULL;

ALTER TABLE quotes ADD CONSTRAINT uc_quotes_public_token UNIQUE (public_token);