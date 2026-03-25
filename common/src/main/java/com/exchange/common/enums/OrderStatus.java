package com.exchange.common.enums;

public enum OrderStatus {
    PENDING,        // accepted, not yet sent to matching engine
    OPEN,           // resting in order book
    PARTIALLY_FILLED,
    FILLED,
    CANCELLED,
    REJECTED
}
