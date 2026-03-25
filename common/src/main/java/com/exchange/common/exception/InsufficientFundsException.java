package com.exchange.common.exception;

public class InsufficientFundsException extends ExchangeException {
    public InsufficientFundsException(String userId, String asset) {
        super("INSUFFICIENT_FUNDS",
              "Insufficient available balance for user " + userId + ", asset: " + asset);
    }
}
