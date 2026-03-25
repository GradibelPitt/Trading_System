package com.exchange.marketdata.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "candles",
       uniqueConstraints = @UniqueConstraint(
               columnNames = {"instrument", "interval_type", "open_time"}),
       indexes = {
           @Index(name = "idx_candles_instrument_interval_time",
                  columnList = "instrument, interval_type, open_time")
       })
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Candle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 20)
    private String instrument;

    /** e.g. "1m", "5m", "1h" */
    @Column(name = "interval_type", nullable = false, length = 5)
    private String intervalType;

    @Column(name = "open_time", nullable = false)
    private Instant openTime;

    @Column(name = "close_time", nullable = false)
    private Instant closeTime;

    @Column(nullable = false, precision = 30, scale = 10)
    private BigDecimal open;

    @Column(nullable = false, precision = 30, scale = 10)
    private BigDecimal high;

    @Column(nullable = false, precision = 30, scale = 10)
    private BigDecimal low;

    @Column(nullable = false, precision = 30, scale = 10)
    private BigDecimal close;

    @Column(nullable = false, precision = 30, scale = 10)
    private BigDecimal volume;

    @Column(name = "trade_count", nullable = false)
    @Builder.Default
    private Long tradeCount = 0L;
}
