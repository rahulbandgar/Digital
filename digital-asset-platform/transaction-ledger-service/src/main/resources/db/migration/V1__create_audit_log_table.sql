CREATE TABLE IF NOT EXISTS audit_logs (
    id                  BIGSERIAL      PRIMARY KEY,
    event_id            VARCHAR(50)    NOT NULL UNIQUE,
    transaction_type    VARCHAR(20)    NOT NULL,
    asset_type          VARCHAR(20),
    from_wallet         VARCHAR(100),
    to_wallet           VARCHAR(100),
    from_user_id        BIGINT,
    to_user_id          BIGINT,
    amount              NUMERIC(30,8),
    blockchain_tx_hash  VARCHAR(100),
    kafka_topic         VARCHAR(100),
    metadata            VARCHAR(1000),
    recorded_at         TIMESTAMPTZ    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_audit_event_id    ON audit_logs (event_id);
CREATE INDEX idx_audit_tx_hash     ON audit_logs (blockchain_tx_hash);
CREATE INDEX idx_audit_from_user   ON audit_logs (from_user_id);
CREATE INDEX idx_audit_to_user     ON audit_logs (to_user_id);
CREATE INDEX idx_audit_type        ON audit_logs (transaction_type);
CREATE INDEX idx_audit_recorded_at ON audit_logs (recorded_at DESC);
