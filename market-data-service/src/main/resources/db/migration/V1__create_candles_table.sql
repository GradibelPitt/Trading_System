CREATE TABLE IF NOT EXISTS candles
(
    id            BIGSERIAL       PRIMARY KEY,
    instrument    VARCHAR(20)     NOT NULL,
    interval_type VARCHAR(5)      NOT NULL,
    open_time     TIMESTAMPTZ     NOT NULL,
    close_time    TIMESTAMPTZ     NOT NULL,
    open          NUMERIC(30,10)  NOT NULL,
    high          NUMERIC(30,10)  NOT NULL,
    low           NUMERIC(30,10)  NOT NULL,
    close         NUMERIC(30,10)  NOT NULL,
    volume        NUMERIC(30,10)  NOT NULL DEFAULT 0,
    trade_count   BIGINT          NOT NULL DEFAULT 0,
    CONSTRAINT uq_candle UNIQUE (instrument, interval_type, open_time)
);

CREATE INDEX idx_candles_lookup
    ON candles (instrument, interval_type, open_time);
