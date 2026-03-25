package com.exchange.order.dto;

import com.exchange.common.enums.OrderSide;
import com.exchange.common.enums.OrderStatus;
import com.exchange.common.enums.OrderType;
import com.exchange.order.entity.Order;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
public class OrderResponse {

    private String orderId;
    private String userId;
    private String instrument;
    private OrderSide side;
    private OrderType type;
    private OrderStatus status;
    private BigDecimal price;
    private BigDecimal quantity;
    private BigDecimal filledQty;
    private BigDecimal remainingQty;
    private Instant createdAt;
    private Instant updatedAt;

    public static OrderResponse from(Order o) {
        return OrderResponse.builder()
                .orderId(o.getId())
                .userId(o.getUserId())
                .instrument(o.getInstrument())
                .side(o.getSide())
                .type(o.getType())
                .status(o.getStatus())
                .price(o.getPrice())
                .quantity(o.getQuantity())
                .filledQty(o.getFilledQty())
                .remainingQty(o.getQuantity().subtract(o.getFilledQty()))
                .createdAt(o.getCreatedAt())
                .updatedAt(o.getUpdatedAt())
                .build();
    }
}
