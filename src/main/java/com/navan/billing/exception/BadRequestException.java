package com.navan.billing.exception;

import java.util.List;

public class BadRequestException extends RuntimeException {

    private final List<String> details;

    public BadRequestException(String message) {
        this(message, List.of());
    }

    public BadRequestException(String message, List<String> details) {
        super(message);
        this.details = details;
    }

    public List<String> details() {
        return details;
    }
}
