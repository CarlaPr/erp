-- =====================================================================
-- V15: Reestruturação do módulo de Contas a Pagar
-- Contas Fixas, Contas Recorrentes, Categorias/Subcategorias, Centro de
-- Custo, Competência, Natureza/Tipo de despesa.
-- =====================================================================

-- ── Tabela de Categorias de Despesa ──────────────────────────────────
CREATE TABLE expense_categories (
    id             UUID PRIMARY KEY,
    code           VARCHAR(40) NOT NULL UNIQUE,
    name           VARCHAR(120) NOT NULL,
    display_order  INTEGER NOT NULL DEFAULT 0,
    is_active      BOOLEAN NOT NULL DEFAULT TRUE
);

-- ── Tabela de Subcategorias de Despesa ───────────────────────────────
CREATE TABLE expense_subcategories (
    id             UUID PRIMARY KEY,
    category_id    UUID NOT NULL REFERENCES expense_categories(id),
    code           VARCHAR(120) NOT NULL,
    name           VARCHAR(150) NOT NULL,
    group_label    VARCHAR(60),
    segment        VARCHAR(40),
    display_order  INTEGER NOT NULL DEFAULT 0,
    is_active      BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT uq_expense_subcategory_code UNIQUE (category_id, code)
);

CREATE INDEX idx_expense_subcategories_category_id ON expense_subcategories(category_id);

-- ── Tabela de Recorrências (Contas Recorrentes) ──────────────────────
CREATE TABLE expense_recurrences (
    id                       UUID PRIMARY KEY,
    description              VARCHAR(255) NOT NULL,
    category                 VARCHAR(40),
    subcategory              VARCHAR(120),
    expense_type             VARCHAR(20),
    expense_nature           VARCHAR(30),
    cost_center              VARCHAR(120),
    base_amount              NUMERIC(12,2) NOT NULL,
    supplier_id              UUID REFERENCES suppliers(id),
    work_order_id            UUID REFERENCES work_orders(id),
    payment_method           VARCHAR(30),
    document_number          VARCHAR(80),
    notes                    TEXT,
    frequency                VARCHAR(20) NOT NULL,
    start_date               DATE NOT NULL,
    end_type                 VARCHAR(20) NOT NULL,
    occurrence_count         INTEGER,
    end_date                 DATE,
    status                   VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    total_generated          INTEGER NOT NULL DEFAULT 0,
    last_generated_due_date  DATE,
    created_at               TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_expense_recurrences_status ON expense_recurrences(status);

-- ── Novos campos em Contas a Pagar ────────────────────────────────────
ALTER TABLE accounts_payable ADD COLUMN IF NOT EXISTS expense_type VARCHAR(20);
ALTER TABLE accounts_payable ADD COLUMN IF NOT EXISTS expense_nature VARCHAR(30);
ALTER TABLE accounts_payable ADD COLUMN IF NOT EXISTS cost_center VARCHAR(120);
ALTER TABLE accounts_payable ADD COLUMN IF NOT EXISTS competencia DATE;
ALTER TABLE accounts_payable ADD COLUMN IF NOT EXISTS recurrence_id UUID;
ALTER TABLE accounts_payable ADD COLUMN IF NOT EXISTS recurrence_seq INTEGER;

CREATE INDEX IF NOT EXISTS idx_accounts_payable_competencia ON accounts_payable(competencia);
CREATE INDEX IF NOT EXISTS idx_accounts_payable_category ON accounts_payable(category);
CREATE INDEX IF NOT EXISTS idx_accounts_payable_recurrence_id ON accounts_payable(recurrence_id);
CREATE INDEX IF NOT EXISTS idx_accounts_payable_expense_type ON accounts_payable(expense_type);

-- ── Normalização de dados existentes ─────────────────────────────────
-- Antes desta migration, 'category' usava os códigos legados 'variable',
-- 'fixed' e 'provision'. Passamos a usar os novos códigos padronizados,
-- preservando o campo expense_type com a mesma semântica de antes para
-- não alterar os cálculos de CMV/Despesas Fixas já existentes.
UPDATE accounts_payable SET category = 'VARIAVEL' WHERE LOWER(category) = 'variable';
UPDATE accounts_payable SET category = 'FIXA' WHERE LOWER(category) = 'fixed';
UPDATE accounts_payable SET category = 'PROVISIONAMENTO' WHERE LOWER(category) = 'provision';

UPDATE accounts_payable SET expense_type = 'VARIAVEL' WHERE UPPER(category) = 'VARIAVEL';
UPDATE accounts_payable SET expense_type = 'FIXA' WHERE expense_type IS NULL;

UPDATE accounts_payable SET competencia = date_trunc('month', due_date)::date WHERE competencia IS NULL;

-- ── Seed: Categorias principais ──────────────────────────────────────
INSERT INTO expense_categories (id, code, name, display_order, is_active) VALUES ('c16f135c-89cf-4c1c-87f1-01249abd8fb0', 'FIXA', 'Fixa', 10, TRUE);
INSERT INTO expense_categories (id, code, name, display_order, is_active) VALUES ('c068b0fa-89d6-439b-925b-1f32f02ae62a', 'VARIAVEL', 'Variável', 20, TRUE);
INSERT INTO expense_categories (id, code, name, display_order, is_active) VALUES ('ffefdb43-1cc5-48e5-bb20-c17352ea007a', 'PROVISIONAMENTO', 'Provisionamento', 30, TRUE);
INSERT INTO expense_categories (id, code, name, display_order, is_active) VALUES ('dcc20ce3-8418-4283-9513-ee820442454d', 'IMPOSTOS', 'Impostos', 40, TRUE);
INSERT INTO expense_categories (id, code, name, display_order, is_active) VALUES ('baf37452-2e94-49a7-916a-5f88918b4eff', 'INVESTIMENTOS', 'Investimentos', 50, TRUE);
INSERT INTO expense_categories (id, code, name, display_order, is_active) VALUES ('d43c1cde-2c6d-42f4-b1b4-8f661b80a848', 'MANUTENCAO', 'Manutenção', 60, TRUE);
INSERT INTO expense_categories (id, code, name, display_order, is_active) VALUES ('f9ad3afe-99d4-4a37-9a63-7cf75602b771', 'FINANCEIRO', 'Financeiro', 70, TRUE);
INSERT INTO expense_categories (id, code, name, display_order, is_active) VALUES ('a332c9dd-b691-48bd-b0d0-b88d85915351', 'PESSOAL', 'Pessoal', 80, TRUE);
INSERT INTO expense_categories (id, code, name, display_order, is_active) VALUES ('61d644f6-a89b-480e-bdde-b8189e2b861d', 'OPERACIONAL', 'Operacional', 90, TRUE);
INSERT INTO expense_categories (id, code, name, display_order, is_active) VALUES ('2f81119e-6705-4c54-9ba0-83550bd277a0', 'ADMINISTRATIVO', 'Administrativo', 100, TRUE);

-- ── Seed: Subcategorias ───────────────────────────────────────────────
INSERT INTO expense_subcategories (id, category_id, code, name, group_label, segment, display_order, is_active) VALUES ('65bb8744-a9cd-43bc-84ea-f592ad9c6deb', 'c16f135c-89cf-4c1c-87f1-01249abd8fb0', 'aluguel', 'Aluguel', NULL, NULL, 10, TRUE);
INSERT INTO expense_subcategories (id, category_id, code, name, group_label, segment, display_order, is_active) VALUES ('749abb97-cac3-4e24-8052-9022df9e4825', 'c16f135c-89cf-4c1c-87f1-01249abd8fb0', 'condominio', 'Condomínio', NULL, NULL, 20, TRUE);
INSERT INTO expense_subcategories (id, category_id, code, name, group_label, segment, display_order, is_active) VALUES ('8c75a8ef-ccd4-49dc-8fbc-8ec34461b20a', 'c16f135c-89cf-4c1c-87f1-01249abd8fb0', 'energia', 'Energia', NULL, NULL, 30, TRUE);
INSERT INTO expense_subcategories (id, category_id, code, name, group_label, segment, display_order, is_active) VALUES ('0f419435-5b53-48ec-b6c0-511dfde001ae', 'c16f135c-89cf-4c1c-87f1-01249abd8fb0', 'agua', 'Água', NULL, NULL, 40, TRUE);
INSERT INTO expense_subcategories (id, category_id, code, name, group_label, segment, display_order, is_active) VALUES ('2b2249c3-2ed4-40ba-86d9-5e7662cd2367', 'c16f135c-89cf-4c1c-87f1-01249abd8fb0', 'internet', 'Internet', NULL, NULL, 50, TRUE);
INSERT INTO expense_subcategories (id, category_id, code, name, group_label, segment, display_order, is_active) VALUES ('4ed99940-7298-4cca-88b1-db5f273eb9b0', 'c16f135c-89cf-4c1c-87f1-01249abd8fb0', 'telefone', 'Telefone', NULL, NULL, 60, TRUE);
INSERT INTO expense_subcategories (id, category_id, code, name, group_label, segment, display_order, is_active) VALUES ('60a7ae1c-8bc2-45ae-93fd-540937f5a0e4', 'c16f135c-89cf-4c1c-87f1-01249abd8fb0', 'contabilidade', 'Contabilidade', NULL, NULL, 70, TRUE);
INSERT INTO expense_subcategories (id, category_id, code, name, group_label, segment, display_order, is_active) VALUES ('514b6e68-dbc0-447e-bf21-d26ae7e3938c', 'c16f135c-89cf-4c1c-87f1-01249abd8fb0', 'sistema_erp', 'Sistema ERP', NULL, NULL, 80, TRUE);
INSERT INTO expense_subcategories (id, category_id, code, name, group_label, segment, display_order, is_active) VALUES ('623cf0a8-ef13-4845-bb29-785f5d39ce32', 'c16f135c-89cf-4c1c-87f1-01249abd8fb0', 'dominio', 'Domínio', NULL, NULL, 90, TRUE);
INSERT INTO expense_subcategories (id, category_id, code, name, group_label, segment, display_order, is_active) VALUES ('7262374f-5e67-47e4-be4a-5f6ac580828e', 'c16f135c-89cf-4c1c-87f1-01249abd8fb0', 'hospedagem', 'Hospedagem', NULL, NULL, 100, TRUE);
INSERT INTO expense_subcategories (id, category_id, code, name, group_label, segment, display_order, is_active) VALUES ('bebe7f6a-3a56-40ec-81b5-9ccc7685aed2', 'c16f135c-89cf-4c1c-87f1-01249abd8fb0', 'seguro_empresarial', 'Seguro empresarial', NULL, NULL, 110, TRUE);
INSERT INTO expense_subcategories (id, category_id, code, name, group_label, segment, display_order, is_active) VALUES ('319ac212-b3f5-472f-877f-5377e41fd478', 'c16f135c-89cf-4c1c-87f1-01249abd8fb0', 'salarios', 'Salários', NULL, NULL, 120, TRUE);
INSERT INTO expense_subcategories (id, category_id, code, name, group_label, segment, display_order, is_active) VALUES ('1ccce7f3-3dbc-4d32-99ba-bd455e12733c', 'c16f135c-89cf-4c1c-87f1-01249abd8fb0', 'pro-labore', 'Pró-labore', NULL, NULL, 130, TRUE);
INSERT INTO expense_subcategories (id, category_id, code, name, group_label, segment, display_order, is_active) VALUES ('832c8552-b9e1-4576-a1d5-6c263001bb09', 'c16f135c-89cf-4c1c-87f1-01249abd8fb0', 'vale_transporte', 'Vale transporte', NULL, NULL, 140, TRUE);
INSERT INTO expense_subcategories (id, category_id, code, name, group_label, segment, display_order, is_active) VALUES ('36a26981-b84c-41f8-be27-74e0d726dd56', 'c16f135c-89cf-4c1c-87f1-01249abd8fb0', 'vale_refeicao', 'Vale refeição', NULL, NULL, 150, TRUE);
INSERT INTO expense_subcategories (id, category_id, code, name, group_label, segment, display_order, is_active) VALUES ('82a5cf03-34c6-4e8f-b0b1-ffa29a460b64', 'c16f135c-89cf-4c1c-87f1-01249abd8fb0', 'plano_de_saude', 'Plano de saúde', NULL, NULL, 160, TRUE);
INSERT INTO expense_subcategories (id, category_id, code, name, group_label, segment, display_order, is_active) VALUES ('bd4193ce-4423-45ac-a36f-86a011a55ff9', 'c16f135c-89cf-4c1c-87f1-01249abd8fb0', 'uniformes', 'Uniformes', NULL, NULL, 170, TRUE);
INSERT INTO expense_subcategories (id, category_id, code, name, group_label, segment, display_order, is_active) VALUES ('da28682d-551d-48e1-9363-1389578b88db', 'c16f135c-89cf-4c1c-87f1-01249abd8fb0', 'seguranca', 'Segurança', NULL, NULL, 180, TRUE);
INSERT INTO expense_subcategories (id, category_id, code, name, group_label, segment, display_order, is_active) VALUES ('575bb8bf-867a-444f-b38e-1fdd1bda48e7', 'c16f135c-89cf-4c1c-87f1-01249abd8fb0', 'limpeza', 'Limpeza', NULL, NULL, 190, TRUE);
INSERT INTO expense_subcategories (id, category_id, code, name, group_label, segment, display_order, is_active) VALUES ('cc7b32f8-6ccd-4426-b8f5-cd3a9aecb29d', 'c16f135c-89cf-4c1c-87f1-01249abd8fb0', 'vigilancia', 'Vigilância', NULL, NULL, 200, TRUE);
INSERT INTO expense_subcategories (id, category_id, code, name, group_label, segment, display_order, is_active) VALUES ('7c7d8ae9-8aaf-4b03-9934-f8e3b394e86b', 'c16f135c-89cf-4c1c-87f1-01249abd8fb0', 'licencas_de_software', 'Licenças de software', NULL, NULL, 210, TRUE);
INSERT INTO expense_subcategories (id, category_id, code, name, group_label, segment, display_order, is_active) VALUES ('503a3c52-2e21-499c-abe5-7620309da57f', 'c068b0fa-89d6-439b-925b-1f32f02ae62a', 'almoco', 'Almoço', NULL, NULL, 10, TRUE);
INSERT INTO expense_subcategories (id, category_id, code, name, group_label, segment, display_order, is_active) VALUES ('d740eb8b-4481-4aef-b9ac-a29d8f29fa55', 'c068b0fa-89d6-439b-925b-1f32f02ae62a', 'combustivel', 'Combustível', NULL, NULL, 20, TRUE);
INSERT INTO expense_subcategories (id, category_id, code, name, group_label, segment, display_order, is_active) VALUES ('55bf9210-ce01-46dd-8394-27dd3f81b7ab', 'c068b0fa-89d6-439b-925b-1f32f02ae62a', 'pedagio', 'Pedágio', NULL, NULL, 30, TRUE);
INSERT INTO expense_subcategories (id, category_id, code, name, group_label, segment, display_order, is_active) VALUES ('3c273cb7-ae85-4cd0-ad9f-4b3ff94771b1', 'c068b0fa-89d6-439b-925b-1f32f02ae62a', 'estacionamento', 'Estacionamento', NULL, NULL, 40, TRUE);
INSERT INTO expense_subcategories (id, category_id, code, name, group_label, segment, display_order, is_active) VALUES ('18ac1920-b93c-43f9-8219-ac96fb5d8d26', 'c068b0fa-89d6-439b-925b-1f32f02ae62a', 'frete', 'Frete', NULL, NULL, 50, TRUE);
INSERT INTO expense_subcategories (id, category_id, code, name, group_label, segment, display_order, is_active) VALUES ('37f21943-757d-4f7c-8b89-8d9595da80fd', 'c068b0fa-89d6-439b-925b-1f32f02ae62a', 'uber_99', 'Uber/99', NULL, NULL, 60, TRUE);
INSERT INTO expense_subcategories (id, category_id, code, name, group_label, segment, display_order, is_active) VALUES ('fe300688-4a42-48b5-810f-8ebf54f5566a', 'c068b0fa-89d6-439b-925b-1f32f02ae62a', 'alimentacao_da_equipe', 'Alimentação da equipe', NULL, NULL, 70, TRUE);
INSERT INTO expense_subcategories (id, category_id, code, name, group_label, segment, display_order, is_active) VALUES ('8c9d64fe-25b7-47ec-9e93-3830ef852840', 'c068b0fa-89d6-439b-925b-1f32f02ae62a', 'hospedagem', 'Hospedagem', NULL, NULL, 80, TRUE);
INSERT INTO expense_subcategories (id, category_id, code, name, group_label, segment, display_order, is_active) VALUES ('b642741a-51f7-4f69-be1d-01ecc0daf504', 'c068b0fa-89d6-439b-925b-1f32f02ae62a', 'material_de_escritorio', 'Material de escritório', NULL, NULL, 90, TRUE);
INSERT INTO expense_subcategories (id, category_id, code, name, group_label, segment, display_order, is_active) VALUES ('a0f04c70-ba47-4852-b31f-a82e8d0eba17', 'c068b0fa-89d6-439b-925b-1f32f02ae62a', 'impressoes', 'Impressões', NULL, NULL, 100, TRUE);
INSERT INTO expense_subcategories (id, category_id, code, name, group_label, segment, display_order, is_active) VALUES ('56506b93-bdf0-43ec-baee-3b6e8de64f5b', 'c068b0fa-89d6-439b-925b-1f32f02ae62a', 'ferramentas', 'Ferramentas', NULL, NULL, 110, TRUE);
INSERT INTO expense_subcategories (id, category_id, code, name, group_label, segment, display_order, is_active) VALUES ('cfc9a8c1-cbe2-41fd-b20d-4596ec7ff183', 'c068b0fa-89d6-439b-925b-1f32f02ae62a', 'pequenas_compras', 'Pequenas compras', NULL, NULL, 120, TRUE);
INSERT INTO expense_subcategories (id, category_id, code, name, group_label, segment, display_order, is_active) VALUES ('7b7a6524-1afd-4fb8-adf9-9d0c40d2e58f', 'c068b0fa-89d6-439b-925b-1f32f02ae62a', 'manutencao_emergencial', 'Manutenção emergencial', NULL, NULL, 130, TRUE);
INSERT INTO expense_subcategories (id, category_id, code, name, group_label, segment, display_order, is_active) VALUES ('d84adf1f-b4fe-46a7-bb2b-e24975645910', 'c068b0fa-89d6-439b-925b-1f32f02ae62a', 'compra_de_epis', 'Compra de EPIs', NULL, NULL, 140, TRUE);
INSERT INTO expense_subcategories (id, category_id, code, name, group_label, segment, display_order, is_active) VALUES ('f8ee8330-d3a2-4e16-9983-2d087fe0e3c2', 'c068b0fa-89d6-439b-925b-1f32f02ae62a', 'marketing', 'Marketing', NULL, NULL, 150, TRUE);
INSERT INTO expense_subcategories (id, category_id, code, name, group_label, segment, display_order, is_active) VALUES ('d75f6c00-d648-40a4-833e-f9c318da7b92', 'c068b0fa-89d6-439b-925b-1f32f02ae62a', 'google_ads', 'Google Ads', NULL, NULL, 160, TRUE);
INSERT INTO expense_subcategories (id, category_id, code, name, group_label, segment, display_order, is_active) VALUES ('2b1d86c1-0282-4f26-90ad-f4671799158e', 'c068b0fa-89d6-439b-925b-1f32f02ae62a', 'facebook_ads', 'Facebook Ads', NULL, NULL, 170, TRUE);
INSERT INTO expense_subcategories (id, category_id, code, name, group_label, segment, display_order, is_active) VALUES ('c945713b-07c7-4291-b104-6a2ca42c5e31', 'c068b0fa-89d6-439b-925b-1f32f02ae62a', 'brindes', 'Brindes', NULL, NULL, 180, TRUE);
INSERT INTO expense_subcategories (id, category_id, code, name, group_label, segment, display_order, is_active) VALUES ('8fd44379-1d71-429d-b87e-216f6aa4bd30', 'c068b0fa-89d6-439b-925b-1f32f02ae62a', 'eventos', 'Eventos', NULL, NULL, 190, TRUE);
INSERT INTO expense_subcategories (id, category_id, code, name, group_label, segment, display_order, is_active) VALUES ('8a5e8126-fe21-47e3-b823-684597da820e', 'ffefdb43-1cc5-48e5-bb20-c17352ea007a', '13o_salario', '13º salário', NULL, NULL, 10, TRUE);
INSERT INTO expense_subcategories (id, category_id, code, name, group_label, segment, display_order, is_active) VALUES ('4798ed54-6d5e-4013-8118-62632a7ace31', 'ffefdb43-1cc5-48e5-bb20-c17352ea007a', 'ferias', 'Férias', NULL, NULL, 20, TRUE);
INSERT INTO expense_subcategories (id, category_id, code, name, group_label, segment, display_order, is_active) VALUES ('689c8bfd-2afa-42f1-88d5-8065e08c50f5', 'ffefdb43-1cc5-48e5-bb20-c17352ea007a', 'rescisoes', 'Rescisões', NULL, NULL, 30, TRUE);
INSERT INTO expense_subcategories (id, category_id, code, name, group_label, segment, display_order, is_active) VALUES ('89168d25-7cef-412b-84c8-1ebd83fe06c1', 'ffefdb43-1cc5-48e5-bb20-c17352ea007a', 'fgts', 'FGTS', NULL, NULL, 40, TRUE);
INSERT INTO expense_subcategories (id, category_id, code, name, group_label, segment, display_order, is_active) VALUES ('2f164b1c-b831-48d3-a5f5-fee81a49d68b', 'ffefdb43-1cc5-48e5-bb20-c17352ea007a', 'inss', 'INSS', NULL, NULL, 50, TRUE);
INSERT INTO expense_subcategories (id, category_id, code, name, group_label, segment, display_order, is_active) VALUES ('06b22f6b-bd51-47a1-a98e-aacb5ef7f668', 'ffefdb43-1cc5-48e5-bb20-c17352ea007a', 'reserva_de_caixa', 'Reserva de caixa', NULL, NULL, 60, TRUE);
INSERT INTO expense_subcategories (id, category_id, code, name, group_label, segment, display_order, is_active) VALUES ('f6401492-0772-4807-b458-c9e0c8e718c9', 'ffefdb43-1cc5-48e5-bb20-c17352ea007a', 'fundo_emergencial', 'Fundo emergencial', NULL, NULL, 70, TRUE);
INSERT INTO expense_subcategories (id, category_id, code, name, group_label, segment, display_order, is_active) VALUES ('0f59459d-3e39-43e7-b5a2-7b66df447084', 'ffefdb43-1cc5-48e5-bb20-c17352ea007a', 'renovacao_de_frota', 'Renovação de frota', NULL, NULL, 80, TRUE);
INSERT INTO expense_subcategories (id, category_id, code, name, group_label, segment, display_order, is_active) VALUES ('992ca589-2990-401a-be78-55772f177eec', 'ffefdb43-1cc5-48e5-bb20-c17352ea007a', 'troca_de_equipamentos', 'Troca de equipamentos', NULL, NULL, 90, TRUE);
INSERT INTO expense_subcategories (id, category_id, code, name, group_label, segment, display_order, is_active) VALUES ('f43ce47d-98e1-44df-8718-571b21e2967e', 'ffefdb43-1cc5-48e5-bb20-c17352ea007a', 'compra_futura_de_maquinas', 'Compra futura de máquinas', NULL, NULL, 100, TRUE);
INSERT INTO expense_subcategories (id, category_id, code, name, group_label, segment, display_order, is_active) VALUES ('47fc6a47-f4ee-4495-821f-da5617b372fa', 'ffefdb43-1cc5-48e5-bb20-c17352ea007a', 'expansao_da_empresa', 'Expansão da empresa', NULL, NULL, 110, TRUE);
INSERT INTO expense_subcategories (id, category_id, code, name, group_label, segment, display_order, is_active) VALUES ('d99a5059-86d4-4b10-967c-4124a2ef9341', 'ffefdb43-1cc5-48e5-bb20-c17352ea007a', 'reserva_tributaria', 'Reserva tributária', NULL, NULL, 120, TRUE);
INSERT INTO expense_subcategories (id, category_id, code, name, group_label, segment, display_order, is_active) VALUES ('32243239-fd31-49ff-a415-dc37c430e81a', 'ffefdb43-1cc5-48e5-bb20-c17352ea007a', 'capital_de_giro', 'Capital de giro', NULL, NULL, 130, TRUE);
INSERT INTO expense_subcategories (id, category_id, code, name, group_label, segment, display_order, is_active) VALUES ('8cd3c966-f02e-47b2-be25-1b6939498475', 'dcc20ce3-8418-4283-9513-ee820442454d', 'das', 'DAS', NULL, NULL, 10, TRUE);
INSERT INTO expense_subcategories (id, category_id, code, name, group_label, segment, display_order, is_active) VALUES ('a060db6f-992d-45db-b0c7-cac05a1c7f60', 'dcc20ce3-8418-4283-9513-ee820442454d', 'simples_nacional', 'Simples Nacional', NULL, NULL, 20, TRUE);
INSERT INTO expense_subcategories (id, category_id, code, name, group_label, segment, display_order, is_active) VALUES ('09ffa86b-b4f5-4aed-9303-a18a33b4e37c', 'dcc20ce3-8418-4283-9513-ee820442454d', 'iss', 'ISS', NULL, NULL, 30, TRUE);
INSERT INTO expense_subcategories (id, category_id, code, name, group_label, segment, display_order, is_active) VALUES ('0c335d6b-0f53-4f17-8e08-8450e2fba189', 'dcc20ce3-8418-4283-9513-ee820442454d', 'icms', 'ICMS', NULL, NULL, 40, TRUE);
INSERT INTO expense_subcategories (id, category_id, code, name, group_label, segment, display_order, is_active) VALUES ('8b377d9e-eb44-461d-9d9d-85c408e02e45', 'dcc20ce3-8418-4283-9513-ee820442454d', 'pis', 'PIS', NULL, NULL, 50, TRUE);
INSERT INTO expense_subcategories (id, category_id, code, name, group_label, segment, display_order, is_active) VALUES ('17b649fe-ade7-462d-a144-0f87029a8a6a', 'dcc20ce3-8418-4283-9513-ee820442454d', 'cofins', 'COFINS', NULL, NULL, 60, TRUE);
INSERT INTO expense_subcategories (id, category_id, code, name, group_label, segment, display_order, is_active) VALUES ('af09bcef-5db6-4543-8e00-55446a5da562', 'dcc20ce3-8418-4283-9513-ee820442454d', 'irpj', 'IRPJ', NULL, NULL, 70, TRUE);
INSERT INTO expense_subcategories (id, category_id, code, name, group_label, segment, display_order, is_active) VALUES ('ab783dd1-2a4c-45ad-9931-698a31d61839', 'dcc20ce3-8418-4283-9513-ee820442454d', 'csll', 'CSLL', NULL, NULL, 80, TRUE);
INSERT INTO expense_subcategories (id, category_id, code, name, group_label, segment, display_order, is_active) VALUES ('e9fe2235-0469-4b41-b2b4-e94a40be0ee0', 'dcc20ce3-8418-4283-9513-ee820442454d', 'iptu', 'IPTU', NULL, NULL, 90, TRUE);
INSERT INTO expense_subcategories (id, category_id, code, name, group_label, segment, display_order, is_active) VALUES ('d7b62227-22a3-4831-8e49-9875be6d4df6', 'dcc20ce3-8418-4283-9513-ee820442454d', 'ipva', 'IPVA', NULL, NULL, 100, TRUE);
INSERT INTO expense_subcategories (id, category_id, code, name, group_label, segment, display_order, is_active) VALUES ('2ffc4001-4721-4549-af46-122437bad234', 'dcc20ce3-8418-4283-9513-ee820442454d', 'taxas_municipais', 'Taxas municipais', NULL, NULL, 110, TRUE);
INSERT INTO expense_subcategories (id, category_id, code, name, group_label, segment, display_order, is_active) VALUES ('812d5811-6401-4357-aa1a-2c0a225eb6f1', 'dcc20ce3-8418-4283-9513-ee820442454d', 'taxas_estaduais', 'Taxas estaduais', NULL, NULL, 120, TRUE);
INSERT INTO expense_subcategories (id, category_id, code, name, group_label, segment, display_order, is_active) VALUES ('b3f53c27-af69-4b50-990f-25151a78ad5f', 'baf37452-2e94-49a7-916a-5f88918b4eff', 'compra_de_maquinas', 'Compra de máquinas', NULL, NULL, 10, TRUE);
INSERT INTO expense_subcategories (id, category_id, code, name, group_label, segment, display_order, is_active) VALUES ('c451eb35-dfdc-4ef1-86d6-9988e70a5e7e', 'baf37452-2e94-49a7-916a-5f88918b4eff', 'compra_de_computadores', 'Compra de computadores', NULL, NULL, 20, TRUE);
INSERT INTO expense_subcategories (id, category_id, code, name, group_label, segment, display_order, is_active) VALUES ('97936ac4-471e-4850-9936-7120f07aa8da', 'baf37452-2e94-49a7-916a-5f88918b4eff', 'compra_de_veiculos', 'Compra de veículos', NULL, NULL, 30, TRUE);
INSERT INTO expense_subcategories (id, category_id, code, name, group_label, segment, display_order, is_active) VALUES ('2b5900b8-4b00-403a-8c88-c54fbed1e420', 'baf37452-2e94-49a7-916a-5f88918b4eff', 'compra_de_ferramentas', 'Compra de ferramentas', NULL, NULL, 40, TRUE);
INSERT INTO expense_subcategories (id, category_id, code, name, group_label, segment, display_order, is_active) VALUES ('8ee15a7d-26ed-4a0a-8b59-d263c4cdbdf8', 'baf37452-2e94-49a7-916a-5f88918b4eff', 'cursos', 'Cursos', NULL, NULL, 50, TRUE);
INSERT INTO expense_subcategories (id, category_id, code, name, group_label, segment, display_order, is_active) VALUES ('a21d6bdb-cc9a-4aca-b941-b00e983ec963', 'baf37452-2e94-49a7-916a-5f88918b4eff', 'treinamentos', 'Treinamentos', NULL, NULL, 60, TRUE);
INSERT INTO expense_subcategories (id, category_id, code, name, group_label, segment, display_order, is_active) VALUES ('81cfe911-5810-4e50-b0ee-aaa9fa62df83', 'baf37452-2e94-49a7-916a-5f88918b4eff', 'consultorias', 'Consultorias', NULL, NULL, 70, TRUE);
INSERT INTO expense_subcategories (id, category_id, code, name, group_label, segment, display_order, is_active) VALUES ('471e27ce-fb03-4061-9025-21b83f58660b', 'baf37452-2e94-49a7-916a-5f88918b4eff', 'expansao_comercial', 'Expansão comercial', NULL, NULL, 80, TRUE);
INSERT INTO expense_subcategories (id, category_id, code, name, group_label, segment, display_order, is_active) VALUES ('bcc7e059-d326-4d5a-b729-ad888edde48a', 'd43c1cde-2c6d-42f4-b1b4-8f661b80a848', 'manutencao_de_veiculos', 'Manutenção de veículos', NULL, NULL, 10, TRUE);
INSERT INTO expense_subcategories (id, category_id, code, name, group_label, segment, display_order, is_active) VALUES ('f2af5704-78ce-450c-af9d-4cda08de843c', 'd43c1cde-2c6d-42f4-b1b4-8f661b80a848', 'manutencao_predial', 'Manutenção predial', NULL, NULL, 20, TRUE);
INSERT INTO expense_subcategories (id, category_id, code, name, group_label, segment, display_order, is_active) VALUES ('2d780579-8bb0-4f7b-bb94-3bda1d4195d5', 'd43c1cde-2c6d-42f4-b1b4-8f661b80a848', 'manutencao_de_maquinas', 'Manutenção de máquinas', NULL, NULL, 30, TRUE);
INSERT INTO expense_subcategories (id, category_id, code, name, group_label, segment, display_order, is_active) VALUES ('6a9da9f2-c523-4270-82fc-cd71bac86130', 'd43c1cde-2c6d-42f4-b1b4-8f661b80a848', 'manutencao_de_ferramentas', 'Manutenção de ferramentas', NULL, NULL, 40, TRUE);
INSERT INTO expense_subcategories (id, category_id, code, name, group_label, segment, display_order, is_active) VALUES ('9b2d1fa5-ec22-4efb-89cb-52e721b0a7f5', 'd43c1cde-2c6d-42f4-b1b4-8f661b80a848', 'troca_de_pecas', 'Troca de peças', NULL, NULL, 50, TRUE);
INSERT INTO expense_subcategories (id, category_id, code, name, group_label, segment, display_order, is_active) VALUES ('29a93950-4e5e-4cfd-a54e-8d3e3640fff8', 'd43c1cde-2c6d-42f4-b1b4-8f661b80a848', 'servicos_terceirizados', 'Serviços terceirizados', NULL, NULL, 60, TRUE);
INSERT INTO expense_subcategories (id, category_id, code, name, group_label, segment, display_order, is_active) VALUES ('ecbea9f0-35a2-4ec3-b582-190ca62420f6', 'f9ad3afe-99d4-4a37-9a63-7cf75602b771', 'tarifas_bancarias', 'Tarifas bancárias', NULL, NULL, 10, TRUE);
INSERT INTO expense_subcategories (id, category_id, code, name, group_label, segment, display_order, is_active) VALUES ('eb1e01bc-474e-4c88-8987-b01ca51f4997', 'f9ad3afe-99d4-4a37-9a63-7cf75602b771', 'juros', 'Juros', NULL, NULL, 20, TRUE);
INSERT INTO expense_subcategories (id, category_id, code, name, group_label, segment, display_order, is_active) VALUES ('25f5b22e-fa8b-41f9-a3c1-e2724cc4380d', 'f9ad3afe-99d4-4a37-9a63-7cf75602b771', 'multas', 'Multas', NULL, NULL, 30, TRUE);
INSERT INTO expense_subcategories (id, category_id, code, name, group_label, segment, display_order, is_active) VALUES ('f9ccb3da-e187-41ca-9170-13ea9dd02366', 'f9ad3afe-99d4-4a37-9a63-7cf75602b771', 'iof', 'IOF', NULL, NULL, 40, TRUE);
INSERT INTO expense_subcategories (id, category_id, code, name, group_label, segment, display_order, is_active) VALUES ('d9c54c92-47ed-457d-a204-a26d323f808f', 'f9ad3afe-99d4-4a37-9a63-7cf75602b771', 'taxas_de_cartao', 'Taxas de cartão', NULL, NULL, 50, TRUE);
INSERT INTO expense_subcategories (id, category_id, code, name, group_label, segment, display_order, is_active) VALUES ('d329f10d-b978-4fdd-928b-03cc07daa7f5', 'f9ad3afe-99d4-4a37-9a63-7cf75602b771', 'antecipacao_de_recebiveis', 'Antecipação de recebíveis', NULL, NULL, 60, TRUE);
INSERT INTO expense_subcategories (id, category_id, code, name, group_label, segment, display_order, is_active) VALUES ('f6debfc6-d9fd-44b9-8c71-24d8710ced55', 'f9ad3afe-99d4-4a37-9a63-7cf75602b771', 'emprestimos', 'Empréstimos', NULL, NULL, 70, TRUE);
INSERT INTO expense_subcategories (id, category_id, code, name, group_label, segment, display_order, is_active) VALUES ('5a76a8d0-0425-4393-a266-62810566c8d3', 'f9ad3afe-99d4-4a37-9a63-7cf75602b771', 'financiamentos', 'Financiamentos', NULL, NULL, 80, TRUE);
INSERT INTO expense_subcategories (id, category_id, code, name, group_label, segment, display_order, is_active) VALUES ('95cff95c-5b50-4132-9f1d-2a177a0aa8cf', 'a332c9dd-b691-48bd-b0d0-b88d85915351', 'bonificacoes', 'Bonificações', NULL, NULL, 10, TRUE);
INSERT INTO expense_subcategories (id, category_id, code, name, group_label, segment, display_order, is_active) VALUES ('1278b4ac-0bcc-4c03-a731-ae446147a249', 'a332c9dd-b691-48bd-b0d0-b88d85915351', 'premiacoes', 'Premiações', NULL, NULL, 20, TRUE);
INSERT INTO expense_subcategories (id, category_id, code, name, group_label, segment, display_order, is_active) VALUES ('dc859b65-7b73-4633-ada8-022566e024f0', 'a332c9dd-b691-48bd-b0d0-b88d85915351', 'horas_extras', 'Horas extras', NULL, NULL, 30, TRUE);
INSERT INTO expense_subcategories (id, category_id, code, name, group_label, segment, display_order, is_active) VALUES ('8d3fb89b-0cc0-4d33-994b-5cd441f3bca5', 'a332c9dd-b691-48bd-b0d0-b88d85915351', 'diarias', 'Diárias', NULL, NULL, 40, TRUE);
INSERT INTO expense_subcategories (id, category_id, code, name, group_label, segment, display_order, is_active) VALUES ('bca62286-75ad-41f3-9383-7849227ef9b5', 'a332c9dd-b691-48bd-b0d0-b88d85915351', 'adiantamentos', 'Adiantamentos', NULL, NULL, 50, TRUE);
INSERT INTO expense_subcategories (id, category_id, code, name, group_label, segment, display_order, is_active) VALUES ('2118ca4a-38cd-44fc-a115-230a54b4d55d', 'a332c9dd-b691-48bd-b0d0-b88d85915351', 'ajuda_de_custo', 'Ajuda de custo', NULL, NULL, 60, TRUE);
INSERT INTO expense_subcategories (id, category_id, code, name, group_label, segment, display_order, is_active) VALUES ('b2719a39-f7e4-4ec7-ab3c-ad62f2953951', '61d644f6-a89b-480e-bdde-b8189e2b861d', 'vidros', 'Vidros', NULL, NULL, 10, TRUE);
INSERT INTO expense_subcategories (id, category_id, code, name, group_label, segment, display_order, is_active) VALUES ('a4505b29-81b4-470d-a50e-c1a0b887ce4a', '61d644f6-a89b-480e-bdde-b8189e2b861d', 'ferragens', 'Ferragens', NULL, NULL, 20, TRUE);
INSERT INTO expense_subcategories (id, category_id, code, name, group_label, segment, display_order, is_active) VALUES ('5fedaf3e-0786-4908-87a2-32b6901f4823', '61d644f6-a89b-480e-bdde-b8189e2b861d', 'perfis_de_aluminio', 'Perfis de alumínio', NULL, NULL, 30, TRUE);
INSERT INTO expense_subcategories (id, category_id, code, name, group_label, segment, display_order, is_active) VALUES ('63fe566c-0878-497d-8445-6051884add81', '61d644f6-a89b-480e-bdde-b8189e2b861d', 'silicone', 'Silicone', NULL, NULL, 40, TRUE);
INSERT INTO expense_subcategories (id, category_id, code, name, group_label, segment, display_order, is_active) VALUES ('29b2fc5d-a3e7-46b8-a367-0640aba3659b', '61d644f6-a89b-480e-bdde-b8189e2b861d', 'espelhos', 'Espelhos', NULL, NULL, 50, TRUE);
INSERT INTO expense_subcategories (id, category_id, code, name, group_label, segment, display_order, is_active) VALUES ('2b1f46f1-ba11-4367-9b7e-11199cbe634e', '61d644f6-a89b-480e-bdde-b8189e2b861d', 'parafusos', 'Parafusos', NULL, NULL, 60, TRUE);
INSERT INTO expense_subcategories (id, category_id, code, name, group_label, segment, display_order, is_active) VALUES ('3d3112aa-424e-46aa-bdea-9e00d6ae8574', '61d644f6-a89b-480e-bdde-b8189e2b861d', 'buchas', 'Buchas', NULL, NULL, 70, TRUE);
INSERT INTO expense_subcategories (id, category_id, code, name, group_label, segment, display_order, is_active) VALUES ('aac320b3-54f0-4e23-8aa1-4b8ed39cd962', '61d644f6-a89b-480e-bdde-b8189e2b861d', 'rebites', 'Rebites', NULL, NULL, 80, TRUE);
INSERT INTO expense_subcategories (id, category_id, code, name, group_label, segment, display_order, is_active) VALUES ('eebfcdaf-b316-4df5-8bcd-a6129003bb0f', '61d644f6-a89b-480e-bdde-b8189e2b861d', 'cola_uv', 'Cola UV', NULL, NULL, 90, TRUE);
INSERT INTO expense_subcategories (id, category_id, code, name, group_label, segment, display_order, is_active) VALUES ('5213b7dc-be6f-4c7d-9f46-9f98240b1b68', '61d644f6-a89b-480e-bdde-b8189e2b861d', 'fitas', 'Fitas', NULL, NULL, 100, TRUE);
INSERT INTO expense_subcategories (id, category_id, code, name, group_label, segment, display_order, is_active) VALUES ('90751547-03cd-4ce2-943e-29186c773c34', '61d644f6-a89b-480e-bdde-b8189e2b861d', 'materiais_de_instalacao', 'Materiais de instalação', NULL, NULL, 110, TRUE);
INSERT INTO expense_subcategories (id, category_id, code, name, group_label, segment, display_order, is_active) VALUES ('0ebba348-93d7-499d-9940-55f989f47a2c', '61d644f6-a89b-480e-bdde-b8189e2b861d', 'equipamentos_de_protecao', 'Equipamentos de proteção', NULL, NULL, 120, TRUE);
INSERT INTO expense_subcategories (id, category_id, code, name, group_label, segment, display_order, is_active) VALUES ('e871da3b-9fb5-41ee-81a7-e442b7e99c34', '61d644f6-a89b-480e-bdde-b8189e2b861d', 'embalagens', 'Embalagens', NULL, NULL, 130, TRUE);
INSERT INTO expense_subcategories (id, category_id, code, name, group_label, segment, display_order, is_active) VALUES ('2bbde4b7-eedb-4d17-bf8d-68fe4cf12ba2', '61d644f6-a89b-480e-bdde-b8189e2b861d', 'transporte', 'Transporte', NULL, NULL, 140, TRUE);
INSERT INTO expense_subcategories (id, category_id, code, name, group_label, segment, display_order, is_active) VALUES ('992a6c67-210d-438c-bcbf-b987a482589c', '61d644f6-a89b-480e-bdde-b8189e2b861d', 'servicos_terceirizados', 'Serviços terceirizados', NULL, NULL, 150, TRUE);
INSERT INTO expense_subcategories (id, category_id, code, name, group_label, segment, display_order, is_active) VALUES ('95753093-88b1-4c11-8012-0055db61c759', '2f81119e-6705-4c54-9ba0-83550bd277a0', 'papelaria', 'Papelaria', NULL, NULL, 10, TRUE);
INSERT INTO expense_subcategories (id, category_id, code, name, group_label, segment, display_order, is_active) VALUES ('08a0157b-1837-4e7f-9315-cc60a2b515fc', '2f81119e-6705-4c54-9ba0-83550bd277a0', 'cartuchos', 'Cartuchos', NULL, NULL, 20, TRUE);
INSERT INTO expense_subcategories (id, category_id, code, name, group_label, segment, display_order, is_active) VALUES ('a277afdf-3738-4286-baf8-19b4e24f66a5', '2f81119e-6705-4c54-9ba0-83550bd277a0', 'impressoras', 'Impressoras', NULL, NULL, 30, TRUE);
INSERT INTO expense_subcategories (id, category_id, code, name, group_label, segment, display_order, is_active) VALUES ('216a22cd-d3fd-4dd9-965f-93a27955caf5', '2f81119e-6705-4c54-9ba0-83550bd277a0', 'moveis', 'Móveis', NULL, NULL, 40, TRUE);
INSERT INTO expense_subcategories (id, category_id, code, name, group_label, segment, display_order, is_active) VALUES ('f8720219-f9f6-4399-8d80-430afec2d46e', '2f81119e-6705-4c54-9ba0-83550bd277a0', 'equipamentos', 'Equipamentos', NULL, NULL, 50, TRUE);
INSERT INTO expense_subcategories (id, category_id, code, name, group_label, segment, display_order, is_active) VALUES ('73758399-378f-464f-bcee-a306bdec292b', '2f81119e-6705-4c54-9ba0-83550bd277a0', 'assinaturas', 'Assinaturas', NULL, NULL, 60, TRUE);
INSERT INTO expense_subcategories (id, category_id, code, name, group_label, segment, display_order, is_active) VALUES ('57e668a5-ef15-4a38-8703-84e8f886b80e', '2f81119e-6705-4c54-9ba0-83550bd277a0', 'servicos_administrativos', 'Serviços administrativos', NULL, NULL, 70, TRUE);
INSERT INTO expense_subcategories (id, category_id, code, name, group_label, segment, display_order, is_active) VALUES ('69c23d00-9c4c-46ab-9600-ad46935bfca0', 'c068b0fa-89d6-439b-925b-1f32f02ae62a', 'vidro_temperado', 'Vidro temperado', 'Insumos', 'VIDRACARIA', 200, TRUE);
INSERT INTO expense_subcategories (id, category_id, code, name, group_label, segment, display_order, is_active) VALUES ('ea12b9c7-e8f9-45a1-8d8d-ae9385fa46fb', 'c068b0fa-89d6-439b-925b-1f32f02ae62a', 'vidro_laminado', 'Vidro laminado', 'Insumos', 'VIDRACARIA', 210, TRUE);
INSERT INTO expense_subcategories (id, category_id, code, name, group_label, segment, display_order, is_active) VALUES ('6bcc4d58-1cda-44ac-9cf3-74567224567f', 'c068b0fa-89d6-439b-925b-1f32f02ae62a', 'vidro_comum', 'Vidro comum', 'Insumos', 'VIDRACARIA', 220, TRUE);
INSERT INTO expense_subcategories (id, category_id, code, name, group_label, segment, display_order, is_active) VALUES ('5df3054c-b24f-490f-b051-c6bb5de3c1bd', 'c068b0fa-89d6-439b-925b-1f32f02ae62a', 'espelho', 'Espelho', 'Insumos', 'VIDRACARIA', 230, TRUE);
INSERT INTO expense_subcategories (id, category_id, code, name, group_label, segment, display_order, is_active) VALUES ('f9f112bc-a580-44f6-aaa9-c8a4d86880c1', 'c068b0fa-89d6-439b-925b-1f32f02ae62a', 'vidro_serigrafado', 'Vidro serigrafado', 'Insumos', 'VIDRACARIA', 240, TRUE);
INSERT INTO expense_subcategories (id, category_id, code, name, group_label, segment, display_order, is_active) VALUES ('91af1be3-cc9e-4873-953f-d96db59fd064', 'c068b0fa-89d6-439b-925b-1f32f02ae62a', 'vidro_jateado', 'Vidro jateado', 'Insumos', 'VIDRACARIA', 250, TRUE);
INSERT INTO expense_subcategories (id, category_id, code, name, group_label, segment, display_order, is_active) VALUES ('d05a4394-88a6-47f7-99df-3693265aa096', 'c068b0fa-89d6-439b-925b-1f32f02ae62a', 'roldanas', 'Roldanas', 'Ferragens', 'VIDRACARIA', 260, TRUE);
INSERT INTO expense_subcategories (id, category_id, code, name, group_label, segment, display_order, is_active) VALUES ('a96a39c3-3ff8-4917-85e0-1daec57f06ed', 'c068b0fa-89d6-439b-925b-1f32f02ae62a', 'trilhos', 'Trilhos', 'Ferragens', 'VIDRACARIA', 270, TRUE);
INSERT INTO expense_subcategories (id, category_id, code, name, group_label, segment, display_order, is_active) VALUES ('fd0fc4ef-e606-43af-b889-6e0a311cf2ab', 'c068b0fa-89d6-439b-925b-1f32f02ae62a', 'puxadores', 'Puxadores', 'Ferragens', 'VIDRACARIA', 280, TRUE);
INSERT INTO expense_subcategories (id, category_id, code, name, group_label, segment, display_order, is_active) VALUES ('b4125da1-c68a-41cc-a94f-b870d1a33bbe', 'c068b0fa-89d6-439b-925b-1f32f02ae62a', 'dobradicas', 'Dobradiças', 'Ferragens', 'VIDRACARIA', 290, TRUE);
INSERT INTO expense_subcategories (id, category_id, code, name, group_label, segment, display_order, is_active) VALUES ('b7ab99c4-90d2-44ef-bb2e-5851e8d41b5c', 'c068b0fa-89d6-439b-925b-1f32f02ae62a', 'fechaduras', 'Fechaduras', 'Ferragens', 'VIDRACARIA', 300, TRUE);
INSERT INTO expense_subcategories (id, category_id, code, name, group_label, segment, display_order, is_active) VALUES ('33e539f9-93c6-49e3-b258-635e2e6a1326', 'c068b0fa-89d6-439b-925b-1f32f02ae62a', 'kits_para_box', 'Kits para Box', 'Ferragens', 'VIDRACARIA', 310, TRUE);
INSERT INTO expense_subcategories (id, category_id, code, name, group_label, segment, display_order, is_active) VALUES ('1a58af76-5a89-435d-8e56-f589b2ffa614', 'c068b0fa-89d6-439b-925b-1f32f02ae62a', 'kits_para_sacada', 'Kits para Sacada', 'Ferragens', 'VIDRACARIA', 320, TRUE);
INSERT INTO expense_subcategories (id, category_id, code, name, group_label, segment, display_order, is_active) VALUES ('eb3998f3-c2d7-41f9-9de5-74d82223825d', 'c068b0fa-89d6-439b-925b-1f32f02ae62a', 'kits_para_guarda_corpo', 'Kits para Guarda-Corpo', 'Ferragens', 'VIDRACARIA', 330, TRUE);
INSERT INTO expense_subcategories (id, category_id, code, name, group_label, segment, display_order, is_active) VALUES ('1e155813-b513-405e-a9fa-fa9ff13c56a3', 'c068b0fa-89d6-439b-925b-1f32f02ae62a', 'linha_suprema', 'Linha Suprema', 'Alumínio', 'VIDRACARIA', 340, TRUE);
INSERT INTO expense_subcategories (id, category_id, code, name, group_label, segment, display_order, is_active) VALUES ('0108dcf2-d180-424f-9f11-18b0e6d30d5d', 'c068b0fa-89d6-439b-925b-1f32f02ae62a', 'linha_gold', 'Linha Gold', 'Alumínio', 'VIDRACARIA', 350, TRUE);
INSERT INTO expense_subcategories (id, category_id, code, name, group_label, segment, display_order, is_active) VALUES ('04ff2d10-11f1-4682-9bc7-ef9ae4938602', 'c068b0fa-89d6-439b-925b-1f32f02ae62a', 'linha_25', 'Linha 25', 'Alumínio', 'VIDRACARIA', 360, TRUE);
INSERT INTO expense_subcategories (id, category_id, code, name, group_label, segment, display_order, is_active) VALUES ('1433141c-00b5-4831-8bbd-0451b8e95757', 'c068b0fa-89d6-439b-925b-1f32f02ae62a', 'linha_30', 'Linha 30', 'Alumínio', 'VIDRACARIA', 370, TRUE);
INSERT INTO expense_subcategories (id, category_id, code, name, group_label, segment, display_order, is_active) VALUES ('a0667b86-23af-438c-8aa1-21e81b241bb5', 'c068b0fa-89d6-439b-925b-1f32f02ae62a', 'linha_42', 'Linha 42', 'Alumínio', 'VIDRACARIA', 380, TRUE);
INSERT INTO expense_subcategories (id, category_id, code, name, group_label, segment, display_order, is_active) VALUES ('fcccb678-ee64-4498-aaf2-db555f43e04e', 'c068b0fa-89d6-439b-925b-1f32f02ae62a', 'linha_integrada', 'Linha Integrada', 'Alumínio', 'VIDRACARIA', 390, TRUE);
INSERT INTO expense_subcategories (id, category_id, code, name, group_label, segment, display_order, is_active) VALUES ('966f350f-8522-4304-ba25-505823f4ca9c', 'c068b0fa-89d6-439b-925b-1f32f02ae62a', 'linha_fachada', 'Linha Fachada', 'Alumínio', 'VIDRACARIA', 400, TRUE);
INSERT INTO expense_subcategories (id, category_id, code, name, group_label, segment, display_order, is_active) VALUES ('d58e8b1c-c331-4389-a496-eac3601dca08', 'c068b0fa-89d6-439b-925b-1f32f02ae62a', 'tempera', 'Têmpera', 'Serviços', 'VIDRACARIA', 410, TRUE);
INSERT INTO expense_subcategories (id, category_id, code, name, group_label, segment, display_order, is_active) VALUES ('1f3ceb7b-de6f-47cd-833f-a2fefd4456fa', 'c068b0fa-89d6-439b-925b-1f32f02ae62a', 'lapidacao', 'Lapidação', 'Serviços', 'VIDRACARIA', 420, TRUE);
INSERT INTO expense_subcategories (id, category_id, code, name, group_label, segment, display_order, is_active) VALUES ('e0dc7b30-12a4-42e3-8d6e-d86061ecbb2f', 'c068b0fa-89d6-439b-925b-1f32f02ae62a', 'furacao', 'Furação', 'Serviços', 'VIDRACARIA', 430, TRUE);
INSERT INTO expense_subcategories (id, category_id, code, name, group_label, segment, display_order, is_active) VALUES ('5343bc00-0959-4652-a2fb-b689cbebf32f', 'c068b0fa-89d6-439b-925b-1f32f02ae62a', 'recortes', 'Recortes', 'Serviços', 'VIDRACARIA', 440, TRUE);
INSERT INTO expense_subcategories (id, category_id, code, name, group_label, segment, display_order, is_active) VALUES ('b1725f23-1819-483e-a0ea-3afdd7cd34fd', 'c068b0fa-89d6-439b-925b-1f32f02ae62a', 'pintura', 'Pintura', 'Serviços', 'VIDRACARIA', 450, TRUE);
INSERT INTO expense_subcategories (id, category_id, code, name, group_label, segment, display_order, is_active) VALUES ('be55c158-008e-4bb2-adf6-2eb48c66c790', 'c068b0fa-89d6-439b-925b-1f32f02ae62a', 'jateamento', 'Jateamento', 'Serviços', 'VIDRACARIA', 460, TRUE);
INSERT INTO expense_subcategories (id, category_id, code, name, group_label, segment, display_order, is_active) VALUES ('a15dca3f-fd42-407f-b396-d79a196b88f4', 'c068b0fa-89d6-439b-925b-1f32f02ae62a', 'transporte', 'Transporte', 'Serviços', 'VIDRACARIA', 470, TRUE);
INSERT INTO expense_subcategories (id, category_id, code, name, group_label, segment, display_order, is_active) VALUES ('0123ce02-b24b-4f79-b07c-ba1c97ebc3ae', 'c068b0fa-89d6-439b-925b-1f32f02ae62a', 'instalacao', 'Instalação', 'Serviços', 'VIDRACARIA', 480, TRUE);
INSERT INTO expense_subcategories (id, category_id, code, name, group_label, segment, display_order, is_active) VALUES ('0a0f1368-83c0-46a2-a1a9-aeade7acbeca', 'c068b0fa-89d6-439b-925b-1f32f02ae62a', 'terceirizacao', 'Terceirização', 'Serviços', 'VIDRACARIA', 490, TRUE);
