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
