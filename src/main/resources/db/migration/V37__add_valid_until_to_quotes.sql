ALTER TABLE quotes ADD COLUMN valid_until DATE;

UPDATE quotes
   SET valid_until = CAST(date_created + INTERVAL '2 months' AS DATE)
 WHERE valid_until IS NULL
   AND date_created IS NOT NULL;
