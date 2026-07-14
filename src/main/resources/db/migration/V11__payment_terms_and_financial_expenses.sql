ALTER TABLE accounts_receivable ADD COLUMN payment_stage VARCHAR(20) DEFAULT 'unico';
ALTER TABLE accounts_receivable ADD COLUMN reference_month DATE;

UPDATE accounts_receivable SET reference_month = due_date WHERE reference_month IS NULL;

ALTER TABLE accounts_payable ADD COLUMN is_financial_expense BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE accounts_payable ADD COLUMN source_receivable_id UUID REFERENCES accounts_receivable(id);

CREATE INDEX idx_accounts_payable_financial_expense ON accounts_payable(is_financial_expense);
CREATE INDEX idx_accounts_receivable_reference_month ON accounts_receivable(reference_month);