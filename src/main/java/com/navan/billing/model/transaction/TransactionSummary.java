package com.navan.billing.model.transaction;

import com.navan.billing.model.enums.TransactionType;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

/** Public constructor is used by the JPQL {@code select new} projection in TransactionRepository. */
@Value
@Builder
@AllArgsConstructor
public class TransactionSummary {

    long id;
    String externalId;
    TransactionType type;
    String currency;
    Instant occurredAt;
    BigDecimal total;
    Instant createdAt;
}
