CREATE TABLE claim_records (
    id                      BIGSERIAL PRIMARY KEY,
    faucet_name             VARCHAR(100)    NOT NULL,
    network                 VARCHAR(50)     NOT NULL,
    wallet_address          VARCHAR(255)    NOT NULL,
    status                  VARCHAR(50)     NOT NULL,
    eth_amount              DECIMAL(28, 18),
    tx_hash                 VARCHAR(255),
    error_message           TEXT,
    pow_solve_duration_ms   BIGINT,
    claimed_at              TIMESTAMP       NOT NULL,
    created_at              TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_claim_faucet_claimed ON claim_records (faucet_name, claimed_at DESC);
CREATE INDEX idx_claim_status ON claim_records (status);
