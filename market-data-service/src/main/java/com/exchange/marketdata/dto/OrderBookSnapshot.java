package com.exchange.marketdata.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * L2 order book snapshot broadcast.
 * Topic: /topic/depth.{instrument}  e.g. /topic/depth.BTC-USDT
 *
 * Each entry is [price, totalQty] at that price level.
 */
@Data
@Builder
public class OrderBookSnapshot {
    private String instrument;
    private List<PriceLevel> bids;   // sorted descending
    private List<PriceLevel> asks;   // sorted ascending
    private Instant snapshotTime;

    @Data
    @Builder
    public static class PriceLevel {
        private BigDecimal price;
        private BigDecimal qty;
    }
}
