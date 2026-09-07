-- =====================================================================
-- V32: Vincula rateio de Contas a Pagar (expense_allocations) ao custo
-- lançado na Ordem de Serviço (work_order_items), permitindo que:
--   - uma conta a pagar seja distribuída entre uma ou mais OS, cada uma
--     com seu próprio valor;
--   - o valor rateado seja lançado automaticamente como um item de
--     custo na OS (aparece em "materiais/custos" e no CMV);
--   - ao excluir a conta a pagar (ou a alocação), o custo lançado na OS
--     seja removido junto, evitando valores órfãos.
--
-- Contas a pagar já existentes NÃO são afetadas: nenhuma alocação é
-- criada retroativamente, então nenhum custo é lançado automaticamente
-- para elas. O recurso vale apenas para alocações novas, criadas a
-- partir desta versão em diante.
-- =====================================================================

-- Descrição específica da alocação (o que será exibido como descrição
-- do custo na OS). Quando nula, o serviço usa a descrição da conta.
ALTER TABLE expense_allocations ADD COLUMN IF NOT EXISTS description VARCHAR(255);

-- Item de custo gerado automaticamente na OS a partir desta alocação.
-- Guardamos o vínculo para poder remover o custo quando a alocação (ou
-- a conta a pagar) for excluída.
ALTER TABLE expense_allocations ADD COLUMN IF NOT EXISTS work_order_item_id UUID;

ALTER TABLE expense_allocations
    ADD CONSTRAINT fk_expense_allocation_work_order_item
    FOREIGN KEY (work_order_item_id) REFERENCES work_order_items(id) ON DELETE SET NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uq_expense_allocation_work_order_item
    ON expense_allocations(work_order_item_id) WHERE work_order_item_id IS NOT NULL;

-- Marca de rastreabilidade: identifica que o item foi gerado
-- automaticamente a partir de uma conta a pagar, para diferenciá-lo de
-- itens lançados manualmente pelo usuário.
ALTER TABLE work_order_items ADD COLUMN IF NOT EXISTS source_expense_allocation_id UUID;

CREATE INDEX IF NOT EXISTS idx_expense_allocations_accounts_payable_id
    ON expense_allocations(accounts_payable_id);

CREATE INDEX IF NOT EXISTS idx_expense_allocations_work_order_id
    ON expense_allocations(work_order_id);
