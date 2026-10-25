package com.navan.billing.model.invoice;

import com.fasterxml.jackson.databind.JsonNode;
import com.navan.billing.model.enums.TransactionType;
import com.navan.billing.model.transaction.FeeLine;
import com.navan.billing.model.transaction.TaxLine;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import lombok.Builder;
import lombok.Value;

/** One invoice line as the API returns it: a copy of a transaction taken at invoicing time. */
@Value
@Builder
public class InvoiceLine {

    long transactionId;
    TransactionType type;
    String externalId;
    String currency;
    Instant occurredAt;
    BigDecimal total;
    List<TaxLine> taxLines;
    List<FeeLine> feeLines;
    JsonNode metadata;
}
