-- =====================================================================
-- Detalhamento técnico posicionado por peça: furos, recortes e chanfros.
-- Usado para gerar o desenho técnico (vista frontal) de cada vidro.
-- Os campos drilling_count/notch_count de cut_plan_items (V15) continuam
-- funcionando como contagem manual rápida quando o detalhamento posicionado
-- não é necessário; quando existem registros aqui, eles passam a ser a
-- fonte da verdade (ver CutPlanItem.getDrillingCount()/getNotchCount()).
-- =====================================================================

CREATE TABLE public.cut_plan_item_drillings (
    id uuid NOT NULL,
    cut_plan_item_id uuid NOT NULL,
    pos_x numeric(10,2) NOT NULL,
    pos_y numeric(10,2) NOT NULL,
    diameter numeric(6,2) NOT NULL,
    drill_type character varying(30) DEFAULT 'passante',
    notes text
);
ALTER TABLE ONLY public.cut_plan_item_drillings ADD CONSTRAINT cut_plan_item_drillings_pkey PRIMARY KEY (id);
ALTER TABLE ONLY public.cut_plan_item_drillings
    ADD CONSTRAINT fk_cpi_drillings_item FOREIGN KEY (cut_plan_item_id) REFERENCES public.cut_plan_items(id) ON DELETE CASCADE;

CREATE TABLE public.cut_plan_item_notches (
    id uuid NOT NULL,
    cut_plan_item_id uuid NOT NULL,
    pos_x numeric(10,2) NOT NULL,
    pos_y numeric(10,2) NOT NULL,
    width numeric(10,2) NOT NULL,
    height numeric(10,2) NOT NULL,
    notes text
);
ALTER TABLE ONLY public.cut_plan_item_notches ADD CONSTRAINT cut_plan_item_notches_pkey PRIMARY KEY (id);
ALTER TABLE ONLY public.cut_plan_item_notches
    ADD CONSTRAINT fk_cpi_notches_item FOREIGN KEY (cut_plan_item_id) REFERENCES public.cut_plan_items(id) ON DELETE CASCADE;

CREATE TABLE public.cut_plan_item_chamfers (
    id uuid NOT NULL,
    cut_plan_item_id uuid NOT NULL,
    corner character varying(20) NOT NULL, -- BOTTOM_LEFT | BOTTOM_RIGHT | TOP_LEFT | TOP_RIGHT
    size numeric(6,2) NOT NULL,
    notes text
);
ALTER TABLE ONLY public.cut_plan_item_chamfers ADD CONSTRAINT cut_plan_item_chamfers_pkey PRIMARY KEY (id);
ALTER TABLE ONLY public.cut_plan_item_chamfers
    ADD CONSTRAINT fk_cpi_chamfers_item FOREIGN KEY (cut_plan_item_id) REFERENCES public.cut_plan_items(id) ON DELETE CASCADE;
