
ALTER TABLE public.quotes
    ADD COLUMN date_approved timestamp(6) without time zone;

ALTER TABLE public.work_orders
    ALTER COLUMN notes TYPE text;

CREATE TABLE public.commercial_schedules (
    id                          uuid NOT NULL,
    quote_id                    uuid NOT NULL,
    work_order_id               uuid NOT NULL,
    client_id                   uuid,
    approval_date               timestamp(6) without time zone,
    deadline_date                date,
    scheduled_date               date,
    scheduled_time                time without time zone,
    estimated_duration_minutes   integer,
    status                       character varying(30) NOT NULL,
    responsible                  character varying(255),
    team                         character varying(255),
    observations                 text,
    reschedule_reason            text,
    created_at                   timestamp(6) without time zone,
    updated_at                   timestamp(6) without time zone
);

ALTER TABLE ONLY public.commercial_schedules
    ADD CONSTRAINT commercial_schedules_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.commercial_schedules
    ADD CONSTRAINT uk_commercial_schedules_quote UNIQUE (quote_id);

ALTER TABLE ONLY public.commercial_schedules
    ADD CONSTRAINT fk_commercial_schedules_quote FOREIGN KEY (quote_id) REFERENCES public.quotes(id);

ALTER TABLE ONLY public.commercial_schedules
    ADD CONSTRAINT fk_commercial_schedules_work_order FOREIGN KEY (work_order_id) REFERENCES public.work_orders(id);

ALTER TABLE ONLY public.commercial_schedules
    ADD CONSTRAINT fk_commercial_schedules_client FOREIGN KEY (client_id) REFERENCES public.clients(id);

CREATE INDEX idx_commercial_schedules_status ON public.commercial_schedules (status);
CREATE INDEX idx_commercial_schedules_scheduled_date ON public.commercial_schedules (scheduled_date);

-- 3) Histórico / timeline de cada agendamento (criação, agendamento, reagendamento,
--    mudanças de status, cancelamento).
CREATE TABLE public.commercial_schedule_history (
    id            uuid NOT NULL,
    schedule_id   uuid NOT NULL,
    event_date    timestamp(6) without time zone,
    action        character varying(50) NOT NULL,
    username      character varying(255),
    reason        text,
    notes         text
);

ALTER TABLE ONLY public.commercial_schedule_history
    ADD CONSTRAINT commercial_schedule_history_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.commercial_schedule_history
    ADD CONSTRAINT fk_commercial_schedule_history_schedule FOREIGN KEY (schedule_id) REFERENCES public.commercial_schedules(id);

CREATE INDEX idx_commercial_schedule_history_schedule_id ON public.commercial_schedule_history (schedule_id);
