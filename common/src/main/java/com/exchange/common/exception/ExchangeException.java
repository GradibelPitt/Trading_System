package com.exchange.common.exception;

public class ExchangeException extends RuntimeException {

    private final String errorCode;

    public ExchangeException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
