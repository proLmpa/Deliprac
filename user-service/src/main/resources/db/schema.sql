CREATE SEQUENCE IF NOT EXISTS users_seq START WITH 1 INCREMENT BY 50;

CREATE TABLE IF NOT EXISTS users
(
    id            BIGINT       PRIMARY KEY,
    email         VARCHAR(255) UNIQUE NOT NULL,
    phone         VARCHAR(255) UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role          VARCHAR(50)  NOT NULL,
    status        VARCHAR(50)  NOT NULL,
    created_at    BIGINT       NOT NULL,
    updated_at    BIGINT       NOT NULL
);
