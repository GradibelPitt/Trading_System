package com.exchange.common.event;

import com.exchange.common.enums.OrderSide;
import com.exchange.common.enums.OrderType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Published to Kafka topic: order-events
 * Consumed by: matching-engine
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderEvent {

    public enum Action { PLACE, CANCEL }

    private String orderId;
    private String userId;
    private String instrument;   // e.g. "BTC-USDT"
    private OrderSide side;      // BUY / SELL
    private OrderType type;      // LIMIT / MARKET
    private BigDecimal price;    // null for MARKET orders
    private BigDecimal quantity;
    private Action action;
    private Instant eventTime;
}
