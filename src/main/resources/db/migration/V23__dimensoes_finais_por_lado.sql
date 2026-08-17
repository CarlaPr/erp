ALTER TABLE plano_corte_itens
    ADD COLUMN altura_final_esquerda_mm NUMERIC(8,2),
    ADD COLUMN altura_final_direita_mm NUMERIC(8,2),
    ADD COLUMN largura_final_superior_mm NUMERIC(8,2),
    ADD COLUMN largura_final_inferior_mm NUMERIC(8,2);
