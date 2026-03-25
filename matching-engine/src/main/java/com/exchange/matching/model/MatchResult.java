package com.exchange.matching.model;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class MatchResult {

    private final String takerOrderId;
    private final List<Fill> fills;
    private final BigDecimal remainingQty;   // 0 = fully filled
    private final boolean cancelled;         // true if market order killed for lack of depth

    @Data
    @Builder
    public static class Fill {
        private final String makerOrderId;
        private final String makerUserId;
        private final String takerUserId;
        private final BigDecimal price;
        private final BigDecimal quantity;
    }
}
