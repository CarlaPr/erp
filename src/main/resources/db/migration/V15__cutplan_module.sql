-- ============================================================================
-- V15 - Módulo CUTPLAN (Plano de Corte de Vidro)
-- ============================================================================
-- Cria por completo as tabelas e colunas do módulo cutplan, mapeadas a partir
-- das entidades JPA em com.alfatahi.erp.cutplan.entity:
--   1. CostTable            -> cost_tables
--   2. CostTableHistory     -> cost_table_history
--   3. CutPlan              -> cut_plans
--   4. CutPlanHistory       -> cut_plan_history
--   5. CutPlanItem          -> cut_plan_items
--   6. GlassCutRule         -> glass_cut_rules
--   7. GlassDrilling        -> glass_drillings
--   8. GlassFinishing       -> glass_finishings
--   9. GlassNotch           -> glass_notches
--  10. GlassTemplate        -> glass_templates
--
-- Dependências externas (já existentes desde V1__init.sql):
--   suppliers(id), app_users(id), work_orders(id), service_categories(id)
--
-- Observação: hibernate.ddl-auto=validate está ativo (application.yml), então
-- nomes de tabela/coluna, tipos e precisões abaixo espelham fielmente as
-- anotações @Column/@JoinColumn das entidades para não quebrar a validação
-- de schema na subida da aplicação.
-- ============================================================================

-- ----------------------------------------------------------------------------
-- 1. COST_TABLES - Tabela de preços parametrizada para cálculo de custos
-- ----------------------------------------------------------------------------
CREATE TABLE cost_tables (
                             id               UUID PRIMARY KEY,
                             category         VARCHAR(50)   NOT NULL,
                             item_type        VARCHAR(255)  NOT NULL,
                             description      TEXT,
                             unit_price       NUMERIC(12,2) NOT NULL,
                             unit             VARCHAR(20),
                             supplier_id      UUID,
                             effective_from   DATE          NOT NULL,
                             effective_to     DATE,
                             active           BOOLEAN       NOT NULL DEFAULT TRUE,
                             created_by       UUID,
                             created_at       TIMESTAMP     NOT NULL DEFAULT now(),
                             updated_at       TIMESTAMP     NOT NULL DEFAULT now(),
                             remarks          TEXT
);

-- ----------------------------------------------------------------------------
-- 2. COST_TABLE_HISTORY - Auditoria de mudanças de preço
-- ----------------------------------------------------------------------------
CREATE TABLE cost_table_history (
                                    id             UUID PRIMARY KEY,
                                    cost_table_id  UUID          NOT NULL,
                                    old_price      NUMERIC(12,2) NOT NULL,
                                    new_price      NUMERIC(12,2) NOT NULL,
                                    changed_by     UUID          NOT NULL,
                                    changed_at     TIMESTAMP     NOT NULL DEFAULT now(),
                                    reason         TEXT,
                                    reference      VARCHAR(500)
);

-- ----------------------------------------------------------------------------
-- 3. CUT_PLANS - Entidade principal do Plano de Corte (1:1 com work_orders)
-- ----------------------------------------------------------------------------
CREATE TABLE cut_plans (
                           id             UUID PRIMARY KEY,
                           work_order_id  UUID          NOT NULL,
                           version        INTEGER       NOT NULL DEFAULT 1,
                           status         VARCHAR(50)   NOT NULL DEFAULT 'DRAFT',
                           description    TEXT,
                           created_by     UUID,
                           created_at     TIMESTAMP     NOT NULL DEFAULT now(),
                           updated_by     UUID,
                           updated_at     TIMESTAMP     NOT NULL DEFAULT now()
);

-- ----------------------------------------------------------------------------
-- 4. CUT_PLAN_HISTORY - Auditoria e rastreabilidade do Plano de Corte
-- ----------------------------------------------------------------------------
CREATE TABLE cut_plan_history (
                                  id                          UUID PRIMARY KEY,
                                  cut_plan_id                 UUID         NOT NULL,
                                  changed_by                  UUID         NOT NULL,
                                  change_type                 VARCHAR(50)  NOT NULL,
                                  description                 TEXT         NOT NULL,
                                  version                     INTEGER      NOT NULL,
                                  changed_at                  TIMESTAMP    NOT NULL DEFAULT now(),
                                  old_values                  TEXT,
                                  new_values                  TEXT,
                                  affected_item_id            UUID,
                                  affected_item_description   VARCHAR(500)
);

-- ----------------------------------------------------------------------------
-- 5. CUT_PLAN_ITEMS - Itens (peças) do Plano de Corte
-- ----------------------------------------------------------------------------
CREATE TABLE cut_plan_items (
                                id                       UUID PRIMARY KEY,
                                cut_plan_id              UUID          NOT NULL,
                                description              TEXT          NOT NULL,
                                environment              VARCHAR(100),
                                glass_type               VARCHAR(50)   NOT NULL,
                                thickness                NUMERIC(5,2)  NOT NULL,
                                color                    VARCHAR(50),
                                finishing                VARCHAR(50),
                                gross_width              NUMERIC(8,2)  NOT NULL,
                                gross_height             NUMERIC(8,2)  NOT NULL,
                                final_width              NUMERIC(8,2)  NOT NULL,
                                final_height             NUMERIC(8,2)  NOT NULL,
                                quantity                 INTEGER       NOT NULL DEFAULT 1,
                                calculated_area          NUMERIC(15,2),
                                estimated_weight         NUMERIC(10,2),
                                glass_cost               NUMERIC(12,2) DEFAULT 0,
                                hardwares_total_cost     NUMERIC(12,2) DEFAULT 0,
                                aluminum_total_cost      NUMERIC(12,2) DEFAULT 0,
                                silicone_total_cost      NUMERIC(12,2) DEFAULT 0,
                                estimated_cost           NUMERIC(12,2) DEFAULT 0,
                                supplier_id              UUID,
                                notes                    TEXT,
                                angle                    NUMERIC(5,2),
                                drilling_diameter        NUMERIC(5,2),
                                drilling_quantity        INTEGER,
                                drilling_cost_per_unit   NUMERIC(8,2),
                                notch_description        VARCHAR(200),
                                notch_cost               NUMERIC(8,2),
                                sent_to_supplier         BOOLEAN       NOT NULL DEFAULT FALSE,
                                supplier_feedback        TEXT
);

-- ----------------------------------------------------------------------------
-- 6. GLASS_CUT_RULES - Regras técnicas parametrizadas de corte por categoria
-- ----------------------------------------------------------------------------
CREATE TABLE glass_cut_rules (
                                 id                     UUID PRIMARY KEY,
                                 service_category_id    UUID          NOT NULL,
                                 rule_type              VARCHAR(50)   NOT NULL,
                                 parameter_name         VARCHAR(100)  NOT NULL,
                                 value                  NUMERIC(8,2)  NOT NULL,
                                 unit                   VARCHAR(20)   NOT NULL,
                                 description            TEXT,
                                 active                 BOOLEAN       NOT NULL DEFAULT TRUE,
                                 application_order      INTEGER       NOT NULL DEFAULT 0
);

-- ----------------------------------------------------------------------------
-- 7. GLASS_DRILLINGS - Catálogo de tipos de furos
-- ----------------------------------------------------------------------------
CREATE TABLE glass_drillings (
                                 id             UUID PRIMARY KEY,
                                 name           VARCHAR(100)  NOT NULL,
                                 description    TEXT,
                                 diameter       NUMERIC(5,2)  NOT NULL,
                                 rebaix_depth   NUMERIC(5,2),
                                 cost_per_unit  NUMERIC(8,2)  NOT NULL,
                                 time_per_hole  INTEGER,
                                 active         BOOLEAN       NOT NULL DEFAULT TRUE
);

-- ----------------------------------------------------------------------------
-- 8. GLASS_FINISHINGS - Catálogo de acabamentos possíveis para vidro
-- ----------------------------------------------------------------------------
CREATE TABLE glass_finishings (
                                  id                        UUID PRIMARY KEY,
                                  name                      VARCHAR(100)  NOT NULL,
                                  description               TEXT,
                                  cost_adjustment           NUMERIC(10,2) DEFAULT 0,
                                  adjustment_type           VARCHAR(20)   DEFAULT 'FIXED',
                                  processing_time_minutes   INTEGER,
                                  active                    BOOLEAN       NOT NULL DEFAULT TRUE
);

-- ----------------------------------------------------------------------------
-- 9. GLASS_NOTCHES - Catálogo de tipos de entalhes (rebaixos)
-- ----------------------------------------------------------------------------
CREATE TABLE glass_notches (
                               id                UUID PRIMARY KEY,
                               name              VARCHAR(100)  NOT NULL,
                               description       TEXT,
                               width             NUMERIC(5,2),
                               depth             NUMERIC(5,2),
                               shape             VARCHAR(50),
                               cost              NUMERIC(8,2)  NOT NULL,
                               processing_time   INTEGER,
                               min_length        NUMERIC(8,2),
                               max_length        NUMERIC(8,2),
                               active            BOOLEAN       NOT NULL DEFAULT TRUE
);

-- ----------------------------------------------------------------------------
-- 10. GLASS_TEMPLATES - Templates configuráveis de especificação de vidro
-- ----------------------------------------------------------------------------
CREATE TABLE glass_templates (
                                 id                      UUID PRIMARY KEY,
                                 service_category_id     UUID          NOT NULL,
                                 name                    VARCHAR(255)  NOT NULL,
                                 description             TEXT,
                                 default_glass_type      VARCHAR(50),
                                 default_thickness       NUMERIC(5,2),
                                 default_color           VARCHAR(50),
                                 applicable_finishings   TEXT,
                                 applicable_drillings    TEXT,
                                 applicable_notches      TEXT,
                                 active                  BOOLEAN       NOT NULL DEFAULT TRUE
);

-- ============================================================================
-- UNIQUE CONSTRAINTS
-- ============================================================================

ALTER TABLE cost_tables
    ADD CONSTRAINT uk_cost_tables_category_item_type_effective
        UNIQUE (category, item_type, effective_from);

ALTER TABLE cut_plans
    ADD CONSTRAINT uk_cut_plans_work_order_id
        UNIQUE (work_order_id);

ALTER TABLE glass_cut_rules
    ADD CONSTRAINT uk_glass_cut_rules_category_parameter
        UNIQUE (service_category_id, parameter_name);

-- ============================================================================
-- FOREIGN KEYS
-- ============================================================================

ALTER TABLE cost_tables
    ADD CONSTRAINT fk_cost_tables_supplier_id
        FOREIGN KEY (supplier_id) REFERENCES suppliers(id);

ALTER TABLE cost_tables
    ADD CONSTRAINT fk_cost_tables_created_by
        FOREIGN KEY (created_by) REFERENCES app_users(id);

ALTER TABLE cost_table_history
    ADD CONSTRAINT fk_cost_table_history_cost_table_id
        FOREIGN KEY (cost_table_id) REFERENCES cost_tables(id);

ALTER TABLE cost_table_history
    ADD CONSTRAINT fk_cost_table_history_changed_by
        FOREIGN KEY (changed_by) REFERENCES app_users(id);

ALTER TABLE cut_plans
    ADD CONSTRAINT fk_cut_plans_work_order_id
        FOREIGN KEY (work_order_id) REFERENCES work_orders(id);

ALTER TABLE cut_plans
    ADD CONSTRAINT fk_cut_plans_created_by
        FOREIGN KEY (created_by) REFERENCES app_users(id);

ALTER TABLE cut_plans
    ADD CONSTRAINT fk_cut_plans_updated_by
        FOREIGN KEY (updated_by) REFERENCES app_users(id);

ALTER TABLE cut_plan_history
    ADD CONSTRAINT fk_cut_plan_history_cut_plan_id
        FOREIGN KEY (cut_plan_id) REFERENCES cut_plans(id);

ALTER TABLE cut_plan_history
    ADD CONSTRAINT fk_cut_plan_history_changed_by
        FOREIGN KEY (changed_by) REFERENCES app_users(id);

ALTER TABLE cut_plan_items
    ADD CONSTRAINT fk_cut_plan_items_cut_plan_id
        FOREIGN KEY (cut_plan_id) REFERENCES cut_plans(id);

ALTER TABLE cut_plan_items
    ADD CONSTRAINT fk_cut_plan_items_supplier_id
        FOREIGN KEY (supplier_id) REFERENCES suppliers(id);

ALTER TABLE glass_cut_rules
    ADD CONSTRAINT fk_glass_cut_rules_service_category_id
        FOREIGN KEY (service_category_id) REFERENCES service_categories(id);

ALTER TABLE glass_templates
    ADD CONSTRAINT fk_glass_templates_service_category_id
        FOREIGN KEY (service_category_id) REFERENCES service_categories(id);

-- ============================================================================
-- ÍNDICES (espelhando @Index de cada entidade)
-- ============================================================================

CREATE INDEX idx_cost_tables_category         ON cost_tables(category);
CREATE INDEX idx_cost_tables_supplier_id       ON cost_tables(supplier_id);
CREATE INDEX idx_cost_tables_effective_from    ON cost_tables(effective_from);
CREATE INDEX idx_cost_tables_effective_to      ON cost_tables(effective_to);
CREATE INDEX idx_cost_tables_active            ON cost_tables(active);

CREATE INDEX idx_cost_table_history_cost_table_id  ON cost_table_history(cost_table_id);
CREATE INDEX idx_cost_table_history_changed_by     ON cost_table_history(changed_by);
CREATE INDEX idx_cost_table_history_changed_at     ON cost_table_history(changed_at);

CREATE INDEX idx_cut_plans_work_order_id  ON cut_plans(work_order_id);
CREATE INDEX idx_cut_plans_status         ON cut_plans(status);
CREATE INDEX idx_cut_plans_created_at     ON cut_plans(created_at);

CREATE INDEX idx_cut_plan_history_cut_plan_id   ON cut_plan_history(cut_plan_id);
CREATE INDEX idx_cut_plan_history_changed_by    ON cut_plan_history(changed_by);
CREATE INDEX idx_cut_plan_history_changed_at    ON cut_plan_history(changed_at);
CREATE INDEX idx_cut_plan_history_change_type   ON cut_plan_history(change_type);

CREATE INDEX idx_cut_plan_items_cut_plan_id  ON cut_plan_items(cut_plan_id);
CREATE INDEX idx_cut_plan_items_supplier_id  ON cut_plan_items(supplier_id);
CREATE INDEX idx_cut_plan_items_glass_type   ON cut_plan_items(glass_type);

CREATE INDEX idx_glass_cut_rules_service_category_id  ON glass_cut_rules(service_category_id);
CREATE INDEX idx_glass_cut_rules_rule_type             ON glass_cut_rules(rule_type);
CREATE INDEX idx_glass_cut_rules_parameter_name        ON glass_cut_rules(parameter_name);

CREATE INDEX idx_glass_drillings_name ON glass_drillings(name);

CREATE INDEX idx_glass_finishings_name ON glass_finishings(name);

CREATE INDEX idx_glass_notches_name ON glass_notches(name);

CREATE INDEX idx_glass_templates_service_category_id  ON glass_templates(service_category_id);
CREATE INDEX idx_glass_templates_name                  ON glass_templates(name);