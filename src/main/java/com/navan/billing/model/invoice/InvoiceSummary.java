package com.navan.billing.model.invoice;

import java.math.BigDecimal;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

/** Public constructor is used by the JPQL {@code select new} projection in InvoiceRepository. */
@Value
@Builder
@AllArgsConstructor
public class InvoiceSummary {

    long id;
    String currency;
    BigDecimal total;
    int lineCount;
    Instant createdAt;
}
