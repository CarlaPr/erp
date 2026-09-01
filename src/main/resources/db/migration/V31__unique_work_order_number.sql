-- Garante que o número da Ordem de Serviço seja único no banco, para que nenhuma
-- condição de corrida (duas O.S. novas salvas quase ao mesmo tempo) possa gerar
-- duas ordens de serviço com o mesmo número.

-- Corrige eventuais duplicidades históricas antes de aplicar a constraint: mantém o
-- número original na O.S. mais antiga de cada grupo e renomeia as demais com o
-- prefixo "OSDUP-" (fora do padrão "OS-%"), para não quebrar o deploy em bases que já
-- tenham números repetidos nem a busca do próximo número sequencial (que assume que
-- tudo após "OS-" é numérico).
WITH ranked AS (
    SELECT id,
           number,
           ROW_NUMBER() OVER (PARTITION BY number ORDER BY created_at NULLS LAST, id) AS rn
    FROM public.work_orders
    WHERE number IS NOT NULL
)
UPDATE public.work_orders wo
SET number = 'OSDUP-' || ranked.rn || '-' || wo.number
FROM ranked
WHERE wo.id = ranked.id
  AND ranked.rn > 1;

ALTER TABLE public.work_orders
    ADD CONSTRAINT uk_work_orders_number UNIQUE (number);
