
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM expense_categories WHERE code = 'OPERACIONAL') THEN
        RAISE EXCEPTION 'Categoria OPERACIONAL não encontrada para transferir os insumos';
    END IF;
END $$;

CREATE TEMPORARY TABLE insumos_operacionais_migrados ON COMMIT DROP AS
SELECT source.id AS source_id,
       source.code AS source_code,
       source.group_label,
       source.segment,
       source.is_active,
       destination.id AS destination_category_id,
       destination.code AS destination_category_code,
       existing.id AS existing_subcategory_id,
       COALESCE(existing.code, source.code) AS destination_subcategory_code
FROM expense_subcategories source
JOIN expense_categories origin ON origin.id = source.category_id AND origin.code = 'VARIAVEL'
CROSS JOIN expense_categories destination
LEFT JOIN expense_subcategories existing
       ON existing.category_id = destination.id AND existing.code = source.code
WHERE destination.code = 'OPERACIONAL'
  AND source.code IN (
      'vidro_temperado', 'vidro_laminado', 'vidro_comum', 'espelho',
      'vidro_serigrafado', 'vidro_jateado',
      'roldanas', 'trilhos', 'puxadores', 'dobradicas', 'fechaduras',
      'kits_para_box', 'kits_para_sacada', 'kits_para_guarda_corpo',
      'linha_suprema', 'linha_gold', 'linha_25', 'linha_30', 'linha_42',
      'linha_integrada', 'linha_fachada',
      'tempera', 'lapidacao', 'furacao', 'recortes', 'pintura',
      'jateamento', 'transporte', 'instalacao', 'terceirizacao'
  );


UPDATE accounts_payable payable
SET category = moved.destination_category_code,
    subcategory = moved.destination_subcategory_code
FROM insumos_operacionais_migrados moved
WHERE UPPER(BTRIM(payable.category)) = 'VARIAVEL'
  AND LOWER(BTRIM(payable.subcategory)) = LOWER(moved.source_code);

UPDATE expense_recurrences recurrence
SET category = moved.destination_category_code,
    subcategory = moved.destination_subcategory_code
FROM insumos_operacionais_migrados moved
WHERE UPPER(BTRIM(recurrence.category)) = 'VARIAVEL'
  AND LOWER(BTRIM(recurrence.subcategory)) = LOWER(moved.source_code);


UPDATE expense_subcategories destination
SET group_label = COALESCE(destination.group_label, moved.group_label),
    segment = COALESCE(destination.segment, moved.segment),
    is_active = destination.is_active OR moved.is_active
FROM insumos_operacionais_migrados moved
WHERE destination.id = moved.existing_subcategory_id;

DELETE FROM expense_subcategories source
USING insumos_operacionais_migrados moved
WHERE source.id = moved.source_id
  AND moved.existing_subcategory_id IS NOT NULL;

UPDATE expense_subcategories source
SET category_id = moved.destination_category_id
FROM insumos_operacionais_migrados moved
WHERE source.id = moved.source_id
  AND moved.existing_subcategory_id IS NULL;

DROP TABLE insumos_operacionais_migrados;
