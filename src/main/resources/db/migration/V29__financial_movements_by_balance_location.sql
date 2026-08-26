CREATE TABLE financial_movements (
    id UUID NOT NULL PRIMARY KEY,
    movement_date DATE NOT NULL,
    type VARCHAR(10) NOT NULL,
    balance_location VARCHAR(10) NOT NULL,
    amount NUMERIC(12,2) NOT NULL,
    payment_method VARCHAR(255),
    accounts_receivable_id UUID,
    accounts_payable_id UUID,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_financial_movement_receivable
        FOREIGN KEY (accounts_receivable_id) REFERENCES accounts_receivable(id) ON DELETE CASCADE,
    CONSTRAINT fk_financial_movement_payable
        FOREIGN KEY (accounts_payable_id) REFERENCES accounts_payable(id) ON DELETE CASCADE,
    CONSTRAINT ck_financial_movement_type
        CHECK (type IN ('ENTRADA', 'SAIDA')),
    CONSTRAINT ck_financial_movement_location
        CHECK (balance_location IN ('BANK', 'CASH')),
    CONSTRAINT ck_financial_movement_source
        CHECK ((accounts_receivable_id IS NOT NULL AND accounts_payable_id IS NULL)
            OR (accounts_receivable_id IS NULL AND accounts_payable_id IS NOT NULL))
);

CREATE INDEX idx_financial_movements_date ON financial_movements(movement_date);
CREATE INDEX idx_financial_movements_location ON financial_movements(balance_location);
CREATE INDEX idx_financial_movements_receivable ON financial_movements(accounts_receivable_id);
CREATE INDEX idx_financial_movements_payable ON financial_movements(accounts_payable_id);

-- Preserva o histórico já existente. Registros antigos possuem apenas a forma
-- consolidada da conta; por isso cada conta quitada/parcial gera uma movimentação
-- inicial, classificada como dinheiro quando a forma informada era CASH/Dinheiro.
INSERT INTO financial_movements (
    id, movement_date, type, balance_location, amount, payment_method,
    accounts_receivable_id, created_at
)
SELECT
    (SUBSTRING(MD5('receivable:' || id::text), 1, 8) || '-' ||
     SUBSTRING(MD5('receivable:' || id::text), 9, 4) || '-' ||
     SUBSTRING(MD5('receivable:' || id::text), 13, 4) || '-' ||
     SUBSTRING(MD5('receivable:' || id::text), 17, 4) || '-' ||
     SUBSTRING(MD5('receivable:' || id::text), 21, 12))::UUID,
    COALESCE(payment_date, due_date),
    'ENTRADA',
    CASE WHEN UPPER(TRIM(COALESCE(payment_method, ''))) IN ('CASH', 'DINHEIRO', 'MONEY')
         THEN 'CASH' ELSE 'BANK' END,
    received_amount,
    payment_method,
    id,
    CURRENT_TIMESTAMP
FROM accounts_receivable
WHERE status IN ('received', 'partial')
  AND COALESCE(received_amount, 0) > 0;

INSERT INTO financial_movements (
    id, movement_date, type, balance_location, amount, payment_method,
    accounts_payable_id, created_at
)
SELECT
    (SUBSTRING(MD5('payable:' || id::text), 1, 8) || '-' ||
     SUBSTRING(MD5('payable:' || id::text), 9, 4) || '-' ||
     SUBSTRING(MD5('payable:' || id::text), 13, 4) || '-' ||
     SUBSTRING(MD5('payable:' || id::text), 17, 4) || '-' ||
     SUBSTRING(MD5('payable:' || id::text), 21, 12))::UUID,
    COALESCE(payment_date, due_date),
    'SAIDA',
    CASE WHEN UPPER(TRIM(COALESCE(payment_method, ''))) IN ('CASH', 'DINHEIRO', 'MONEY')
         THEN 'CASH' ELSE 'BANK' END,
    paid_amount,
    payment_method,
    id,
    CURRENT_TIMESTAMP
FROM accounts_payable
WHERE status IN ('paid', 'partial')
  AND COALESCE(paid_amount, 0) > 0;
