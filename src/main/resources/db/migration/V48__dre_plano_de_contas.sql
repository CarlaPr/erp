
ALTER TABLE expense_categories    ADD COLUMN IF NOT EXISTS dre_group     VARCHAR(40);
ALTER TABLE expense_subcategories ADD COLUMN IF NOT EXISTS dre_group     VARCHAR(40);
ALTER TABLE expense_subcategories ADD COLUMN IF NOT EXISTS cost_behavior VARCHAR(10);


CREATE TEMPORARY TABLE dre_catalog_v48 (
    cat          VARCHAR(40)  NOT NULL,
    code         VARCHAR(120) NOT NULL,
    name         VARCHAR(150) NOT NULL,
    group_label  VARCHAR(60),
    display_ord  INTEGER      NOT NULL,
    dre_group    VARCHAR(40)  NOT NULL,
    behavior     VARCHAR(10)
) ON COMMIT DROP;

INSERT INTO dre_catalog_v48 (cat, code, name, group_label, display_ord, dre_group, behavior) VALUES
    ('OPERACIONAL', 'vidros', 'Vidros (geral)', 'Vidros', 10, 'CMV_MATERIAIS', 'VARIAVEL'),
    ('OPERACIONAL', 'vidro_temperado', 'Vidro temperado', 'Vidros', 20, 'CMV_MATERIAIS', 'VARIAVEL'),
    ('OPERACIONAL', 'vidro_laminado', 'Vidro laminado', 'Vidros', 30, 'CMV_MATERIAIS', 'VARIAVEL'),
    ('OPERACIONAL', 'vidro_comum', 'Vidro comum', 'Vidros', 40, 'CMV_MATERIAIS', 'VARIAVEL'),
    ('OPERACIONAL', 'vidro_serigrafado', 'Vidro serigrafado', 'Vidros', 50, 'CMV_MATERIAIS', 'VARIAVEL'),
    ('OPERACIONAL', 'vidro_jateado', 'Vidro jateado', 'Vidros', 60, 'CMV_MATERIAIS', 'VARIAVEL'),
    ('OPERACIONAL', 'espelhos', 'Espelhos', 'Vidros', 70, 'CMV_MATERIAIS', 'VARIAVEL'),
    ('OPERACIONAL', 'ferragens', 'Ferragens (geral)', 'Ferragens', 80, 'CMV_MATERIAIS', 'VARIAVEL'),
    ('OPERACIONAL', 'roldanas', 'Roldanas', 'Ferragens', 90, 'CMV_MATERIAIS', 'VARIAVEL'),
    ('OPERACIONAL', 'trilhos', 'Trilhos', 'Ferragens', 100, 'CMV_MATERIAIS', 'VARIAVEL'),
    ('OPERACIONAL', 'puxadores', 'Puxadores', 'Ferragens', 110, 'CMV_MATERIAIS', 'VARIAVEL'),
    ('OPERACIONAL', 'dobradicas', 'Dobradiças', 'Ferragens', 120, 'CMV_MATERIAIS', 'VARIAVEL'),
    ('OPERACIONAL', 'fechaduras', 'Fechaduras', 'Ferragens', 130, 'CMV_MATERIAIS', 'VARIAVEL'),
    ('OPERACIONAL', 'kits_para_box', 'Kits para Box', 'Ferragens', 140, 'CMV_MATERIAIS', 'VARIAVEL'),
    ('OPERACIONAL', 'kits_para_sacada', 'Kits para Sacada', 'Ferragens', 150, 'CMV_MATERIAIS', 'VARIAVEL'),
    ('OPERACIONAL', 'kits_para_guarda_corpo', 'Kits para Guarda-Corpo', 'Ferragens', 160, 'CMV_MATERIAIS', 'VARIAVEL'),
    ('OPERACIONAL', 'perfis_de_aluminio', 'Perfis de alumínio', 'Alumínio', 170, 'CMV_MATERIAIS', 'VARIAVEL'),
    ('OPERACIONAL', 'linha_suprema', 'Linha Suprema', 'Alumínio', 180, 'CMV_MATERIAIS', 'VARIAVEL'),
    ('OPERACIONAL', 'linha_gold', 'Linha Gold', 'Alumínio', 190, 'CMV_MATERIAIS', 'VARIAVEL'),
    ('OPERACIONAL', 'linha_25', 'Linha 25', 'Alumínio', 200, 'CMV_MATERIAIS', 'VARIAVEL'),
    ('OPERACIONAL', 'linha_30', 'Linha 30', 'Alumínio', 210, 'CMV_MATERIAIS', 'VARIAVEL'),
    ('OPERACIONAL', 'linha_42', 'Linha 42', 'Alumínio', 220, 'CMV_MATERIAIS', 'VARIAVEL'),
    ('OPERACIONAL', 'linha_integrada', 'Linha Integrada', 'Alumínio', 230, 'CMV_MATERIAIS', 'VARIAVEL'),
    ('OPERACIONAL', 'linha_fachada', 'Linha Fachada', 'Alumínio', 240, 'CMV_MATERIAIS', 'VARIAVEL'),
    ('OPERACIONAL', 'silicone', 'Silicone', 'Consumíveis', 250, 'CMV_MATERIAIS', 'VARIAVEL'),
    ('OPERACIONAL', 'parafusos', 'Parafusos', 'Consumíveis', 260, 'CMV_MATERIAIS', 'VARIAVEL'),
    ('OPERACIONAL', 'buchas', 'Buchas', 'Consumíveis', 270, 'CMV_MATERIAIS', 'VARIAVEL'),
    ('OPERACIONAL', 'rebites', 'Rebites', 'Consumíveis', 280, 'CMV_MATERIAIS', 'VARIAVEL'),
    ('OPERACIONAL', 'cola_uv', 'Cola UV', 'Consumíveis', 290, 'CMV_MATERIAIS', 'VARIAVEL'),
    ('OPERACIONAL', 'fitas', 'Fitas', 'Consumíveis', 300, 'CMV_MATERIAIS', 'VARIAVEL'),
    ('OPERACIONAL', 'materiais_de_instalacao', 'Materiais de instalação', 'Consumíveis', 310, 'CMV_MATERIAIS', 'VARIAVEL'),
    ('OPERACIONAL', 'embalagens', 'Embalagens', 'Consumíveis', 320, 'CMV_MATERIAIS', 'VARIAVEL'),
    ('OPERACIONAL', 'tempera', 'Têmpera', 'Serviços de produção', 330, 'CMV_SERV_TERCEIROS', 'VARIAVEL'),
    ('OPERACIONAL', 'lapidacao', 'Lapidação', 'Serviços de produção', 340, 'CMV_SERV_TERCEIROS', 'VARIAVEL'),
    ('OPERACIONAL', 'furacao', 'Furação', 'Serviços de produção', 350, 'CMV_SERV_TERCEIROS', 'VARIAVEL'),
    ('OPERACIONAL', 'recortes', 'Recortes', 'Serviços de produção', 360, 'CMV_SERV_TERCEIROS', 'VARIAVEL'),
    ('OPERACIONAL', 'pintura', 'Pintura', 'Serviços de produção', 370, 'CMV_SERV_TERCEIROS', 'VARIAVEL'),
    ('OPERACIONAL', 'jateamento', 'Jateamento', 'Serviços de produção', 380, 'CMV_SERV_TERCEIROS', 'VARIAVEL'),
    ('OPERACIONAL', 'servicos_terceirizados', 'Serviços terceirizados de produção', 'Serviços de produção', 390, 'CMV_SERV_TERCEIROS', 'VARIAVEL'),
    ('OPERACIONAL', 'instalacao', 'Instalação', 'Instalação e logística', 400, 'CMV_INSTALACAO', 'VARIAVEL'),
    ('OPERACIONAL', 'terceirizacao', 'Terceirização de instalação', 'Instalação e logística', 410, 'CMV_INSTALACAO', 'VARIAVEL'),
    ('OPERACIONAL', 'transporte', 'Transporte', 'Instalação e logística', 420, 'CMV_INSTALACAO', 'VARIAVEL'),
    ('OPERACIONAL', 'frete', 'Frete', 'Instalação e logística', 430, 'CMV_INSTALACAO', 'VARIAVEL'),
    ('OPERACIONAL', 'combustivel', 'Combustível', 'Instalação e logística', 440, 'CMV_INSTALACAO', 'VARIAVEL'),
    ('OPERACIONAL', 'pedagio', 'Pedágio', 'Instalação e logística', 450, 'CMV_INSTALACAO', 'VARIAVEL'),
    ('OPERACIONAL', 'estacionamento', 'Estacionamento', 'Instalação e logística', 460, 'CMV_INSTALACAO', 'VARIAVEL'),
    ('OPERACIONAL', 'uber_99', 'Uber/99', 'Instalação e logística', 470, 'CMV_INSTALACAO', 'VARIAVEL'),
    ('OPERACIONAL', 'almoco', 'Almoço', 'Instalação e logística', 480, 'CMV_INSTALACAO', 'VARIAVEL'),
    ('OPERACIONAL', 'alimentacao_da_equipe', 'Alimentação da equipe', 'Instalação e logística', 490, 'CMV_INSTALACAO', 'VARIAVEL'),
    ('OPERACIONAL', 'hospedagem_equipe', 'Hospedagem da equipe', 'Instalação e logística', 500, 'CMV_INSTALACAO', 'VARIAVEL'),
    ('OPERACIONAL', 'equipamentos_de_protecao', 'EPIs e equipamentos de proteção', 'Instalação e logística', 510, 'CMV_INSTALACAO', 'VARIAVEL'),
    ('OPERACIONAL', 'ferramentas', 'Ferramentas e pequenos utensílios', 'Instalação e logística', 520, 'CMV_INSTALACAO', 'VARIAVEL'),
    ('OPERACIONAL', 'pequenas_compras', 'Pequenas compras de obra', 'Instalação e logística', 530, 'CMV_INSTALACAO', 'VARIAVEL'),
    ('PESSOAL', 'salarios', 'Salários', 'Remuneração', 10, 'DESP_PESSOAL', 'FIXA'),
    ('PESSOAL', 'pro-labore', 'Pró-labore', 'Remuneração', 20, 'DESP_PESSOAL', 'FIXA'),
    ('PESSOAL', 'adiantamentos', 'Adiantamentos', 'Remuneração', 30, 'DESP_PESSOAL', 'FIXA'),
    ('PESSOAL', 'vale_transporte', 'Vale transporte', 'Benefícios', 40, 'DESP_PESSOAL', 'FIXA'),
    ('PESSOAL', 'vale_refeicao', 'Vale refeição', 'Benefícios', 50, 'DESP_PESSOAL', 'FIXA'),
    ('PESSOAL', 'plano_de_saude', 'Plano de saúde', 'Benefícios', 60, 'DESP_PESSOAL', 'FIXA'),
    ('PESSOAL', 'horas_extras', 'Horas extras', 'Variáveis', 70, 'DESP_PESSOAL', 'VARIAVEL'),
    ('PESSOAL', 'bonificacoes', 'Bonificações', 'Variáveis', 80, 'DESP_PESSOAL', 'VARIAVEL'),
    ('PESSOAL', 'premiacoes', 'Premiações', 'Variáveis', 90, 'DESP_PESSOAL', 'VARIAVEL'),
    ('PESSOAL', 'ajuda_de_custo', 'Ajuda de custo', 'Variáveis', 100, 'DESP_PESSOAL', 'VARIAVEL'),
    ('PESSOAL', 'uniformes', 'Uniformes', 'Variáveis', 110, 'DESP_PESSOAL', 'VARIAVEL'),
    ('PESSOAL', 'cursos', 'Cursos e capacitação', 'Variáveis', 120, 'DESP_PESSOAL', 'VARIAVEL'),
    ('PESSOAL', 'treinamentos', 'Treinamentos', 'Variáveis', 130, 'DESP_PESSOAL', 'VARIAVEL'),
    ('PESSOAL', 'diarias', 'Diárias (equipe de obra)', 'Equipe de obra', 140, 'CMV_INSTALACAO', 'VARIAVEL'),
    ('COMERCIAL', 'marketing', 'Marketing', 'Marketing', 10, 'DESP_COMERCIAL', 'FIXA'),
    ('COMERCIAL', 'google_ads', 'Google Ads', 'Marketing', 20, 'DESP_COMERCIAL', 'FIXA'),
    ('COMERCIAL', 'facebook_ads', 'Facebook/Instagram Ads', 'Marketing', 30, 'DESP_COMERCIAL', 'FIXA'),
    ('COMERCIAL', 'brindes', 'Brindes', 'Marketing', 40, 'DESP_COMERCIAL', 'FIXA'),
    ('COMERCIAL', 'eventos', 'Eventos', 'Marketing', 50, 'DESP_COMERCIAL', 'FIXA'),
    ('COMERCIAL', 'expansao_comercial', 'Expansão comercial', 'Marketing', 60, 'DESP_COMERCIAL', 'FIXA'),
    ('COMERCIAL', 'comissoes', 'Comissões de vendas', 'Vendas', 70, 'DESP_COMERCIAL', 'VARIAVEL'),
    ('ADMINISTRATIVO', 'aluguel', 'Aluguel', 'Ocupação e utilidades', 10, 'DESP_OCUPACAO', 'FIXA'),
    ('ADMINISTRATIVO', 'condominio', 'Condomínio', 'Ocupação e utilidades', 20, 'DESP_OCUPACAO', 'FIXA'),
    ('ADMINISTRATIVO', 'energia', 'Energia', 'Ocupação e utilidades', 30, 'DESP_OCUPACAO', 'FIXA'),
    ('ADMINISTRATIVO', 'agua', 'Água', 'Ocupação e utilidades', 40, 'DESP_OCUPACAO', 'FIXA'),
    ('ADMINISTRATIVO', 'internet', 'Internet', 'Ocupação e utilidades', 50, 'DESP_OCUPACAO', 'FIXA'),
    ('ADMINISTRATIVO', 'telefone', 'Telefone', 'Ocupação e utilidades', 60, 'DESP_OCUPACAO', 'FIXA'),
    ('ADMINISTRATIVO', 'seguranca', 'Segurança', 'Ocupação e utilidades', 70, 'DESP_OCUPACAO', 'FIXA'),
    ('ADMINISTRATIVO', 'vigilancia', 'Vigilância', 'Ocupação e utilidades', 80, 'DESP_OCUPACAO', 'FIXA'),
    ('ADMINISTRATIVO', 'limpeza', 'Limpeza', 'Ocupação e utilidades', 90, 'DESP_OCUPACAO', 'FIXA'),
    ('ADMINISTRATIVO', 'contabilidade', 'Contabilidade', 'Serviços e sistemas', 100, 'DESP_ADMIN', 'FIXA'),
    ('ADMINISTRATIVO', 'consultorias', 'Consultorias', 'Serviços e sistemas', 110, 'DESP_ADMIN', 'FIXA'),
    ('ADMINISTRATIVO', 'sistema_erp', 'Sistema ERP', 'Serviços e sistemas', 120, 'DESP_ADMIN', 'FIXA'),
    ('ADMINISTRATIVO', 'licencas_de_software', 'Licenças de software', 'Serviços e sistemas', 130, 'DESP_ADMIN', 'FIXA'),
    ('ADMINISTRATIVO', 'assinaturas', 'Assinaturas', 'Serviços e sistemas', 140, 'DESP_ADMIN', 'FIXA'),
    ('ADMINISTRATIVO', 'dominio', 'Domínio', 'Serviços e sistemas', 150, 'DESP_ADMIN', 'FIXA'),
    ('ADMINISTRATIVO', 'hospedagem_site', 'Hospedagem de site', 'Serviços e sistemas', 160, 'DESP_ADMIN', 'FIXA'),
    ('ADMINISTRATIVO', 'seguro_empresarial', 'Seguro empresarial', 'Serviços e sistemas', 170, 'DESP_ADMIN', 'FIXA'),
    ('ADMINISTRATIVO', 'servicos_administrativos', 'Serviços administrativos', 'Serviços e sistemas', 180, 'DESP_ADMIN', 'FIXA'),
    ('ADMINISTRATIVO', 'papelaria', 'Papelaria e material de escritório', 'Escritório', 190, 'DESP_ADMIN', 'VARIAVEL'),
    ('ADMINISTRATIVO', 'cartuchos', 'Cartuchos e impressões', 'Escritório', 200, 'DESP_ADMIN', 'VARIAVEL'),
    ('MANUTENCAO', 'manutencao_de_veiculos', 'Manutenção de veículos', NULL, 10, 'DESP_MANUTENCAO', 'VARIAVEL'),
    ('MANUTENCAO', 'manutencao_de_maquinas', 'Manutenção de máquinas', NULL, 20, 'DESP_MANUTENCAO', 'VARIAVEL'),
    ('MANUTENCAO', 'manutencao_de_ferramentas', 'Manutenção de ferramentas', NULL, 30, 'DESP_MANUTENCAO', 'VARIAVEL'),
    ('MANUTENCAO', 'troca_de_pecas', 'Troca de peças', NULL, 40, 'DESP_MANUTENCAO', 'VARIAVEL'),
    ('MANUTENCAO', 'manutencao_emergencial', 'Manutenção emergencial', NULL, 50, 'DESP_MANUTENCAO', 'VARIAVEL'),
    ('MANUTENCAO', 'manutencao_terceirizada', 'Serviços terceirizados de manutenção', NULL, 60, 'DESP_MANUTENCAO', 'VARIAVEL'),
    ('MANUTENCAO', 'manutencao_predial', 'Manutenção predial', NULL, 70, 'DESP_MANUTENCAO', 'FIXA'),
    ('IMPOSTOS', 'das', 'DAS (Simples Nacional)', 'Sobre vendas (dedução da receita)', 10, 'DED_IMPOSTOS_VENDAS', 'VARIAVEL'),
    ('IMPOSTOS', 'simples_nacional', 'Simples Nacional (outros)', 'Sobre vendas (dedução da receita)', 20, 'DED_IMPOSTOS_VENDAS', 'VARIAVEL'),
    ('IMPOSTOS', 'iss', 'ISS', 'Sobre vendas (dedução da receita)', 30, 'DED_IMPOSTOS_VENDAS', 'VARIAVEL'),
    ('IMPOSTOS', 'icms', 'ICMS', 'Sobre vendas (dedução da receita)', 40, 'DED_IMPOSTOS_VENDAS', 'VARIAVEL'),
    ('IMPOSTOS', 'pis', 'PIS', 'Sobre vendas (dedução da receita)', 50, 'DED_IMPOSTOS_VENDAS', 'VARIAVEL'),
    ('IMPOSTOS', 'cofins', 'COFINS', 'Sobre vendas (dedução da receita)', 60, 'DED_IMPOSTOS_VENDAS', 'VARIAVEL'),
    ('IMPOSTOS', 'irpj', 'IRPJ', 'Sobre o lucro', 70, 'IR_CSLL', 'VARIAVEL'),
    ('IMPOSTOS', 'csll', 'CSLL', 'Sobre o lucro', 80, 'IR_CSLL', 'VARIAVEL'),
    ('IMPOSTOS', 'iptu', 'IPTU', 'Taxas e tributos gerais', 90, 'DESP_TRIBUTARIAS', 'FIXA'),
    ('IMPOSTOS', 'ipva', 'IPVA', 'Taxas e tributos gerais', 100, 'DESP_TRIBUTARIAS', 'FIXA'),
    ('IMPOSTOS', 'taxas_municipais', 'Taxas municipais', 'Taxas e tributos gerais', 110, 'DESP_TRIBUTARIAS', 'FIXA'),
    ('IMPOSTOS', 'taxas_estaduais', 'Taxas estaduais', 'Taxas e tributos gerais', 120, 'DESP_TRIBUTARIAS', 'FIXA'),
    ('FINANCEIRO', 'tarifas_bancarias', 'Tarifas bancárias', NULL, 10, 'DESP_FINANCEIRAS', 'FIXA'),
    ('FINANCEIRO', 'juros', 'Juros e encargos (empréstimos, financiamentos, atrasos)', NULL, 20, 'DESP_FINANCEIRAS', 'FIXA'),
    ('FINANCEIRO', 'multas', 'Multas', NULL, 30, 'DESP_FINANCEIRAS', 'VARIAVEL'),
    ('FINANCEIRO', 'iof', 'IOF', NULL, 40, 'DESP_FINANCEIRAS', 'VARIAVEL'),
    ('FINANCEIRO', 'antecipacao_de_recebiveis', 'Antecipação de recebíveis', NULL, 50, 'DESP_FINANCEIRAS', 'VARIAVEL'),
    ('FINANCEIRO', 'taxas_de_cartao', 'Taxas de cartão / maquininha', NULL, 60, 'DESP_FIN_CARTAO', 'VARIAVEL'),
    ('FINANCEIRO', 'emprestimos', 'Parcelas de empréstimos (principal)', 'Fora da DRE (principal da dívida)', 70, 'FORA_AMORTIZACAO', NULL),
    ('FINANCEIRO', 'financiamentos', 'Parcelas de financiamentos (principal)', 'Fora da DRE (principal da dívida)', 80, 'FORA_AMORTIZACAO', NULL),
    ('PROVISIONAMENTO', '13o_salario', '13º salário', 'Provisões trabalhistas (despesa)', 10, 'DESP_PESSOAL', 'FIXA'),
    ('PROVISIONAMENTO', 'ferias', 'Férias', 'Provisões trabalhistas (despesa)', 20, 'DESP_PESSOAL', 'FIXA'),
    ('PROVISIONAMENTO', 'fgts', 'FGTS', 'Provisões trabalhistas (despesa)', 30, 'DESP_PESSOAL', 'FIXA'),
    ('PROVISIONAMENTO', 'inss', 'INSS', 'Provisões trabalhistas (despesa)', 40, 'DESP_PESSOAL', 'FIXA'),
    ('PROVISIONAMENTO', 'rescisoes', 'Rescisões', 'Provisões trabalhistas (despesa)', 50, 'DESP_PESSOAL', 'VARIAVEL'),
    ('PROVISIONAMENTO', 'reserva_de_caixa', 'Reserva de caixa', 'Reservas (fora da DRE)', 60, 'FORA_RESERVAS', NULL),
    ('PROVISIONAMENTO', 'fundo_emergencial', 'Fundo emergencial', 'Reservas (fora da DRE)', 70, 'FORA_RESERVAS', NULL),
    ('PROVISIONAMENTO', 'capital_de_giro', 'Capital de giro', 'Reservas (fora da DRE)', 80, 'FORA_RESERVAS', NULL),
    ('PROVISIONAMENTO', 'reserva_tributaria', 'Reserva tributária', 'Reservas (fora da DRE)', 90, 'FORA_RESERVAS', NULL),
    ('PROVISIONAMENTO', 'expansao_da_empresa', 'Expansão da empresa', 'Reservas (fora da DRE)', 100, 'FORA_RESERVAS', NULL),
    ('PROVISIONAMENTO', 'compra_futura_de_maquinas', 'Compra futura de máquinas', 'Reservas (fora da DRE)', 110, 'FORA_RESERVAS', NULL),
    ('PROVISIONAMENTO', 'renovacao_de_frota', 'Renovação de frota', 'Reservas (fora da DRE)', 120, 'FORA_RESERVAS', NULL),
    ('PROVISIONAMENTO', 'troca_de_equipamentos', 'Troca de equipamentos', 'Reservas (fora da DRE)', 130, 'FORA_RESERVAS', NULL),
    ('INVESTIMENTOS', 'compra_de_maquinas', 'Compra de máquinas', NULL, 10, 'FORA_INVESTIMENTOS', NULL),
    ('INVESTIMENTOS', 'compra_de_computadores', 'Compra de computadores', NULL, 20, 'FORA_INVESTIMENTOS', NULL),
    ('INVESTIMENTOS', 'compra_de_veiculos', 'Compra de veículos', NULL, 30, 'FORA_INVESTIMENTOS', NULL),
    ('INVESTIMENTOS', 'compra_de_ferramentas', 'Compra de ferramentas duráveis', NULL, 40, 'FORA_INVESTIMENTOS', NULL),
    ('INVESTIMENTOS', 'compra_de_moveis', 'Compra de móveis', NULL, 50, 'FORA_INVESTIMENTOS', NULL),
    ('INVESTIMENTOS', 'compra_de_equipamentos', 'Compra de equipamentos', NULL, 60, 'FORA_INVESTIMENTOS', NULL),
    ('INVESTIMENTOS', 'compra_de_impressoras', 'Compra de impressoras', NULL, 70, 'FORA_INVESTIMENTOS', NULL),
    ('SOCIOS', 'distribuicao_de_lucros', 'Distribuição de lucros', NULL, 10, 'FORA_RETIRADAS', NULL),
    ('SOCIOS', 'retirada_de_socios', 'Retiradas dos sócios', NULL, 20, 'FORA_RETIRADAS', NULL);


CREATE TEMPORARY TABLE dre_legacy_map_v48 (
    old_cat VARCHAR(40)  NOT NULL,
    old_sub VARCHAR(120) NOT NULL,
    new_cat VARCHAR(40)  NOT NULL,
    new_sub VARCHAR(120) NOT NULL
) ON COMMIT DROP;

INSERT INTO dre_legacy_map_v48 (old_cat, old_sub, new_cat, new_sub) VALUES
    ('ADMINISTRATIVO', 'equipamentos', 'INVESTIMENTOS', 'compra_de_equipamentos'),
    ('ADMINISTRATIVO', 'impressoras', 'INVESTIMENTOS', 'compra_de_impressoras'),
    ('ADMINISTRATIVO', 'moveis', 'INVESTIMENTOS', 'compra_de_moveis'),
    ('FIXA', 'agua', 'ADMINISTRATIVO', 'agua'),
    ('FIXA', 'aluguel', 'ADMINISTRATIVO', 'aluguel'),
    ('FIXA', 'condominio', 'ADMINISTRATIVO', 'condominio'),
    ('FIXA', 'contabilidade', 'ADMINISTRATIVO', 'contabilidade'),
    ('FIXA', 'dominio', 'ADMINISTRATIVO', 'dominio'),
    ('FIXA', 'energia', 'ADMINISTRATIVO', 'energia'),
    ('FIXA', 'hospedagem', 'ADMINISTRATIVO', 'hospedagem_site'),
    ('FIXA', 'internet', 'ADMINISTRATIVO', 'internet'),
    ('FIXA', 'licencas_de_software', 'ADMINISTRATIVO', 'licencas_de_software'),
    ('FIXA', 'limpeza', 'ADMINISTRATIVO', 'limpeza'),
    ('FIXA', 'plano_de_saude', 'PESSOAL', 'plano_de_saude'),
    ('FIXA', 'pro-labore', 'PESSOAL', 'pro-labore'),
    ('FIXA', 'salarios', 'PESSOAL', 'salarios'),
    ('FIXA', 'seguranca', 'ADMINISTRATIVO', 'seguranca'),
    ('FIXA', 'seguro_empresarial', 'ADMINISTRATIVO', 'seguro_empresarial'),
    ('FIXA', 'sistema_erp', 'ADMINISTRATIVO', 'sistema_erp'),
    ('FIXA', 'telefone', 'ADMINISTRATIVO', 'telefone'),
    ('FIXA', 'uniformes', 'PESSOAL', 'uniformes'),
    ('FIXA', 'vale_refeicao', 'PESSOAL', 'vale_refeicao'),
    ('FIXA', 'vale_transporte', 'PESSOAL', 'vale_transporte'),
    ('FIXA', 'vigilancia', 'ADMINISTRATIVO', 'vigilancia'),
    ('INVESTIMENTOS', 'consultorias', 'ADMINISTRATIVO', 'consultorias'),
    ('INVESTIMENTOS', 'cursos', 'PESSOAL', 'cursos'),
    ('INVESTIMENTOS', 'expansao_comercial', 'COMERCIAL', 'expansao_comercial'),
    ('INVESTIMENTOS', 'treinamentos', 'PESSOAL', 'treinamentos'),
    ('MANUTENCAO', 'servicos_terceirizados', 'MANUTENCAO', 'manutencao_terceirizada'),
    ('OPERACIONAL', 'espelho', 'OPERACIONAL', 'espelhos'),
    ('VARIAVEL', 'alimentacao_da_equipe', 'OPERACIONAL', 'alimentacao_da_equipe'),
    ('VARIAVEL', 'almoco', 'OPERACIONAL', 'almoco'),
    ('VARIAVEL', 'brindes', 'COMERCIAL', 'brindes'),
    ('VARIAVEL', 'combustivel', 'OPERACIONAL', 'combustivel'),
    ('VARIAVEL', 'compra_de_epis', 'OPERACIONAL', 'equipamentos_de_protecao'),
    ('VARIAVEL', 'estacionamento', 'OPERACIONAL', 'estacionamento'),
    ('VARIAVEL', 'eventos', 'COMERCIAL', 'eventos'),
    ('VARIAVEL', 'facebook_ads', 'COMERCIAL', 'facebook_ads'),
    ('VARIAVEL', 'ferramentas', 'OPERACIONAL', 'ferramentas'),
    ('VARIAVEL', 'frete', 'OPERACIONAL', 'frete'),
    ('VARIAVEL', 'google_ads', 'COMERCIAL', 'google_ads'),
    ('VARIAVEL', 'hospedagem', 'OPERACIONAL', 'hospedagem_equipe'),
    ('VARIAVEL', 'impressoes', 'ADMINISTRATIVO', 'cartuchos'),
    ('VARIAVEL', 'manutencao_emergencial', 'MANUTENCAO', 'manutencao_emergencial'),
    ('VARIAVEL', 'marketing', 'COMERCIAL', 'marketing'),
    ('VARIAVEL', 'material_de_escritorio', 'ADMINISTRATIVO', 'papelaria'),
    ('VARIAVEL', 'pedagio', 'OPERACIONAL', 'pedagio'),
    ('VARIAVEL', 'pequenas_compras', 'OPERACIONAL', 'pequenas_compras'),
    ('VARIAVEL', 'uber_99', 'OPERACIONAL', 'uber_99');

-- ── Categorias funcionais (cria as novas e atualiza nome/ordem/linha padrão da DRE) ──
INSERT INTO expense_categories (id, code, name, display_order, is_active, dre_group)
VALUES (md5('expense_category:v48:' || 'OPERACIONAL')::uuid, 'OPERACIONAL', 'Operacional (Custo dos Serviços)', 10, TRUE, 'CMV_MATERIAIS')
ON CONFLICT (code) DO UPDATE SET name = EXCLUDED.name, display_order = EXCLUDED.display_order,
    is_active = EXCLUDED.is_active, dre_group = EXCLUDED.dre_group;
INSERT INTO expense_categories (id, code, name, display_order, is_active, dre_group)
VALUES (md5('expense_category:v48:' || 'PESSOAL')::uuid, 'PESSOAL', 'Pessoal e Encargos', 20, TRUE, 'DESP_PESSOAL')
ON CONFLICT (code) DO UPDATE SET name = EXCLUDED.name, display_order = EXCLUDED.display_order,
    is_active = EXCLUDED.is_active, dre_group = EXCLUDED.dre_group;
INSERT INTO expense_categories (id, code, name, display_order, is_active, dre_group)
VALUES (md5('expense_category:v48:' || 'COMERCIAL')::uuid, 'COMERCIAL', 'Comercial e Marketing', 30, TRUE, 'DESP_COMERCIAL')
ON CONFLICT (code) DO UPDATE SET name = EXCLUDED.name, display_order = EXCLUDED.display_order,
    is_active = EXCLUDED.is_active, dre_group = EXCLUDED.dre_group;
INSERT INTO expense_categories (id, code, name, display_order, is_active, dre_group)
VALUES (md5('expense_category:v48:' || 'ADMINISTRATIVO')::uuid, 'ADMINISTRATIVO', 'Administrativo e Ocupação', 40, TRUE, 'DESP_ADMIN')
ON CONFLICT (code) DO UPDATE SET name = EXCLUDED.name, display_order = EXCLUDED.display_order,
    is_active = EXCLUDED.is_active, dre_group = EXCLUDED.dre_group;
INSERT INTO expense_categories (id, code, name, display_order, is_active, dre_group)
VALUES (md5('expense_category:v48:' || 'MANUTENCAO')::uuid, 'MANUTENCAO', 'Manutenção e Conservação', 50, TRUE, 'DESP_MANUTENCAO')
ON CONFLICT (code) DO UPDATE SET name = EXCLUDED.name, display_order = EXCLUDED.display_order,
    is_active = EXCLUDED.is_active, dre_group = EXCLUDED.dre_group;
INSERT INTO expense_categories (id, code, name, display_order, is_active, dre_group)
VALUES (md5('expense_category:v48:' || 'IMPOSTOS')::uuid, 'IMPOSTOS', 'Impostos e Taxas', 60, TRUE, 'DESP_TRIBUTARIAS')
ON CONFLICT (code) DO UPDATE SET name = EXCLUDED.name, display_order = EXCLUDED.display_order,
    is_active = EXCLUDED.is_active, dre_group = EXCLUDED.dre_group;
INSERT INTO expense_categories (id, code, name, display_order, is_active, dre_group)
VALUES (md5('expense_category:v48:' || 'FINANCEIRO')::uuid, 'FINANCEIRO', 'Despesas Financeiras', 70, TRUE, 'DESP_FINANCEIRAS')
ON CONFLICT (code) DO UPDATE SET name = EXCLUDED.name, display_order = EXCLUDED.display_order,
    is_active = EXCLUDED.is_active, dre_group = EXCLUDED.dre_group;
INSERT INTO expense_categories (id, code, name, display_order, is_active, dre_group)
VALUES (md5('expense_category:v48:' || 'PROVISIONAMENTO')::uuid, 'PROVISIONAMENTO', 'Provisões e Reservas', 80, TRUE, 'FORA_RESERVAS')
ON CONFLICT (code) DO UPDATE SET name = EXCLUDED.name, display_order = EXCLUDED.display_order,
    is_active = EXCLUDED.is_active, dre_group = EXCLUDED.dre_group;
INSERT INTO expense_categories (id, code, name, display_order, is_active, dre_group)
VALUES (md5('expense_category:v48:' || 'INVESTIMENTOS')::uuid, 'INVESTIMENTOS', 'Investimentos (Imobilizado)', 90, TRUE, 'FORA_INVESTIMENTOS')
ON CONFLICT (code) DO UPDATE SET name = EXCLUDED.name, display_order = EXCLUDED.display_order,
    is_active = EXCLUDED.is_active, dre_group = EXCLUDED.dre_group;
INSERT INTO expense_categories (id, code, name, display_order, is_active, dre_group)
VALUES (md5('expense_category:v48:' || 'SOCIOS')::uuid, 'SOCIOS', 'Retiradas e Distribuição de Lucros', 100, TRUE, 'FORA_RETIRADAS')
ON CONFLICT (code) DO UPDATE SET name = EXCLUDED.name, display_order = EXCLUDED.display_order,
    is_active = EXCLUDED.is_active, dre_group = EXCLUDED.dre_group;
INSERT INTO expense_categories (id, code, name, display_order, is_active, dre_group)
VALUES (md5('expense_category:v48:' || 'FIXA')::uuid, 'FIXA', 'Fixa (legado - reclassificar)', 900, FALSE, 'NAO_CLASSIFICADO')
ON CONFLICT (code) DO UPDATE SET name = EXCLUDED.name, display_order = EXCLUDED.display_order,
    is_active = EXCLUDED.is_active, dre_group = EXCLUDED.dre_group;
INSERT INTO expense_categories (id, code, name, display_order, is_active, dre_group)
VALUES (md5('expense_category:v48:' || 'VARIAVEL')::uuid, 'VARIAVEL', 'Variável (legado - reclassificar)', 910, FALSE, 'NAO_CLASSIFICADO')
ON CONFLICT (code) DO UPDATE SET name = EXCLUDED.name, display_order = EXCLUDED.display_order,
    is_active = EXCLUDED.is_active, dre_group = EXCLUDED.dre_group;

-- ── Subcategorias: cria as novas e atualiza nome/grupo/linha da DRE/comportamento ──
INSERT INTO expense_subcategories (id, category_id, code, name, group_label, display_order, is_active, dre_group, cost_behavior)
SELECT md5('expense_subcategory:v48:' || k.cat || ':' || k.code)::uuid,
       cat.id, k.code, k.name, k.group_label, k.display_ord, TRUE, k.dre_group, k.behavior
FROM dre_catalog_v48 k
JOIN expense_categories cat ON cat.code = k.cat
ON CONFLICT (category_id, code) DO UPDATE SET
    name          = EXCLUDED.name,
    group_label   = EXCLUDED.group_label,
    display_order = EXCLUDED.display_order,
    is_active     = TRUE,
    dre_group     = EXCLUDED.dre_group,
    cost_behavior = EXCLUDED.cost_behavior;


-- ── Reclassifica contas a pagar e recorrências existentes (de-para) ──
UPDATE accounts_payable p
   SET category = m.new_cat, subcategory = m.new_sub
  FROM dre_legacy_map_v48 m
 WHERE UPPER(BTRIM(p.category)) = m.old_cat
   AND LOWER(BTRIM(p.subcategory)) = m.old_sub
   AND (m.old_cat <> m.new_cat OR m.old_sub <> m.new_sub);

UPDATE expense_recurrences r
   SET category = m.new_cat, subcategory = m.new_sub
  FROM dre_legacy_map_v48 m
 WHERE UPPER(BTRIM(r.category)) = m.old_cat
   AND LOWER(BTRIM(r.subcategory)) = m.old_sub
   AND (m.old_cat <> m.new_cat OR m.old_sub <> m.new_sub);

-- ── Remove as subcategorias antigas que foram movidas/unificadas ──
DELETE FROM expense_subcategories s
 USING expense_categories cat, dre_legacy_map_v48 m
 WHERE s.category_id = cat.id
   AND cat.code = m.old_cat
   AND s.code = m.old_sub
   AND NOT EXISTS (SELECT 1 FROM dre_catalog_v48 k WHERE k.cat = m.old_cat AND k.code = m.old_sub);

-- ── Comportamento do custo (FIXA/VARIAVEL/FORA_DRE) recalculado pela subcategoria ──
UPDATE accounts_payable p
   SET expense_type = CASE WHEN LEFT(s.dre_group, 5) = 'FORA_' THEN 'FORA_DRE'
                           ELSE COALESCE(s.cost_behavior, p.expense_type) END
  FROM expense_subcategories s
  JOIN expense_categories cat ON cat.id = s.category_id
 WHERE UPPER(BTRIM(p.category)) = cat.code
   AND LOWER(BTRIM(p.subcategory)) = LOWER(s.code);

UPDATE expense_recurrences r
   SET expense_type = CASE WHEN LEFT(s.dre_group, 5) = 'FORA_' THEN 'FORA_DRE'
                           ELSE COALESCE(s.cost_behavior, r.expense_type) END
  FROM expense_subcategories s
  JOIN expense_categories cat ON cat.id = s.category_id
 WHERE UPPER(BTRIM(r.category)) = cat.code
   AND LOWER(BTRIM(r.subcategory)) = LOWER(s.code);

-- Contas e recorrências sem subcategoria herdam o comportamento pela linha padrão da categoria
-- (fora da DRE / custo variável); as demais mantêm o valor atual.
UPDATE accounts_payable p
   SET expense_type = CASE WHEN LEFT(cat.dre_group, 5) = 'FORA_' THEN 'FORA_DRE' ELSE 'VARIAVEL' END
  FROM expense_categories cat
 WHERE UPPER(BTRIM(p.category)) = cat.code
   AND (p.subcategory IS NULL OR BTRIM(p.subcategory) = '')
   AND (LEFT(cat.dre_group, 5) = 'FORA_' OR LEFT(cat.dre_group, 4) IN ('CMV_', 'DED_'));

UPDATE expense_recurrences r
   SET expense_type = CASE WHEN LEFT(cat.dre_group, 5) = 'FORA_' THEN 'FORA_DRE' ELSE 'VARIAVEL' END
  FROM expense_categories cat
 WHERE UPPER(BTRIM(r.category)) = cat.code
   AND (r.subcategory IS NULL OR BTRIM(r.subcategory) = '')
   AND (LEFT(cat.dre_group, 5) = 'FORA_' OR LEFT(cat.dre_group, 4) IN ('CMV_', 'DED_'));

CREATE INDEX IF NOT EXISTS idx_accounts_payable_status_competencia
    ON accounts_payable(status, competencia);
