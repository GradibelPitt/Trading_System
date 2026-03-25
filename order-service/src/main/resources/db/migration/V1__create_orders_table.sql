CREATE TABLE IF NOT EXISTS orders
(
    id          VARCHAR(36)    NOT NULL PRIMARY KEY,
    user_id     VARCHAR(36)    NOT NULL,
    instrument  VARCHAR(20)    NOT NULL,
    side        VARCHAR(10)    NOT NULL,
    type        VARCHAR(10)    NOT NULL,
    status      VARCHAR(20)    NOT NULL,
    price       NUMERIC(30,10),
    quantity    NUMERIC(30,10) NOT NULL,
    filled_qty  NUMERIC(30,10) NOT NULL DEFAULT 0,
    created_at  TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_orders_user_id    ON orders (user_id);
CREATE INDEX idx_orders_instrument ON orders (instrument);
CREATE INDEX idx_orders_status     ON orders (status);
CREATE INDEX idx_orders_user_status ON orders (user_id, status);
