ALTER TABLE work_orders
    ADD COLUMN IF NOT EXISTS conditions_observations TEXT,
    ADD COLUMN IF NOT EXISTS warranty_terms TEXT;

UPDATE work_orders
SET conditions_observations = '• Materiais produzidos ou adquiridos sob medida para o cliente poderão ter seus respectivos custos cobrados em caso de cancelamento, independentemente da forma de pagamento escolhida.
• O cliente declara estar de acordo com as medidas, especificações, materiais, valores e demais condições.
• A execução está limitada aos serviços expressamente descritos nesta Ordem de Serviço. Serviços adicionais ou não previstos deverão ser previamente acordados entre as partes e poderão gerar custos extras.
• Produtos sob medida não possuem troca ou devolução, conforme o Código de Defesa do Consumidor.
• Alterações solicitadas pelo cliente após a confirmação do pedido poderão gerar custos adicionais e alteração do prazo de entrega.'
WHERE conditions_observations IS NULL;

UPDATE work_orders
SET warranty_terms = 'Garantia de 90 dias, a partir da conclusão da instalação, cobrindo apenas regulagens, vedação e defeitos decorrentes da instalação realizada pela
empresa. Não cobre:
• Mau uso, acidentes ou impactos;
• Quebra de vidros ou uso inadequado;
• Desgaste natural;
• Falta de manutenção ou limpeza inadequada;
• Reparos realizados por terceiros;
• Danos causados por fatores externos ou fenômenos naturais.
A garantia será cancelada caso o produto seja alterado ou reparado por pessoas não autorizadas.'
WHERE warranty_terms IS NULL;


UPDATE work_orders AS work_order
SET installation_notes = NULL
FROM quotes AS quote
WHERE work_order.quote_id = quote.id
  AND NULLIF(BTRIM(work_order.installation_notes), '') IS NOT NULL
  AND BTRIM(work_order.installation_notes) = BTRIM(quote.observations);
