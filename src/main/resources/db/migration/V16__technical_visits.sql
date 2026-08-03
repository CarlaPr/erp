-- =====================================================================
-- V16: Visitas Técnicas
-- Permite agendar, a partir de um Orçamento, uma visita técnica ao
-- cliente. As visitas aparecem na Agenda Comercial junto com os
-- agendamentos de execução de serviço (commercial_schedules).
-- =====================================================================

CREATE TABLE public.technical_visits (
    id             uuid NOT NULL,
    quote_id       uuid NOT NULL,
    client_id      uuid,
    visit_date     date NOT NULL,
    visit_time     time without time zone,
    notes          text,
    created_at     timestamp(6) without time zone,
    updated_at     timestamp(6) without time zone
);

ALTER TABLE ONLY public.technical_visits
    ADD CONSTRAINT technical_visits_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.technical_visits
    ADD CONSTRAINT fk_technical_visits_quote FOREIGN KEY (quote_id) REFERENCES public.quotes(id);

ALTER TABLE ONLY public.technical_visits
    ADD CONSTRAINT fk_technical_visits_client FOREIGN KEY (client_id) REFERENCES public.clients(id);

CREATE INDEX idx_technical_visits_quote_id ON public.technical_visits (quote_id);
CREATE INDEX idx_technical_visits_visit_date ON public.technical_visits (visit_date);
