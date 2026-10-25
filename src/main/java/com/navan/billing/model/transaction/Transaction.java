package com.navan.billing.model.transaction;

import com.navan.billing.model.enums.TransactionType;
import com.navan.billing.model.transaction.metadata.TransactionMetadata;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class Transaction {

    Long id;
    String externalId;
    TransactionType type;
    String currency;
    Instant occurredAt;
    BigDecimal total;
    List<TaxLine> taxLines;
    List<FeeLine> feeLines;
    TransactionMetadata metadata;
    Instant createdAt;
    Instant updatedAt;
}
