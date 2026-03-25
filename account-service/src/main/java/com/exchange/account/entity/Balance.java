package com.exchange.account.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "balances",
       uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "asset"}))
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Balance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, length = 36)
    private String userId;

    @Column(nullable = false, length = 20)
    private String asset;            // e.g. "USDT", "BTC"

    @Column(nullable = false, precision = 30, scale = 10)
    @Builder.Default
    private BigDecimal available = BigDecimal.ZERO;

    @Column(nullable = false, precision = 30, scale = 10)
    @Builder.Default
    private BigDecimal frozen = BigDecimal.ZERO;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;

    public BigDecimal getTotal() {
        return available.add(frozen);
    }
}
