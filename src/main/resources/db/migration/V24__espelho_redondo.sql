ALTER TABLE plano_corte_itens
    ADD COLUMN redondo BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE plano_corte_item_furacoes DROP CONSTRAINT plano_corte_item_furacoes_tipo_check;

ALTER TABLE plano_corte_item_elementos ALTER COLUMN lado TYPE VARCHAR(50);
ALTER TABLE plano_corte_item_elementos ALTER COLUMN tipo TYPE VARCHAR(50);