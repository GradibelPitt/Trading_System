package com.exchange.account.dto;

import com.exchange.account.entity.Balance;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
public class BalanceResponse {

    private String userId;
    private String asset;
    private BigDecimal available;
    private BigDecimal frozen;
    private BigDecimal total;
    private Instant updatedAt;

    public static BalanceResponse from(Balance b) {
        return BalanceResponse.builder()
                .userId(b.getUserId())
                .asset(b.getAsset())
                .available(b.getAvailable())
                .frozen(b.getFrozen())
                .total(b.getTotal())
                .updatedAt(b.getUpdatedAt())
                .build();
    }
}
