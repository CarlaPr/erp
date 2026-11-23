ALTER TABLE bank_accounts
    ADD COLUMN opening_balance NUMERIC(14,2);

CREATE UNIQUE INDEX uq_bank_accounts_configured_name
    ON bank_accounts (LOWER(name))
    WHERE opening_balance IS NOT NULL;

ALTER TABLE financial_movements
    ADD COLUMN bank_account_id UUID REFERENCES bank_accounts(id),
    ADD CONSTRAINT ck_financial_movement_bank_account
        CHECK (bank_account_id IS NULL OR balance_location = 'BANK');

CREATE INDEX idx_financial_movements_bank_account_date
    ON financial_movements (bank_account_id, movement_date)
    WHERE bank_account_id IS NOT NULL;
