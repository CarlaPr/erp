ALTER TABLE technical_visit_openings
    ADD COLUMN service_category VARCHAR(40);

ALTER TABLE plano_corte_itens
    ADD COLUMN categoria VARCHAR(40);

UPDATE plano_corte_itens item
SET categoria = plano.categoria
FROM planos_corte plano
WHERE plano.id = item.plano_corte_id;

ALTER TABLE plano_corte_itens
    ALTER COLUMN categoria SET NOT NULL;

ALTER TABLE plano_corte_itens
    ADD CONSTRAINT chk_plano_corte_item_categoria
    CHECK (categoria IN (
        'ABRIGO_PIA', 'PORTA_CORRER', 'JANELA_PADRAO', 'PORTA_ABRIR',
        'VIDRO_FIXO_PERFIL_U', 'JANELA_BASCULANTE', 'SACADA', 'ESPELHO',
        'BOX_BANHEIRO_PADRAO'
    ));
