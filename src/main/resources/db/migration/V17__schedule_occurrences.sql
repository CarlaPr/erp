-- =====================================================================
-- V17: Múltiplas datas por agendamento (Agenda Comercial)
-- Até aqui, cada "commercial_schedules" (vinculado 1:1 a um quote_id)
-- só podia ter UMA data de execução (scheduled_date). Isso impedia que
-- uma mesma OS fosse agendada em mais de um dia (ex.: OS1001 em 10/08
-- e novamente em 17/08, para conclusão do serviço em duas etapas).
--
-- A partir de agora, cada Schedule pode ter N ocorrências de agenda,
-- cada uma com sua própria data, horário, duração, status e observação.
-- Os campos scheduled_date/scheduled_time/estimated_duration_minutes
-- em commercial_schedules são mantidos (não removidos) para não quebrar
-- código legado/relatórios, mas passam a representar apenas a PRÓXIMA
-- ocorrência (ou a mais recente), sincronizados automaticamente pela
-- aplicação sempre que uma ocorrência é criada, editada ou removida.
-- =====================================================================

CREATE TABLE public.commercial_schedule_occurrences (
    id                          uuid NOT NULL,
    schedule_id                 uuid NOT NULL,
    occurrence_date             date NOT NULL,
    occurrence_time             time without time zone,
    estimated_duration_minutes  integer,
    status                      character varying(30) NOT NULL,
    responsible                 character varying(255),
    team                        character varying(255),
    observations                text,
    created_at                  timestamp(6) without time zone,
    updated_at                  timestamp(6) without time zone
);

ALTER TABLE ONLY public.commercial_schedule_occurrences
    ADD CONSTRAINT commercial_schedule_occurrences_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.commercial_schedule_occurrences
    ADD CONSTRAINT fk_commercial_schedule_occurrences_schedule
        FOREIGN KEY (schedule_id) REFERENCES public.commercial_schedules(id) ON DELETE CASCADE;

-- Evita duas ocorrências idênticas (mesma data) para o mesmo agendamento.
ALTER TABLE ONLY public.commercial_schedule_occurrences
    ADD CONSTRAINT uk_commercial_schedule_occurrences_schedule_date
        UNIQUE (schedule_id, occurrence_date);

CREATE INDEX idx_commercial_schedule_occurrences_schedule_id
    ON public.commercial_schedule_occurrences (schedule_id);

CREATE INDEX idx_commercial_schedule_occurrences_date
    ON public.commercial_schedule_occurrences (occurrence_date);

-- Migra os agendamentos já existentes: quem já tinha uma scheduled_date
-- ganha uma ocorrência correspondente, preservando horário, duração,
-- responsável, equipe, observações e status atuais.
INSERT INTO public.commercial_schedule_occurrences
    (id, schedule_id, occurrence_date, occurrence_time, estimated_duration_minutes,
     status, responsible, team, observations, created_at, updated_at)
SELECT
    gen_random_uuid(),
    cs.id,
    cs.scheduled_date,
    cs.scheduled_time,
    cs.estimated_duration_minutes,
    cs.status,
    cs.responsible,
    cs.team,
    cs.observations,
    COALESCE(cs.created_at, now()),
    COALESCE(cs.updated_at, now())
FROM public.commercial_schedules cs
WHERE cs.scheduled_date IS NOT NULL;
