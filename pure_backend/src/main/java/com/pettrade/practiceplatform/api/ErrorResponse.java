package com.pettrade.practiceplatform.api;

public record ErrorResponse(String code, String message, String traceId) {

    public ErrorResponse(String code, String message) {
        this(code, message, null);
    }
}
