
-- ── TABELA: cut_plans ──────────────────────────────────────
CREATE TABLE IF NOT EXISTS cut_plans (
    id UUID PRIMARY KEY,
    work_order_id UUID REFERENCES work_orders(id) ON DELETE SET NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'DRAFT',
    version INTEGER NOT NULL DEFAULT 1,
    description TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by_id UUID REFERENCES app_users(id) ON DELETE SET NULL,
    updated_by_id UUID REFERENCES app_users(id) ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS idx_cut_plans_work_order_id ON cut_plans(work_order_id);
CREATE INDEX IF NOT EXISTS idx_cut_plans_status ON cut_plans(status);

-- ── TABELA: cut_plan_items ──────────────────────────────────
CREATE TABLE IF NOT EXISTS cut_plan_items (
    id UUID PRIMARY KEY,
    cut_plan_id UUID NOT NULL REFERENCES cut_plans(id) ON DELETE CASCADE,
    description TEXT,
    environment VARCHAR(200),
    glass_type VARCHAR(100) DEFAULT 'TEMPERADO',
    thickness NUMERIC(6,2) DEFAULT 8,
    color VARCHAR(100) DEFAULT 'INCOLOR',
    finishing VARCHAR(100),
    gross_width NUMERIC(10,2),
    gross_height NUMERIC(10,2),
    final_width NUMERIC(10,2),
    final_height NUMERIC(10,2),
    quantity INTEGER NOT NULL DEFAULT 1,
    calculated_area NUMERIC(14,4),
    estimated_cost NUMERIC(12,2),
    notes TEXT,
    angle NUMERIC(6,2),
    drilling_diameter NUMERIC(6,2),
    drilling_quantity INTEGER,
    drilling_cost_per_unit NUMERIC(10,2),
    notch_description TEXT,
    notch_cost NUMERIC(10,2),
    supplier_id UUID REFERENCES suppliers(id) ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS idx_cut_plan_items_cut_plan_id ON cut_plan_items(cut_plan_id);

-- ── TABELA: cut_plan_history ────────────────────────────────
CREATE TABLE IF NOT EXISTS cut_plan_history (
    id UUID PRIMARY KEY,
    cut_plan_id UUID NOT NULL REFERENCES cut_plans(id) ON DELETE CASCADE,
    changed_by_id UUID REFERENCES app_users(id) ON DELETE SET NULL,
    change_type VARCHAR(100),
    description TEXT,
    version INTEGER DEFAULT 1,
    old_values TEXT,
    new_values TEXT,
    changed_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_cut_plan_history_cut_plan_id ON cut_plan_history(cut_plan_id);

-- ── TABELA: glass_cut_rules ─────────────────────────────────
CREATE TABLE IF NOT EXISTS glass_cut_rules (
    id UUID PRIMARY KEY,
    service_category_id UUID NOT NULL REFERENCES service_categories(id) ON DELETE CASCADE,
    rule_type VARCHAR(50) NOT NULL,
    parameter_name VARCHAR(100) NOT NULL,
    value NUMERIC(8,2) NOT NULL,
    unit VARCHAR(20) NOT NULL DEFAULT 'MM',
    description TEXT,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    application_order INTEGER NOT NULL DEFAULT 0,
    CONSTRAINT uk_glass_cut_rules_category_parameter UNIQUE (service_category_id, parameter_name)
);

CREATE INDEX IF NOT EXISTS idx_glass_cut_rules_service_category_id ON glass_cut_rules(service_category_id);
CREATE INDEX IF NOT EXISTS idx_glass_cut_rules_rule_type ON glass_cut_rules(rule_type);
CREATE INDEX IF NOT EXISTS idx_glass_cut_rules_parameter_name ON glass_cut_rules(parameter_name);

-- ── TABELA: glass_templates ──────────────────────────────────
CREATE TABLE IF NOT EXISTS glass_templates (
    id UUID PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    description TEXT,
    service_category_id UUID REFERENCES service_categories(id) ON DELETE SET NULL,
    glass_type VARCHAR(100),
    thickness NUMERIC(6,2),
    color VARCHAR(100),
    finishing VARCHAR(100),
    active BOOLEAN NOT NULL DEFAULT TRUE
);

-- ============================================================
-- SEEDS: Categorias de Serviço (se não existirem)
-- ============================================================
INSERT INTO service_categories (id, name, description) VALUES
    (gen_random_uuid(), 'Box de Banheiro', 'Box de banheiro - temperado correr, abrir, camarão, elegance'),
    (gen_random_uuid(), 'Box até o Teto', 'Box do piso ao teto - altura variável'),
    (gen_random_uuid(), 'Box Flex / Flex Open', 'Box articulado - 2 folhas móveis'),
    (gen_random_uuid(), 'Box Elegance', 'Box com roldana aparente - correr'),
    (gen_random_uuid(), 'Box Camarão', 'Box sanfonado / 4 folhas'),
    (gen_random_uuid(), 'Box Abrir / Pivotante', 'Box de abrir - 1 ou 2 portas + painel fixo'),
    (gen_random_uuid(), 'Cortina de Vidro', 'Painéis pivotantes/deslizantes - fechamento sacada'),
    (gen_random_uuid(), 'Envidraçamento Sacada', 'Sistema deslizante com roldana'),
    (gen_random_uuid(), 'Varanda Europeia', 'Painéis deslizantes e articulados'),
    (gen_random_uuid(), 'Sacada Fixa', 'Painel sem abertura - estrutural'),
    (gen_random_uuid(), 'Janela de Correr', 'Janela de correr 2, 3 ou 4 folhas temperado'),
    (gen_random_uuid(), 'Janela Basculante', 'Janela basculante eixo horizontal'),
    (gen_random_uuid(), 'Janela de Abrir', 'Janela de abrir 1 ou 2 folhas / batente'),
    (gen_random_uuid(), 'Janela Maxar / Projetante', 'Janela maxar eixo superior'),
    (gen_random_uuid(), 'Janela Fixa', 'Painel fixo sem abertura'),
    (gen_random_uuid(), 'Porta Pivotante', 'Porta pivotante - eixo deslocado sem trilho'),
    (gen_random_uuid(), 'Porta de Correr', 'Porta de correr 1 ou 2 folhas em trilho'),
    (gen_random_uuid(), 'Porta de Abrir', 'Porta de abrir - dobradiças laterais'),
    (gen_random_uuid(), 'Porta Sanfonada', 'Porta articulada que dobra lateral'),
    (gen_random_uuid(), 'Versatik Classic', 'Kit Versatik Classic - correr 2 ou 4 folhas - 1 trilho'),
    (gen_random_uuid(), 'Versatik Plus', 'Kit Versatik Plus - correr 4 folhas 2 trilhos'),
    (gen_random_uuid(), 'Versatik Division', 'Kit Versatik Division - divisória interna'),
    (gen_random_uuid(), 'Versatik VT3', 'Kit Versatik VT3 - 3 folhas vão central livre'),
    (gen_random_uuid(), 'Versatik VT6', 'Kit Versatik VT6 - 6 folhas sacadas grandes'),
    (gen_random_uuid(), 'Guarda-corpo com Clamp', 'Guarda-corpo GC com clamp - fixação lateral por pressão'),
    (gen_random_uuid(), 'Guarda-corpo Furo Passante', 'Guarda-corpo GC com furo passante - parafuso passante inox'),
    (gen_random_uuid(), 'Guarda-corpo Perfil U', 'Guarda-corpo GC embutido em perfil U'),
    (gen_random_uuid(), 'Guarda-corpo Laminado', 'Guarda-corpo laminado - coberturas e áreas críticas'),
    (gen_random_uuid(), 'Gabinete de Pia', 'Abrigo de pia - painel frontal + laterais'),
    (gen_random_uuid(), 'Cobertura Laminada', 'Cobertura de vidro laminado - painel inclinado fixo'),
    (gen_random_uuid(), 'Cobertura Metálica', 'Cobertura com estrutura metálica'),
    (gen_random_uuid(), 'Claraboia / Shed', 'Abertura zenital no telhado'),
    (gen_random_uuid(), 'Espelho', 'Espelho lapidado ou bisotado'),
    (gen_random_uuid(), 'Prateleira de Vidro', 'Prateleira de vidro temperado plano'),
    (gen_random_uuid(), 'Divisória de Escritório', 'Divisória de escritório - painel fixo ou com porta'),
    (gen_random_uuid(), 'Tampo de Mesa', 'Tampo de mesa - vidro plano sobre base'),
    (gen_random_uuid(), 'Vitrine / Fachada', 'Vitrine ou fachada - painel externo'),
    (gen_random_uuid(), 'Spider / Frameless', 'Sistema spider frameless - fachada sem caixilho')
ON CONFLICT DO NOTHING;

-- ============================================================
-- SEEDS: Regras por categoria
-- ============================================================

-- BOX DE BANHEIRO (padrão)
DO $$
DECLARE cat_id UUID;
BEGIN
    SELECT id INTO cat_id FROM service_categories WHERE name = 'Box de Banheiro' LIMIT 1;
    IF cat_id IS NOT NULL THEN
        INSERT INTO glass_cut_rules (id, service_category_id, rule_type, parameter_name, value, unit, description, active, application_order)
        VALUES
            (gen_random_uuid(), cat_id, 'DISCOUNT', 'LATERAL_DISCOUNT', 10.0, 'MM', 'Desconto lateral total (5mm cada lado)', TRUE, 1),
            (gen_random_uuid(), cat_id, 'DISCOUNT', 'SUPERIOR_DISCOUNT', 15.0, 'MM', 'Desconto superior (7mm) + inferior (8mm) = 15mm total', TRUE, 2),
            (gen_random_uuid(), cat_id, 'GAP', 'HARDWARE_GAP', 2.0, 'MM', 'Folga para ferragem (ambos os lados)', TRUE, 3),
            (gen_random_uuid(), cat_id, 'GAP', 'SILICONE_GAP', 3.0, 'MM', 'Folga para silicone', TRUE, 4),
            (gen_random_uuid(), cat_id, 'GAP', 'INSTALL_GAP', 2.0, 'MM', 'Folga de instalação', TRUE, 5),
            (gen_random_uuid(), cat_id, 'GAP', 'OVERLAP', 12.5, 'MM', 'Sobrepasse entre folhas (10–15mm)', TRUE, 6)
        ON CONFLICT ON CONSTRAINT uk_glass_cut_rules_category_parameter DO NOTHING;
    END IF;
END$$;

-- BOX ATÉ O TETO
DO $$
DECLARE cat_id UUID;
BEGIN
    SELECT id INTO cat_id FROM service_categories WHERE name = 'Box até o Teto' LIMIT 1;
    IF cat_id IS NOT NULL THEN
        INSERT INTO glass_cut_rules (id, service_category_id, rule_type, parameter_name, value, unit, description, active, application_order)
        VALUES
            (gen_random_uuid(), cat_id, 'DISCOUNT', 'LATERAL_DISCOUNT', 10.0, 'MM', 'Desconto lateral total (5mm cada lado)', TRUE, 1),
            (gen_random_uuid(), cat_id, 'DISCOUNT', 'SUPERIOR_DISCOUNT', 20.0, 'MM', 'Desconto superior 10mm + inferior 10mm', TRUE, 2),
            (gen_random_uuid(), cat_id, 'GAP', 'HARDWARE_GAP', 2.0, 'MM', 'Folga para ferragem', TRUE, 3)
        ON CONFLICT ON CONSTRAINT uk_glass_cut_rules_category_parameter DO NOTHING;
    END IF;
END$$;

-- BOX FLEX
DO $$
DECLARE cat_id UUID;
BEGIN
    SELECT id INTO cat_id FROM service_categories WHERE name = 'Box Flex / Flex Open' LIMIT 1;
    IF cat_id IS NOT NULL THEN
        INSERT INTO glass_cut_rules (id, service_category_id, rule_type, parameter_name, value, unit, description, active, application_order)
        VALUES
            (gen_random_uuid(), cat_id, 'DISCOUNT', 'LATERAL_DISCOUNT', 12.0, 'MM', 'Desconto total lateral para vão (dividir por nº folhas)', TRUE, 1),
            (gen_random_uuid(), cat_id, 'DISCOUNT', 'SUPERIOR_DISCOUNT', 0.0, 'MM', 'Sem desconto vertical (alt. max 1900mm)', TRUE, 2)
        ON CONFLICT ON CONSTRAINT uk_glass_cut_rules_category_parameter DO NOTHING;
    END IF;
END$$;

-- BOX ELEGANCE
DO $$
DECLARE cat_id UUID;
BEGIN
    SELECT id INTO cat_id FROM service_categories WHERE name = 'Box Elegance' LIMIT 1;
    IF cat_id IS NOT NULL THEN
        INSERT INTO glass_cut_rules (id, service_category_id, rule_type, parameter_name, value, unit, description, active, application_order)
        VALUES
            (gen_random_uuid(), cat_id, 'DISCOUNT', 'LATERAL_DISCOUNT', 10.0, 'MM', 'Desconto lateral total', TRUE, 1),
            (gen_random_uuid(), cat_id, 'DISCOUNT', 'SUPERIOR_DISCOUNT', 80.0, 'MM', 'Desconto: roldana sup. + trilho inf.', TRUE, 2),
            (gen_random_uuid(), cat_id, 'GAP', 'ROLLER_HOLE', 13.0, 'MM', 'Furo roldana superior ø12–14mm (2 por folha)', TRUE, 3)
        ON CONFLICT ON CONSTRAINT uk_glass_cut_rules_category_parameter DO NOTHING;
    END IF;
END$$;

-- CORTINA DE VIDRO (sacada)
DO $$
DECLARE cat_id UUID;
BEGIN
    SELECT id INTO cat_id FROM service_categories WHERE name = 'Cortina de Vidro' LIMIT 1;
    IF cat_id IS NOT NULL THEN
        INSERT INTO glass_cut_rules (id, service_category_id, rule_type, parameter_name, value, unit, description, active, application_order)
        VALUES
            (gen_random_uuid(), cat_id, 'DISCOUNT', 'LATERAL_DISCOUNT', 5.0, 'MM', 'Folga 5mm por lateral (escova adesiva)', TRUE, 1),
            (gen_random_uuid(), cat_id, 'DISCOUNT', 'SUPERIOR_DISCOUNT', 20.0, 'MM', 'Desconto vertical: folga trilho inf. + sup.', TRUE, 2)
        ON CONFLICT ON CONSTRAINT uk_glass_cut_rules_category_parameter DO NOTHING;
    END IF;
END$$;

-- ENVIDRAÇAMENTO SACADA
DO $$
DECLARE cat_id UUID;
BEGIN
    SELECT id INTO cat_id FROM service_categories WHERE name = 'Envidraçamento Sacada' LIMIT 1;
    IF cat_id IS NOT NULL THEN
        INSERT INTO glass_cut_rules (id, service_category_id, rule_type, parameter_name, value, unit, description, active, application_order)
        VALUES
            (gen_random_uuid(), cat_id, 'DISCOUNT', 'LATERAL_DISCOUNT', 5.0, 'MM', 'Desconto por folha (dividir vão por nº folhas - 5mm)', TRUE, 1),
            (gen_random_uuid(), cat_id, 'DISCOUNT', 'SUPERIOR_DISCOUNT', 80.0, 'MM', 'Trilho sup. 40mm + inf. 40mm', TRUE, 2),
            (gen_random_uuid(), cat_id, 'GAP', 'OVERLAP', 25.0, 'MM', 'Sobrepasse entre folhas 25mm', TRUE, 3)
        ON CONFLICT ON CONSTRAINT uk_glass_cut_rules_category_parameter DO NOTHING;
    END IF;
END$$;

-- JANELA DE CORRER
DO $$
DECLARE cat_id UUID;
BEGIN
    SELECT id INTO cat_id FROM service_categories WHERE name = 'Janela de Correr' LIMIT 1;
    IF cat_id IS NOT NULL THEN
        INSERT INTO glass_cut_rules (id, service_category_id, rule_type, parameter_name, value, unit, description, active, application_order)
        VALUES
            (gen_random_uuid(), cat_id, 'DISCOUNT', 'LATERAL_DISCOUNT', 5.0, 'MM', 'Desconto lateral por folha (dividir vão por nº folhas - 5mm)', TRUE, 1),
            (gen_random_uuid(), cat_id, 'DISCOUNT', 'SUPERIOR_DISCOUNT', 15.0, 'MM', 'Desconto vertical: trilho sup. + inf.', TRUE, 2),
            (gen_random_uuid(), cat_id, 'GAP', 'OVERLAP', 10.0, 'MM', 'Sobrepasse entre folhas 10mm', TRUE, 3)
        ON CONFLICT ON CONSTRAINT uk_glass_cut_rules_category_parameter DO NOTHING;
    END IF;
END$$;

-- JANELA BASCULANTE
DO $$
DECLARE cat_id UUID;
BEGIN
    SELECT id INTO cat_id FROM service_categories WHERE name = 'Janela Basculante' LIMIT 1;
    IF cat_id IS NOT NULL THEN
        INSERT INTO glass_cut_rules (id, service_category_id, rule_type, parameter_name, value, unit, description, active, application_order)
        VALUES
            (gen_random_uuid(), cat_id, 'DISCOUNT', 'LATERAL_DISCOUNT', 10.0, 'MM', 'Desconto lateral: 5mm por lado', TRUE, 1),
            (gen_random_uuid(), cat_id, 'DISCOUNT', 'SUPERIOR_DISCOUNT', 10.0, 'MM', 'Desconto vertical: 5mm sup. + 5mm inf.', TRUE, 2)
        ON CONFLICT ON CONSTRAINT uk_glass_cut_rules_category_parameter DO NOTHING;
    END IF;
END$$;

-- JANELA FIXA
DO $$
DECLARE cat_id UUID;
BEGIN
    SELECT id INTO cat_id FROM service_categories WHERE name = 'Janela Fixa' LIMIT 1;
    IF cat_id IS NOT NULL THEN
        INSERT INTO glass_cut_rules (id, service_category_id, rule_type, parameter_name, value, unit, description, active, application_order)
        VALUES
            (gen_random_uuid(), cat_id, 'DISCOUNT', 'LATERAL_DISCOUNT', 20.0, 'MM', 'Desconto lateral: 10mm por lado (encaixe perfil U)', TRUE, 1),
            (gen_random_uuid(), cat_id, 'DISCOUNT', 'SUPERIOR_DISCOUNT', 20.0, 'MM', 'Desconto vertical: 10mm por lado', TRUE, 2)
        ON CONFLICT ON CONSTRAINT uk_glass_cut_rules_category_parameter DO NOTHING;
    END IF;
END$$;

-- PORTA PIVOTANTE
DO $$
DECLARE cat_id UUID;
BEGIN
    SELECT id INTO cat_id FROM service_categories WHERE name = 'Porta Pivotante' LIMIT 1;
    IF cat_id IS NOT NULL THEN
        INSERT INTO glass_cut_rules (id, service_category_id, rule_type, parameter_name, value, unit, description, active, application_order)
        VALUES
            (gen_random_uuid(), cat_id, 'DISCOUNT', 'LATERAL_DISCOUNT', 10.0, 'MM', 'Desconto lateral: 5mm por lado', TRUE, 1),
            (gen_random_uuid(), cat_id, 'DISCOUNT', 'SUPERIOR_DISCOUNT', 20.0, 'MM', 'Folga piso 10mm + dobradiça topo 10mm', TRUE, 2)
        ON CONFLICT ON CONSTRAINT uk_glass_cut_rules_category_parameter DO NOTHING;
    END IF;
END$$;

-- PORTA DE CORRER
DO $$
DECLARE cat_id UUID;
BEGIN
    SELECT id INTO cat_id FROM service_categories WHERE name = 'Porta de Correr' LIMIT 1;
    IF cat_id IS NOT NULL THEN
        INSERT INTO glass_cut_rules (id, service_category_id, rule_type, parameter_name, value, unit, description, active, application_order)
        VALUES
            (gen_random_uuid(), cat_id, 'DISCOUNT', 'LATERAL_DISCOUNT', 10.0, 'MM', 'Desconto lateral por folha', TRUE, 1),
            (gen_random_uuid(), cat_id, 'DISCOUNT', 'SUPERIOR_DISCOUNT', 85.0, 'MM', 'Trilho sup. 40mm + caixilho inf. 45mm', TRUE, 2),
            (gen_random_uuid(), cat_id, 'GAP', 'OVERLAP', 25.0, 'MM', 'Sobrepasse entre folhas 25mm', TRUE, 3)
        ON CONFLICT ON CONSTRAINT uk_glass_cut_rules_category_parameter DO NOTHING;
    END IF;
END$$;

-- VERSATIK CLASSIC
DO $$
DECLARE cat_id UUID;
BEGIN
    SELECT id INTO cat_id FROM service_categories WHERE name = 'Versatik Classic' LIMIT 1;
    IF cat_id IS NOT NULL THEN
        INSERT INTO glass_cut_rules (id, service_category_id, rule_type, parameter_name, value, unit, description, active, application_order)
        VALUES
            (gen_random_uuid(), cat_id, 'DISCOUNT', 'LATERAL_DISCOUNT', 5.0, 'MM', 'Desconto lateral por folha (5mm cada lado). Dividir vão por nº folhas.', TRUE, 1),
            (gen_random_uuid(), cat_id, 'DISCOUNT', 'SUPERIOR_DISCOUNT', 85.0, 'MM', 'Trilho sup. 40mm + perfil inf. 45mm (total 85mm)', TRUE, 2),
            (gen_random_uuid(), cat_id, 'GAP', 'OVERLAP', 25.0, 'MM', 'Sobrepasse entre folhas 25mm', TRUE, 3),
            (gen_random_uuid(), cat_id, 'GAP', 'ROLLER_HOLE', 12.0, 'MM', 'Furo roldana superior ø12mm - 2 furos por folha (60mm das laterais, 35mm do topo). Usar gabarito Tec-Vidro.', TRUE, 4)
        ON CONFLICT ON CONSTRAINT uk_glass_cut_rules_category_parameter DO NOTHING;
    END IF;
END$$;

-- GUARDA-CORPO COM CLAMP
DO $$
DECLARE cat_id UUID;
BEGIN
    SELECT id INTO cat_id FROM service_categories WHERE name = 'Guarda-corpo com Clamp' LIMIT 1;
    IF cat_id IS NOT NULL THEN
        INSERT INTO glass_cut_rules (id, service_category_id, rule_type, parameter_name, value, unit, description, active, application_order)
        VALUES
            (gen_random_uuid(), cat_id, 'GAP', 'CLAMP_GAP', 0.0, 'MM', 'Medida exata entre clamps - sem desconto (clamp abraça a borda)', TRUE, 1),
            (gen_random_uuid(), cat_id, 'GAP', 'SUPPORT_GAP', 5.0, 'MM', 'Folga de suporte (ambos os lados)', TRUE, 2)
        ON CONFLICT ON CONSTRAINT uk_glass_cut_rules_category_parameter DO NOTHING;
    END IF;
END$$;

-- GUARDA-CORPO PERFIL U
DO $$
DECLARE cat_id UUID;
BEGIN
    SELECT id INTO cat_id FROM service_categories WHERE name = 'Guarda-corpo Perfil U' LIMIT 1;
    IF cat_id IS NOT NULL THEN
        INSERT INTO glass_cut_rules (id, service_category_id, rule_type, parameter_name, value, unit, description, active, application_order)
        VALUES
            (gen_random_uuid(), cat_id, 'DISCOUNT', 'LATERAL_DISCOUNT', 10.0, 'MM', 'Desconto lateral: 5mm por lado (encaixe perfil U)', TRUE, 1),
            (gen_random_uuid(), cat_id, 'GAP', 'SUPERIOR_DISCOUNT', 120.0, 'MM', 'Acréscimo inferior: 120mm de encaixe no perfil U do piso', TRUE, 2)
        ON CONFLICT ON CONSTRAINT uk_glass_cut_rules_category_parameter DO NOTHING;
    END IF;
END$$;

-- ESPELHO
DO $$
DECLARE cat_id UUID;
BEGIN
    SELECT id INTO cat_id FROM service_categories WHERE name = 'Espelho' LIMIT 1;
    IF cat_id IS NOT NULL THEN
        INSERT INTO glass_cut_rules (id, service_category_id, rule_type, parameter_name, value, unit, description, active, application_order)
        VALUES
            (gen_random_uuid(), cat_id, 'GAP', 'POLISH_GAP', 3.0, 'MM', 'Folga de lapidação (ambas as dimensões)', TRUE, 1),
            (gen_random_uuid(), cat_id, 'GAP', 'BEVEL_GAP', 2.0, 'MM', 'Folga de bisotê (ambas as dimensões)', TRUE, 2),
            (gen_random_uuid(), cat_id, 'GAP', 'LED_SPACE', 5.0, 'MM', 'Espaço para fita de LED (largura)', TRUE, 3)
        ON CONFLICT ON CONSTRAINT uk_glass_cut_rules_category_parameter DO NOTHING;
    END IF;
END$$;

-- COBERTURA LAMINADA
DO $$
DECLARE cat_id UUID;
BEGIN
    SELECT id INTO cat_id FROM service_categories WHERE name = 'Cobertura Laminada' LIMIT 1;
    IF cat_id IS NOT NULL THEN
        INSERT INTO glass_cut_rules (id, service_category_id, rule_type, parameter_name, value, unit, description, active, application_order)
        VALUES
            (gen_random_uuid(), cat_id, 'DISCOUNT', 'LATERAL_DISCOUNT', 15.0, 'MM', 'Desconto lateral: –15mm para encaixe em calha/perfil', TRUE, 1),
            (gen_random_uuid(), cat_id, 'DISCOUNT', 'SUPERIOR_DISCOUNT', 20.0, 'MM', 'Desconto: encaixe cumeeira + beiral (–20mm)', TRUE, 2),
            (gen_random_uuid(), cat_id, 'GAP', 'EXPANSION_GAP', 5.0, 'MM', 'Folga de dilatação térmica (8mm por metro linear)', TRUE, 3),
            (gen_random_uuid(), cat_id, 'GAP', 'OVERLAP', 50.0, 'MM', 'Sobreposição entre chapas: 50mm (altura)', TRUE, 4)
        ON CONFLICT ON CONSTRAINT uk_glass_cut_rules_category_parameter DO NOTHING;
    END IF;
END$$;

