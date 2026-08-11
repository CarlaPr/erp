-- ===================================================================
-- Módulo "Plano de Corte" (integrado do GlassCutPro para dentro do ERP)
-- ===================================================================
-- Diferenças em relação ao schema original do GlassCutPro:
--   * "fornecedores"  -> reaproveita a tabela "suppliers" já existente no ERP
--     (fornecedor_id BIGINT  vira  supplier_id UUID)
--   * "clientes"      -> plano de corte passa a referenciar a OS (work_orders),
--     não mais o cliente diretamente (work_order_id UUID)
--   * "usuarios"      -> reaproveita "app_users" (login já existente no ERP);
--     o histórico de preço de vidro guarda apenas o username (texto), sem FK.
-- ===================================================================

-- Catálogo de insumos ------------------------------------------------

CREATE TABLE vidros (
    id BIGSERIAL PRIMARY KEY,
    nome VARCHAR(150) NOT NULL,
    fabricante VARCHAR(100),
    supplier_id UUID REFERENCES suppliers(id),
    tipo VARCHAR(20) NOT NULL CHECK (tipo IN ('COMUM','TEMPERADO','LAMINADO','ESPELHO','ACRILICO')),
    espessura NUMERIC(6,2) NOT NULL,
    cor VARCHAR(50),
    acabamento VARCHAR(80),
    valor_por_m2 NUMERIC(12,2) NOT NULL,
    valor_minimo NUMERIC(12,2),
    peso_por_m2 NUMERIC(8,3),
    data_vigencia DATE,
    data_validade DATE,
    observacoes TEXT,
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP
);
CREATE INDEX idx_vidros_ativo ON vidros(ativo);
CREATE INDEX idx_vidros_supplier ON vidros(supplier_id);

CREATE TABLE historico_preco_vidro (
    id BIGSERIAL PRIMARY KEY,
    vidro_id BIGINT NOT NULL REFERENCES vidros(id) ON DELETE CASCADE,
    vidro_nome_snapshot VARCHAR(150) NOT NULL,
    valor_antigo NUMERIC(12,2),
    valor_novo NUMERIC(12,2) NOT NULL,
    usuario VARCHAR(60) NOT NULL,
    origem VARCHAR(20) NOT NULL CHECK (origem IN ('MANUAL','IMPORTACAO')),
    criado_em TIMESTAMP NOT NULL DEFAULT now()
);
CREATE INDEX idx_historico_preco_vidro_vidro ON historico_preco_vidro(vidro_id);

CREATE TABLE perfis_aluminio (
    id BIGSERIAL PRIMARY KEY,
    linha VARCHAR(100) NOT NULL,
    perfil VARCHAR(100) NOT NULL,
    cor VARCHAR(50),
    fabricante VARCHAR(100),
    supplier_id UUID REFERENCES suppliers(id),
    valor_por_metro NUMERIC(12,2) NOT NULL,
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP
);
CREATE INDEX idx_perfis_aluminio_ativo ON perfis_aluminio(ativo);

CREATE TABLE ferragens (
    id BIGSERIAL PRIMARY KEY,
    categoria VARCHAR(80) NOT NULL,
    produto VARCHAR(150) NOT NULL,
    codigo VARCHAR(50),
    supplier_id UUID REFERENCES suppliers(id),
    valor NUMERIC(12,2) NOT NULL,
    unidade VARCHAR(20) NOT NULL DEFAULT 'un',
    tipo_elemento_padrao VARCHAR(20) CHECK (tipo_elemento_padrao IN ('FURO','RASGO','RECORTE','CHANFRO','BOLEADO')),
    diametro_furacao_mm NUMERIC(8,2),
    quantidade_furos INTEGER,
    distancia_borda_mm NUMERIC(8,2),
    distancia_topo_mm NUMERIC(8,2),
    largura_mm NUMERIC(8,2),
    altura_mm NUMERIC(8,2),
    comprimento_mm NUMERIC(8,2),
    profundidade_mm NUMERIC(8,2),
    raio_mm NUMERIC(8,2),
    angulo_graus NUMERIC(6,2),
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP
);
CREATE INDEX idx_ferragens_ativo ON ferragens(ativo);
CREATE INDEX idx_ferragens_categoria ON ferragens(categoria);

CREATE TABLE silicones (
    id BIGSERIAL PRIMARY KEY,
    marca VARCHAR(100) NOT NULL,
    cor VARCHAR(50),
    embalagem VARCHAR(80),
    supplier_id UUID REFERENCES suppliers(id),
    valor NUMERIC(12,2) NOT NULL,
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP
);
CREATE INDEX idx_silicones_ativo ON silicones(ativo);

CREATE TABLE acessorios (
    id BIGSERIAL PRIMARY KEY,
    nome VARCHAR(150) NOT NULL,
    tipo VARCHAR(20) NOT NULL CHECK (tipo IN ('PARAFUSO','BOTAO','BUCHA','CALCO','FITA','ROLDANA','KIT','CANTONEIRA','OUTRO')),
    unidade VARCHAR(20) NOT NULL DEFAULT 'un',
    supplier_id UUID REFERENCES suppliers(id),
    valor NUMERIC(12,2) NOT NULL,
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP
);
CREATE INDEX idx_acessorios_ativo ON acessorios(ativo);

-- Regras técnicas e parâmetros de serviço -----------------------------

CREATE TABLE regras_tecnicas (
    id BIGSERIAL PRIMARY KEY,
    categoria VARCHAR(30) NOT NULL CHECK (categoria IN (
        'ABRIGO_PIA','PORTA_CORRER','JANELA_PADRAO','PORTA_ABRIR',
        'VIDRO_FIXO_PERFIL_U','JANELA_BASCULANTE','SACADA','ESPELHO','BOX_BANHEIRO_PADRAO')),
    codigo VARCHAR(40) NOT NULL,
    descricao VARCHAR(150) NOT NULL,
    dimensao VARCHAR(20) NOT NULL CHECK (dimensao IN ('LARGURA','ALTURA','LARGURA_E_ALTURA')),
    valor_mm NUMERIC(8,2) NOT NULL,
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP,
    CONSTRAINT uk_regra_tecnica_categoria_codigo UNIQUE (categoria, codigo)
);

CREATE TABLE parametros_servico (
    id BIGSERIAL PRIMARY KEY,
    categoria VARCHAR(30) NOT NULL CHECK (categoria IN (
        'ABRIGO_PIA','PORTA_CORRER','JANELA_PADRAO','PORTA_ABRIR',
        'VIDRO_FIXO_PERFIL_U','JANELA_BASCULANTE','SACADA','ESPELHO','BOX_BANHEIRO_PADRAO')),
    codigo VARCHAR(50) NOT NULL,
    descricao VARCHAR(150) NOT NULL,
    valor NUMERIC(10,2),
    valor_texto VARCHAR(100),
    origem VARCHAR(30) DEFAULT 'PARAMETRO_EMPRESA' CHECK (origem IN (
        'NORMA_TECNICA','RECOMENDACAO_FABRICANTE','BOA_PRATICA','PARAMETRO_EMPRESA')),
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP,
    CONSTRAINT uk_parametro_servico_categoria_codigo UNIQUE (categoria, codigo)
);

-- Planos de corte -------------------------------------------------------

CREATE TABLE planos_corte (
    id BIGSERIAL PRIMARY KEY,
    work_order_id UUID NOT NULL REFERENCES work_orders(id),
    categoria VARCHAR(30) NOT NULL CHECK (categoria IN (
        'ABRIGO_PIA','PORTA_CORRER','JANELA_PADRAO','PORTA_ABRIR',
        'VIDRO_FIXO_PERFIL_U','JANELA_BASCULANTE','SACADA','ESPELHO','BOX_BANHEIRO_PADRAO')),
    descricao VARCHAR(255),
    status VARCHAR(20) NOT NULL CHECK (status IN ('RASCUNHO','FINALIZADO')),
    area_total_m2 NUMERIC(12,4) NOT NULL DEFAULT 0,
    peso_total_kg NUMERIC(12,3) NOT NULL DEFAULT 0,
    valor_total_plano NUMERIC(12,2) NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP
);
CREATE INDEX idx_planos_corte_work_order ON planos_corte(work_order_id);
CREATE INDEX idx_planos_corte_status ON planos_corte(status);

CREATE TABLE plano_corte_itens (
    id BIGSERIAL PRIMARY KEY,
    plano_corte_id BIGINT NOT NULL REFERENCES planos_corte(id) ON DELETE CASCADE,
    vidro_id BIGINT,
    vidro_nome_snapshot VARCHAR(150) NOT NULL,
    espessura_snapshot NUMERIC(6,2),
    cor_snapshot VARCHAR(50),
    valor_m2_snapshot NUMERIC(12,2) NOT NULL,
    valor_minimo_snapshot NUMERIC(12,2),
    peso_m2_snapshot NUMERIC(8,3),
    largura_bruta_mm NUMERIC(8,2) NOT NULL,
    altura_bruta_mm NUMERIC(8,2) NOT NULL,
    quantidade INTEGER NOT NULL,
    tipo_borda VARCHAR(20) NOT NULL CHECK (tipo_borda IN ('LISO','LAPIDADO','BISOTADO')),
    observacoes TEXT,
    largura_final_mm NUMERIC(8,2) NOT NULL,
    altura_final_mm NUMERIC(8,2) NOT NULL,
    area_m2 NUMERIC(12,4) NOT NULL,
    espessura_bisote_mm NUMERIC(8,2),
    peso_kg NUMERIC(12,3) NOT NULL,
    valor_total NUMERIC(12,2) NOT NULL,
    tipo_folha VARCHAR(10) NOT NULL DEFAULT 'UNICA' CHECK (tipo_folha IN ('UNICA','FIXA','MOVEL')),
    grupo_vao INTEGER,
    created_at TIMESTAMP NOT NULL DEFAULT now()
);
CREATE INDEX idx_plano_corte_itens_plano ON plano_corte_itens(plano_corte_id);

CREATE TABLE plano_corte_item_furacoes (
    item_id BIGINT NOT NULL REFERENCES plano_corte_itens(id) ON DELETE CASCADE,
    ordem INTEGER NOT NULL,
    tipo VARCHAR(20) CHECK (tipo IN ('ROLDANA','PUXADOR','DOBRADICA','FECHADURA','RECORTE','OUTRO')),
    posicao_x_mm NUMERIC(8,2),
    posicao_y_mm NUMERIC(8,2),
    diametro_mm NUMERIC(8,2),
    descricao VARCHAR(100),
    PRIMARY KEY (item_id, ordem)
);

CREATE TABLE plano_corte_item_elementos (
    item_id BIGINT NOT NULL REFERENCES plano_corte_itens(id) ON DELETE CASCADE,
    ordem INTEGER NOT NULL,
    tipo VARCHAR(20) CHECK (tipo IN ('FURO','RASGO','RECORTE','CHANFRO','BOLEADO')),
    referencia_horizontal VARCHAR(20) CHECK (referencia_horizontal IN ('ESQUERDA','CENTRO','DIREITA')),
    distancia_horizontal_mm NUMERIC(8,2),
    referencia_vertical VARCHAR(20) CHECK (referencia_vertical IN ('SUPERIOR','CENTRO','INFERIOR')),
    distancia_vertical_mm NUMERIC(8,2),
    posicao_x_mm NUMERIC(8,2),
    posicao_y_mm NUMERIC(8,2),
    diametro_mm NUMERIC(8,2),
    largura_mm NUMERIC(8,2),
    altura_mm NUMERIC(8,2),
    comprimento_mm NUMERIC(8,2),
    profundidade_mm NUMERIC(8,2),
    raio_mm NUMERIC(8,2),
    angulo_graus NUMERIC(6,2),
    orientacao VARCHAR(30),
    formato VARCHAR(30),
    lado VARCHAR(20),
    ferragem_nome_snapshot VARCHAR(150),
    observacao VARCHAR(200),
    PRIMARY KEY (item_id, ordem)
);

-- Permissão de acesso ao módulo ------------------------------------------
-- (o SecurityConfig já reservava "/cut-plans/**" para GESTAO; TECNICO também
--  passa a acessar o módulo pois é quem opera o corte na prática)
