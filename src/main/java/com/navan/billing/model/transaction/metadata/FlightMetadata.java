package com.navan.billing.model.transaction.metadata;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.navan.billing.model.MoneyDeserializer;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import java.util.List;
import lombok.Value;

@Value
public class FlightMetadata implements TransactionMetadata {

    @NotBlank
    String origin;

    @NotBlank
    String destination;

    @NotNull
    @Digits(integer = 17, fraction = 2)
    @PositiveOrZero
    @JsonDeserialize(using = MoneyDeserializer.class)
    BigDecimal fare;

    @NotEmpty
    List<@NotNull @Valid Coupon> coupons;
}
