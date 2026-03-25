package com.exchange.common.event;

import com.exchange.common.enums.OrderSide;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Published to Kafka topic: trade-events
 * Consumed by: account-service, market-data-service
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TradeEvent {

    private String tradeId;
    private String instrument;

    private String makerOrderId;
    private String takerOrderId;
    private String makerUserId;
    private String takerUserId;

    private OrderSide takerSide;   // direction of the aggressor
    private BigDecimal price;
    private BigDecimal quantity;
    private BigDecimal makerFee;
    private BigDecimal takerFee;

    private Instant tradeTime;
}
