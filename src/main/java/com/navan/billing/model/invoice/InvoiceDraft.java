package com.navan.billing.model.invoice;

import java.math.BigDecimal;
import java.util.List;
import lombok.Builder;
import lombok.Value;

/** Invoice content computed by InvoiceSnapshotBuilder, before it has an id or timestamps. */
@Value
@Builder
public class InvoiceDraft {

    String currency;
    BigDecimal total;
    List<InvoiceLine> lines;
    List<TaxRollupLine> taxRollup;
    List<FeeRollupLine> feeRollup;
}
