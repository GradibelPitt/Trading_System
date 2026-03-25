package com.exchange.marketdata.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

/** Broadcast topic: /topic/ticker.{instrument}  e.g. /topic/ticker.BTC-USDT */
@Data
@Builder
public class TickerMessage {
    private String instrument;
    private BigDecimal lastPrice;
    private BigDecimal lastQty;
    private Instant tradeTime;
}
