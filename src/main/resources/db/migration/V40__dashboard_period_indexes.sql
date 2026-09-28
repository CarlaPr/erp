
CREATE INDEX IF NOT EXISTS idx_receivable_due_date ON accounts_receivable(due_date);
CREATE INDEX IF NOT EXISTS idx_receivable_payment_date ON accounts_receivable(payment_date);
CREATE INDEX IF NOT EXISTS idx_payable_due_date ON accounts_payable(due_date);
CREATE INDEX IF NOT EXISTS idx_work_orders_created_at ON work_orders(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_work_orders_install_date ON work_orders(install_date);
CREATE INDEX IF NOT EXISTS idx_quotes_date_created ON quotes(date_created);
CREATE INDEX IF NOT EXISTS idx_quotes_date_approved ON quotes(date_approved);
