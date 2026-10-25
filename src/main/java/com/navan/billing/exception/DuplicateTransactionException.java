package com.navan.billing.exception;

public class DuplicateTransactionException extends RuntimeException {

    private final long existingId;

    public DuplicateTransactionException(String externalId, long existingId) {
        super("transaction with external_id '" + externalId + "' already exists");
        this.existingId = existingId;
    }

    public long existingId() {
        return existingId;
    }
}
