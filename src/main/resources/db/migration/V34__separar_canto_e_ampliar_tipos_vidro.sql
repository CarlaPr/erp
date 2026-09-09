ALTER TABLE plano_corte_itens ADD COLUMN tipo_canto VARCHAR(20) NOT NULL DEFAULT 'NORMAL';
ALTER TABLE plano_corte_itens ADD CONSTRAINT plano_corte_itens_tipo_canto_check
    CHECK (tipo_canto IN ('NORMAL', 'CANTO_MOEDA', 'CANTO_GARRAFA'));

-- Os acabamentos antigos não registravam um tratamento de borda separado.
UPDATE plano_corte_itens SET tipo_canto = tipo_borda, tipo_borda = 'LISO'
WHERE tipo_borda IN ('CANTO_MOEDA', 'CANTO_GARRAFA');

ALTER TABLE vidros DROP CONSTRAINT IF EXISTS vidros_tipo_check;
ALTER TABLE vidros ADD CONSTRAINT vidros_tipo_check
    CHECK (tipo IN ('COMUM', 'TEMPERADO', 'LAMINADO', 'ARAMADO', 'INSULADO', 'ESPELHO', 'ACRILICO'));
