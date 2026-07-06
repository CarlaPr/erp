
UPDATE quotes
SET date_approved = date_created
WHERE status = 'approved'
  AND date_approved IS NULL;
