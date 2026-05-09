CREATE TABLE IF NOT EXISTS assets (
    id                     BIGSERIAL      PRIMARY KEY,
    name                   VARCHAR(100)   NOT NULL,
    description            VARCHAR(500),
    asset_type             VARCHAR(20)    NOT NULL,
    physical_quantity      NUMERIC(30,8)  NOT NULL,
    total_supply           NUMERIC(30,8)  NOT NULL DEFAULT 0,
    token_ratio            NUMERIC(30,8)  NOT NULL DEFAULT 1,
    smart_contract_address VARCHAR(100),
    vault_ref              VARCHAR(255),
    is_active              BOOLEAN        NOT NULL DEFAULT TRUE,
    created_at             TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    updated_at             TIMESTAMPTZ    NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS token_operations (
    id                  BIGSERIAL      PRIMARY KEY,
    asset_id            BIGINT         NOT NULL REFERENCES assets(id),
    operation_type      VARCHAR(20)    NOT NULL,
    asset_type          VARCHAR(20)    NOT NULL,
    amount              NUMERIC(30,8)  NOT NULL,
    wallet_id           BIGINT,
    wallet_address      VARCHAR(100),
    blockchain_tx_hash  VARCHAR(100),
    performed_by        VARCHAR(50)    NOT NULL,
    reason              VARCHAR(500),
    created_at          TIMESTAMPTZ    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_assets_type          ON assets (asset_type);
CREATE INDEX idx_token_ops_asset_id   ON token_operations (asset_id);
CREATE INDEX idx_token_ops_type       ON token_operations (operation_type);
CREATE INDEX idx_token_ops_wallet     ON token_operations (wallet_id);
