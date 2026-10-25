package com.navan.billing.entity;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import com.navan.billing.model.enums.TransactionType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

/**
 * Storage format of invoice_lines.snapshot. It owns its tax and fee line types, so renaming a field
 * on the API models can't change this JSON or break reading older invoices. Bump VERSION when this shape changes.
 */
@Value
@Builder
@Jacksonized
public class InvoiceLineSnapshot {

    public static final short VERSION = 1;

    long transactionId;
    TransactionType type;
    String externalId;
    String currency;
    Instant occurredAt;
    BigDecimal total;
    List<TaxLineSnapshot> taxLines;
    List<FeeLineSnapshot> feeLines;
    // Copied verbatim from the transaction and only ever read back as raw JSON, never bound to API classes.
    JsonNode metadata;

    @Value
    @Builder
    @Jacksonized
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class TaxLineSnapshot {
        String name;
        BigDecimal rate;
        BigDecimal amount;
    }

    @Value
    @Builder
    @Jacksonized
    public static class FeeLineSnapshot {
        String name;
        BigDecimal amount;
    }
}
