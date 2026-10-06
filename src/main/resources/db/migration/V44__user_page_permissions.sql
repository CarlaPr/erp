CREATE TABLE user_page_permissions (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES app_users(id) ON DELETE CASCADE,
    page_key VARCHAR(64) NOT NULL,
    can_view BOOLEAN NOT NULL DEFAULT FALSE,
    can_edit BOOLEAN NOT NULL DEFAULT FALSE,
    can_delete BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT uq_user_page_permission UNIQUE (user_id, page_key),
    CONSTRAINT ck_permission_requires_view CHECK (can_view OR (NOT can_edit AND NOT can_delete))
);
