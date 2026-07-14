
CREATE TABLE financial_closings (
                                    id                UUID PRIMARY KEY,
                                    period_start      DATE NOT NULL,
                                    period_end        DATE NOT NULL,
                                    opening_balance   NUMERIC(12,2) NOT NULL DEFAULT 0,
                                    total_in          NUMERIC(12,2) NOT NULL DEFAULT 0,
                                    total_out         NUMERIC(12,2) NOT NULL DEFAULT 0,
                                    pending_amount    NUMERIC(12,2) NOT NULL DEFAULT 0,
                                    received_amount   NUMERIC(12,2) NOT NULL DEFAULT 0,
                                    financial_expenses NUMERIC(12,2) NOT NULL DEFAULT 0,
                                    net_profit        NUMERIC(12,2) NOT NULL DEFAULT 0,
                                    closing_balance   NUMERIC(12,2) NOT NULL DEFAULT 0,
                                    closed_at         TIMESTAMP NOT NULL DEFAULT now(),
                                    closed_by         VARCHAR(120),
                                    notes             TEXT
);

CREATE UNIQUE INDEX idx_financial_closings_period_start ON financial_closings(period_start);

ALTER TABLE quotes ADD COLUMN payment_plan VARCHAR(20) DEFAULT 'SPLIT_50_50';
