CREATE TABLE IF NOT EXISTS balances
(
    id         BIGSERIAL      PRIMARY KEY,
    user_id    VARCHAR(36)    NOT NULL,
    asset      VARCHAR(20)    NOT NULL,
    available  NUMERIC(30,10) NOT NULL DEFAULT 0,
    frozen     NUMERIC(30,10) NOT NULL DEFAULT 0,
    updated_at TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_balances_user_asset UNIQUE (user_id, asset),
    CONSTRAINT chk_available_non_negative CHECK (available >= 0),
    CONSTRAINT chk_frozen_non_negative    CHECK (frozen    >= 0)
);

CREATE INDEX idx_balances_user_id ON balances (user_id);
