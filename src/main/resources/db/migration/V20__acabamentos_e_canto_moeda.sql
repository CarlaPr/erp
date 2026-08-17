
ALTER TABLE plano_corte_itens
    DROP CONSTRAINT IF EXISTS plano_corte_itens_tipo_borda_check;

ALTER TABLE plano_corte_itens
    ADD CONSTRAINT plano_corte_itens_tipo_borda_check
        CHECK (tipo_borda IN (
            'LISO', 'LAPIDADO', 'BISOTADO',
            'JATEADO', 'ACIDATO', 'SERIGRAFADO', 'PINTADO', 'ADESIVADO', 'CANTO_MOEDA'
        ));

ALTER TABLE plano_corte_itens
    ADD COLUMN canto_moeda_sup_esquerdo BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN canto_moeda_sup_direito BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN canto_moeda_inf_esquerdo BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN canto_moeda_inf_direito BOOLEAN NOT NULL DEFAULT FALSE;
