CREATE TABLE work_order_checklists (
    work_order_id UUID PRIMARY KEY REFERENCES work_orders(id) ON DELETE CASCADE
);

CREATE TABLE work_order_checklist_items (
    id UUID PRIMARY KEY,
    work_order_id UUID NOT NULL REFERENCES work_order_checklists(work_order_id) ON DELETE CASCADE,
    name VARCHAR(180) NOT NULL,
    quantity NUMERIC(10, 2) CHECK (quantity > 0),
    observation TEXT,
    completed BOOLEAN NOT NULL DEFAULT FALSE,
    suggestion_key VARCHAR(64),
    created_at TIMESTAMP NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    UNIQUE (work_order_id, suggestion_key)
);

CREATE INDEX idx_work_order_checklist_items_order
    ON work_order_checklist_items (work_order_id, completed, created_at);

CREATE TABLE work_order_execution_notes (
    id UUID PRIMARY KEY,
    work_order_id UUID NOT NULL REFERENCES work_orders(id) ON DELETE CASCADE,
    author_id UUID REFERENCES app_users(id) ON DELETE SET NULL,
    author_name VARCHAR(255) NOT NULL,
    body TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_work_order_execution_notes_order
    ON work_order_execution_notes (work_order_id, created_at DESC);
