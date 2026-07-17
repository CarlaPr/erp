ALTER TABLE accounts_receivable
    ADD COLUMN IF NOT EXISTS payment_stage VARCHAR(50) DEFAULT 'unico';

CREATE INDEX IF NOT EXISTS idx_accounts_receivable_payment_stage
    ON accounts_receivable(payment_stage);


CREATE INDEX IF NOT EXISTS idx_accounts_receivable_status_stage
    ON accounts_receivable(status, payment_stage);