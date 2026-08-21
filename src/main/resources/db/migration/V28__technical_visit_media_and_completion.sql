ALTER TABLE technical_visits
    ADD COLUMN completed_date DATE;

UPDATE technical_visits
SET completed_date = visit_date
WHERE status = 'CONCLUIDA' AND completed_date IS NULL;

ALTER TABLE technical_visit_photos
    ADD COLUMN opening_id UUID;

ALTER TABLE technical_visit_photos
    ADD CONSTRAINT fk_technical_visit_photos_opening
        FOREIGN KEY (opening_id) REFERENCES technical_visit_openings(id) ON DELETE SET NULL;

CREATE INDEX idx_technical_visit_photos_opening ON technical_visit_photos(opening_id);
