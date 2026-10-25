package com.navan.billing.model.invoice;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.math.BigDecimal;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Builder
@Jacksonized
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TaxRollupLine {

    String name;
    BigDecimal rate;
    BigDecimal amount;
}
