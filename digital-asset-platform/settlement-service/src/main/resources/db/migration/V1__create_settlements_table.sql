CREATE TABLE IF NOT EXISTS settlements (
    id                     BIGSERIAL      PRIMARY KEY,
    settlement_id          VARCHAR(50)    NOT NULL UNIQUE,
    buyer_user_id          BIGINT         NOT NULL,
    seller_user_id         BIGINT         NOT NULL,
    buyer_wallet_address   VARCHAR(100)   NOT NULL,
    seller_wallet_address  VARCHAR(100)   NOT NULL,
    token_amount           NUMERIC(30,8)  NOT NULL,
    settlement_amount      NUMERIC(30,8)  NOT NULL,
    currency               VARCHAR(10)    NOT NULL DEFAULT 'INR',
    status                 VARCHAR(20)    NOT NULL DEFAULT 'INITIATED',
    blockchain_tx_hash     VARCHAR(100),
    failure_reason         VARCHAR(500),
    created_at             TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    updated_at             TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    settled_at             TIMESTAMPTZ
);

CREATE INDEX idx_settlements_id          ON settlements (settlement_id);
CREATE INDEX idx_settlements_buyer       ON settlements (buyer_user_id);
CREATE INDEX idx_settlements_seller      ON settlements (seller_user_id);
CREATE INDEX idx_settlements_status      ON settlements (status);
CREATE INDEX idx_settlements_created_at  ON settlements (created_at DESC);
