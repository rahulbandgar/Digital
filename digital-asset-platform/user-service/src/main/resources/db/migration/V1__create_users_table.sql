CREATE TABLE IF NOT EXISTS users (
    id                BIGSERIAL PRIMARY KEY,
    username          VARCHAR(50)  NOT NULL UNIQUE,
    email             VARCHAR(100) NOT NULL UNIQUE,
    password          VARCHAR(255) NOT NULL,
    full_name         VARCHAR(100) NOT NULL,
    role              VARCHAR(20)  NOT NULL DEFAULT 'INVESTOR',
    kyc_status        VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    phone_number      VARCHAR(20),
    country           VARCHAR(3),
    kyc_document_ref  VARCHAR(255),
    is_active         BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_users_username   ON users (username);
CREATE INDEX idx_users_email      ON users (email);
CREATE INDEX idx_users_kyc_status ON users (kyc_status);

-- Seed admin user (password: Admin@1234)
INSERT INTO users (username, email, password, full_name, role, kyc_status)
VALUES (
    'admin',
    'admin@digitalasset.com',
    '$2a$12$tJWf0TFiSU3SzpFw5yHiH.EBvfZJm3e.ufh.r5m9XqNaV7Yl6lkHy',
    'Platform Administrator',
    'ADMIN',
    'VERIFIED'
) ON CONFLICT DO NOTHING;
