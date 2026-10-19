CREATE TABLE users (
    id         BIGSERIAL PRIMARY KEY,
    email      VARCHAR(160) NOT NULL,
    full_name  VARCHAR(160) NOT NULL,
    password   VARCHAR(100) NOT NULL,
    role       VARCHAR(20)  NOT NULL DEFAULT 'CUSTOMER',
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uk_user_email UNIQUE (email)
);
