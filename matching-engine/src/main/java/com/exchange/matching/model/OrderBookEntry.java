package com.exchange.matching.model;

import com.exchange.common.enums.OrderSide;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Represents one resting limit order inside the in-memory order book.
 * Comparable by time so PriorityQueue gives us FIFO within the same price level.
 */
@Data
@Builder
@AllArgsConstructor
public class OrderBookEntry implements Comparable<OrderBookEntry> {

    private final String orderId;
    private final String userId;
    private final String instrument;
    private final OrderSide side;
    private final BigDecimal price;
    private BigDecimal remainingQty;
    private final Instant entryTime;

    @Override
    public int compareTo(OrderBookEntry other) {
        return this.entryTime.compareTo(other.entryTime);
    }
}
