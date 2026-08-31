
ALTER TABLE public.work_orders ADD COLUMN deadline_date date;

UPDATE public.commercial_schedules cs
SET deadline_date = (
    SELECT COALESCE(q.date_approved, cs.approval_date)::date + day_offset
    FROM generate_series(1, 25) AS days(day_offset)
    WHERE EXTRACT(ISODOW FROM (COALESCE(q.date_approved, cs.approval_date)::date + day_offset)) BETWEEN 1 AND 5
    ORDER BY day_offset
    OFFSET 14 LIMIT 1
)
FROM public.quotes q
WHERE q.id = cs.quote_id
  AND cs.deadline_date IS NULL
  AND COALESCE(q.date_approved, cs.approval_date) IS NOT NULL;


UPDATE public.work_orders wo
SET deadline_date = cs.deadline_date,
    install_date = cs.scheduled_date
FROM public.commercial_schedules cs
WHERE cs.work_order_id = wo.id;

UPDATE public.work_orders wo
SET deadline_date = wo.install_date
WHERE NOT EXISTS (
    SELECT 1 FROM public.commercial_schedules cs WHERE cs.work_order_id = wo.id
);

UPDATE public.work_orders wo
SET install_date = NULL
FROM public.quotes q
WHERE q.id = wo.quote_id
  AND q.status = 'approved'
  AND NOT EXISTS (
      SELECT 1 FROM public.commercial_schedules cs WHERE cs.work_order_id = wo.id
  );
