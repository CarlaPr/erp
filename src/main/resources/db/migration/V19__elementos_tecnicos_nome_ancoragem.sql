
ALTER TABLE plano_corte_item_elementos
    ADD COLUMN nome VARCHAR(80),
    ADD COLUMN ancoragem VARCHAR(20) NOT NULL DEFAULT 'CENTRO'
        CHECK (ancoragem IN ('CENTRO', 'CANTO'));
