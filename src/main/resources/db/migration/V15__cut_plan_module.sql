-- =====================================================================
-- Módulo PLANO DE CORTE + CATÁLOGO DE INSUMOS
-- Integrado à Ordem de Serviço (work_orders) já existente.
-- Nenhuma tabela/coluna existente é alterada de forma destrutiva.
-- =====================================================================

-- ---------------------------------------------------------------------
-- 1) Conjuntos de regras de desconto/folga técnica (PARAMETRIZÁVEL)
--    Um "rule set" agrupa parâmetros que o administrador cadastra e
--    edita livremente (nenhum valor fica fixo no código Java).
-- ---------------------------------------------------------------------
CREATE TABLE public.cut_rule_sets (
    id uuid NOT NULL,
    service_category_id uuid,
    name character varying(150) NOT NULL,
    glass_type character varying(100),
    description text,
    active boolean NOT NULL DEFAULT true,
    created_at timestamp(6) without time zone NOT NULL DEFAULT now()
);
ALTER TABLE ONLY public.cut_rule_sets ADD CONSTRAINT cut_rule_sets_pkey PRIMARY KEY (id);
ALTER TABLE ONLY public.cut_rule_sets
    ADD CONSTRAINT fk_cut_rule_sets_category FOREIGN KEY (service_category_id) REFERENCES public.service_categories(id);

-- Cada linha é 1 parâmetro (ex: "Desconto lateral", "Folga ferragem"...).
-- dimension indica se o valor é descontado da LARGURA, ALTURA ou AMBAS.
CREATE TABLE public.cut_rule_parameters (
    id uuid NOT NULL,
    rule_set_id uuid NOT NULL,
    param_key character varying(100) NOT NULL,
    label character varying(150) NOT NULL,
    dimension character varying(10) NOT NULL DEFAULT 'BOTH', -- WIDTH | HEIGHT | BOTH
    value_mm numeric(10,2) NOT NULL DEFAULT 0,
    sort_order integer NOT NULL DEFAULT 0,
    notes text
);
ALTER TABLE ONLY public.cut_rule_parameters ADD CONSTRAINT cut_rule_parameters_pkey PRIMARY KEY (id);
ALTER TABLE ONLY public.cut_rule_parameters
    ADD CONSTRAINT fk_cut_rule_parameters_ruleset FOREIGN KEY (rule_set_id) REFERENCES public.cut_rule_sets(id) ON DELETE CASCADE;

-- ---------------------------------------------------------------------
-- 2) Catálogo de Insumos (tabela de preços de compra, versionada)
-- ---------------------------------------------------------------------
CREATE TABLE public.material_price_items (
    id uuid NOT NULL,
    category character varying(30) NOT NULL, -- GLASS | ALUMINUM | HARDWARE | SILICONE | ACCESSORY | OTHER
    name character varying(200) NOT NULL,
    manufacturer character varying(150),
    supplier_id uuid,
    glass_type character varying(100),
    thickness numeric(6,2),
    color character varying(100),
    finish character varying(100),
    aluminum_line character varying(150),
    aluminum_profile character varying(150),
    hardware_category character varying(150),
    code character varying(100),
    unit character varying(10) NOT NULL, -- M2 | M | UNIT | TUBE
    price numeric(12,4) NOT NULL,
    min_price numeric(12,4),
    effective_date date,
    expiry_date date,
    active boolean NOT NULL DEFAULT true,
    notes text,
    created_at timestamp(6) without time zone NOT NULL DEFAULT now(),
    updated_at timestamp(6) without time zone NOT NULL DEFAULT now()
);
ALTER TABLE ONLY public.material_price_items ADD CONSTRAINT material_price_items_pkey PRIMARY KEY (id);
ALTER TABLE ONLY public.material_price_items
    ADD CONSTRAINT fk_material_price_items_supplier FOREIGN KEY (supplier_id) REFERENCES public.suppliers(id);
CREATE INDEX idx_material_price_items_category ON public.material_price_items (category, active);

CREATE TABLE public.material_price_history (
    id uuid NOT NULL,
    price_item_id uuid NOT NULL,
    old_price numeric(12,4),
    new_price numeric(12,4) NOT NULL,
    changed_by character varying(150),
    changed_at timestamp(6) without time zone NOT NULL DEFAULT now(),
    reason text
);
ALTER TABLE ONLY public.material_price_history ADD CONSTRAINT material_price_history_pkey PRIMARY KEY (id);
ALTER TABLE ONLY public.material_price_history
    ADD CONSTRAINT fk_material_price_history_item FOREIGN KEY (price_item_id) REFERENCES public.material_price_items(id) ON DELETE CASCADE;

-- ---------------------------------------------------------------------
-- 3) Plano de Corte (1..N por Ordem de Serviço)
-- ---------------------------------------------------------------------
CREATE TABLE public.cut_plans (
    id uuid NOT NULL,
    work_order_id uuid NOT NULL,
    plan_number integer NOT NULL,
    title character varying(150),
    status character varying(30) NOT NULL DEFAULT 'draft', -- draft | in_production | sent_to_supplier | done | cancelled
    origin character varying(30), -- quote | manual
    responsible character varying(150),
    rule_set_id uuid,
    created_by character varying(150),
    created_at timestamp(6) without time zone NOT NULL DEFAULT now(),
    updated_at timestamp(6) without time zone NOT NULL DEFAULT now(),
    notes text
);
ALTER TABLE ONLY public.cut_plans ADD CONSTRAINT cut_plans_pkey PRIMARY KEY (id);
ALTER TABLE ONLY public.cut_plans
    ADD CONSTRAINT fk_cut_plans_work_order FOREIGN KEY (work_order_id) REFERENCES public.work_orders(id) ON DELETE CASCADE;
ALTER TABLE ONLY public.cut_plans
    ADD CONSTRAINT fk_cut_plans_rule_set FOREIGN KEY (rule_set_id) REFERENCES public.cut_rule_sets(id);
CREATE INDEX idx_cut_plans_work_order ON public.cut_plans (work_order_id);

CREATE TABLE public.cut_plan_items (
    id uuid NOT NULL,
    cut_plan_id uuid NOT NULL,
    description character varying(200),
    environment character varying(150),
    glass_type character varying(100),
    thickness numeric(6,2),
    color character varying(100),
    finish character varying(100),
    gross_width numeric(10,2),
    gross_height numeric(10,2),
    final_width numeric(10,2),
    final_height numeric(10,2),
    quantity numeric(10,2) NOT NULL DEFAULT 1,
    angle_type character varying(50),
    edge_work character varying(100),
    drilling_count integer NOT NULL DEFAULT 0,
    notch_count integer NOT NULL DEFAULT 0,
    supplier_id uuid,
    price_item_id uuid,
    unit_price_snapshot numeric(12,4),
    observations text,
    sort_order integer NOT NULL DEFAULT 0
);
ALTER TABLE ONLY public.cut_plan_items ADD CONSTRAINT cut_plan_items_pkey PRIMARY KEY (id);
ALTER TABLE ONLY public.cut_plan_items
    ADD CONSTRAINT fk_cut_plan_items_plan FOREIGN KEY (cut_plan_id) REFERENCES public.cut_plans(id) ON DELETE CASCADE;
ALTER TABLE ONLY public.cut_plan_items
    ADD CONSTRAINT fk_cut_plan_items_supplier FOREIGN KEY (supplier_id) REFERENCES public.suppliers(id);
ALTER TABLE ONLY public.cut_plan_items
    ADD CONSTRAINT fk_cut_plan_items_price FOREIGN KEY (price_item_id) REFERENCES public.material_price_items(id);

CREATE TABLE public.cut_plan_materials (
    id uuid NOT NULL,
    cut_plan_id uuid NOT NULL,
    category character varying(30) NOT NULL, -- HARDWARE | ALUMINUM | SILICONE | OTHER
    description character varying(200) NOT NULL,
    quantity numeric(10,3) NOT NULL DEFAULT 1,
    unit character varying(10),
    unit_price_snapshot numeric(12,4) NOT NULL DEFAULT 0,
    supplier_name character varying(150),
    price_item_id uuid,
    notes text
);
ALTER TABLE ONLY public.cut_plan_materials ADD CONSTRAINT cut_plan_materials_pkey PRIMARY KEY (id);
ALTER TABLE ONLY public.cut_plan_materials
    ADD CONSTRAINT fk_cut_plan_materials_plan FOREIGN KEY (cut_plan_id) REFERENCES public.cut_plans(id) ON DELETE CASCADE;
ALTER TABLE ONLY public.cut_plan_materials
    ADD CONSTRAINT fk_cut_plan_materials_price FOREIGN KEY (price_item_id) REFERENCES public.material_price_items(id);

CREATE TABLE public.cut_plan_history (
    id uuid NOT NULL,
    cut_plan_id uuid NOT NULL,
    action character varying(50) NOT NULL,
    details text,
    changed_by character varying(150),
    changed_at timestamp(6) without time zone NOT NULL DEFAULT now()
);
ALTER TABLE ONLY public.cut_plan_history ADD CONSTRAINT cut_plan_history_pkey PRIMARY KEY (id);
ALTER TABLE ONLY public.cut_plan_history
    ADD CONSTRAINT fk_cut_plan_history_plan FOREIGN KEY (cut_plan_id) REFERENCES public.cut_plans(id) ON DELETE CASCADE;

-- ---------------------------------------------------------------------
-- 4) Regras/parâmetros padrão (SEED) — apenas ponto de partida.
--    Todos os valores podem ser editados/renomeados/excluídos pelo
--    administrador na tela de Regras de Desconto, sem alterar código.
-- ---------------------------------------------------------------------
INSERT INTO public.cut_rule_sets (id, name, description, active) VALUES
 (gen_random_uuid(), 'Box de Vidro (padrão)', 'Descontos técnicos padrão para box - ajuste conforme fabricante do kit/ferragem', true),
 (gen_random_uuid(), 'Janela de Alumínio (padrão)', 'Descontos técnicos padrão para janelas - ajuste conforme linha de perfil', true),
 (gen_random_uuid(), 'Espelho (padrão)', 'Folgas padrão para espelhos', true),
 (gen_random_uuid(), 'Guarda-corpo (padrão)', 'Folgas padrão para guarda-corpo', true),
 (gen_random_uuid(), 'Cobertura (padrão)', 'Folgas padrão para coberturas de vidro', true);

INSERT INTO public.cut_rule_parameters (id, rule_set_id, param_key, label, dimension, value_mm, sort_order)
SELECT gen_random_uuid(), rs.id, p.param_key, p.label, p.dimension, p.value_mm, p.sort_order
FROM public.cut_rule_sets rs
JOIN (VALUES
    ('BOX', 'LATERAL_DISCOUNT', 'Desconto lateral (ambos os lados)', 'WIDTH', 6.0, 1),
    ('BOX', 'TOP_DISCOUNT', 'Desconto superior', 'HEIGHT', 4.0, 2),
    ('BOX', 'BOTTOM_DISCOUNT', 'Desconto inferior', 'HEIGHT', 4.0, 3),
    ('BOX', 'HARDWARE_CLEARANCE', 'Folga para ferragem', 'BOTH', 2.0, 4),
    ('BOX', 'SILICONE_GAP', 'Folga para silicone', 'BOTH', 3.0, 5),
    ('BOX', 'INSTALL_GAP', 'Folga de instalação', 'BOTH', 2.0, 6)
) AS p(cat, param_key, label, dimension, value_mm, sort_order) ON rs.name = 'Box de Vidro (padrão)'
UNION ALL
SELECT gen_random_uuid(), rs.id, p.param_key, p.label, p.dimension, p.value_mm, p.sort_order
FROM public.cut_rule_sets rs
JOIN (VALUES
    ('JAN', 'PROFILE_DISCOUNT', 'Desconto perfil de alumínio', 'BOTH', 10.0, 1),
    ('JAN', 'TRACK_GAP', 'Folga de trilho', 'WIDTH', 3.0, 2),
    ('JAN', 'ROLLER_GAP', 'Folga de roldana', 'HEIGHT', 3.0, 3),
    ('JAN', 'SEAL_GAP', 'Folga de vedação', 'BOTH', 2.0, 4)
) AS p(cat, param_key, label, dimension, value_mm, sort_order) ON rs.name = 'Janela de Alumínio (padrão)'
UNION ALL
SELECT gen_random_uuid(), rs.id, p.param_key, p.label, p.dimension, p.value_mm, p.sort_order
FROM public.cut_rule_sets rs
JOIN (VALUES
    ('ESP', 'LAPIDATION_GAP', 'Folga de lapidação', 'BOTH', 3.0, 1),
    ('ESP', 'BEVEL_GAP', 'Folga de bisotê', 'BOTH', 2.0, 2),
    ('ESP', 'LED_SPACE', 'Espaço para fita de LED', 'WIDTH', 5.0, 3)
) AS p(cat, param_key, label, dimension, value_mm, sort_order) ON rs.name = 'Espelho (padrão)'
UNION ALL
SELECT gen_random_uuid(), rs.id, p.param_key, p.label, p.dimension, p.value_mm, p.sort_order
FROM public.cut_rule_sets rs
JOIN (VALUES
    ('GC', 'SUPPORT_GAP', 'Folga de suporte', 'BOTH', 5.0, 1),
    ('GC', 'BUTTON_GAP', 'Folga de botão', 'BOTH', 4.0, 2),
    ('GC', 'PROFILE_GAP', 'Folga de perfil', 'BOTH', 4.0, 3)
) AS p(cat, param_key, label, dimension, value_mm, sort_order) ON rs.name = 'Guarda-corpo (padrão)'
UNION ALL
SELECT gen_random_uuid(), rs.id, p.param_key, p.label, p.dimension, p.value_mm, p.sort_order
FROM public.cut_rule_sets rs
JOIN (VALUES
    ('COB', 'DILATION_GAP', 'Folga de dilatação', 'BOTH', 5.0, 1),
    ('COB', 'OVERLAP', 'Sobreposição entre chapas', 'HEIGHT', 50.0, 2)
) AS p(cat, param_key, label, dimension, value_mm, sort_order) ON rs.name = 'Cobertura (padrão)';
