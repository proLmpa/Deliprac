CREATE TABLE IF NOT EXISTS users
(
    id            UUID PRIMARY KEY         DEFAULT gen_random_uuid(),
    email         VARCHAR(255) UNIQUE NOT NULL,
    phone         VARCHAR(255) UNIQUE,
    password_hash VARCHAR(255)       NOT NULL,
    role          VARCHAR(50)        NOT NULL DEFAULT 'CUSTOMER',
    status        VARCHAR(50)        NOT NULL DEFAULT 'ACTIVE',
    created_at    TIMESTAMPTZ        NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ        NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS stores
(
    id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id              UUID         NOT NULL UNIQUE REFERENCES users (id) ON DELETE CASCADE,
    name                 VARCHAR(100) NOT NULL,
    address              VARCHAR(255) NOT NULL,
    phone                VARCHAR(20)  NOT NULL,
    content              TEXT         NOT NULL,
    store_picture_url    VARCHAR(500),
    product_created_time TIME         NOT NULL,
    opened_time          TIME         NOT NULL,
    closed_time          TIME         NOT NULL,
    closed_days          VARCHAR(50)  NOT NULL,
    created_at           TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_stores_user_id ON stores (user_id);
