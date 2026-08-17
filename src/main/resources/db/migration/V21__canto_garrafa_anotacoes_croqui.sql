
ALTER TABLE plano_corte_itens
    DROP CONSTRAINT IF EXISTS plano_corte_itens_tipo_borda_check;

ALTER TABLE plano_corte_itens
    ADD CONSTRAINT plano_corte_itens_tipo_borda_check
        CHECK (tipo_borda IN (
            'LISO', 'LAPIDADO', 'BISOTADO',
            'JATEADO', 'ACIDATO', 'SERIGRAFADO', 'PINTADO', 'ADESIVADO',
            'CANTO_MOEDA', 'CANTO_GARRAFA'
        ));

ALTER TABLE plano_corte_item_elementos
    ADD COLUMN rotulo_croqui VARCHAR(60);


CREATE TABLE plano_corte_item_anotacoes (
    item_id BIGINT NOT NULL REFERENCES plano_corte_itens(id) ON DELETE CASCADE,
    ordem INTEGER NOT NULL,
    tipo VARCHAR(10) CHECK (tipo IN ('TEXTO', 'SETA', 'LINHA')),
    x1_mm NUMERIC(9,2),
    y1_mm NUMERIC(9,2),
    x2_mm NUMERIC(9,2),
    y2_mm NUMERIC(9,2),
    texto VARCHAR(120),
    PRIMARY KEY (item_id, ordem)
);
