ALTER TABLE user_page_permissions
    ADD COLUMN can_manage_agenda_blocks BOOLEAN NOT NULL DEFAULT FALSE;

CREATE TABLE agenda_day_blocks (
    block_date DATE PRIMARY KEY,
    reason VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
