-- ================================================
-- V3__receipts.sql
-- Migration para criar o módulo de recibos
-- Data: 2026-07-01
-- ================================================

-- Criar tabela de recibos
CREATE TABLE IF NOT EXISTS receipts (
    id UUID NOT NULL PRIMARY KEY,
    number VARCHAR(255) NOT NULL UNIQUE,
    accounts_receivable_id UUID NOT NULL,
    work_order_id UUID NOT NULL,
    client_id UUID NOT NULL,
    receipt_date DATE,
    total_amount NUMERIC(12,2),
    payment_method VARCHAR(255),
    status VARCHAR(50) NOT NULL DEFAULT 'draft',
    notes TEXT,
    responsible_name VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by UUID,
    
    CONSTRAINT fk_receipt_accounts_receivable 
        FOREIGN KEY (accounts_receivable_id) 
        REFERENCES accounts_receivable(id) ON DELETE RESTRICT,
    
    CONSTRAINT fk_receipt_work_order 
        FOREIGN KEY (work_order_id) 
        REFERENCES work_orders(id) ON DELETE RESTRICT,
    
    CONSTRAINT fk_receipt_client 
        FOREIGN KEY (client_id) 
        REFERENCES clients(id) ON DELETE RESTRICT,
    
    CONSTRAINT fk_receipt_created_by 
        FOREIGN KEY (created_by) 
        REFERENCES app_users(id) ON DELETE SET NULL
);

-- Índices para melhorar performance
CREATE INDEX idx_receipt_number ON receipts(number);
CREATE INDEX idx_receipt_status ON receipts(status);
CREATE INDEX idx_receipt_receipt_date ON receipts(receipt_date);
CREATE INDEX idx_receipt_client_id ON receipts(client_id);
CREATE INDEX idx_receipt_work_order_id ON receipts(work_order_id);
CREATE INDEX idx_receipt_accounts_receivable_id ON receipts(accounts_receivable_id);

-- Criar tabela de histórico de recibos
CREATE TABLE IF NOT EXISTS receipt_history (
    id UUID NOT NULL PRIMARY KEY,
    receipt_id UUID NOT NULL,
    event_type VARCHAR(50) NOT NULL,
    event_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    event_by UUID,
    notes TEXT,
    
    CONSTRAINT fk_history_receipt 
        FOREIGN KEY (receipt_id) 
        REFERENCES receipts(id) ON DELETE CASCADE,
    
    CONSTRAINT fk_history_event_by 
        FOREIGN KEY (event_by) 
        REFERENCES app_users(id) ON DELETE SET NULL
);

-- Índices para histórico
CREATE INDEX idx_receipt_history_receipt_id ON receipt_history(receipt_id);
CREATE INDEX idx_receipt_history_event_type ON receipt_history(event_type);
CREATE INDEX idx_receipt_history_event_date ON receipt_history(event_date);

-- ================================================
-- COMENTÁRIOS EXPLICATIVOS
-- ================================================

/*
TABELA receipts:
- id: Identificador único do recibo
- number: Número único (formato: REC-YYYYMMDD-XXXX)
- accounts_receivable_id: Referência à conta a receber (vínculo obrigatório)
- work_order_id: Referência à ordem de serviço (para rastreamento)
- client_id: Referência ao cliente (denormalização para performance)
- receipt_date: Data de emissão do recibo
- total_amount: VALOR BRUTO DO SERVIÇO (NUNCA muda)
- payment_method: Forma de pagamento (PIX, CARD, BOLETO, CASH, TRANSFER)
- status: Estado do recibo (draft, issued, printed, sent, reissued)
- notes: Observações adicionais
- responsible_name: Nome do responsável pela assinatura
- created_at: Data/hora de criação
- created_by: Usuário que criou o recibo

TABELA receipt_history:
- id: Identificador único do evento
- receipt_id: Referência ao recibo
- event_type: Tipo de evento (CREATED, PDF_GENERATED, PRINTED, SENT, REISSUED)
- event_date: Data/hora do evento
- event_by: Usuário que realizou a ação
- notes: Detalhes do evento

REGRA CRÍTICA:
O campo total_amount em receipts SEMPRE é preenchido com o valor de 
accounts_receivable.total_amount (não com received_amount ou gross_received_amount).

Isso garante que o recibo sempre mostre o valor bruto do serviço, 
independentemente de taxas ou descontos.
*/
