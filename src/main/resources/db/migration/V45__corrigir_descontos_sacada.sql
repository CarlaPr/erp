INSERT INTO parametros_servico (categoria, codigo, descricao, valor, origem)
VALUES
    ('SACADA', 'DESCONTO_ALTURA_MM', 'Desconto de altura da sacada (mm)', 155, 'PARAMETRO_EMPRESA'),
    ('SACADA', 'DESCONTO_LATERAL_ESQUERDO_MM', 'Desconto lateral esquerdo da largura total da sacada (mm)', 10, 'PARAMETRO_EMPRESA'),
    ('SACADA', 'DESCONTO_LATERAL_DIREITO_MM', 'Desconto lateral direito da largura total da sacada (mm)', 10, 'PARAMETRO_EMPRESA')
ON CONFLICT (categoria, codigo) DO UPDATE
SET valor = EXCLUDED.valor,
    updated_at = now();
