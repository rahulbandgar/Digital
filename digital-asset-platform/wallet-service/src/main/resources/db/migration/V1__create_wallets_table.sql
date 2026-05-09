CREATE TABLE IF NOT EXISTS wallets (
    id             BIGSERIAL PRIMARY KEY,
    user_id        BIGINT          NOT NULL,
    wallet_address VARCHAR(100)    NOT NULL UNIQUE,
    asset_type     VARCHAR(20)     NOT NULL,
    balance        NUMERIC(30, 8)  NOT NULL DEFAULT 0,
    is_active      BOOLEAN         NOT NULL DEFAULT TRUE,
    version        BIGINT          NOT NULL DEFAULT 0,
    created_at     TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_user_asset UNIQUE (user_id, asset_type)
);

CREATE INDEX idx_wallets_user_id        ON wallets (user_id);
CREATE INDEX idx_wallets_address        ON wallets (wallet_address);
CREATE INDEX idx_wallets_asset_type     ON wallets (asset_type);
