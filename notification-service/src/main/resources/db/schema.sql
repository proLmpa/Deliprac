CREATE TABLE IF NOT EXISTS notifications (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id    BIGINT       NOT NULL,
    type       VARCHAR(32)  NOT NULL DEFAULT 'NEW_ORDER',
    title      VARCHAR(255) NOT NULL,
    content    TEXT         NOT NULL,
    store_id   BIGINT,
    store_name VARCHAR(255),
    items      TEXT         NOT NULL DEFAULT '[]',
    is_read    BOOLEAN      NOT NULL DEFAULT FALSE,
    issued_at  BIGINT       NOT NULL,
    expiry     BIGINT       NOT NULL,
    created_at BIGINT       NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_notifications_user_id ON notifications(user_id);

ALTER TABLE notifications ADD COLUMN IF NOT EXISTS type       VARCHAR(32)  NOT NULL DEFAULT 'NEW_ORDER';
ALTER TABLE notifications ADD COLUMN IF NOT EXISTS store_id   BIGINT;
ALTER TABLE notifications ADD COLUMN IF NOT EXISTS store_name VARCHAR(255);
ALTER TABLE notifications ADD COLUMN IF NOT EXISTS items      TEXT         NOT NULL DEFAULT '[]';
