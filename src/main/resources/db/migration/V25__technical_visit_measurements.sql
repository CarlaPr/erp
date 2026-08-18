-- Coleta completa de dados da visita técnica: vãos nomeados, medidas,
-- furos/recortes livres e fotos. A visita também pode nascer diretamente
-- de um cliente, sem exigir orçamento prévio.

ALTER TABLE technical_visits
    ALTER COLUMN quote_id DROP NOT NULL;

ALTER TABLE technical_visits
    ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'AGENDADA'
        CHECK (status IN ('AGENDADA', 'EM_ANDAMENTO', 'CONCLUIDA'));

ALTER TABLE technical_visits
    DROP CONSTRAINT IF EXISTS fk_technical_visits_quote;

ALTER TABLE technical_visits
    ADD CONSTRAINT fk_technical_visits_quote
        FOREIGN KEY (quote_id) REFERENCES quotes(id) ON DELETE SET NULL;

CREATE TABLE technical_visit_openings (
    id UUID PRIMARY KEY,
    technical_visit_id UUID NOT NULL REFERENCES technical_visits(id) ON DELETE CASCADE,
    name VARCHAR(120) NOT NULL,
    width_mm NUMERIC(9,2),
    height_mm NUMERIC(9,2),
    gross_height_left_mm NUMERIC(9,2),
    gross_height_right_mm NUMERIC(9,2),
    gross_width_top_mm NUMERIC(9,2),
    gross_width_bottom_mm NUMERIC(9,2),
    notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now()
);
CREATE INDEX idx_technical_visit_openings_visit ON technical_visit_openings(technical_visit_id);

CREATE TABLE technical_visit_features (
    id UUID PRIMARY KEY,
    opening_id UUID NOT NULL REFERENCES technical_visit_openings(id) ON DELETE CASCADE,
    type VARCHAR(30) NOT NULL CHECK (type IN ('FURO','RASGO','RECORTE','CHANFRO','BOLEADO')),
    name VARCHAR(100),
    reference_horizontal VARCHAR(20) NOT NULL DEFAULT 'ESQUERDA'
        CHECK (reference_horizontal IN ('ESQUERDA','CENTRO','DIREITA')),
    distance_horizontal_mm NUMERIC(9,2),
    reference_vertical VARCHAR(20) NOT NULL DEFAULT 'SUPERIOR'
        CHECK (reference_vertical IN ('SUPERIOR','CENTRO','INFERIOR')),
    distance_vertical_mm NUMERIC(9,2),
    diameter_mm NUMERIC(9,2),
    width_mm NUMERIC(9,2),
    height_mm NUMERIC(9,2),
    depth_mm NUMERIC(9,2),
    radius_mm NUMERIC(9,2),
    corner VARCHAR(40),
    notes VARCHAR(255)
);
CREATE INDEX idx_technical_visit_features_opening ON technical_visit_features(opening_id);

CREATE TABLE technical_visit_photos (
    id UUID PRIMARY KEY,
    technical_visit_id UUID NOT NULL REFERENCES technical_visits(id) ON DELETE CASCADE,
    file_name VARCHAR(255) NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    file_size BIGINT NOT NULL,
    caption VARCHAR(180),
    content BYTEA NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT now()
);
CREATE INDEX idx_technical_visit_photos_visit ON technical_visit_photos(technical_visit_id);
