package com.exchange.order.entity;

import com.exchange.common.enums.OrderSide;
import com.exchange.common.enums.OrderStatus;
import com.exchange.common.enums.OrderType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "orders", indexes = {
    @Index(name = "idx_orders_user_id",   columnList = "user_id"),
    @Index(name = "idx_orders_instrument", columnList = "instrument"),
    @Index(name = "idx_orders_status",    columnList = "status")
})
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order {

    @Id
    @Column(length = 36)
    private String id;               // UUID generated before insert

    @Column(name = "user_id", nullable = false, length = 36)
    private String userId;

    @Column(nullable = false, length = 20)
    private String instrument;       // e.g. BTC-USDT

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private OrderSide side;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private OrderType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrderStatus status;

    @Column(precision = 30, scale = 10)
    private BigDecimal price;        // null for MARKET

    @Column(nullable = false, precision = 30, scale = 10)
    private BigDecimal quantity;

    @Column(name = "filled_qty", nullable = false, precision = 30, scale = 10)
    @Builder.Default
    private BigDecimal filledQty = BigDecimal.ZERO;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;
}
