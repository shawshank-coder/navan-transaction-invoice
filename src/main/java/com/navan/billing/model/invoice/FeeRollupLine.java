package com.navan.billing.model.invoice;

import java.math.BigDecimal;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Builder
@Jacksonized
public class FeeRollupLine {

    String name;
    BigDecimal amount;
}
