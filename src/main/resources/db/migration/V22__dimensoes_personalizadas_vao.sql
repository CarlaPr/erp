ALTER TABLE plano_corte_itens
    ADD COLUMN altura_bruta_esquerda_mm NUMERIC(8,2),
    ADD COLUMN altura_bruta_direita_mm NUMERIC(8,2),
    ADD COLUMN largura_bruta_superior_mm NUMERIC(8,2),
    ADD COLUMN largura_bruta_inferior_mm NUMERIC(8,2);
