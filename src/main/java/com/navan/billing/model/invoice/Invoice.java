package com.navan.billing.model.invoice;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class Invoice {

    long id;
    String currency;
    BigDecimal total;
    List<InvoiceLine> lines;
    List<TaxRollupLine> taxRollup;
    List<FeeRollupLine> feeRollup;
    Instant createdAt;
    Instant updatedAt;
}
